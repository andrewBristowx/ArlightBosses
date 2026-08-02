package com.arlight.tetris.network;

import com.arlight.tetris.ArlightTetrisMod;
import com.arlight.tetris.game.Board;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record ClientboundOpponentsStatePacket(List<OpponentSummary> opponents) implements CustomPacketPayload {
    public static final Type<ClientboundOpponentsStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArlightTetrisMod.MODID, "opponents_state"));

    public static final StreamCodec<ByteBuf, ClientboundOpponentsStatePacket> STREAM_CODEC = StreamCodec.composite(
            OpponentSummary.STREAM_CODEC.apply(ByteBufCodecs.list()), ClientboundOpponentsStatePacket::opponents,
            ClientboundOpponentsStatePacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record OpponentSummary(UUID playerId, String name, byte[] columnHeights, int linesCleared, boolean alive) {
        public static final StreamCodec<ByteBuf, OpponentSummary> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, s -> s.playerId().toString(),
                ByteBufCodecs.STRING_UTF8, OpponentSummary::name,
                ByteBufCodecs.byteArray(Board.WIDTH), OpponentSummary::columnHeights,
                ByteBufCodecs.VAR_INT, OpponentSummary::linesCleared,
                ByteBufCodecs.BOOL, OpponentSummary::alive,
                (id, name, heights, lines, alive) -> new OpponentSummary(UUID.fromString(id), name, heights, lines, alive));
    }
}
