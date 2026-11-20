package com.mechanical.wildlife.coldhunters;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.BirdNestBlock;
import com.mechanical.Birds;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Squirrel;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Pine marten: a chocolate-brown tree weasel with a creamy-orange bib and a bushy tail, of the forests and the taiga.
 * At home in the trees: scared (or idly) it runs up the nearest trunk to just under the crown and waits there. Hunts red
 * squirrels, and raids Bird Nests ({@link BirdNestBlock}) within {@link #NEST_SEARCH} blocks for their eggs, leaving the
 * shells: the birds take it like a robbery by a player, and a nest raided bare often enough is abandoned.
 */
public class PineMarten extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/pine_marten.png");
    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(PineMarten.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DESCENDING = SynchedEntityData.defineId(PineMarten.class, EntityDataSerializers.BOOLEAN);
    static final int NEST_SEARCH = 16;

    public PineMarten(EntityType<? extends PineMarten> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 3.0);
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
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal"));
        goalSelector.addGoal(-1, new ClimbTreeGoal()); // before AnimalFear's flee goal (0): up a tree rather than away
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false));
        goalSelector.addGoal(3, new RaidNestGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Squirrel.class, true,
                (target, level) -> !isBaby() && random.nextInt(6) == 0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && !isClimbing() && isNoGravity()) setNoGravity(false);
        if (level().isClientSide() || !onGround() || isClimbing()) return;
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistance() > 0.14 && random.nextInt(4) == 0) setDeltaMovement(v.x, 0.26, v.z); // bounding run
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
        return 1.3;
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
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.35F;
    }

    /** Walks to the nearest Bird Nest with eggs, eats one and leaves the shell; empties count as a robbery. */
    class RaidNestGoal extends Goal {
        private BlockPos nest;
        private int ticks;

        RaidNestGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || isDozing() || getTarget() != null || isClimbing() || random.nextInt(300) != 0) return false;
            BlockPos at = blockPosition();
            if (!level().hasChunksAt(at.offset(-NEST_SEARCH, 0, -NEST_SEARCH), at.offset(NEST_SEARCH, 0, NEST_SEARCH))) return false;
            nest = null;
            double best = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-NEST_SEARCH, -4, -NEST_SEARCH), at.offset(NEST_SEARCH, 6, NEST_SEARCH))) {
                BlockState s = level().getBlockState(p);
                if (s.is(Birds.BIRD_NEST.get()) && s.getValue(BirdNestBlock.EGGS) > 0 && p.distSqr(at) < best) {
                    best = p.distSqr(at);
                    nest = p.immutable();
                }
            }
            return nest != null && getNavigation().moveTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, 1.0);
        }

        @Override
        public void start() {
            ticks = 600;
        }

        @Override
        public boolean canContinueToUse() {
            BlockState s = level().getBlockState(nest);
            return --ticks > 0 && getTarget() == null && s.is(Birds.BIRD_NEST.get()) && s.getValue(BirdNestBlock.EGGS) > 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Vec3 c = Vec3.atBottomCenterOf(nest);
            getLookControl().setLookAt(c);
            if (distanceToSqr(c) > 1.6 * 1.6) {
                if ((tickCount + getId()) % 20 == 0) getNavigation().moveTo(c.x, c.y, c.z, 1.0);
                return;
            }
            rob((ServerLevel) level(), nest);
            ticks = 0;
        }
    }

    /** One egg out of the nest at {@code pos}, as BirdNestBlock#useWithoutItem does for a player (unease, abandonment). */
    void rob(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int eggs = state.getValue(BirdNestBlock.EGGS);
        BlockState next = state.setValue(BirdNestBlock.EGGS, eggs - 1);
        if (eggs == 1 && state.getValue(BirdNestBlock.BIRD) != BirdNestBlock.Bird.NONE) {
            int robbed = state.getValue(BirdNestBlock.ROBBED) + 1;
            next = robbed >= BirdNestBlock.ABANDON_AFTER ? next.setValue(BirdNestBlock.BIRD, BirdNestBlock.Bird.NONE) : next.setValue(BirdNestBlock.ROBBED, robbed);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, getSoundSource(), 0.8F, 1.3F);
        level.playSound(null, pos, SoundEvents.FOX_EAT, getSoundSource(), 0.7F, 1.4F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.BONE_BLOCK.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.05);
        spawnAtLocation(level, new ItemStack(Mechanical.EGGSHELL.get()));
        heal(2.0F);
        // birds about the nest scatter
        for (var bird : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, getBoundingBox().inflate(10),
                a -> a instanceof com.mechanical.wildlife.FlockBird || a.getType() == com.mechanical.wildlife.Wildlife.PHEASANT.get())) {
            if (bird instanceof com.mechanical.wildlife.FlockBird f) f.scare(position());
            else AnimalFear.panic(bird, position());
        }
    }

    /** Scared (or idly): runs to a trunk, climbs to just under the crown, waits, comes back down head first. */
    class ClimbTreeGoal extends Goal {
        private enum Phase { RUN, UP, SIT, DOWN }
        private static final int SEARCH = 12, NOTICE = 10, NOTICE_SNEAK = 3;
        private static final double CLIMB_SPEED = 0.2, DESCEND_SPEED = 0.12;
        private BlockPos trunk;
        private Direction side;
        private Phase phase;
        private int timer;
        private boolean scared;

        ClimbTreeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean threatened() {
            if (AnimalFear.isPanicking(PineMarten.this)) return true;
            Player p = level().getNearestPlayer(PineMarten.this, NOTICE * fearMultiplier());
            if (p == null || p.isCreative() || p.isSpectator()) return false;
            double range = (p.isCrouching() ? NOTICE_SNEAK : NOTICE) * fearMultiplier();
            return distanceTo(p) < range && (distanceTo(p) < NOTICE_SNEAK || hasLineOfSight(p));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || isDozing() || !onGround() || getTarget() != null) return false;
            scared = threatened();
            if (!scared && random.nextInt(600) != 0) return false;
            return findTrunk();
        }

        /** Nearest log with another log above it and a free side to climb on. */
        private boolean findTrunk() {
            BlockPos at = blockPosition();
            double best = Double.MAX_VALUE;
            trunk = null;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-SEARCH, -2, -SEARCH), at.offset(SEARCH, 3, SEARCH))) {
                if (!level().getBlockState(p).is(BlockTags.LOGS) || !level().getBlockState(p.above()).is(BlockTags.LOGS)) continue;
                double d = p.distSqr(at);
                if (d >= best) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos stand = p.relative(dir);
                    if (level().getBlockState(stand).isAir() && level().getBlockState(stand.below()).isFaceSturdy(level(), stand.below(), Direction.UP)) {
                        best = d;
                        trunk = p.immutable();
                        side = dir;
                        break;
                    }
                }
            }
            return trunk != null && getNavigation().moveTo(trunk.getX() + 0.5 + side.getStepX(), trunk.getY(), trunk.getZ() + 0.5 + side.getStepZ(), scared ? 2.0 : 1.1);
        }

        @Override
        public void start() {
            phase = Phase.RUN;
            timer = 140;
        }

        @Override
        public boolean canContinueToUse() {
            return phase != null && isAlive();
        }

        @Override
        public void stop() {
            phase = null;
            entityData.set(CLIMBING, false);
            entityData.set(DESCENDING, false);
            setNoGravity(false);
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private Vec3 clingPoint() {
            double off = 0.5 + getBbWidth() / 2 + 0.02;
            return new Vec3(trunk.getX() + 0.5 + side.getStepX() * off, getY(), trunk.getZ() + 0.5 + side.getStepZ() * off);
        }

        private boolean canGoUp() {
            double top = getY() + getBbHeight() + 0.2;
            BlockPos ahead = new BlockPos(trunk.getX() + side.getStepX(), Mth.floor(top), trunk.getZ() + side.getStepZ());
            return level().getBlockState(new BlockPos(trunk.getX(), Mth.floor(top), trunk.getZ())).is(BlockTags.LOGS)
                    && level().getBlockState(ahead).getCollisionShape(level(), ahead).isEmpty();
        }

        private void hug(double vy) {
            Vec3 c = clingPoint();
            setDeltaMovement((c.x - getX()) * 0.5, vy, (c.z - getZ()) * 0.5);
            float yaw = (float) (Mth.atan2(side.getStepX(), -side.getStepZ()) * Mth.RAD_TO_DEG); // towards the trunk
            if (phase == Phase.DOWN) yaw += 180.0F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }

        @Override
        public void tick() {
            if (phase == null || trunk == null) return;
            if (!level().getBlockState(trunk).is(BlockTags.LOGS)) {
                phase = null;
                return;
            }
            switch (phase) {
                case RUN -> {
                    double dx = trunk.getX() + 0.5 - getX(), dz = trunk.getZ() + 0.5 - getZ();
                    if (dx * dx + dz * dz < 1.4 * 1.4) {
                        getNavigation().stop();
                        entityData.set(CLIMBING, true);
                        setNoGravity(true);
                        phase = Phase.UP;
                        timer = 240;
                    } else if (--timer <= 0 || getNavigation().isDone()) {
                        phase = null;
                    }
                }
                case UP -> {
                    if (canGoUp() && --timer > 0) hug(CLIMB_SPEED);
                    else {
                        hug(0);
                        phase = Phase.SIT;
                        timer = (scared ? 400 : 160) + random.nextInt(300);
                    }
                }
                case SIT -> {
                    hug(0);
                    if ((tickCount + getId()) % 20 == 0 && threatened()) timer = Math.max(timer, 120);
                    if (--timer <= 0) {
                        phase = Phase.DOWN;
                        entityData.set(DESCENDING, true);
                        timer = 400;
                    }
                }
                case DOWN -> {
                    hug(-DESCEND_SPEED);
                    if (onGround() || --timer <= 0) phase = null;
                }
            }
        }
    }
}
