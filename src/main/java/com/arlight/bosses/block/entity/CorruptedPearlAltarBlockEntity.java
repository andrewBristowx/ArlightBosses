package com.arlight.bosses.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Mantiene el altar flotando en reposo y permite que Bingo dispare una activación cinemática. */
public final class CorruptedPearlAltarBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String CONTROLLER = "corrupted_pearl_altar";
    private static final String ACTIVATE_TRIGGER = "activate";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.corrupted_pearl_altar.idle");
    private static final RawAnimation ACTIVATE = RawAnimation.begin().thenPlay("animation.corrupted_pearl_altar.activate");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CorruptedPearlAltarBlockEntity(BlockPos pos, BlockState state) {
        super(BossBlockEntities.CORRUPTED_PEARL_ALTAR.get(), pos, state);
    }

    /** Llamado por reflexión desde ArlightBingo al consumir la llave del dragón. */
    public void playExternalActivate() {
        if (level != null && !level.isClientSide) triggerAnim(CONTROLLER, ACTIVATE_TRIGGER);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CorruptedPearlAltarBlockEntity> controller =
                new AnimationController<>(this, CONTROLLER, 2, state -> state.setAndContinue(IDLE));
        controller.triggerableAnim(ACTIVATE_TRIGGER, ACTIVATE);
        controllers.add(controller);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
