package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → client: cost settings from the server config (sent on login and after the config changed). */
public record CostSettingsPayload(CostSettings settings) implements CustomPacketPayload {
    public static final Type<CostSettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "cost_settings"));
    public static final StreamCodec<FriendlyByteBuf, CostSettingsPayload> STREAM_CODEC =
            CostSettings.STREAM_CODEC.map(CostSettingsPayload::new, CostSettingsPayload::settings);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
