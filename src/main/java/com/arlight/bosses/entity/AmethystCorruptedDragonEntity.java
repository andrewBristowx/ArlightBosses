package com.arlight.bosses.entity;

import com.arlight.bosses.ArlightBosses;
import com.arlight.bosses.entity.minion.VoidEndermanMinion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad propia del Dragón Corrupto de Amatista.
 *
 * No deriva ni transforma a minecraft:ender_dragon: posee EntityType, huevo,
 * vida, IA de vuelo, barra y fases propias. El modelo vanilla se reutiliza
 * únicamente en el renderer para conservar la silueta de dragón.
 */
public final class AmethystCorruptedDragonEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_COMBAT_PHASE =
            SynchedEntityData.defineId(AmethystCorruptedDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_COCOONED =
            SynchedEntityData.defineId(AmethystCorruptedDragonEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float MAX_BOSS_HEALTH = 500.0F;
    private static final float PHASE_TWO_HEALTH = 300.0F;
    private static final float PHASE_THREE_HEALTH = 125.0F;

    private static final DustParticleOptions MAGENTA_DUST =
            new DustParticleOptions(new Vector3f(0.88F, 0.10F, 1.00F), 1.45F);
    private static final DustParticleOptions WHITE_VIOLET_DUST =
            new DustParticleOptions(new Vector3f(0.93F, 0.80F, 1.00F), 1.20F);
    private static final BlockParticleOption AMETHYST_BLOCK_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());
    private static final BlockParticleOption AMETHYST_CLUSTER_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState());

    private static final String NBT_PHASE = "CombatPhase";
    private static final String NBT_CENTER_X = "ArenaCenterX";
    private static final String NBT_CENTER_Y = "ArenaCenterY";
    private static final String NBT_CENTER_Z = "ArenaCenterZ";
    private static final String NBT_COCOON_TICKS = "CocoonTicks";
    private static final String NBT_COCOON_X = "CocoonX";
    private static final String NBT_COCOON_Y = "CocoonY";
    private static final String NBT_COCOON_Z = "CocoonZ";
    private static final String NBT_DEATH_EGG_PLACED = "DeathEggPlaced";
    private static final int COCOON_DURATION_TICKS = 140;
    private static final int DEATH_FADE_TICKS = 100;

    /** Datos de latencia usados por el modelo vanilla del dragón. */
    public final double[][] positions = new double[64][3];
    public int posPointer = -1;
    public float oFlapTime;
    public float flapTime;
    public int dragonDeathTime;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§d✦ Dragón Corrupto de Amatista §5• Fase 1"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    private BlockPos arenaCenter;
    private Vec3 flightWaypoint;
    private int flightPattern;
    private int flightPatternTicks;
    private int breathCooldown = 45;
    private int summonCooldown = 600;
    private int beamCooldown = 90;
    private int rainCooldown = 180;
    private int sonicCooldown = 180;
    private int frenzyCooldown = 35;
    private int collisionCooldown;
    private int reflectSoundCooldown;
    private int beamTicks;
    private UUID beamTarget;
    private int rainTicks;
    private BlockPos rainCenter;
    private int lastCrystalFlapSound = -100;
    private int cocoonTicks;
    private Vec3 cocoonAnchor;
    private boolean deathEggPlaced;
    private int aiFailureCount;
    private int lastAiFailureTick = -200;
    private final List<BlockPos> resonantBuds = new ArrayList<>();

    public AmethystCorruptedDragonEntity(
            EntityType<? extends AmethystCorruptedDragonEntity> type,
            Level level
    ) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true; // Como el dragón vanilla, no se atasca en torres o terreno.
        this.noCulling = true;
        this.xpReward = 12000;
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.bossEvent.setPlayBossMusic(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_BOSS_HEALTH)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 192.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.FLYING_SPEED, 0.72D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // El dragón utiliza una máquina de estados aérea propia.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COMBAT_PHASE, 1);
        builder.define(DATA_COCOONED, false);
    }

    public int getCombatPhase() {
        return Mth.clamp(this.entityData.get(DATA_COMBAT_PHASE), 1, 3);
    }

    private void setCombatPhase(int phase) {
        this.entityData.set(DATA_COMBAT_PHASE, Mth.clamp(phase, 1, 3));
    }

    public boolean isCocooned() {
        return this.entityData.get(DATA_COCOONED);
    }

    private void setCocooned(boolean cocooned) {
        this.entityData.set(DATA_COCOONED, cocooned);
    }

    public float getDeathFadeProgress(float partialTick) {
        if (!this.isDeadOrDying()) return 0.0F;
        return Mth.clamp((this.deathTime + partialTick) / (float) DEATH_FADE_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.oFlapTime = this.flapTime;
        Vec3 motion = this.getDeltaMovement();
        double horizontal = motion.horizontalDistance();
        if (getCombatPhase() == 2 && !isCocooned()) {
            // En la fase terrestre las alas permanecen plegadas y dejan de aletear.
            this.flapTime = Mth.lerp(0.14F, this.flapTime, 0.18F);
        } else if (isCocooned()) {
            this.flapTime = Mth.lerp(0.20F, this.flapTime, 0.0F);
        } else {
            this.flapTime += 0.045F + (float) Math.min(0.16D, horizontal * 0.14D);
        }
        updateLatencyHistory();
        super.tick();
    }

    private void updateLatencyHistory() {
        if (this.posPointer < 0) {
            for (int i = 0; i < this.positions.length; i++) {
                this.positions[i][0] = this.getYRot();
                this.positions[i][1] = this.getY();
                this.positions[i][2] = this.getXRot();
            }
        }
        this.posPointer = (this.posPointer + 1) & 63;
        this.positions[this.posPointer][0] = this.getYRot();
        this.positions[this.posPointer][1] = this.getY();
        this.positions[this.posPointer][2] = this.getXRot();
    }

    public double[] getLatencyPos(int offset, float partialTick) {
        if (this.isDeadOrDying()) partialTick = 0.0F;
        partialTick = 1.0F - partialTick;
        int current = this.posPointer - offset & 63;
        int previous = this.posPointer - offset - 1 & 63;
        double[] result = new double[3];
        double yaw = this.positions[current][0];
        double yawDelta = Mth.wrapDegrees(this.positions[previous][0] - yaw);
        result[0] = yaw + yawDelta * partialTick;
        double y = this.positions[current][1];
        result[1] = y + (this.positions[previous][1] - y) * partialTick;
        result[2] = Mth.lerp((double) partialTick,
                this.positions[current][2], this.positions[previous][2]);
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
        // Un fallo de IA nunca debe tumbar el servidor dedicado. Arclight mezcla
        // eventos Bukkit y NeoForge, por lo que protegemos la máquina de estados y
        // dejamos al dragón en un estado recuperable antes del siguiente tick.
        try {
            runProtectedBossAi(level);
            if (aiFailureCount > 0 && tickCount % 100 == 0) aiFailureCount--;
        } catch (Throwable failure) {
            aiFailureCount++;
            this.setDeltaMovement(Vec3.ZERO);
            this.flightWaypoint = null;
            this.beamTicks = 0;
            this.rainTicks = 0;
            if (tickCount - lastAiFailureTick >= 100) {
                lastAiFailureTick = tickCount;
                ArlightBosses.LOGGER.error("Fallo contenido en la IA del Dragón Corrupto (intento {}). "
                        + "La entidad fue estabilizada sin cerrar el servidor.", aiFailureCount, failure);
            }
            if (this.arenaCenter == null) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Mth.floor(getX()), Mth.floor(getZ()));
                this.arenaCenter = new BlockPos(Mth.floor(getX()), y, Mth.floor(getZ()));
            }
            if (aiFailureCount >= 12) {
                // Evita un bucle de error por tick; conserva la entidad y su bossbar,
                // pero la coloca en reposo hasta que vuelva a encontrar un objetivo.
                this.setTarget(null);
                this.setNoGravity(true);
                this.noPhysics = true;
                aiFailureCount = 6;
            }
        }
    }

    private void runProtectedBossAi(ServerLevel level) {
        ensureInitialized(level);
        tickCooldowns();
        updateCombatPhase(level);
        updateBossBar();
        tickAura(level);
        configureBreathClouds(level);
        cleanupExpiredFallingCrystals(level);

        if (isCocooned()) {
            tickCocoon(level);
            return;
        }

        LivingEntity target = findTarget(level, 190.0D);
        if (target == null) {
            if (getCombatPhase() == 2) moveOnGround(level, Vec3.atCenterOf(this.arenaCenter), 0.18D);
            else flyToward(Vec3.atCenterOf(this.arenaCenter).add(0.0D, 18.0D, 0.0D), 0.40D);
            return;
        }
        this.setTarget(target);
        int phase = getCombatPhase();
        if (phase == 1) tickPhaseOne(level, target);
        else if (phase == 2) tickPhaseTwo(level, target);
        else tickPhaseThree(level, target);
        hitNearbyPlayers(level);
    }

    private void ensureInitialized(ServerLevel level) {
        if (this.arenaCenter != null) return;
        int x = Mth.floor(this.getX());
        int z = Mth.floor(this.getZ());
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        this.arenaCenter = new BlockPos(x, y, z);
        this.setCustomName(Component.literal("§dDragón del End Corrupto por la Amatista"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setHealth(MAX_BOSS_HEALTH);
        this.flightWaypoint = this.position().add(0.0D, 8.0D, 0.0D);
        setAirborneMode();
        playAncestralVoice(level,
                "§5[Dragón ancestral] §d¿Me desafías? §fVerás las consecuencias. §dÚnete a la amatista.");
        level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                this.getX(), this.getY() + 2.5D, this.getZ(),
                220, 5.5D, 3.5D, 5.5D, 0.16D);
    }

    private void updateCombatPhase(ServerLevel level) {
        int phase = getCombatPhase();
        if (phase < 2 && this.getHealth() <= PHASE_TWO_HEALTH) {
            enterPhaseTwo(level);
            phase = 2;
        }
        if (phase < 3 && this.getHealth() <= PHASE_THREE_HEALTH) {
            enterPhaseThree(level);
        }
    }

    private void tickCooldowns() {
        if (breathCooldown > 0) breathCooldown--;
        if (summonCooldown > 0) summonCooldown--;
        if (beamCooldown > 0) beamCooldown--;
        if (rainCooldown > 0) rainCooldown--;
        if (sonicCooldown > 0) sonicCooldown--;
        if (frenzyCooldown > 0) frenzyCooldown--;
        if (collisionCooldown > 0) collisionCooldown--;
        if (reflectSoundCooldown > 0) reflectSoundCooldown--;
        if (flightPatternTicks > 0) flightPatternTicks--;
    }

    /**
     * Fase 1: alterna ascenso, pasada lateral, picado y retirada.
     * No usa una órbita fija, por lo que ya no se queda girando en círculos.
     */
    private void tickPhaseOne(ServerLevel level, LivingEntity target) {
        if (flightWaypoint == null || flightPatternTicks <= 0
                || this.position().distanceToSqr(flightWaypoint) < 16.0D) {
            chooseAttackWaypoint(target, false);
        }
        flyToward(flightWaypoint, flightPattern == 2 ? 0.92D : 0.66D);

        if (breathCooldown <= 0 && (flightPattern == 1 || flightPattern == 3)) {
            fireAmethystBreath(level, target);
            breathCooldown = 92;
        }
        if (summonCooldown <= 0) {
            summonCorruptedEndermen(level, target, 3);
            summonCooldown = 600;
        }
    }

    /**
     * Fase 2: realiza pasadas rectas entre ataques, se detiene para cargar
     * el rayo y vuela por encima del objetivo durante la lluvia de esquirlas.
     */
    private void tickPhaseTwo(ServerLevel level, LivingEntity target) {
        // Furia Geoda es completamente terrestre: el dragón desciende, pliega
        // las alas y persigue al jugador desde el suelo.
        setGroundedMode();

        if (beamTicks > 0) {
            tickCrystalBeam(level);
            holdGroundPosition(level);
            return;
        }
        if (rainTicks > 0) {
            tickShardRain(level);
            holdGroundPosition(level);
            return;
        }

        double distance = this.distanceToSqr(target);
        if (distance > 10.0D * 10.0D) {
            moveOnGround(level, target.position(), 0.38D);
        } else {
            holdGroundPosition(level);
            facePoint(target.getEyePosition());
            if (frenzyCooldown <= 0) {
                groundClawStrike(level, target);
                frenzyCooldown = 34;
            }
        }

        if (beamCooldown <= 0) {
            startCrystalBeam(level, target);
            beamCooldown = 230;
        } else if (rainCooldown <= 0) {
            startShardRain(level, target);
            rainCooldown = 285;
        }
    }

    private void tickPhaseThree(ServerLevel level, LivingEntity target) {
        setAirborneMode();
        applyResonantField(level);
        if (sonicCooldown <= 0) {
            sonicShatter(level);
            sonicCooldown = 240;
        }

        Vec3 center = Vec3.atCenterOf(this.arenaCenter).add(0.0D, 5.5D, 0.0D);
        if (frenzyCooldown <= 0) {
            this.flightWaypoint = target.getEyePosition().add(0.0D, 0.5D, 0.0D);
            frenzyCooldown = 58;
            if (this.distanceToSqr(target) > 24.0D * 24.0D) fireAmethystBreath(level, target);
        } else if (this.position().distanceToSqr(this.flightWaypoint == null ? center : this.flightWaypoint) < 16.0D) {
            this.flightWaypoint = center;
        }
        flyToward(this.flightWaypoint == null ? center : this.flightWaypoint, 1.08D);
    }

    private void setAirborneMode() {
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    private void setGroundedMode() {
        // Se mantiene noPhysics para que el enorme hitbox no quede atrapado en
        // bloques de la arena; la altura se fija manualmente al terreno.
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setXRot(Mth.rotLerp(0.30F, this.getXRot(), 0.0F));
    }

    private void moveOnGround(ServerLevel level, Vec3 destination, double speed) {
        int x = Mth.floor(this.getX());
        int z = Mth.floor(this.getZ());
        double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 0.10D;
        Vec3 horizontal = new Vec3(destination.x - this.getX(), 0.0D, destination.z - this.getZ());
        Vec3 desiredHorizontal = horizontal.lengthSqr() < 0.01D
                ? Vec3.ZERO
                : horizontal.normalize().scale(speed);
        double vertical = Mth.clamp((groundY - this.getY()) * 0.28D, -0.72D, 0.42D);
        Vec3 current = this.getDeltaMovement();
        Vec3 motion = new Vec3(
                Mth.lerp(0.24D, current.x, desiredHorizontal.x),
                vertical,
                Mth.lerp(0.24D, current.z, desiredHorizontal.z));
        this.setDeltaMovement(motion);
        if (desiredHorizontal.lengthSqr() > 0.001D) faceMovement(desiredHorizontal);
        this.setXRot(0.0F);
    }

    private void holdGroundPosition(ServerLevel level) {
        int x = Mth.floor(this.getX());
        int z = Mth.floor(this.getZ());
        double groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 0.10D;
        double vertical = Mth.clamp((groundY - this.getY()) * 0.30D, -0.65D, 0.38D);
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.58D, 0.0D, 0.58D).add(0.0D, vertical, 0.0D));
        this.setXRot(0.0F);
    }

    private void groundClawStrike(ServerLevel level, LivingEntity target) {
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE, 2.2F, 0.58F);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.HOSTILE, 1.8F, 0.62F);
        level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                target.getX(), target.getY() + 0.5D, target.getZ(),
                55, 1.7D, 0.8D, 1.7D, 0.16D);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(target.position()) > 5.5D * 5.5D) continue;
            player.hurt(level.damageSources().mobAttack(this), 12.0F);
            Vec3 away = player.position().subtract(this.position()).normalize().scale(1.05D);
            player.push(away.x, 0.48D, away.z);
            player.hurtMarked = true;
        }
    }

    private void chooseAttackWaypoint(LivingEntity target, boolean furious) {
        this.flightPattern = (this.flightPattern + 1) & 3;
        this.flightPatternTicks = furious ? 62 : 78;
        Vec3 targetPos = target.position();
        Vec3 forward = target.getLookAngle();
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 0.001D) forward = new Vec3(0.0D, 0.0D, 1.0D);
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double direction = ((this.tickCount / 40) & 1) == 0 ? 1.0D : -1.0D;

        switch (this.flightPattern) {
            case 0 -> this.flightWaypoint = targetPos.add(side.scale(15.0D * direction)).add(0.0D, 13.0D, 0.0D);
            case 1 -> this.flightWaypoint = target.getEyePosition().add(forward.scale(-5.0D)).add(0.0D, 3.0D, 0.0D);
            case 2 -> this.flightWaypoint = target.getEyePosition().add(forward.scale(7.0D)); // Picado recto.
            default -> this.flightWaypoint = targetPos.add(side.scale(-18.0D * direction)).add(0.0D, 16.0D, 0.0D);
        }
    }

    private void flyToward(Vec3 target, double speed) {
        if (target == null) return;
        Vec3 delta = target.subtract(this.position());
        if (delta.lengthSqr() < 0.01D) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.72D));
            return;
        }
        Vec3 desired = delta.normalize().scale(speed);
        double steering = getCombatPhase() == 3 ? 0.24D : 0.17D;
        Vec3 motion = this.getDeltaMovement().scale(1.0D - steering).add(desired.scale(steering));
        if (motion.length() > speed) motion = motion.normalize().scale(speed);
        this.setDeltaMovement(motion);
        faceMovement(motion);
    }

    private void hoverNear(Vec3 target) {
        Vec3 delta = target.subtract(this.position());
        if (delta.length() > 3.5D) flyToward(target, 0.44D);
        else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.58D));
            facePoint(target);
        }
    }

    private void faceMovement(Vec3 motion) {
        if (motion.horizontalDistanceSqr() < 1.0E-5D) return;
        float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
        this.setYRot(Mth.rotLerp(0.24F, this.getYRot(), yaw));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        this.setXRot(Mth.rotLerp(0.20F, this.getXRot(), Mth.clamp(pitch, -35.0F, 35.0F)));
    }

    private void facePoint(Vec3 target) {
        faceMovement(target.subtract(this.getEyePosition()));
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.96D));
        } else {
            super.travel(travelVector);
        }
    }

    private Vec3 getMouthPosition() {
        Vec3 forward = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        return this.position().add(0.0D, 2.3D, 0.0D).add(forward.scale(4.5D));
    }

    private void fireAmethystBreath(ServerLevel level, LivingEntity target) {
        Vec3 origin = getMouthPosition();
        Vec3 direction = target.getEyePosition().subtract(origin).normalize();
        DragonFireball fireball = new DragonFireball(level, this, direction);
        fireball.setPos(origin.x, origin.y, origin.z);
        fireball.addTag("arlightbosses_amethyst_breath_projectile");
        level.addFreshEntity(fireball);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT,
                SoundSource.HOSTILE, 2.0F, 0.78F);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, 1.6F, 0.58F);
        sendLineParticles(level, origin, target.getEyePosition(), MAGENTA_DUST, 1.4D);
    }

    private void configureBreathClouds(ServerLevel level) {
        if (this.tickCount % 5 != 0) return;
        AABB search = this.getBoundingBox().inflate(128.0D);
        for (AreaEffectCloud cloud : level.getEntitiesOfClass(AreaEffectCloud.class, search)) {
            if (cloud.getOwner() != this || cloud.getTags().contains("arlightbosses_amethyst_cloud")) continue;
            cloud.addTag("arlightbosses_amethyst_cloud");
            cloud.setDuration(200);
            cloud.setRadius(3.6F);
            cloud.setRadiusPerTick(-0.004F);
            cloud.setWaitTime(0);
            cloud.setParticle(AMETHYST_BLOCK_PARTICLE);
            cloud.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
            level.playSound(null, cloud.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.HOSTILE, 1.4F, 0.70F);
        }
    }

    private void summonCorruptedEndermen(ServerLevel level, LivingEntity target, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count + this.random.nextDouble() * 0.4D;
            int x = Mth.floor(target.getX() + Math.cos(angle) * 7.0D);
            int z = Mth.floor(target.getZ() + Math.sin(angle) * 7.0D);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
            VoidEndermanMinion minion = BossEntities.VOID_ENDERMAN_MINION.get().spawn(
                    level, new BlockPos(x, y, z), MobSpawnType.EVENT);
            if (minion == null) continue;
            minion.addTag("arlightbosses_dragon_summon");
            minion.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 90, 0));
            minion.setTarget(target);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 1.8F, 0.82F);
        level.sendParticles(ParticleTypes.PORTAL,
                this.getX(), this.getY(), this.getZ(), 100, 7.0D, 2.0D, 7.0D, 0.22D);
    }

    private void startCrystalBeam(ServerLevel level, LivingEntity target) {
        this.beamTicks = 60;
        this.beamTarget = target.getUUID();
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 2.0F, 1.25F);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 2.0F, 1.15F);
    }

    private void tickCrystalBeam(ServerLevel level) {
        this.beamTicks--;
        Entity entity = this.beamTarget == null ? null : level.getEntity(this.beamTarget);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            this.beamTicks = 0;
            this.beamTarget = null;
            return;
        }
        facePoint(target.getEyePosition());
        Vec3 start = getMouthPosition();
        Vec3 end = target.getEyePosition();
        sendLineParticles(level, start, end, MAGENTA_DUST, 0.52D);
        sendLineParticles(level, start, end, ParticleTypes.ELECTRIC_SPARK, 1.0D);
        if (this.beamTicks % 5 == 0) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || !player.isAlive()) continue;
                if (distanceToSegment(player.getEyePosition(), start, end) <= 1.35D) {
                    player.hurt(level.damageSources().magic(), 3.0F);
                }
            }
        }
        if (this.beamTicks % 10 == 0) {
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 1.2F, 1.45F);
        }
        if (this.beamTicks <= 0) this.beamTarget = null;
    }

    private void startShardRain(ServerLevel level, LivingEntity target) {
        this.rainTicks = 64;
        this.rainCenter = target.blockPosition();
        level.playSound(null, this.rainCenter, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 2.2F, 0.76F);
        level.sendParticles(MAGENTA_DUST,
                rainCenter.getX() + 0.5D, rainCenter.getY() + 0.2D, rainCenter.getZ() + 0.5D,
                120, 7.5D, 0.2D, 7.5D, 0.02D);
    }

    private void tickShardRain(ServerLevel level) {
        this.rainTicks--;
        if (rainCenter == null) {
            rainTicks = 0;
            return;
        }
        if (rainTicks % 3 == 0) {
            for (int i = 0; i < 3; i++) {
                int x = rainCenter.getX() + random.nextInt(15) - 7;
                int z = rainCenter.getZ() + random.nextInt(15) - 7;
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int spawnY = Math.min(level.getMaxBuildHeight() - 3, groundY + 18 + random.nextInt(8));
                BlockPos source = new BlockPos(x, spawnY, z);
                if (!level.getBlockState(source).isAir()) continue;
                BlockState shardState = random.nextBoolean()
                        ? Blocks.AMETHYST_CLUSTER.defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, Direction.DOWN)
                            .setValue(AmethystClusterBlock.WATERLOGGED, false)
                        : Blocks.AMETHYST_BLOCK.defaultBlockState();
                level.setBlock(source, shardState, 3);
                FallingBlockEntity shard = FallingBlockEntity.fall(level, source, shardState);
                shard.disableDrop();
                shard.setHurtsEntities(1.35F, 12);
                shard.addTag("arlightbosses_amethyst_shard_rain");
                shard.setDeltaMovement((random.nextDouble() - 0.5D) * 0.08D, -0.20D,
                        (random.nextDouble() - 0.5D) * 0.08D);
            }
        }
        if (rainTicks % 8 == 0) {
            level.playSound(null, rainCenter, SoundEvents.AMETHYST_CLUSTER_HIT,
                    SoundSource.HOSTILE, 1.3F, 0.72F + random.nextFloat() * 0.25F);
        }
        if (rainTicks <= 0) rainCenter = null;
    }

    private void enterPhaseTwo(ServerLevel level) {
        setCombatPhase(2);
        setGroundedMode();
        this.setHealth(Math.min(MAX_BOSS_HEALTH, this.getHealth() + 100.0F));
        if (this.getAttribute(Attributes.ARMOR) != null)
            this.getAttribute(Attributes.ARMOR).setBaseValue(26.0D);
        if (this.getAttribute(Attributes.ARMOR_TOUGHNESS) != null)
            this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(10.0D);
        this.flightWaypoint = null;
        this.flightPatternTicks = 0;
        this.beamCooldown = 55;
        this.rainCooldown = 135;
        this.frenzyCooldown = 20;
        playAncestralVoice(level, "§5[Dragón ancestral] §d¡Pagarás por esto!");
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                this.getX(), this.getY() + 2.0D, this.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                this.getX(), this.getY() + 2.5D, this.getZ(), 280, 5.8D, 3.7D, 5.8D, 0.19D);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > 28.0D * 28.0D) continue;
            Vec3 away = player.position().subtract(this.position()).normalize().scale(2.1D);
            player.push(away.x, 0.85D, away.z);
            player.hurtMarked = true;
        }
    }

    private void enterPhaseThree(ServerLevel level) {
        setCombatPhase(3);
        setCocooned(true);
        this.cocoonTicks = COCOON_DURATION_TICKS;
        this.cocoonAnchor = this.position();
        this.setHealth(MAX_BOSS_HEALTH);
        this.setDeltaMovement(Vec3.ZERO);
        this.flightWaypoint = null;
        this.beamTicks = 0;
        this.rainTicks = 0;
        if (this.getAttribute(Attributes.ARMOR) != null)
            this.getAttribute(Attributes.ARMOR).setBaseValue(20.0D);
        if (this.getAttribute(Attributes.ARMOR_TOUGHNESS) != null)
            this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(7.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 4.0F, 0.42F);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 2.8F, 0.55F);
        level.sendParticles(WHITE_VIOLET_DUST,
                this.getX(), this.getY() + 2.0D, this.getZ(), 420, 7.0D, 5.0D, 7.0D, 0.28D);
        broadcastDialogue(level, "§5[Dragón ancestral] §dLa amatista no muere... renace.");
    }

    private void tickCocoon(ServerLevel level) {
        if (cocoonAnchor == null) cocoonAnchor = this.position();
        this.setPos(cocoonAnchor.x, cocoonAnchor.y, cocoonAnchor.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setYRot(this.yRotO);
        this.setXRot(0.0F);
        if (cocoonTicks > 0) cocoonTicks--;

        double progress = 1.0D - cocoonTicks / (double) COCOON_DURATION_TICKS;
        int particles = 18 + (int) (progress * 28.0D);
        level.sendParticles(progress > 0.72D ? WHITE_VIOLET_DUST : MAGENTA_DUST,
                this.getX(), this.getY() + 2.0D, this.getZ(), particles,
                3.2D + progress * 2.2D, 2.8D + progress * 1.8D, 3.2D + progress * 2.2D, 0.08D);
        if (cocoonTicks % 20 == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 2.0F, 0.48F + (float) progress * 0.75F);
        }
        if (cocoonTicks > 0) return;

        setCocooned(false);
        setAirborneMode();
        this.sonicCooldown = 70;
        this.frenzyCooldown = 18;
        this.flightWaypoint = Vec3.atCenterOf(this.arenaCenter).add(0.0D, 8.5D, 0.0D);
        buildResonantField(level);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 4.0F, 0.48F);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 4.0F, 0.62F);
        level.sendParticles(WHITE_VIOLET_DUST,
                this.getX(), this.getY() + 2.0D, this.getZ(), 520, 8.0D, 5.0D, 8.0D, 0.32D);
        level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                this.getX(), this.getY() + 2.0D, this.getZ(), 360, 6.0D, 4.0D, 6.0D, 0.24D);
    }

    private void buildResonantField(ServerLevel level) {
        if (!resonantBuds.isEmpty()) return;
        for (int attempt = 0; attempt < 170 && resonantBuds.size() < 82; attempt++) {
            int dx = random.nextInt(41) - 20;
            int dz = random.nextInt(41) - 20;
            if (dx * dx + dz * dz > 400) continue;
            int x = arenaCenter.getX() + dx;
            int z = arenaCenter.getZ() + dz;
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos budPos = new BlockPos(x, groundY + 1, z);
            if (!level.getBlockState(budPos).isAir()) continue;
            if (!level.getBlockState(budPos.below()).isSolidRender(level, budPos.below())) continue;
            BlockState bud = Blocks.SMALL_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.UP)
                    .setValue(AmethystClusterBlock.WATERLOGGED, false);
            if (bud.canSurvive(level, budPos)) {
                level.setBlock(budPos, bud, 3);
                resonantBuds.add(budPos.immutable());
            }
        }
    }

    private void applyResonantField(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(arenaCenter).add(0.0D, 1.0D, 0.0D);
        if (tickCount % 5 == 0) {
            level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                    center.x, center.y, center.z, 35, 10.0D, 0.2D, 10.0D, 0.05D);
            level.sendParticles(MAGENTA_DUST,
                    center.x, center.y + 0.4D, center.z, 24, 12.0D, 0.5D, 12.0D, 0.04D);
        }
        if (tickCount % 10 != 0) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(center) > 20.0D * 20.0D) continue;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 35, 0, false, true));
        }
    }

    private void sonicShatter(ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(arenaCenter).add(0.0D, 2.0D, 0.0D);
        level.playSound(null, arenaCenter, SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 3.0F, 1.25F);
        level.playSound(null, arenaCenter, SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 3.0F, 0.52F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(WHITE_VIOLET_DUST,
                center.x, center.y, center.z, 240, 10.0D, 3.0D, 10.0D, 0.24D);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(center) > 24.0D * 24.0D) continue;
            Vec3 away = player.position().subtract(center).normalize().scale(1.35D);
            player.hurt(level.damageSources().sonicBoom(this), 9.0F);
            player.push(away.x, 1.20D, away.z);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 32, 1));
            if (player.isBlocking()) {
                player.getUseItem().hurtAndBreak(18, level, player, ignored -> {});
                player.disableShield();
            }
        }
    }

    private void hitNearbyPlayers(ServerLevel level) {
        if (collisionCooldown > 0 || this.getDeltaMovement().lengthSqr() < 0.12D) return;
        AABB attackBox = this.getBoundingBox().inflate(1.3D);
        boolean hit = false;
        for (Player player : level.getEntitiesOfClass(Player.class, attackBox,
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            player.hurt(level.damageSources().mobAttack(this), getCombatPhase() == 3 ? 13.0F : 9.0F);
            Vec3 away = player.position().subtract(this.position()).normalize().scale(1.2D);
            player.push(away.x, 0.55D, away.z);
            player.hurtMarked = true;
            hit = true;
        }
        if (hit) collisionCooldown = 12;
    }

    private void tickAura(ServerLevel level) {
        int phase = getCombatPhase();
        if (tickCount % 2 == 0) {
            level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                    getX() + (random.nextDouble() - 0.5D) * 8.0D,
                    getY() + 1.0D + random.nextDouble() * 5.0D,
                    getZ() + (random.nextDouble() - 0.5D) * 8.0D,
                    3, 0.25D, 0.25D, 0.25D, 0.06D);
            level.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 2.0D, getZ(), 3 + phase, 4.0D, 2.5D, 4.0D, 0.12D);
        }
        if (tickCount % 8 == 0) {
            level.sendParticles(phase == 3 ? WHITE_VIOLET_DUST : MAGENTA_DUST,
                    getX(), getY() + 2.5D, getZ(), 10 + phase * 4, 4.5D, 2.8D, 4.5D, 0.10D);
        }
        if (tickCount % 20 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + 2.5D, getZ(), phase == 3 ? 30 : 12, 4.5D, 3.0D, 4.5D, 0.22D);
        }
        if (this.getDeltaMovement().horizontalDistance() > 0.16D
                && tickCount - lastCrystalFlapSound >= 12) {
            lastCrystalFlapSound = tickCount;
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 0.85F, 0.72F + random.nextFloat() * 0.22F);
        }
    }

    private void updateBossBar() {
        int phase = getCombatPhase();
        String phaseName = phase == 1 ? "Resonancia de Amatista"
                : phase == 2 ? "Furia Geoda — Terrestre"
                : isCocooned() ? "Renacimiento en Capullo" : "Sobrecarga de Geoda";
        bossEvent.setName(Component.literal("§d✦ Dragón Corrupto de Amatista §5• Fase "
                + phase + " §7— §f" + phaseName));
        bossEvent.setProgress(Mth.clamp(this.getHealth() / MAX_BOSS_HEALTH, 0.0F, 1.0F));
        bossEvent.setColor(phase == 3 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.PURPLE);
        bossEvent.setVisible(true);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCocooned()) {
            if (level() instanceof ServerLevel level && reflectSoundCooldown <= 0) {
                reflectSoundCooldown = 5;
                level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                        SoundSource.HOSTILE, 2.0F, 0.55F);
                level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                        getX(), getY() + 2.0D, getZ(), 28, 2.8D, 2.4D, 2.8D, 0.16D);
            }
            return false;
        }
        if (getCombatPhase() == 2 && source.is(DamageTypeTags.IS_PROJECTILE)) {
            reflectProjectile(source.getDirectEntity());
            return false;
        }
        if (getCombatPhase() == 2) amount *= 0.62F;
        return super.hurt(source, amount);
    }

    private void reflectProjectile(Entity direct) {
        if (direct instanceof Projectile projectile && !projectile.isRemoved()) {
            projectile.setOwner(this);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.35D));
            projectile.setPos(projectile.getX(), projectile.getY() + 0.15D, projectile.getZ());
        }
        if (!(level() instanceof ServerLevel level)) return;
        if (reflectSoundCooldown <= 0) {
            reflectSoundCooldown = 5;
            level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.HOSTILE, 1.6F, 1.35F);
        }
        Vec3 impact = direct == null ? getMouthPosition() : direct.position();
        level.sendParticles(AMETHYST_CLUSTER_PARTICLE,
                impact.x, impact.y, impact.z, 35, 0.8D, 0.8D, 0.8D, 0.18D);
    }

    private LivingEntity findTarget(ServerLevel level, double range) {
        ServerPlayer best = null;
        double bestDistance = range * range;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            double distance = player.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private void cleanupExpiredFallingCrystals(ServerLevel level) {
        if (tickCount % 40 != 0) return;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof FallingBlockEntity falling)) continue;
            if (!falling.getTags().contains("arlightbosses_amethyst_shard_rain")) continue;
            if (falling.time > 100 || falling.distanceToSqr(this) > 220.0D * 220.0D) falling.discard();
        }
    }

    private void clearResonantField(ServerLevel level) {
        for (BlockPos pos : resonantBuds) {
            BlockState current = level.getBlockState(pos);
            if (current.is(Blocks.SMALL_AMETHYST_BUD) || current.is(Blocks.MEDIUM_AMETHYST_BUD)
                    || current.is(Blocks.LARGE_AMETHYST_BUD) || current.is(Blocks.AMETHYST_CLUSTER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        resonantBuds.clear();
    }

    private void cleanupTemporaryEntities(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getTags().contains("arlightbosses_amethyst_shard_rain")
                    || entity.getTags().contains("arlightbosses_amethyst_cloud")) {
                entity.discard();
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossEvent.removeAllPlayers();
        this.setCocooned(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel level) {
            clearResonantField(level);
            cleanupTemporaryEntities(level);
            level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH,
                    SoundSource.HOSTILE, 4.0F, 0.52F);
            level.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.HOSTILE, 4.0F, 0.48F);
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.dragonDeathTime = Mth.clamp((int) (200.0F * this.deathTime / DEATH_FADE_TICKS), 0, 200);
        this.setDeltaMovement(0.0D, 0.018D, 0.0D);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (level() instanceof ServerLevel level) {
            float fade = getDeathFadeProgress(0.0F);
            int count = 10 + (int) (fade * 36.0F);
            level.sendParticles(fade > 0.55F ? WHITE_VIOLET_DUST : MAGENTA_DUST,
                    getX(), getY() + 2.0D, getZ(), count,
                    4.0D + fade * 3.0D, 3.0D + fade * 2.0D, 4.0D + fade * 3.0D, 0.12D);
            if (deathTime % 12 == 0) {
                level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.HOSTILE, 1.4F, 0.55F + fade * 0.85F);
            }
            if (deathTime >= DEATH_FADE_TICKS) {
                placeDragonEgg(level);
                this.remove(Entity.RemovalReason.KILLED);
            }
        } else if (deathTime >= DEATH_FADE_TICKS) {
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    private void placeDragonEgg(ServerLevel level) {
        if (deathEggPlaced) return;
        deathEggPlaced = true;
        // Durante una partida de Bingo, el huevo forma parte de la cinemática de Somita.
        // El plugin lo coloca y lo retira en el momento correcto para evitar duplicados.
        if (getTags().contains("arlightbingo_boss_dragon")) return;
        int baseX = Mth.floor(this.getX());
        int baseZ = Mth.floor(this.getZ());
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);
        BlockPos preferred = new BlockPos(baseX, baseY, baseZ);
        BlockPos placement = findEggPlacement(level, preferred);
        if (placement != null) {
            level.setBlock(placement, Blocks.DRAGON_EGG.defaultBlockState(), 3);
            level.playSound(null, placement, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 2.5F, 0.62F);
            level.sendParticles(WHITE_VIOLET_DUST,
                    placement.getX() + 0.5D, placement.getY() + 0.7D, placement.getZ() + 0.5D,
                    180, 2.0D, 1.5D, 2.0D, 0.20D);
        }
    }

    private BlockPos findEggPlacement(ServerLevel level, BlockPos preferred) {
        for (int radius = 0; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = preferred.offset(dx, 0, dz);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            candidate.getX(), candidate.getZ());
                    candidate = new BlockPos(candidate.getX(), y, candidate.getZ());
                    if (!level.getBlockState(candidate).canBeReplaced()) continue;
                    if (!level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())) continue;
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public void checkDespawn() {
        // Nunca desaparece por distancia.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_PHASE, getCombatPhase());
        if (arenaCenter != null) {
            tag.putInt(NBT_CENTER_X, arenaCenter.getX());
            tag.putInt(NBT_CENTER_Y, arenaCenter.getY());
            tag.putInt(NBT_CENTER_Z, arenaCenter.getZ());
        }
        tag.putInt("DragonDeathTime", dragonDeathTime);
        tag.putInt(NBT_COCOON_TICKS, cocoonTicks);
        tag.putBoolean(NBT_DEATH_EGG_PLACED, deathEggPlaced);
        if (cocoonAnchor != null) {
            tag.putDouble(NBT_COCOON_X, cocoonAnchor.x);
            tag.putDouble(NBT_COCOON_Y, cocoonAnchor.y);
            tag.putDouble(NBT_COCOON_Z, cocoonAnchor.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_PHASE)) setCombatPhase(tag.getInt(NBT_PHASE));
        if (tag.contains(NBT_CENTER_X) && tag.contains(NBT_CENTER_Y) && tag.contains(NBT_CENTER_Z)) {
            arenaCenter = new BlockPos(tag.getInt(NBT_CENTER_X), tag.getInt(NBT_CENTER_Y), tag.getInt(NBT_CENTER_Z));
        }
        dragonDeathTime = tag.getInt("DragonDeathTime");
        cocoonTicks = tag.getInt(NBT_COCOON_TICKS);
        deathEggPlaced = tag.getBoolean(NBT_DEATH_EGG_PLACED);
        if (tag.contains(NBT_COCOON_X) && tag.contains(NBT_COCOON_Y) && tag.contains(NBT_COCOON_Z)) {
            cocoonAnchor = new Vec3(tag.getDouble(NBT_COCOON_X), tag.getDouble(NBT_COCOON_Y), tag.getDouble(NBT_COCOON_Z));
        }
        setCocooned(cocoonTicks > 0);
        if (getCombatPhase() == 2) setGroundedMode();
        else setAirborneMode();
        updateBossBar();
    }

    private void playAncestralVoice(ServerLevel level, String dialogue) {
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 4.0F, 0.38F);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 2.5F, 0.48F);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 3.0F, 0.42F);
        broadcastDialogue(level, dialogue);
    }

    private void broadcastDialogue(ServerLevel level, String dialogue) {
        Component message = Component.literal(dialogue);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) <= 192.0D * 192.0D) {
                player.sendSystemMessage(message);
                player.displayClientMessage(message, true);
            }
        }
    }

    private static void sendLineParticles(
            ServerLevel level,
            Vec3 start,
            Vec3 end,
            net.minecraft.core.particles.ParticleOptions particle,
            double spacing
    ) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.001D) return;
        Vec3 step = delta.normalize().scale(spacing);
        int count = Math.min(320, Math.max(1, (int) Math.ceil(length / spacing)));
        Vec3 cursor = start;
        for (int i = 0; i <= count; i++) {
            level.sendParticles(particle, cursor.x, cursor.y, cursor.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            cursor = cursor.add(step);
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared <= 1.0E-7D) return point.distanceTo(start);
        double t = Mth.clamp(point.subtract(start).dot(segment) / lengthSquared, 0.0D, 1.0D);
        return point.distanceTo(start.add(segment.scale(t)));
    }
}
