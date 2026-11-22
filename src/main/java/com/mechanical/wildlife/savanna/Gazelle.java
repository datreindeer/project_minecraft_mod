package com.mechanical.wildlife.savanna;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gazelle: a small, fast, very skittish antelope of the savanna in herds of 4-8, sandy with a dark flank stripe and a
 * white belly; both sexes carry ringed lyre-shaped horns. It notices people twice as far off as a deer, and when the
 * herd bolts each one now and then springs high into the air (stotting), which sets the rest off too.
 */
public class Gazelle extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/gazelle.png");

    public Gazelle(EntityType<? extends Gazelle> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 2.0;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // stotting: a stiff-legged leap straight up while fleeing
        if (!level().isClientSide() && onGround() && AnimalFear.isPanicking(this) && random.nextInt(30) == 0) {
            setDeltaMovement(getDeltaMovement().add(0, 0.55, 0));
            needsSync = true;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // silent grazers; a snort when hurt
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.1F, 1.4F);
    }
}
