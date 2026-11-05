package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.RunelessRPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Server → client: per-spell base costs from datapacks (sent on login and after /reload). */
public record SpellCostOverridesPayload(Map<ResourceLocation, Double> costs) implements CustomPacketPayload {
    public static final Type<SpellCostOverridesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "spell_costs"));
    public static final StreamCodec<ByteBuf, SpellCostOverridesPayload> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, Double, Map<ResourceLocation, Double>>map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.DOUBLE)
            .map(SpellCostOverridesPayload::new, SpellCostOverridesPayload::costs);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
