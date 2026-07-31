package com.arlight.bosses.entity.minion;

import com.arlight.bosses.entity.BossEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Piglin corrompido por vetas de oro y cuarzo del Nether. Pelea cuerpo a cuerpo como
 * antes, pero ahora también arroja esquirlas de oro corrompido a media distancia
 * (igual que un piglin vainilla alterna espada/ballesta), se envalentona con un
 * arrebato de velocidad al conectar un golpe, y celebra en voz alta cuando remata
 * a alguien. El montaje automático se omite por estabilidad en Arclight.
 */
public final class GildedPiglinMinion extends CorruptedMinionEntity implements RangedAttackMob {
    private static final int SHARD_COOLDOWN_TICKS = 55;
    private static final double SHARD_MIN_RANGE = 5.0D;
    private static final double SHARD_MAX_RANGE = 14.0D;
    private static final float FRENZY_CHANCE_ON_HIT = 0.25F;
    private static final int FRENZY_DURATION_TICKS = 60;

    public GildedPiglinMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.gilded_piglin"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.END_ROD; }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new GoldShardGoal(this, SHARD_MIN_RANGE, SHARD_MAX_RANGE, SHARD_COOLDOWN_TICKS));
    }

    /**
     * El montaje dinámico se mantiene desactivado en servidores híbridos Arclight.
     * startRiding() puede atravesar el puente Bukkit/NeoForge y disparar eventos de
     * montura recursivos o bloqueos al cargar chunks del Nether. El Hoglin Rider
     * continúa siendo una criatura fusionada en un solo modelo, sin pasajeros reales.
     */

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        Vec3 from = position().add(0.0D, getEyeHeight() * 0.8D, 0.0D);
        GildedShardProjectile shard = new GildedShardProjectile(BossEntities.GILDED_SHARD_PROJECTILE.get(), this, level());
        shard.setPos(from.x, from.y, from.z);

        double dx = target.getX() - from.x;
        double dy = target.getY(0.5D) - from.y;
        double dz = target.getZ() - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        shard.shoot(dx, dy + horizontalDistance * 0.18D, dz, 1.5F, 8.0F);
        shard.setBaseDamage(configuredAttackDamage() * 0.55D);

        playAttackAnimation();
        level().playSound(null, blockPosition(), SoundEvents.PIGLIN_AMBIENT,
                SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.15F);
        serverLevel.addFreshEntity(shard);
    }

    /** Golpe cuerpo a cuerpo: puede desatar un arrebato de velocidad, y celebra si remata al objetivo. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel serverLevel) {
            if (random.nextFloat() < FRENZY_CHANCE_ON_HIT) {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FRENZY_DURATION_TICKS, 0));
                serverLevel.playSound(null, blockPosition(), SoundEvents.PIGLIN_ANGRY,
                        SoundSource.HOSTILE, 1.0F, 1.05F);
            }
            if (target instanceof LivingEntity living && !living.isAlive()) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.PIGLIN_CELEBRATE,
                        SoundSource.HOSTILE, 1.0F, 0.95F + random.nextFloat() * 0.1F);
            }
        }
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PIGLIN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PIGLIN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PIGLIN_DEATH; }

    /**
     * Mientras el objetivo esté en la banda de media distancia y haya línea de visión,
     * tira una esquirla dorada cada tanto. No reemplaza el acercamiento/combate cuerpo
     * a cuerpo: solo se activa en esa banda, dejando que el resto de los goals manejen
     * el resto (a diferencia del arquero, este esbirro prefiere terminar cuerpo a cuerpo).
     */
    private static final class GoldShardGoal extends Goal {
        private final Mob mob;
        private final RangedAttackMob attacker;
        private final double minRange;
        private final double maxRange;
        private final int cooldownTicks;
        private int cooldown;

        GoldShardGoal(Mob mob, double minRange, double maxRange, int cooldownTicks) {
            this.mob = mob;
            this.attacker = (RangedAttackMob) mob;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.cooldownTicks = cooldownTicks;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) cooldown--;
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive() || cooldown > 0) return false;
            double distanceSqr = mob.distanceToSqr(target);
            return distanceSqr >= minRange * minRange && distanceSqr <= maxRange * maxRange
                    && mob.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            double distanceSqr = mob.distanceToSqr(target);
            float distanceFactor = (float) Math.sqrt(distanceSqr) / (float) maxRange;
            attacker.performRangedAttack(target, distanceFactor);
            cooldown = cooldownTicks;
        }
    }
}
