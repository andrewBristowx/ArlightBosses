package com.arlight.bosses.client;

import com.arlight.bosses.item.MedalPedestalBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class MedalPedestalItemRenderer extends GeoItemRenderer<MedalPedestalBlockItem> {
    public MedalPedestalItemRenderer() { super(new MedalPedestalItemModel()); }
}
