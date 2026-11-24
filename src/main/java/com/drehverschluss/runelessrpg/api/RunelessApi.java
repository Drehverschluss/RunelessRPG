package com.drehverschluss.runelessrpg.api;

import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.cost.ManaCastHandler;
import com.drehverschluss.runelessrpg.cost.ManaCosts;
import com.drehverschluss.runelessrpg.cost.SpellCostInfo;
import com.drehverschluss.runelessrpg.impact.ForeignSpellHits;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import com.drehverschluss.runelessrpg.util.DisplayUnit;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;
import java.util.Set;

/**
 * Public API of RunelessRPG for other mods and scripts.
 * <p>
 * Reading works on both sides: on the client the values of the local player are the ones last synchronized
 * by the server. Changing mana is only possible on the logical server; every change is clamped to
 * {@code 0 … max_mana}, synchronized to the client and reported with a {@link ManaChangeEvent}
 * ({@link ManaChangeEvent.Reason#API}).
 * <p>
 * The section "Other mana systems" at the end (since 0.4.0) is experimental and may still change in later versions.
 */
public final class RunelessApi {
    private RunelessApi() {}

    /** @return the current mana of the player (may be negative after casting in a debt mode) */
    public static double getMana(Player player) {
        return ManaManager.getMana(player);
    }

    /**
     * @return the max mana of the player (never negative): the value of {@code runelessrpg:max_mana}, or the maximum of
     *         another mod's {@link ManaStorage} if one is active
     */
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

    // ================================================================== other mana systems (experimental, since 0.4.0)

    /**
     * Makes {@code storage} the mana storage of all players (server only). Exactly one storage is active; without a call
     * it is RunelessRPG's built-in one. Online players keep their mana and debt: RunelessRPG reads the value from the old
     * storage and writes it into the new one (at most its maximum; a debt goes into the new storage or RunelessRPG's
     * debt account). Changes of the value are reported as {@link ManaChangeEvent.Reason#EXTERNAL}. Can be called again
     * at any time, e.g. after a config reload; from another thread it is run on the server thread.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void setStorage(ManaStorage storage) {
        ManaManager.setStorage(storage);
    }

    /**
     * Goes back to RunelessRPG's built-in storage, with the same transfer as {@link #setStorage}.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void resetStorage() {
        ManaManager.setStorage(null);
    }

    /**
     * @return the active storage (server side; the built-in one has the id {@code runelessrpg:builtin})
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static ManaStorage getStorage() {
        return ManaManager.storage();
    }

    /**
     * @return the debt account of the player in RunelessRPG mana (positive; 0 without debt). Only used for a storage that
     *         cannot go below 0; with the built-in storage the debt is the negative mana itself.
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static double getDebt(ServerPlayer player) {
        return ManaManager.debt(player);
    }

    /**
     * Cost multiplier for another mod's spell: {@code max(lower limit, runelessrpg:mana_cost)}, where Spell Infinity on
     * the main hand item lowers the limit and the cost like for Spell Engine spells. Overcharge is not included. Same
     * value on client and server.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static double getCostMultiplier(Player player) {
        return ManaCosts.manaCostFactor(player);
    }

    /**
     * Whether the player can pay {@code amount} mana for another mod's spell (client and server). With {@code allowDebt}
     * the rules of Spell Engine spells apply: {@code manaMode}, {@code maxDebtPercent} (of {@code amount}), Borrowed Power.
     * Without it the full amount is needed. Creative players can always pay with {@code creativeFreeCasting}.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static boolean canAfford(Player player, double amount, boolean allowDebt) {
        return ManaCosts.canAffordExternal(player, amount, allowDebt);
    }

    /**
     * @return how much mana {@link #canAfford} is missing (0 if the player can pay), e.g. for
     *         {@link #signalInsufficientMana}
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static double getMissingMana(Player player, double amount, boolean allowDebt) {
        return ManaCosts.shortfallExternal(player, amount, allowDebt);
    }

    /**
     * Pays {@code amount} mana for another mod's spell if {@link #canAfford} allows it (server only), reason
     * {@link ManaChangeEvent.Reason#EXTERNAL}. With {@code allowDebt} the mana may go below 0 within the same rules.
     * Creative players with {@code creativeFreeCasting} pay nothing.
     *
     * @return {@code true} if paid (or free), {@code false} if the player could not pay; nothing is changed then
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static boolean spend(ServerPlayer player, double amount, boolean allowDebt) {
        if (!ManaCosts.canAffordExternal(player, amount, allowDebt)) {
            return false;
        }
        if (amount > 0.0 && !ManaCosts.isCreativeFree(player)) {
            boolean debt = allowDebt && ManaCosts.allowsDebt(player, CostSettings.server());
            ManaManager.spendForSpell(player, amount, debt, true, ManaChangeEvent.Reason.EXTERNAL);
        }
        return true;
    }

    /**
     * Gives mana (server only): pays debt first and never goes above max mana; does nothing at or above max mana.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void giveMana(ServerPlayer player, double amount, ManaChangeEvent.Reason reason) {
        ManaManager.restoreMana(player, amount, reason);
    }

    /**
     * Takes mana (server only) down to 0 at most; never creates or deepens a debt.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void takeMana(ServerPlayer player, double amount, ManaChangeEvent.Reason reason) {
        ManaManager.drainMana(player, amount, reason);
    }

    /**
     * Modifiers on {@code runelessrpg:max_mana} and {@code runelessrpg:mana_regen} by operation, without the base value
     * (client and server). Modifiers whose id has one of the given namespaces are left out, e.g. a mod's own modifiers.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static ManaBonus getManaBonus(Player player, String... excludedNamespaces) {
        Set<String> excluded = Set.of(excludedNamespaces);
        return new ManaBonus(bonusPart(player, RunelessAttributes.MAX_MANA, excluded), bonusPart(player, RunelessAttributes.MANA_REGEN, excluded));
    }

    private static ManaBonus.Part bonusPart(Player player, Holder<Attribute> attribute, Set<String> excluded) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return ManaBonus.Part.NONE;
        }
        double add = 0.0;
        double base = 0.0;
        double total = 1.0;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (excluded.contains(modifier.id().getNamespace())) {
                continue;
            }
            switch (modifier.operation()) {
                case ADD_VALUE -> add += modifier.amount();
                case ADD_MULTIPLIED_BASE -> base += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> total *= 1.0 + modifier.amount();
            }
        }
        return new ManaBonus.Part(add, base, total);
    }

    /**
     * Registers a check that recognizes direct hits of another mod's spells, so Spell Mana Leech applies to them with
     * all its rules ({@link SpellHitPredicate}). Asked only when the hit is not a Spell Engine hit.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void registerSpellHitPredicate(SpellHitPredicate predicate) {
        ForeignSpellHits.register(predicate);
    }

    /**
     * Removes a check added with {@link #registerSpellHitPredicate}.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void unregisterSpellHitPredicate(SpellHitPredicate predicate) {
        ForeignSpellHits.unregister(predicate);
    }

    /**
     * Shows "not enough mana" to the player (server only): the red flash of RunelessRPG's bar and
     * {@link ManaInsufficientEvent} on the client, without Spell Engine's message. Call it once per failed attempt.
     *
     * @param missing how much mana was missing, in RunelessRPG mana
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void signalInsufficientMana(ServerPlayer player, double missing) {
        ManaCastHandler.signalInsufficientMana(player, missing);
    }

    /**
     * Sets the display factor (client only): display units per RunelessRPG mana, default 1. It multiplies the number at
     * the bar and in its hover tooltip, the mana line of spell tooltips and flat values of {@code max_mana} and
     * {@code mana_regen} in attribute tooltips (also in mods that use the attribute's own text, e.g. Gearscope). Percent
     * values, potion texts, commands and the cost report stay in RunelessRPG mana. Values not above 0 reset it to 1.
     *
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static void setDisplayFactor(double factor) {
        DisplayUnit.setFactor(factor);
    }

    /**
     * @return the display factor (client only, default 1)
     * @apiNote Experimental: may still change in later versions.
     */
    @ApiStatus.Experimental
    public static double getDisplayFactor() {
        return DisplayUnit.factor();
    }
}
