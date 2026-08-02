package com.arlight.tetris.multiplayer;

import com.arlight.tetris.game.GameSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maneja el ciclo de vida de UNA sala de ArlightTetris.
 *
 * El modo normal conserva el mínimo de dos jugadores. El modo de prueba
 * individual solo puede iniciarlo un comando administrativo y no entrega
 * victoria ni XP: sirve para comprobar HUD, controles, arena y red sin
 * necesitar una segunda cuenta.
 */
public class LobbyManager {

    public enum State { WAITING, COUNTDOWN, IN_PROGRESS, FINISHED }

    public interface Listener {
        default void onMatchStart(List<UUID> orderedPlayers) {}
        default void onMatchEnd(List<UUID> allPlayers, UUID winner) {}
        default void onPlayerLeft(UUID playerId) {}
    }

    private final Map<UUID, GameSession> sessions = new LinkedHashMap<>();
    private final List<Listener> listeners = new ArrayList<>();
    private State state = State.WAITING;
    private int countdownTicksRemaining = -1;
    private boolean matchEndFired = false;
    private boolean soloTestMode = false;

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public boolean addPlayer(UUID playerId) {
        if (playerId == null || state != State.WAITING) return false;
        if (sessions.size() >= MatchConfig.MAX_PLAYERS) return false;
        if (sessions.containsKey(playerId)) return false;

        sessions.put(playerId, null);
        maybeStartCountdown();
        return true;
    }

    /** Inicia inmediatamente una partida de prueba con un único jugador. */
    public boolean startSoloTest(UUID playerId) {
        if (playerId == null || state != State.WAITING || !sessions.isEmpty()) return false;
        sessions.put(playerId, null);
        soloTestMode = true;
        startMatch();
        return true;
    }

    /**
     * Detiene la prueba individual, dispara la restauración del jugador y deja
     * la sala lista para otra prueba sin esperar los ocho segundos del resultado.
     */
    public boolean stopSoloTest(UUID playerId) {
        if (!soloTestMode || playerId == null || !sessions.containsKey(playerId)) return false;
        List<UUID> all = new ArrayList<>(sessions.keySet());
        if (!matchEndFired) {
            matchEndFired = true;
            for (Listener listener : listeners) listener.onMatchEnd(all, null);
        }
        clearToWaiting();
        return true;
    }

    public boolean removePlayer(UUID playerId) {
        if (state == State.IN_PROGRESS) return false;
        boolean removed = sessions.containsKey(playerId);
        if (removed) {
            sessions.remove(playerId);
            for (Listener listener : listeners) listener.onPlayerLeft(playerId);
        }
        if (sessions.size() < MatchConfig.MIN_PLAYERS_TO_START) cancelCountdown();
        return removed;
    }

    private void maybeStartCountdown() {
        if (state == State.WAITING && sessions.size() >= MatchConfig.MIN_PLAYERS_TO_START) {
            state = State.COUNTDOWN;
            countdownTicksRemaining = MatchConfig.START_COUNTDOWN_SECONDS * 20;
        }
    }

    private void cancelCountdown() {
        if (state == State.COUNTDOWN) {
            state = State.WAITING;
            countdownTicksRemaining = -1;
        }
    }

    public void tick() {
        switch (state) {
            case COUNTDOWN -> {
                countdownTicksRemaining--;
                if (countdownTicksRemaining <= 0) startMatch();
            }
            case IN_PROGRESS -> tickMatch();
            default -> { }
        }
    }

    private void startMatch() {
        long seed = ThreadLocalRandom.current().nextLong();
        List<UUID> ordered = new ArrayList<>(sessions.keySet());
        for (UUID playerId : ordered) {
            sessions.put(playerId, new GameSession(playerId, seed ^ playerId.getMostSignificantBits()));
        }
        state = State.IN_PROGRESS;
        countdownTicksRemaining = -1;
        matchEndFired = false;
        for (Listener listener : listeners) listener.onMatchStart(ordered);
    }

    private void tickMatch() {
        for (Map.Entry<UUID, GameSession> entry : sessions.entrySet()) {
            GameSession session = entry.getValue();
            if (session != null && !session.isTopOut()) {
                session.tick();
                distributePendingAttack(entry.getKey());
            }
        }
        checkForWinner();
    }

    public void distributePendingAttack(UUID playerId) {
        GameSession session = sessions.get(playerId);
        if (session == null) return;
        int attack = session.pollPendingAttack();
        if (attack > 0) sendAttack(playerId, attack);
    }

    public void sendAttack(UUID attackerId, int lines) {
        if (lines <= 0) return;
        var alive = sessions.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(attackerId))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isTopOut())
                .toList();
        if (alive.isEmpty()) return;
        var target = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        target.getValue().receiveGarbage(lines);
    }

    private void checkForWinner() {
        List<Map.Entry<UUID, GameSession>> alive = sessions.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isTopOut())
                .toList();

        // Una prueba individual continúa mientras el jugador siga vivo. Al
        // perder, termina sin ganador para que nunca otorgue XP de victoria.
        if (soloTestMode) {
            if (alive.isEmpty() && !matchEndFired) finishMatch(null);
            return;
        }

        if (alive.size() <= 1 && !matchEndFired) {
            UUID winner = alive.size() == 1 ? alive.get(0).getKey() : null;
            finishMatch(winner);
        }
    }

    private void finishMatch(UUID winner) {
        state = State.FINISHED;
        matchEndFired = true;
        List<UUID> all = new ArrayList<>(sessions.keySet());
        for (Listener listener : listeners) listener.onMatchEnd(all, winner);
    }

    public void resetForNextMatch() {
        clearToWaiting();
    }

    private void clearToWaiting() {
        sessions.clear();
        state = State.WAITING;
        countdownTicksRemaining = -1;
        matchEndFired = false;
        soloTestMode = false;
    }

    public State getState() { return state; }
    public int getPlayerCount() { return sessions.size(); }
    public GameSession getSession(UUID playerId) { return sessions.get(playerId); }
    public Map<UUID, GameSession> getSessions() { return java.util.Collections.unmodifiableMap(sessions); }
    public boolean containsPlayer(UUID playerId) { return sessions.containsKey(playerId); }
    public boolean isSoloTestMode() { return soloTestMode; }
    public boolean isSoloTester(UUID playerId) { return soloTestMode && sessions.containsKey(playerId); }

    public int getCountdownSecondsRemaining() {
        if (state != State.COUNTDOWN) return -1;
        return (countdownTicksRemaining + 19) / 20;
    }
}
