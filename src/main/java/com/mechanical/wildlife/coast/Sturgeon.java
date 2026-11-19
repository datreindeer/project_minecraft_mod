package com.mechanical.wildlife.coast;

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
 * Sturgeon: a rare, huge, armoured fish of the big rivers. Cruises slowly along the bottom feeling for food with its
 * barbels and now and then leaps clear of the water. Slow to scare, slow to flee, hard to kill. Too big to come out of
 * the water as an item: it leaves a Carcass (Butchery) with lots of meat, the roe and the swim bladder (Isinglass).
 */
public class Sturgeon extends AbstractFish {
    public Sturgeon(EntityType<? extends Sturgeon> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PanicGoal(this, 1.4));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 1.0, 1.3, EntitySelector.NO_CREATIVE_OR_SPECTATOR));
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.7, 60));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // the famous leap: rarely, near the surface, it throws itself out of the water
        if (!level().isClientSide() && isInWater() && !isUnderWater() && random.nextInt(1500) == 0) {
            setDeltaMovement(getDeltaMovement().add(getLookAngle().scale(0.3)).add(0.0, 0.7, 0.0));
            playSound(SoundEvents.GENERIC_SPLASH, 1.0F, 0.7F);
        }
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
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
        return SoundEvents.COD_AMBIENT;
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

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }
}
