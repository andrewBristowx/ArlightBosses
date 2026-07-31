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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Enderman de élite, más grande y resistente que el {@link VoidEndermanMinion} común.
 * Nota de nombre: la idea original lo llamaba "Warden", pero para no pisar el nombre
 * del Warden vainilla de Minecraft lo dejé como "Sentinel". Reusa el mismo arte que el
 * enderman base (mismo modelo/textura/animación — solo cambia el hitbox y las
 * estadísticas), pero suma un grito propio que ciega en área a quien esté cerca y
 * tenga línea de visión, en vez de teletransportarse para emboscar.
 */
public final class VoidEndermanSentinelMinion extends CorruptedMinionEntity {
    private static final int SCREAM_COOLDOWN_TICKS = 150;
    private static final int SCREAM_TELEGRAPH_TICKS = 12;
    private static final double SCREAM_TRIGGER_RANGE = 8.0D;
    private static final double SCREAM_RADIUS = 6.0D;
    private static final int BLINDNESS_DURATION_TICKS = 60;

    private int screamCooldown;
    private int screamTelegraphTicks;

    public VoidEndermanSentinelMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.void_enderman_sentinel"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.PORTAL; }
    @Override protected double defaultAttackReach() { return 2.5D; }

    /** Bastante más resistente que un enderman común: es una versión de élite. */
    public static AttributeSupplier.Builder createSentinelAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 55.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (screamCooldown > 0) screamCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            screamTelegraphTicks = 0;
            return;
        }

        if (screamTelegraphTicks > 0) {
            screamTelegraphTicks--;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (screamTelegraphTicks == 0) performVoidScream();
            return;
        }

        if (screamCooldown <= 0 && distanceToSqr(target) <= SCREAM_TRIGGER_RANGE * SCREAM_TRIGGER_RANGE
                && getSensing().hasLineOfSight(target)) {
            screamTelegraphTicks = SCREAM_TELEGRAPH_TICKS;
            level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_STARE,
                    SoundSource.HOSTILE, 1.2F, 0.6F);
        }
    }

    private void performVoidScream() {
        screamCooldown = SCREAM_COOLDOWN_TICKS;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_SCREAM,
                SoundSource.HOSTILE, 1.3F, 0.7F);
        serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                40, SCREAM_RADIUS * 0.4D, 1.0D, SCREAM_RADIUS * 0.4D, 0.1D);

        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(SCREAM_RADIUS), entity -> entity != this)) {
            if (!getSensing().hasLineOfSight(living)) continue;
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0));
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDERMAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDERMAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDERMAN_DEATH; }
}
