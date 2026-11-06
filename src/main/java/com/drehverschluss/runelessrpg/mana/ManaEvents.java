package com.drehverschluss.runelessrpg.mana;

import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Server-side lifecycle of the mana pool: base values, login, respawn, dimension change and regeneration.
 */
public final class ManaEvents {
    private ManaEvents() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ManaEvents::onConfigReloading);
        NeoForge.EVENT_BUS.addListener(ManaEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(ManaEvents::onClone);
        NeoForge.EVENT_BUS.addListener(ManaEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(ManaEvents::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(ManaEvents::onPlayerTick);
    }

    /** Applies the configured base values of max mana and regeneration. */
    public static void applyBaseValues(ServerPlayer player) {
        setBase(player, RunelessAttributes.MAX_MANA, RunelessServerConfig.BASE_MAX_MANA.get());
        setBase(player, RunelessAttributes.MANA_REGEN, RunelessServerConfig.BASE_MANA_REGEN.get());
    }

    private static void setBase(ServerPlayer player, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }

    // Equipment modifiers (e.g. max mana on armor) are not saved; Minecraft applies them again on the player's first tick.
    // So login and respawn never clamp or refill right away: onPlayerTick does it after the equipment update.
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyBaseValues(player);
            ManaManager.initializeIfMissing(player);
            ManaManager.sync(player);
        }
    }

    // Keep the pre-death value on the new player so the respawn refill reports a correct old value.
    private static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            player.setData(ManaAttachments.MANA, ManaManager.getMana(event.getOriginal()));
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyBaseValues(player);
            if (!event.isEndConquered()) {
                player.getData(ManaAttachments.SYNC_STATE).refillPending = true;
            }
            ManaManager.sync(player);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyBaseValues(player);
            ManaManager.clampToMax(player);
            ManaManager.sync(player);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            ManaAttachments.ManaSyncState state = player.getData(ManaAttachments.SYNC_STATE);
            if (state.refillPending) {
                state.refillPending = false;
                ManaManager.setMana(player, ManaManager.getMaxMana(player), ManaChangeEvent.Reason.RESPAWN);
            }
            ManaManager.clampToMax(player);
            if (!player.isSpectator()) {
                ManaManager.tickRegeneration(player);
            }
        }
    }

    // The config watcher fires on its own thread; apply on the server thread.
    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != RunelessServerConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    applyBaseValues(player);
                    ManaManager.clampToMax(player);
                }
            });
        }
    }
}
