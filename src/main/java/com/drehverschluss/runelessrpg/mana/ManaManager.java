package com.drehverschluss.runelessrpg.mana;

import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.network.ManaSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Central access to a player's mana. All writes are server-side, clamped to {@code 0 … max_mana}
 * and synchronized to the owning client.
 */
public final class ManaManager {
    /** Minimum change of regenerated mana before it is sent to the client again. */
    public static final double REGEN_SYNC_STEP = 0.05;

    private ManaManager() {}

    public static double getMana(Player player) {
        return player.getData(ManaAttachments.MANA);
    }

    public static double getMaxMana(Player player) {
        AttributeInstance instance = player.getAttribute(RunelessAttributes.MAX_MANA);
        return instance == null ? 0.0 : Math.max(0.0, instance.getValue());
    }

    public static double getManaRegen(Player player) {
        AttributeInstance instance = player.getAttribute(RunelessAttributes.MANA_REGEN);
        return instance == null ? 0.0 : instance.getValue();
    }

    public static void setMana(ServerPlayer player, double value, ManaChangeEvent.Reason reason) {
        double old = getMana(player);
        double clamped = clamp(value, getMaxMana(player));
        if (clamped == old) {
            return;
        }
        player.setData(ManaAttachments.MANA, clamped);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, clamped, reason));
        sync(player, reason != ManaChangeEvent.Reason.REGEN);
    }

    public static void addMana(ServerPlayer player, double amount, ManaChangeEvent.Reason reason) {
        setMana(player, getMana(player) + amount, reason);
    }

    public static boolean trySpend(ServerPlayer player, double amount, ManaChangeEvent.Reason reason) {
        if (amount <= 0.0) {
            return true;
        }
        double current = getMana(player);
        if (current < amount) {
            return false;
        }
        setMana(player, current - amount, reason);
        return true;
    }

    /**
     * Gives mana back (mana leech): never above max mana, and in debt it pays the debt back (the result may still be
     * negative). Does nothing if the player is already at or above max mana.
     */
    public static void restoreMana(ServerPlayer player, double amount, ManaChangeEvent.Reason reason) {
        if (amount <= 0.0) {
            return;
        }
        double old = getMana(player);
        double max = getMaxMana(player);
        if (old >= max) {
            return;
        }
        double next = Math.min(old + amount, max);
        player.setData(ManaAttachments.MANA, next);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, next, reason));
        sync(player, true);
    }

    /**
     * Removes mana for a spell. With {@code allowDebt} the result may become negative (DEBT modes), otherwise it stops at 0.
     */
    public static void spendForSpell(ServerPlayer player, double amount, boolean allowDebt) {
        spendForSpell(player, amount, allowDebt, true);
    }

    /** {@code notable} = false for DRAIN channel ticks (they do not show the number for {@code ON_CHANGE}). */
    public static void spendForSpell(ServerPlayer player, double amount, boolean allowDebt, boolean notable) {
        double old = getMana(player);
        double next = Math.min(old - amount, getMaxMana(player));
        if (!allowDebt) {
            next = Math.max(0.0, next);
        }
        if (Math.abs(next) < 1.0E-9) {
            next = 0.0; // float residue from fractional channel ticks
        }
        if (next == old) {
            return;
        }
        player.setData(ManaAttachments.MANA, next);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, next, ManaChangeEvent.Reason.SPELL));
        sync(player, notable);
    }

    /** Fills a player that has never stored mana before. Returns true if the player was new. */
    static boolean initializeIfMissing(ServerPlayer player) {
        if (player.hasData(ManaAttachments.MANA)) {
            return false;
        }
        player.setData(ManaAttachments.MANA, getMaxMana(player));
        return true;
    }

    /** Lowers the stored mana to the current maximum, e.g. after armor with max mana was removed. */
    static void clampToMax(ServerPlayer player) {
        double max = getMaxMana(player);
        if (getMana(player) > max) {
            setMana(player, max, ManaChangeEvent.Reason.CLAMP);
        }
    }

    static void tickRegeneration(ServerPlayer player) {
        double regenPerTick = getManaRegen(player) / 20.0;
        if (regenPerTick == 0.0) {
            return;
        }
        double max = getMaxMana(player);
        double current = getMana(player);
        double next;
        if (current < 0.0) {
            // Debt from spells: positive regeneration pays it back, negative regeneration never deepens it.
            next = regenPerTick > 0.0 ? Math.min(current + regenPerTick, max) : current;
        } else {
            next = clamp(current + regenPerTick, max);
        }
        if (next == current) {
            return;
        }
        player.setData(ManaAttachments.MANA, next);

        // Regeneration only syncs (and reports) in steps, or when it reaches full/empty.
        ManaAttachments.ManaSyncState state = player.getData(ManaAttachments.SYNC_STATE);
        double last = state.lastSynced;
        boolean boundary = next == max || next == 0.0;
        if (Double.isNaN(last) || Math.abs(next - last) >= REGEN_SYNC_STEP || (boundary && next != last)) {
            NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, Double.isNaN(last) ? current : last, next, ManaChangeEvent.Reason.REGEN));
            sync(player, false);
        }
    }

    /** Sends the current mana without marking it as a visible change (login, dimension change, respawn resync). */
    public static void sync(ServerPlayer player) {
        sync(player, false);
    }

    private static void sync(ServerPlayer player, boolean notable) {
        double mana = getMana(player);
        player.getData(ManaAttachments.SYNC_STATE).lastSynced = mana;
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(mana, notable));
    }

    private static double clamp(double value, double max) {
        return Math.max(0.0, Math.min(value, max));
    }
}
