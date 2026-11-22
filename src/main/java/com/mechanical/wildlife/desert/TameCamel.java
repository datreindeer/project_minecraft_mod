package com.mechanical.wildlife.desert;

import com.mechanical.wildlife.warm.TameOnager;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * A tamed {@link WildCamel}: the tame onager's pack animal (ridden bareback, Pack Baskets, the Cart; a vanilla donkey
 * underneath) with the camel's looks, stronger and slower. Made for the desert: walking drains its stamina at half the
 * horse's rate (HorseWork.CAMEL_DRAIN) and a drink lasts it four times as long (Husbandry.CAMEL_WATER). Two tame camels
 * breed a tame calf.
 */
public class TameCamel extends TameOnager {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/camel.png");

    public TameCamel(EntityType<? extends TameCamel> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseChestedHorseAttributes().add(Attributes.MAX_HEALTH, 31.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.JUMP_STRENGTH, 0.45);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    /** Strong and steady: 28-34 health, speed 0.17-0.22, jump 0.4-0.5. */
    @Override
    public void randomizeAttributes(RandomSource random) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(28 + random.nextInt(7));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.17 + random.nextDouble() * 0.05);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.4 + random.nextDouble() * 0.1);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return isBaby() ? 1.4F : 1.0F;
    }
}
