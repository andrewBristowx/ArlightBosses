package com.arlight.bosses.block;

import com.arlight.bosses.block.entity.TreasureChestBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cofre animado y funcional. Fuera de Bingo abre un inventario real con tabla
 * de botín; dentro de Bingo el plugin intercepta el clic y entrega la versión
 * personal por jugador.
 */
public final class TreasureChestBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TreasureChestBlock> CODEC = simpleCodec(TreasureChestBlock::new);
    public static final EnumProperty<ChestState> CHEST_STATE = EnumProperty.create("chest_state", ChestState.class);
    private static final VoxelShape SHAPE = Block.box(0.5D, 0.0D, 0.5D, 15.5D, 15.0D, 15.5D);

    public TreasureChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CHEST_STATE, ChestState.CLOSED));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CHEST_STATE);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreasureChestBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TreasureChestBlockEntity chest) {
            player.openMenu(chest);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.arlight.bosses.block.entity.BossBlockEntities.TREASURE_CHEST.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<TreasureChestBlockEntity>)
                TreasureChestBlockEntity::serverTick;
    }

    public enum ChestState implements StringRepresentable {
        CLOSED("closed"), OPENING("opening"), OPENED("opened"), CLOSING("closing");
        private final String serializedName;
        ChestState(String serializedName) { this.serializedName = serializedName; }
        @Override public String getSerializedName() { return serializedName; }
    }
}
