package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.RunelessRPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: not enough mana. {@code missing} = how much mana was missing; {@code message} = also show Spell
 * Engine's "Missing Mana" message (a cast rejected or a channel ended by RunelessRPG), false for another mod's signal.
 * The client flashes the bar and fires {@link com.drehverschluss.runelessrpg.api.ManaInsufficientEvent}.
 */
public record ManaInsufficientPayload(double missing, boolean message) implements CustomPacketPayload {
    public static final Type<ManaInsufficientPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "mana_insufficient"));
    public static final StreamCodec<ByteBuf, ManaInsufficientPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ManaInsufficientPayload::missing, ByteBufCodecs.BOOL, ManaInsufficientPayload::message,
            ManaInsufficientPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
