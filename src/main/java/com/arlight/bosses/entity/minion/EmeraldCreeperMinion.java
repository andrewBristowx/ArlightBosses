package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Creeper invadido por vetas de esmeralda. A diferencia de antes (que explotaba
 * apenas conectaba un golpe cuerpo a cuerpo), ahora tiene una mecha real: se acerca,
 * empieza a silbar y pulsar cuando el objetivo entra en rango, y si el objetivo se
 * aleja a tiempo la mecha se apaga en vez de explotar sí o sí — igual que el creeper
 * vainilla, con la misma tensión de "¿me alejo o no llego a tiempo?".
 */
public final class EmeraldCreeperMinion extends CorruptedMinionEntity {
    private static final double FUSE_RANGE = 3.0D;
    private static final int MAX_FUSE_TICKS = 30;
    private static final double EXPLOSION_RADIUS = 3.0D;

    private int fuseTicks;

    public EmeraldCreeperMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.emerald_creeper"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.HAPPY_VILLAGER; }
    @Override protected double defaultAttackReach() { return 2.0D; }

    /** El contacto cuerpo a cuerpo ya no hace nada: el daño solo llega por la explosión de la mecha. */
    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        LivingEntity target = getTarget();

        if (target == null || !target.isAlive()) {
            if (fuseTicks > 0) defuse();
            return;
        }

        boolean inFuseRange = distanceToSqr(target) <= FUSE_RANGE * FUSE_RANGE;
        if (inFuseRange) {
            if (fuseTicks == 0 && level() instanceof ServerLevel serverLevel) {
                playAttackAnimation();
                serverLevel.playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED,
                        SoundSource.HOSTILE, 1.0F, 0.7F);
            }
            fuseTicks++;
            if (fuseTicks % 6 == 0 && level() instanceof ServerLevel serverLevel) {
                if (fuseTicks % 12 == 0) playAttackAnimation();
                float progress = fuseTicks / (float) MAX_FUSE_TICKS;
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        3, 0.25D, 0.35D, 0.25D, 0.01D);
                serverLevel.playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED,
                        SoundSource.HOSTILE, 0.6F, 0.7F + progress * 0.7F);
            }
            if (fuseTicks >= MAX_FUSE_TICKS) {
                explode();
            }
        } else if (fuseTicks > 0) {
            defuse();
        }
    }

    private void defuse() {
        fuseTicks = 0;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.HOSTILE, 0.5F, 1.4F);
        }
    }

    private void explode() {
        fuseTicks = 0;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.8D, getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.25F, 0.9F);
            for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(EXPLOSION_RADIUS), entity -> entity != this)) {
                double distance = Math.max(0.5D, distanceTo(living));
                float damage = (float) (configuredAttackDamage() * Math.max(0.35D, 1.35D - distance / EXPLOSION_RADIUS));
                living.hurt(damageSources().mobAttack(this), damage);
            }
        }
        discard();
    }

    /** Los creepers no tienen sonido ambiental: solo silban al prender la mecha (arriba) y gruñen al recibir daño o morir. */
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CREEPER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CREEPER_DEATH; }
}
