package com.mechanical.wildlife.coldhunters;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Norway lemming: a tiny, round, black-and-tawny rodent of the tundra and snowy plains, in scurrying bunches. Bold for
 * its size (it squeaks at you before it runs). Prey for foxes, lynx and ermine, and caught in snares like other small
 * game. Lemming years: every {@link #CYCLE_DAYS} days the population booms for {@link #BOOM_DAYS} days, and while it does
 * each grown lemming now and then has a young one, up to {@link #CROWD} lemmings within {@link #CROWD_RANGE} blocks.
 */
public class Lemming extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/lemming.png");
    static final int CYCLE_DAYS = 8, BOOM_DAYS = 2, CROWD = 10, BREED_CHECK = 600;
    static final double CROWD_RANGE = 12;
    static final float BREED_CHANCE = 0.3F;

    public Lemming(EntityType<? extends Lemming> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    /** A boom ("lemming year") is on in this world right now. */
    public static boolean boom(Level level) {
        return (level.getOverworldClockTime() / 24000L) % CYCLE_DAYS >= CYCLE_DAYS - BOOM_DAYS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || isBaby() || (tickCount + getId()) % BREED_CHECK != 0 || !boom(level)) return;
        if (random.nextFloat() >= BREED_CHANCE || level.getEntitiesOfClass(Lemming.class, getBoundingBox().inflate(CROWD_RANGE)).size() >= CROWD) return;
        Lemming young = ColdhuntersFauna.LEMMING.get().create(level, EntitySpawnReason.BREEDING);
        if (young == null) return;
        young.setBaby(true);
        young.snapTo(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
        level.addFreshEntity(young);
        playSound(SoundEvents.RABBIT_AMBIENT, 0.6F, 2.0F);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.35F;
    }

    /** Lets you come close and squeaks at you, then scurries off. */
    @Override
    public double fearMultiplier() {
        return 0.7;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.9F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
