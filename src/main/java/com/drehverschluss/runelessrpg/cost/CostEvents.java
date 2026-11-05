package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.network.CostSettingsPayload;
import com.drehverschluss.runelessrpg.network.SpellCostOverridesPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Wiring of the cost system: datapack values, sync to clients, report, Spell Engine events. */
public final class CostEvents {
    private CostEvents() {}

    public static void register(IEventBus modBus) {
        ManaCastHandler.registerSpellEngineEvents();
        modBus.addListener(CostEvents::onConfigReloading);
        NeoForge.EVENT_BUS.addListener(CostEvents::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(CostEvents::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(CostEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(CostEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(CostEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(CostEvents::onPlayerTick);
    }

    private static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SpellCostOverrides.Loader(event.getRegistryAccess()));
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        SpellCostOverridesPayload payload = new SpellCostOverridesPayload(SpellCostOverrides.serverValues());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
        if (event.getPlayer() == null) {
            // Null player = /reload: the report reflects the new datapack values.
            SpellCostReport.write(event.getPlayerList().getServer());
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new CostSettingsPayload(CostSettings.server()));
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ManaCastHandler.onLogout(player);
        }
    }

    private static void onServerStarted(ServerStartedEvent event) {
        SpellCostReport.write(event.getServer());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ManaCastHandler.tick(player);
        }
    }

    // NeoForge only syncs server configs on login; runtime changes are sent here (config watcher thread → server thread).
    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != RunelessServerConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> PacketDistributor.sendToAllPlayers(new CostSettingsPayload(CostSettings.server())));
        }
    }
}
