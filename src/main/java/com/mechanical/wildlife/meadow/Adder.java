package com.mechanical.wildlife.meadow;

import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Adder (common European viper): thick-bodied, grey (male) or brown (female) with a black zigzag down the back and a
 * dark V on the head, copper-red eyes. On heaths, meadows and sunny rocks. It doesn't run from you: it trusts its
 * colouring, basks coiled in the sun and lies low in the grass, and strikes whoever walks right up to it without
 * watching their step (sneaking past is safe). The bite poisons; Snakeskin Boots stop it. After a strike, or when hurt,
 * it slips away. Gives snakeskin, snake meat and a Venom Gland.
 */
public class Adder extends Snake {
    private static final Identifier MALE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/adder.png");
    private static final Identifier FEMALE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/adder_female.png");
    /** Strikes at a player who comes this close, not sneaking. */
    private static final double STRIKE_RANGE = 1.6;
    /** The venom: Poison I for 6 s. */
    private static final int VENOM_TICKS = 120;

    public Adder(EntityType<? extends Adder> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    /** Females are brown; from the UUID so the carcass keeps the look. */
    public boolean isFemale() {
        return (getUUID().getLeastSignificantBits() & 1) == 0;
    }

    /** Snakeskin over the boots: the fangs don't get through. */
    public static boolean bootsProtect(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.FEET).is(MeadowFauna.SNAKESKIN_BOOTS.get());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.8, false));
        targetSelector.addGoal(2, new SteppedOnGoal(this));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        playSound(hiss(), 1.0F, 1.5F);
        boolean bit;
        if (target instanceof LivingEntity living && bootsProtect(living)) {
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.HOSTILE, 1.0F, 1.4F);
            living.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(1, living, EquipmentSlot.FEET);
            bit = false;
        } else {
            bit = super.doHurtTarget(level, target);
            if (bit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, 0), this);
        }
        // one strike, then away into the grass
        setTarget(null);
        AnimalFear.panic(this, target.position());
        return bit;
    }

    @Override
    public Identifier texture() {
        return isFemale() ? FEMALE : MALE;
    }

    @Override
    public double fearMultiplier() {
        return 0.0; // stays put and relies on its camouflage
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // a warning hiss when someone is close
        Player near = level().getNearestPlayer(this, 4);
        return near != null && !near.isSpectator() ? hiss() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 2.0F;
    }

    /** A player walking (not sneaking) right up to it is struck. */
    static class SteppedOnGoal extends TargetGoal {
        private final Adder adder;

        SteppedOnGoal(Adder adder) {
            super(adder, false);
            this.adder = adder;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (adder.getTarget() != null || AnimalFear.isPanicking(adder)) return false;
            Player p = adder.level().getNearestPlayer(adder, STRIKE_RANGE);
            if (p == null || p.isCreative() || p.isSpectator() || p.isSteppingCarefully()) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            adder.setTarget(targetMob);
            adder.setBasking(false);
            super.start();
        }
    }
}
