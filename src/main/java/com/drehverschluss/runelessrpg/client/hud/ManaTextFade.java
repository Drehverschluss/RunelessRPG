package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Visibility of the number for {@link TextVisibility#ON_CHANGE}: shown for 2 s after a notable mana change, then faded
 * out within 0.5 s. Notable changes come from the server ({@code ManaSyncPayload.notable}: spell costs except DRAIN
 * channel ticks, potions, leech, commands, respawn), plus "Missing Mana" and a change of max mana seen on the client.
 * Regeneration (also negative) and DRAIN ticks never keep the number visible.
 */
public final class ManaTextFade {
    private static final long SHOW_NANOS = 2_000_000_000L;
    private static final long FADE_NANOS = 500_000_000L;

    private static long lastEvent = Long.MIN_VALUE;
    private static double lastMax = Double.NaN;

    private ManaTextFade() {}

    /** Shows the number again. */
    public static void ping() {
        lastEvent = System.nanoTime();
    }

    /** Called every client tick: detects a changed max mana. */
    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastMax = Double.NaN;
            return;
        }
        double max = ManaBar.maxMana(player);
        if (!Double.isNaN(lastMax) && max != lastMax) {
            ping();
        }
        lastMax = max;
    }

    /** @return 0 … 1, the opacity of the number right now */
    public static float alpha() {
        if (RunelessClientConfig.TEXT_VISIBILITY.get() == TextVisibility.ALWAYS) {
            return 1.0F;
        }
        if (lastEvent == Long.MIN_VALUE) {
            return 0.0F;
        }
        long elapsed = System.nanoTime() - lastEvent;
        if (elapsed < SHOW_NANOS) {
            return 1.0F;
        }
        if (elapsed < SHOW_NANOS + FADE_NANOS) {
            return 1.0F - (elapsed - SHOW_NANOS) / (float) FADE_NANOS;
        }
        return 0.0F;
    }
}
