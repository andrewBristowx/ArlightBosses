package com.arlight.bosses.client;

import com.arlight.bosses.item.CampaignMedalItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CampaignMedalModel extends GeoModel<CampaignMedalItem> {
    @Override public ResourceLocation getModelResource(CampaignMedalItem item) {
        return BossClientEvents.id("geo/campaign_medal.geo.json");
    }

    @Override public ResourceLocation getTextureResource(CampaignMedalItem item) {
        return BossClientEvents.id("textures/item/" + item.variantPath() + ".png");
    }

    @Override public ResourceLocation getAnimationResource(CampaignMedalItem item) {
        return BossClientEvents.id("animations/campaign_medal.animation.json");
    }
}
