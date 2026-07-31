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

import java.util.HashSet;
import java.util.Set;

/**
 * Blaze corrompido por oro (gilded), flotante.
 */
public final class GildedBlazeWraithMinion extends CorruptedMinionEntity {
    private static final int ATTACK_INTERVAL_TICKS = 70;
    private static final int RING_DURATION_TICKS = 24;
    private static final int TELEGRAPH_LEAD_TICKS = 12;
    private static final double RING_MAX_RADIUS = 4.5D;
    private static final double RING_BAND_HALF_WIDTH = 0.55D;
    private static final double ATTACK_TRIGGER_RANGE = 16.0D;

    private final Set<Integer> hitThisRing = new HashSet<>();
    private int attackCooldown;
    private int ringTicks;
    private double ringCenterX;
    private double ringCenterY;
    private double ringCenterZ;

    public GildedBlazeWraithMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 14;
    }

    @Override protected String animationPrefix() { return "animation.gilded_blaze_wraith"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.FLAME; }
    @Override protected double defaultAttackReach() { return 2.2D; }

    public static AttributeSupplier.Builder createWraithAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 26.0D);
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

        if (ringTicks > 0) {
            tickFireRing();
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.75D).add(0.0D,
                    Math.sin((tickCount + getId()) * 0.05D) * 0.01D, 0.0D));
            return;
        }

        Vec3 hoverSpot = target.position().add(0.0D, 3.0D + Math.sin((tickCount + getId()) * 0.05D) * 0.6D, 0.0D);
        Vec3 toHoverSpot = hoverSpot.subtract(position());
        double distanceToHover = toHoverSpot.length();
        if (distanceToHover > 2.0D) {
            setDeltaMovement(getDeltaMovement().scale(0.72D).add(toHoverSpot.normalize().scale(0.06D)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.6D));
        }
        getLookControl().setLookAt(target, 30.0F, 30.0F);

        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        boolean inTriggerRange = horizontalDistance <= ATTACK_TRIGGER_RANGE;

        if (attackCooldown == TELEGRAPH_LEAD_TICKS && inTriggerRange && level() instanceof ServerLevel) {
            level().playSound(null, blockPosition(), SoundEvents.BLAZE_BURN,
                    SoundSource.HOSTILE, 0.6F, 0.6F + random.nextFloat() * 0.1F);
        }
        if (attackCooldown-- <= 0 && inTriggerRange
                && level() instanceof ServerLevel) {
            startFireRing(target);
            attackCooldown = ATTACK_INTERVAL_TICKS;
        }
    }

    private void startFireRing(LivingEntity target) {
        ringTicks = RING_DURATION_TICKS;
        ringCenterX = target.getX();
        ringCenterY = target.getY();
        ringCenterZ = target.getZ();
        hitThisRing.clear();
        playAttackAnimation();
        level().playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 1.1F, 0.75F);
    }

    private void tickFireRing() {
        ringTicks--;
        double progress = 1.0D - (ringTicks / (double) RING_DURATION_TICKS);
        double radius = progress * RING_MAX_RADIUS;

        if (level() instanceof ServerLevel serverLevel) {
            int points = 20;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D) * i / points;
                double px = ringCenterX + Math.cos(angle) * radius;
                double pz = ringCenterZ + Math.sin(angle) * radius;
                serverLevel.sendParticles(ParticleTypes.FLAME, px, ringCenterY + 0.1D, pz,
                        1, 0.0D, 0.02D, 0.0D, 0.001D);
            }

            if (ringTicks % 4 == 0) {
                double crackleAngle = progress * Math.PI * 2.0D;
                double cx = ringCenterX + Math.cos(crackleAngle) * radius;
                double cz = ringCenterZ + Math.sin(crackleAngle) * radius;
                serverLevel.playSound(null, cx, ringCenterY, cz, SoundEvents.FIRE_AMBIENT,
                        SoundSource.HOSTILE, 0.4F, 1.0F + random.nextFloat() * 0.3F);
            }

            for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(RING_MAX_RADIUS + 1.0D), entity -> entity != this)) {
                if (hitThisRing.contains(living.getId())) continue;
                double dx = living.getX() - ringCenterX;
                double dz = living.getZ() - ringCenterZ;
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                boolean inBand = Math.abs(horizontalDistance - radius) <= RING_BAND_HALF_WIDTH;
                boolean nearHeight = Math.abs(living.getY() - ringCenterY) <= 2.2D;
                if (inBand && nearHeight) {
                    hitThisRing.add(living.getId());
                    living.hurt(damageSources().mobAttack(this), configuredAttackDamage());
                    living.setRemainingFireTicks(living.getRemainingFireTicks() + 80);
                    serverLevel.playSound(null, living.blockPosition(), SoundEvents.GENERIC_BURN,
                            SoundSource.HOSTILE, 0.8F, 1.0F + random.nextFloat() * 0.2F);
                }
            }
        }

        if (ringTicks <= 0) hitThisRing.clear();
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.HOSTILE, 0.8F, 1.1F + random.nextFloat() * 0.1F);
        }
    }
}
