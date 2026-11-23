package com.mechanical.wildlife.cold;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.CaveLion;
import com.mechanical.wildlife.Moose;
import com.mechanical.wildlife.RedDeer;
import com.mechanical.wildlife.Reindeer;
import com.mechanical.wildlife.RoeDeer;
import com.mechanical.wildlife.WildBoar;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Siberian tiger: the taiga's apex hunter, rare and only in snowy taiga. The cave lion's way of hunting (creeps up
 * crouched, freezes when watched, pounces, drags the kill off, fears fire), but bigger, heavier and alone: red deer,
 * reindeer, wild boar, roe deer and moose calves, and people. It hunts at dusk and by night and lies up by day.
 */
public class SiberianTiger extends CaveLion {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/siberian_tiger.png");

    public SiberianTiger(EntityType<? extends SiberianTiger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return !e.isBaby() && (e instanceof RedDeer || e instanceof Reindeer || e instanceof WildBoar || e instanceof RoeDeer)
                || e instanceof Moose && e.isBaby() || e instanceof com.mechanical.people.Tribesperson;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 1.6;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.9F;
    }
}
