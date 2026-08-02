package com.arlight.tetris.integration;

import com.arlight.tetris.ArlightTetrisMod;
import com.arlight.tetris.multiplayer.LobbyManager;
import com.arlight.tetris.multiplayer.MatchConfig;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Puente reflectivo seguro entre NeoForge/Arclight y ArlightCore. */
public final class ArlightCoreIntegration {

    private static final String MINIGAME_ID = "tetris";
    private static final Logger LOGGER = Logger.getLogger("ArlightTetris");

    private static volatile boolean active;
    private static volatile String lastStatus = "sin intentar";
    private static volatile int attempts;

    private static ClassLoader coreLoader;
    private static Class<?> coreApiClass;
    private static Class<?> providerInterface;
    private static Class<?> statusClass;
    private static Class<?> bukkitPlayerClass;
    private static Class<?> bukkitClass;
    private static Object providerProxy;

    private ArlightCoreIntegration() { }

    /**
     * Registra el proveedor y verifica que realmente haya quedado dentro del
     * MinigameRegistry. ArlightCoreAPI#registerMinigame no devuelve resultado,
     * así que esta verificación evita marcar la integración como activa cuando
     * el Core todavía no terminó de inicializarse.
     */
    public static synchronized boolean tryRegister() {
        if (active) return true;
        attempts++;

        try {
            bukkitClass = loadFirst("org.bukkit.Bukkit");
            if (bukkitClass == null) return unavailable("Bukkit no está presente");

            Object pluginManager = bukkitClass.getMethod("getPluginManager").invoke(null);
            Object corePlugin = pluginManager.getClass().getMethod("getPlugin", String.class)
                    .invoke(pluginManager, "ArlightCore");
            if (corePlugin == null) return unavailable("ArlightCore no está cargado todavía");

            try {
                Object enabled = corePlugin.getClass().getMethod("isEnabled").invoke(corePlugin);
                if (enabled instanceof Boolean value && !value) {
                    return unavailable("ArlightCore existe pero aún no está habilitado");
                }
            } catch (NoSuchMethodException ignored) {
                // Algunos wrappers híbridos no exponen el método en la clase concreta.
            }

            coreLoader = corePlugin.getClass().getClassLoader();
            coreApiClass = Class.forName("com.arlight.core.api.ArlightCoreAPI", true, coreLoader);
            providerInterface = Class.forName("com.arlight.core.api.MinigameProvider", true, coreLoader);
            statusClass = Class.forName("com.arlight.core.api.MinigameStatus", true, coreLoader);
            bukkitPlayerClass = Class.forName("org.bukkit.entity.Player", true, coreLoader);

            if (providerProxy == null) {
                providerProxy = Proxy.newProxyInstance(coreLoader,
                        new Class<?>[]{providerInterface}, new ProviderHandler());
            }

            coreApiClass.getMethod("registerMinigame", providerInterface)
                    .invoke(null, providerProxy);

            Object registry = resolveRegistry(corePlugin);
            if (registry == null) return unavailable("ArlightCore todavía no inicializó MinigameRegistry");
            Object registered = registry.getClass().getMethod("get", String.class)
                    .invoke(registry, MINIGAME_ID);
            if (registered == null) return unavailable("el registro de Tetris no quedó confirmado");

            active = true;
            lastStatus = "activo y registrado (intento " + attempts + ")";
            LOGGER.info("ArlightTetris registrado y verificado en el selector de ArlightCore.");
            return true;
        } catch (Throwable throwable) {
            active = false;
            lastStatus = "error: " + unwrap(throwable).getClass().getSimpleName()
                    + " - " + safeMessage(unwrap(throwable));
            LOGGER.log(Level.WARNING,
                    "No se pudo registrar ArlightTetris en ArlightCore; sigue standalone.", unwrap(throwable));
            return false;
        }
    }

    public static boolean isActive() { return active; }

    public static String describeStatus() {
        return lastStatus + " | intentos=" + attempts;
    }

    public static boolean beginSession(UUID playerId) {
        if (!active) return true;
        Object player = findBukkitPlayer(playerId);
        if (player == null) return false;
        return invokeCoreBoolean("beginMinigameSession",
                new Class<?>[]{bukkitPlayerClass, String.class}, player, MINIGAME_ID);
    }

    public static void markStarted(UUID playerId) {
        if (!active) return;
        Object player = findBukkitPlayer(playerId);
        if (player != null) invokeCoreBoolean("markMinigameStarted",
                new Class<?>[]{bukkitPlayerClass}, player);
    }

    public static void awardWin(UUID winnerId) {
        if (!active || winnerId == null) return;
        Object player = findBukkitPlayer(winnerId);
        if (player == null) return;
        try {
            coreApiClass.getMethod("addWinXp", bukkitPlayerClass).invoke(null, player);
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "Falló addWinXp de ArlightCore", unwrap(throwable));
        }
    }

    public static void endSession(UUID playerId) {
        if (!active) return;
        Object player = findBukkitPlayer(playerId);
        if (player != null) invokeCoreBoolean("endMinigameSession",
                new Class<?>[]{bukkitPlayerClass}, player);
    }

    private static boolean unavailable(String reason) {
        active = false;
        lastStatus = "esperando: " + reason;
        if (attempts == 1) LOGGER.info(lastStatus + "; se reintentará automáticamente.");
        return false;
    }

    private static Object resolveRegistry(Object corePlugin) {
        try {
            return corePlugin.getClass().getMethod("getMinigameRegistry").invoke(corePlugin);
        } catch (Throwable ignored) {
            try {
                Field field = coreApiClass.getDeclaredField("registry");
                field.setAccessible(true);
                return field.get(null);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static Object findBukkitPlayer(UUID playerId) {
        if (!active || playerId == null) return null;
        try {
            return bukkitClass.getMethod("getPlayer", UUID.class).invoke(null, playerId);
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "No se pudo localizar al jugador Bukkit", unwrap(throwable));
            return null;
        }
    }

    private static boolean invokeCoreBoolean(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Object result = coreApiClass.getMethod(name, parameterTypes).invoke(null, args);
            return result instanceof Boolean value && value;
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "Falló " + name + " de ArlightCore", unwrap(throwable));
            return false;
        }
    }

    private static Class<?> loadFirst(String name) {
        ClassLoader[] loaders = {
                Thread.currentThread().getContextClassLoader(),
                ArlightCoreIntegration.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException ignored) { }
        }
        return null;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable cause = throwable.getCause();
        return cause == null ? throwable : cause;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? "sin detalle" : message;
    }

    private static UUID playerUuid(Object player) {
        if (player == null) return null;
        try {
            return (UUID) bukkitPlayerClass.getMethod("getUniqueId").invoke(player);
        } catch (Throwable throwable) {
            LOGGER.log(Level.WARNING, "No se pudo leer el UUID Bukkit", unwrap(throwable));
            return null;
        }
    }

    private static void sendMessage(Object player, String message) {
        if (player == null) return;
        try {
            Class<?> senderClass = Class.forName("org.bukkit.command.CommandSender", true, coreLoader);
            senderClass.getMethod("sendMessage", String.class).invoke(player, message);
        } catch (Throwable throwable) {
            LOGGER.log(Level.FINE, "No se pudo enviar un mensaje Bukkit", unwrap(throwable));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object status(String name) {
        return Enum.valueOf((Class<? extends Enum>) statusClass.asSubclass(Enum.class), name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object buildIcon() throws Exception {
        Class<?> materialClass = Class.forName("org.bukkit.Material", true, coreLoader);
        Object material = Enum.valueOf((Class<? extends Enum>) materialClass.asSubclass(Enum.class),
                "MAGENTA_CONCRETE");
        Class<?> itemStackClass = Class.forName("org.bukkit.inventory.ItemStack", true, coreLoader);
        Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta", true, coreLoader);
        Object icon = itemStackClass.getConstructor(materialClass).newInstance(material);
        Object meta = itemStackClass.getMethod("getItemMeta").invoke(icon);
        if (meta != null) {
            itemMetaClass.getMethod("setDisplayName", String.class).invoke(meta, "§d§lTetris");
            itemMetaClass.getMethod("setLore", List.class).invoke(meta, List.of(
                    "§7Multijugador estilo tetr.io",
                    "§7Mínimo " + MatchConfig.MIN_PLAYERS_TO_START
                            + " / Máximo " + MatchConfig.MAX_PLAYERS + " jugadores",
                    "§8Prueba OP: /tetris test start"
            ));
            itemStackClass.getMethod("setItemMeta", itemMetaClass).invoke(icon, meta);
        }
        return icon;
    }

    private static final class ProviderHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return switch (name) {
                    case "toString" -> "ArlightTetrisMinigameProviderProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> null;
                };
            }

            return switch (name) {
                case "getId", "getCategory" -> MINIGAME_ID;
                case "getDisplayName" -> "§dTetris";
                case "getIcon" -> buildIcon();
                case "getStatus" -> statusForLobby();
                case "join" -> { joinPlayer(args == null ? null : args[0]); yield null; }
                case "leave" -> { leavePlayer(args == null ? null : args[0]); yield null; }
                case "handleDisconnect" -> { disconnectPlayer(args == null ? null : args[0]); yield null; }
                case "cleanupAfterRecovery" -> null;
                case "isPlaying" -> {
                    UUID id = playerUuid(args == null ? null : args[0]);
                    yield id != null && ArlightTetrisMod.DEBUG_LOBBY.containsPlayer(id);
                }
                case "getCurrentPlayers" -> ArlightTetrisMod.DEBUG_LOBBY.getPlayerCount();
                case "getMaxPlayers" -> MatchConfig.MAX_PLAYERS;
                default -> defaultValue(method.getReturnType());
            };
        }

        private Object statusForLobby() {
            return switch (ArlightTetrisMod.DEBUG_LOBBY.getState()) {
                case WAITING -> status("WAITING");
                case COUNTDOWN, IN_PROGRESS -> status("IN_PROGRESS");
                case FINISHED -> status("RESTARTING");
            };
        }

        private void joinPlayer(Object player) {
            UUID playerId = playerUuid(player);
            if (playerId == null) return;
            LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;
            if (lobby.getState() != LobbyManager.State.WAITING) {
                sendMessage(player, "§cLa sala de Tetris no acepta jugadores ahora mismo.");
                return;
            }
            if (!lobby.addPlayer(playerId)) {
                sendMessage(player, "§cNo te pudiste unir (sala llena o ya estás dentro).");
                return;
            }
            if (!beginSession(playerId)) {
                lobby.removePlayer(playerId);
                sendMessage(player, "§cArlightCore no pudo reservar tu sesión.");
                return;
            }
            sendMessage(player, "§dTe uniste a Tetris (" + lobby.getPlayerCount()
                    + "/" + MatchConfig.MAX_PLAYERS + ").");
        }

        private void leavePlayer(Object player) {
            UUID playerId = playerUuid(player);
            if (playerId == null) return;
            LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;
            if (lobby.isSoloTester(playerId)) lobby.stopSoloTest(playerId);
            else lobby.removePlayer(playerId);
        }

        /** Core conserva la sesión al desconectar; solo se retira del lobby. */
        private void disconnectPlayer(Object player) {
            UUID playerId = playerUuid(player);
            if (playerId == null) return;
            LobbyManager lobby = ArlightTetrisMod.DEBUG_LOBBY;
            if (lobby.getState() != LobbyManager.State.IN_PROGRESS) {
                // No invoca endSession aquí: el Core restaurará al reconectar.
                lobby.removePlayer(playerId);
            }
        }

        private Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            if (type == double.class) return 0D;
            if (type == char.class) return '\0';
            return null;
        }
    }
}
