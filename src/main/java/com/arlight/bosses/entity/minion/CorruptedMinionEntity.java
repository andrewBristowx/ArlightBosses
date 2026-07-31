package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Base compartida de los esbirros corrompidos. */
public abstract class CorruptedMinionEntity extends Monster implements GeoEntity {
    protected static final String CONTROLLER = "main";
    protected static final String ATTACK_TRIGGER = "attack";
    protected static final String DEATH_TRIGGER = "death";
    protected static final String HURT_TRIGGER = "hurt";
    protected static final String SPAWN_TRIGGER = "spawn";
    
    private static final int DEATH_ANIMATION_TICKS = 32;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    protected double attackReach = 2.4D;
    protected float particleIntensity = 1.0F;

    protected CorruptedMinionEntity(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    protected abstract String animationPrefix();
    protected abstract ParticleOptions corruptionParticle();

    protected double defaultAttackReach() { return 2.4D; }

    @Override
    protected void registerGoals() {
        attackReach = defaultAttackReach();
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.08D, false) {
            @Override
            protected boolean canPerformAttack(LivingEntity target) {
                return isTimeToAttack()
                        && mob.distanceToSqr(target) <= attackReach * attackReach
                        && mob.getSensing().hasLineOfSight(target);
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = animationPrefix();
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state -> {
            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + ".walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + ".idle"));
        })
        .triggerableAnim(ATTACK_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".attack"))
        .triggerableAnim(DEATH_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".death"))
        .triggerableAnim(HURT_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".hurt"))
        .triggerableAnim(SPAWN_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".spawn")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        super.swing(hand, updateSelf);
        if (!level().isClientSide()) triggerAnim(CONTROLLER, ATTACK_TRIGGER);
    }

    protected void playAttackAnimation() {
        if (!level().isClientSide()) triggerAnim(CONTROLLER, ATTACK_TRIGGER);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide()) triggerAnim(CONTROLLER, ATTACK_TRIGGER);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !isDeadOrDying() && !level().isClientSide()) {
            triggerAnim(CONTROLLER, HURT_TRIGGER);
        }
        return damaged;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide()) triggerAnim(CONTROLLER, DEATH_TRIGGER);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DEATH_ANIMATION_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;

        if (tickCount == 1) {
            triggerAnim(CONTROLLER, SPAWN_TRIGGER);
        }

        if (tickCount % Math.max(3, Math.round(8.0F / Math.max(0.25F, particleIntensity))) == 0) {
            ((ServerLevel)level()).sendParticles(corruptionParticle(), getX(), getY() + getBbHeight() * 0.65D, getZ(),
                    Math.max(1, Math.round(2.0F * particleIntensity)), 0.22D, 0.35D, 0.22D, 0.01D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag minion = new CompoundTag();
        minion.putDouble("MaxHealth", getAttributeValue(Attributes.MAX_HEALTH));
        minion.putDouble("AttackDamage", getAttributeValue(Attributes.ATTACK_DAMAGE));
        minion.putDouble("FollowRange", getAttributeValue(Attributes.FOLLOW_RANGE));
        minion.putDouble("AttackReach", attackReach);
        minion.putFloat("ParticleIntensity", particleIntensity);
        tag.put("ArlightMinion", minion);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("ArlightMinion", Tag.TAG_COMPOUND)) return;
        CompoundTag minion = tag.getCompound("ArlightMinion");
        applyAttribute(Attributes.MAX_HEALTH, minion, "MaxHealth", 4.0D, 500.0D);
        applyAttribute(Attributes.ATTACK_DAMAGE, minion, "AttackDamage", 1.0D, 80.0D);
        applyAttribute(Attributes.FOLLOW_RANGE, minion, "FollowRange", 6.0D, 96.0D);
        if (tag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
            setHealth((float) Math.max(1.0, Math.min(tag.getFloat("Health"), getMaxHealth())));
        }
        if (minion.contains("AttackReach", Tag.TAG_ANY_NUMERIC)) {
            attackReach = Math.max(1.5, Math.min(minion.getDouble("AttackReach"), 8.0));
        }
        if (minion.contains("ParticleIntensity", Tag.TAG_ANY_NUMERIC)) {
            particleIntensity = (float) Math.max(0.0, Math.min(minion.getFloat("ParticleIntensity"), 4.0));
        }
    }

    private void applyAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                CompoundTag source, String key, double minimum, double maximum) {
        if (!source.contains(key, Tag.TAG_ANY_NUMERIC)) return;
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) instance.setBaseValue(Math.max(minimum, Math.min(source.getDouble(key), maximum)));
    }

    protected float configuredAttackDamage() {
        return (float) Math.max(1.0D, getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
}
