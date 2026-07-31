package com.arlight.bosses.entity.minion;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Strider corrompido por vetas de magma dorado. Inmune al fuego y a la lava (no se
 * quema ni se hunde despacio), y se impulsa con un empujón extra cuando está parado
 * sobre lava para cruzarla con confianza. Pensado como montura veloz: un
 * {@link GildedPiglinMinion} cercano puede montarlo (misma lógica que con la cría de
 * ravager) y hostigar a distancia mientras el strider lo mueve rápido de un lado a otro.
 *
 * Nota técnica: esto NO replica la navegación real de "caminar sobre la superficie de
 * la lava" del Strider vainilla (esa parte usa un pathfinder específico bastante interno
 * del juego) — lo que sí tiene es inmunidad total a fuego/lava y un impulso de velocidad
 * mientras está parado en ella, que en la práctica se siente rápido y confiado cruzando
 * lava, aunque técnicamsente sigue caminando por el fondo en vez de flotar en la superficie.
 */
public final class MoltenStriderMinion extends CorruptedMinionEntity {
    private static final double LAVA_THRUST_STRENGTH = 0.045D;

    public MoltenStriderMinion(EntityType<? extends CorruptedMinionEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String animationPrefix() { return "animation.molten_strider"; }
    @Override protected ParticleOptions corruptionParticle() { return ParticleTypes.FLAME; }
    @Override protected double defaultAttackReach() { return 1.8D; }

    /** Rápido y con vida moderada: es una montura hostigadora, no un tanque. */
    public static AttributeSupplier.Builder createStriderAttributes() {
        return CorruptedMinionEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isInLava()) {
            Vec3 look = getLookAngle();
            setDeltaMovement(getDeltaMovement().add(look.x * LAVA_THRUST_STRENGTH, 0.0D, look.z * LAVA_THRUST_STRENGTH));
            if (tickCount % 60 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.STRIDER_HAPPY,
                        SoundSource.HOSTILE, 0.8F, 1.0F + random.nextFloat() * 0.1F);
            }
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.STRIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.STRIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.STRIDER_DEATH; }
}
