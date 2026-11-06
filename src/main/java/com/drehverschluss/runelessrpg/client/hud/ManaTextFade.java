package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.client.ClientManaState;
import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

/**
 * Visibility of the number for {@link TextVisibility#ON_CHANGE}: shown for 2 s after mana was spent, after
 * "Missing Mana" and while channeling, then faded out within 0.5 s.
 */
public final class ManaTextFade {
    private static final long SHOW_NANOS = 2_000_000_000L;
    private static final long FADE_NANOS = 500_000_000L;

    private static long lastEvent = Long.MIN_VALUE;
    private static double lastMana = Double.NaN;

    private ManaTextFade() {}

    /** Shows the number again (spent mana, "Missing Mana", channeling). */
    public static void ping() {
        lastEvent = System.nanoTime();
    }

    /** Called every client tick: detects spent mana and running channels. */
    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastMana = Double.NaN;
            return;
        }
        double mana = ClientManaState.getMana();
        if (!Double.isNaN(lastMana) && mana < lastMana) {
            ping();
        }
        lastMana = mana;
        if (isChanneling(player)) {
            ping();
        }
    }

    private static boolean isChanneling(LocalPlayer player) {
        SpellCast.Process process = ((SpellCaster.Player) player).getInteractor().process();
        if (process == null || process.spell() == null) {
            return false;
        }
        Spell spell = process.spell().value();
        return spell.active != null && SpellParameters.isChanneled(spell);
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
