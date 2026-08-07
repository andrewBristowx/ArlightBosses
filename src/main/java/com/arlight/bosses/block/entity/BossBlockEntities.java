package com.arlight.bosses.block.entity;

import com.arlight.bosses.ArlightBosses;
import com.arlight.bosses.block.BossBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BossBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArlightBosses.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetherDungeonLockBlockEntity>> NETHER_DUNGEON_LOCK =
            BLOCK_ENTITY_TYPES.register("nether_dungeon_lock", () -> BlockEntityType.Builder.of(
                    NetherDungeonLockBlockEntity::new, BossBlocks.NETHER_DUNGEON_LOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorruptedPearlAltarBlockEntity>> CORRUPTED_PEARL_ALTAR =
            BLOCK_ENTITY_TYPES.register("corrupted_pearl_altar", () -> BlockEntityType.Builder.of(
                    CorruptedPearlAltarBlockEntity::new, BossBlocks.CORRUPTED_PEARL_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MedalPedestalBlockEntity>> MEDAL_PEDESTAL =
            BLOCK_ENTITY_TYPES.register("medal_pedestal", () -> BlockEntityType.Builder.of(
                    MedalPedestalBlockEntity::new,
                    BossBlocks.HOME_MEDAL_PEDESTAL.get(), BossBlocks.TRADE_MEDAL_PEDESTAL.get(),
                    BossBlocks.BASTION_MEDAL_PEDESTAL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TreasureChestBlockEntity>> TREASURE_CHEST =
            BLOCK_ENTITY_TYPES.register("treasure_chest", () -> BlockEntityType.Builder.of(
                    TreasureChestBlockEntity::new,
                    BossBlocks.COPPER_TREASURE_CHEST.get(), BossBlocks.IRON_TREASURE_CHEST.get(),
                    BossBlocks.PONY_TREASURE_CHEST.get(), BossBlocks.DIAMOND_TREASURE_CHEST.get(),
                    BossBlocks.NETHERITE_TREASURE_CHEST.get()).build(null));

    private BossBlockEntities() { }
    public static void register(IEventBus bus) { BLOCK_ENTITY_TYPES.register(bus); }
}
