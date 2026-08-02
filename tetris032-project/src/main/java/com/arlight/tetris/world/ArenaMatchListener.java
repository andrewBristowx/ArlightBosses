package com.arlight.tetris.world;

import com.arlight.tetris.integration.ArlightCoreIntegration;
import com.arlight.tetris.multiplayer.LobbyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ArenaMatchListener implements LobbyManager.Listener {
    private static final Logger LOGGER = Logger.getLogger("ArlightTetris");
    private final MinecraftServer server;

    public ArenaMatchListener(MinecraftServer server) { this.server = server; }

    @Override
    public void onMatchStart(List<UUID> orderedPlayers) {
        Optional<TetrisArenaConfig.ArenaLocation> arena = TetrisArenaConfig.load();
        if (arena.isEmpty()) LOGGER.warning("No hay arena configurada para Tetris.");
        List<BlockPos> pods = arena.map(a -> TetrisArena.podCenters(a.center(), orderedPlayers.size()))
                .orElse(List.of());
        for (int i = 0; i < orderedPlayers.size(); i++) {
            UUID id = orderedPlayers.get(i);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            PlayerReturnPoints.capture(player);
            ArlightCoreIntegration.markStarted(id);
            if (arena.isPresent() && i < pods.size()) teleportToPod(player, arena.get(), pods.get(i));
        }
    }

    @Override
    public void onMatchEnd(List<UUID> allPlayers, UUID winner) {
        ArlightCoreIntegration.awardWin(winner);
        for (UUID id : allPlayers) {
            ArlightCoreIntegration.endSession(id);
            returnPlayer(id);
        }
    }

    @Override
    public void onPlayerLeft(UUID playerId) {
        ArlightCoreIntegration.endSession(playerId);
        returnPlayer(playerId);
    }

    private void returnPlayer(UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        PlayerReturnPoints.ReturnPoint point = PlayerReturnPoints.get(id);
        if (player == null || point == null) return;
        ServerLevel level = server.getLevel(point.dimension());
        if (level != null) {
            try {
                player.teleportTo(level, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "No se pudo devolver al jugador", t);
            }
        }
        PlayerReturnPoints.clear(id);
    }

    private void teleportToPod(ServerPlayer player, TetrisArenaConfig.ArenaLocation arena, BlockPos pod) {
        ServerLevel level = server.getLevel(arena.dimension());
        if (level == null) return;
        BlockPos standing = TetrisArena.standingPos(pod);
        float yaw = TetrisArena.podYawFacingCenter(arena.center(), pod);
        try {
            player.teleportTo(level, standing.getX() + 0.5, standing.getY(), standing.getZ() + 0.5, yaw, 0f);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "No se pudo teletransportar al pod", t);
        }
    }
}
