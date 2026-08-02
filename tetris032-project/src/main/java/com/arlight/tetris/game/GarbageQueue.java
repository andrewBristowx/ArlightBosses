package com.arlight.tetris.game;

import java.util.Random;

public class GarbageQueue {
    private int pendingLines = 0;
    private final Random random;
    private int lastHoleColumn = -1;

    public GarbageQueue(long seed) { this.random = new Random(seed); }
    public void addIncoming(int lines) { pendingLines += lines; }

    public int cancelWith(int cleared) {
        int canceled = Math.min(cleared, pendingLines);
        pendingLines -= canceled;
        return cleared - canceled;
    }

    public boolean hasPending() { return pendingLines > 0; }

    public int drainAll() {
        int lines = pendingLines;
        pendingLines = 0;
        return lines;
    }

    public void applyTo(Board board) {
        int lines = drainAll();
        if (lines <= 0) return;
        if (lastHoleColumn == -1) lastHoleColumn = random.nextInt(Board.WIDTH);
        for (int i = 0; i < lines; i++) board.addGarbageLine(lastHoleColumn);
    }
}
