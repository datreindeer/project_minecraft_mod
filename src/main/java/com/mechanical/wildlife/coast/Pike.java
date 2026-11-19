package com.mechanical.wildlife.coast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Northern pike of rivers, lakes and fens: an ambush hunter that hangs almost still in the water, then darts at small
 * fish (cod, salmon ...) and swallows them; fed, it rests a while. It slips away from people who don't move quietly, so
 * sneak up to spear it, or catch it on the hook. Dies as a Whole Pike (gutted with a knife like any whole fish).
 */
public class Pike extends AbstractFish {
    /** Fed: no hunting for about two minutes. */
    private static final int FED_TICKS = 2400;
    private int fed;

    public Pike(EntityType<? extends Pike> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        // not the fish defaults: it lurks instead of milling about, and only quick movement scares it
        goalSelector.addGoal(0, new PanicGoal(this, 1.6));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, p -> !p.isCrouching(), 6.0F, 1.4, 1.8, EntitySelector.NO_CREATIVE_OR_SPECTATOR));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 2.4, false));
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.8, 160));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, AbstractSchoolingFish.class, 20, true, false,
                (target, level) -> fed <= 0 && isInWater()));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (fed > 0) fed--;
    }

    /** It swallows small fish whole (no catch left floating). */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof AbstractSchoolingFish prey) {
            level.sendParticles(ParticleTypes.BUBBLE, prey.getX(), prey.getY(0.5), prey.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            playSound(SoundEvents.GENERIC_EAT.value(), 0.5F, 1.4F);
            prey.discard();
            fed = FED_TICKS;
            setTarget(null);
            return true;
        }
        return super.doHurtTarget(level, target);
    }

    /** No bucket for a fish this size. */
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
        return SoundEvents.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SALMON_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.SALMON_FLOP;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }
}
