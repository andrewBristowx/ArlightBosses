package com.arlight.bosses.client;

import com.arlight.bosses.item.TreasureChestBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TreasureChestItemModel extends GeoModel<TreasureChestBlockItem> {
    @Override
    public ResourceLocation getModelResource(TreasureChestBlockItem item) {
        return BossClientEvents.id("geo/" + item.variantPath() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TreasureChestBlockItem item) {
        return BossClientEvents.id("textures/block/" + item.variantPath() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(TreasureChestBlockItem item) {
        return BossClientEvents.id("animations/" + item.variantPath() + ".animation.json");
    }
}
