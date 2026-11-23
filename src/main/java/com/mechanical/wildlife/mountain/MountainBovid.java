package com.mechanical.wildlife.mountain;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Horned grazers of the mountain group (ibex, chamois, saiga): male or female (synced, saved), and for the climbers
 * (ibex, chamois) a sure foot on rock: they walk straight up natural rock faces like a spider climbs (WallClimberNavigation
 * heads straight for a goal it can't path to, and {@link #onClimbable} holds while pressed against rock, earth or snow -
 * never a built wall of planks or logs), and when a player comes too close they flee <em>uphill</em> (FleeUphillGoal)
 * instead of just away. A long safe fall distance (attribute) lets them drop down ledges unhurt.
 */
public abstract class MountainBovid extends WildAnimal {
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(MountainBovid.class, EntityDataSerializers.BOOLEAN);
    private boolean climbing;

    protected MountainBovid(EntityType<? extends MountainBovid> type, Level level) {
        super(type, level);
    }

    /** Share of grown animals that are male. */
    protected abstract float maleChance();

    /** Ibex and chamois climb rock faces and flee uphill; the saiga is a plains runner. */
    protected boolean climbs() {
        return true;
    }

    public boolean isMale() {
        return entityData.get(MALE);
    }

    /** Tests and spawning code: make it a male or a female. */
    public void setMale(boolean male) {
        entityData.set(MALE, male);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MALE, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        entityData.set(MALE, level.getRandom().nextFloat() < maleChance());
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("male", isMale());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(MALE, input.getBooleanOr("male", false));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // priority 0 and added before AnimalFear's flee goal (added on join), so it gets the first say when a player comes
        if (climbs()) goalSelector.addGoal(0, new FleeUphillGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return climbs() ? new WallClimberNavigation(this, level) : super.createNavigation(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) climbing = climbs() && horizontalCollision && againstRock();
    }

    @Override
    public boolean onClimbable() {
        return climbing || super.onClimbable();
    }

    /** The block in front at the feet or knees is natural ground: rock, earth, gravel, snow, ice or terracotta. */
    private boolean againstRock() {
        Vec3 look = Vec3.directionFromRotation(0, getYRot()).scale(getBbWidth() / 2 + 0.3);
        BlockPos front = BlockPos.containing(getX() + look.x, getY() + 0.2, getZ() + look.z);
        return naturalGround(level().getBlockState(front)) || naturalGround(level().getBlockState(front.above()));
    }

    static boolean naturalGround(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.SNOW)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.CALCITE) || s.is(Blocks.TUFF) || s.is(Blocks.SMOOTH_BASALT);
    }

    /**
     * Noticed a player (same distances as AnimalFear, times the animal's skittishness) or panicking: picks the highest of
     * a dozen escape spots away from him and makes for it, up the rock face if need be. Leaves it to AnimalFear's flight
     * when no spot is at least 2 blocks higher (flat ground).
     */
    static class FleeUphillGoal extends Goal {
        private final MountainBovid mob;
        private Vec3 threat;

        FleeUphillGoal(MountainBovid mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private Vec3 findThreat() {
            if (mob.isDozing() || mob.isLeashed()) return null;
            Player p = mob.level().getNearestPlayer(mob, 24);
            if (p == null || p.isCreative() || p.isSpectator()) return null;
            if (AnimalFear.isPanicking(mob)) return p.position();
            double range = (p.isCrouching() ? 4 : p.isSprinting() ? 16 : 10) * mob.fearMultiplier();
            double d = mob.distanceTo(p);
            return d <= range && (d <= 4 || mob.hasLineOfSight(p)) ? p.position() : null;
        }

        private Vec3 uphill(Vec3 from) {
            Vec3 best = null;
            for (int i = 0; i < 12; i++) {
                Vec3 v = DefaultRandomPos.getPosAway(mob, 14, 10, from);
                if (v != null && v.y >= mob.getY() + 2 && (best == null || v.y > best.y)) best = v;
            }
            return best;
        }

        private boolean flee() {
            Vec3 to = uphill(threat);
            return to != null && mob.getNavigation().moveTo(to.x, to.y, to.z, AnimalFear.isPanicking(mob) ? 2.0 : 1.6);
        }

        @Override
        public boolean canUse() {
            threat = findThreat();
            return threat != null && flee();
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if ((mob.tickCount + mob.getId()) % 20 != 0 || !mob.getNavigation().isDone()) return;
            Vec3 t = findThreat();
            if (t != null) {
                threat = t;
                flee();
            }
        }
    }
}
