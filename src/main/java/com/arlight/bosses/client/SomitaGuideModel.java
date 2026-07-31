package com.arlight.bosses.client;

import com.arlight.bosses.entity.SomitaGuideEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SomitaGuideModel extends GeoModel<SomitaGuideEntity> {
    @Override
    public ResourceLocation getModelResource(SomitaGuideEntity entity) {
        return BossClientEvents.id("geo/somita_vampire_guardian.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SomitaGuideEntity entity) {
        return BossClientEvents.id("textures/entity/somita_vampire_guardian.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SomitaGuideEntity entity) {
        return BossClientEvents.id("animations/somita_guide.animation.json");
    }
}
