package com.mechanical.wildlife.jungle;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Proboscis Monkey: a pot-bellied orange monkey of the mangroves with a long tail and (the males) a great drooping nose.
 * Lives in troops and is never quiet: it honks and grunts all day, and when it sees a hunter coming (a player within
 * {@link #ALARM_RANGE} blocks, not sneaking) it gives the alarm honk and every wild animal within {@link #ALARM_SPREAD}
 * blocks takes fright and runs. A bad neighbour for a stalking hunter. Gives meat and a little hide.
 */
public class ProboscisMonkey extends WildAnimal {
    private static final Identifier MALE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/proboscis_monkey.png");
    static final double ALARM_RANGE = 14, ALARM_SPREAD = 20;
    private static final int ALARM_COOLDOWN = 300;
    private int alarmCooldown;

    public ProboscisMonkey(EntityType<? extends ProboscisMonkey> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER; // good swimmers, they cross the channels
    }

    @Override
    public Identifier texture() {
        return MALE;
    }

    /** Males carry the big nose (and the young none); from the UUID so the carcass keeps it. */
    @Override
    public boolean showsHorns() {
        return !isBaby() && (getUUID().getLeastSignificantBits() & 1) == 0;
    }

    @Override
    public double fearMultiplier() {
        return 1.4;
    }

    /** The alarm honk: every wild animal near takes fright and runs from {@code from}. */
    public void alarm(ServerLevel level, LivingEntity from) {
        alarmCooldown = ALARM_COOLDOWN;
        level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.NEUTRAL, 3.0F, 0.55F);
        for (WildAnimal a : level.getEntitiesOfClass(WildAnimal.class, getBoundingBox().inflate(ALARM_SPREAD),
                a -> !(a instanceof Predator) || ((Predator) a).fearsPlayers())) {
            if (a instanceof com.mechanical.wildlife.FlockBird bird) bird.scare(from.position());
            else AnimalFear.panic(a, from.position());
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || isNoAi()) return;
        if (alarmCooldown > 0) {
            alarmCooldown--;
            return;
        }
        if ((tickCount + getId()) % 10 != 0) return;
        Player p = level.getNearestPlayer(this, ALARM_RANGE);
        if (p != null && !p.isCreative() && !p.isSpectator() && (!p.isShiftKeyDown() || distanceTo(p) < 5) && hasLineOfSight(p)) alarm(level, p);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextBoolean() ? SoundEvents.GOAT_SCREAMING_AMBIENT : SoundEvents.PANDA_AMBIENT; // honks and grunts
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100; // noisy
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.0F : 0.6F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PANDA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PANDA_DEATH;
    }
}
