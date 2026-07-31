package com.arlight.bosses.client;

import com.arlight.bosses.item.TreasureChestBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Render 3D de los cinco cofres en inventario, mano, suelo y marcos. */
public final class TreasureChestItemRenderer extends GeoItemRenderer<TreasureChestBlockItem> {
    public TreasureChestItemRenderer() {
        super(new TreasureChestItemModel());
    }
}
