package com.arlight.tetris;

import com.arlight.tetris.integration.ArlightCoreIntegration;
import com.arlight.tetris.multiplayer.LobbyManager;
import com.arlight.tetris.multiplayer.MatchConfig;
import com.arlight.tetris.network.NetworkSender;
import com.arlight.tetris.world.TetrisArenaBuilder;
import com.arlight.tetris.world.TetrisArenaConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ArlightTetrisMod.MODID)
public final class TetrisCommands {

    private TetrisCommands() { }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("tetris")
                .then(Commands.literal("join").executes(TetrisCommands::join))
                .then(Commands.literal("leave").executes(TetrisCommands::leave))
                .then(Commands.literal("status").executes(TetrisCommands::status))
                .then(Commands.literal("test")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start").executes(TetrisCommands::testStart))
                        .then(Commands.literal("stop").executes(TetrisCommands::testStop))
                        .then(Commands.literal("restart").executes(TetrisCommands::testRestart)))
                .then(Commands.literal("core")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("status").executes(TetrisCommands::coreStatus))
                        .then(Commands.literal("retry").executes(TetrisCommands::coreRetry)))
                .then(Commands.literal("arena")
                        .then(Commands.literal("setcenter")
                                .requires(source -> source.hasPermission(2))
                                .executes(TetrisCommands::arenaSetCenter))
                        .then(Commands.literal("build")
                                .requires(source -> source.hasPermission(2))
                                .executes(TetrisCommands::arenaBuild)))
        );
    }

    private static int join(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;

        if (lobby.getState() != LobbyManager.State.WAITING) {
            player.sendSystemMessage(Component.literal("La sala no está aceptando jugadores ahora mismo."));
            return 0;
        }
        if (!lobby.addPlayer(player.getUUID())) {
            player.sendSystemMessage(Component.literal("No te pudiste unir (sala llena o ya estás adentro)."));
            return 0;
        }
        if (!ArlightCoreIntegration.beginSession(player.getUUID())) {
            lobby.removePlayer(player.getUUID());
            player.sendSystemMessage(Component.literal("ArlightCore no pudo reservar tu sesión. Inténtalo nuevamente."));
            return 0;
        }
        player.sendSystemMessage(Component.literal(
                "Te uniste a Tetris (" + lobby.getPlayerCount() + "/" + MatchConfig.MAX_PLAYERS + ")."));
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;

        if (lobby.isSoloTester(player.getUUID())) {
            boolean stopped = lobby.stopSoloTest(player.getUUID());
            NetworkSender.sendClearState(player);
            player.sendSystemMessage(Component.literal(stopped
                    ? "Prueba individual detenida."
                    : "No se pudo detener la prueba individual."));
            return stopped ? 1 : 0;
        }

        if (lobby.removePlayer(player.getUUID())) {
            NetworkSender.sendClearState(player);
            player.sendSystemMessage(Component.literal("Saliste de la sala de Tetris."));
            return 1;
        }
        player.sendSystemMessage(Component.literal("No se pudo salir (la partida ya está en curso o no estabas anotado)."));
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;
        context.getSource().sendSuccess(() -> Component.literal(
                "Estado: " + lobby.getState()
                        + " | Jugadores: " + lobby.getPlayerCount() + "/" + MatchConfig.MAX_PLAYERS
                        + " | Prueba individual: " + (lobby.isSoloTestMode() ? "sí" : "no")
                        + " | Core: " + ArlightCoreIntegration.describeStatus()), false);
        return 1;
    }

    private static int testStart(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;

        if (lobby.getState() != LobbyManager.State.WAITING || lobby.getPlayerCount() != 0) {
            player.sendSystemMessage(Component.literal(
                    "La sala debe estar vacía y en WAITING. Usa /tetris test stop o espera a que termine la partida."));
            return 0;
        }
        if (!ArlightCoreIntegration.beginSession(player.getUUID())) {
            player.sendSystemMessage(Component.literal("ArlightCore no pudo reservar tu sesión para la prueba."));
            return 0;
        }
        if (!lobby.startSoloTest(player.getUUID())) {
            ArlightCoreIntegration.endSession(player.getUUID());
            player.sendSystemMessage(Component.literal("No se pudo iniciar la prueba individual."));
            return 0;
        }
        player.sendSystemMessage(Component.literal(
                "Prueba individual iniciada. Usa /tetris test stop para terminarla sin otorgar XP."));
        return 1;
    }

    private static int testStop(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        boolean stopped = ArlightTetrisMod.DEBUG_LOBBY.stopSoloTest(player.getUUID());
        NetworkSender.sendClearState(player);
        player.sendSystemMessage(Component.literal(stopped
                ? "Prueba individual detenida y sesión restaurada."
                : "No tienes una prueba individual activa."));
        return stopped ? 1 : 0;
    }

    private static int testRestart(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;
        if (lobby.isSoloTester(player.getUUID())) {
            lobby.stopSoloTest(player.getUUID());
            NetworkSender.sendClearState(player);
        }
        return testStart(context);
    }

    private static int coreStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "Integración ArlightCore: " + ArlightCoreIntegration.describeStatus()), false);
        return ArlightCoreIntegration.isActive() ? 1 : 0;
    }

    private static int coreRetry(CommandContext<CommandSourceStack> context) {
        boolean active = ArlightCoreIntegration.tryRegister();
        context.getSource().sendSuccess(() -> Component.literal(
                "Resultado del reintento: " + ArlightCoreIntegration.describeStatus()), true);
        return active ? 1 : 0;
    }

    private static int arenaSetCenter(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = player(context);
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        TetrisArenaConfig.save(new TetrisArenaConfig.ArenaLocation(level.dimension(), center));
        context.getSource().sendSuccess(() -> Component.literal(
                "Centro de la arena guardado en " + center.toShortString()
                        + " (" + level.dimension().location() + "). Usa /tetris arena build."), true);
        return 1;
    }

    private static int arenaBuild(CommandContext<CommandSourceStack> context) {
        var arena = TetrisArenaConfig.load();
        if (arena.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "No hay centro guardado. Usa /tetris arena setcenter primero."));
            return 0;
        }

        var location = arena.get();
        ServerLevel level = context.getSource().getServer().getLevel(location.dimension());
        if (level == null) {
            context.getSource().sendFailure(Component.literal("La dimensión de la arena no está cargada."));
            return 0;
        }

        TetrisArenaBuilder.build(level, location.center());
        context.getSource().sendSuccess(() -> Component.literal(
                "Arena de Tetris construida en " + location.center().toShortString() + "."), true);
        return 1;
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
        return context.getSource().getEntity() instanceof ServerPlayer player ? player : null;
    }
}
