package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.CorruptedPearlAltarBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CorruptedPearlAltarModel extends GeoModel<CorruptedPearlAltarBlockEntity> {
    @Override public ResourceLocation getModelResource(CorruptedPearlAltarBlockEntity altar) {
        return BossClientEvents.id("geo/corrupted_pearl_altar.geo.json");
    }
    @Override public ResourceLocation getTextureResource(CorruptedPearlAltarBlockEntity altar) {
        return BossClientEvents.id("textures/block/corrupted_pearl_altar.png");
    }
    @Override public ResourceLocation getAnimationResource(CorruptedPearlAltarBlockEntity altar) {
        return BossClientEvents.id("animations/corrupted_pearl_altar.animation.json");
    }
}
