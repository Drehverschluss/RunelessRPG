package com.drehverschluss.runelessrpg.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CostFormat {
    private CostFormat() {}

    /** At most two decimals without trailing zeros ("4", "7.5", "5.25", "0.9", "0.13"). */
    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return String.valueOf(value);
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        if (rounded.signum() == 0) {
            return "0";
        }
        return rounded.toPlainString();
    }
}
