package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.client.ClientPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RunelessNetwork {
    private static final String PROTOCOL_VERSION = "4";

    private RunelessNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(RunelessNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // The client handler class is only loaded when a payload actually arrives on the client.
        registrar.playToClient(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleManaSync(payload));
        registrar.playToClient(CostSettingsPayload.TYPE, CostSettingsPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleCostSettings(payload));
        registrar.playToClient(SpellCostOverridesPayload.TYPE, SpellCostOverridesPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleSpellCosts(payload));
        registrar.playToClient(ManaInsufficientPayload.TYPE, ManaInsufficientPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloadHandler.handleManaInsufficient());
    }
}
