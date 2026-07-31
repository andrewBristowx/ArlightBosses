package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Escolta de Wither Skeleton corrompido por oro. Pelea cuerpo a cuerpo con un hacha
 * grande — cada golpe conectado aplica Wither — y cada tanto, si tiene enemigos cerca,
 * pega un mazazo al piso que empuja en área a todo lo que lo rodea (útil para abrir
 * espacio y proteger a un jefe cercano de que lo rodeen).
 */
public final class GildedWitherSkeletonVanguardMinion extends CorruptedMinionEntity {
    private static final int SLAM_COOLDOWN_TICKS = 100;
    private static final int SLAM_WINDUP_TICKS = 8;
    private static final double SLAM_TRIGGER_RANGE = 4.0D;
    private static final double SLAM_RADIUS = 3.5D;
    private static final int WITHER_DURATION_TICKS = 100;

    private int slamCooldown;
    private int slamWindup;

    public GildedWitherSkeletonVanguardMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.gilded_wither_vanguard"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.ASH; }
    @Override protected double defaultAttackReach() { return 3.0D; }

    /** Tanque de escolta: más vida y resistencia a empujones que un esbirro base, para aguantar el frente. */
    public static AttributeSupplier.Builder createVanguardAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 38.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (slamCooldown > 0) slamCooldown--;
        if (slamWindup > 0) {
            slamWindup--;
            getNavigation().stop();
            if (slamWindup == 0) performGroundSlamImpact();
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || slamCooldown > 0) return;
        if (distanceToSqr(target) <= SLAM_TRIGGER_RANGE * SLAM_TRIGGER_RANGE) {
            beginGroundSlam();
        }
    }

    private void beginGroundSlam() {
        slamCooldown = SLAM_COOLDOWN_TICKS;
        slamWindup = SLAM_WINDUP_TICKS;
        getNavigation().stop();
        playAttackAnimation();
    }

    private void performGroundSlamImpact() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        serverLevel.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.0F, 0.7F);
        serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.1D, getZ(),
                24, 1.6D, 0.1D, 1.6D, 0.02D);

        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(SLAM_RADIUS), entity -> entity != this)) {
            double dx = living.getX() - getX();
            double dz = living.getZ() - getZ();
            double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            living.hurt(damageSources().mobAttack(this), configuredAttackDamage() * 0.5F);
            living.knockback(0.9D, -dx / length, -dz / length);
        }
    }

    /** El hacha corrompida deja Wither en cada golpe conectado. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, 0));
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
}
