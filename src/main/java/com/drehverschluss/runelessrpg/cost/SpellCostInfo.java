package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.api.ManaCategory;

/**
 * Player-independent cost data of a spell.
 *
 * @param category    resolved category ({@code NONE} = Spell Engine unchanged)
 * @param source      where the decision came from
 * @param baseCost    base cost before player attributes; 0 for {@code NONE}/{@code FREE}
 * @param runeCost    true if the spell's Spell Engine cost item is a rune
 * @param channeled   true for channeled spells
 * @param tier        the spell's tier
 */
public record SpellCostInfo(ManaCategory category, Source source, double baseCost, boolean runeCost, boolean channeled, int tier) {
    public enum Source {
        TAG, RUNE, DATAPACK, FREE, NONE
    }

    /** True if RunelessRPG handles the spell's cost (also for free spells, whose runes are never consumed). */
    public boolean handled() {
        return category != ManaCategory.NONE;
    }

    /** True if the spell costs mana (possibly 0 from a datapack). */
    public boolean costsMana() {
        return category != ManaCategory.NONE && category != ManaCategory.FREE;
    }
}
