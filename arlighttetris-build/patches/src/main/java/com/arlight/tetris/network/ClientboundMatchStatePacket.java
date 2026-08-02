package com.arlight.tetris.network;

import com.arlight.tetris.ArlightTetrisMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Estado general de la sala/partida: para mostrar "Esperando jugadores (2/8
 * mínimo)", el countdown, o la pantalla de resultado al terminar.
 * stateId sigue el orden de {@link com.arlight.tetris.multiplayer.LobbyManager.State}.
 */
public record ClientboundMatchStatePacket(
        int stateId,
        int playerCount,
        int minPlayers,
        int maxPlayers,
        int countdownSecondsRemaining,
        boolean youAreAlive,
        boolean youWon
) implements CustomPacketPayload {

    public static final Type<ClientboundMatchStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArlightTetrisMod.MODID, "match_state"));

    public static final StreamCodec<ByteBuf, ClientboundMatchStatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClientboundMatchStatePacket decode(ByteBuf buffer) {
            return new ClientboundMatchStatePacket(
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)
            );
        }

        @Override
        public void encode(ByteBuf buffer, ClientboundMatchStatePacket value) {
            ByteBufCodecs.VAR_INT.encode(buffer, value.stateId());
            ByteBufCodecs.VAR_INT.encode(buffer, value.playerCount());
            ByteBufCodecs.VAR_INT.encode(buffer, value.minPlayers());
            ByteBufCodecs.VAR_INT.encode(buffer, value.maxPlayers());
            ByteBufCodecs.VAR_INT.encode(buffer, value.countdownSecondsRemaining());
            ByteBufCodecs.BOOL.encode(buffer, value.youAreAlive());
            ByteBufCodecs.BOOL.encode(buffer, value.youWon());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
