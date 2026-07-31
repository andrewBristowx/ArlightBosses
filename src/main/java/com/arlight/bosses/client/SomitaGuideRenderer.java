package com.arlight.bosses.client;

import com.arlight.bosses.entity.SomitaGuideEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class SomitaGuideRenderer extends GeoEntityRenderer<SomitaGuideEntity> {
    public SomitaGuideRenderer(EntityRendererProvider.Context context) {
        super(context, new SomitaGuideModel());
        shadowRadius = 0.45F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(SomitaGuideEntity entity) {
        return 0.0F;
    }
}
