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
import net.minecraft.world.entity.Entity;
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
 * Esquirla de oro corrompido.
 *
 * <p>La variante normal sigue siendo el proyectil ligero del piglin. La variante
 * de bloque de oro es exclusiva del Guardián del Nether: cae desde arriba,
 * renderiza un cubo de oro y genera un impacto pesado al tocar suelo o entidad.</p>
 */
public final class GildedShardProjectile extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> GOLD_BLOCK_VARIANT =
            SynchedEntityData.defineId(GildedShardProjectile.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions TRAIL_PARTICLE =
            new DustParticleOptions(new Vector3f(0.92F, 0.75F, 0.25F), 1.0F);
    private static final BlockParticleOption GOLD_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState());

    public GildedShardProjectile(EntityType<? extends GildedShardProjectile> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public GildedShardProjectile(EntityType<? extends GildedShardProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level, new ItemStack(Items.ARROW), null);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GOLD_BLOCK_VARIANT, false);
    }

    public void setGoldBlockVariant(boolean goldBlockVariant) {
        entityData.set(GOLD_BLOCK_VARIANT, goldBlockVariant);
        if (goldBlockVariant) setSoundEvent(SoundEvents.ANVIL_LAND);
    }

    public boolean isGoldBlockVariant() {
        return entityData.get(GOLD_BLOCK_VARIANT);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected double getDefaultGravity() {
        return isGoldBlockVariant() ? 0.12D : super.getDefaultGravity();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
        return isGoldBlockVariant() ? SoundEvents.ANVIL_LAND : super.getDefaultHitGroundSoundEvent();
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

        if (isGoldBlockVariant()) {
            serverLevel.sendParticles(GOLD_PARTICLE, getX(), getY() + 0.1D, getZ(),
                    2, 0.08D, 0.08D, 0.08D, 0.01D);
        } else {
            serverLevel.sendParticles(TRAIL_PARTICLE, getX(), getY() + 0.1D, getZ(),
                    1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity living)) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (isGoldBlockVariant()) {
            heavyGoldImpact(serverLevel, living.getX(), living.getY(), living.getZ(), living);
            discard();
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        double dx = living.getX() - getX();
        double dz = living.getZ() - getZ();
        double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        living.knockback(0.4D, -dx / length, -dz / length);
        serverLevel.sendParticles(TRAIL_PARTICLE,
                living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                10, 0.2D, 0.3D, 0.2D, 0.01D);
        serverLevel.playSound(null, living.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.4F, 1.6F);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (isGoldBlockVariant() && level() instanceof ServerLevel serverLevel) {
            heavyGoldImpact(serverLevel,
                    result.getLocation().x, result.getLocation().y, result.getLocation().z, null);
        }
        discard();
    }

    private void heavyGoldImpact(ServerLevel world, double x, double y, double z, LivingEntity directHit) {
        world.sendParticles(GOLD_PARTICLE, x, y + 0.2D, z,
                48, 0.7D, 0.45D, 0.7D, 0.18D);
        world.sendParticles(ParticleTypes.CLOUD, x, y + 0.15D, z,
                18, 0.65D, 0.12D, 0.65D, 0.05D);
        world.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.15F, 0.72F + random.nextFloat() * 0.12F);
        world.playSound(null, blockPosition(), SoundEvents.NETHER_GOLD_ORE_BREAK,
                SoundSource.HOSTILE, 0.85F, 0.85F);

        Entity owner = getOwner();
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(2.35D), entity -> entity.isAlive() && entity != owner)) {
            if (victim == directHit) continue; // el impacto directo ya recibió el daño de AbstractArrow
            victim.hurt(damageSources().fallingBlock(this), (float) Math.max(4.0D, getBaseDamage() * 0.55D));
            double dx = victim.getX() - x;
            double dz = victim.getZ() - z;
            double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            victim.knockback(0.55D, -dx / length, -dz / length);
        }
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
        tag.putBoolean("ArlightGoldBlockVariant", isGoldBlockVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("ArlightGoldBlockVariant")) {
            setGoldBlockVariant(tag.getBoolean("ArlightGoldBlockVariant"));
        }
    }
}
