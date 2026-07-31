package com.arlight.bosses.block;

import com.arlight.bosses.block.entity.CorruptedPearlAltarBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Altar animado de campaña. Su modelo sobrepasa un bloque, pero conserva un único punto de interacción. */
public final class CorruptedPearlAltarBlock extends Block implements EntityBlock {
    public static final MapCodec<CorruptedPearlAltarBlock> CODEC = simpleCodec(CorruptedPearlAltarBlock::new);
    private static final VoxelShape SHAPE = Block.box(-5.0D, 0.0D, -5.0D, 21.0D, 32.0D, 21.0D);

    public CorruptedPearlAltarBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CorruptedPearlAltarBlockEntity(pos, state);
    }
}
