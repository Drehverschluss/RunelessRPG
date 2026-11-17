package com.drehverschluss.runelessrpg.orb;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import com.drehverschluss.runelessrpg.potion.RunelessPotions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Mana orbs: registration, the drop rules for kills and boss hits, and picking orbs up.
 * <ul>
 *   <li><b>Kills</b> ({@link LivingExperienceDropEvent}, so exactly the vanilla experience rules: a player hit the mob
 *       in the last 5 s, doMobLoot, no baby animals, not cancelled by another mod). Chance = (base chance + Mana Harvest
 *       level × chance per level) × data factor, at most 100 %; further kills of the same player in the same game tick
 *       get {@code extraKillPercent} of it.</li>
 *   <li><b>Bosses</b> ({@code #runelessrpg:mana_orb_bosses}): no kill chance; {@code bossKillOrbs} on death, and while
 *       fighting a chance per hit ({@link LivingDamageEvent.Post}) with a cooldown per player. The orb appears on free
 *       ground 2–4 blocks around the player who hit.</li>
 *   <li>Only real players count: no {@link FakePlayer}, and the player must be in the server's player list.</li>
 * </ul>
 * Cooldowns and counters are in game ticks, in memory only, removed on logout.
 */
public final class ManaOrbs {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, RunelessRPG.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ManaOrb>> MANA_ORB = ENTITY_TYPES.register("mana_orb",
            () -> EntityType.Builder.<ManaOrb>of(ManaOrb::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .fireImmune()
                    .build("mana_orb"));

    /** Mobs that never drop orbs. */
    public static final TagKey<EntityType<?>> NO_MANA_ORBS = TagKey.create(Registries.ENTITY_TYPE, id("no_mana_orbs"));
    /** Bosses: no kill chance, orbs from hits instead. Contains {@code #c:bosses}. */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, id("mana_orb_bosses"));
    /** Damage types that never create orbs from boss hits (thorns). */
    public static final TagKey<DamageType> IGNORED_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, id("mana_orbs/ignored_damage"));

    private static final int PICKUP_DELAY_TICKS = 2;

    private record KillCounter(long gameTime, int kills) {}

    private static final Map<UUID, KillCounter> KILLS = new HashMap<>();
    private static final Map<UUID, Long> KILL_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> BOSS_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> NEXT_PICKUP = new HashMap<>();

    private ManaOrbs() {}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingExperienceDropEvent.class, ManaOrbs::onExperienceDrop);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDeathEvent.class, ManaOrbs::onDeath);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, ManaOrbs::onDamage);
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> event.addListener(new ManaOrbData.Loader()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            UUID uuid = event.getEntity().getUUID();
            KILLS.remove(uuid);
            KILL_COOLDOWN_UNTIL.remove(uuid);
            BOSS_COOLDOWN_UNTIL.remove(uuid);
            NEXT_PICKUP.remove(uuid);
        });
    }

    // ------------------------------------------------------------------ picking up

    /** Called when a player touches an orb: gives the mana of one orb, at most one orb per player every 2 ticks. */
    static boolean tryPickUp(ServerPlayer player, ManaOrb orb) {
        long now = player.level().getGameTime();
        Long next = NEXT_PICKUP.get(player.getUUID());
        if (next != null && now < next) {
            return false;
        }
        NEXT_PICKUP.put(player.getUUID(), now + PICKUP_DELAY_TICKS);
        double amount = ManaManager.getMaxMana(player) * RunelessServerConfig.ORB_MANA_PERCENT.get() / 100.0;
        if (amount > 0.0) {
            ManaManager.restoreMana(player, amount, ManaChangeEvent.Reason.ORB);
        }
        RandomSource random = player.getRandom();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.25F, 1.6F + random.nextFloat() * 0.4F);
        return true;
    }

    // ------------------------------------------------------------------ kills

    private static void onExperienceDrop(LivingExperienceDropEvent event) {
        try {
            if (!RunelessServerConfig.MANA_ORBS_ENABLED.get() || !(event.getEntity().level() instanceof ServerLevel level)
                    || !(event.getAttackingPlayer() instanceof ServerPlayer player) || !isRealPlayer(player)) {
                return;
            }
            LivingEntity mob = event.getEntity();
            EntityType<?> type = mob.getType();
            if (mob instanceof Player || type.is(NO_MANA_ORBS) || type.is(BOSSES)) {
                return;
            }
            ManaOrbData.Entry entry = ManaOrbData.find(type);
            if (entry == null && !categoryAllowed(type.getCategory())) {
                return;
            }
            long now = level.getGameTime();
            KillCounter counter = KILLS.get(player.getUUID());
            int previousKills = counter != null && counter.gameTime() == now ? counter.kills() : 0;
            KILLS.put(player.getUUID(), new KillCounter(now, previousKills + 1));
            Long cooldownUntil = KILL_COOLDOWN_UNTIL.get(player.getUUID());
            if (cooldownUntil != null && now < cooldownUntil) {
                return;
            }
            double chance = killChance(entry, harvestLevel(player));
            if (previousKills > 0) {
                chance *= RunelessServerConfig.EXTRA_KILL_PERCENT.get() / 100.0;
            }
            if (chance <= 0.0 || level.getRandom().nextDouble() >= chance) {
                return;
            }
            spawn(level, mob.position(), entry != null ? entry.count() : 1, true);
            double cooldown = RunelessServerConfig.KILL_COOLDOWN_SECONDS.get();
            if (cooldown > 0.0) {
                KILL_COOLDOWN_UNTIL.put(player.getUUID(), now + Math.round(cooldown * 20.0));
            }
        } catch (RuntimeException e) {
            RunelessRPG.LOGGER.error("Mana orb drop failed", e);
        }
    }

    /** Kill chance 0–1: (data base chance or config base chance + level × chance per level) × factor, at most 100 %. */
    static double killChance(ManaOrbData.Entry entry, int harvestLevel) {
        double base = entry != null && entry.baseChance() != null ? entry.baseChance() : RunelessServerConfig.KILL_BASE_CHANCE_PERCENT.get();
        double percent = (base + harvestLevel * RunelessServerConfig.KILL_CHANCE_PERCENT_PER_LEVEL.get()) * (entry != null ? entry.factor() : 1.0);
        return Mth.clamp(percent, 0.0, 100.0) / 100.0;
    }

    private static boolean categoryAllowed(MobCategory category) {
        for (String name : RunelessServerConfig.KILL_MOB_CATEGORIES.get()) {
            if (category.name().equalsIgnoreCase(name.trim()) || category.getName().equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ bosses

    private static void onDeath(LivingDeathEvent event) {
        try {
            LivingEntity boss = event.getEntity();
            int orbs = RunelessServerConfig.BOSS_KILL_ORBS.get();
            if (orbs <= 0 || !RunelessServerConfig.MANA_ORBS_ENABLED.get() || !(boss.level() instanceof ServerLevel level)
                    || !boss.getType().is(BOSSES) || boss.getType().is(NO_MANA_ORBS)) {
                return;
            }
            if (boss.getKillCredit() instanceof ServerPlayer player && isRealPlayer(player)) {
                spawn(level, boss.position(), orbs, true);
            }
        } catch (RuntimeException e) {
            RunelessRPG.LOGGER.error("Mana orb boss drop failed", e);
        }
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        try {
            LivingEntity boss = event.getEntity();
            if (event.getNewDamage() <= 0.0F || !RunelessServerConfig.MANA_ORBS_ENABLED.get() || !(boss.level() instanceof ServerLevel level)
                    || !boss.getType().is(BOSSES) || boss.getType().is(NO_MANA_ORBS) || event.getSource().is(IGNORED_DAMAGE)
                    || !(event.getSource().getEntity() instanceof ServerPlayer player) || !isRealPlayer(player)) {
                return;
            }
            double percent = RunelessServerConfig.BOSS_HIT_BASE_CHANCE_PERCENT.get()
                    + harvestLevel(player) * RunelessServerConfig.BOSS_HIT_CHANCE_PERCENT_PER_LEVEL.get();
            double chance = Mth.clamp(percent, 0.0, 100.0) / 100.0;
            if (chance <= 0.0) {
                return;
            }
            long now = level.getGameTime();
            Long cooldownUntil = BOSS_COOLDOWN_UNTIL.get(player.getUUID());
            if (cooldownUntil != null && now < cooldownUntil) {
                return;
            }
            if (level.getRandom().nextDouble() >= chance) {
                return;
            }
            spawn(level, spotNear(level, player), 1, false);
            double cooldown = RunelessServerConfig.BOSS_HIT_COOLDOWN_SECONDS.get();
            if (cooldown > 0.0) {
                BOSS_COOLDOWN_UNTIL.put(player.getUUID(), now + Math.round(cooldown * 20.0));
            }
        } catch (RuntimeException e) {
            RunelessRPG.LOGGER.error("Mana orb boss hit failed", e);
        }
    }

    /**
     * A free spot on the ground {@code bossOrbMinDistance}–{@code bossOrbMaxDistance} blocks around the player: solid
     * floor, no block in the way, no lava, in sight of the player (not behind a wall). Without one (flying, on a pillar,
     * above the void) the orb appears at the player's feet and is picked up right away.
     */
    static Vec3 spotNear(ServerLevel level, ServerPlayer player) {
        RandomSource random = player.getRandom();
        double min = RunelessServerConfig.BOSS_ORB_MIN_DISTANCE.get();
        double max = Math.max(min, RunelessServerConfig.BOSS_ORB_MAX_DISTANCE.get());
        Vec3 eye = player.getEyePosition();
        int startY = Mth.floor(player.getY()) + 3;
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = min + random.nextDouble() * (max - min);
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            for (int y = startY; y >= startY - 9; y--) {
                BlockPos pos = BlockPos.containing(x, y, z);
                if (pos.getY() <= level.getMinBuildHeight() || !level.isLoaded(pos)) {
                    break;
                }
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || level.getFluidState(pos).is(FluidTags.LAVA)) {
                    continue;
                }
                BlockPos below = pos.below();
                VoxelShape floor = level.getBlockState(below).getCollisionShape(level, below);
                if (floor.isEmpty()) {
                    continue;
                }
                Vec3 spot = new Vec3(x, below.getY() + floor.max(Direction.Axis.Y) + 0.05, z);
                if (!level.noCollision(AABB.ofSize(spot.add(0.0, 0.25, 0.0), 0.5, 0.5, 0.5))) {
                    break;
                }
                if (level.clip(new ClipContext(eye, spot.add(0.0, 0.25, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                        .getType() == HitResult.Type.MISS) {
                    return spot;
                }
                break;
            }
        }
        return player.position();
    }

    // ------------------------------------------------------------------ helpers

    /** Spawns {@code count} orbs as one entity, or adds them to an orb that is already there. */
    public static void spawn(ServerLevel level, Vec3 pos, int count, boolean scatter) {
        if (count <= 0) {
            return;
        }
        for (ManaOrb existing : level.getEntitiesOfClass(ManaOrb.class, AABB.ofSize(pos, 1.0, 1.0, 1.0), orb -> !orb.isRemoved())) {
            existing.addCount(count);
            return;
        }
        level.addFreshEntity(new ManaOrb(level, pos, count, scatter));
    }

    /** Level of Mana Harvest (0 without the effect). */
    public static int harvestLevel(Player player) {
        MobEffectInstance effect = player.getEffect(RunelessPotions.MANA_HARVEST);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    /** True for players that are really connected: no fake players of mob farms, deployers or killer blocks. */
    public static boolean isRealPlayer(Player player) {
        return player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)
                && serverPlayer.server.getPlayerList().getPlayer(serverPlayer.getUUID()) == serverPlayer;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, path.toLowerCase(Locale.ROOT));
    }
}
