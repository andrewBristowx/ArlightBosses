package com.arlight.tetris.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Teclas reconfigurables para Tetris. */
public final class TetrisKeyBindings {

    private TetrisKeyBindings() {}

    public static final KeyMapping MOVE_LEFT = new KeyMapping(
            "key.arlighttetris.move_left", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_LEFT, "key.categories.arlighttetris");

    public static final KeyMapping MOVE_RIGHT = new KeyMapping(
            "key.arlighttetris.move_right", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT, "key.categories.arlighttetris");

    public static final KeyMapping SOFT_DROP = new KeyMapping(
            "key.arlighttetris.soft_drop", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_DOWN, "key.categories.arlighttetris");

    public static final KeyMapping HARD_DROP = new KeyMapping(
            "key.arlighttetris.hard_drop", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_SPACE, "key.categories.arlighttetris");

    public static final KeyMapping ROTATE_CW = new KeyMapping(
            "key.arlighttetris.rotate_cw", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_UP, "key.categories.arlighttetris");

    public static final KeyMapping ROTATE_CCW = new KeyMapping(
            "key.arlighttetris.rotate_ccw", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "key.categories.arlighttetris");

    public static final KeyMapping HOLD = new KeyMapping(
            "key.arlighttetris.hold", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, InputConstants.KEY_C, "key.categories.arlighttetris");
}
