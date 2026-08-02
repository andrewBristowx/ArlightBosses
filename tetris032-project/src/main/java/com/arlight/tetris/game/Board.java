package com.arlight.tetris.game;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int WIDTH = 10;
    public static final int VISIBLE_HEIGHT = 20;
    public static final int BUFFER_HEIGHT = 4;
    public static final int TOTAL_HEIGHT = VISIBLE_HEIGHT + BUFFER_HEIGHT;

    private final TetrominoType[][] grid = new TetrominoType[TOTAL_HEIGHT][WIDTH];

    public boolean isInsideBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < TOTAL_HEIGHT;
    }

    public boolean isCellFree(int x, int y) {
        return isInsideBounds(x, y) && grid[y][x] == null;
    }

    public TetrominoType getCell(int x, int y) {
        if (!isInsideBounds(x, y)) return null;
        return grid[y][x];
    }

    public boolean canPlace(ActivePiece piece) {
        for (int[] cell : piece.getAbsoluteCells()) {
            int x = cell[0], y = cell[1];
            if (!isInsideBounds(x, y) || grid[y][x] != null) return false;
        }
        return true;
    }

    public void lockPiece(ActivePiece piece) {
        for (int[] cell : piece.getAbsoluteCells()) grid[cell[1]][cell[0]] = piece.getType();
    }

    public List<Integer> clearFullLines() {
        List<Integer> cleared = new ArrayList<>();
        for (int y = 0; y < TOTAL_HEIGHT; y++) {
            boolean full = true;
            for (int x = 0; x < WIDTH; x++) {
                if (grid[y][x] == null) { full = false; break; }
            }
            if (full) cleared.add(y);
        }
        if (cleared.isEmpty()) return cleared;
        TetrominoType[][] newGrid = new TetrominoType[TOTAL_HEIGHT][WIDTH];
        int writeRow = TOTAL_HEIGHT - 1;
        for (int y = TOTAL_HEIGHT - 1; y >= 0; y--) {
            if (cleared.contains(y)) continue;
            newGrid[writeRow--] = grid[y];
        }
        for (int y = 0; y <= writeRow; y++) newGrid[y] = new TetrominoType[WIDTH];
        System.arraycopy(newGrid, 0, grid, 0, TOTAL_HEIGHT);
        return cleared;
    }

    public void addGarbageLine(int holeColumn) {
        for (int y = 0; y < TOTAL_HEIGHT - 1; y++) grid[y] = grid[y + 1];
        TetrominoType[] row = new TetrominoType[WIDTH];
        for (int x = 0; x < WIDTH; x++) if (x != holeColumn) row[x] = TetrominoType.L;
        grid[TOTAL_HEIGHT - 1] = row;
    }

    public boolean isTopOut() {
        for (int x = 0; x < WIDTH; x++) if (grid[0][x] != null) return true;
        return false;
    }

    public byte[] encode() {
        byte[] out = new byte[WIDTH * TOTAL_HEIGHT];
        int i = 0;
        for (int y = 0; y < TOTAL_HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                TetrominoType t = grid[y][x];
                out[i++] = (byte) (t == null ? 0 : t.ordinal() + 1);
            }
        }
        return out;
    }

    public byte[] columnHeights() {
        byte[] heights = new byte[WIDTH];
        for (int x = 0; x < WIDTH; x++) {
            int height = 0;
            for (int y = 0; y < TOTAL_HEIGHT; y++) {
                if (grid[y][x] != null) { height = TOTAL_HEIGHT - y; break; }
            }
            heights[x] = (byte) height;
        }
        return heights;
    }
}
