package com.drehverschluss.runelessrpg.util;

/**
 * Display factor of mana amounts on the client (display units per RunelessRPG mana, 1 = RunelessRPG mana): the number
 * at the bar and its hover tooltip, the mana line of spell tooltips and flat values of {@code max_mana} and
 * {@code mana_regen} in attribute tooltips. Percent values, potion texts, commands and the cost report stay in
 * RunelessRPG mana. Plain class without client imports (read by the attribute classes).
 */
public final class DisplayUnit {
    private static volatile double factor = 1.0;

    private DisplayUnit() {}

    public static double factor() {
        return factor;
    }

    /** Sets the factor; values that are not finite or not above 0 reset it to 1. */
    public static void setFactor(double value) {
        factor = Double.isFinite(value) && value > 0.0 ? value : 1.0;
    }
}
