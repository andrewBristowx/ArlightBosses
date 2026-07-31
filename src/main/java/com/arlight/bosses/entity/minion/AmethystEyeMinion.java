package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ojo de amatista flotante. Ataca igual que un Guardián vainilla: canaliza un rayo
 * durante un rato (con la iris pulsando cada vez más rápido) y si mantiene línea de
 * visión todo ese tiempo, dispara un golpe fuerte y garantizado. Si el objetivo rompe
 * la línea de visión o escapa de rango durante la canalización, el ataque se cancela
 * y tiene que empezar de nuevo — a diferencia de antes, que pegaba sin poder evitarlo.
 */
public final class AmethystEyeMinion extends CorruptedMinionEntity {
    private static final int CHANNEL_DURATION_TICKS = 50;
    private static final int POST_FIRE_COOLDOWN_TICKS = 30;
    private static final int PULSE_INTERVAL_TICKS = 8;
    private static final double ATTACK_RANGE = 16.0D;

    private int channelTicks;
    private int cooldownTicks;

    public AmethystEyeMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected String animationPrefix() { return "animation.amethyst_eye"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.WITCH; }
    @Override protected double defaultAttackReach() { return 10.0D; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);
        LivingEntity target = getTarget();

        if (target == null || !target.isAlive()) {
            cancelChannel(false);
            setDeltaMovement(getDeltaMovement().scale(0.72D).add(0.0D,
                    Math.sin((tickCount + getId()) * 0.12D) * 0.008D, 0.0D));
            return;
        }

        Vec3 aim = target.getEyePosition().subtract(position());
        double distance = aim.length();
        boolean hovering = channelTicks == 0;
        if (hovering && distance > 3.5D) {
            setDeltaMovement(getDeltaMovement().scale(0.68D).add(aim.normalize().scale(0.055D)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(channelTicks > 0 ? 0.75D : 0.55D));
        }
        getLookControl().setLookAt(target, 25.0F, 25.0F);

        if (cooldownTicks > 0) cooldownTicks--;

        boolean inRange = distance <= ATTACK_RANGE;
        boolean hasLineOfSight = getSensing().hasLineOfSight(target);

        if (channelTicks > 0) {
            if (!inRange || !hasLineOfSight || !target.isAlive()) {
                cancelChannel(true);
                return;
            }
            channelTicks++;
            if (channelTicks % PULSE_INTERVAL_TICKS == 0) {
                playAttackAnimation();
                float progress = channelTicks / (float) CHANNEL_DURATION_TICKS;
                level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.HOSTILE, 0.5F, 0.6F + progress * 0.9F);
            }
            if (channelTicks >= CHANNEL_DURATION_TICKS) {
                fireBeam(target);
            }
            return;
        }

        if (cooldownTicks <= 0 && inRange && hasLineOfSight) {
            channelTicks = 1;
            playAttackAnimation();
            level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 0.5F, 0.6F);
        }
    }

    private void fireBeam(LivingEntity target) {
        if (level() instanceof ServerLevel serverLevel) {
            target.hurt(damageSources().mobAttack(this), configuredAttackDamage() * 1.3F);
            serverLevel.playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK,
                    SoundSource.HOSTILE, 1.0F, 1.2F);
            Vec3 direction = target.getEyePosition().subtract(getEyePosition());
            int points = 16;
            for (int i = 0; i <= points; i++) {
                Vec3 point = getEyePosition().add(direction.scale(i / (double) points));
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                        2, 0.04D, 0.04D, 0.04D, 0.0D);
            }
        }
        channelTicks = 0;
        cooldownTicks = POST_FIRE_COOLDOWN_TICKS;
    }

    private void cancelChannel(boolean playFizzleSound) {
        if (channelTicks <= 0) return;
        channelTicks = 0;
        cooldownTicks = Math.min(cooldownTicks, 12);
        if (playFizzleSound && level() instanceof ServerLevel) {
            level().playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.HOSTILE, 0.5F, 1.6F);
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.AMETHYST_BLOCK_CHIME; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_BREAK; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.AMETHYST_CLUSTER_BREAK; }
}
