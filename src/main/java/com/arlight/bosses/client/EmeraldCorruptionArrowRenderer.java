package com.arlight.bosses.client;

import com.arlight.bosses.entity.minion.EmeraldCorruptionArrow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Flecha normal o roca esmeralda, según la variante sincronizada del proyectil. */
public final class EmeraldCorruptionArrowRenderer extends ArrowRenderer<EmeraldCorruptionArrow> {
    private static final ResourceLocation TEXTURE = BossClientEvents.id("textures/entity/emerald_corruption_arrow.png");
    private final BlockRenderDispatcher blockRenderer;

    public EmeraldCorruptionArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(EmeraldCorruptionArrow entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!entity.isStoneVariant()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 18.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 24.0F));
        poseStack.scale(0.48F, 0.48F, 0.48F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        blockRenderer.renderSingleBlock(Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EmeraldCorruptionArrow entity) {
        return TEXTURE;
    }
}
