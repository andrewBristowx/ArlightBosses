package com.arlight.bosses.block;

import com.arlight.bosses.ArlightBosses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Bloques de misión y contenedores visuales usados por ArlightBingo. */
public final class BossBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ArlightBosses.MOD_ID);

    public static final DeferredHolder<Block, CorruptedPearlAltarBlock> CORRUPTED_PEARL_ALTAR =
            BLOCKS.register("corrupted_pearl_altar", () -> new CorruptedPearlAltarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .strength(8.0F, 1200.0F).sound(SoundType.AMETHYST)
                            .lightLevel(state -> 10).noOcclusion().requiresCorrectToolForDrops().noLootTable()));

    public static final DeferredHolder<Block, NetherDungeonLockBlock> NETHER_DUNGEON_LOCK =
            BLOCKS.register("nether_dungeon_lock", () -> new NetherDungeonLockBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                            .strength(12.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                            .lightLevel(state -> state.getValue(NetherDungeonLockBlock.LOCK_STATE)
                                    == NetherDungeonLockBlock.LockState.OPENED ? 5 : 12)
                            .noOcclusion().requiresCorrectToolForDrops().noLootTable()));

    public static final DeferredHolder<Block, MedalPedestalBlock> HOME_MEDAL_PEDESTAL = pedestal(
            "home_medal_pedestal", MedalPedestalBlock.MedalKind.HOME,
            MapColor.COLOR_GREEN, SoundType.MOSS);
    public static final DeferredHolder<Block, MedalPedestalBlock> TRADE_MEDAL_PEDESTAL = pedestal(
            "trade_medal_pedestal", MedalPedestalBlock.MedalKind.TRADE,
            MapColor.COLOR_ORANGE, SoundType.COPPER);
    public static final DeferredHolder<Block, MedalPedestalBlock> BASTION_MEDAL_PEDESTAL = pedestal(
            "bastion_medal_pedestal", MedalPedestalBlock.MedalKind.BASTION,
            MapColor.COLOR_GREEN, SoundType.DEEPSLATE);

    public static final DeferredHolder<Block, TreasureChestBlock> COPPER_TREASURE_CHEST = chest(
            "copper_treasure_chest", MapColor.COLOR_ORANGE, SoundType.COPPER, 3);
    public static final DeferredHolder<Block, TreasureChestBlock> IRON_TREASURE_CHEST = chest(
            "iron_treasure_chest", MapColor.METAL, SoundType.METAL, 4);
    public static final DeferredHolder<Block, TreasureChestBlock> PONY_TREASURE_CHEST = chest(
            "pony_treasure_chest", MapColor.COLOR_PINK, SoundType.CHERRY_WOOD, 7);
    public static final DeferredHolder<Block, TreasureChestBlock> DIAMOND_TREASURE_CHEST = chest(
            "diamond_treasure_chest", MapColor.COLOR_LIGHT_BLUE, SoundType.AMETHYST, 9);
    public static final DeferredHolder<Block, TreasureChestBlock> NETHERITE_TREASURE_CHEST = chest(
            "netherite_treasure_chest", MapColor.COLOR_BLACK, SoundType.NETHERITE_BLOCK, 11);

    private static DeferredHolder<Block, MedalPedestalBlock> pedestal(
            String id, MedalPedestalBlock.MedalKind kind, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new MedalPedestalBlock(kind,
                BlockBehaviour.Properties.of().mapColor(color).strength(10.0F, 1200.0F)
                        .sound(sound)
                        .lightLevel(state -> state.getValue(MedalPedestalBlock.PEDESTAL_STATE)
                                == MedalPedestalBlock.PedestalState.EMPTY ? 4 : 11)
                        .noOcclusion().requiresCorrectToolForDrops().noLootTable()));
    }

    private static DeferredHolder<Block, TreasureChestBlock> chest(
            String id, MapColor color, SoundType sound, int light) {
        return BLOCKS.register(id, () -> new TreasureChestBlock(
                BlockBehaviour.Properties.of().mapColor(color).strength(8.0F, 1200.0F)
                        .sound(sound).lightLevel(state -> light).noOcclusion()
                        .requiresCorrectToolForDrops().noLootTable()));
    }

    private BossBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
