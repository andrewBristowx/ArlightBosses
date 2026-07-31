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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fantasma corrompido por amatista.
 */
public final class AmethystPhantomMinion extends CorruptedMinionEntity {
    private static final int ORBIT_CHANGE_TICKS = 24;
    private static final int DIVE_COOLDOWN_TICKS = 90;
    private static final int DIVE_TIMEOUT_TICKS = 40;
    private static final double DIVE_TRIGGER_RANGE = 14.0D;
    private static final double MELEE_REACH = 2.3D;

    private Vec3 orbitTarget = Vec3.ZERO;
    private int orbitTicks;
    private int diveCooldown;
    private int diveTicks;
    private boolean diving;
    private boolean hitThisDive;

    public AmethystPhantomMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected String animationPrefix() { return "animation.amethyst_phantom"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.REVERSE_PORTAL; }
    @Override protected double defaultAttackReach() { return MELEE_REACH; }

    public static AttributeSupplier.Builder createPhantomAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 18.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setNoGravity(true);
        if (diveCooldown > 0) diveCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            wanderErratically();
            return;
        }

        if (diving) {
            tickDive(target);
            return;
        }

        orbitAround(target);

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (diveCooldown <= 0 && horizontalDistance <= DIVE_TRIGGER_RANGE && getSensing().hasLineOfSight(target)) {
            startDive();
        }
    }

    private void orbitAround(LivingEntity target) {
        if (orbitTicks <= 0 || orbitTarget.equals(Vec3.ZERO)) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 4.0D + random.nextDouble() * 6.0D;
            double height = 2.0D + random.nextDouble() * 5.0D;
            orbitTarget = target.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            orbitTicks = ORBIT_CHANGE_TICKS + random.nextInt(16);
        }
        orbitTicks--;

        Vec3 toOrbit = orbitTarget.subtract(position());
        if (toOrbit.length() > 1.0D) {
            setDeltaMovement(getDeltaMovement().scale(0.7D).add(toOrbit.normalize().scale(0.09D)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
        }
        getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void wanderErratically() {
        if (orbitTicks <= 0) {
            orbitTarget = position().add((random.nextDouble() - 0.5D) * 10.0D,
                    (random.nextDouble() - 0.5D) * 4.0D, (random.nextDouble() - 0.5D) * 10.0D);
            orbitTicks = ORBIT_CHANGE_TICKS * 2;
        }
        orbitTicks--;
        Vec3 toOrbit = orbitTarget.subtract(position());
        if (toOrbit.length() > 1.0D) {
            setDeltaMovement(getDeltaMovement().scale(0.75D).add(toOrbit.normalize().scale(0.04D)));
        }
    }

    private void startDive() {
        diving = true;
        diveTicks = DIVE_TIMEOUT_TICKS;
        hitThisDive = false;
        diveCooldown = DIVE_COOLDOWN_TICKS;
        playAttackAnimation();
        level().playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP,
                SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    private void tickDive(LivingEntity target) {
        diveTicks--;
        Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(position());
        double distance = toTarget.length();
        if (distance > 0.1D) {
            setDeltaMovement(toTarget.normalize().scale(0.42D));
        }
        getLookControl().setLookAt(target, 40.0F, 40.0F);

        if (!hitThisDive && distance <= MELEE_REACH) {
            hitThisDive = true;
            target.hurt(damageSources().mobAttack(this), configuredAttackDamage());
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            target.knockback(0.6D, -dx / length, -dz / length);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_BITE,
                        SoundSource.HOSTILE, 1.0F, 1.1F);
            }
        }

        if (diveTicks <= 0 || distance < 0.6D && hitThisDive) {
            diving = false;
            orbitTicks = 0;
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PHANTOM_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PHANTOM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PHANTOM_DEATH; }
}
