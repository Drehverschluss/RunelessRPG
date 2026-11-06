package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.RunelessRPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → client: a cast was rejected or a channel ended for lack of mana (shows the message and flashes the bar). */
public record ManaInsufficientPayload() implements CustomPacketPayload {
    public static final ManaInsufficientPayload INSTANCE = new ManaInsufficientPayload();
    public static final Type<ManaInsufficientPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "mana_insufficient"));
    public static final StreamCodec<ByteBuf, ManaInsufficientPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
