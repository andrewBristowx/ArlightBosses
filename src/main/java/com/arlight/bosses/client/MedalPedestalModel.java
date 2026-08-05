package com.arlight.bosses.client;

import com.arlight.bosses.block.MedalPedestalBlock;
import com.arlight.bosses.block.entity.MedalPedestalBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modelo GeckoLib con textura completa propia para cada pedestal y su medalla. */
public final class MedalPedestalModel extends GeoModel<MedalPedestalBlockEntity> {
    @Override public ResourceLocation getModelResource(MedalPedestalBlockEntity pedestal) {
        return BossClientEvents.id("geo/medal_pedestal.geo.json");
    }

    @Override public ResourceLocation getTextureResource(MedalPedestalBlockEntity pedestal) {
        if (pedestal.getBlockState().getBlock() instanceof MedalPedestalBlock block) {
            return switch (block.kind()) {
                case HOME -> BossClientEvents.id("textures/block/home_medal_pedestal.png");
                case TRADE -> BossClientEvents.id("textures/block/trade_medal_pedestal.png");
                case BASTION -> BossClientEvents.id("textures/block/bastion_medal_pedestal.png");
            };
        }
        return BossClientEvents.id("textures/block/home_medal_pedestal.png");
    }

    @Override public ResourceLocation getAnimationResource(MedalPedestalBlockEntity pedestal) {
        return BossClientEvents.id("animations/medal_pedestal.animation.json");
    }
}
