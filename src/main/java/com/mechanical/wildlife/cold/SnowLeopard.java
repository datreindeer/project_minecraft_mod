package com.mechanical.wildlife.cold;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.CaveLion;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.Mouflon;
import com.mechanical.wildlife.mountain.Chamois;
import com.mechanical.wildlife.mountain.Ibex;
import com.mechanical.wildlife.mountain.Marmot;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Snow leopard: the grey ghost of the highest peaks. Rare and shy - by day it keeps well away from people (twice as far as
 * other game) - but it hunts at any hour: ibex above all, chamois, mouflon, marmots and hares, the cave lion's way
 * (creeps up crouched, freezes when watched, pounces). Only at night is a player quarry. Lands on its feet from
 * {@link #SAFE_FALL} blocks: it drops down cliffs onto its prey.
 */
public class SnowLeopard extends CaveLion {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/snow_leopard.png");
    private static final float SAFE_FALL = 12.0F;

    public SnowLeopard(EntityType<? extends SnowLeopard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof Ibex || e instanceof Chamois || e instanceof Mouflon || e instanceof Marmot || e instanceof Hare;
    }

    /** Prey at any hour; people only at night. */
    @Override
    protected boolean huntingTime() {
        return true;
    }

    @Override
    protected boolean canHunt(LivingEntity e) {
        return (!(e instanceof Player) || level().isDarkOutside()) && super.canHunt(e);
    }

    @Override
    public boolean fearsPlayers() {
        return retreating() || level().isBrightOutside();
    }

    /** It rests in the open on a ledge, but never deep enough to be walked up on. */
    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    public double fearMultiplier() {
        return 2.0;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return super.causeFallDamage(Math.max(0, fallDistance - SAFE_FALL), damageModifier, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() || isCrouching() ? null : SoundEvents.OCELOT_AMBIENT; // the soft chuffing call
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.OCELOT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
