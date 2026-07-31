package com.arlight.bosses.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Item de misión sin receta, apilado a uno y resistente al fuego. */
public final class CampaignKeyItem extends Item {
    private final String loreKey;

    public CampaignKeyItem(String loreKey, Properties properties) {
        super(properties.stacksTo(1).fireResistant());
        this.loreKey = loreKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(loreKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.arlightbosses.campaign_key_warning")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
