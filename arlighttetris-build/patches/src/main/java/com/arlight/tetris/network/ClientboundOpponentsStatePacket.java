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

/**
 * Resumen liviano de TODOS los jugadores vivos de la partida, para dibujar
 * los mini-tableros / lista de rivales al costado (estilo tetr.io). En vez
 * de mandar la grilla completa de cada rival (caro), se manda solo la
 * altura de cada columna — alcanza para un "skyline" reconocible.
 */
public record ClientboundOpponentsStatePacket(List<OpponentSummary> opponents) implements CustomPacketPayload {

    public static final Type<ClientboundOpponentsStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArlightTetrisMod.MODID, "opponents_state"));

    public static final StreamCodec<ByteBuf, ClientboundOpponentsStatePacket> STREAM_CODEC = StreamCodec.composite(
            OpponentSummary.STREAM_CODEC.apply(ByteBufCodecs.list()), ClientboundOpponentsStatePacket::opponents,
            ClientboundOpponentsStatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** columnHeights: 10 valores 0-24, uno por columna del tablero de ese rival. */
    public record OpponentSummary(UUID playerId, String name, byte[] columnHeights, int linesCleared, boolean alive) {

        public static final StreamCodec<ByteBuf, OpponentSummary> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, s -> s.playerId().toString(),
                ByteBufCodecs.STRING_UTF8, OpponentSummary::name,
                ByteBufCodecs.byteArray(Board.WIDTH), OpponentSummary::columnHeights,
                ByteBufCodecs.VAR_INT, OpponentSummary::linesCleared,
                ByteBufCodecs.BOOL, OpponentSummary::alive,
                (idStr, name, heights, lines, alive) -> new OpponentSummary(UUID.fromString(idStr), name, heights, lines, alive)
        );
    }
}
