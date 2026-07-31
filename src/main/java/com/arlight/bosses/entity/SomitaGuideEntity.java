package com.arlight.bosses.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Somita como guía cinemática del Bingo. No combate, no muestra bossbar y solo
 * reproduce animaciones según las etiquetas con las que el plugin la invoca.
 */
public final class SomitaGuideEntity extends PathfinderMob implements GeoEntity {
    private static final String CONTROLLER = "main";
    private static final String INTRO = "intro";
    private static final String OUTRO = "outro";
    private static final String TAKE_EGG = "take_egg";
    private static final String GOODBYE = "goodbye";
    private static final String VANISH = "vanish";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String lastScene = "";

    public SomitaGuideEntity(EntityType<? extends SomitaGuideEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setNoAi(true);
        setSilent(true);
        setNoGravity(false);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        String scene = currentScene();
        if (!scene.equals(lastScene)) {
            lastScene = scene;
            triggerAnim(CONTROLLER, scene);
        }
    }

    private String currentScene() {
        if (getTags().contains("arlight_somita_vanish")) return VANISH;
        if (getTags().contains("arlight_somita_goodbye")) return GOODBYE;
        if (getTags().contains("arlight_somita_take_egg")) return TAKE_EGG;
        if (getTags().contains("arlight_somita_outro")) return OUTRO;
        return INTRO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 3,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.somita_guide.idle")))
                .triggerableAnim(INTRO, RawAnimation.begin().thenPlay("animation.somita_guide.kawaii_intro"))
                .triggerableAnim(OUTRO, RawAnimation.begin().thenPlay("animation.somita_guide.portal_arrival"))
                .triggerableAnim(TAKE_EGG, RawAnimation.begin().thenPlay("animation.somita_guide.take_egg"))
                .triggerableAnim(GOODBYE, RawAnimation.begin().thenPlay("animation.somita_guide.wave_goodbye"))
                .triggerableAnim(VANISH, RawAnimation.begin().thenPlay("animation.somita_guide.portal_vanish")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void checkDespawn() {
        // La cinemática decide cuándo retirarla.
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ArlightSomitaScene", lastScene);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lastScene = tag.getString("ArlightSomitaScene");
    }
}
