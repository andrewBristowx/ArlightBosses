package com.arlight.bosses.item;

import com.arlight.bosses.ArlightBosses;
import com.arlight.bosses.entity.BossEntities;
import com.arlight.bosses.block.BossBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Huevos de aparición para todas las criaturas registradas del mod.
 * Incluye guardianes, esbirros, Somita guía y el Dragón Corrupto de Amatista. Al ser {@link SpawnEggItem}
 * normales, funcionan igual que cualquier huevo vainilla al usarlos sobre un Monster
 * Spawner: fijan ese tipo de entidad como la que el spawner va a generar.
 */
public final class BossItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ArlightBosses.MOD_ID);


    public static final DeferredHolder<Item, Item> IGNEOUS_LEGENDARY_KEY =
            ITEMS.register("igneous_legendary_key", () -> new CampaignKeyItem(
                    "item.arlightbosses.igneous_legendary_key.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> EMERALDIZED_DRAGON_KEY =
            ITEMS.register("emeraldized_dragon_key", () -> new CampaignKeyItem(
                    "item.arlightbosses.emeraldized_dragon_key.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> MOSSBOUND_HOME_MEDAL =
            ITEMS.register("mossbound_home_medal", () -> new CampaignKeyItem(
                    "item.arlightbosses.mossbound_home_medal.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> GILDED_TRADE_MEDAL =
            ITEMS.register("gilded_trade_medal", () -> new CampaignKeyItem(
                    "item.arlightbosses.gilded_trade_medal.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, Item> EMERALD_BASTION_MEDAL =
            ITEMS.register("emerald_bastion_medal", () -> new CampaignKeyItem(
                    "item.arlightbosses.emerald_bastion_medal.lore",
                    new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<Item, BlockItem> CORRUPTED_PEARL_ALTAR =
            ITEMS.register("corrupted_pearl_altar", () -> new BlockItem(
                    BossBlocks.CORRUPTED_PEARL_ALTAR.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<Item, BlockItem> NETHER_DUNGEON_LOCK =
            ITEMS.register("nether_dungeon_lock", () -> new BlockItem(
                    BossBlocks.NETHER_DUNGEON_LOCK.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<Item, BlockItem> HOME_MEDAL_PEDESTAL =
            ITEMS.register("home_medal_pedestal", () -> new BlockItem(
                    BossBlocks.HOME_MEDAL_PEDESTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<Item, BlockItem> TRADE_MEDAL_PEDESTAL =
            ITEMS.register("trade_medal_pedestal", () -> new BlockItem(
                    BossBlocks.TRADE_MEDAL_PEDESTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<Item, BlockItem> BASTION_MEDAL_PEDESTAL =
            ITEMS.register("bastion_medal_pedestal", () -> new BlockItem(
                    BossBlocks.BASTION_MEDAL_PEDESTAL.get(),
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<Item, BlockItem> COPPER_TREASURE_CHEST = treasureBlockItem(
            "copper_treasure_chest", BossBlocks.COPPER_TREASURE_CHEST, Rarity.COMMON);
    public static final DeferredHolder<Item, BlockItem> IRON_TREASURE_CHEST = treasureBlockItem(
            "iron_treasure_chest", BossBlocks.IRON_TREASURE_CHEST, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, BlockItem> PONY_TREASURE_CHEST = treasureBlockItem(
            "pony_treasure_chest", BossBlocks.PONY_TREASURE_CHEST, Rarity.RARE);
    public static final DeferredHolder<Item, BlockItem> DIAMOND_TREASURE_CHEST = treasureBlockItem(
            "diamond_treasure_chest", BossBlocks.DIAMOND_TREASURE_CHEST, Rarity.RARE);
    public static final DeferredHolder<Item, BlockItem> NETHERITE_TREASURE_CHEST = treasureBlockItem(
            "netherite_treasure_chest", BossBlocks.NETHERITE_TREASURE_CHEST, Rarity.EPIC);

    public static final DeferredHolder<Item, SpawnEggItem> SURFACE_GUARDIAN_SPAWN_EGG =
            registerEgg("surface_guardian_spawn_egg", BossEntities.SURFACE_GUARDIAN::get, 0x344A25, 0x9BE45D);
    public static final DeferredHolder<Item, SpawnEggItem> NETHER_GUARDIAN_SPAWN_EGG =
            registerEgg("nether_guardian_spawn_egg", BossEntities.NETHER_GUARDIAN::get, 0x25100B, 0xFF6B16);
    public static final DeferredHolder<Item, SpawnEggItem> VOID_GUARDIAN_SPAWN_EGG =
            registerEgg("void_guardian_spawn_egg", BossEntities.VOID_GUARDIAN::get, 0x160922, 0xC13CFF);
    public static final DeferredHolder<Item, SpawnEggItem> DRAGON_GUARDIAN_SPAWN_EGG =
            registerEgg("dragon_guardian_spawn_egg", BossEntities.DRAGON_GUARDIAN::get, 0x3B1739, 0xFF8BCB);

    public static final DeferredHolder<Item, SpawnEggItem> SOMITA_GUIDE_SPAWN_EGG =
            registerEgg("somita_guide_spawn_egg", BossEntities.SOMITA_GUIDE::get, 0xF7A6D2, 0x713474);

    public static final DeferredHolder<Item, SpawnEggItem> AMETHYST_CORRUPTED_ENDER_DRAGON_SPAWN_EGG =
            registerEgg("amethyst_corrupted_ender_dragon_spawn_egg",
                    BossEntities.AMETHYST_CORRUPTED_ENDER_DRAGON::get, 0x2B083D, 0xD16CFF);
    public static final DeferredHolder<Item, SpawnEggItem> EMERALD_ZOMBIE_MINION_SPAWN_EGG =
            registerEgg("emerald_zombie_minion_spawn_egg", BossEntities.EMERALD_ZOMBIE_MINION::get, 0x2E7D32, 0x50C878);
    public static final DeferredHolder<Item, SpawnEggItem> EMERALD_CREEPER_MINION_SPAWN_EGG =
            registerEgg("emerald_creeper_minion_spawn_egg", BossEntities.EMERALD_CREEPER_MINION::get, 0x2F6B3A, 0x39FF6A);
    public static final DeferredHolder<Item, SpawnEggItem> EMERALD_RAVAGER_CUB_MINION_SPAWN_EGG =
            registerEgg("emerald_ravager_cub_minion_spawn_egg", BossEntities.EMERALD_RAVAGER_CUB_MINION::get, 0x6C543C, 0x3AB86E);
    public static final DeferredHolder<Item, SpawnEggItem> MOSSBOUND_SPIDER_MINION_SPAWN_EGG =
            registerEgg("mossbound_spider_minion_spawn_egg", BossEntities.MOSSBOUND_SPIDER_MINION::get, 0x28221A, 0x3A803E);
    public static final DeferredHolder<Item, SpawnEggItem> GILDED_WITHER_SKELETON_VANGUARD_MINION_SPAWN_EGG =
            registerEgg("gilded_wither_skeleton_vanguard_minion_spawn_egg", BossEntities.GILDED_WITHER_SKELETON_VANGUARD_MINION::get, 0x2A1A08, 0xC8A03C);
    public static final DeferredHolder<Item, SpawnEggItem> MOLTEN_STRIDER_MINION_SPAWN_EGG =
            registerEgg("molten_strider_minion_spawn_egg", BossEntities.MOLTEN_STRIDER_MINION::get, 0x8A5028, 0xE8781C);
    public static final DeferredHolder<Item, SpawnEggItem> AMETHYST_GUARDIAN_SHARD_MINION_SPAWN_EGG =
            registerEgg("amethyst_guardian_shard_minion_spawn_egg", BossEntities.AMETHYST_GUARDIAN_SHARD_MINION::get, 0x40245C, 0xD6ABFF);
    public static final DeferredHolder<Item, SpawnEggItem> EMERALD_GOLEM_SENTINEL_MINION_SPAWN_EGG =
            registerEgg("emerald_golem_sentinel_minion_spawn_egg", BossEntities.EMERALD_GOLEM_SENTINEL_MINION::get, 0x5A5C58, 0x32BE6E);
    public static final DeferredHolder<Item, SpawnEggItem> CORRUPTED_ENDER_MITE_MINION_SPAWN_EGG =
            registerEgg("corrupted_ender_mite_minion_spawn_egg", BossEntities.CORRUPTED_ENDER_MITE_MINION::get, 0x2D1E3C, 0x9660DC);
    public static final DeferredHolder<Item, SpawnEggItem> EMERALD_SKELETON_ARCHER_MINION_SPAWN_EGG =
            registerEgg("emerald_skeleton_archer_minion_spawn_egg", BossEntities.EMERALD_SKELETON_ARCHER_MINION::get, 0xD6D2C4, 0x39A85A);
    public static final DeferredHolder<Item, SpawnEggItem> GILDED_PIGLIN_MINION_SPAWN_EGG =
            registerEgg("gilded_piglin_minion_spawn_egg", BossEntities.GILDED_PIGLIN_MINION::get, 0xC79A3B, 0xF2D06B);
    public static final DeferredHolder<Item, SpawnEggItem> GILDED_HOGLIN_RIDER_MINION_SPAWN_EGG =
            registerEgg("gilded_hoglin_rider_minion_spawn_egg", BossEntities.GILDED_HOGLIN_RIDER_MINION::get, 0x8B5A2B, 0xE6B325);
    public static final DeferredHolder<Item, SpawnEggItem> GILDED_BLAZE_WRAITH_MINION_SPAWN_EGG =
            registerEgg("gilded_blaze_wraith_minion_spawn_egg", BossEntities.GILDED_BLAZE_WRAITH_MINION::get, 0xA87A1E, 0xFFE07A);
    public static final DeferredHolder<Item, SpawnEggItem> VOID_ENDERMAN_MINION_SPAWN_EGG =
            registerEgg("void_enderman_minion_spawn_egg", BossEntities.VOID_ENDERMAN_MINION::get, 0x120022, 0x8B00FF);
    public static final DeferredHolder<Item, SpawnEggItem> VOID_ENDERMAN_SENTINEL_MINION_SPAWN_EGG =
            registerEgg("void_enderman_sentinel_minion_spawn_egg", BossEntities.VOID_ENDERMAN_SENTINEL_MINION::get, 0x0A0016, 0xB040FF);
    public static final DeferredHolder<Item, SpawnEggItem> AMETHYST_EYE_MINION_SPAWN_EGG =
            registerEgg("amethyst_eye_minion_spawn_egg", BossEntities.AMETHYST_EYE_MINION::get, 0x3B0764, 0xB983FF);
    public static final DeferredHolder<Item, SpawnEggItem> AMETHYST_SHULKER_MINION_SPAWN_EGG =
            registerEgg("amethyst_shulker_minion_spawn_egg", BossEntities.AMETHYST_SHULKER_MINION::get, 0x4B0082, 0xC9A0FF);
    public static final DeferredHolder<Item, SpawnEggItem> AMETHYST_PHANTOM_MINION_SPAWN_EGG =
            registerEgg("amethyst_phantom_minion_spawn_egg", BossEntities.AMETHYST_PHANTOM_MINION::get, 0x462D5F, 0xBE78F0);

    private BossItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(BossItems::buildCreativeTab);
    }

    private static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(IGNEOUS_LEGENDARY_KEY.get());
            event.accept(EMERALDIZED_DRAGON_KEY.get());
            event.accept(MOSSBOUND_HOME_MEDAL.get());
            event.accept(GILDED_TRADE_MEDAL.get());
            event.accept(EMERALD_BASTION_MEDAL.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CORRUPTED_PEARL_ALTAR.get());
            event.accept(NETHER_DUNGEON_LOCK.get());
            event.accept(HOME_MEDAL_PEDESTAL.get());
            event.accept(TRADE_MEDAL_PEDESTAL.get());
            event.accept(BASTION_MEDAL_PEDESTAL.get());
            event.accept(COPPER_TREASURE_CHEST.get());
            event.accept(IRON_TREASURE_CHEST.get());
            event.accept(PONY_TREASURE_CHEST.get());
            event.accept(DIAMOND_TREASURE_CHEST.get());
            event.accept(NETHERITE_TREASURE_CHEST.get());
        }
        if (event.getTabKey() != CreativeModeTabs.SPAWN_EGGS) return;
        event.accept(SURFACE_GUARDIAN_SPAWN_EGG.get());
        event.accept(NETHER_GUARDIAN_SPAWN_EGG.get());
        event.accept(VOID_GUARDIAN_SPAWN_EGG.get());
        event.accept(DRAGON_GUARDIAN_SPAWN_EGG.get());
        event.accept(SOMITA_GUIDE_SPAWN_EGG.get());
        event.accept(AMETHYST_CORRUPTED_ENDER_DRAGON_SPAWN_EGG.get());
        event.accept(EMERALD_ZOMBIE_MINION_SPAWN_EGG.get());
        event.accept(EMERALD_CREEPER_MINION_SPAWN_EGG.get());
        event.accept(EMERALD_RAVAGER_CUB_MINION_SPAWN_EGG.get());
        event.accept(MOSSBOUND_SPIDER_MINION_SPAWN_EGG.get());
        event.accept(GILDED_WITHER_SKELETON_VANGUARD_MINION_SPAWN_EGG.get());
        event.accept(MOLTEN_STRIDER_MINION_SPAWN_EGG.get());
        event.accept(AMETHYST_GUARDIAN_SHARD_MINION_SPAWN_EGG.get());
        event.accept(EMERALD_GOLEM_SENTINEL_MINION_SPAWN_EGG.get());
        event.accept(CORRUPTED_ENDER_MITE_MINION_SPAWN_EGG.get());
        event.accept(EMERALD_SKELETON_ARCHER_MINION_SPAWN_EGG.get());
        event.accept(GILDED_PIGLIN_MINION_SPAWN_EGG.get());
        event.accept(GILDED_HOGLIN_RIDER_MINION_SPAWN_EGG.get());
        event.accept(GILDED_BLAZE_WRAITH_MINION_SPAWN_EGG.get());
        event.accept(VOID_ENDERMAN_MINION_SPAWN_EGG.get());
        event.accept(VOID_ENDERMAN_SENTINEL_MINION_SPAWN_EGG.get());
        event.accept(AMETHYST_EYE_MINION_SPAWN_EGG.get());
        event.accept(AMETHYST_SHULKER_MINION_SPAWN_EGG.get());
        event.accept(AMETHYST_PHANTOM_MINION_SPAWN_EGG.get());
    }

    private static DeferredHolder<Item, BlockItem> treasureBlockItem(
            String id, java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block, Rarity rarity) {
        return ITEMS.register(id, () -> new TreasureChestBlockItem(block.get(),
                new Item.Properties().rarity(rarity).fireResistant(), id));
    }

    private static DeferredHolder<Item, SpawnEggItem> registerEgg(
            String name,
            java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,
            int backgroundColor, int highlightColor) {
        return ITEMS.register(name, () -> new DeferredSpawnEggItem(type, backgroundColor, highlightColor,
                new Item.Properties()));
    }
}
