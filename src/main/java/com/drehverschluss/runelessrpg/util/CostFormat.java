package com.drehverschluss.runelessrpg.util;

import java.util.Locale;

public final class CostFormat {
    private CostFormat() {}

    /** Whole numbers without decimals, otherwise one decimal ("4", "5.3", "0.9"). */
    public static String format(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return String.format(Locale.ROOT, "%d", (long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }
}
