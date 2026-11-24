package com.drehverschluss.runelessrpg.mana;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.api.ManaStorage;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.client.ClientManaState;
import com.drehverschluss.runelessrpg.network.ManaSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * Central access to a player's mana. All writes are server-side, clamped to {@code 0 … max_mana} (except spells in a
 * debt mode) and synchronized to the owning client.
 * <p>
 * The mana lives in the active {@link ManaStorage}: the built-in one (the {@link ManaAttachments#MANA} attachment) or
 * one set by another mod. For a storage that cannot go below 0, the debt is kept in {@link ManaAttachments#DEBT}; the
 * mana reported everywhere is then {@code storage − debt}.
 */
public final class ManaManager {
    /** Minimum change of regenerated mana before it is sent to the client again. */
    public static final double REGEN_SYNC_STEP = 0.05;

    private static volatile ManaStorage storage = BuiltinManaStorage.INSTANCE;
    private static boolean storageErrorLogged;

    private ManaManager() {}

    // ------------------------------------------------------------------ storage

    public static ManaStorage storage() {
        return storage;
    }

    /** True if another mod's storage is active. */
    public static boolean isForeignStorage() {
        return storage != BuiltinManaStorage.INSTANCE;
    }

    /** True if the active storage handles login, respawn, first fill and the max clamp itself. */
    static boolean storageHandlesLifecycle() {
        ManaStorage active = storage;
        return active != BuiltinManaStorage.INSTANCE && active.handlesLifecycle();
    }

    /**
     * Makes {@code next} the active storage. Online players keep their mana and debt: the value is read from the old
     * storage and written into the new one (at most its maximum; debt goes into the new storage or the debt account).
     * Runs on the server thread (scheduled there if called from another thread while a server is running).
     */
    public static void setStorage(ManaStorage next) {
        ManaStorage target = next == null ? BuiltinManaStorage.INSTANCE : next;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.isSameThread()) {
            server.execute(() -> setStorage(target));
            return;
        }
        ManaStorage old = storage;
        if (old == target) {
            return;
        }
        List<ServerPlayer> players = server != null ? new ArrayList<>(server.getPlayerList().getPlayers()) : List.of();
        double[] values = new double[players.size()];
        for (int i = 0; i < players.size(); i++) {
            values[i] = getMana(players.get(i));
        }
        storage = target;
        RunelessRPG.LOGGER.info("Mana storage changed from {} to {}", old.id(), target.id());
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (old == BuiltinManaStorage.INSTANCE && player.getData(ManaAttachments.MANA) < 0.0) {
                // The debt moves on; the built-in value must not count it a second time if it is used again later
                player.setData(ManaAttachments.MANA, 0.0);
            }
            ManaAttachments.ManaSyncState state = player.getData(ManaAttachments.SYNC_STATE);
            state.lastKnown = Double.NaN;
            double value = Math.min(values[i], Math.max(0.0, target.getMaxMana(player)));
            double stored = store(player, value);
            if (stored != values[i]) {
                NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, values[i], stored, ManaChangeEvent.Reason.EXTERNAL));
            }
            sync(player, false);
        }
    }

    // ------------------------------------------------------------------ reading

    /**
     * Current mana. A player that never had mana (new player) starts full on the first access, whichever mod reads
     * first; players with saved mana (also from older versions) keep their value. With another mod's storage:
     * storage − debt account.
     */
    public static double getMana(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ManaStorage active = storage;
            if (active == BuiltinManaStorage.INSTANCE) {
                return BuiltinManaStorage.INSTANCE.getMana(serverPlayer);
            }
            return active.getMana(serverPlayer) - debt(serverPlayer);
        }
        double mana = player.getData(ManaAttachments.MANA);
        if (Double.isNaN(mana)) {
            return getMaxMana(player);
        }
        return mana;
    }

    /**
     * Max mana: the value of {@code runelessrpg:max_mana}, or the maximum of another mod's storage (on the client the one
     * the server sent for the local player).
     */
    public static double getMaxMana(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ManaStorage active = storage;
            if (active != BuiltinManaStorage.INSTANCE) {
                return Math.max(0.0, active.getMaxMana(serverPlayer));
            }
        } else if (player.isLocalPlayer()) {
            double synced = ClientManaState.getStorageMax();
            if (!Double.isNaN(synced)) {
                return synced;
            }
        }
        return attributeMax(player);
    }

    /** Value of {@code runelessrpg:max_mana} (never negative), regardless of the storage. */
    public static double attributeMax(Player player) {
        AttributeInstance instance = player.getAttribute(RunelessAttributes.MAX_MANA);
        return instance == null ? 0.0 : Math.max(0.0, instance.getValue());
    }

    public static double getManaRegen(Player player) {
        AttributeInstance instance = player.getAttribute(RunelessAttributes.MANA_REGEN);
        return instance == null ? 0.0 : instance.getValue();
    }

    /** Part of {@code runelessrpg:mana_regen} above its base value (may be negative). */
    public static double regenAboveBase(Player player) {
        AttributeInstance instance = player.getAttribute(RunelessAttributes.MANA_REGEN);
        return instance == null ? 0.0 : instance.getValue() - instance.getBaseValue();
    }

    /** Debt account (positive amount); always 0 for a storage that can hold negative values itself. */
    public static double debt(ServerPlayer player) {
        return player.hasData(ManaAttachments.DEBT) ? player.getData(ManaAttachments.DEBT) : 0.0;
    }

    // ------------------------------------------------------------------ writing

    /**
     * Writes a reported mana value into the active storage: below 0 the storage gets 0 and the rest goes into the debt
     * account, unless the storage can hold negative values itself. Returns the value read back.
     */
    private static double store(ServerPlayer player, double value) {
        ManaStorage active = storage;
        if (active == BuiltinManaStorage.INSTANCE) {
            player.setData(ManaAttachments.MANA, value);
            if (player.hasData(ManaAttachments.DEBT)) {
                player.removeData(ManaAttachments.DEBT);
            }
            return value;
        }
        if (active.canGoNegative() || value >= 0.0) {
            active.setMana(player, value);
            if (player.hasData(ManaAttachments.DEBT)) {
                player.removeData(ManaAttachments.DEBT);
            }
        } else {
            active.setMana(player, 0.0);
            player.setData(ManaAttachments.DEBT, -value);
        }
        double stored = getMana(player);
        player.getData(ManaAttachments.SYNC_STATE).lastKnown = stored;
        return stored;
    }

    public static void setMana(ServerPlayer player, double value, ManaChangeEvent.Reason reason) {
        double old = getMana(player);
        double clamped = clamp(value, getMaxMana(player));
        if (clamped == old) {
            return;
        }
        double stored = store(player, clamped);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, stored, reason));
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
     * Gives mana back (potion, orb, leech, API): never above max mana, and in debt it pays the debt back (the result may
     * still be negative). Does nothing if the player is already at or above max mana.
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
        double stored = store(player, next);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, stored, reason));
        sync(player, true);
    }

    /**
     * Removes mana down to 0 at most, never into debt; does nothing while the player is already at 0 or in debt.
     */
    public static void drainMana(ServerPlayer player, double amount, ManaChangeEvent.Reason reason) {
        if (amount <= 0.0) {
            return;
        }
        double old = getMana(player);
        if (old <= 0.0) {
            return;
        }
        double next = Math.max(0.0, Math.min(old - amount, getMaxMana(player)));
        if (next == old) {
            return;
        }
        double stored = store(player, next);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, stored, reason));
        sync(player, true);
    }

    /**
     * Removes mana for a spell. With {@code allowDebt} the result may become negative (DEBT modes), otherwise it stops at 0.
     */
    public static void spendForSpell(ServerPlayer player, double amount, boolean allowDebt) {
        spendForSpell(player, amount, allowDebt, true, ManaChangeEvent.Reason.SPELL);
    }

    /** {@code notable} = false for DRAIN channel ticks (they do not show the number for {@code ON_CHANGE}). */
    public static void spendForSpell(ServerPlayer player, double amount, boolean allowDebt, boolean notable) {
        spendForSpell(player, amount, allowDebt, notable, ManaChangeEvent.Reason.SPELL);
    }

    /** Like {@link #spendForSpell(ServerPlayer, double, boolean, boolean)} with a chosen reason. */
    public static void spendForSpell(ServerPlayer player, double amount, boolean allowDebt, boolean notable, ManaChangeEvent.Reason reason) {
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
        double stored = store(player, next);
        NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, old, stored, reason));
        sync(player, notable);
    }

    // ------------------------------------------------------------------ lifecycle

    /** Fills a player that has never stored mana before (built-in storage only). Returns true if the player was new. */
    static boolean initializeIfMissing(ServerPlayer player) {
        if (!Double.isNaN(player.getData(ManaAttachments.MANA))) {
            return false;
        }
        initialize(player);
        return true;
    }

    // Base values first: another mod may read before our login handler has applied the configured max mana
    static double initialize(ServerPlayer player) {
        ManaEvents.applyBaseValues(player);
        double max = attributeMax(player);
        player.setData(ManaAttachments.MANA, max);
        return max;
    }

    /** A storage that can hold negative values takes over a debt left in the account (e.g. after a storage change offline). */
    static void foldDebt(ServerPlayer player) {
        double debt = debt(player);
        ManaStorage active = storage;
        if (debt > 0.0 && (active == BuiltinManaStorage.INSTANCE || active.canGoNegative())) {
            double mana = active.getMana(player);
            player.removeData(ManaAttachments.DEBT);
            active.setMana(player, mana - debt);
        }
    }

    /** Lowers the stored mana to the current maximum, e.g. after armor with max mana was removed. */
    static void clampToMax(ServerPlayer player) {
        if (storageHandlesLifecycle()) {
            return;
        }
        double max = getMaxMana(player);
        if (getMana(player) > max) {
            setMana(player, max, ManaChangeEvent.Reason.CLAMP);
        }
    }

    /**
     * Another mod's storage, every server tick: pays the debt account from what came into the storage, then reports
     * changes RunelessRPG did not make ({@link ManaChangeEvent.Reason#EXTERNAL}, in the same steps as regeneration) and
     * sends them and a changed maximum to the client.
     */
    static void tickStorage(ServerPlayer player) {
        ManaStorage active = storage;
        if (active == BuiltinManaStorage.INSTANCE) {
            return;
        }
        try {
            if (!active.canGoNegative()) {
                double debt = debt(player);
                if (debt > 0.0) {
                    double inStorage = active.getMana(player);
                    if (inStorage > 0.0) {
                        double paid = Math.min(inStorage, debt);
                        active.setMana(player, inStorage - paid);
                        double left = debt - paid;
                        if (left > 1.0E-9) {
                            player.setData(ManaAttachments.DEBT, left);
                        } else {
                            player.removeData(ManaAttachments.DEBT);
                        }
                    }
                }
            }
            ManaAttachments.ManaSyncState state = player.getData(ManaAttachments.SYNC_STATE);
            double mana = getMana(player);
            double max = getMaxMana(player);
            if (Double.isNaN(state.lastKnown)) {
                state.lastKnown = mana;
                sync(player, false);
                return;
            }
            if (mana != state.lastKnown) {
                double previous = state.lastKnown;
                state.lastKnown = mana;
                double last = state.lastSynced;
                boolean boundary = mana == max || mana == 0.0;
                if (Double.isNaN(last) || Math.abs(mana - last) >= REGEN_SYNC_STEP || (boundary && mana != last)) {
                    NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, Double.isNaN(last) ? previous : last, mana, ManaChangeEvent.Reason.EXTERNAL));
                    sync(player, false);
                    return;
                }
            }
            if (Double.compare(max, state.lastSyncedMax) != 0) {
                sync(player, false);
            }
        } catch (RuntimeException e) {
            if (!storageErrorLogged) {
                storageErrorLogged = true;
                RunelessRPG.LOGGER.error("Mana storage {} failed", active.id(), e);
            }
        }
    }

    /**
     * Regeneration: the whole {@code runelessrpg:mana_regen}, or only {@link ManaStorage#bonusRegenPerSecond} if another
     * mod's storage regenerates itself.
     */
    static void tickRegeneration(ServerPlayer player) {
        ManaStorage active = storage;
        double perSecond = active != BuiltinManaStorage.INSTANCE && active.regeneratesItself()
                ? active.bonusRegenPerSecond(player) : getManaRegen(player);
        double regenPerTick = perSecond / 20.0;
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
        next = store(player, next);

        // Regeneration only syncs (and reports) in steps, or when it reaches full/empty.
        ManaAttachments.ManaSyncState state = player.getData(ManaAttachments.SYNC_STATE);
        double last = state.lastSynced;
        boolean boundary = next == max || next == 0.0;
        if (Double.isNaN(last) || Math.abs(next - last) >= REGEN_SYNC_STEP || (boundary && next != last)) {
            NeoForge.EVENT_BUS.post(new ManaChangeEvent(player, Double.isNaN(last) ? current : last, next, ManaChangeEvent.Reason.REGEN));
            sync(player, false);
        }
    }

    // ------------------------------------------------------------------ sync

    /** Sends the current mana without marking it as a visible change (login, dimension change, respawn resync). */
    public static void sync(ServerPlayer player) {
        sync(player, false);
    }

    private static void sync(ServerPlayer player, boolean notable) {
        double mana = getMana(player);
        // The maximum only goes along for another mod's storage; otherwise the client uses the synced attribute
        double max = isForeignStorage() ? getMaxMana(player) : Double.NaN;
        ManaAttachments.ManaSyncState state = player.getData(ManaAttachments.SYNC_STATE);
        state.lastSynced = mana;
        state.lastSyncedMax = max;
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(mana, notable, max));
    }

    private static double clamp(double value, double max) {
        return Math.max(0.0, Math.min(value, max));
    }
}
