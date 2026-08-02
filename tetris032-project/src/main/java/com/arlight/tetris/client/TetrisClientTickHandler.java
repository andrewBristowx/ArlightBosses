package com.arlight.tetris.client;

import com.arlight.tetris.network.GameAction;
import com.arlight.tetris.network.ServerboundGameActionPacket;
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
 * Lee los controles de Tetris y captura los controles normales de Minecraft
 * mientras hay una partida activa.
 *
 * Además de las flechas configurables, acepta los controles de movimiento
 * actuales del jugador como alias: izquierda/derecha/atrás/adelante suelen
 * ser A/D/S/W. Espacio usa el mismo botón configurado para saltar.
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
    private static int vanillaHardDropClicks;

    private TetrisClientTickHandler() {}

    /**
     * Se ejecuta antes del tick del jugador. Primero guarda las pulsaciones
     * que Tetris necesita y después desactiva los controles vanilla, evitando
     * que el personaje camine, salte, corra, ataque o abra el inventario.
     */
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (!shouldCapture(minecraft, player)) {
            vanillaLeftDown = false;
            vanillaRightDown = false;
            vanillaSoftDropDown = false;
            vanillaRotateClicks = 0;
            vanillaHardDropClicks = 0;
            return;
        }

        Options options = minecraft.options;

        // WASD (o las teclas de movimiento configuradas por el jugador)
        // también controlan Tetris durante la partida.
        vanillaLeftDown = options.keyLeft.isDown();
        vanillaRightDown = options.keyRight.isDown();
        vanillaSoftDropDown = options.keyDown.isDown();
        vanillaRotateClicks += drainClicks(options.keyUp);
        vanillaHardDropClicks += drainClicks(options.keyJump);

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

        while (TetrisKeyBindings.ROTATE_CW.consumeClick()) {
            sendAction(GameAction.ROTATE_CW);
        }
        while (vanillaRotateClicks-- > 0) {
            sendAction(GameAction.ROTATE_CW);
        }
        vanillaRotateClicks = 0;

        while (TetrisKeyBindings.ROTATE_CCW.consumeClick()) {
            sendAction(GameAction.ROTATE_CCW);
        }
        while (TetrisKeyBindings.HARD_DROP.consumeClick()) {
            sendAction(GameAction.HARD_DROP);
        }
        while (vanillaHardDropClicks-- > 0) {
            sendAction(GameAction.HARD_DROP);
        }
        vanillaHardDropClicks = 0;

        while (TetrisKeyBindings.HOLD.consumeClick()) {
            sendAction(GameAction.HOLD);
        }

        // Elimina cualquier impulso horizontal que otro mod o el tick del
        // jugador haya agregado después de la captura previa.
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
            // Vacía pulsaciones pendientes para que no se ejecuten después.
        }
    }

    private static int drainClicks(KeyMapping mapping) {
        int clicks = 0;
        while (mapping.consumeClick()) {
            clicks++;
        }
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

    private static void resetRepeatState() {
        leftHeldTicks = 0;
        rightHeldTicks = 0;
        softDropTicks = 0;
        vanillaLeftDown = false;
        vanillaRightDown = false;
        vanillaSoftDropDown = false;
        vanillaRotateClicks = 0;
        vanillaHardDropClicks = 0;
    }

    private static void handleHeld(boolean isDown, boolean isLeft) {
        int ticks = isLeft ? leftHeldTicks : rightHeldTicks;

        if (!isDown) {
            if (isLeft) {
                leftHeldTicks = 0;
            } else {
                rightHeldTicks = 0;
            }
            return;
        }

        if (ticks == 0 || (ticks >= DAS_TICKS && (ticks - DAS_TICKS) % ARR_TICKS == 0)) {
            sendAction(isLeft ? GameAction.MOVE_LEFT : GameAction.MOVE_RIGHT);
        }

        ticks++;
        if (isLeft) {
            leftHeldTicks = ticks;
        } else {
            rightHeldTicks = ticks;
        }
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
