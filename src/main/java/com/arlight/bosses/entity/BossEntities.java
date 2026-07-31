package com.arlight.bosses.entity;

import com.arlight.bosses.ArlightBosses;
import com.arlight.bosses.entity.minion.AmethystEyeMinion;
import com.arlight.bosses.entity.minion.AmethystGuardianShardMinion;
import com.arlight.bosses.entity.minion.AmethystPhantomMinion;
import com.arlight.bosses.entity.minion.AmethystShulkerMinion;
import com.arlight.bosses.entity.minion.CorruptedEnderMiteMinion;
import com.arlight.bosses.entity.minion.CorruptedMinionEntity;
import com.arlight.bosses.entity.minion.EmeraldCorruptionArrow;
import com.arlight.bosses.entity.minion.EmeraldCreeperMinion;
import com.arlight.bosses.entity.minion.EmeraldGolemSentinelMinion;
import com.arlight.bosses.entity.minion.EmeraldRavagerCubMinion;
import com.arlight.bosses.entity.minion.EmeraldSkeletonArcherMinion;
import com.arlight.bosses.entity.minion.EmeraldZombieMinion;
import com.arlight.bosses.entity.minion.GildedBlazeWraithMinion;
import com.arlight.bosses.entity.minion.GildedHoglinRiderMinion;
import com.arlight.bosses.entity.minion.GildedPiglinMinion;
import com.arlight.bosses.entity.minion.GildedShardProjectile;
import com.arlight.bosses.entity.minion.GildedWitherSkeletonVanguardMinion;
import com.arlight.bosses.entity.minion.MoltenStriderMinion;
import com.arlight.bosses.entity.minion.MossboundSpiderMinion;
import com.arlight.bosses.entity.minion.VoidEndermanMinion;
import com.arlight.bosses.entity.minion.VoidEndermanSentinelMinion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BossEntities {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ArlightBosses.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SurfaceGuardian>> SURFACE_GUARDIAN =
            TYPES.register("surface_guardian", () -> EntityType.Builder
                    .of(SurfaceGuardian::new, MobCategory.MONSTER).sized(3.35F, 3.40F)
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("surface_guardian").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<NetherGuardian>> NETHER_GUARDIAN =
            TYPES.register("nether_guardian", () -> EntityType.Builder
                    .of(NetherGuardian::new, MobCategory.MONSTER).sized(1.80F, 3.18F).fireImmune()
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("nether_guardian").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidGuardian>> VOID_GUARDIAN =
            TYPES.register("void_guardian", () -> EntityType.Builder
                    .of(VoidGuardian::new, MobCategory.MONSTER).sized(3.30F, 4.66F)
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("void_guardian").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<AmethystCorruptedDragonEntity>> AMETHYST_CORRUPTED_ENDER_DRAGON =
            TYPES.register("amethyst_corrupted_ender_dragon", () -> EntityType.Builder
                    .of(AmethystCorruptedDragonEntity::new, MobCategory.MONSTER)
                    .sized(8.0F, 4.0F).clientTrackingRange(16).updateInterval(1)
                    .build(id("amethyst_corrupted_ender_dragon").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SomitaGuideEntity>> SOMITA_GUIDE =
            TYPES.register("somita_guide", () -> EntityType.Builder
                    .of(SomitaGuideEntity::new, MobCategory.CREATURE).sized(1.25F, 2.25F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("somita_guide").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<DragonGuardian>> DRAGON_GUARDIAN =
            TYPES.register("dragon_guardian", () -> EntityType.Builder
                    .of(DragonGuardian::new, MobCategory.MONSTER).sized(1.60F, 2.32F).fireImmune()
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("dragon_guardian").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldZombieMinion>> EMERALD_ZOMBIE_MINION =
            TYPES.register("emerald_zombie_minion", () -> EntityType.Builder
                    .of(EmeraldZombieMinion::new, MobCategory.MONSTER).sized(1.00F, 2.44F)
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("emerald_zombie_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldCreeperMinion>> EMERALD_CREEPER_MINION =
            TYPES.register("emerald_creeper_minion", () -> EntityType.Builder
                    .of(EmeraldCreeperMinion::new, MobCategory.MONSTER).sized(0.92F, 2.02F)
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("emerald_creeper_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldGolemSentinelMinion>> EMERALD_GOLEM_SENTINEL_MINION =
            TYPES.register("emerald_golem_sentinel_minion", () -> EntityType.Builder
                    .of(EmeraldGolemSentinelMinion::new, MobCategory.MONSTER).sized(1.60F, 2.55F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("emerald_golem_sentinel_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CorruptedEnderMiteMinion>> CORRUPTED_ENDER_MITE_MINION =
            TYPES.register("corrupted_ender_mite_minion", () -> EntityType.Builder
                    .of(CorruptedEnderMiteMinion::new, MobCategory.MONSTER).sized(1.45F, 1.10F)
                    .clientTrackingRange(6).updateInterval(4)
                    .build(id("corrupted_ender_mite_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldSkeletonArcherMinion>> EMERALD_SKELETON_ARCHER_MINION =
            TYPES.register("emerald_skeleton_archer_minion", () -> EntityType.Builder
                    .of(EmeraldSkeletonArcherMinion::new, MobCategory.MONSTER).sized(0.98F, 2.48F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("emerald_skeleton_archer_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldCorruptionArrow>> EMERALD_CORRUPTION_ARROW =
            TYPES.register("emerald_corruption_arrow", () -> EntityType.Builder
                    .<EmeraldCorruptionArrow>of(EmeraldCorruptionArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                    .build(id("emerald_corruption_arrow").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<EmeraldRavagerCubMinion>> EMERALD_RAVAGER_CUB_MINION =
            TYPES.register("emerald_ravager_cub_minion", () -> EntityType.Builder
                    .of(EmeraldRavagerCubMinion::new, MobCategory.MONSTER).sized(2.15F, 1.94F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("emerald_ravager_cub_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<MossboundSpiderMinion>> MOSSBOUND_SPIDER_MINION =
            TYPES.register("mossbound_spider_minion", () -> EntityType.Builder
                    .of(MossboundSpiderMinion::new, MobCategory.MONSTER).sized(1.55F, 1.18F)
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("mossbound_spider_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<MoltenStriderMinion>> MOLTEN_STRIDER_MINION =
            TYPES.register("molten_strider_minion", () -> EntityType.Builder
                    .of(MoltenStriderMinion::new, MobCategory.MONSTER).sized(1.55F, 1.68F).fireImmune()
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("molten_strider_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GildedPiglinMinion>> GILDED_PIGLIN_MINION =
            TYPES.register("gilded_piglin_minion", () -> EntityType.Builder
                    .of(GildedPiglinMinion::new, MobCategory.MONSTER).sized(1.05F, 2.50F).fireImmune()
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("gilded_piglin_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GildedShardProjectile>> GILDED_SHARD_PROJECTILE =
            TYPES.register("gilded_shard_projectile", () -> EntityType.Builder
                    .<GildedShardProjectile>of(GildedShardProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                    .build(id("gilded_shard_projectile").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GildedWitherSkeletonVanguardMinion>> GILDED_WITHER_SKELETON_VANGUARD_MINION =
            TYPES.register("gilded_wither_skeleton_vanguard_minion", () -> EntityType.Builder
                    .of(GildedWitherSkeletonVanguardMinion::new, MobCategory.MONSTER).sized(1.46F, 2.50F).fireImmune()
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("gilded_wither_skeleton_vanguard_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GildedHoglinRiderMinion>> GILDED_HOGLIN_RIDER_MINION =
            TYPES.register("gilded_hoglin_rider_minion", () -> EntityType.Builder
                    .of(GildedHoglinRiderMinion::new, MobCategory.MONSTER).sized(1.72F, 2.28F).fireImmune()
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("gilded_hoglin_rider_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GildedBlazeWraithMinion>> GILDED_BLAZE_WRAITH_MINION =
            TYPES.register("gilded_blaze_wraith_minion", () -> EntityType.Builder
                    .of(GildedBlazeWraithMinion::new, MobCategory.MONSTER).sized(0.92F, 2.38F).fireImmune()
                    .clientTrackingRange(10).updateInterval(2)
                    .build(id("gilded_blaze_wraith_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidEndermanMinion>> VOID_ENDERMAN_MINION =
            TYPES.register("void_enderman_minion", () -> EntityType.Builder
                    .of(VoidEndermanMinion::new, MobCategory.MONSTER).sized(0.82F, 2.44F)
                    .clientTrackingRange(8).updateInterval(3)
                    .build(id("void_enderman_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidEndermanSentinelMinion>> VOID_ENDERMAN_SENTINEL_MINION =
            TYPES.register("void_enderman_sentinel_minion", () -> EntityType.Builder
                    .of(VoidEndermanSentinelMinion::new, MobCategory.MONSTER).sized(1.08F, 3.18F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("void_enderman_sentinel_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<AmethystEyeMinion>> AMETHYST_EYE_MINION =
            TYPES.register("amethyst_eye_minion", () -> EntityType.Builder
                    .of(AmethystEyeMinion::new, MobCategory.MONSTER).sized(1.55F, 1.92F)
                    .clientTrackingRange(10).updateInterval(2)
                    .build(id("amethyst_eye_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<AmethystGuardianShardMinion>> AMETHYST_GUARDIAN_SHARD_MINION =
            TYPES.register("amethyst_guardian_shard_minion", () -> EntityType.Builder
                    .of(AmethystGuardianShardMinion::new, MobCategory.MONSTER).sized(1.38F, 2.18F)
                    .clientTrackingRange(10).updateInterval(2)
                    .build(id("amethyst_guardian_shard_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<AmethystShulkerMinion>> AMETHYST_SHULKER_MINION =
            TYPES.register("amethyst_shulker_minion", () -> EntityType.Builder
                    .of(AmethystShulkerMinion::new, MobCategory.MONSTER).sized(1.28F, 1.30F)
                    .clientTrackingRange(10).updateInterval(3)
                    .build(id("amethyst_shulker_minion").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<AmethystPhantomMinion>> AMETHYST_PHANTOM_MINION =
            TYPES.register("amethyst_phantom_minion", () -> EntityType.Builder
                    .of(AmethystPhantomMinion::new, MobCategory.MONSTER).sized(2.35F, 0.95F)
                    .clientTrackingRange(10).updateInterval(2)
                    .build(id("amethyst_phantom_minion").toString()));

    private BossEntities() {}

    public static void register(IEventBus bus) { TYPES.register(bus); }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SURFACE_GUARDIAN.get(), GuardianEntity.createAttributes().build());
        event.put(NETHER_GUARDIAN.get(), GuardianEntity.createAttributes().build());
        event.put(VOID_GUARDIAN.get(), GuardianEntity.createAttributes().build());
        event.put(DRAGON_GUARDIAN.get(), GuardianEntity.createAttributes().build());
        event.put(SOMITA_GUIDE.get(), SomitaGuideEntity.createAttributes().build());
        event.put(AMETHYST_CORRUPTED_ENDER_DRAGON.get(), AmethystCorruptedDragonEntity.createAttributes().build());
        event.put(EMERALD_ZOMBIE_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(EMERALD_CREEPER_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(EMERALD_GOLEM_SENTINEL_MINION.get(), EmeraldGolemSentinelMinion.createSentinelAttributes().build());
        event.put(CORRUPTED_ENDER_MITE_MINION.get(), CorruptedEnderMiteMinion.createMiteAttributes().build());
        event.put(EMERALD_RAVAGER_CUB_MINION.get(), EmeraldRavagerCubMinion.createCubAttributes().build());
        event.put(MOSSBOUND_SPIDER_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(GILDED_WITHER_SKELETON_VANGUARD_MINION.get(), GildedWitherSkeletonVanguardMinion.createVanguardAttributes().build());
        event.put(MOLTEN_STRIDER_MINION.get(), MoltenStriderMinion.createStriderAttributes().build());
        event.put(EMERALD_SKELETON_ARCHER_MINION.get(), EmeraldSkeletonArcherMinion.createArcherAttributes().build());
        event.put(GILDED_PIGLIN_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(GILDED_HOGLIN_RIDER_MINION.get(), GildedHoglinRiderMinion.createHoglinRiderAttributes().build());
        event.put(GILDED_BLAZE_WRAITH_MINION.get(), GildedBlazeWraithMinion.createWraithAttributes().build());
        event.put(VOID_ENDERMAN_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(VOID_ENDERMAN_SENTINEL_MINION.get(), VoidEndermanSentinelMinion.createSentinelAttributes().build());
        event.put(AMETHYST_EYE_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(AMETHYST_GUARDIAN_SHARD_MINION.get(), AmethystGuardianShardMinion.createShardAttributes().build());
        event.put(AMETHYST_SHULKER_MINION.get(), CorruptedMinionEntity.createAttributes().build());
        event.put(AMETHYST_PHANTOM_MINION.get(), AmethystPhantomMinion.createPhantomAttributes().build());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArlightBosses.MOD_ID, path);
    }
}
