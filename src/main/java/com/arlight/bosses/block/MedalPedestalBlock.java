package com.arlight.bosses.block;

import com.arlight.bosses.block.entity.MedalPedestalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pedestal ritual de la campaña del Overworld. Cada variante acepta una medalla
 * concreta; ArlightBingo controla la inserción y conserva el estado en el propio
 * BlockState para que sobreviva reinicios y copias de arena.
 */
public final class MedalPedestalBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<PedestalState> PEDESTAL_STATE =
            EnumProperty.create("pedestal_state", PedestalState.class);
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 24.0D, 15.0D);

    private final MedalKind kind;

    public MedalPedestalBlock(MedalKind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PEDESTAL_STATE, PedestalState.EMPTY));
    }

    public MedalKind kind() { return kind; }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return MapCodec.unit(this); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PEDESTAL_STATE);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return SHAPE;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MedalPedestalBlockEntity(pos, state);
    }

    public enum MedalKind {
        HOME("home"), TRADE("trade"), BASTION("bastion");
        private final String serializedName;
        MedalKind(String serializedName) { this.serializedName = serializedName; }
        public String serializedName() { return serializedName; }
    }

    public enum PedestalState implements StringRepresentable {
        EMPTY("empty"), INSERTING("inserting"), FILLED("filled"),
        UNLOCKING("unlocking"), UNLOCKED("unlocked");
        private final String serializedName;
        PedestalState(String serializedName) { this.serializedName = serializedName; }
        @Override public String getSerializedName() { return serializedName; }
    }
}
