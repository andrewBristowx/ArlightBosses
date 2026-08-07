package com.arlight.bosses.block.entity;

import com.arlight.bosses.block.MedalPedestalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animaciones persistentes y sincronizadas de los pedestales del Overworld. */
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
        controllers.add(new AnimationController<>(this, "medal_pedestal", 1, state -> {
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

    public void playExternalInsert() { setVisualState(MedalPedestalBlock.PedestalState.INSERTING); }
    public void playExternalFilled() { setVisualState(MedalPedestalBlock.PedestalState.FILLED); }
    public void playExternalUnlock() { setVisualState(MedalPedestalBlock.PedestalState.UNLOCKING); }
    public void playExternalUnlocked() { setVisualState(MedalPedestalBlock.PedestalState.UNLOCKED); }
    public void playExternalReset() { setVisualState(MedalPedestalBlock.PedestalState.EMPTY); }

    private void setVisualState(MedalPedestalBlock.PedestalState target) {
        Level currentLevel = getLevel();
        BlockState oldState = getBlockState();
        if (currentLevel == null || !oldState.hasProperty(MedalPedestalBlock.PEDESTAL_STATE)) return;
        BlockState newState = oldState.setValue(MedalPedestalBlock.PEDESTAL_STATE, target);
        if (newState == oldState) {
            currentLevel.sendBlockUpdated(worldPosition, oldState, oldState, 3);
            setChanged();
            return;
        }
        currentLevel.setBlock(worldPosition, newState, 3);
        currentLevel.sendBlockUpdated(worldPosition, oldState, newState, 3);
        setChanged();
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
