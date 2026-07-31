package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.TreasureChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TreasureChestModel extends GeoModel<TreasureChestBlockEntity> {
    @Override public ResourceLocation getModelResource(TreasureChestBlockEntity chest) {
        return BossClientEvents.id("geo/" + chest.variantPath() + ".geo.json");
    }
    @Override public ResourceLocation getTextureResource(TreasureChestBlockEntity chest) {
        return BossClientEvents.id("textures/block/" + chest.variantPath() + ".png");
    }
    @Override public ResourceLocation getAnimationResource(TreasureChestBlockEntity chest) {
        return BossClientEvents.id("animations/" + chest.variantPath() + ".animation.json");
    }
}
