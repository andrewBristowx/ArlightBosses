package com.arlight.bosses.client;

import com.arlight.bosses.ArlightBosses;
import com.arlight.bosses.entity.BossEntities;
import com.arlight.bosses.block.entity.BossBlockEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class BossClientEvents {
    private BossClientEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(BossClientEvents::registerRenderers);
        NeoForge.EVENT_BUS.register(EpicBossBarOverlay.class);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BossBlockEntities.NETHER_DUNGEON_LOCK.get(), NetherDungeonLockRenderer::new);
        event.registerBlockEntityRenderer(BossBlockEntities.CORRUPTED_PEARL_ALTAR.get(), CorruptedPearlAltarRenderer::new);
        event.registerBlockEntityRenderer(BossBlockEntities.TREASURE_CHEST.get(), TreasureChestRenderer::new);
        event.registerEntityRenderer(BossEntities.AMETHYST_CORRUPTED_ENDER_DRAGON.get(),
                AmethystCorruptedDragonRenderer::new);
        event.registerEntityRenderer(BossEntities.SOMITA_GUIDE.get(), SomitaGuideRenderer::new);
        event.registerEntityRenderer(BossEntities.SURFACE_GUARDIAN.get(), context -> new GuardianRenderer<>(context, true));
        event.registerEntityRenderer(BossEntities.NETHER_GUARDIAN.get(), context -> new GuardianRenderer<>(context, true));
        event.registerEntityRenderer(BossEntities.VOID_GUARDIAN.get(), context -> new GuardianRenderer<>(context, true));
        event.registerEntityRenderer(BossEntities.DRAGON_GUARDIAN.get(), context -> new GuardianRenderer<>(context, true));
        event.registerEntityRenderer(BossEntities.EMERALD_ZOMBIE_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.EMERALD_CREEPER_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.GILDED_PIGLIN_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.GILDED_HOGLIN_RIDER_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.VOID_ENDERMAN_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.VOID_ENDERMAN_SENTINEL_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.AMETHYST_EYE_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.AMETHYST_SHULKER_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.EMERALD_SKELETON_ARCHER_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.GILDED_BLAZE_WRAITH_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.AMETHYST_PHANTOM_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.EMERALD_RAVAGER_CUB_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.MOSSBOUND_SPIDER_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.GILDED_WITHER_SKELETON_VANGUARD_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.MOLTEN_STRIDER_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.AMETHYST_GUARDIAN_SHARD_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.EMERALD_GOLEM_SENTINEL_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.CORRUPTED_ENDER_MITE_MINION.get(), CorruptedMinionRenderer::new);
        event.registerEntityRenderer(BossEntities.EMERALD_CORRUPTION_ARROW.get(), EmeraldCorruptionArrowRenderer::new);
        event.registerEntityRenderer(BossEntities.GILDED_SHARD_PROJECTILE.get(), GildedShardProjectileRenderer::new);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArlightBosses.MOD_ID, path);
    }
}
