package com.mechanical.wildlife.deep;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowFlockLeaderGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Blind Cave Fish: small, pale pink, eyeless fish in schools of 3-5 in the dark cave lakes. Blind, so a player doesn't
 * scare them until he hits one; they are caught on the hook in cave water (fishing loot: under the open sky they never
 * bite) or simply struck. Dies as a Blind Cave Fish, eaten raw or cooked. Not luminous.
 */
public class BlindCaveFish extends AbstractSchoolingFish {
    public BlindCaveFish(EntityType<? extends BlindCaveFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 3.0);
    }

    @Override
    protected void registerGoals() {
        // blind: no fleeing from players they can't see (the vanilla fish goal), only from blows
        goalSelector.addGoal(0, new PanicGoal(this, 1.25));
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 40) {
            @Override
            public boolean canUse() {
                return canRandomSwim() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new FollowFlockLeaderGoal(this));
    }

    @Override
    public int getMaxSchoolSize() {
        return 5;
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
}
