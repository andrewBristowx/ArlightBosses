package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Zombi de la superficie invadido por cristales de esmeralda. Al recibir daño puede
 * "llamar" a otros zombis de esmeralda cercanos sin objetivo (igual que un zombi
 * vainilla pidiendo refuerzos), y si queda muy herido entra en sobrecarga de
 * cristales: un único arrebato de velocidad y fuerza por vida, como si la corrupción
 * se desestabilizara para darle una última embestida.
 */
public final class EmeraldZombieMinion extends CorruptedMinionEntity {
    private static final double CALL_RADIUS = 12.0D;
    private static final int CALL_COOLDOWN_TICKS = 100;
    private static final int OVERLOAD_DURATION_TICKS = 160;

    private int reinforcementCooldown;
    private boolean overloadTriggered;

    public EmeraldZombieMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.emerald_zombie"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.HAPPY_VILLAGER; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (reinforcementCooldown > 0) reinforcementCooldown--;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt || !isAlive() || !(level() instanceof ServerLevel serverLevel)) return hurt;

        if (source.getEntity() instanceof LivingEntity attacker && reinforcementCooldown <= 0) {
            callReinforcements(serverLevel, attacker);
        }
        if (!overloadTriggered && getHealth() <= getMaxHealth() * 0.3F) {
            triggerCrystalOverload(serverLevel);
        }
        return hurt;
    }

    /** Grita para que otros zombis de esmeralda sin objetivo cercanos vengan a ayudar. */
    private void callReinforcements(ServerLevel serverLevel, LivingEntity attacker) {
        reinforcementCooldown = CALL_COOLDOWN_TICKS;
        serverLevel.playSound(null, blockPosition(), SoundEvents.ZOMBIE_AMBIENT,
                SoundSource.HOSTILE, 1.4F, 0.7F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.7D, getZ(),
                14, 0.4D, 0.5D, 0.4D, 0.02D);

        List<EmeraldZombieMinion> nearby = serverLevel.getEntitiesOfClass(EmeraldZombieMinion.class,
                getBoundingBox().inflate(CALL_RADIUS),
                other -> other != this && other.isAlive() && other.getTarget() == null);
        for (EmeraldZombieMinion ally : nearby) {
            ally.setTarget(attacker);
            ally.reinforcementCooldown = CALL_COOLDOWN_TICKS;
        }
    }

    /** Una única vez por vida: al quedar por debajo del 30% de vida, la corrupción se desestabiliza y lo potencia. */
    private void triggerCrystalOverload(ServerLevel serverLevel) {
        overloadTriggered = true;
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, OVERLOAD_DURATION_TICKS, 1));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, OVERLOAD_DURATION_TICKS, 0));
        serverLevel.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, 1.0F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                20, 0.3D, 0.5D, 0.3D, 0.05D);
    }
}
