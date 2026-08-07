package com.arlight.bosses.item;

import com.arlight.bosses.client.CampaignMedalRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Medalla tridimensional de misión con render y animación GeckoLib propios. */
public final class CampaignMedalItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.campaign_medal.idle");
    private final String variantPath;
    private final String loreKey;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CampaignMedalItem(String variantPath, String loreKey, Properties properties) {
        super(properties.stacksTo(1).fireResistant());
        this.variantPath = variantPath;
        this.loreKey = loreKey;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public String variantPath() { return variantPath; }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private CampaignMedalRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new CampaignMedalRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "campaign_medal", 3,
                state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(loreKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.arlightbosses.campaign_key_warning")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
