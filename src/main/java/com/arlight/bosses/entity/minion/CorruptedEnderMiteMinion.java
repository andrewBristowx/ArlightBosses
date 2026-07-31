package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Ácaro del End corrompido. Solo, es prácticamente inofensivo (poca vida, poco daño) —
 * la idea es que aparezcan en grupo, dando sensación de enjambre antes de un jefe.
 * Para reforzar esa lectura mecánicamente: cada golpe cuenta cuántos otros ácaros
 * están cerca atacando al mismo objetivo y suma daño extra por cada uno (tope de 3),
 * así que un ácaro suelto casi no molesta pero un grupo se vuelve peligroso de verdad.
 */
public final class CorruptedEnderMiteMinion extends CorruptedMinionEntity {
    private static final double SWARM_CHECK_RADIUS = 4.0D;
    private static final float DAMAGE_PER_NEARBY_ALLY = 0.35F;
    private static final int MAX_COUNTED_ALLIES = 3;

    public CorruptedEnderMiteMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.corrupted_ender_mite"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.PORTAL; }
    @Override protected double defaultAttackReach() { return 1.2D; }

    /** Muy débil individualmente: se supone que aparecen en grupo, no solos. */
    public static AttributeSupplier.Builder createMiteAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    /** Cuenta aliados cercanos atacando al mismo objetivo y suma daño extra por cada uno. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(level() instanceof ServerLevel serverLevel) || !(target instanceof LivingEntity living)) {
            return super.doHurtTarget(target);
        }
        long nearbyAllies = level().getEntitiesOfClass(CorruptedEnderMiteMinion.class,
                        getBoundingBox().inflate(SWARM_CHECK_RADIUS),
                        other -> other != this && other.isAlive() && other.getTarget() == getTarget())
                .stream().limit(MAX_COUNTED_ALLIES).count();

        boolean hit = super.doHurtTarget(target);
        if (hit && nearbyAllies > 0) {
            living.hurt(damageSources().mobAttack(this), (float) nearbyAllies * DAMAGE_PER_NEARBY_ALLY);
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
}
