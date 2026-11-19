package com.mechanical.wildlife.mountain;

import java.util.Comparator;
import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Alpine ibex: herds of 2-5 on peaks and windswept hills. Climbs sheer rock and flees uphill ({@link MountainBovid}).
 * Bucks carry huge ridged horns sweeping back in a scimitar curve and a short beard; nannies have short thin horns. In the
 * rut bucks pick fights: two square up, rear and clash horns (RivalButtGoal) a few times, knocking each other back, then
 * part - the crack carries far. Replaces the vanilla goat in the high mountains (gen_fauna_mountain.py).
 */
public class Ibex extends MountainBovid {
    private static final Identifier BUCK = tex("ibex_buck"), NANNY = tex("ibex_nanny"), KID = tex("ibex_kid");
    /** Squared up to a rival, head lowered (synced for the model through isCharging). */
    private static final EntityDataAccessor<Boolean> BUTTING = SynchedEntityData.defineId(Ibex.class, EntityDataSerializers.BOOLEAN);
    private Ibex rival;
    private int clashCooldown;

    public Ibex(EntityType<? extends Ibex> type, Level level) {
        super(type, level);
    }

    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/" + name + ".png");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.SAFE_FALL_DISTANCE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BUTTING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(5, new RivalButtGoal());
    }

    @Override
    protected float maleChance() {
        return 0.45F;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? KID : isMale() ? BUCK : NANNY;
    }

    /** The buck's great horns; nannies and kids show short ones. */
    @Override
    public boolean showsHorns() {
        return isMale() && !isBaby();
    }

    @Override
    public boolean isCharging() {
        return entityData.get(BUTTING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (clashCooldown > 0) clashCooldown--;
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    @Override
    public double fearMultiplier() {
        return 1.2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.15F, 0.9F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.3F : isMale() ? 0.7F : 0.85F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    private boolean free() {
        return isMale() && !isBaby() && !isDozing() && !AnimalFear.isPanicking(this) && getTarget() == null;
    }

    /**
     * Rare by day: a grown buck challenges the nearest other free buck within 12 blocks. Both walk up to each other, rear
     * and lower their horns (the model's charge pose), and at contact the horns crack together: both are thrown back a
     * little (no damage). Four clashes or 15 s, then they part. Panic (a player, a hit) breaks it off at once.
     */
    class RivalButtGoal extends Goal {
        private int ticks, clashes;

        RivalButtGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!free()) return false;
            if (rival != null) return rival.isAlive() && rival.rival == Ibex.this;
            if (random.nextInt(1200) != 0 || !level().isBrightOutside()) return false;
            Ibex other = level().getEntitiesOfClass(Ibex.class, getBoundingBox().inflate(12), o -> o != Ibex.this && o.free() && o.rival == null)
                    .stream().min(Comparator.comparingDouble(Ibex.this::distanceToSqr)).orElse(null);
            if (other == null) return false;
            rival = other;
            other.rival = Ibex.this; // its own RivalButtGoal picks this up
            return true;
        }

        @Override
        public void start() {
            ticks = 300;
            clashes = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && clashes < 4 && rival != null && rival.isAlive() && rival.rival == Ibex.this && free() && rival.free()
                    && distanceTo(rival) < 16;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(rival, 30.0F, 30.0F);
            double d = distanceTo(rival);
            entityData.set(BUTTING, d < 5);
            if (d > 1.7) {
                if ((tickCount + getId()) % 10 == 0) getNavigation().moveTo(rival, d < 5 ? 1.3 : 0.9);
                return;
            }
            getNavigation().stop();
            // the lower id strikes for both, so each clash happens once
            if (getId() < rival.getId() && clashCooldown == 0 && rival.clashCooldown == 0) {
                clashCooldown = rival.clashCooldown = 35;
                clashes++;
                playSound(SoundEvents.GOAT_RAM_IMPACT, 1.5F, 0.75F + random.nextFloat() * 0.1F);
                Vec3 apart = rival.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.55);
                rival.push(apart.x, 0.15, apart.z);
                push(-apart.x, 0.15, -apart.z);
            }
        }

        @Override
        public void stop() {
            entityData.set(BUTTING, false);
            if (rival != null && rival.rival == Ibex.this) rival.rival = null;
            rival = null;
        }
    }
}
