package com.drehverschluss.runelessrpg.potion;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mana Sickness ({@code runelessrpg:mana_sickness}): every mana restoration gives it for {@code manaSicknessSeconds}
 * (or renews it); while it is active a mana restoration gives only {@code manaSicknessRestorePercent} %.
 * <p>
 * The effect is added at the end of the server tick: a tipped arrow applies Mana Restoration while the player's effects
 * are being iterated, adding an effect right there would fail. Until then the player counts as sick already, so two
 * potions in the same tick are handled like two potions one after the other.
 */
public final class ManaSickness {
    private static final Set<UUID> PENDING = new LinkedHashSet<>();

    private ManaSickness() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> applyPending(event.getServer()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> PENDING.remove(event.getEntity().getUUID()));
    }

    /** Share of a mana restoration the player gets right now (1 without Mana Sickness). */
    public static double restoreFactor(ServerPlayer player) {
        if (RunelessServerConfig.MANA_SICKNESS_SECONDS.get() <= 0) {
            return 1.0;
        }
        if (player.hasEffect(RunelessPotions.MANA_SICKNESS) || PENDING.contains(player.getUUID())) {
            return RunelessServerConfig.MANA_SICKNESS_RESTORE_PERCENT.get() / 100.0;
        }
        return 1.0;
    }

    /** Gives (or renews) Mana Sickness at the end of this tick. */
    public static void trigger(ServerPlayer player) {
        if (RunelessServerConfig.MANA_SICKNESS_SECONDS.get() > 0) {
            PENDING.add(player.getUUID());
        }
    }

    private static void applyPending(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        int ticks = RunelessServerConfig.MANA_SICKNESS_SECONDS.get() * 20;
        for (UUID uuid : PENDING) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (ticks > 0 && player != null && player.isAlive()) {
                // No particles, icon only
                player.addEffect(new MobEffectInstance(RunelessPotions.MANA_SICKNESS, ticks, 0, false, false, true));
            }
        }
        PENDING.clear();
    }

    /** The effect itself: no attribute, only a marker. Milk and totems do not cure it (death and /effect clear do). */
    static final class Effect extends MobEffect {
        Effect(int color) {
            super(MobEffectCategory.HARMFUL, color);
        }

        @Override
        public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
            // Intentionally empty: drinking milk must not skip the potion cooldown
        }
    }
}
