package com.drehverschluss.runelessrpg.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Bonuses on {@code runelessrpg:max_mana} and {@code runelessrpg:mana_regen} by operation, without the base value
 * ({@link RunelessApi#getManaBonus}). Flat values are in RunelessRPG mana (per second for regeneration).
 *
 * @param maxMana   modifiers on {@code runelessrpg:max_mana}
 * @param manaRegen modifiers on {@code runelessrpg:mana_regen}
 * @apiNote Experimental since 0.4.0: this record may still change in later versions.
 */
@ApiStatus.Experimental
public record ManaBonus(Part maxMana, Part manaRegen) {
    /**
     * Modifiers of one attribute.
     *
     * @param addValue          sum of all {@code ADD_VALUE} modifiers (flat)
     * @param addMultipliedBase sum of all {@code ADD_MULTIPLIED_BASE} modifiers (0.1 = +10 % of the base value)
     * @param multiplyTotal     product of {@code 1 + amount} of all {@code ADD_MULTIPLIED_TOTAL} modifiers (1 = none)
     */
    public record Part(double addValue, double addMultipliedBase, double multiplyTotal) {
        /** No modifiers. */
        public static final Part NONE = new Part(0.0, 0.0, 1.0);

        /**
         * Applies the bonus to another base value the same way Minecraft does:
         * {@code (base + addValue) × (1 + addMultipliedBase) × multiplyTotal}, minus nothing.
         */
        public double applyTo(double base) {
            return (base + addValue) * (1.0 + addMultipliedBase) * multiplyTotal;
        }
    }
}
