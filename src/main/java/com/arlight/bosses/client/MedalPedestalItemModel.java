package com.arlight.bosses.client;

import com.arlight.bosses.item.MedalPedestalBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MedalPedestalItemModel extends GeoModel<MedalPedestalBlockItem> {
    @Override public ResourceLocation getModelResource(MedalPedestalBlockItem item) {
        return BossClientEvents.id("geo/medal_pedestal.geo.json");
    }

    @Override public ResourceLocation getTextureResource(MedalPedestalBlockItem item) {
        return switch (item.kind()) {
            case HOME -> BossClientEvents.id("textures/block/home_medal_pedestal.png");
            case TRADE -> BossClientEvents.id("textures/block/trade_medal_pedestal.png");
            case BASTION -> BossClientEvents.id("textures/block/bastion_medal_pedestal.png");
        };
    }

    @Override public ResourceLocation getAnimationResource(MedalPedestalBlockItem item) {
        return BossClientEvents.id("animations/medal_pedestal.animation.json");
    }
}
