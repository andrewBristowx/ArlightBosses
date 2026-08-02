package com.arlight.tetris.game;

public class ActivePiece {
    private final TetrominoType type;
    private int x;
    private int y;
    private int rotation;
    private boolean lastMoveWasRotation = false;
    private boolean lastRotationUsedKick = false;

    public ActivePiece(TetrominoType type, int spawnX, int spawnY) {
        this.type = type;
        this.x = spawnX;
        this.y = spawnY;
        this.rotation = 0;
    }

    public TetrominoType getType() { return type; }
    public int getRotation() { return rotation; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean lastMoveWasRotation() { return lastMoveWasRotation; }
    public boolean lastRotationUsedKick() { return lastRotationUsedKick; }

    public int[][] getAbsoluteCells() {
        int[][] shape = type.cells[rotation];
        int[][] result = new int[shape.length][2];
        for (int i = 0; i < shape.length; i++) {
            result[i][0] = x + shape[i][0];
            result[i][1] = y + shape[i][1];
        }
        return result;
    }

    public boolean tryMove(Board board, int dx, int dy) {
        int oldX = x, oldY = y;
        x += dx;
        y += dy;
        if (!board.canPlace(this)) {
            x = oldX;
            y = oldY;
            return false;
        }
        lastMoveWasRotation = false;
        return true;
    }

    public boolean tryRotate(Board board, int direction) {
        int from = rotation;
        int to = Math.floorMod(rotation + direction, 4);
        int[][] kicks = SRSData.getKicks(type, from, to);
        int oldX = x, oldY = y, oldRot = rotation;
        rotation = to;
        for (int[] kick : kicks) {
            x = oldX + kick[0];
            y = oldY - kick[1];
            if (board.canPlace(this)) {
                lastMoveWasRotation = true;
                lastRotationUsedKick = !(kick[0] == 0 && kick[1] == 0);
                return true;
            }
        }
        x = oldX;
        y = oldY;
        rotation = oldRot;
        return false;
    }

    public int distanceToGround(Board board) {
        int dropDistance = 0;
        while (true) {
            y += 1;
            if (!board.canPlace(this)) {
                y -= 1;
                break;
            }
            dropDistance++;
        }
        y -= dropDistance;
        return dropDistance;
    }

    public ActivePiece copy() {
        ActivePiece p = new ActivePiece(type, x, y);
        p.rotation = rotation;
        return p;
    }
}
