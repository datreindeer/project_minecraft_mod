package com.mechanical.wildlife.desert;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Desert Scorpion: a straw-yellow scorpion of the sand, the gypsum flats and the badlands, with the tail curled up over
 * its back. It hides by day (lies still under the sand's edge: dozing, {@link #sleepTime} is the day) and hunts at night.
 * At night it stings whoever walks right up to it ({@link #STING_RANGE}, sneaking only by day helps), and anything that
 * wakes or hurts it: the sting gives Weakness ({@link #WEAKNESS_TICKS}) and a short Poison. After a sting it backs off.
 * Never runs from a player. Butchered: a little meat and its Scorpion Venom (8 arrows round it: Arrows of Poison).
 */
public class DesertScorpion extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/desert_scorpion.png");
    /** A player this close at night is stung (sneaking: still, at night it feels the steps). */
    public static final double STING_RANGE = 1.8;
    public static final int WEAKNESS_TICKS = 200, POISON_TICKS = 60;

    public DesertScorpion(EntityType<? extends DesertScorpion> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.ARMOR, 3.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal") || g.getClass().getSimpleName().equals("FollowParentGoal"));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false));
        targetSelector.addGoal(2, new NightStingGoal());
    }

    /** Hidden by day (dozing), out at night. */
    @Override
    protected boolean sleepTime() {
        return level().isBrightOutside();
    }

    /** Woken up (stepped on): stings the one who woke it. */
    @Override
    protected void onWoken(Player near) {
        setTarget(near);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && isAlive() && distanceTo(attacker) < 4) setTarget(attacker);
        return hurt;
    }

    /** The sting: Weakness and a short Poison; then it backs off. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) sting(living);
        setTarget(null);
        AnimalFear.panic(this, target.position());
        return hit;
    }

    public void sting(LivingEntity living) {
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0), this);
        living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0), this);
        playSound(SoundEvents.SPIDER_HURT, 0.6F, 1.8F);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.0; // stands its ground, tail up
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() ? null : SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.08F, 1.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    /** At night a player (not in creative) who comes within {@link #STING_RANGE} is stung. */
    class NightStingGoal extends TargetGoal {
        NightStingGoal() {
            super(DesertScorpion.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || AnimalFear.isPanicking(DesertScorpion.this) || !level().isDarkOutside()) return false;
            Player p = level().getNearestPlayer(DesertScorpion.this, STING_RANGE);
            if (p == null || p.isCreative() || p.isSpectator()) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            setTarget(targetMob);
            super.start();
        }
    }
}
