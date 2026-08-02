package com.arlight.tetris.game;

import java.util.List;
import java.util.UUID;

public class GameSession {
    public final UUID playerId;
    public final Board board = new Board();
    private final Bag7Randomizer randomizer;
    private final LockDelayHandler lockDelay = new LockDelayHandler();
    private final GarbageQueue garbageQueue;

    private ActivePiece current;
    private TetrominoType holdPiece = null;
    private boolean holdUsedThisPiece = false;
    private int comboCount = -1;
    private boolean backToBack = false;
    private boolean topOut = false;
    private int pendingAttackToSend = 0;
    private int linesCleared = 0;
    private int piecesPlaced = 0;

    public GameSession(UUID playerId, long seed) {
        this.playerId = playerId;
        this.randomizer = new Bag7Randomizer(seed);
        this.garbageQueue = new GarbageQueue(seed);
        spawnNextPiece();
    }

    private void spawnNextPiece() {
        TetrominoType type = randomizer.next();
        int spawnX = (Board.WIDTH - type.boxSize) / 2;
        current = new ActivePiece(type, spawnX, Board.BUFFER_HEIGHT - 2);
        holdUsedThisPiece = false;
        lockDelay.reset();
        if (!board.canPlace(current)) topOut = true;
    }

    public List<TetrominoType> getNextQueue(int count) { return randomizer.peek(count); }
    public ActivePiece getCurrentPiece() { return current; }
    public TetrominoType getHoldPiece() { return holdPiece; }
    public boolean isTopOut() { return topOut; }

    public boolean move(int dx, int dy) {
        boolean moved = current.tryMove(board, dx, dy);
        if (moved) lockDelay.onSuccessfulAction();
        return moved;
    }

    public boolean rotate(int direction) {
        boolean rotated = current.tryRotate(board, direction);
        if (rotated) lockDelay.onSuccessfulAction();
        return rotated;
    }

    public void hold() {
        if (holdUsedThisPiece || topOut) return;
        TetrominoType currentType = current.getType();
        if (holdPiece == null) {
            holdPiece = currentType;
            spawnNextPiece();
        } else {
            TetrominoType swap = holdPiece;
            holdPiece = currentType;
            int spawnX = (Board.WIDTH - swap.boxSize) / 2;
            current = new ActivePiece(swap, spawnX, Board.BUFFER_HEIGHT - 2);
            lockDelay.reset();
        }
        holdUsedThisPiece = true;
    }

    public int hardDrop() {
        int distance = current.distanceToGround(board);
        current.tryMove(board, 0, distance);
        return lockCurrentPiece(distance * 2);
    }

    public void tick() {
        if (topOut) return;
        boolean movedDown = current.tryMove(board, 0, 1);
        boolean grounded;
        if (movedDown) {
            current.tryMove(board, 0, -1);
            grounded = false;
        } else grounded = true;
        lockDelay.update(grounded);
        if (lockDelay.shouldLock()) lockCurrentPiece(0);
    }

    private int lockCurrentPiece(int dropBonus) {
        boolean isTSpin = detectTSpin();
        board.lockPiece(current);
        List<Integer> cleared = board.clearFullLines();
        piecesPlaced++;
        int attackLines = 0;
        int score = dropBonus;

        if (!cleared.isEmpty()) {
            comboCount++;
            linesCleared += cleared.size();
            boolean isTetris = cleared.size() == 4;
            boolean qualifiesB2B = isTetris || isTSpin;
            attackLines = computeAttack(cleared.size(), isTSpin, qualifiesB2B && backToBack, comboCount);
            score += computeScore(cleared.size(), isTSpin, qualifiesB2B && backToBack);
            backToBack = qualifiesB2B;
            if (isBoardEmpty()) {
                attackLines += 10;
                score += 3000;
            }
            garbageQueue.cancelWith(cleared.size());
            if (attackLines > 0) pendingAttackToSend += attackLines;
        } else {
            comboCount = -1;
            garbageQueue.applyTo(board);
        }

        if (board.isTopOut()) topOut = true;
        else spawnNextPiece();
        return score;
    }

    private boolean detectTSpin() {
        if (current.getType() != TetrominoType.T || !current.lastMoveWasRotation()) return false;
        int x = current.getX(), y = current.getY();
        int[][] corners = {{x, y}, {x + 2, y}, {x, y + 2}, {x + 2, y + 2}};
        int occupied = 0;
        for (int[] c : corners) if (!board.isCellFree(c[0], c[1])) occupied++;
        return occupied >= 3;
    }

    private boolean isBoardEmpty() {
        for (int y = 0; y < Board.TOTAL_HEIGHT; y++)
            for (int x = 0; x < Board.WIDTH; x++)
                if (board.getCell(x, y) != null) return false;
        return true;
    }

    private int computeAttack(int lines, boolean tSpin, boolean b2b, int combo) {
        int base;
        if (tSpin) {
            base = switch (lines) { case 1 -> 2; case 2 -> 4; case 3 -> 6; default -> 0; };
        } else {
            base = switch (lines) { case 1 -> 0; case 2 -> 1; case 3 -> 2; case 4 -> 4; default -> 0; };
        }
        if (b2b) base++;
        if (combo > 0) base += comboBonus(combo);
        return base;
    }

    private int comboBonus(int combo) {
        if (combo <= 1) return 0;
        if (combo <= 3) return 1;
        if (combo <= 5) return 2;
        if (combo <= 7) return 3;
        return 4;
    }

    private int computeScore(int lines, boolean tSpin, boolean b2b) {
        int base;
        if (tSpin) {
            base = switch (lines) { case 0 -> 400; case 1 -> 800; case 2 -> 1200; case 3 -> 1600; default -> 0; };
        } else {
            base = switch (lines) { case 1 -> 100; case 2 -> 300; case 3 -> 500; case 4 -> 800; default -> 0; };
        }
        return b2b ? (int) (base * 1.5) : base;
    }

    public void receiveGarbage(int lines) { garbageQueue.addIncoming(lines); }
    public int pollPendingAttack() { int n = pendingAttackToSend; pendingAttackToSend = 0; return n; }
    public int getLinesCleared() { return linesCleared; }
    public int getPiecesPlaced() { return piecesPlaced; }
    public int getComboCount() { return comboCount; }
    public boolean isBackToBack() { return backToBack; }
    public int getGhostY() { return current.getY() + current.distanceToGround(board); }

    public byte[] encodeNextQueue(int count) {
        List<TetrominoType> next = getNextQueue(count);
        byte[] out = new byte[next.size()];
        for (int i = 0; i < next.size(); i++) out[i] = (byte) next.get(i).ordinal();
        return out;
    }
}
