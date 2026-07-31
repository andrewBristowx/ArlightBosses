package com.arlight.bosses.client;

import com.arlight.bosses.ArlightBosses;
import com.arlight.bosses.entity.AmethystCorruptedDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Renderer exclusivo del EntityType propio del dragón corrupto. */
public final class AmethystCorruptedDragonRenderer extends EntityRenderer<AmethystCorruptedDragonEntity> {
    private static final ResourceLocation DRAGON_TEXTURE = id("textures/entity/amethyst_corrupted_ender_dragon.png");
    private static final ResourceLocation DRAGON_EYES = id("textures/entity/amethyst_corrupted_ender_dragon_eyes.png");
    private static final ResourceLocation DRAGON_EXPLODING = id("textures/entity/amethyst_corrupted_ender_dragon_exploding.png");

    private final EnderDragonRenderer.DragonModel model;
    private final BlockRenderDispatcher blockRenderer;
    private EnderDragon renderProxy;
    private Level proxyLevel;

    public AmethystCorruptedDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.5F;
        this.model = new EnderDragonRenderer.DragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
            AmethystCorruptedDragonEntity dragon,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (dragon.isCocooned()) {
            renderCocoon(dragon, poseStack, buffers);
            super.render(dragon, entityYaw, partialTick, poseStack, buffers, packedLight);
            return;
        }

        EnderDragon proxy = prepareProxy(dragon);
        poseStack.pushPose();
        applyDragonTransform(dragon, partialTick, poseStack);
        boolean hurt = dragon.hurtTime > 0;
        model.prepareMobModel(proxy, 0.0F, 0.0F, partialTick);

        float fadeProgress = dragon.getDeathFadeProgress(partialTick);
        if (fadeProgress > 0.0F) {
            int alpha = Mth.clamp(Mth.floor((1.0F - fadeProgress) * 255.0F), 0, 255);
            int color = FastColor.ARGB32.color(alpha, 255, 255, 255);
            VertexConsumer fadingBody = buffers.getBuffer(RenderType.entityTranslucent(DRAGON_TEXTURE));
            model.renderToBuffer(poseStack, fadingBody, packedLight,
                    OverlayTexture.pack(0.0F, hurt), color);
        } else {
            VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(DRAGON_TEXTURE));
            model.renderToBuffer(poseStack, body, packedLight, OverlayTexture.pack(0.0F, hurt));
        }

        if (fadeProgress < 0.72F) {
            VertexConsumer eyes = buffers.getBuffer(RenderType.eyes(DRAGON_EYES));
            model.renderToBuffer(poseStack, eyes, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
        if (fadeProgress < 0.58F) renderCrystalGrowths(dragon, poseStack, buffers);
        poseStack.popPose();
        super.render(dragon, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private void renderCocoon(
            AmethystCorruptedDragonEntity dragon,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.8D, 0.0D);
        float pulse = 1.0F + Mth.sin((dragon.tickCount) * 0.18F) * 0.055F;
        BlockState core = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState cluster = Blocks.AMETHYST_CLUSTER.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, Direction.UP)
                .setValue(AmethystClusterBlock.WATERLOGGED, false);
        BlockState bud = Blocks.LARGE_AMETHYST_BUD.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, Direction.UP)
                .setValue(AmethystClusterBlock.WATERLOGGED, false);

        renderCrystal(poseStack, buffers, core, 0.0F, 0.0F, 0.0F, 2.65F * pulse, 0, 0, 0);
        renderCrystal(poseStack, buffers, cluster, 0.0F, 2.25F, 0.0F, 1.85F * pulse, 0, 0, 0);
        renderCrystal(poseStack, buffers, cluster, 0.0F, -2.15F, 0.0F, 1.70F * pulse, 180, 0, 0);
        renderCrystal(poseStack, buffers, bud, 2.15F, 0.45F, 0.0F, 1.55F * pulse, 0, 0, -62);
        renderCrystal(poseStack, buffers, bud, -2.15F, 0.45F, 0.0F, 1.55F * pulse, 0, 0, 62);
        renderCrystal(poseStack, buffers, cluster, 0.0F, 0.35F, 2.15F, 1.45F * pulse, 62, 0, 0);
        renderCrystal(poseStack, buffers, cluster, 0.0F, 0.35F, -2.15F, 1.45F * pulse, -62, 0, 0);
        renderCrystal(poseStack, buffers, bud, 1.45F, 1.55F, 1.25F, 1.10F * pulse, 35, 20, -35);
        renderCrystal(poseStack, buffers, bud, -1.45F, 1.55F, -1.25F, 1.10F * pulse, -35, 20, 35);
        poseStack.popPose();
    }

    private EnderDragon prepareProxy(AmethystCorruptedDragonEntity dragon) {
        if (renderProxy == null || proxyLevel != dragon.level()) {
            proxyLevel = dragon.level();
            renderProxy = new EnderDragon(EntityType.ENDER_DRAGON, proxyLevel);
        }
        renderProxy.setPos(dragon.getX(), dragon.getY(), dragon.getZ());
        renderProxy.xo = dragon.xo;
        renderProxy.yo = dragon.yo;
        renderProxy.zo = dragon.zo;
        renderProxy.setYRot(dragon.getYRot());
        renderProxy.setXRot(dragon.getXRot());
        renderProxy.yRotO = dragon.yRotO;
        renderProxy.xRotO = dragon.xRotO;
        renderProxy.oFlapTime = dragon.oFlapTime;
        renderProxy.flapTime = dragon.flapTime;
        renderProxy.dragonDeathTime = dragon.dragonDeathTime;
        renderProxy.hurtTime = dragon.hurtTime;
        renderProxy.tickCount = dragon.tickCount;
        renderProxy.posPointer = dragon.posPointer;
        for (int i = 0; i < dragon.positions.length; i++) {
            System.arraycopy(dragon.positions[i], 0, renderProxy.positions[i], 0, 3);
        }
        renderProxy.setHealth(dragon.isAlive() ? Math.max(1.0F,
                Math.min(renderProxy.getMaxHealth(), dragon.getHealth())) : 0.0F);
        return renderProxy;
    }

    private static void applyDragonTransform(
            AmethystCorruptedDragonEntity dragon,
            float partialTick,
            PoseStack poseStack
    ) {
        float bodyYaw = (float) dragon.getLatencyPos(7, partialTick)[0];
        float pitch = (float) (dragon.getLatencyPos(5, partialTick)[1]
                - dragon.getLatencyPos(10, partialTick)[1]);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        // El modelo vanilla mira en sentido contrario al frente lógico de una
        // entidad Monster. Este giro alinea la cabeza con el movimiento real.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * 10.0F));
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
    }

    private void renderCrystalGrowths(
            AmethystCorruptedDragonEntity dragon,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        boolean overload = dragon.getCombatPhase() == 3;
        boolean whiteFlash = overload && ((dragon.tickCount / 3) & 1) == 0;
        BlockState main = whiteFlash
                ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                : Blocks.AMETHYST_CLUSTER.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.UP)
                    .setValue(AmethystClusterBlock.WATERLOGGED, false);
        BlockState bud = whiteFlash
                ? Blocks.CALCITE.defaultBlockState()
                : Blocks.LARGE_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.UP)
                    .setValue(AmethystClusterBlock.WATERLOGGED, false);

        float pulse = overload ? 0.92F + Mth.sin(dragon.tickCount * 0.85F) * 0.10F : 1.0F;
        renderCrystal(poseStack, buffers, main, 0.00F, -0.75F, 0.20F, 0.48F * pulse, 0, 0, 0);
        renderCrystal(poseStack, buffers, bud, 0.00F, -0.62F, -1.15F, 0.42F * pulse, -12, 0, 0);
        renderCrystal(poseStack, buffers, main, 0.00F, -0.48F, -2.25F, 0.36F * pulse, -18, 0, 0);
        renderCrystal(poseStack, buffers, bud, -0.95F, -0.35F, -0.25F, 0.34F * pulse, 0, 0, 24);
        renderCrystal(poseStack, buffers, bud, 0.95F, -0.35F, -0.25F, 0.34F * pulse, 0, 0, -24);
        renderCrystal(poseStack, buffers, main, -1.85F, -0.18F, -0.60F, 0.28F * pulse, 15, 0, 36);
        renderCrystal(poseStack, buffers, main, 1.85F, -0.18F, -0.60F, 0.28F * pulse, 15, 0, -36);
        renderCrystal(poseStack, buffers, bud, -0.52F, -1.05F, 2.05F, 0.28F * pulse, -18, 0, 15);
        renderCrystal(poseStack, buffers, bud, 0.52F, -1.05F, 2.05F, 0.28F * pulse, -18, 0, -15);
    }

    private void renderCrystal(
            PoseStack poseStack,
            MultiBufferSource buffers,
            BlockState state,
            float x, float y, float z, float scale,
            float pitch, float yaw, float roll
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        blockRenderer.renderSingleBlock(state, poseStack, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AmethystCorruptedDragonEntity dragon) {
        return DRAGON_TEXTURE;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArlightBosses.MOD_ID, path);
    }
}
