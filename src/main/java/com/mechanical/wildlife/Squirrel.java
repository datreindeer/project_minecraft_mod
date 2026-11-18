package com.mechanical.wildlife;

import java.util.EnumSet;

import com.mechanical.AnimalFear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Red squirrel: tiny, quick and very skittish. When scared (or now and then for no reason) it dashes to the nearest
 * tree, runs up the trunk to just under the crown and clings there a while, then comes back down head first.
 * With no tree in reach it simply flees like any other animal (AnimalFear).
 */
public class Squirrel extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/squirrel.png");
    /** Synced so the model can switch to the clinging pose (body upright against the bark). */
    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(Squirrel.class, EntityDataSerializers.BOOLEAN);
    /** Head down while coming down the trunk. */
    private static final EntityDataAccessor<Boolean> DESCENDING = SynchedEntityData.defineId(Squirrel.class, EntityDataSerializers.BOOLEAN);

    public Squirrel(EntityType<? extends Squirrel> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.32);
    }

    public boolean isClimbing() {
        return entityData.get(CLIMBING);
    }

    public boolean isDescending() {
        return entityData.get(DESCENDING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, false);
        builder.define(DESCENDING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal); // loners
        // above AnimalFear's flee goal (priority 0), so a squirrel with a tree in reach climbs instead of running off
        goalSelector.addGoal(-1, new ClimbTreeGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // no gravity only while clinging to a trunk (the synced flag is not saved, so a reload drops it too)
        if (!level().isClientSide() && !isClimbing() && isNoGravity()) setNoGravity(false);
        if (level().isClientSide() || !onGround() || isClimbing()) return;
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistance() > 0.12 && random.nextInt(3) == 0) setDeltaMovement(v.x, 0.25, v.z); // bounding run
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.2F;
    }

    @Override
    public double fearMultiplier() {
        return 1.6;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
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
        return super.getVoicePitch() * 1.5F;
    }

    /** Scared (or idly): run to a trunk, climb to the crown, cling there, come back down. */
    static class ClimbTreeGoal extends Goal {
        private enum Phase { RUN, UP, SIT, DOWN }
        private static final int SEARCH = 10, NOTICE = 10, NOTICE_SNEAK = 3;
        private static final double CLIMB_SPEED = 0.18, DESCEND_SPEED = 0.1;
        private final Squirrel mob;
        private BlockPos trunk;
        private Direction side;
        private Phase phase;
        private int timer;
        private boolean scared;

        ClimbTreeGoal(Squirrel mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean threatened() {
            if (AnimalFear.isPanicking(mob)) return true;
            Player p = mob.level().getNearestPlayer(mob, NOTICE * mob.fearMultiplier());
            if (p == null || p.isCreative() || p.isSpectator()) return false;
            double range = (p.isCrouching() ? NOTICE_SNEAK : NOTICE) * mob.fearMultiplier();
            return mob.distanceTo(p) < range && (mob.distanceTo(p) < NOTICE_SNEAK || mob.hasLineOfSight(p));
        }

        @Override
        public boolean canUse() {
            if (mob.isBaby() || mob.isDozing() || !mob.onGround()) return false;
            scared = threatened();
            if (!scared && mob.getRandom().nextInt(400) != 0) return false;
            return findTrunk();
        }

        /** Nearest log with another log above it and a free side to climb on, within reach. */
        private boolean findTrunk() {
            BlockPos at = mob.blockPosition();
            double best = Double.MAX_VALUE;
            trunk = null;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-SEARCH, -2, -SEARCH), at.offset(SEARCH, 3, SEARCH))) {
                if (!mob.level().getBlockState(p).is(BlockTags.LOGS) || !mob.level().getBlockState(p.above()).is(BlockTags.LOGS)) continue;
                double d = p.distSqr(at);
                if (d >= best) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos stand = p.relative(dir);
                    if (mob.level().getBlockState(stand).isAir() && mob.level().getBlockState(stand.below()).isFaceSturdy(mob.level(), stand.below(), Direction.UP)) {
                        best = d;
                        trunk = p.immutable();
                        side = dir;
                        break;
                    }
                }
            }
            return trunk != null && mob.getNavigation().moveTo(trunk.getX() + 0.5 + side.getStepX(), trunk.getY(), trunk.getZ() + 0.5 + side.getStepZ(), scared ? 2.2 : 1.2);
        }

        @Override
        public void start() {
            phase = Phase.RUN;
            timer = 120; // give up the dash after 6 s
        }

        @Override
        public boolean canContinueToUse() {
            return phase != null && mob.isAlive();
        }

        @Override
        public void stop() {
            phase = null;
            mob.entityData.set(CLIMBING, false);
            mob.entityData.set(DESCENDING, false);
            mob.setNoGravity(false);
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        /** Where the squirrel hugs the bark: just outside the trunk face it climbs. */
        private Vec3 clingPoint() {
            double off = 0.5 + mob.getBbWidth() / 2 + 0.02;
            return new Vec3(trunk.getX() + 0.5 + side.getStepX() * off, mob.getY(), trunk.getZ() + 0.5 + side.getStepZ() * off);
        }

        private boolean logAt(double y) {
            return mob.level().getBlockState(new BlockPos(trunk.getX(), Mth.floor(y), trunk.getZ())).is(BlockTags.LOGS);
        }

        /** Free to move up: the trunk goes on and nothing (leaves, branches) is in the way on its side. */
        private boolean canGoUp() {
            double top = mob.getY() + mob.getBbHeight() + 0.2;
            BlockPos ahead = new BlockPos(trunk.getX() + side.getStepX(), Mth.floor(top), trunk.getZ() + side.getStepZ());
            return logAt(top) && mob.level().getBlockState(ahead).getCollisionShape(mob.level(), ahead).isEmpty();
        }

        private void face() {
            float yaw = (float) (Mth.atan2(side.getStepX(), -side.getStepZ()) * Mth.RAD_TO_DEG); // towards the trunk
            if (phase == Phase.DOWN) yaw += 180.0F;
            mob.setYRot(yaw);
            mob.yBodyRot = yaw;
            mob.yHeadRot = yaw;
        }

        private void hug(double vy) {
            Vec3 c = clingPoint();
            mob.setDeltaMovement((c.x - mob.getX()) * 0.5, vy, (c.z - mob.getZ()) * 0.5);
            face();
        }

        @Override
        public void tick() {
            if (!mob.level().getBlockState(trunk).is(BlockTags.LOGS)) { // tree felled under it
                phase = null;
                return;
            }
            switch (phase) {
                case RUN -> {
                    double dx = trunk.getX() + 0.5 - mob.getX(), dz = trunk.getZ() + 0.5 - mob.getZ();
                    if (dx * dx + dz * dz < 1.3 * 1.3) {
                        mob.getNavigation().stop();
                        mob.entityData.set(CLIMBING, true);
                        mob.setNoGravity(true);
                        phase = Phase.UP;
                        timer = 200;
                    } else if (--timer <= 0 || mob.getNavigation().isDone()) {
                        phase = null;
                    }
                }
                case UP -> {
                    if (canGoUp() && --timer > 0) {
                        hug(CLIMB_SPEED);
                    } else {
                        hug(0);
                        phase = Phase.SIT;
                        timer = (scared ? 300 : 100) + mob.getRandom().nextInt(300);
                    }
                }
                case SIT -> {
                    hug(0);
                    if (mob.tickCount % 20 == 0 && threatened()) timer = Math.max(timer, 100); // stays up while danger lingers
                    if (--timer <= 0) {
                        phase = Phase.DOWN;
                        mob.entityData.set(DESCENDING, true);
                        timer = 400;
                    }
                }
                case DOWN -> {
                    hug(-DESCEND_SPEED);
                    if (mob.onGround() || --timer <= 0) phase = null;
                }
            }
        }
    }
}
