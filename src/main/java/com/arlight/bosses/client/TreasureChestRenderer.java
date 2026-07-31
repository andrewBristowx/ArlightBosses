package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.TreasureChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Las estructuras de Bingo colocan los cofres orientados al norte. */
public final class TreasureChestRenderer extends GeoBlockRenderer<TreasureChestBlockEntity> {
    public TreasureChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new TreasureChestModel());
    }
}
