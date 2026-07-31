package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.CorruptedPearlAltarBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class CorruptedPearlAltarRenderer extends GeoBlockRenderer<CorruptedPearlAltarBlockEntity> {
    public CorruptedPearlAltarRenderer(BlockEntityRendererProvider.Context context) {
        super(new CorruptedPearlAltarModel());
    }
}
