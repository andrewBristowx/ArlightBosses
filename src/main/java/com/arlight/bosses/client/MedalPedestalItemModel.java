package com.arlight.bosses.client;

import com.arlight.bosses.item.MedalPedestalBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Vista de inventario con el mismo aspecto original que el pedestal colocado. */
public final class MedalPedestalItemModel extends GeoModel<MedalPedestalBlockItem> {
    @Override public ResourceLocation getModelResource(MedalPedestalBlockItem item) {
        return BossClientEvents.id("geo/medal_pedestal.geo.json");
    }

    @Override public ResourceLocation getTextureResource(MedalPedestalBlockItem item) {
        return switch (item.kind()) {
            case HOME -> ResourceLocation.fromNamespaceAndPath("minecraft",
                    "textures/block/mossy_stone_bricks.png");
            case TRADE -> ResourceLocation.fromNamespaceAndPath("minecraft",
                    "textures/block/exposed_cut_copper.png");
            case BASTION -> ResourceLocation.fromNamespaceAndPath("minecraft",
                    "textures/block/chiseled_deepslate.png");
        };
    }

    @Override public ResourceLocation getAnimationResource(MedalPedestalBlockItem item) {
        return BossClientEvents.id("animations/medal_pedestal.animation.json");
    }
}
