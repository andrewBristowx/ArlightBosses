package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Enderman corrompido por la energía del Guardián del Vacío. Cuando el objetivo está
 * lejos, gruñe (usa la animación "growl" que ya existía en el archivo pero nunca se
 * disparaba desde código), se teletransporta junto a él y conecta un golpe potenciado.
 * También parpadea para esquivar cuando lo golpean y evita quedarse bajo la lluvia,
 * igual que un enderman de verdad.
 */
public final class VoidEndermanMinion extends CorruptedMinionEntity {
    private static final ResourceLocation AMBUSH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("arlightbosses", "void_enderman_ambush");

    private static final int BLINK_COOLDOWN_TICKS = 90;
    private static final int GROWL_LEAD_TICKS = 14;
    private static final int EMPOWERED_WINDOW_TICKS = 30;
    private static final double BLINK_TRIGGER_RANGE = 7.0D;
    private static final double BLINK_LANDING_RADIUS = 2.2D;
    private static final float EVASIVE_TELEPORT_CHANCE = 0.3F;
    private static final int EVASIVE_COOLDOWN_TICKS = 40;
    private static final int RAIN_ESCAPE_COOLDOWN_TICKS = 20;

    private int blinkCooldown;
    private int growlTicks;
    private int empoweredTicks;
    private int evasiveCooldown;
    private int rainEscapeCooldown;

    public VoidEndermanMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.void_enderman"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.PORTAL; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        RawAnimation growl = RawAnimation.begin().thenPlay(animationPrefix() + ".growl");
        controllers.add(new AnimationController<>(this, "growl_controller", 0,
                state -> PlayState.STOP).triggerableAnim("growl", growl));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (blinkCooldown > 0) blinkCooldown--;
        if (evasiveCooldown > 0) evasiveCooldown--;
        if (rainEscapeCooldown > 0) rainEscapeCooldown--;

        if (level() instanceof ServerLevel corrosionLevel && tickCount % 47 == 0 && random.nextFloat() < 0.5F) {
            corrosionLevel.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                    3, 0.3D, 0.4D, 0.3D, 0.01D);
            if (random.nextFloat() < 0.4F) {
                corrosionLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_AMBIENT,
                        SoundSource.HOSTILE, 0.4F, 0.5F + random.nextFloat() * 0.15F);
            }
        }

        if (isInWaterRainOrBubble() && rainEscapeCooldown <= 0) {
            rainEscapeCooldown = RAIN_ESCAPE_COOLDOWN_TICKS;
            blinkTo(getX() + (random.nextDouble() - 0.5D) * 12.0D,
                    getY() + random.nextInt(6) - 2,
                    getZ() + (random.nextDouble() - 0.5D) * 12.0D, false);
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            growlTicks = 0;
            return;
        }

        if (growlTicks > 0) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            growlTicks--;
            if (growlTicks == 0) performBlinkStrike(target);
            return;
        }

        if (empoweredTicks > 0) empoweredTicks--;

        double distanceSqr = distanceToSqr(target);
        boolean farEnoughToBlink = distanceSqr > BLINK_TRIGGER_RANGE * BLINK_TRIGGER_RANGE;
        if (blinkCooldown <= 0 && farEnoughToBlink && getSensing().hasLineOfSight(target)) {
            growlTicks = GROWL_LEAD_TICKS;
            triggerAnim("growl_controller", "growl");
            level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_STARE,
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    private void performBlinkStrike(LivingEntity target) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 1.4D + random.nextDouble() * (BLINK_LANDING_RADIUS - 1.4D);
        boolean teleported = blinkTo(target.getX() + Math.cos(angle) * radius,
                target.getY(), target.getZ() + Math.sin(angle) * radius, true);
        blinkCooldown = BLINK_COOLDOWN_TICKS;
        if (teleported) {
            empoweredTicks = EMPOWERED_WINDOW_TICKS;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    /**
     * Se teletransporta a un punto; si {@code leaveCorrosionScar} es true (solo en la
     * emboscada), deja atrás un charco de corrosión que pudre a quien lo pise.
     */
    private boolean blinkTo(double x, double y, double z, boolean leaveCorrosionScar) {
        double originX = getX();
        double originY = getY();
        double originZ = getZ();
        boolean teleported = randomTeleport(x, y, z, true);
        if (teleported && level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
            if (leaveCorrosionScar) spawnCorrosionScar(serverLevel, originX, originY, originZ);
        }
        return teleported;
    }

    private void spawnCorrosionScar(ServerLevel serverLevel, double x, double y, double z) {
        AreaEffectCloud scar = new AreaEffectCloud(serverLevel, x, y, z);
        scar.setOwner(this);
        scar.setParticle(ParticleTypes.SCULK_SOUL);
        scar.setRadius(1.6F);
        scar.setDuration(60);
        scar.setRadiusPerTick(-1.6F / 60F);
        scar.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
        serverLevel.addFreshEntity(scar);
    }

    /** Golpe potenciado si venía de teletransportarse a la emboscada; refuerzo temporal de daño en vez de un segundo golpe. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean empowered = empoweredTicks > 0;
        AttributeInstance damageAttribute = getAttribute(Attributes.ATTACK_DAMAGE);
        if (empowered && damageAttribute != null) {
            damageAttribute.removeModifier(AMBUSH_MODIFIER_ID);
            damageAttribute.addTransientModifier(new AttributeModifier(AMBUSH_MODIFIER_ID,
                    configuredAttackDamage() * 0.75D, AttributeModifier.Operation.ADD_VALUE));
        }
        boolean hit = super.doHurtTarget(target);
        if (damageAttribute != null) damageAttribute.removeModifier(AMBUSH_MODIFIER_ID);
        if (hit && empowered) {
            empoweredTicks = 0;
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            }
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_SCREAM,
                        SoundSource.HOSTILE, 1.0F, 0.9F);
            }
        }
        return hit;
    }

    /** Parpadea para esquivar en vez de quedarse plantado recibiendo golpes. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && evasiveCooldown <= 0 && random.nextFloat() < EVASIVE_TELEPORT_CHANCE) {
            evasiveCooldown = EVASIVE_COOLDOWN_TICKS;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 4.0D + random.nextDouble() * 3.0D;
            blinkTo(getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius, false);
        }
        return hurt;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDERMAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDERMAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDERMAN_DEATH; }
}
