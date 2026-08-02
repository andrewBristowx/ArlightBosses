package com.arlight.tetris.network;

import com.arlight.tetris.ArlightTetrisMod;
import com.arlight.tetris.game.Board;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundBoardStatePacket(
        byte[] grid, int currentType, int currentX, int currentY,
        int currentRotation, int ghostY, int holdType, byte[] nextQueue,
        int linesCleared, int score, int level, int combo,
        boolean backToBack, boolean topOut
) implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, byte[]> GRID_CODEC =
            ByteBufCodecs.byteArray(Board.WIDTH * Board.TOTAL_HEIGHT);
    private static final StreamCodec<ByteBuf, byte[]> NEXT_QUEUE_CODEC = ByteBufCodecs.byteArray(5);

    public static final Type<ClientboundBoardStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArlightTetrisMod.MODID, "board_state"));

    public static final StreamCodec<ByteBuf, ClientboundBoardStatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClientboundBoardStatePacket decode(ByteBuf buffer) {
            return new ClientboundBoardStatePacket(
                    GRID_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    NEXT_QUEUE_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)
            );
        }

        @Override
        public void encode(ByteBuf buffer, ClientboundBoardStatePacket value) {
            GRID_CODEC.encode(buffer, value.grid());
            ByteBufCodecs.VAR_INT.encode(buffer, value.currentType());
            ByteBufCodecs.VAR_INT.encode(buffer, value.currentX());
            ByteBufCodecs.VAR_INT.encode(buffer, value.currentY());
            ByteBufCodecs.VAR_INT.encode(buffer, value.currentRotation());
            ByteBufCodecs.VAR_INT.encode(buffer, value.ghostY());
            ByteBufCodecs.VAR_INT.encode(buffer, value.holdType());
            NEXT_QUEUE_CODEC.encode(buffer, value.nextQueue());
            ByteBufCodecs.VAR_INT.encode(buffer, value.linesCleared());
            ByteBufCodecs.VAR_INT.encode(buffer, value.score());
            ByteBufCodecs.VAR_INT.encode(buffer, value.level());
            ByteBufCodecs.VAR_INT.encode(buffer, value.combo());
            ByteBufCodecs.BOOL.encode(buffer, value.backToBack());
            ByteBufCodecs.BOOL.encode(buffer, value.topOut());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
