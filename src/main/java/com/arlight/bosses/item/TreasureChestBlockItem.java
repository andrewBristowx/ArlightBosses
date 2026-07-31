package com.arlight.bosses.item;

import com.arlight.bosses.client.TreasureChestItemRenderer;
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

/**
 * BlockItem renderizado con el modelo 3D real del cofre. Evita usar el atlas
 * completo como sprite plano en el inventario, la mano, marcos y objetos tirados.
 */
public final class TreasureChestBlockItem extends BlockItem implements GeoItem {
    private final String variantPath;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TreasureChestBlockItem(Block block, Properties properties, String variantPath) {
        super(block, properties);
        this.variantPath = variantPath;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public String variantPath() {
        return variantPath;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private TreasureChestItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new TreasureChestItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "treasure_chest_item", 0,
                state -> state.setAndContinue(RawAnimation.begin()
                        .thenLoop("animation." + variantPath + ".closed"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
