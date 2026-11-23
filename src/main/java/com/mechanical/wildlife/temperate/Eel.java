package com.mechanical.wildlife.temperate;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * European eel: a long, slimy, olive-brown fish of rivers, fens and bogs. By day it lies still in the mud at the bottom;
 * at night it comes out and winds about hunting ({@link #isNightActive}). That is when the Fish Trap takes eels too.
 * Dies as a Whole Eel (gutted with a knife like any whole fish).
 */
public class Eel extends AbstractFish {
    public Eel(EntityType<? extends Eel> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 6.0);
    }

    /** Out and about: at night. */
    boolean isNightActive() {
        return level().isDarkOutside();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PanicGoal(this, 1.6));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, p -> !p.isCrouching(), 5.0F, 1.3, 1.6, EntitySelector.NO_CREATIVE_OR_SPECTATOR));
        // at night it roams; by day it hardly moves from its spot on the bottom
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.9, 40) {
            @Override
            public boolean canUse() {
                return isNightActive() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.4, 600));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }
}
