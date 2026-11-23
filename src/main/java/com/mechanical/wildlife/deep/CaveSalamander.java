package com.mechanical.wildlife.deep;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cave Salamander (olm): a pale, eyeless, finger-long salamander of the dark cave lakes, pink-white with feathery red gills.
 * Wholly aquatic: it creeps along the bottom and drifts slowly; out of the water it dries out like a fish. It is blind,
 * so neither light nor a sneaking player bothers it; it feels the water move instead: anything splashing about within
 * {@link #FEEL_RANGE} blocks in the same water sends it darting off. Dies as a whole Cave Salamander (no carcass), the
 * key to the Dark-Sight Broth. Not luminous: in a pitch-black cave it is as dark as everything else.
 */
public class CaveSalamander extends AbstractFish {
    static final double FEEL_RANGE = 3;

    public CaveSalamander(EntityType<? extends CaveSalamander> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        // not the fish defaults (no fleeing from players it can't see): panics when hurt, slow drifting otherwise
        goalSelector.addGoal(0, new PanicGoal(this, 1.8));
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.5, 120));
    }

    /** Something moving in the water nearby (a swimmer, or anything in it that isn't creeping): it darts away from it. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !isInWater() || tickCount % 10 != 0) return;
        for (var e : level().getEntities(this, getBoundingBox().inflate(FEEL_RANGE), e -> e.isInWater() && !(e instanceof CaveSalamander)
                && e.getDeltaMovement().horizontalDistanceSqr() > 0.0004 && !e.isSpectator())) {
            Vec3 away = position().subtract(e.position()).normalize().scale(0.25);
            setDeltaMovement(getDeltaMovement().add(away.x, 0.02, away.z));
            feltIt = true;
            return;
        }
        feltIt = false;
    }

    private boolean feltIt;

    /** Last check found something moving in the water nearby (for tests). */
    public boolean feltMovement() {
        return feltIt;
    }

    @Override
    protected boolean canRandomSwim() {
        return true;
    }

    /** No bucket: it slips out of the hand (catch it by hand-killing or a spear). */
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
        return null; // silent
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TADPOLE_FLOP;
    }
}
