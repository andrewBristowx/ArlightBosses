package com.arlight.bosses.client;

import com.arlight.bosses.block.MedalPedestalBlock;
import com.arlight.bosses.block.entity.MedalPedestalBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Conserva exactamente los materiales visuales originales de los pedestales.
 * La medalla se dibuja por separado para no sustituir el pedestal por una
 * textura plana generada.
 */
public final class MedalPedestalModel extends GeoModel<MedalPedestalBlockEntity> {
    @Override public ResourceLocation getModelResource(MedalPedestalBlockEntity pedestal) {
        return BossClientEvents.id("geo/medal_pedestal.geo.json");
    }

    @Override public ResourceLocation getTextureResource(MedalPedestalBlockEntity pedestal) {
        if (pedestal.getBlockState().getBlock() instanceof MedalPedestalBlock block) {
            return switch (block.kind()) {
                case HOME -> ResourceLocation.fromNamespaceAndPath("minecraft",
                        "textures/block/mossy_stone_bricks.png");
                case TRADE -> ResourceLocation.fromNamespaceAndPath("minecraft",
                        "textures/block/exposed_cut_copper.png");
                case BASTION -> ResourceLocation.fromNamespaceAndPath("minecraft",
                        "textures/block/chiseled_deepslate.png");
            };
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft",
                "textures/block/mossy_stone_bricks.png");
    }

    @Override public ResourceLocation getAnimationResource(MedalPedestalBlockEntity pedestal) {
        return BossClientEvents.id("animations/medal_pedestal.animation.json");
    }
}
