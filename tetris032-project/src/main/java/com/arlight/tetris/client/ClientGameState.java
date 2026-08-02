package com.arlight.tetris.client;

import com.arlight.tetris.network.ClientboundBoardStatePacket;
import com.arlight.tetris.network.ClientboundMatchStatePacket;
import com.arlight.tetris.network.ClientboundOpponentsStatePacket;

import java.util.List;

/**
 * Estado del cliente para renderizar el HUD de Tetris. Un solo objeto
 * estático simple alcanza para v1 (el jugador solo puede estar en una
 * partida a la vez).
 */
public final class ClientGameState {

    private ClientGameState() {}

    public static volatile ClientboundBoardStatePacket board = null;
    public static volatile List<ClientboundOpponentsStatePacket.OpponentSummary> opponents = List.of();
    public static volatile ClientboundMatchStatePacket match = null;

    /** true si el jugador está anotado en la sala, en cualquier estado (esperando, countdown, jugando o recién terminada). */
    public static boolean isInLobbyOrMatch() {
        return match != null;
    }

    /** true solo mientras la partida está efectivamente en curso (para decidir si se envían inputs). */
    public static boolean isPlaying() {
        return match != null && match.stateId() == 2 /* IN_PROGRESS */;
    }

    public static void clear() {
        board = null;
        opponents = List.of();
        match = null;
    }
}
