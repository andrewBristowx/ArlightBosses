package com.arlight.tetris.network;

import com.arlight.tetris.ArlightTetrisMod;
import com.arlight.tetris.game.GameSession;
import com.arlight.tetris.multiplayer.LobbyManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public final class ServerPacketHandler {
    private ServerPacketHandler() {}

    public static void handleGameAction(ServerboundGameActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;
            UUID id = player.getUUID();
            if (!lobby.containsPlayer(id) || lobby.getState() != LobbyManager.State.IN_PROGRESS) return;
            GameSession session = lobby.getSession(id);
            if (session == null || session.isTopOut()) return;
            switch (packet.action()) {
                case MOVE_LEFT -> session.move(-1, 0);
                case MOVE_RIGHT -> session.move(1, 0);
                case SOFT_DROP -> session.move(0, 1);
                case ROTATE_CW -> session.rotate(1);
                case ROTATE_CCW -> session.rotate(-1);
                case HARD_DROP -> session.hardDrop();
                case HOLD -> session.hold();
            }
            lobby.distributePendingAttack(id);
            NetworkSender.sendBoardState(player, session);
        });
    }
}
