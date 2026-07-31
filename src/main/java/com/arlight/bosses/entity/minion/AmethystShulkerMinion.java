package com.arlight.bosses.entity.minion;

import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shulker corrompido por amatista. En vez de un golpe instantáneo garantizado, ahora
 * abre la tapa (misma animación de siempre) y dispara una {@link ShulkerBullet} real
 * — la bala teledirigida vainilla, que curva en pleno vuelo y se puede esquivar u
 * ocultar detrás de un bloque — en vez de un rayo invisible que conectaba sí o sí.
 * También se teletransporta para reubicarse cuando lo golpean, como el shulker real.
 */
public final class AmethystShulkerMinion extends CorruptedMinionEntity {
    private static final int ATTACK_COOLDOWN_TICKS = 70;
    private static final double ATTACK_RANGE = 20.0D;
    private static final float RELOCATE_CHANCE = 0.4F;
    private static final int RELOCATE_COOLDOWN_TICKS = 60;

    private int attackCooldown;
    private int relocateCooldown;

    public AmethystShulkerMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected String animationPrefix() { return "animation.amethyst_shulker"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.REVERSE_PORTAL; }
    @Override protected double defaultAttackReach() { return 12.0D; }

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
        setDeltaMovement(Vec3.ZERO);
        if (relocateCooldown > 0) relocateCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        if (distanceToSqr(target) > ATTACK_RANGE * ATTACK_RANGE || !getSensing().hasLineOfSight(target)) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        attackCooldown = ATTACK_COOLDOWN_TICKS;
        playAttackAnimation();
        level().playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT,
                SoundSource.HOSTILE, 1.0F, 1.0F);

        double dx = Math.abs(target.getX() - getX());
        double dz = Math.abs(target.getZ() - getZ());
        Direction.Axis axis = dx > dz ? Direction.Axis.X : Direction.Axis.Z;
        ShulkerBullet bullet = new ShulkerBullet(level(), this, target, axis);
        serverLevel.addFreshEntity(bullet);
    }

    /** Al recibir daño, puede teletransportarse a un punto cercano para reubicarse, igual que un shulker real. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && relocateCooldown <= 0 && random.nextFloat() < RELOCATE_CHANCE) {
            relocateCooldown = RELOCATE_COOLDOWN_TICKS;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 3.0D + random.nextDouble() * 5.0D;
            double verticalOffset = random.nextInt(5) - 2;
            boolean teleported = randomTeleport(getX() + Math.cos(angle) * radius,
                    getY() + verticalOffset, getZ() + Math.sin(angle) * radius, true);
            if (teleported && level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.SHULKER_TELEPORT,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
        return hurt;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SHULKER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SHULKER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SHULKER_DEATH; }
}
