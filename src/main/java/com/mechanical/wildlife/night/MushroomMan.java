package com.mechanical.wildlife.night;

import java.util.EnumSet;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Mushroom Man: a walking fungus of the dark, damp woods and the caves, in the creeper's place. It creeps up on you; within
 * 3 blocks it stops, hisses and swells for 1.5 s, and bursts into a spore cloud (NightFauna#sporeBurst): Blindness, Nausea
 * and a blow to your sanity, a little damage close in, and never a broken block. Step back more than 7 blocks (or out of
 * sight) and it shrinks back. Killed first, it drops Mushroom Flesh and maybe a Spore Sac; burst, nothing.
 * As a Monster it gets NightDangers' fire fear, torch smashing and the rest.
 */
public class MushroomMan extends Monster {
    /** -1 shrinking/idle, 1 swelling. Synced for the renderer and FleeThreatGoal (people back off from a swelling one). */
    private static final EntityDataAccessor<Integer> SWELL_DIR = SynchedEntityData.defineId(MushroomMan.class, EntityDataSerializers.INT);
    static final int MAX_SWELL = 30;
    private int swell, oldSwell;

    public MushroomMan(EntityType<? extends MushroomMan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWELL_DIR, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SwellGoal());
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false)); // only to walk up: SwellGoal takes over close in
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    public int getSwellDir() {
        return entityData.get(SWELL_DIR);
    }

    void setSwellDir(int dir) {
        entityData.set(SWELL_DIR, dir);
    }

    /** 0..1 (a little over at the end): how far it has swollen, for the renderer. */
    public float getSwelling(float partialTicks) {
        return Mth.lerp(partialTicks, (float) oldSwell, (float) swell) / (MAX_SWELL - 2);
    }

    @Override
    public void tick() {
        if (isAlive()) {
            oldSwell = swell;
            int dir = getSwellDir();
            if (dir > 0 && swell == 0) {
                playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.6F); // the hiss of a splitting cap
                gameEvent(GameEvent.PRIME_FUSE);
            }
            swell = Math.max(0, swell + dir);
            if (swell >= MAX_SWELL) {
                swell = MAX_SWELL;
                burst();
            }
        }
        super.tick();
    }

    private void burst() {
        if (!(level() instanceof ServerLevel level)) return;
        dead = true;
        NightFauna.sporeBurst(level, position(), this, true);
        discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.MOSS_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    /** Close in and in sight: swell; otherwise shrink back. */
    class SwellGoal extends Goal {
        SwellGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return getSwellDir() > 0 || t != null && distanceToSqr(t) < 9;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void stop() {
            setSwellDir(-1);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            setSwellDir(t == null || distanceToSqr(t) > 49 || !getSensing().hasLineOfSight(t) ? -1 : 1);
        }
    }
}
