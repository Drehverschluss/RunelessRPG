package com.drehverschluss.runelessrpg.network;

import com.drehverschluss.runelessrpg.RunelessRPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: current mana of the receiving player. {@code notable} = a change the player should see (spell cost
 * except DRAIN channel ticks, potion, leech, command, respawn, max mana); false for regeneration, DRAIN ticks and plain
 * resyncs. The client shows the number for {@code textVisibility = ON_CHANGE} only on notable changes.
 */
public record ManaSyncPayload(double mana, boolean notable) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "mana_sync"));
    public static final StreamCodec<ByteBuf, ManaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ManaSyncPayload::mana, ByteBufCodecs.BOOL, ManaSyncPayload::notable, ManaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
