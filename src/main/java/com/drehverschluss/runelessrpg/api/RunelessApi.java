package com.drehverschluss.runelessrpg.api;

import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public API of RunelessRPG for other mods and scripts.
 * <p>
 * Reading works on both sides: on the client the values of the local player are the ones last synchronized
 * by the server. Changing mana is only possible on the logical server; every change is clamped to
 * {@code 0 … max_mana}, synchronized to the client and reported with a {@link ManaChangeEvent}
 * ({@link ManaChangeEvent.Reason#API}).
 */
public final class RunelessApi {
    private RunelessApi() {}

    /** @return the current mana of the player */
    public static double getMana(Player player) {
        return ManaManager.getMana(player);
    }

    /** @return the current value of {@code runelessrpg:max_mana} (never negative) */
    public static double getMaxMana(Player player) {
        return ManaManager.getMaxMana(player);
    }

    /** @return the current value of {@code runelessrpg:mana_regen} in mana per second (may be negative) */
    public static double getManaRegen(Player player) {
        return ManaManager.getManaRegen(player);
    }

    /** Sets the mana of the player, clamped to {@code 0 … max_mana}. */
    public static void setMana(ServerPlayer player, double mana) {
        ManaManager.setMana(player, mana, ManaChangeEvent.Reason.API);
    }

    /** Adds (or with a negative amount removes) mana, clamped to {@code 0 … max_mana}. */
    public static void addMana(ServerPlayer player, double amount) {
        ManaManager.addMana(player, amount, ManaChangeEvent.Reason.API);
    }

    /**
     * Removes {@code amount} mana if the player has at least that much.
     *
     * @return {@code true} if the mana was spent (always {@code true} for amounts {@code <= 0}),
     *         {@code false} if the player had too little mana; nothing is changed in that case
     */
    public static boolean trySpend(ServerPlayer player, double amount) {
        return ManaManager.trySpend(player, amount, ManaChangeEvent.Reason.API);
    }
}
