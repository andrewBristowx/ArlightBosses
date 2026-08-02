package com.arlight.tetris.client;

import com.arlight.tetris.network.GameAction;
import com.arlight.tetris.network.ServerboundGameActionPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Captura los controles de Tetris antes de que Minecraft los use.
 * Incluye lectura directa de Espacio, C y Z como respaldo para evitar que
 * otros mods o los controles vanilla consuman la pulsación.
 */
@EventBusSubscriber(modid = "arlighttetris", value = Dist.CLIENT)
public final class TetrisClientTickHandler {

    private static final int DAS_TICKS = 3;
    private static final int ARR_TICKS = 1;
    private static final int SOFT_DROP_TICKS = 1;

    private static int leftHeldTicks;
    private static int rightHeldTicks;
    private static int softDropTicks;

    private static boolean vanillaLeftDown;
    private static boolean vanillaRightDown;
    private static boolean vanillaSoftDropDown;
    private static int vanillaRotateClicks;

    private static boolean rawHardDropWasDown;
    private static boolean rawHoldWasDown;
    private static boolean rawRotateCcwWasDown;
    private static boolean rawHardDropPressed;
    private static boolean rawHoldPressed;
    private static boolean rawRotateCcwPressed;

    private TetrisClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (!shouldCapture(minecraft, player)) {
            resetRawState();
            return;
        }

        Options options = minecraft.options;
        long window = minecraft.getWindow().getWindow();

        vanillaLeftDown = options.keyLeft.isDown();
        vanillaRightDown = options.keyRight.isDown();
        vanillaSoftDropDown = options.keyDown.isDown();
        vanillaRotateClicks += drainClicks(options.keyUp);

        boolean hardDropDown = InputConstants.isKeyDown(window, InputConstants.KEY_SPACE);
        boolean holdDown = InputConstants.isKeyDown(window, InputConstants.KEY_C);
        boolean rotateCcwDown = InputConstants.isKeyDown(window, InputConstants.KEY_Z);

        rawHardDropPressed = hardDropDown && !rawHardDropWasDown;
        rawHoldPressed = holdDown && !rawHoldWasDown;
        rawRotateCcwPressed = rotateCcwDown && !rawRotateCcwWasDown;

        rawHardDropWasDown = hardDropDown;
        rawHoldWasDown = holdDown;
        rawRotateCcwWasDown = rotateCcwDown;

        suppressVanillaControls(options);
        stopPlayerMovement(player);
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (!shouldCapture(minecraft, player)) {
            resetRepeatState();
            return;
        }

        handleHeld(TetrisKeyBindings.MOVE_LEFT.isDown() || vanillaLeftDown, true);
        handleHeld(TetrisKeyBindings.MOVE_RIGHT.isDown() || vanillaRightDown, false);
        handleSoftDrop(TetrisKeyBindings.SOFT_DROP.isDown() || vanillaSoftDropDown);

        boolean rotatedClockwise = drainClicks(TetrisKeyBindings.ROTATE_CW) > 0;
        if (rotatedClockwise) {
            sendAction(GameAction.ROTATE_CW);
        } else if (vanillaRotateClicks > 0) {
            sendAction(GameAction.ROTATE_CW);
        }
        vanillaRotateClicks = 0;

        if (drainClicks(TetrisKeyBindings.ROTATE_CCW) > 0 || rawRotateCcwPressed) {
            sendAction(GameAction.ROTATE_CCW);
        }
        if (drainClicks(TetrisKeyBindings.HARD_DROP) > 0 || rawHardDropPressed) {
            sendAction(GameAction.HARD_DROP);
        }
        if (drainClicks(TetrisKeyBindings.HOLD) > 0 || rawHoldPressed) {
            sendAction(GameAction.HOLD);
        }

        rawHardDropPressed = false;
        rawHoldPressed = false;
        rawRotateCcwPressed = false;

        stopPlayerMovement(player);
    }

    private static boolean shouldCapture(Minecraft minecraft, LocalPlayer player) {
        return player != null && minecraft.screen == null && ClientGameState.isPlaying();
    }

    private static void suppressVanillaControls(Options options) {
        release(options.keyUp);
        release(options.keyDown);
        release(options.keyLeft);
        release(options.keyRight);
        release(options.keyJump);
        release(options.keyShift);
        release(options.keySprint);
        release(options.keyAttack);
        release(options.keyUse);
        release(options.keyPickItem);
        release(options.keyDrop);
        release(options.keySwapOffhand);
        release(options.keyInventory);
    }

    private static void release(KeyMapping mapping) {
        mapping.setDown(false);
        while (mapping.consumeClick()) {
            // Vacía acciones normales pendientes.
        }
    }

    private static int drainClicks(KeyMapping mapping) {
        int clicks = 0;
        while (mapping.consumeClick()) clicks++;
        return clicks;
    }

    private static void stopPlayerMovement(LocalPlayer player) {
        player.input.leftImpulse = 0.0F;
        player.input.forwardImpulse = 0.0F;
        player.input.jumping = false;
        player.input.shiftKeyDown = false;
        player.setSprinting(false);

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, velocity.y, 0.0D);
    }

    private static void resetRawState() {
        vanillaLeftDown = false;
        vanillaRightDown = false;
        vanillaSoftDropDown = false;
        vanillaRotateClicks = 0;
        rawHardDropWasDown = false;
        rawHoldWasDown = false;
        rawRotateCcwWasDown = false;
        rawHardDropPressed = false;
        rawHoldPressed = false;
        rawRotateCcwPressed = false;
    }

    private static void resetRepeatState() {
        leftHeldTicks = 0;
        rightHeldTicks = 0;
        softDropTicks = 0;
        resetRawState();
    }

    private static void handleHeld(boolean isDown, boolean isLeft) {
        int ticks = isLeft ? leftHeldTicks : rightHeldTicks;

        if (!isDown) {
            if (isLeft) leftHeldTicks = 0;
            else rightHeldTicks = 0;
            return;
        }

        if (ticks == 0 || (ticks >= DAS_TICKS && (ticks - DAS_TICKS) % ARR_TICKS == 0)) {
            sendAction(isLeft ? GameAction.MOVE_LEFT : GameAction.MOVE_RIGHT);
        }

        ticks++;
        if (isLeft) leftHeldTicks = ticks;
        else rightHeldTicks = ticks;
    }

    private static void handleSoftDrop(boolean isDown) {
        if (!isDown) {
            softDropTicks = 0;
            return;
        }
        if (softDropTicks % SOFT_DROP_TICKS == 0) {
            sendAction(GameAction.SOFT_DROP);
        }
        softDropTicks++;
    }

    private static void sendAction(GameAction action) {
        PacketDistributor.sendToServer(new ServerboundGameActionPacket(action.ordinal()));
    }
}
