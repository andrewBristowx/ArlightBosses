package com.arlight.bosses.client;

import com.arlight.bosses.entity.minion.CorruptedMinionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderer for the new per-creature models, including their generated emissive accent masks. */
public final class CorruptedMinionRenderer<T extends CorruptedMinionEntity> extends GeoEntityRenderer<T> {
    public CorruptedMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new CorruptedMinionModel<>());
        this.shadowRadius = 0.72F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override protected float getDeathMaxRotation(T minion) { return 0.0F; }
}
