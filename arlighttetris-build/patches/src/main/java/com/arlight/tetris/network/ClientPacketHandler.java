package com.arlight.tetris.network;

import com.arlight.tetris.client.ClientGameState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Del lado cliente: recibe el estado que manda el servidor y lo guarda para que el overlay lo dibuje. */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void handleBoardState(ClientboundBoardStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientGameState.board = packet);
    }

    public static void handleOpponentsState(ClientboundOpponentsStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientGameState.opponents = packet.opponents());
    }

    public static void handleMatchState(ClientboundMatchStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.stateId() < 0) ClientGameState.clear();
            else ClientGameState.match = packet;
        });
    }
}
