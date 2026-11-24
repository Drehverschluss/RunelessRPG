package com.drehverschluss.runelessrpg.api;

import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Where a player's mana pool is kept. RunelessRPG always reads and writes mana through exactly one active storage
 * ({@link RunelessApi#setStorage(ManaStorage)}); without a call it uses its own built-in storage (a saved value on the
 * player). Another mana system can replace it, so that RunelessRPG (spell costs, potions, orbs, leech, the HUD) and that
 * system share one pool.
 * <p>
 * <b>Server only.</b> All amounts are in RunelessRPG mana; a storage that keeps its value in another unit converts
 * itself. RunelessRPG asks the storage every server tick for the current value and its maximum, so both calls should
 * be cheap. The client never sees the storage: RunelessRPG sends it the mana and the maximum.
 * <p>
 * <b>Debt:</b> a storage that cannot go below 0 ({@link #canGoNegative()} = {@code false}) gets a debt account kept by
 * RunelessRPG. The mana RunelessRPG reports is then {@code storage − debt}; while there is debt the storage stays at 0
 * and everything that comes into it pays the debt first.
 *
 * @apiNote Experimental since 0.4.0: this interface may still change in later versions.
 */
@ApiStatus.Experimental
public interface ManaStorage {
    /** @return an id for logs and comparisons, e.g. {@code mymod:mana} */
    ResourceLocation id();

    /** @return the mana of the player in the storage, in RunelessRPG mana */
    double getMana(ServerPlayer player);

    /** @return the maximum of the storage for this player, in RunelessRPG mana (never negative) */
    double getMaxMana(ServerPlayer player);

    /**
     * Sets the mana of the player. RunelessRPG only passes values from {@code 0} to {@link #getMaxMana}, or below 0 if
     * {@link #canGoNegative()} is {@code true}. The storage may clamp or round the value; RunelessRPG reads it back.
     */
    void setMana(ServerPlayer player, double mana);

    /**
     * @return {@code true} if the storage can hold negative values itself (the built-in storage can); {@code false}
     *         lets RunelessRPG keep the debt in its own account
     */
    default boolean canGoNegative() {
        return false;
    }

    /**
     * @return {@code true} if the storage has its own regeneration. RunelessRPG then only adds
     *         {@link #bonusRegenPerSecond(ServerPlayer)} every tick instead of the whole {@code runelessrpg:mana_regen}.
     */
    default boolean regeneratesItself() {
        return true;
    }

    /**
     * @return {@code true} if the storage itself decides what happens on login, respawn and dimension change. RunelessRPG
     *         then neither fills new players, nor refills after a respawn, nor lowers the mana to the maximum. It still
     *         sets the base values of its attributes and clears the debt account on death.
     */
    default boolean handlesLifecycle() {
        return true;
    }

    /**
     * Mana per second RunelessRPG adds every tick if {@link #regeneratesItself()} is {@code true}. Default: the part of
     * {@code runelessrpg:mana_regen} above its base value (enchantments, affixes, modifiers of other mods). As with the
     * built-in regeneration, a negative value never takes the mana below 0 or deepens a debt.
     */
    default double bonusRegenPerSecond(ServerPlayer player) {
        return ManaManager.regenAboveBase(player);
    }
}
