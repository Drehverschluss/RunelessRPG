package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.cost.SpellCostOverrides;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Server → client: per-spell costs and factors from data files (sent on login and after /reload). */
public record SpellCostOverridesPayload(Map<ResourceLocation, SpellCostOverrides.Value> costs) implements CustomPacketPayload {
    public static final Type<SpellCostOverridesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "spell_costs"));
    public static final StreamCodec<ByteBuf, SpellCostOverridesPayload> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, SpellCostOverrides.Value, Map<ResourceLocation, SpellCostOverrides.Value>>map(HashMap::new,
                    ResourceLocation.STREAM_CODEC, SpellCostOverrides.Value.STREAM_CODEC)
            .map(SpellCostOverridesPayload::new, SpellCostOverridesPayload::costs);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
