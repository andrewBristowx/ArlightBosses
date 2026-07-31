package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;

/**
 * Proyectil de corrupción esmeralda.
 *
 * <p>Normalmente funciona como la flecha del arquero. Cuando el Guardián de la
 * Superficie activa la variante de piedra, el mismo proyectil se sincroniza y
 * renderiza como una roca de musgo, rompe con sonido de piedra y deja de aplicar
 * veneno. Así no se modifica el ataque normal del esqueleto.</p>
 */
public final class EmeraldCorruptionArrow extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> STONE_VARIANT =
            SynchedEntityData.defineId(EmeraldCorruptionArrow.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions TRAIL_PARTICLE =
            new DustParticleOptions(new Vector3f(0.31F, 0.86F, 0.42F), 1.1F);
    private static final BlockParticleOption STONE_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState());

    public EmeraldCorruptionArrow(EntityType<? extends EmeraldCorruptionArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public EmeraldCorruptionArrow(EntityType<? extends EmeraldCorruptionArrow> type, LivingEntity shooter, Level level) {
        super(type, shooter, level, new ItemStack(Items.ARROW), null);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STONE_VARIANT, false);
    }

    public void setStoneVariant(boolean stoneVariant) {
        entityData.set(STONE_VARIANT, stoneVariant);
        if (stoneVariant) setSoundEvent(SoundEvents.STONE_HIT);
    }

    public boolean isStoneVariant() {
        return entityData.get(STONE_VARIANT);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected double getDefaultGravity() {
        return isStoneVariant() ? 0.075D : super.getDefaultGravity();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
        return isStoneVariant() ? SoundEvents.STONE_BREAK : super.getDefaultHitGroundSoundEvent();
    }

    @Override
    public void tick() {
        super.tick();
        // Proyectiles de combate temporales: nunca deben quedar guardados indefinidamente
        // en chunks ni sobrevivir al cierre de una arena.
        if (tickCount > 600 || (inGround && tickCount > 80)) {
            discard();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 2 != 0) return;

        if (isStoneVariant()) {
            serverLevel.sendParticles(STONE_PARTICLE, getX(), getY() + 0.1D, getZ(),
                    2, 0.08D, 0.08D, 0.08D, 0.01D);
        } else {
            serverLevel.sendParticles(TRAIL_PARTICLE, getX(), getY() + 0.1D, getZ(),
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity living)) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (isStoneVariant()) {
            burstStone(serverLevel, living.getX(), living.getY() + living.getBbHeight() * 0.45D, living.getZ());
            double dx = living.getX() - getX();
            double dz = living.getZ() - getZ();
            double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            living.knockback(0.55D, -dx / length, -dz / length);
            discard();
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        serverLevel.sendParticles(TRAIL_PARTICLE,
                living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                12, 0.25D, 0.35D, 0.25D, 0.01D);
        serverLevel.playSound(null, living.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL,
                SoundSource.HOSTILE, 0.6F, 1.3F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (isStoneVariant() && level() instanceof ServerLevel serverLevel) {
            burstStone(serverLevel, result.getLocation().x, result.getLocation().y, result.getLocation().z);
            discard();
        }
    }

    private void burstStone(ServerLevel world, double x, double y, double z) {
        world.sendParticles(STONE_PARTICLE, x, y, z,
                30, 0.45D, 0.45D, 0.45D, 0.12D);
        world.playSound(null, blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, 1.1F, 0.78F + random.nextFloat() * 0.22F);
    }

    /** Los proyectiles son efectos temporales de combate y nunca se persisten en chunks. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // No llama a AbstractArrow: sus ItemStacks internos son irrelevantes porque
        // este proyectil de arena nunca debe persistir.
        tag.putBoolean("ArlightStoneVariant", isStoneVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("ArlightStoneVariant")) setStoneVariant(tag.getBoolean("ArlightStoneVariant"));
    }

    @Override
    public boolean isNoPhysics() {
        return false;
    }
}
