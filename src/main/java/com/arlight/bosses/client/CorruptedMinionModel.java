package com.arlight.bosses.client;

import com.arlight.bosses.entity.minion.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Maps every registered minion to the matching model, texture and animation set imported from Downloads.zip. */
public final class CorruptedMinionModel<T extends CorruptedMinionEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T minion) {
        return id("geo/" + asset(minion) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T minion) {
        return id("textures/entity/" + asset(minion) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T minion) {
        return id("animations/" + asset(minion) + ".animation.json");
    }

    private static String asset(CorruptedMinionEntity minion) {
        if (minion instanceof EmeraldCreeperMinion) return "emerald_creeper_minion";
        if (minion instanceof EmeraldGolemSentinelMinion) return "emerald_golem_sentinel_minion";
        if (minion instanceof EmeraldSkeletonArcherMinion) return "emerald_skeleton_archer";
        if (minion instanceof EmeraldRavagerCubMinion) return "emerald_ravager_cub";
        if (minion instanceof MossboundSpiderMinion) return "mossbound_spider_minion";
        if (minion instanceof GildedPiglinMinion) return "gilded_piglin_minion";
        if (minion instanceof GildedBlazeWraithMinion) return "gilded_blaze_wraith_minion";
        if (minion instanceof GildedHoglinRiderMinion) return "gilded_hoglin_rider_minion";
        if (minion instanceof GildedWitherSkeletonVanguardMinion) return "gilded_wither_skeleton_vanguard";
        if (minion instanceof MoltenStriderMinion) return "molten_strider_minion";
        if (minion instanceof VoidEndermanSentinelMinion) return "void_enderman_sentinel_minion";
        if (minion instanceof VoidEndermanMinion) return "void_enderman_minion";
        if (minion instanceof AmethystEyeMinion) return "amethyst_eye_minion";
        if (minion instanceof AmethystGuardianShardMinion) return "amethyst_guardian_shard_minion";
        if (minion instanceof AmethystPhantomMinion) return "amethyst_phantom_minion";
        if (minion instanceof AmethystShulkerMinion) return "amethyst_shulker_minion";
        if (minion instanceof CorruptedEnderMiteMinion) return "corrupted_ender_mite_minion";
        return "emerald_zombie_minion";
    }

    private static ResourceLocation id(String path) { return BossClientEvents.id(path); }
}
