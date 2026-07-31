package com.arlight.bosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.Locale;

/** Reemplaza únicamente las barras de ArlightBosses por diseños temáticos. */
public final class EpicBossBarOverlay {
    private EpicBossBarOverlay() { }

    @SubscribeEvent
    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        String name = boss.getName().getString();
        Style style = styleFor(name);
        if (style == null) return;

        event.setCanceled(true);
        event.setIncrement(27);

        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int width = 190;
        int height = 12;
        int x = (g.guiWidth() - width) / 2;
        int y = event.getY() + 7;
        double progress = Math.max(0.0D, Math.min(1.0D, boss.getProgress()));

        drawTheme(g, x, y, width, height, progress, style);
        Component title = Component.literal(style.symbol + " " + name + " " + style.symbol);
        g.drawCenteredString(mc.font, title, g.guiWidth() / 2, y - 11, style.titleColor);
    }

    private static void drawTheme(GuiGraphics g, int x, int y, int width, int height,
                                  double progress, Style style) {
        // Sombra, marco y pista oscura.
        fillPill(g, x - 3, y - 3, width + 6, height + 6, 0xA0000000);
        fillPill(g, x - 2, y - 2, width + 4, height + 4, style.borderDark);
        fillPill(g, x, y, width, height, style.track);

        int filled = (int) Math.round(width * progress);
        if (filled > 0) {
            fillPill(g, x, y, Math.max(height, filled), height, style.fillDark);
            if (filled > 5) fillPill(g, x + 2, y + 2,
                    Math.max(height - 4, filled - 4), Math.max(2, height - 4), style.fillBright);
        }

        switch (style) {
            case SURFACE -> drawMoss(g, x, y, width, height, filled);
            case NETHER -> drawLava(g, x, y, width, height, filled);
            case VOID -> drawVoid(g, x, y, width, height, filled);
            case DRAGON -> drawDragon(g, x, y, width, height, filled);
        }
    }

    private static void drawMoss(GuiGraphics g, int x, int y, int width, int height, int filled) {
        for (int px = 8; px < width - 5; px += 19) {
            int color = px < filled ? 0xFFB6E36A : 0xFF4B5A36;
            g.fill(x + px, y - 2, x + px + 5, y, color);
            if ((px / 19) % 2 == 0) g.fill(x + px + 2, y, x + px + 4, y + 3, color);
        }
        g.fill(x + 5, y + height, x + Math.min(width - 5, 30), y + height + 2, 0xFF6D8F3B);
    }

    private static void drawLava(GuiGraphics g, int x, int y, int width, int height, int filled) {
        for (int px = 7; px < Math.min(width - 4, filled); px += 14) {
            int flameH = 2 + (px % 5);
            g.fill(x + px, y + height - flameH, x + px + 3, y + height, 0xFFFFD45A);
        }
        for (int px = 13; px < width - 5; px += 31) {
            g.fill(x + px, y - 2, x + px + 7, y, 0xFF5A1A10);
        }
    }

    private static void drawVoid(GuiGraphics g, int x, int y, int width, int height, int filled) {
        for (int px = 9; px < width - 4; px += 17) {
            int color = px < filled ? 0xFFD99BFF : 0xFF45315C;
            int py = ((px / 17) % 2 == 0) ? y + 2 : y + height - 3;
            g.fill(x + px, py, x + px + 2, py + 2, color);
        }
        g.fill(x + width / 2 - 2, y - 3, x + width / 2 + 2, y + 1, 0xFFB650FF);
    }

    private static void drawDragon(GuiGraphics g, int x, int y, int width, int height, int filled) {
        for (int px = 6; px < width - 4; px += 12) {
            int color = px < filled ? 0xFFFFC857 : 0xFF4E4158;
            g.fill(x + px, y - 1, x + px + 5, y + 1, color);
        }
        g.fill(x + width / 2 - 5, y + height - 1, x + width / 2 + 5, y + height + 2, 0xFF8B5EEA);
    }

    private static void fillPill(GuiGraphics g, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        double radius = height / 2.0D;
        for (int row = 0; row < height; row++) {
            double dy = Math.abs((row + 0.5D) - radius);
            int inset = Math.max(0, (int) Math.ceil(radius
                    - Math.sqrt(Math.max(0.0D, radius * radius - dy * dy))));
            int maxInset = Math.max(0, width / 2);
            int left = x + Math.min(inset, maxInset);
            int right = x + width - Math.min(inset, maxInset);
            if (right > left) g.fill(left, y + row, right, y + row + 1, color);
        }
    }

    private static Style styleFor(String name) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (!value.contains("guardián") && !value.contains("guardian")) return null;
        if (value.contains("superficie") || value.contains("surface") || value.contains("tierra")) return Style.SURFACE;
        if (value.contains("nether")) return Style.NETHER;
        if (value.contains("vacío") || value.contains("vacio") || value.contains("void") || value.contains("end")) return Style.VOID;
        if (value.contains("drac") || value.contains("dragon")) return Style.DRAGON;
        return null;
    }

    private enum Style {
        SURFACE("❈", 0xFFCCF59A, 0xFF263118, 0xFF11170D, 0xFF416422, 0xFF83C94B),
        NETHER("◆", 0xFFFFB36A, 0xFF4A120B, 0xFF170806, 0xFF9B210E, 0xFFFF6128),
        VOID("✦", 0xFFE4B2FF, 0xFF2B123E, 0xFF0D0712, 0xFF57217A, 0xFFB44CFF),
        DRAGON("✧", 0xFFFFDE78, 0xFF24172E, 0xFF0B0810, 0xFF5B3B87, 0xFFFFB73E);

        final String symbol;
        final int titleColor;
        final int borderDark;
        final int track;
        final int fillDark;
        final int fillBright;

        Style(String symbol, int titleColor, int borderDark, int track, int fillDark, int fillBright) {
            this.symbol = symbol;
            this.titleColor = titleColor;
            this.borderDark = borderDark;
            this.track = track;
            this.fillDark = fillDark;
            this.fillBright = fillBright;
        }
    }
}
