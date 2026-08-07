package com.arlight.bosses.client;

import com.arlight.bosses.item.CampaignMedalItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class CampaignMedalRenderer extends GeoItemRenderer<CampaignMedalItem> {
    public CampaignMedalRenderer() { super(new CampaignMedalModel()); }
}
