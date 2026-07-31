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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Araña de la superficie cubierta de musgo y esmeralda. Trepa paredes como una araña
 * real, y de tanto en tanto marca el suelo bajo el objetivo: si no se mueve de ahí a
 * tiempo, las raíces brotan y lo atrapan (Lentitud fuerte). La mordida cuerpo a cuerpo
 * también entorpece, aunque más suave que la trampa.
 */
public final class MossboundSpiderMinion extends CorruptedMinionEntity {
    private static final int SNARE_COOLDOWN_TICKS = 110;
    private static final int SNARE_TELEGRAPH_TICKS = 15;
    private static final double SNARE_MIN_RANGE = 3.0D;
    private static final double SNARE_MAX_RANGE = 10.0D;
    private static final double SNARE_CATCH_RADIUS = 1.4D;
    private static final int BITE_SLOW_DURATION_TICKS = 60;
    private static final int SNARE_SLOW_DURATION_TICKS = 90;

    private int snareCooldown;
    private int snareTelegraphTicks;
    private Vec3 snarePoint = Vec3.ZERO;

    public MossboundSpiderMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.mossbound_spider"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.HAPPY_VILLAGER; }
    @Override protected double defaultAttackReach() { return 1.6D; }

    /** Se aferra a paredes igual que una araña vainilla. */
    @Override
    public boolean onClimbable() {
        return horizontalCollision;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (snareCooldown > 0) snareCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            snareTelegraphTicks = 0;
            return;
        }

        if (snareTelegraphTicks > 0) {
            snareTelegraphTicks--;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (level() instanceof ServerLevel serverLevel && snareTelegraphTicks % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, snarePoint.x, snarePoint.y + 0.1D, snarePoint.z,
                        2, 0.15D, 0.05D, 0.15D, 0.0D);
            }
            if (snareTelegraphTicks == 0) triggerSnare();
            return;
        }

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (snareCooldown <= 0 && horizontalDistance >= SNARE_MIN_RANGE && horizontalDistance <= SNARE_MAX_RANGE
                && getSensing().hasLineOfSight(target)) {
            snareTelegraphTicks = SNARE_TELEGRAPH_TICKS;
            snarePoint = target.position();
            level().playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT,
                    SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private void triggerSnare() {
        snareCooldown = SNARE_COOLDOWN_TICKS;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, snarePoint.x, snarePoint.y, snarePoint.z, SoundEvents.MOSS_BREAK,
                SoundSource.HOSTILE, 1.0F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, snarePoint.x, snarePoint.y + 0.2D, snarePoint.z,
                16, 0.3D, 0.2D, 0.3D, 0.02D);
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(snarePoint.x - SNARE_CATCH_RADIUS, snarePoint.y - 1.0D, snarePoint.z - SNARE_CATCH_RADIUS,
                        snarePoint.x + SNARE_CATCH_RADIUS, snarePoint.y + 2.0D, snarePoint.z + SNARE_CATCH_RADIUS),
                entity -> entity != this)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SNARE_SLOW_DURATION_TICKS, 3));
            living.hurt(damageSources().mobAttack(this), configuredAttackDamage() * 0.4F);
        }
    }

    /** La mordida cuerpo a cuerpo entorpece más suave que la trampa de raíces. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BITE_SLOW_DURATION_TICKS, 1));
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SPIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SPIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
}
