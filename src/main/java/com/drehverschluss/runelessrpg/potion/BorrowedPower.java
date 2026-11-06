package com.drehverschluss.runelessrpg.potion;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Borrowed Power: the debt limit while it is active, and the backlash when it ends in debt.
 * <p>
 * The backlash is triggered by the effect running out ({@link MobEffectEvent.Expired}) or being removed
 * ({@link MobEffectEvent.Remove}: milk, {@code /effect clear}, totem). Renewing the effect (drinking again) fires
 * neither. The damage is dealt at the end of the server tick, not inside the event: both events fire while the entity
 * iterates its effects, and damage could change them.
 */
public final class BorrowedPower {
    private static final Set<UUID> PENDING_BACKLASH = new LinkedHashSet<>();

    private BorrowedPower() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, MobEffectEvent.Expired.class,
                event -> onEnded(event.getEntity(), event.getEffectInstance() != null ? event.getEffectInstance().getEffect() : null));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, MobEffectEvent.Remove.class, event -> onEnded(event.getEntity(), event.getEffect()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> applyBacklash(event.getServer()));
    }

    public static boolean isActive(Player player) {
        return player.hasEffect(RunelessPotions.BORROWED_POWER);
    }

    /** Debt limit of Borrowed Power as a positive amount of mana (0 without the effect). Same on client and server. */
    public static double debtLimit(Player player, CostSettings settings) {
        if (!isActive(player)) {
            return 0.0;
        }
        return settings.borrowedPowerDebtPercent() / 100.0 * ManaManager.getMaxMana(player);
    }

    private static void onEnded(LivingEntity entity, Holder<MobEffect> effect) {
        if (effect == null || effect.value() != RunelessPotions.BORROWED_POWER.get() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isDeadOrDying() && !player.isRemoved()) {
            PENDING_BACKLASH.add(player.getUUID());
        }
    }

    private static void applyBacklash(MinecraftServer server) {
        if (PENDING_BACKLASH.isEmpty()) {
            return;
        }
        for (UUID uuid : PENDING_BACKLASH) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null || player.isDeadOrDying() || isActive(player)) {
                continue;
            }
            double damage = backlashDamage(player);
            if (damage > 0.0) {
                DamageSource source = new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(RunelessPotions.MANA_BACKLASH));
                player.hurt(source, (float) damage);
            }
        }
        PENDING_BACKLASH.clear();
    }

    /** Debt in % of max mana ÷ 10 × {@code backlashDamagePer10Percent}, at most {@code backlashMaxDamage}; 0 without debt. */
    public static double backlashDamage(ServerPlayer player) {
        double mana = ManaManager.getMana(player);
        double max = ManaManager.getMaxMana(player);
        if (mana >= 0.0 || max <= 0.0) {
            return 0.0;
        }
        double debtPercent = -mana / max * 100.0;
        double damage = debtPercent / 10.0 * RunelessServerConfig.BACKLASH_DAMAGE_PER_10_PERCENT.get();
        return Math.min(damage, RunelessServerConfig.BACKLASH_MAX_DAMAGE.get());
    }
}
