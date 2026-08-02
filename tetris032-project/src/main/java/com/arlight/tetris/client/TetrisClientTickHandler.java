package com.arlight.tetris.client;

import com.arlight.tetris.network.GameAction;
import com.arlight.tetris.network.ServerboundGameActionPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Implementa DAS (Delayed Auto Shift) y ARR (Auto Repeat Rate) del lado
 * cliente para que el movimiento lateral se sienta tan ágil como en
 * tetr.io: al mantener una tecla, hay un delay inicial (DAS) y después
 * repite a intervalos fijos (ARR), en vez de depender del repeat del OS.
 *
 * Valores por defecto pensados para 20 ticks/segundo (tick rate del server
 * de Minecraft, que es el límite real de granularidad acá).
 */
@EventBusSubscriber(modid = "arlighttetris", value = Dist.CLIENT)
public final class TetrisClientTickHandler {

    private static final int DAS_TICKS = 3;
    private static final int ARR_TICKS = 1;
    private static final int SOFT_DROP_TICKS = 1;

    private static int leftHeldTicks = 0;
    private static int rightHeldTicks = 0;
    private static int softDropTicks = 0;

    private TetrisClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ClientGameState.isPlaying()) {
            leftHeldTicks = 0;
            rightHeldTicks = 0;
            softDropTicks = 0;
            return;
        }
        if (Minecraft.getInstance().screen != null) return;

        handleHeld(TetrisKeyBindings.MOVE_LEFT.isDown(), true);
        handleHeld(TetrisKeyBindings.MOVE_RIGHT.isDown(), false);
        handleSoftDrop(TetrisKeyBindings.SOFT_DROP.isDown());

        while (TetrisKeyBindings.ROTATE_CW.consumeClick()) sendAction(GameAction.ROTATE_CW);
        while (TetrisKeyBindings.ROTATE_CCW.consumeClick()) sendAction(GameAction.ROTATE_CCW);
        while (TetrisKeyBindings.HARD_DROP.consumeClick()) sendAction(GameAction.HARD_DROP);
        while (TetrisKeyBindings.HOLD.consumeClick()) sendAction(GameAction.HOLD);
    }

    private static void handleHeld(boolean isDown, boolean isLeft) {
        int ticks = isLeft ? leftHeldTicks : rightHeldTicks;

        if (!isDown) {
            if (isLeft) leftHeldTicks = 0; else rightHeldTicks = 0;
            return;
        }

        if (ticks == 0) {
            sendAction(isLeft ? GameAction.MOVE_LEFT : GameAction.MOVE_RIGHT);
        } else if (ticks >= DAS_TICKS && (ticks - DAS_TICKS) % ARR_TICKS == 0) {
            sendAction(isLeft ? GameAction.MOVE_LEFT : GameAction.MOVE_RIGHT);
        }

        ticks++;
        if (isLeft) leftHeldTicks = ticks; else rightHeldTicks = ticks;
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
