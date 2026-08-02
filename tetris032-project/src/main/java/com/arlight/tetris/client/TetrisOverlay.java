package com.arlight.tetris.client;

import com.arlight.tetris.game.Board;
import com.arlight.tetris.game.TetrominoType;
import com.arlight.tetris.network.ClientboundBoardStatePacket;
import com.arlight.tetris.network.ClientboundOpponentsStatePacket.OpponentSummary;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TetrisOverlay implements LayeredDraw.Layer {

    private static final int CELL = 18;
    private static final int BOARD_LEFT = 40;
    private static final int BOARD_TOP = 30;

    private static final int[] COLORS = {
            0xFF00FFFF, 0xFFFFFF00, 0xFFA000FF, 0xFF00FF00,
            0xFFFF3030, 0xFF3060FF, 0xFFFFA000
    };

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientGameState.isInLobbyOrMatch()) return;
        int screenWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        drawMatchBanner(graphics, screenWidth);
        ClientboundBoardStatePacket board = ClientGameState.board;
        if (board == null) return;
        drawBoard(graphics, board);
        drawGhostAndCurrent(graphics, board);
        drawHold(graphics, board);
        drawNextQueue(graphics, board);
        drawStats(graphics, board);
        drawOpponents(graphics, screenWidth);
        drawControls(graphics, screenWidth);
    }

    private void drawMatchBanner(GuiGraphics graphics, int screenWidth) {
        var match = ClientGameState.match;
        if (match == null) return;
        String text = switch (match.stateId()) {
            case 0 -> "Esperando jugadores (" + match.playerCount() + "/" + match.minPlayers() + " mínimo)";
            case 1 -> "Arrancando en " + match.countdownSecondsRemaining() + "s...";
            case 3 -> match.youWon() ? "¡Ganaste!" : (match.youAreAlive() ? "Partida terminada" : "Eliminado");
            default -> "";
        };
        if (!text.isEmpty()) {
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    Component.literal(text), screenWidth / 2, 8, 0xFFFFFF);
        }
    }

    private void drawBoard(GuiGraphics graphics, ClientboundBoardStatePacket board) {
        byte[] grid = board.grid();
        for (int y = Board.BUFFER_HEIGHT; y < Board.TOTAL_HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                int value = grid[y * Board.WIDTH + x];
                int screenY = BOARD_TOP + (y - Board.BUFFER_HEIGHT) * CELL;
                int screenX = BOARD_LEFT + x * CELL;
                int color = value == 0 ? 0x30FFFFFF : COLORS[value - 1];
                graphics.fill(screenX, screenY, screenX + CELL - 1, screenY + CELL - 1, color);
            }
        }
        graphics.renderOutline(BOARD_LEFT - 1, BOARD_TOP - 1,
                Board.WIDTH * CELL + 2, Board.VISIBLE_HEIGHT * CELL + 2, 0xFFFFFFFF);
    }

    private void drawGhostAndCurrent(GuiGraphics graphics, ClientboundBoardStatePacket board) {
        TetrominoType type = TetrominoType.values()[board.currentType()];
        int color = COLORS[board.currentType()];
        for (int[] cell : type.cells[board.currentRotation()]) {
            int gx = board.currentX() + cell[0];
            drawCellIfVisible(graphics, gx, board.ghostY() + cell[1],
                    (color & 0x00FFFFFF) | 0x50000000);
            drawCellIfVisible(graphics, gx, board.currentY() + cell[1], color);
        }
    }

    private void drawCellIfVisible(GuiGraphics graphics, int gx, int gy, int color) {
        if (gy < Board.BUFFER_HEIGHT) return;
        int screenX = BOARD_LEFT + gx * CELL;
        int screenY = BOARD_TOP + (gy - Board.BUFFER_HEIGHT) * CELL;
        graphics.fill(screenX, screenY, screenX + CELL - 1, screenY + CELL - 1, color);
    }

    private void drawHold(GuiGraphics graphics, ClientboundBoardStatePacket board) {
        int boxX = BOARD_LEFT - 70;
        int boxY = BOARD_TOP;
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "HOLD", boxX, boxY - 10, 0xFFFFFF);
        graphics.renderOutline(boxX, boxY, 56, 40, 0xFFAAAAAA);
        if (board.holdType() >= 0) {
            drawMiniPiece(graphics, TetrominoType.values()[board.holdType()], boxX + 8, boxY + 8, COLORS[board.holdType()]);
        }
    }

    private void drawNextQueue(GuiGraphics graphics, ClientboundBoardStatePacket board) {
        int boxX = BOARD_LEFT + Board.WIDTH * CELL + 14;
        int boxY = BOARD_TOP;
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "NEXT", boxX, boxY - 10, 0xFFFFFF);
        byte[] next = board.nextQueue();
        for (int i = 0; i < next.length; i++) {
            int type = next[i];
            int y = boxY + i * 44;
            graphics.renderOutline(boxX, y, 56, 40, 0xFF888888);
            drawMiniPiece(graphics, TetrominoType.values()[type], boxX + 8, y + 8, COLORS[type]);
        }
    }

    private void drawMiniPiece(GuiGraphics graphics, TetrominoType type, int x, int y, int color) {
        int size = 9;
        for (int[] cell : type.cells[0]) {
            graphics.fill(x + cell[0] * size, y + cell[1] * size,
                    x + cell[0] * size + size - 1, y + cell[1] * size + size - 1, color);
        }
    }

    private void drawStats(GuiGraphics graphics, ClientboundBoardStatePacket board) {
        int textX = BOARD_LEFT;
        int textY = BOARD_TOP + Board.VISIBLE_HEIGHT * CELL + 8;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, "Líneas: " + board.linesCleared(), textX, textY, 0xFFFFFF);
        if (board.combo() > 0) graphics.drawString(font, "Combo x" + board.combo(), textX, textY + 10, 0xFFD060);
        if (board.backToBack()) graphics.drawString(font, "Back-to-Back", textX, textY + 20, 0x60D0FF);
    }

    private void drawControls(GuiGraphics graphics, int screenWidth) {
        if (!ClientGameState.isPlaying()) return;
        int screenHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawCenteredString(font,
                Component.literal("A/D o ←/→ mover · S o ↓ bajar · W o ↑ rotar · Espacio caída"),
                screenWidth / 2, screenHeight - 34, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("Z giro izquierdo · C guardar · Esc disponible"),
                screenWidth / 2, screenHeight - 22, 0xFFD8A8FF);
    }

    private void drawOpponents(GuiGraphics graphics, int screenWidth) {
        List<OpponentSummary> opponents = ClientGameState.opponents;
        if (opponents.isEmpty()) return;
        int x = screenWidth - 90;
        int y = 30;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, "Rivales", x, y - 12, 0xFFFFFF);
        for (OpponentSummary opp : opponents) {
            int color = opp.alive() ? 0xFFFFFFFF : 0xFF808080;
            graphics.drawString(font, opp.name() + " (" + opp.linesCleared() + ")", x, y, color);
            for (int col = 0; col < opp.columnHeights().length; col++) {
                int h = Math.min(opp.columnHeights()[col], (byte) 20);
                int barX = x + col * 7;
                int barTop = y + 12 + (20 - h);
                graphics.fill(barX, barTop, barX + 6, y + 32,
                        opp.alive() ? 0xFF60C0FF : 0xFF505050);
            }
            y += 46;
        }
    }
}
