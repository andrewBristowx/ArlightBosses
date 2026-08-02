package com.arlight.tetris.client;

import com.arlight.tetris.ArlightTetrisMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = ArlightTetrisMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TetrisKeyBindings.MOVE_LEFT);
        event.register(TetrisKeyBindings.MOVE_RIGHT);
        event.register(TetrisKeyBindings.SOFT_DROP);
        event.register(TetrisKeyBindings.HARD_DROP);
        event.register(TetrisKeyBindings.ROTATE_CW);
        event.register(TetrisKeyBindings.ROTATE_CCW);
        event.register(TetrisKeyBindings.HOLD);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ArlightTetrisMod.MODID, "tetris_overlay"),
                new TetrisOverlay()
        );
    }
}
