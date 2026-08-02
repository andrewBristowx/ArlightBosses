package com.arlight.tetris.network;

import com.arlight.tetris.game.ActivePiece;
import com.arlight.tetris.game.GameSession;
import com.arlight.tetris.multiplayer.LobbyManager;
import com.arlight.tetris.multiplayer.MatchConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Arma los paquetes clientbound a partir del estado real del juego y los manda. */
public final class NetworkSender {

    private NetworkSender() {}

    public static void sendBoardState(ServerPlayer player, GameSession session) {
        ActivePiece piece = session.getCurrentPiece();
        ClientboundBoardStatePacket packet = new ClientboundBoardStatePacket(
                session.board.encode(),
                piece.getType().ordinal(),
                piece.getX(),
                piece.getY(),
                piece.getRotation(),
                session.getGhostY(),
                session.getHoldPiece() == null ? -1 : session.getHoldPiece().ordinal(),
                session.encodeNextQueue(5),
                session.getLinesCleared(),
                Math.max(session.getComboCount(), 0),
                session.isBackToBack(),
                session.isTopOut()
        );
        PacketDistributor.sendToPlayer(player, packet);
    }

    /** Manda a TODOS los jugadores de la sala el resumen de rivales (para el HUD lateral). */
    public static void broadcastOpponentsState(LobbyManager lobby, java.util.function.Function<UUID, ServerPlayer> resolver) {
        Map<UUID, GameSession> sessions = lobby.getSessions();

        List<ClientboundOpponentsStatePacket.OpponentSummary> summaries = new ArrayList<>();
        for (Map.Entry<UUID, GameSession> entry : sessions.entrySet()) {
            GameSession session = entry.getValue();
            if (session == null) continue;
            ServerPlayer p = resolver.apply(entry.getKey());
            String name = p != null ? p.getGameProfile().getName() : "???";
            summaries.add(new ClientboundOpponentsStatePacket.OpponentSummary(
                    entry.getKey(), name, session.board.columnHeights(), session.getLinesCleared(), !session.isTopOut()
            ));
        }

        ClientboundOpponentsStatePacket packet = new ClientboundOpponentsStatePacket(summaries);
        for (UUID id : sessions.keySet()) {
            ServerPlayer p = resolver.apply(id);
            if (p != null) PacketDistributor.sendToPlayer(p, packet);
        }
    }

    /** Limpia inmediatamente el HUD de Tetris del cliente. */
    public static void sendClearState(ServerPlayer player) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new ClientboundMatchStatePacket(
                -1, 0, MatchConfig.MIN_PLAYERS_TO_START, MatchConfig.MAX_PLAYERS,
                -1, false, false));
        PacketDistributor.sendToPlayer(player, new ClientboundOpponentsStatePacket(List.of()));
    }

    public static void sendMatchState(ServerPlayer player, LobbyManager lobby, UUID playerId) {
        GameSession session = lobby.getSession(playerId);
        boolean alive = session == null || !session.isTopOut();
        boolean won = lobby.getState() == LobbyManager.State.FINISHED && alive && lobby.getPlayerCount() > 1;

        ClientboundMatchStatePacket packet = new ClientboundMatchStatePacket(
                lobby.getState().ordinal(),
                lobby.getPlayerCount(),
                MatchConfig.MIN_PLAYERS_TO_START,
                MatchConfig.MAX_PLAYERS,
                lobby.getCountdownSecondsRemaining(),
                alive,
                won
        );
        PacketDistributor.sendToPlayer(player, packet);
    }
}
