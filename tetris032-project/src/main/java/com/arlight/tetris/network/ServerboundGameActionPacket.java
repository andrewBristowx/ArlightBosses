package com.arlight.tetris.network;

import com.arlight.tetris.ArlightTetrisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundGameActionPacket(int actionId) implements CustomPacketPayload {
    public static final Type<ServerboundGameActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArlightTetrisMod.MODID, "game_action"));
    public static final StreamCodec<ByteBuf, ServerboundGameActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerboundGameActionPacket::actionId, ServerboundGameActionPacket::new);
    public GameAction action() { return GameAction.values()[actionId]; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
