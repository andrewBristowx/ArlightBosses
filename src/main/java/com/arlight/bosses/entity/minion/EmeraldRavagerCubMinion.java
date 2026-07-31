package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Versión mini del ravager, invadida por esmeralda. Embiste con una carga corta igual
 * que su primo grande, y — a diferencia de cualquier otro esbirro del mod — puede
 * llevar a un {@link GildedPiglinMinion} montado encima (ver la lógica de montura en
 * esa clase): mientras el piglin tira esquirlas desde arriba, la cría se encarga de
 * cerrar distancia embistiendo.
 */
public final class EmeraldRavagerCubMinion extends CorruptedMinionEntity {
    private static final int CHARGE_COOLDOWN_TICKS = 80;
    private static final int CHARGE_DURATION_TICKS = 12;
    private static final double CHARGE_MIN_RANGE = 4.0D;
    private static final double CHARGE_MAX_RANGE = 9.0D;

    private int chargeCooldown;

    public EmeraldRavagerCubMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.emerald_ravager_cub"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.HAPPY_VILLAGER; }
    @Override protected double defaultAttackReach() { return 2.4D; }

    /** Más chica que un hoglin jinete, pero con buena resistencia a empujones para poder cargar con un piglin encima. */
    public static AttributeSupplier.Builder createCubAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new CubChargeGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (chargeCooldown > 0) chargeCooldown--;
        // Un poco más rápida mientras lleva un jinete: no reemplaza al piglin, lo acerca.
        if (!getPassengers().isEmpty()) {
            setDeltaMovement(getDeltaMovement().multiply(1.06D, 1.0D, 1.06D));
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }

    /** Carga corta hacia el objetivo cuando está a media distancia; al conectar, empuja fuerte. */
    private static final class CubChargeGoal extends Goal {
        private final EmeraldRavagerCubMinion mob;
        private double dirX;
        private double dirZ;
        private int ticksLeft;
        private boolean impactApplied;

        CubChargeGoal(EmeraldRavagerCubMinion mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || mob.chargeCooldown > 0) return false;
            double distanceSqr = mob.distanceToSqr(target);
            return distanceSqr >= CHARGE_MIN_RANGE * CHARGE_MIN_RANGE
                    && distanceSqr <= CHARGE_MAX_RANGE * CHARGE_MAX_RANGE
                    && mob.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return ticksLeft > 0 && !impactApplied;
        }

        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            double dx = target.getX() - mob.getX();
            double dz = target.getZ() - mob.getZ();
            double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            dirX = dx / length;
            dirZ = dz / length;
            ticksLeft = CHARGE_DURATION_TICKS;
            impactApplied = false;
            mob.playAttackAnimation();
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR,
                    SoundSource.HOSTILE, 1.0F, 1.3F);
        }

        @Override
        public void stop() {
            mob.chargeCooldown = CHARGE_COOLDOWN_TICKS;
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.3D));
        }

        @Override
        public void tick() {
            ticksLeft--;
            mob.setDeltaMovement(dirX * 0.5D, mob.getDeltaMovement().y, dirZ * 0.5D);
            mob.getLookControl().setLookAt(mob.getX() + dirX, mob.getY(), mob.getZ() + dirZ);

            LivingEntity target = mob.getTarget();
            if (target == null || impactApplied) return;
            double reach = mob.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + 0.5D;
            if (mob.distanceToSqr(target) <= reach * reach) {
                impactApplied = true;
                target.hurt(mob.damageSources().mobAttack(mob), mob.configuredAttackDamage() * 1.2F);
                double dx = target.getX() - mob.getX();
                double dz = target.getZ() - mob.getZ();
                double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                target.knockback(1.1D, -dx / length, -dz / length);
            }
        }
    }
}
