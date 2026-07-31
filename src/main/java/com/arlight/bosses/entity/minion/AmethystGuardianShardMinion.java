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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fragmento cristalino de amatista, flotante y casi estático (referencia visual a los
 * cristales del End). Mantiene distancia y dispara un rayo recto telegrafiado: marca la
 * línea de tiro un instante antes de disparar, y cuando dispara golpea a CUALQUIERA que
 * esté parado sobre esa línea, no solo al objetivo original — así que puede pegarle a
 * más de uno si están alineados, pero también se puede esquivar moviéndose del trazo.
 */
public final class AmethystGuardianShardMinion extends CorruptedMinionEntity {
    private static final int BEAM_COOLDOWN_TICKS = 80;
    private static final int BEAM_TELEGRAPH_TICKS = 14;
    private static final double BEAM_RANGE = 24.0D;
    private static final double BEAM_HIT_RADIUS = 0.6D;
    private static final double PREFERRED_DISTANCE = 9.0D;

    private int beamCooldown;
    private int beamTelegraphTicks;
    private Vec3 beamOrigin = Vec3.ZERO;
    private Vec3 beamDirection = Vec3.ZERO;

    public AmethystGuardianShardMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected String animationPrefix() { return "animation.amethyst_guardian_shard"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.REVERSE_PORTAL; }
    @Override protected double defaultAttackReach() { return 6.0D; }

    /** Poca vida (es un cristal, no un peleador), pero pega fuerte a distancia. */
    public static AttributeSupplier.Builder createShardAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 26.0D);
    }

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
        if (beamCooldown > 0) beamCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
            beamTelegraphTicks = 0;
            return;
        }

        if (beamTelegraphTicks > 0) {
            setDeltaMovement(getDeltaMovement().scale(0.5D));
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            tickTelegraph();
            return;
        }

        Vec3 toTarget = target.position().subtract(position());
        double distance = toTarget.length();
        if (distance > PREFERRED_DISTANCE + 2.0D) {
            setDeltaMovement(getDeltaMovement().scale(0.7D).add(toTarget.normalize().scale(0.05D)));
        } else if (distance < PREFERRED_DISTANCE - 2.0D) {
            setDeltaMovement(getDeltaMovement().scale(0.7D).add(toTarget.normalize().scale(-0.05D)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
        }
        getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (beamCooldown <= 0 && distance <= BEAM_RANGE && getSensing().hasLineOfSight(target)) {
            beamOrigin = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
            beamDirection = target.getEyePosition().subtract(beamOrigin).normalize();
            beamTelegraphTicks = BEAM_TELEGRAPH_TICKS;
            level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 0.7F, 0.7F);
        }
    }

    private void tickTelegraph() {
        beamTelegraphTicks--;
        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 1; i <= 20; i++) {
                Vec3 point = beamOrigin.add(beamDirection.scale(i * (BEAM_RANGE / 20.0D)));
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (beamTelegraphTicks == 0) fireBeam();
    }

    private void fireBeam() {
        beamCooldown = BEAM_COOLDOWN_TICKS;
        if (!(level() instanceof ServerLevel serverLevel)) return;

        playAttackAnimation();
        serverLevel.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 1.1F, 1.1F);
        for (int i = 0; i <= 40; i++) {
            Vec3 point = beamOrigin.add(beamDirection.scale(i * (BEAM_RANGE / 40.0D)));
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }

        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(BEAM_RANGE), entity -> entity != this)) {
            Vec3 toEntity = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D).subtract(beamOrigin);
            double along = toEntity.dot(beamDirection);
            if (along <= 0.0D || along > BEAM_RANGE) continue;
            Vec3 closestPoint = beamOrigin.add(beamDirection.scale(along));
            double perpendicular = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D).distanceTo(closestPoint);
            if (perpendicular <= BEAM_HIT_RADIUS + living.getBbWidth() * 0.5D) {
                living.hurt(damageSources().mobAttack(this), configuredAttackDamage());
            }
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.AMETHYST_BLOCK_CHIME; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.AMETHYST_CLUSTER_BREAK; }
}
