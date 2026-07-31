package com.arlight.bosses.block.entity;

import com.arlight.bosses.block.TreasureChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Cofre real de 27 espacios con apertura/cierre nativos. Además de los menús
 * vanilla, expone dos métodos públicos sin dependencias Bukkit para que
 * ArlightBingo pueda reproducir la animación al abrir su inventario personal.
 */
public final class TreasureChestBlockEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity {
    private static final int SIZE = 27;
    private static final int OPENING_TICKS = 14;
    private static final int CLOSING_TICKS = 12;
    private static final String CONTROLLER = "treasure_chest";
    private static final String OPEN_TRIGGER = "open";
    private static final String CLOSE_TRIGGER = "close";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String variantPath;
    private final RawAnimation closedAnimation;
    private final RawAnimation openingAnimation;
    private final RawAnimation openedAnimation;
    private final RawAnimation closingAnimation;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int viewers;
    private int transitionTicks;

    public TreasureChestBlockEntity(BlockPos pos, BlockState state) {
        super(BossBlockEntities.TREASURE_CHEST.get(), pos, state);
        this.variantPath = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        this.closedAnimation = RawAnimation.begin().thenLoop("animation." + variantPath + ".closed");
        this.openingAnimation = RawAnimation.begin().thenPlay("animation." + variantPath + ".opening");
        this.openedAnimation = RawAnimation.begin().thenLoop("animation." + variantPath + ".opened");
        this.closingAnimation = RawAnimation.begin().thenPlay("animation." + variantPath + ".closing");
    }

    public String variantPath() { return variantPath; }

    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.arlightbosses.treasure_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (player.isSpectator()) return;
        viewers++;
        if (viewers == 1) playOpening(true);
    }

    @Override
    public void stopOpen(Player player) {
        if (player.isSpectator()) return;
        viewers = Math.max(0, viewers - 1);
        if (viewers == 0) playClosing(true);
    }

    /** Llamado por reflexión desde ArlightBingo; no sustituye el bloque ni su inventario. */
    public void playExternalOpen() { playOpening(false); }

    /** Llamado por reflexión desde ArlightBingo al cerrar el inventario personal. */
    public void playExternalClose() { playClosing(false); }

    private void playOpening(boolean playSound) {
        transitionTicks = 0;
        setVisualState(TreasureChestBlock.ChestState.OPENING);
        if (level != null && !level.isClientSide) {
            triggerAnim(CONTROLLER, OPEN_TRIGGER);
            if (playSound) level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN,
                    SoundSource.BLOCKS, 0.65F, 0.94F + level.random.nextFloat() * 0.08F);
        }
    }

    private void playClosing(boolean playSound) {
        transitionTicks = 0;
        setVisualState(TreasureChestBlock.ChestState.CLOSING);
        if (level != null && !level.isClientSide) {
            triggerAnim(CONTROLLER, CLOSE_TRIGGER);
            if (playSound) level.playSound(null, worldPosition, SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS, 0.60F, 0.94F + level.random.nextFloat() * 0.08F);
        }
    }

    private void setVisualState(TreasureChestBlock.ChestState state) {
        if (level == null) return;
        BlockState current = getBlockState();
        if (current.hasProperty(TreasureChestBlock.CHEST_STATE)
                && current.getValue(TreasureChestBlock.CHEST_STATE) != state) {
            level.setBlock(worldPosition, current.setValue(TreasureChestBlock.CHEST_STATE, state), 3);
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TreasureChestBlockEntity chest) {
        if (level.isClientSide || !state.hasProperty(TreasureChestBlock.CHEST_STATE)) return;
        TreasureChestBlock.ChestState visual = state.getValue(TreasureChestBlock.CHEST_STATE);
        if (visual != TreasureChestBlock.ChestState.OPENING
                && visual != TreasureChestBlock.ChestState.CLOSING) return;
        chest.transitionTicks++;
        if (visual == TreasureChestBlock.ChestState.OPENING && chest.transitionTicks >= OPENING_TICKS) {
            chest.transitionTicks = 0;
            chest.setVisualState(TreasureChestBlock.ChestState.OPENED);
        } else if (visual == TreasureChestBlock.ChestState.CLOSING && chest.transitionTicks >= CLOSING_TICKS) {
            chest.transitionTicks = 0;
            chest.setVisualState(TreasureChestBlock.ChestState.CLOSED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TreasureChestBlockEntity> controller = new AnimationController<>(
                this, CONTROLLER, 1, state -> {
            BlockState blockState = getBlockState();
            if (!blockState.hasProperty(TreasureChestBlock.CHEST_STATE)) {
                return state.setAndContinue(closedAnimation);
            }
            return switch (blockState.getValue(TreasureChestBlock.CHEST_STATE)) {
                case CLOSED -> state.setAndContinue(closedAnimation);
                case OPENING -> state.setAndContinue(openingAnimation);
                case OPENED -> state.setAndContinue(openedAnimation);
                case CLOSING -> state.setAndContinue(closingAnimation);
            };
        });
        controller.triggerableAnim(OPEN_TRIGGER, openingAnimation);
        controller.triggerableAnim(CLOSE_TRIGGER, closingAnimation);
        controllers.add(controller);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
