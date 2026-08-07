package com.arlight.bosses.client;

import com.arlight.bosses.block.MedalPedestalBlock;
import com.arlight.bosses.block.entity.MedalPedestalBlockEntity;
import com.arlight.bosses.item.BossItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Dibuja el pedestal con su diseño original y la medalla GeckoLib como una pieza
 * independiente. Así el pedestal no pierde sus materiales de piedra/cobre y la
 * medalla conserva su propia textura, relieve y animación.
 */
public final class MedalPedestalRenderer extends GeoBlockRenderer<MedalPedestalBlockEntity> {
    public MedalPedestalRenderer(BlockEntityRendererProvider.Context context) {
        super(new MedalPedestalModel());
    }

    @Override
    public void render(MedalPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(pedestal, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        BlockState state = pedestal.getBlockState();
        if (!(state.getBlock() instanceof MedalPedestalBlock block)
                || !state.hasProperty(MedalPedestalBlock.PEDESTAL_STATE)) {
            return;
        }

        MedalPedestalBlock.PedestalState visualState =
                state.getValue(MedalPedestalBlock.PEDESTAL_STATE);
        if (visualState == MedalPedestalBlock.PedestalState.EMPTY) return;

        ItemStack medal = switch (block.kind()) {
            case HOME -> new ItemStack(BossItems.MOSSBOUND_HOME_MEDAL.get());
            case TRADE -> new ItemStack(BossItems.GILDED_TRADE_MEDAL.get());
            case BASTION -> new ItemStack(BossItems.EMERALD_BASTION_MEDAL.get());
        };

        float age = pedestal.getLevel() == null
                ? partialTick
                : pedestal.getLevel().getGameTime() + partialTick;
        float bob = (float) Math.sin(age * 0.12F) * 0.045F;
        float y = 1.55F + bob;
        float scale = 0.72F;
        float extraSpin = 0.0F;

        if (visualState == MedalPedestalBlock.PedestalState.INSERTING) {
            y = 1.43F + bob;
            scale = 0.64F;
        } else if (visualState == MedalPedestalBlock.PedestalState.UNLOCKING) {
            y = 1.78F + bob;
            scale = 0.84F;
            extraSpin = age * 16.0F;
        } else if (visualState == MedalPedestalBlock.PedestalState.UNLOCKED) {
            y = 1.66F + bob;
            scale = 0.76F;
            extraSpin = age * 4.0F;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, y, 0.5D);
        if (extraSpin != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(extraSpin));
        poseStack.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                medal,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                pedestal.getLevel(),
                pedestal.getBlockPos().hashCode());
        poseStack.popPose();
    }
}
