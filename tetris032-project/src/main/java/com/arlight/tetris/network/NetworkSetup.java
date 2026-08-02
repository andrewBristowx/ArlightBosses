package com.arlight.tetris.network;

import com.arlight.tetris.ArlightTetrisMod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ArlightTetrisMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkSetup {
    private NetworkSetup() {}

    @net.neoforged.bus.api.SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ArlightTetrisMod.MODID).versioned("1");
        registrar.playToServer(ServerboundGameActionPacket.TYPE,
                ServerboundGameActionPacket.STREAM_CODEC, ServerPacketHandler::handleGameAction);
        registrar.playToClient(ClientboundBoardStatePacket.TYPE,
                ClientboundBoardStatePacket.STREAM_CODEC, ClientPacketHandler::handleBoardState);
        registrar.playToClient(ClientboundOpponentsStatePacket.TYPE,
                ClientboundOpponentsStatePacket.STREAM_CODEC, ClientPacketHandler::handleOpponentsState);
        registrar.playToClient(ClientboundMatchStatePacket.TYPE,
                ClientboundMatchStatePacket.STREAM_CODEC, ClientPacketHandler::handleMatchState);
    }
}
