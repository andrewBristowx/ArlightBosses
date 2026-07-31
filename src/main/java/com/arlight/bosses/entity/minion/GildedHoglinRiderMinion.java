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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Hoglin de oro y cuarzo con un Wither Skeleton corrupto integrado en el modelo.
 * Su golpe cuerpo a cuerpo aplica un toque de Wither (el jinete clava su espada
 * corrupta), y cada tanto embiste a media distancia igual que un hoglin real,
 * cerrando terreno de golpe y aplicando un empujón fuerte al conectar.
 */
public final class GildedHoglinRiderMinion extends CorruptedMinionEntity {
    private static final int CHARGE_COOLDOWN_TICKS = 100;
    private static final int CHARGE_DURATION_TICKS = 16;
    private static final double CHARGE_MIN_RANGE = 5.0D;
    private static final double CHARGE_MAX_RANGE = 11.0D;
    private static final int WITHER_DURATION_TICKS = 100;

    private int chargeCooldown;

    public GildedHoglinRiderMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.gilded_hoglin_rider"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.END_ROD; }
    @Override protected double defaultAttackReach() { return 3.2D; }

    /** Más resistente y con knockback que un esbirro cuerpo a cuerpo normal: es dos amenazas en una montura. */
    public static AttributeSupplier.Builder createHoglinRiderAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 42.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new TuskChargeGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (chargeCooldown > 0) chargeCooldown--;
    }

    /** El jinete corrupto clava su espada de Wither en cada golpe cuerpo a cuerpo conectado. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && level() instanceof ServerLevel) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, 0));
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.HOGLIN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.HOGLIN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.HOGLIN_DEATH; }

    /** Al morir suenan las dos criaturas fusionadas: el bramido del hoglin y el traqueteo óseo del jinete. */
    @Override
    public void die(DamageSource source) {
        boolean firstDeath = !dead;
        super.die(source);
        if (firstDeath && level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.WITHER_SKELETON_DEATH,
                    SoundSource.HOSTILE, 0.7F, 1.3F);
        }
    }

    /**
     * Embestida a media distancia: cuando el objetivo está lejos para el cuerpo a cuerpo pero
     * cerca para un rush, el hoglin se lanza en línea recta durante {@link #CHARGE_DURATION_TICKS}
     * ticks y, si conecta, aplica daño extra y un empujón fuerte en vez del golpe normal.
     */
    private static final class TuskChargeGoal extends Goal {
        private final GildedHoglinRiderMinion mob;
        private double dirX;
        private double dirZ;
        private int ticksLeft;
        private boolean impactApplied;

        TuskChargeGoal(GildedHoglinRiderMinion mob) {
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
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.HOGLIN_ANGRY,
                    SoundSource.HOSTILE, 1.0F, 0.85F);
        }

        @Override
        public void stop() {
            mob.chargeCooldown = CHARGE_COOLDOWN_TICKS;
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.3D));
        }

        @Override
        public void tick() {
            ticksLeft--;
            mob.setDeltaMovement(dirX * 0.62D, mob.getDeltaMovement().y, dirZ * 0.62D);
            mob.getLookControl().setLookAt(mob.getX() + dirX, mob.getY(), mob.getZ() + dirZ);

            LivingEntity target = mob.getTarget();
            if (target == null || impactApplied) return;
            double reach = mob.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + 0.6D;
            if (mob.distanceToSqr(target) <= reach * reach) {
                impactApplied = true;
                target.hurt(mob.damageSources().mobAttack(mob), mob.configuredAttackDamage() * 1.4F);
                double dx = target.getX() - mob.getX();
                double dz = target.getZ() - mob.getZ();
                double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                target.knockback(1.35D, -dx / length, -dz / length);
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                        SoundSource.HOSTILE, 1.0F, 0.8F);
            }
        }
    }
}
