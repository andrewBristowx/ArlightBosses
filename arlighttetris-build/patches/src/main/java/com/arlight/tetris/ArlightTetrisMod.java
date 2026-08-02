package com.arlight.tetris;

import com.arlight.tetris.integration.ArlightCoreIntegration;
import com.arlight.tetris.multiplayer.LobbyManager;
import com.arlight.tetris.network.NetworkSender;
import com.arlight.tetris.world.ArenaMatchListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Punto de entrada de ArlightTetris 0.3.2. */
@Mod(ArlightTetrisMod.MODID)
public class ArlightTetrisMod {

    public static final String MODID = "arlighttetris";
    private static final Logger LOGGER = Logger.getLogger("ArlightTetris");

    public static final LobbyManager DEBUG_LOBBY = new LobbyManager();

    private static int broadcastTickCounter = 0;
    private static int finishedTicksElapsed = -1;
    private static final int FINISHED_DISPLAY_TICKS = 20 * 8;

    // Arclight puede terminar de habilitar los plugins unos ticks después del
    // evento NeoForge. Se reintenta durante un minuto en vez de asumir que el
    // primer registro silencioso funcionó.
    private static int coreRetryTicks = 0;
    private static int coreRetriesRemaining = 12;

    public ArlightTetrisMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // El registro de red vive en NetworkSetup.
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        DEBUG_LOBBY.addListener(new ArenaMatchListener(event.getServer()));
        tryRegisterCore();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        retryCoreIntegrationWhenNeeded();

        LobbyManager.State stateBefore = DEBUG_LOBBY.getState();
        DEBUG_LOBBY.tick();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        boolean stateChanged = stateBefore != DEBUG_LOBBY.getState();
        broadcastTickCounter++;

        for (UUID playerId : DEBUG_LOBBY.getSessions().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) continue;

            var session = DEBUG_LOBBY.getSession(playerId);
            if (session != null) NetworkSender.sendBoardState(player, session);

            if (stateChanged || broadcastTickCounter % 5 == 0) {
                NetworkSender.sendMatchState(player, DEBUG_LOBBY, playerId);
            }
        }

        if (stateChanged || broadcastTickCounter % 5 == 0) {
            NetworkSender.broadcastOpponentsState(DEBUG_LOBBY,
                    id -> server.getPlayerList().getPlayer(id));
        }

        handleFinishedResetTimer(stateChanged, server);
    }

    private void retryCoreIntegrationWhenNeeded() {
        if (ArlightCoreIntegration.isActive() || coreRetriesRemaining <= 0) return;
        coreRetryTicks++;
        if (coreRetryTicks < 100) return;
        coreRetryTicks = 0;
        coreRetriesRemaining--;
        tryRegisterCore();
    }

    private void tryRegisterCore() {
        try {
            if (ArlightCoreIntegration.tryRegister()) coreRetriesRemaining = 0;
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING,
                    "Integración con ArlightCore no disponible; ArlightTetris sigue standalone.", throwable);
        }
    }

    private void handleFinishedResetTimer(boolean stateChanged, MinecraftServer server) {
        if (DEBUG_LOBBY.getState() != LobbyManager.State.FINISHED) {
            finishedTicksElapsed = -1;
            return;
        }
        if (stateChanged) {
            finishedTicksElapsed = 0;
            return;
        }
        finishedTicksElapsed++;
        if (finishedTicksElapsed >= FINISHED_DISPLAY_TICKS) {
            // Copia previa porque resetForNextMatch vacía el mapa.
            for (UUID id : new ArrayList<>(DEBUG_LOBBY.getSessions().keySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) NetworkSender.sendClearState(player);
            }
            DEBUG_LOBBY.resetForNextMatch();
            finishedTicksElapsed = -1;
        }
    }
}
