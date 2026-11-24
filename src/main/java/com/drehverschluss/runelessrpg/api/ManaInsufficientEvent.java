package com.drehverschluss.runelessrpg.api;

import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} on the <b>client only</b>, every time RunelessRPG
 * signals "not enough mana" (the red flash of its bar): a cast the client itself rejected, a cast or channel the server
 * rejected or ended, or {@link RunelessApi#signalInsufficientMana}. Fired even if the flash is turned off in the client
 * config. A mod that shows its own mana bar can use it to give the same signal.
 *
 * @apiNote Experimental since 0.4.0: this event may still change in later versions.
 */
@ApiStatus.Experimental
public class ManaInsufficientEvent extends Event {
    /** Where the signal came from. */
    public enum Source {
        /** The client's own cast check (Spell Engine shows "Missing Mana"). */
        CLIENT,
        /** The server rejected a cast or ended a channel, or another mod called {@link RunelessApi#signalInsufficientMana}. */
        SERVER
    }

    private final double missingMana;
    private final Source source;

    public ManaInsufficientEvent(double missingMana, Source source) {
        this.missingMana = missingMana;
        this.source = source;
    }

    /**
     * @return how much mana was missing, in RunelessRPG mana (without the display factor). For a spell: the amount of
     *         this step (the cast, or one channel tick) minus the mana and the allowed debt. In the {@code DEBT} mode it is
     *         the debt (the cast needs mana above 0), so it can be 0 with exactly 0 mana.
     */
    public double getMissingMana() {
        return missingMana;
    }

    /** @return whether the client or the server detected it */
    public Source getSource() {
        return source;
    }
}
