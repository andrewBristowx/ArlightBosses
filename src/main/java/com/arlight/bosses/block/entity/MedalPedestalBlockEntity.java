package com.arlight.bosses.block.entity;

import com.arlight.bosses.block.MedalPedestalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animaciones persistentes de los tres pedestales de medallas del Overworld. */
public final class MedalPedestalBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation EMPTY = RawAnimation.begin().thenLoop("animation.medal_pedestal.empty");
    private static final RawAnimation INSERTING = RawAnimation.begin().thenPlay("animation.medal_pedestal.insert");
    private static final RawAnimation FILLED = RawAnimation.begin().thenLoop("animation.medal_pedestal.filled");
    private static final RawAnimation UNLOCKING = RawAnimation.begin().thenPlay("animation.medal_pedestal.unlock");
    private static final RawAnimation UNLOCKED = RawAnimation.begin().thenLoop("animation.medal_pedestal.unlocked");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MedalPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(BossBlockEntities.MEDAL_PEDESTAL.get(), pos, state);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "medal_pedestal", 3, state -> {
            BlockState current = getBlockState();
            if (!current.hasProperty(MedalPedestalBlock.PEDESTAL_STATE)) {
                return state.setAndContinue(EMPTY);
            }
            return switch (current.getValue(MedalPedestalBlock.PEDESTAL_STATE)) {
                case EMPTY -> state.setAndContinue(EMPTY);
                case INSERTING -> state.setAndContinue(INSERTING);
                case FILLED -> state.setAndContinue(FILLED);
                case UNLOCKING -> state.setAndContinue(UNLOCKING);
                case UNLOCKED -> state.setAndContinue(UNLOCKED);
            };
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
