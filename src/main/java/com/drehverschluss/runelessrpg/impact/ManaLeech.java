package com.drehverschluss.runelessrpg.impact;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.spell_engine.api.spell.Spell;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mana leech (weapon damage, {@code runelessrpg:mana_leech}) and spell mana leech (direct spell hits,
 * {@code runelessrpg:spell_mana_leech}), from the real damage after armor ({@link LivingDamageEvent.Post}).
 * <p>
 * Spell Engine hits are recognized by the {@link SpellHitContext} set around {@code Entity.hurt} in
 * {@code performImpact} (the damage events fire synchronously inside it).
 */
public final class ManaLeech {
    /** Damage types that count for weapon mana leech. */
    public static final TagKey<DamageType> WEAPON_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "mana_leech/weapon_damage"));
    /** Magic spell damage of Spell Power ({@code #spell_power:all}). */
    public static final TagKey<DamageType> SPELL_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("spell_power", "all"));
    /** Entities that never give mana (training dummies, armor stands). */
    public static final TagKey<EntityType<?>> NO_MANA_LEECH = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "no_mana_leech"));

    /** Hits per player in the current game tick, separately for weapon and spell leech. */
    private record Counter(long gameTime, int hits) {}

    private static final Map<UUID, Counter> WEAPON_HITS = new HashMap<>();
    private static final Map<UUID, Counter> SPELL_HITS = new HashMap<>();
    /** Health of targets right before the damage is applied (Pre → Post of the same hit). */
    private static final Map<LivingEntity, Float> HEALTH_BEFORE = new IdentityHashMap<>();

    private ManaLeech() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ManaLeech::onDamagePre);
        NeoForge.EVENT_BUS.addListener(ManaLeech::onDamagePost);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            WEAPON_HITS.remove(event.getEntity().getUUID());
            SPELL_HITS.remove(event.getEntity().getUUID());
        });
        // Damage that is reduced to 0 never reaches Post; never keep entities longer than a tick
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> HEALTH_BEFORE.clear());
    }

    private static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!event.getEntity().level().isClientSide()) {
            HEALTH_BEFORE.put(event.getEntity(), event.getEntity().getHealth());
        }
    }

    private static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        Float healthBefore = HEALTH_BEFORE.remove(target);
        if (target.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || target == player || target.getType().is(NO_MANA_LEECH)) {
            return;
        }
        float damage = event.getNewDamage();
        if (damage <= 0.0F) {
            return;
        }
        if (RunelessServerConfig.LEECH_CAP_AT_TARGET_HEALTH.get() && healthBefore != null) {
            damage = Math.min(damage, Math.max(0.0F, healthBefore));
        }
        SpellHitContext.Hit hit = SpellHitContext.currentHit();
        if (isWeaponHit(source, hit, player)) {
            leech(player, damage, player.getAttributeValue(RunelessAttributes.MANA_LEECH),
                    WEAPON_HITS, RunelessServerConfig.MANA_LEECH_EXTRA_TARGET_PERCENT.get(), ManaChangeEvent.Reason.LEECH);
        } else if (isDirectSpellHit(source, hit, player)) {
            leech(player, damage, player.getAttributeValue(RunelessAttributes.SPELL_MANA_LEECH),
                    SPELL_HITS, RunelessServerConfig.SPELL_MANA_LEECH_EXTRA_TARGET_PERCENT.get(), ManaChangeEvent.Reason.SPELL_LEECH);
        }
    }

    /**
     * Weapon damage: damage type in {@code #runelessrpg:mana_leech/weapon_damage}, not magic; inside a Spell Engine hit
     * only if the player himself cast it and it is neither a cloud nor a summon's projectile (summons like the Fire Hydra
     * deal their damage as the player, the hit context tells them apart).
     */
    static boolean isWeaponHit(DamageSource source, SpellHitContext.Hit hit, ServerPlayer player) {
        if (!source.is(WEAPON_DAMAGE) || source.is(SPELL_DAMAGE)) {
            return false;
        }
        return hit == null || hit.givenCaster() == player && (hit.origin() == ImpactOrigin.DIRECT || hit.origin() == ImpactOrigin.STASH);
    }

    /**
     * Direct spell hit: inside a Spell Engine hit with origin DIRECT, cast by the player himself, an ACTIVE spell and
     * magic damage ({@code #spell_power:all}).
     */
    static boolean isDirectSpellHit(DamageSource source, SpellHitContext.Hit hit, ServerPlayer player) {
        return hit != null && hit.origin() == ImpactOrigin.DIRECT && hit.givenCaster() == player
                && hit.spell().value().type == Spell.Type.ACTIVE && source.is(SPELL_DAMAGE);
    }

    private static void leech(ServerPlayer player, float damage, double percent, Map<UUID, Counter> counters, double extraTargetPercent,
                              ManaChangeEvent.Reason reason) {
        if (percent <= 0.0) {
            return;
        }
        long gameTime = player.level().getGameTime();
        Counter counter = counters.get(player.getUUID());
        int previousHits = counter != null && counter.gameTime() == gameTime ? counter.hits() : 0;
        counters.put(player.getUUID(), new Counter(gameTime, previousHits + 1));
        double share = previousHits == 0 ? 1.0 : extraTargetPercent / 100.0;
        double amount = damage * percent * share;
        if (amount > 0.0) {
            ManaManager.restoreMana(player, amount, reason);
        }
    }
}
