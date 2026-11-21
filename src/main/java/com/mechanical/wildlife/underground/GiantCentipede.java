package com.mechanical.wildlife.underground;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Giant centipede: a long, armoured, many-legged hunter of the deep caves (Y 0 to -30). Climbs walls like a spider, and
 * clings to cave ceilings waiting for prey: a player who walks underneath (within {@link #DROP_RANGE} blocks sideways)
 * gets it dropped on him, venomous forcipules first (Poison I, {@link #VENOM_TICKS} ticks). Hunts players within
 * {@link #SIGHT} blocks (sneaking: {@link #SIGHT_SNEAKING}) - but it hates light: someone holding a torch (or anything in
 * {@code mechanical:scares_predators}) is left alone and backed away from, and it won't drop on him.
 */
public class GiantCentipede extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/giant_centipede.png");
    /** 0 on the ground, 1 on a wall, 2 clinging to the ceiling (synced for the model). */
    private static final EntityDataAccessor<Integer> CLING = SynchedEntityData.defineId(GiantCentipede.class, EntityDataSerializers.INT);
    public static final int GROUND = 0, WALL = 1, CEILING = 2;
    static final int VENOM_TICKS = 100;
    static final double SIGHT = 12, SIGHT_SNEAKING = 5, DROP_RANGE = 1.5, TORCH_BACK_OFF = 6;

    public GiantCentipede(EntityType<? extends GiantCentipede> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLING, GROUND);
    }

    public int cling() {
        return entityData.get(CLING);
    }

    private void setCling(int c) {
        entityData.set(CLING, c);
        setNoGravity(c == CEILING);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal")); // solitary
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false) {
            @Override
            public boolean canUse() {
                return cling() != CEILING && super.canUse();
            }
        });
        goalSelector.addGoal(2, new ClimbToCeilingGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (t, level) -> !holdsLight(t) && cling() != CEILING && distanceTo(t) < (t.isShiftKeyDown() ? SIGHT_SNEAKING : SIGHT)));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return cling() == WALL;
    }

    /** Holds a torch or the like in either hand. */
    static boolean holdsLight(LivingEntity e) {
        return e.getMainHandItem().is(Predator.SCARES_PREDATORS) || e.getOffhandItem().is(Predator.SCARES_PREDATORS);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (cling() == CEILING) {
            setDeltaMovement(Vec3.ZERO);
            if (!solidAbove() || hurtTime > 0 || random.nextInt(2400) == 0) setCling(GROUND);
            else {
                Player p = level().getNearestPlayer(this, 8);
                if (p != null && !p.isCreative() && !p.isSpectator() && !holdsLight(p) && p.getY() < getY()
                        && Math.abs(p.getX() - getX()) < DROP_RANGE && Math.abs(p.getZ() - getZ()) < DROP_RANGE) {
                    setCling(GROUND); // let go: it falls on him
                    setTarget(p);
                    playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.6F);
                }
            }
            return;
        }
        if (!climbingUp) setCling(horizontalCollision ? WALL : GROUND);
        LivingEntity t = getTarget();
        if (t != null && holdsLight(t) && distanceTo(t) < TORCH_BACK_OFF) {
            setTarget(null);
            AnimalFear.panic(this, t.position());
        }
    }

    /** The block just above its back blocks movement (a ceiling to hang from). */
    private boolean solidAbove() {
        BlockPos above = BlockPos.containing(getX(), getBoundingBox().maxY + 0.1, getZ());
        BlockState s = level().getBlockState(above);
        return s.isFaceSturdy(level(), above, Direction.DOWN);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean bit = super.doHurtTarget(level, target);
        if (bit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, 0), this);
        return bit;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) setCling(GROUND);
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return entityTags().contains(Predator.NIGHT_TAG);
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
        return 0.0; // it doesn't run from people; light is what drives it off
    }

    /** No night or day down there: it never dozes. */
    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    public float bleedThreshold() {
        return 0.3F;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true; // the chitin takes the blows
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
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
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.25F, 0.6F);
    }

    private boolean climbingUp;

    /**
     * Idle against a wall with a ceiling 2-6 blocks over it: crawls straight up the wall and grips the ceiling, waiting
     * there. Gives up after 5 s if it gets stuck.
     */
    class ClimbToCeilingGoal extends Goal {
        private int ticks;

        ClimbToCeilingGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || cling() == CEILING || !onGround() || random.nextInt(200) != 0) return false;
            Direction wall = wall();
            if (wall == null) return false;
            BlockPos p = blockPosition();
            for (int dy = 1; dy <= 6; dy++) {
                BlockPos up = p.above(dy);
                if (level().getBlockState(up).isFaceSturdy(level(), up, Direction.DOWN)) {
                    if (dy < 2) return false;
                    setYRot(wall.toYRot());
                    yBodyRot = getYRot();
                    return true;
                }
                if (!level().getBlockState(up).getCollisionShape(level(), up).isEmpty()) return false;
            }
            return false;
        }

        /** A solid side (N/E/S/W) at its feet and one above, or null. */
        private Direction wall() {
            BlockPos p = blockPosition();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos s = p.relative(d);
                if (level().getBlockState(s).isFaceSturdy(level(), s, d.getOpposite())
                        && level().getBlockState(s.above()).isFaceSturdy(level(), s.above(), d.getOpposite())) return d;
            }
            return null;
        }

        @Override
        public void start() {
            ticks = 100;
            climbingUp = true;
            getNavigation().stop();
            entityData.set(CLING, WALL);
        }

        @Override
        public boolean canContinueToUse() {
            return climbingUp && ticks > 0 && getTarget() == null && hurtTime == 0;
        }

        @Override
        public void tick() {
            ticks--;
            if (solidAbove()) {
                setCling(CEILING);
                climbingUp = false;
                return;
            }
            setNoGravity(true);
            setDeltaMovement(0, 0.12, 0);
        }

        @Override
        public void stop() {
            climbingUp = false;
            if (cling() != CEILING) setCling(GROUND);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
