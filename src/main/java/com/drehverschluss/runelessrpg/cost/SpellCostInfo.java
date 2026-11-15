package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.api.ManaCategory;
import com.drehverschluss.runelessrpg.util.CostFormat;

/**
 * Player-independent cost data of a spell.
 *
 * @param category    resolved category ({@code NONE} = Spell Engine unchanged)
 * @param source      where the decision came from
 * @param baseCost    base cost before player attributes; 0 for {@code NONE}/{@code FREE}
 * @param runeCost    true if the spell's Spell Engine cost item is a rune
 * @param channeled   true for channeled spells
 * @param tier        the spell's tier
 * @param fixed       true if {@code baseCost} is a fixed value from a data file (no tier calculation)
 * @param tierCost    tier cost from {@code tierCosts} (0 for fixed values)
 * @param factor      factor on the tier cost from a data file (1 without one)
 * @param multiplier  multiplier of the category (0 for fixed values)
 */
public record SpellCostInfo(ManaCategory category, Source source, double baseCost, boolean runeCost, boolean channeled, int tier,
                            boolean fixed, double tierCost, double factor, double multiplier) {
    public enum Source {
        /** Category from a cost tag */
        TAG,
        /** Category from the rune cost item */
        RUNE,
        /** Fixed cost or factor from a datapack */
        DATAPACK,
        /** Fixed cost or factor from RunelessRPG's own data ({@code rpg_series.json}) */
        BUILTIN,
        FREE,
        NONE
    }

    /** Spell without a mana cost (category NONE or FREE). */
    static SpellCostInfo noCost(ManaCategory category, Source source, boolean runeCost, boolean channeled, int tier) {
        return new SpellCostInfo(category, source, 0.0, runeCost, channeled, tier, false, 0.0, 1.0, 0.0);
    }

    /** True if RunelessRPG handles the spell's cost (also for free spells, whose runes are never consumed). */
    public boolean handled() {
        return category != ManaCategory.NONE;
    }

    /** True if the spell costs mana (possibly 0 from a datapack). */
    public boolean costsMana() {
        return category != ManaCategory.NONE && category != ManaCategory.FREE;
    }

    /** The calculation of the base cost, e.g. {@code "5 × 0.7 = 3.5"}, or just the fixed value. */
    public String calculation() {
        if (fixed || (factor == 1.0 && multiplier == 1.0)) {
            return CostFormat.format(baseCost);
        }
        StringBuilder text = new StringBuilder(CostFormat.format(tierCost));
        if (factor != 1.0) {
            text.append(" × ").append(CostFormat.format(factor));
        }
        if (multiplier != 1.0) {
            text.append(" × ").append(CostFormat.format(multiplier));
        }
        return text.append(" = ").append(CostFormat.format(baseCost)).toString();
    }
}
