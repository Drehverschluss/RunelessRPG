package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.ChannelCostMode;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import com.drehverschluss.runelessrpg.network.ManaInsufficientPayload;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-side mana deduction for spells. */
public final class ManaCastHandler {
    /** Channels paid in full at their start (START mode): player → spell. */
    private static final Map<UUID, ResourceLocation> START_PAID = new HashMap<>();
    /** Channels to end at the end of the player's tick because the next channel tick is not affordable. */
    private static final Map<UUID, ResourceLocation> PENDING_END = new HashMap<>();
    /** Game time of the last "not enough mana" notification per player (at most one per 10 ticks). */
    private static final Map<UUID, Long> LAST_NOTIFY = new HashMap<>();
    private static final long NOTIFY_INTERVAL = 10;

    private ManaCastHandler() {}

    public static void registerSpellEngineEvents() {
        SpellEvents.COST_CONSUME.register(args -> {
            try {
                if (args.caster() instanceof ServerPlayer player) {
                    onCostConsume(player, args.spell());
                }
            } catch (Throwable t) {
                RunelessRPG.LOGGER.error("Mana cost deduction failed", t);
            }
        });
    }

    /** A finished cast (Spell Engine's single cost event). */
    private static void onCostConsume(ServerPlayer player, Holder<Spell> holder) {
        if (holder.value().type == Spell.Type.PASSIVE) {
            return;
        }
        SpellCostInfo info = ManaCosts.resolve(player.level(), holder);
        if (!info.costsMana() || ManaCosts.isCreativeFree(player)) {
            return;
        }
        CostSettings settings = CostSettings.server();
        if (info.channeled()) {
            ResourceLocation id = id(holder);
            if (settings.channelCostMode() == ChannelCostMode.START) {
                START_PAID.remove(player.getUUID(), id);
                return;
            }
            if (settings.channelCostMode() == ChannelCostMode.DRAIN) {
                return;
            }
        }
        spend(player, ManaCosts.cost(player, holder, info), settings);
    }

    /** A channel tick that passed the check and is being performed. */
    public static void onChannelTick(ServerPlayer player, Holder<Spell> holder) {
        SpellCostInfo info = ManaCosts.resolve(player.level(), holder);
        CostSettings settings = CostSettings.server();
        if (!info.costsMana() || !info.channeled() || ManaCosts.isCreativeFree(player) || settings.channelCostMode() != ChannelCostMode.DRAIN) {
            return;
        }
        double total = ManaCosts.cost(player, holder, info);
        spend(player, ManaCosts.costPerChannelTick(player, holder, total), settings);
    }

    /** A cast with a duration has started on the server (process created). */
    public static void onCastStarted(ServerPlayer player, Holder<Spell> holder) {
        SpellCostInfo info = ManaCosts.resolve(player.level(), holder);
        if (!info.costsMana() || !info.channeled() || ManaCosts.isCreativeFree(player)) {
            return;
        }
        CostSettings settings = CostSettings.server();
        double total = ManaCosts.cost(player, holder, info);
        switch (settings.channelCostMode()) {
            case DRAIN -> {
                if (settings.channelStartPercent() > 0.0) {
                    spend(player, total * settings.channelStartPercent() / 100.0, settings);
                }
            }
            case START -> {
                spend(player, total, settings);
                START_PAID.put(player.getUUID(), id(holder));
            }
            case END -> { }
        }
    }

    public static void requestChannelEnd(ServerPlayer player, Holder<Spell> holder) {
        PENDING_END.put(player.getUUID(), id(holder));
    }

    /** End of the player's server tick: end channels that ran out of mana, forget finished START channels. */
    public static void tick(ServerPlayer player) {
        SpellCaster.Player caster = (SpellCaster.Player) player;
        SpellCast.Process process = caster.getInteractor().process();
        ResourceLocation pending = PENDING_END.remove(player.getUUID());
        if (pending != null && process != null && process.id().equals(pending)) {
            // Same as releasing the key: fires RELEASE (cooldown) and clears the process, which the client follows.
            SpellCast.TargetSnapshot snapshot = caster.getInteractor().latestSnapshot(pending);
            caster.getInteractor().requestEnd(pending, snapshot != null ? snapshot : SpellCast.TargetSnapshot.EMPTY);
            process = caster.getInteractor().process();
            notifyInsufficientMana(player);
        }
        ResourceLocation paid = START_PAID.get(player.getUUID());
        if (paid != null && (process == null || !process.id().equals(paid))) {
            START_PAID.remove(player.getUUID());
        }
    }

    public static void onLogout(ServerPlayer player) {
        START_PAID.remove(player.getUUID());
        PENDING_END.remove(player.getUUID());
        LAST_NOTIFY.remove(player.getUUID());
    }

    /**
     * Tells the client that the server rejected a cast or ended a channel for lack of mana (message + bar flash).
     * Sent at most once per {@value #NOTIFY_INTERVAL} ticks per player.
     */
    public static void notifyInsufficientMana(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_NOTIFY.get(player.getUUID());
        if (last != null && now - last < NOTIFY_INTERVAL && now >= last) {
            return;
        }
        LAST_NOTIFY.put(player.getUUID(), now);
        PacketDistributor.sendToPlayer(player, ManaInsufficientPayload.INSTANCE);
    }

    private static void spend(ServerPlayer player, double amount, CostSettings settings) {
        if (amount > 0.0) {
            ManaManager.spendForSpell(player, amount, ManaCosts.allowsDebt(settings));
        }
    }

    private static ResourceLocation id(Holder<Spell> holder) {
        Optional<ResourceLocation> id = holder.unwrapKey().map(ResourceKey::location);
        return id.orElse(SpellRegistry.ID);
    }
}
