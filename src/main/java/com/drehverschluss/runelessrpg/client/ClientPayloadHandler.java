package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.client.hud.ManaTextFade;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.cost.SpellCostOverrides;
import com.drehverschluss.runelessrpg.mana.ManaAttachments;
import com.drehverschluss.runelessrpg.network.CostSettingsPayload;
import com.drehverschluss.runelessrpg.network.ManaSyncPayload;
import com.drehverschluss.runelessrpg.network.SpellCostOverridesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleManaSync(ManaSyncPayload payload) {
        ClientManaState.setMana(payload.mana());
        if (payload.notable()) {
            ManaTextFade.ping();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setData(ManaAttachments.MANA, payload.mana());
        }
    }

    public static void handleCostSettings(CostSettingsPayload payload) {
        CostSettings.setClient(payload.settings());
    }

    public static void handleSpellCosts(SpellCostOverridesPayload payload) {
        SpellCostOverrides.setClientValues(payload.costs());
    }

    public static void handleManaInsufficient() {
        ManaFeedback.onServerInsufficientMana();
    }
}
