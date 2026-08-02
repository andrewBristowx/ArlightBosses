package com.arlight.tetris.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;

/**
 * Teclas reconfigurables de Tetris.
 *
 * El contexto solo se activa durante una partida y cuando no hay una
 * pantalla de Minecraft abierta. Así las mismas teclas pueden conservar
 * sus funciones normales fuera de Tetris sin producir conflictos.
 */
public final class TetrisKeyBindings {

    private TetrisKeyBindings() {}

    private static final IKeyConflictContext TETRIS_ONLY = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            Minecraft minecraft = Minecraft.getInstance();
            return ClientGameState.isPlaying() && minecraft.screen == null;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this;
        }
    };

    public static final KeyMapping MOVE_LEFT = new KeyMapping(
            "key.arlighttetris.move_left", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_LEFT, "key.categories.arlighttetris");

    public static final KeyMapping MOVE_RIGHT = new KeyMapping(
            "key.arlighttetris.move_right", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT, "key.categories.arlighttetris");

    public static final KeyMapping SOFT_DROP = new KeyMapping(
            "key.arlighttetris.soft_drop", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_DOWN, "key.categories.arlighttetris");

    public static final KeyMapping HARD_DROP = new KeyMapping(
            "key.arlighttetris.hard_drop", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_SPACE, "key.categories.arlighttetris");

    public static final KeyMapping ROTATE_CW = new KeyMapping(
            "key.arlighttetris.rotate_cw", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_UP, "key.categories.arlighttetris");

    public static final KeyMapping ROTATE_CCW = new KeyMapping(
            "key.arlighttetris.rotate_ccw", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "key.categories.arlighttetris");

    public static final KeyMapping HOLD = new KeyMapping(
            "key.arlighttetris.hold", TETRIS_ONLY,
            InputConstants.Type.KEYSYM, InputConstants.KEY_C, "key.categories.arlighttetris");
}
