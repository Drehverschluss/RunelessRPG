package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;

/** Red flash of the mana bar: two short pulses within half a second. */
public final class ManaFlash {
    private static final long DURATION_NANOS = 500_000_000L;
    private static final long MIN_INTERVAL_NANOS = 500_000_000L;

    private static long start = Long.MIN_VALUE;

    private ManaFlash() {}

    /** Starts a flash, unless disabled or one is still running. */
    public static void trigger() {
        if (!RunelessClientConfig.FLASH_ON_INSUFFICIENT_MANA.get()) {
            return;
        }
        long now = System.nanoTime();
        if (start != Long.MIN_VALUE && now - start < MIN_INTERVAL_NANOS) {
            return;
        }
        start = now;
    }

    /** @return 0 … 1, the current strength of the flash */
    public static float intensity() {
        if (start == Long.MIN_VALUE) {
            return 0.0F;
        }
        long elapsed = System.nanoTime() - start;
        if (elapsed < 0 || elapsed >= DURATION_NANOS) {
            return 0.0F;
        }
        // |sin| over a full period gives two pulses
        return (float) Math.abs(Math.sin(2.0 * Math.PI * elapsed / DURATION_NANOS));
    }
}
