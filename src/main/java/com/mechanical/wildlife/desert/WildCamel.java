package com.mechanical.wildlife.desert;

import com.mechanical.wildlife.warm.Onager;
import com.mechanical.wildlife.warm.TameOnager;

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
 * Wild Camel (dromedary): a tall, long-legged, one-humped camel in small herds on the desert and the gypsum flats. Less
 * skittish than the onager but just as wary. Tamed exactly like the onager ({@link Onager}: crouch with horse feed, feed by
 * hand, Rope Halter at trust 3, tame at 7) into a {@link TameCamel}: the desert's pack animal, an alternative to the onager
 * that goes far without water. Butchered: camel meat, a big hide, hump fat and sinew.
 */
public class WildCamel extends Onager {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/camel.png");

    public WildCamel(EntityType<? extends WildCamel> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected EntityType<? extends TameOnager> tameType() {
        return DesertFauna.TAME_CAMEL.get();
    }

    @Override
    protected String messageKey() {
        return "camel";
    }

    @Override
    public Identifier texture() {
        return TEXTURE; // calves: the same coat, the model drawn smaller
    }

    @Override
    public double fearMultiplier() {
        return 1.2;
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CAMEL_STEP, 0.3F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return isBaby() ? 1.4F : 1.0F;
    }
}
