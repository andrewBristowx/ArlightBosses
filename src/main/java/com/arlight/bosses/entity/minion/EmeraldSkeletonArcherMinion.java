package com.arlight.bosses.entity.minion;

import com.arlight.bosses.entity.BossEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Esqueleto de la superficie con armadura de esmeralda agrietada. Mantiene distancia
 * y dispara flechas corrompidas (ver {@link EmeraldCorruptionArrow}) que dejan un
 * rastro de partículas de veneno/corrupción.
 */
public final class EmeraldSkeletonArcherMinion extends CorruptedMinionEntity implements RangedAttackMob {

    public EmeraldSkeletonArcherMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.emerald_skeleton_archer"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.SNEEZE; }
    @Override protected double defaultAttackReach() { return 14.0D; }

    /** Menos vida y daño de golpe que un esbirro cuerpo a cuerpo, compensado por el rango. */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createArcherAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 24.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 4.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.26D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new ArcherKiteGoal(this, 1.0D, 60, 7.0F, 13.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        Vec3 from = position().add(0.0D, getEyeHeight() * 0.85D, 0.0D);
        EmeraldCorruptionArrow arrow = new EmeraldCorruptionArrow(BossEntities.EMERALD_CORRUPTION_ARROW.get(), this, level());
        arrow.setPos(from.x, from.y, from.z);

        double dx = target.getX() - from.x;
        double dy = target.getY(0.5D) - from.y;
        double dz = target.getZ() - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        // Arco suave para compensar la caída de la flecha, igual que un esqueleto vanilla.
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, 1.6F, 6.0F);

        arrow.setBaseDamage(configuredAttackDamage() * 0.7D);
        playAttackAnimation();
        level().playSound(null, blockPosition(), SoundEvents.SKELETON_SHOOT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.95F + random.nextFloat() * 0.1F);
        serverLevel.addFreshEntity(arrow);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }

    /** Al morir, además del quejido óseo vainilla, se suma un estallido cristalino de corrupción. */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        boolean firstDeath = !dead;
        super.die(source);
        if (firstDeath && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 1.4F + random.nextFloat() * 0.1F);
        }
    }

    /**
     * Mantiene al arquero dentro de una banda de distancia respecto a su objetivo:
     * se aleja si lo tienen encima, se acerca si está muy lejos, y dispara cuando
     * hay línea de visión dentro del rango preferido.
     */
    private static final class ArcherKiteGoal extends Goal {
        private static final int DRAW_TELEGRAPH_LEAD_TICKS = 10;

        private final Mob mob;
        private final RangedAttackMob attacker;
        private final double speed;
        private final int attackIntervalTicks;
        private final float minRange;
        private final float maxRange;
        private int attackCooldown;
        private int seeTime;

        ArcherKiteGoal(Mob mob, double speed, int attackIntervalTicks, float minRange, float maxRange) {
            this.mob = mob;
            this.attacker = (RangedAttackMob) mob;
            this.speed = speed;
            this.attackIntervalTicks = attackIntervalTicks;
            this.minRange = minRange;
            this.maxRange = maxRange;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() != null && mob.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && (mob.getTarget() != null && mob.getNavigation().isDone() || isValidTarget());
        }

        private boolean isValidTarget() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            attackCooldown = 0;
            seeTime = 0;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            seeTime = 0;
            attackCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;

            double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean hasLineOfSight = mob.getSensing().hasLineOfSight(target);
            seeTime = hasLineOfSight ? seeTime + 1 : Math.max(0, seeTime - 1);

            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (distanceSqr < minRange * minRange) {
                fleeFrom(target);
            } else if (distanceSqr > maxRange * maxRange) {
                mob.getNavigation().moveTo(target, speed);
            } else if (mob.getNavigation().isDone()) {
                mob.getNavigation().stop();
            }

            if (attackCooldown > 0) attackCooldown--;
            if (attackCooldown == DRAW_TELEGRAPH_LEAD_TICKS && hasLineOfSight
                    && distanceSqr <= (double) maxRange * maxRange) {
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON,
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.5F, 1.7F + mob.getRandom().nextFloat() * 0.2F);
            }
            if (attackCooldown <= 0 && hasLineOfSight && seeTime >= 5 && distanceSqr <= (double) maxRange * maxRange) {
                float distanceFactor = (float) Math.sqrt(distanceSqr) / maxRange;
                attacker.performRangedAttack(target, distanceFactor);
                attackCooldown = attackIntervalTicks;
            }
        }

        private void fleeFrom(LivingEntity target) {
            double dx = mob.getX() - target.getX();
            double dz = mob.getZ() - target.getZ();
            mob.getNavigation().moveTo(mob.getX() + dx, mob.getY(), mob.getZ() + dz, speed);
        }
    }
}
