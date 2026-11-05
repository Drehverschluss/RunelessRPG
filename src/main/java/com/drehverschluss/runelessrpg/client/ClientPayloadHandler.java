package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.mana.ManaAttachments;
import com.drehverschluss.runelessrpg.network.ManaSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleManaSync(ManaSyncPayload payload) {
        ClientManaState.setMana(payload.mana());
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setData(ManaAttachments.MANA, payload.mana());
        }
    }
}
