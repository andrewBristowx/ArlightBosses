package com.arlight.bosses.item;

import com.arlight.bosses.block.MedalPedestalBlock;
import com.arlight.bosses.client.MedalPedestalItemRenderer;
import java.util.function.Consumer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Render 3D del pedestal en inventario, mano, suelo y marcos. */
public final class MedalPedestalBlockItem extends BlockItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.medal_pedestal.empty");
    private final MedalPedestalBlock.MedalKind kind;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MedalPedestalBlockItem(Block block, MedalPedestalBlock.MedalKind kind, Properties properties) {
        super(block, properties);
        this.kind = kind;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public MedalPedestalBlock.MedalKind kind() { return kind; }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MedalPedestalItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new MedalPedestalItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "medal_pedestal_item", 3,
                state -> state.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
