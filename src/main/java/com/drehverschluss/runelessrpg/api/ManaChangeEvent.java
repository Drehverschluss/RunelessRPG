package com.drehverschluss.runelessrpg.api;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Fired on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} on the logical server after a player's mana
 * has actually changed. The event is informational and cannot be cancelled.
 * <p>
 * Changes caused by regeneration are reported in the same steps in which they are sent to the client
 * (at least {@code 0.05} mana, or when the pool becomes full or empty), not every tick. For those events
 * {@link #getOldMana()} is the value of the previous report.
 */
public class ManaChangeEvent extends Event {
    /** Why the mana of a player changed. */
    public enum Reason {
        /** Regeneration from {@code runelessrpg:mana_regen} (may be negative). */
        REGEN,
        /** The {@code /runelessrpg mana} command. */
        COMMAND,
        /** Another mod through {@link RunelessApi}. */
        API,
        /** Refill after the player respawned. */
        RESPAWN,
        /** Lowered to a smaller maximum, e.g. after removing gear with max mana. */
        CLAMP,
        /** Paid for a spell (also every tick of a channeled spell). May go below 0 in the debt modes. */
        SPELL,
        /**
         * Weapon mana leech ({@code runelessrpg:mana_leech}): a part of the weapon damage dealt came back as mana.
         * Never above max mana; pays back debt.
         */
        LEECH,
        /**
         * Spell mana leech ({@code runelessrpg:spell_mana_leech}): a part of the damage of a direct spell hit came back
         * as mana. Never above max mana; pays back debt.
         */
        SPELL_LEECH
    }

    private final Player player;
    private final double oldMana;
    private final double newMana;
    private final Reason reason;

    public ManaChangeEvent(Player player, double oldMana, double newMana, Reason reason) {
        this.player = player;
        this.oldMana = oldMana;
        this.newMana = newMana;
        this.reason = reason;
    }

    /** @return the player whose mana changed */
    public Player getPlayer() {
        return player;
    }

    /** @return the mana before the change */
    public double getOldMana() {
        return oldMana;
    }

    /** @return the mana after the change; clamped to {@code 0 … max_mana}, except spells in a debt mode (may be negative) */
    public double getNewMana() {
        return newMana;
    }

    /** @return the cause of the change */
    public Reason getReason() {
        return reason;
    }
}
