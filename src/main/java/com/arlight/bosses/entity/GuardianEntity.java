package com.arlight.bosses.entity;

import com.arlight.bosses.entity.minion.CorruptedMinionEntity;
import com.arlight.bosses.entity.minion.EmeraldCorruptionArrow;
import com.arlight.bosses.entity.minion.GildedShardProjectile;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * Base compartida de los guardianes dimensionales.
 *
 * <p>Los tres guardianes activos tienen animaciones completas y un ataque especial
 * diferente por fase. El espacio de DragonGuardian/Somita se conserva registrado
 * para compatibilidad, pero queda fuera del sistema de fases hasta que se implemente.</p>
 */
public class GuardianEntity extends Monster implements GeoEntity {
    protected static final String CONTROLLER = "main";
    protected static final String MELEE_TRIGGER = "attack";
    protected static final String PHASE_ONE_ATTACK_TRIGGER = "phase_one_attack";
    protected static final String PHASE_TWO_ATTACK_TRIGGER = "phase_two_attack";
    protected static final String PHASE_THREE_ATTACK_TRIGGER = "phase_three_attack";
    protected static final String PHASE_TWO_TRIGGER = "phase_two";
    protected static final String PHASE_THREE_TRIGGER = "phase_three";
    protected static final String ROAR_TRIGGER = "roar";
    protected static final String DEATH_TRIGGER = "death";
    protected static final String HURT_TRIGGER = "hurt";
    protected static final String SPAWN_TRIGGER = "spawn";

    private static final String NBT_PHASE = "ArlightCombatPhase";
    private static final String NBT_SPECIAL_COOLDOWN = "ArlightSpecialCooldown";

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Guardián"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    protected double attackReach = 4.5D;
    protected float phaseTwoThreshold = 0.60F;
    protected float phaseThreeThreshold = 0.25F;
    protected int currentPhase = 1;
    protected int specialAttackCooldown = 70;
    protected int phaseTransitionTicks;

    public GuardianEntity(EntityType<? extends GuardianEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 150;
        bossBar.setColor(resolveBossBarColor());
        bossBar.setDarkenScreen(true);
        updateBossBarName();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.8D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ARMOR, 18.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = animationPrefix();
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state -> {
            if (isDeadOrDying()) return state.setAndContinue(RawAnimation.begin().thenPlay(prefix + ".death"));
            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + ".walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + ".idle"));
        })
                .triggerableAnim(MELEE_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".attack"))
                .triggerableAnim(PHASE_ONE_ATTACK_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".attack_phase_1"))
                .triggerableAnim(PHASE_TWO_ATTACK_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".attack_phase_2"))
                .triggerableAnim(PHASE_THREE_ATTACK_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".attack_phase_3"))
                .triggerableAnim(PHASE_TWO_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".phase_two"))
                .triggerableAnim(PHASE_THREE_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".phase_three"))
                .triggerableAnim(ROAR_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".roar"))
                .triggerableAnim(DEATH_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".death"))
                .triggerableAnim(HURT_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".hurt"))
                .triggerableAnim(SPAWN_TRIGGER, RawAnimation.begin().thenPlay(prefix + ".spawn")));
    }

    protected String animationPrefix() {
        if (this instanceof NetherGuardian) return "animation.nether_guardian";
        if (this instanceof VoidGuardian) return "animation.void_guardian";
        if (this instanceof DragonGuardian) return "animation.somita_vampire";
        return "animation.surface_guardian";
    }

    protected boolean supportsPhaseCombat() {
        return !(this instanceof DragonGuardian);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;

        if (tickCount == 1) triggerAnim(CONTROLLER, SPAWN_TRIGGER);

        bossBar.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        if (supportsPhaseCombat()) updateCombatPhase();
        spawnAuraParticles();

        if (specialAttackCooldown > 0) specialAttackCooldown--;
        if (phaseTransitionTicks > 0) {
            phaseTransitionTicks--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.25D, 1.0D, 0.25D));
        }
    }

    private void spawnAuraParticles() {
        if (!(level() instanceof ServerLevel world)) return;

        double radius = 2.0D + (currentPhase * 0.5D);
        double speed = 0.15D;
        for (int i = 0; i < 2; i++) {
            double angle = (tickCount * speed) + (i * Math.PI);
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            world.sendParticles(primaryParticle(), getX() + ox, getY() + 0.1D, getZ() + oz,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        if (tickCount % 3 == 0) {
            world.sendParticles(secondaryParticle(),
                    getX() + (random.nextDouble() - 0.5D) * 2.0D,
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * 2.0D,
                    currentPhase, 0.0D, 0.15D, 0.0D, 0.04D);
        }
    }

    private void updateCombatPhase() {
        float ratio = getHealth() / getMaxHealth();
        if (currentPhase < 3 && ratio <= phaseThreeThreshold) enterPhase(3);
        else if (currentPhase < 2 && ratio <= phaseTwoThreshold) enterPhase(2);
    }

    private void enterPhase(int phase) {
        currentPhase = phase;
        phaseTransitionTicks = phase == 3 ? 52 : 40;
        specialAttackCooldown = 35;
        getNavigation().stop();

        triggerAnim(CONTROLLER, phase == 3 ? PHASE_THREE_TRIGGER : PHASE_TWO_TRIGGER);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 1.2F, phase == 3 ? 0.72F : 0.92F);

        if (level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.0D, getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            world.sendParticles(primaryParticle(), getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    80 + phase * 30, 2.0D, 2.0D, 2.0D, 0.18D);
            spawnMinions(world, 1 + phase);
        }

        applyPhaseAttributes();
        addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        updateBossBarName();
    }

    private void applyPhaseAttributes() {
        setBaseAttribute(Attributes.ATTACK_DAMAGE, 18.0D + (currentPhase - 1) * 4.0D);
        setBaseAttribute(Attributes.MOVEMENT_SPEED, 0.28D + (currentPhase - 1) * 0.025D);
        setBaseAttribute(Attributes.ARMOR, 18.0D + (currentPhase - 1) * 3.0D);
    }

    private void setBaseAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                  double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private void updateBossBarName() {
        String name;
        if (this instanceof SurfaceGuardian) name = "Guardián de la Superficie";
        else if (this instanceof NetherGuardian) name = "Guardián del Nether";
        else if (this instanceof VoidGuardian) name = "Guardián del Vacío";
        else name = "Guardián";
        bossBar.setName(Component.literal(name + (supportsPhaseCombat() ? "  •  Fase " + currentPhase : "")));
    }

    private void spawnMinions(ServerLevel world, int count) {
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5D) * 9.0D;
            double oz = (random.nextDouble() - 0.5D) * 9.0D;
            EntityType<? extends Mob> minionType = getThemeMinion();
            Mob minion = minionType.spawn(world, blockPosition().offset((int) ox, 1, (int) oz), MobSpawnType.MOB_SUMMONED);
            if (minion != null && getTarget() != null) minion.setTarget(getTarget());
        }
    }

    protected EntityType<? extends Mob> getThemeMinion() {
        if (this instanceof NetherGuardian) {
            int rand = random.nextInt(5);
            if (rand == 0) return BossEntities.GILDED_PIGLIN_MINION.get();
            if (rand == 1) return BossEntities.GILDED_WITHER_SKELETON_VANGUARD_MINION.get();
            if (rand == 2) return BossEntities.GILDED_BLAZE_WRAITH_MINION.get();
            if (rand == 3) return BossEntities.MOLTEN_STRIDER_MINION.get();
            return BossEntities.GILDED_HOGLIN_RIDER_MINION.get();
        }
        if (this instanceof VoidGuardian || this instanceof DragonGuardian) {
            int rand = random.nextInt(6);
            if (rand == 0) return BossEntities.VOID_ENDERMAN_MINION.get();
            if (rand == 1) return BossEntities.VOID_ENDERMAN_SENTINEL_MINION.get();
            if (rand == 2) return BossEntities.AMETHYST_EYE_MINION.get();
            if (rand == 3) return BossEntities.AMETHYST_PHANTOM_MINION.get();
            if (rand == 4) return BossEntities.AMETHYST_SHULKER_MINION.get();
            return BossEntities.AMETHYST_GUARDIAN_SHARD_MINION.get();
        }
        int rand = random.nextInt(6);
        if (rand == 0) return BossEntities.EMERALD_ZOMBIE_MINION.get();
        if (rand == 1) return BossEntities.EMERALD_CREEPER_MINION.get();
        if (rand == 2) return BossEntities.MOSSBOUND_SPIDER_MINION.get();
        if (rand == 3) return BossEntities.EMERALD_SKELETON_ARCHER_MINION.get();
        if (rand == 4) return BossEntities.EMERALD_RAVAGER_CUB_MINION.get();
        return BossEntities.EMERALD_GOLEM_SENTINEL_MINION.get();
    }

    private void triggerCurrentPhaseAttackAnimation() {
        String trigger = currentPhase == 1 ? PHASE_ONE_ATTACK_TRIGGER
                : currentPhase == 2 ? PHASE_TWO_ATTACK_TRIGGER
                : PHASE_THREE_ATTACK_TRIGGER;
        triggerAnim(CONTROLLER, trigger);
    }

    private int phaseAttackWindup() {
        return currentPhase == 1 ? 14 : currentPhase == 2 ? 20 : 27;
    }

    private int phaseAttackDuration() {
        return currentPhase == 1 ? 32 : currentPhase == 2 ? 42 : 58;
    }

    private int phaseAttackCooldown() {
        return currentPhase == 1 ? 105 : currentPhase == 2 ? 88 : 72;
    }

    private void performCurrentPhaseAttack(LivingEntity target) {
        if (!(level() instanceof ServerLevel world) || target == null || !target.isAlive()) return;

        if (this instanceof SurfaceGuardian) {
            if (currentPhase == 1) surfaceQuake(world);
            else if (currentPhase == 2) surfaceEmeraldVolley(world, target);
            else surfaceOvergrowth(world);
            return;
        }

        if (this instanceof NetherGuardian) {
            if (currentPhase == 1) netherFlameCleave(world);
            else if (currentPhase == 2) netherGildedBarrage(world, target);
            else netherInfernoEruption(world);
            return;
        }

        if (this instanceof VoidGuardian) {
            if (currentPhase == 1) voidBlinkStrike(world, target);
            else if (currentPhase == 2) voidGravityWell(world);
            else voidCollapse(world);
        }
    }

    /** Fase 1 de Superficie: golpe sísmico frontal y elevación corta. */
    private void surfaceQuake(ServerLevel world) {
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.25F, 0.65F);
        world.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.5D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.2D, getZ(),
                65, 4.5D, 0.35D, 4.5D, 0.12D);
        for (Player player : combatPlayers(6.5D)) {
            player.hurt(damageSources().mobAttack(this), attackDamage(1.05F));
            player.push(0.0D, 0.75D, 0.0D);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 1));
        }
    }

    /** Fase 2 de Superficie: lluvia de rocas musgosas que se fragmentan al impactar. */
    private void surfaceEmeraldVolley(ServerLevel world, LivingEntity target) {
        level().playSound(null, blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, 1.15F, 0.62F);
        Vec3 from = position().add(0.0D, getEyeHeight() * 0.82D, 0.0D);
        for (int i = 0; i < 9; i++) {
            EmeraldCorruptionArrow rock = new EmeraldCorruptionArrow(
                    BossEntities.EMERALD_CORRUPTION_ARROW.get(), this, level());
            rock.setStoneVariant(true);
            rock.setPos(from.x, from.y, from.z);
            double dx = target.getX() - from.x + (random.nextDouble() - 0.5D) * 2.6D;
            double dy = target.getY(0.50D) - from.y + (random.nextDouble() - 0.5D) * 1.2D;
            double dz = target.getZ() - from.z + (random.nextDouble() - 0.5D) * 2.6D;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            rock.shoot(dx, dy + horizontal * 0.18D, dz, 1.45F, 4.0F);
            rock.setBaseDamage(attackDamage(0.48F));
            world.addFreshEntity(rock);
        }
        world.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                from.x, from.y, from.z, 55, 1.2D, 1.0D, 1.2D, 0.18D);
    }

    /** Fase 3 de Superficie: sobrecrecimiento corrupto que cubre toda la arena cercana. */
    private void surfaceOvergrowth(ServerLevel world) {
        level().playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 1.15F, 1.25F);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0D, getZ(),
                180, 8.0D, 2.0D, 8.0D, 0.22D);
        world.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.0D, getZ(),
                130, 7.0D, 2.5D, 7.0D, 0.14D);
        for (Player player : combatPlayers(12.0D)) {
            player.hurt(damageSources().mobAttack(this), attackDamage(0.85F));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }
        spawnSpecificMinion(world, BossEntities.EMERALD_GOLEM_SENTINEL_MINION.get());
        spawnSpecificMinion(world, BossEntities.EMERALD_RAVAGER_CUB_MINION.get());
    }

    /** Fase 1 del Nether: barrido de fuego solo en el frente del guardián. */
    private void netherFlameCleave(ServerLevel world) {
        level().playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 1.3F, 0.65F);
        Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        for (int step = 1; step <= 8; step++) {
            world.sendParticles(ParticleTypes.FLAME,
                    getX() + look.x * step, getY() + 0.8D, getZ() + look.z * step,
                    14, 0.8D, 0.55D, 0.8D, 0.08D);
        }
        for (Player player : combatPlayers(8.0D)) {
            Vec3 toward = player.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            if (look.dot(toward) < 0.05D) continue;
            player.hurt(damageSources().mobAttack(this), attackDamage(1.0F));
            player.igniteForSeconds(5.0F);
            player.push(look.x * 0.85D, 0.2D, look.z * 0.85D);
        }
    }

    /** Fase 2 del Nether: bloques de oro materializados sobre el objetivo y dejados caer. */
    private void netherGildedBarrage(ServerLevel world, LivingEntity target) {
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE,
                SoundSource.HOSTILE, 1.25F, 0.78F);
        for (int i = 0; i < 11; i++) {
            double spawnX = target.getX() + (random.nextDouble() - 0.5D) * 9.0D;
            double spawnY = target.getY() + 9.0D + random.nextDouble() * 5.0D;
            double spawnZ = target.getZ() + (random.nextDouble() - 0.5D) * 9.0D;
            GildedShardProjectile goldBlock = new GildedShardProjectile(
                    BossEntities.GILDED_SHARD_PROJECTILE.get(), this, level());
            goldBlock.setGoldBlockVariant(true);
            goldBlock.setPos(spawnX, spawnY, spawnZ);
            goldBlock.setDeltaMovement(
                    (target.getX() - spawnX) * 0.015D,
                    -0.58D - random.nextDouble() * 0.18D,
                    (target.getZ() - spawnZ) * 0.015D);
            goldBlock.setBaseDamage(attackDamage(0.62F));
            world.addFreshEntity(goldBlock);
            world.sendParticles(ParticleTypes.FLAME, spawnX, spawnY, spawnZ,
                    10, 0.35D, 0.35D, 0.35D, 0.06D);
        }
        world.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()),
                target.getX(), target.getY() + 8.0D, target.getZ(),
                70, 4.5D, 3.0D, 4.5D, 0.12D);
    }

    /** Fase 3 del Nether: erupción circular de magma, fuego y refuerzos élite. */
    private void netherInfernoEruption(ServerLevel world) {
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.5F, 0.55F);
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0D, getZ(),
                210, 9.0D, 2.5D, 9.0D, 0.28D);
        world.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5D, getZ(),
                85, 8.0D, 1.5D, 8.0D, 0.18D);
        for (Player player : combatPlayers(13.0D)) {
            player.hurt(damageSources().mobAttack(this), attackDamage(1.15F));
            player.igniteForSeconds(8.0F);
            Vec3 away = player.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            player.push(away.x * 1.1D, 0.85D, away.z * 1.1D);
        }
        spawnSpecificMinion(world, BossEntities.GILDED_BLAZE_WRAITH_MINION.get());
        spawnSpecificMinion(world, BossEntities.GILDED_HOGLIN_RIDER_MINION.get());
    }

    /** Fase 1 del Vacío: estocada de su arma que atraviesa una línea frente al guardián. */
    private void voidBlinkStrike(ServerLevel world, LivingEntity target) {
        Vec3 direction = horizontalDirectionTo(target);
        level().playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.HOSTILE, 1.25F, 0.62F);
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.0F, 0.55F);

        Vec3 origin = position().add(0.0D, 1.45D, 0.0D);
        for (int i = 1; i <= 24; i++) {
            double distance = i * 0.45D;
            Vec3 point = origin.add(direction.scale(distance));
            world.sendParticles(i % 3 == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.END_ROD,
                    point.x, point.y, point.z, 1, 0.08D, 0.08D, 0.08D, 0.01D);
        }

        for (Player player : combatPlayers(12.0D)) {
            if (!insideWeaponLane(player, origin, direction, 10.8D, 1.65D)) continue;
            player.hurt(damageSources().mobAttack(this), attackDamage(1.28F));
            player.push(direction.x * 1.35D, 0.35D, direction.z * 1.35D);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
        }
        setDeltaMovement(direction.x * 0.72D, 0.08D, direction.z * 0.72D);
    }

    /** Fase 2 del Vacío: tres cortes dimensionales disparados por el arma. */
    private void voidGravityWell(ServerLevel world) {
        LivingEntity target = getTarget();
        Vec3 center = horizontalDirectionTo(target);
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 1.35F, 0.48F);
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.15F, 0.42F);

        double[] angles = {-42.0D, 0.0D, 42.0D};
        Vec3 origin = position().add(0.0D, 1.55D, 0.0D);
        for (double angle : angles) {
            Vec3 slash = rotateHorizontal(center, angle);
            for (int i = 2; i <= 26; i++) {
                double distance = i * 0.48D;
                Vec3 point = origin.add(slash.scale(distance));
                world.sendParticles(i % 4 == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.REVERSE_PORTAL,
                        point.x, point.y + Math.sin(i * 0.55D) * 0.35D, point.z,
                        1, 0.08D, 0.12D, 0.08D, 0.02D);
            }
        }

        for (Player player : combatPlayers(14.0D)) {
            boolean hit = false;
            for (double angle : angles) {
                if (insideWeaponLane(player, origin, rotateHorizontal(center, angle), 12.5D, 1.35D)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) continue;
            player.hurt(damageSources().mobAttack(this), attackDamage(0.92F));
            Vec3 away = player.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 0.01D) away = away.normalize();
            player.push(away.x * 0.85D, 0.50D, away.z * 0.85D);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 28, 0));
        }
    }

    /** Fase 3 del Vacío: clava el arma y abre grietas radiales por toda la arena. */
    private void voidCollapse(ServerLevel world) {
        level().playSound(null, blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY,
                SoundSource.HOSTILE, 1.55F, 0.50F);
        level().playSound(null, blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.HOSTILE, 0.85F, 1.18F);
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.25D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);

        int rifts = 12;
        for (int ray = 0; ray < rifts; ray++) {
            double angle = (Math.PI * 2.0D * ray) / rifts;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            for (int step = 1; step <= 18; step++) {
                Vec3 point = position().add(direction.scale(step * 0.8D));
                world.sendParticles(step % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
                        point.x, getY() + 0.12D + random.nextDouble() * 0.35D, point.z,
                        2, 0.12D, 0.18D, 0.12D, 0.04D);
            }
        }
        world.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 0.8D, getZ(),
                120, 8.5D, 1.8D, 8.5D, 0.18D);

        for (Player player : combatPlayers(15.0D)) {
            player.hurt(damageSources().mobAttack(this), attackDamage(1.18F));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 2));
            Vec3 pull = position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
            if (pull.lengthSqr() > 0.01D) pull = pull.normalize().scale(1.10D);
            player.push(pull.x, 0.90D, pull.z);
        }
        spawnSpecificMinion(world, BossEntities.AMETHYST_GUARDIAN_SHARD_MINION.get());
        spawnSpecificMinion(world, BossEntities.AMETHYST_PHANTOM_MINION.get());
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        Vec3 direction;
        if (target != null) direction = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        else direction = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.001D) direction = new Vec3(0.0D, 0.0D, 1.0D);
        return direction.normalize();
    }

    private Vec3 rotateHorizontal(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(direction.x * cos - direction.z * sin, 0.0D,
                direction.x * sin + direction.z * cos).normalize();
    }

    private boolean insideWeaponLane(Player player, Vec3 origin, Vec3 direction,
                                     double reach, double halfWidth) {
        Vec3 point = player.position().add(0.0D, player.getBbHeight() * 0.45D, 0.0D);
        Vec3 relative = point.subtract(origin);
        double along = relative.dot(direction);
        if (along < 0.0D || along > reach) return false;
        Vec3 lateral = relative.subtract(direction.scale(along));
        return lateral.lengthSqr() <= halfWidth * halfWidth;
    }

    private float attackDamage(float multiplier) {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
    }

    private List<Player> combatPlayers(double radius) {
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
    }

    private void spawnSpecificMinion(ServerLevel world, EntityType<? extends Mob> type) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int ox = (int) Math.round(Math.cos(angle) * 4.0D);
        int oz = (int) Math.round(Math.sin(angle) * 4.0D);
        Mob minion = type.spawn(world, blockPosition().offset(ox, 1, oz), MobSpawnType.MOB_SUMMONED);
        if (minion != null && getTarget() != null) minion.setTarget(getTarget());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PhaseSpecialAttackGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    static final class PhaseSpecialAttackGoal extends Goal {
        private final GuardianEntity boss;
        private int timer;
        private LivingEntity target;

        PhaseSpecialAttackGoal(GuardianEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity currentTarget = boss.getTarget();
            return boss.supportsPhaseCombat()
                    && boss.phaseTransitionTicks <= 0
                    && boss.specialAttackCooldown <= 0
                    && currentTarget != null
                    && currentTarget.isAlive()
                    && boss.distanceToSqr(currentTarget) <= 24.0D * 24.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && timer < boss.phaseAttackDuration();
        }

        @Override
        public void start() {
            timer = 0;
            target = boss.getTarget();
            boss.getNavigation().stop();
            boss.triggerCurrentPhaseAttackAnimation();
        }

        @Override
        public void tick() {
            timer++;
            boss.getNavigation().stop();
            if (target != null) boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (timer == boss.phaseAttackWindup()) boss.performCurrentPhaseAttack(target);
        }

        @Override
        public void stop() {
            boss.specialAttackCooldown = boss.phaseAttackCooldown();
            target = null;
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }
    }

    private ParticleOptions primaryParticle() {
        if (this instanceof SurfaceGuardian) return ParticleTypes.HAPPY_VILLAGER;
        if (this instanceof NetherGuardian) return ParticleTypes.FLAME;
        if (this instanceof VoidGuardian) return ParticleTypes.PORTAL;
        return ParticleTypes.ENCHANT;
    }

    private ParticleOptions secondaryParticle() {
        if (this instanceof SurfaceGuardian) return ParticleTypes.CHERRY_LEAVES;
        if (this instanceof NetherGuardian) return ParticleTypes.LAVA;
        if (this instanceof VoidGuardian) return ParticleTypes.WITCH;
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    private BossEvent.BossBarColor resolveBossBarColor() {
        if (this instanceof SurfaceGuardian) return BossEvent.BossBarColor.GREEN;
        if (this instanceof NetherGuardian) return BossEvent.BossBarColor.RED;
        if (this instanceof VoidGuardian) return BossEvent.BossBarColor.PURPLE;
        return BossEvent.BossBarColor.BLUE;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        super.swing(hand, updateSelf);
        if (!level().isClientSide()) triggerAnim(CONTROLLER, MELEE_TRIGGER);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide()) triggerAnim(CONTROLLER, MELEE_TRIGGER);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide() && !isDeadOrDying()) {
            triggerAnim(CONTROLLER, HURT_TRIGGER);
        }
        return damaged;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) triggerAnim(CONTROLLER, DEATH_TRIGGER);
        bossBar.setProgress(0.0F);
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= 82 && !level().isClientSide() && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_PHASE, currentPhase);
        tag.putInt(NBT_SPECIAL_COOLDOWN, specialAttackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        currentPhase = Math.max(1, Math.min(3, tag.getInt(NBT_PHASE)));
        if (!tag.contains(NBT_PHASE)) currentPhase = 1;
        specialAttackCooldown = Math.max(0, tag.getInt(NBT_SPECIAL_COOLDOWN));
        applyPhaseAttributes();
        updateBossBarName();
    }
}
