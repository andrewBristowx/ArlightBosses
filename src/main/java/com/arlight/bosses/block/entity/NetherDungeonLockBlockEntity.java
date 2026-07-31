package com.arlight.bosses.block.entity;

import com.arlight.bosses.block.NetherDungeonLockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animación sincronizada mediante el estado de bloque, por lo que funciona tras reinicios y recargas de chunk. */
public final class NetherDungeonLockBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nether_dungeon_lock.idle");
    private static final RawAnimation UNLOCK = RawAnimation.begin().thenPlay("animation.nether_dungeon_lock.unlock");
    private static final RawAnimation OPENED = RawAnimation.begin().thenLoop("animation.nether_dungeon_lock.opened");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NetherDungeonLockBlockEntity(BlockPos pos, BlockState state) {
        super(BossBlockEntities.NETHER_DUNGEON_LOCK.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "lock", 3, state -> {
            BlockState blockState = getBlockState();
            if (!blockState.hasProperty(NetherDungeonLockBlock.LOCK_STATE)) return state.setAndContinue(IDLE);
            return switch (blockState.getValue(NetherDungeonLockBlock.LOCK_STATE)) {
                case LOCKED -> state.setAndContinue(IDLE);
                case OPENING -> state.setAndContinue(UNLOCK);
                case OPENED -> state.setAndContinue(OPENED);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
