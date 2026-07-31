package com.arlight.bosses.client;

import com.arlight.bosses.entity.minion.GildedShardProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Esquirla ligera o bloque de oro pesado, según la variante sincronizada. */
public final class GildedShardProjectileRenderer extends ArrowRenderer<GildedShardProjectile> {
    private static final ResourceLocation TEXTURE = BossClientEvents.id("textures/entity/gilded_shard_projectile.png");
    private final BlockRenderDispatcher blockRenderer;

    public GildedShardProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(GildedShardProjectile entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!entity.isGoldBlockVariant()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 11.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 8.0F));
        poseStack.scale(0.70F, 0.70F, 0.70F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        blockRenderer.renderSingleBlock(Blocks.GOLD_BLOCK.defaultBlockState(),
                poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GildedShardProjectile entity) {
        return TEXTURE;
    }
}
