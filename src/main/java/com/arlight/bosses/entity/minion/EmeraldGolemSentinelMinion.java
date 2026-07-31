package com.arlight.bosses.entity.minion;

import com.arlight.bosses.entity.BossEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
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
 * Mini-golem de esmeralda completamente estático: no camina ni persigue, solo gira
 * para encarar al objetivo y dispara flechas corrompidas a distancia. Pensado como
 * "objeto de mazmorra" — algo que hay que destruir a distancia o cuerpo a cuerpo antes
 * de poder avanzar, no algo que te persiga por el mapa. Tiene bastante vida para
 * compensar que no se mueve ni esquiva.
 */
public final class EmeraldGolemSentinelMinion extends CorruptedMinionEntity implements RangedAttackMob {
    private static final int ATTACK_COOLDOWN_TICKS = 50;
    private static final double ATTACK_RANGE = 18.0D;

    private int attackCooldown;

    public EmeraldGolemSentinelMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.emerald_golem_sentinel"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.HAPPY_VILLAGER; }
    @Override protected double defaultAttackReach() { return 3.0D; }

    /** Bastante vida: es un objetivo fijo, tiene que aguantar mientras no puede esquivar ni huir. */
    public static AttributeSupplier.Builder createSentinelAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 46.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** Sin goals de movimiento: no persigue ni huye, solo mira y dispara. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 20.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setDeltaMovement(Vec3.ZERO);
        if (attackCooldown > 0) attackCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || attackCooldown > 0) return;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= ATTACK_RANGE && getSensing().hasLineOfSight(target)) {
            performRangedAttack(target, (float) (horizontalDistance / ATTACK_RANGE));
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        attackCooldown = ATTACK_COOLDOWN_TICKS;

        Vec3 from = position().add(0.0D, getEyeHeight() * 0.85D, 0.0D);
        EmeraldCorruptionArrow arrow = new EmeraldCorruptionArrow(BossEntities.EMERALD_CORRUPTION_ARROW.get(), this, level());
        arrow.setPos(from.x, from.y, from.z);

        double dx = target.getX() - from.x;
        double dy = target.getY(0.5D) - from.y;
        double dz = target.getZ() - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontalDistance * 0.2D, dz, 1.6F, 5.0F);
        arrow.setBaseDamage(configuredAttackDamage() * 0.8D);

        playAttackAnimation();
        level().playSound(null, blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, 1.0F, 0.7F);
        serverLevel.addFreshEntity(arrow);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.STONE_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.STONE_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.STONE_BREAK; }
}
