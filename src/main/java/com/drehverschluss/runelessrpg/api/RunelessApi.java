package com.drehverschluss.runelessrpg.api;

import com.drehverschluss.runelessrpg.cost.ManaCosts;
import com.drehverschluss.runelessrpg.cost.SpellCostInfo;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;

import java.util.OptionalDouble;

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

    /** @return the current mana of the player (may be negative after casting in a debt mode) */
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

    /** @return the cost category of the spell ({@link ManaCategory#NONE} = not handled, Spell Engine costs apply) */
    public static ManaCategory getCategory(Player player, Holder<Spell> spell) {
        return ManaCosts.resolve(player.level(), spell).category();
    }

    /**
     * @return the base cost (tier table × category multiplier, or the datapack value) before player attributes,
     *         or empty if the spell does not cost mana ({@link ManaCategory#NONE} or {@link ManaCategory#FREE})
     */
    public static OptionalDouble getBaseCost(Player player, Holder<Spell> spell) {
        SpellCostInfo info = ManaCosts.resolve(player.level(), spell);
        return info.costsMana() ? OptionalDouble.of(info.baseCost()) : OptionalDouble.empty();
    }

    /**
     * Current total cost for this player: base cost × max(lower limit, {@code mana_cost}) × (1 + {@code overcharge}),
     * after {@link ManaCostEvent}. For channeled spells this is the cost of the full channel duration.
     * Creative players without costs still get the regular value here.
     *
     * @return the cost, or empty if the spell does not cost mana
     */
    public static OptionalDouble getCost(Player player, Holder<Spell> spell) {
        SpellCostInfo info = ManaCosts.resolve(player.level(), spell);
        return info.costsMana() ? OptionalDouble.of(ManaCosts.cost(player, spell, info)) : OptionalDouble.empty();
    }

    /**
     * Damage / healing multiplier from {@code runelessrpg:overcharge} for this spell cast by this player right now:
     * {@code 1 + overcharge × overchargeDamageFactor} (server config), only for spells that cost the player mana.
     * Healing only gets it with {@code overchargeAffectsHealing}. Summons and spell clouds keep the value from the
     * moment they were created instead.
     *
     * @return the multiplier, {@code 1.0} = no bonus
     */
    public static double getOverchargeBonus(Player player, Holder<Spell> spell) {
        return ManaCosts.overchargeBonus(player, spell);
    }

    /** @return true if the spell is channeled (its cost is spread over the channel ticks in the DRAIN mode) */
    public static boolean isChanneled(Holder<Spell> spell) {
        return net.spell_engine.internals.SpellParameters.isChanneled(spell.value());
    }

    /**
     * @return the cost of one channel tick ({@link #getCost} ÷ channel ticks), or empty if the spell is not channeled
     *         or does not cost mana
     */
    public static OptionalDouble getCostPerChannelTick(Player player, Holder<Spell> spell) {
        SpellCostInfo info = ManaCosts.resolve(player.level(), spell);
        if (!info.costsMana() || !info.channeled()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(ManaCosts.costPerChannelTick(player, spell, ManaCosts.cost(player, spell, info)));
    }
}
