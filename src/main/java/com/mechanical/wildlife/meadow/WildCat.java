package com.mechanical.wildlife.meadow;

import com.mechanical.wildlife.Pheasant;
import com.mechanical.wildlife.Squirrel;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;
import net.minecraft.world.entity.animal.feline.CatSoundVariants;
import net.minecraft.world.level.Level;

/**
 * European wild cat: a stocky grey-brown tabby with a thick, blunt, black-ringed tail. Shy (it notices players from far
 * off and slips away) and nocturnal: it sleeps by day and hunts voles, moles, squirrels, rabbits and ground birds at
 * night, creeping up low and pouncing the last few blocks. Solitary; a mother may have a kitten along.
 * Taming hook: {@link #isPrey} is what it hunts; a tame cat would keep this (voles near its home) and drop the fear.
 */
public class WildCat extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_cat.png");
    private static final Identifier KITTEN_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/wild_cat_kitten.png");
    /** Creeping up on its prey (synced for the low stalking pose). */
    private static final EntityDataAccessor<Boolean> STALKING = SynchedEntityData.defineId(WildCat.class, EntityDataSerializers.BOOLEAN);
    /** Starts creeping inside this range of its prey, and pounces inside POUNCE. */
    private static final double STALK = 10, POUNCE = 3.5;

    public WildCat(EntityType<? extends WildCat> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    /** What a wild cat hunts: small game that a cat can kill. */
    public static boolean isPrey(LivingEntity e) {
        if (e instanceof Mole mole) return !mole.isBurrowed();
        return e instanceof FieldVole || e instanceof Squirrel || e instanceof Rabbit || e instanceof Pheasant
                || e instanceof Chicken || e instanceof com.mechanical.wildlife.FlockBird bird && bird.getType() == com.mechanical.wildlife.Wildlife.ROCK_DOVE.get();
    }

    public boolean isStalking() {
        return entityData.get(STALKING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STALKING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        MeadowFauna.removeGoals(goalSelector, "FollowHerdGoal"); // solitary
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return getTarget() != null && distanceTo(getTarget()) < POUNCE && super.canUse();
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 20, true, false,
                (target, level) -> isPrey(target) && huntingTime() && !isBaby()));
    }

    /** Dusk to dawn: kittens and sleeping cats don't hunt. */
    private boolean huntingTime() {
        return level().isDarkOutside() && !isDozing();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || !huntingTime())) setTarget(null);
        // creep low and slow while closing in, then the pounce (LeapAtTargetGoal) and the dash
        boolean stalk = t != null && distanceTo(t) < STALK && distanceTo(t) > POUNCE;
        if (stalk != isStalking()) entityData.set(STALKING, stalk);
    }

    @Override
    public float getSpeed() {
        return super.getSpeed() * (isStalking() && !com.mechanical.AnimalFear.isPanicking(this) ? 0.45F : 1.0F);
    }

    @Override
    protected boolean sleepTime() {
        return level().isBrightOutside(); // nocturnal
    }

    @Override
    public Identifier texture() {
        return isBaby() ? KITTEN_TEXTURE : TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.35F; // a mother with her kitten
    }

    @Override
    public double fearMultiplier() {
        return 1.6;
    }

    private static CatSoundVariant.CatSoundSet sounds(boolean baby) {
        CatSoundVariant v = SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC);
        return baby ? v.babySounds() : v.adultSounds();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // mostly silent; a hiss when cornered (charging), a yowl now and then at night
        if (isCharging()) return sounds(isBaby()).hissSound().value();
        return level().isDarkOutside() && random.nextInt(4) == 0 ? sounds(isBaby()).strayAmbientSound().value() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds(isBaby()).hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds(isBaby()).deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.85F;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        setTarget(null);
        return super.killedEntity(level, entity, source);
    }
}
