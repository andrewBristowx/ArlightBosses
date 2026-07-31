package com.arlight.bosses.client;

import com.arlight.bosses.entity.GuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class GuardianRenderer<T extends GuardianEntity> extends GeoEntityRenderer<T> {
    public GuardianRenderer(EntityRendererProvider.Context context, boolean emissive) {
        super(context, new GuardianModel<>());
        this.shadowRadius = 1.6F;
        if (emissive) addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(T guardian) {
        return 0.0F;
    }
}
