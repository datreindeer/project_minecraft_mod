package com.mechanical.wildlife.warm;

import java.util.EnumSet;

import com.mechanical.CarcassEntity;
import com.mechanical.wildlife.CaveLion;
import com.mechanical.wildlife.Mouflon;
import com.mechanical.wildlife.RoeDeer;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Leopard: a lone spotted cat of jungle and savanna. Hunts like the cave lion it inherits from (creeps up crouched, freezes
 * when watched, pounces from 2.5-7 blocks, fears fire), but smaller and a climber: it spends the day up a tree, lying on
 * the canopy ({@link TreeGoal}), and at dusk drops from there onto whatever passes below. A fresh kill it hauls up its
 * tree in its jaws and leaves lying in the branches, out of reach of everything else. Lands on its feet: the first
 * {@link #SAFE_FALL} blocks of a fall don't hurt it.
 */
public class Leopard extends CaveLion {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/leopard.png");
    private static final float SAFE_FALL = 10.0F;
    private static final int TREE_RANGE = 14;
    /** Tag on a carcass a leopard has stored in a tree: it is not dragged off again. */
    public static final String CACHED_TAG = "mechanical.leopard_cache";

    public Leopard(EntityType<? extends Leopard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // ahead of the lion's drag-away (4): with a tree near, the kill goes up it; by day, up a tree to sleep
        goalSelector.addGoal(3, new TreeGoal());
        goalSelector.addGoal(5, new PerchGoal());
    }

    /** It spends the day up a tree (TreeGoal), not in the lion's shade. */
    @Override
    protected boolean restsInShade() {
        return false;
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return !e.isBaby() && (e instanceof Tapir || e instanceof Onager || e instanceof JungleFowl || e instanceof Peafowl || e instanceof RoeDeer
                || e instanceof Mouflon || e instanceof WildBoar || e instanceof DesertLizard
                || e instanceof com.mechanical.people.Tribesperson) || e instanceof WaterBuffalo && e.isBaby();
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return super.causeFallDamage(Math.max(0, fallDistance - SAFE_FALL), damageModifier, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDozing() || isCrouching() ? null : SoundEvents.OCELOT_AMBIENT; // the rasping "sawing" call, pitched low
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.OCELOT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.55F;
    }

    /** Top of a tree near {@code base}: standing on the highest leaves within 2 blocks of the trunk, or null. */
    private BlockPos canopy(BlockPos base) {
        BlockPos best = null;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int top = level().getHeight(Heightmap.Types.MOTION_BLOCKING, base.getX() + dx, base.getZ() + dz) - 1;
            BlockPos p = new BlockPos(base.getX() + dx, top, base.getZ() + dz);
            if (top > base.getY() + 2 && top < base.getY() + 16 && level().getBlockState(p).is(BlockTags.LEAVES)
                    && (best == null || top > best.getY())) best = p;
        }
        return best == null ? null : best.above();
    }

    /** The foot of the nearest tree trunk (a log standing on soil) within range, or null. */
    private BlockPos nearestTrunk() {
        BlockPos me = blockPosition(), best = null;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-TREE_RANGE, -3, -TREE_RANGE), me.offset(TREE_RANGE, 3, TREE_RANGE))) {
            if (!level().getBlockState(p).is(BlockTags.LOGS) || !level().getBlockState(p.below()).is(BlockTags.DIRT)) continue;
            if (best == null || p.distSqr(me) < best.distSqr(me)) best = p.immutable();
        }
        return best;
    }

    /**
     * Up a tree: by day to lie on the canopy, or with a fresh carcass in its jaws to store it there. Walks to the trunk,
     * climbs it (claws in the bark: it passes up through the leaves) and steps out onto the top; a carried carcass is left
     * beside it in the branches. Stays up there while it rests.
     */
    class TreeGoal extends Goal {
        private BlockPos trunk, top;
        private CarcassEntity carcass;
        private int ticks;
        private boolean climbing, done;

        TreeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        private CarcassEntity freshKill() {
            return level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(4),
                    c -> c.stage() == 0 && c.tickCount < 1200 && c.animal() != WarmFauna.LEOPARD.get() && c.animal() != Wildlife.CAVE_LION.get()
                            && c.hungAt() == null && c.haulerId() < 0 && !c.isPassenger() && !c.entityTags().contains(CACHED_TAG)
                            && !c.isLarge()).stream().findFirst().orElse(null);
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || (tickCount + getId()) % 20 != 0 || random.nextInt(3) != 0) return false;
            carcass = freshKill();
            boolean rest = level().isBrightOutside() && onGround() && !level().getBlockState(blockPosition().below()).is(BlockTags.LEAVES);
            if (carcass == null && !rest) return false;
            trunk = nearestTrunk();
            top = trunk == null ? null : canopy(trunk);
            return top != null && getNavigation().moveTo(trunk.getX() + 0.5, trunk.getY(), trunk.getZ() + 0.5, carcass != null ? 0.8 : 1.0);
        }

        @Override
        public void start() {
            ticks = 600;
            climbing = false;
            done = false;
        }

        @Override
        public boolean canContinueToUse() {
            if (carcass != null && !carcass.isAlive()) carcass = null;
            return !done && getTarget() == null && (--ticks > 0 || climbing);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 foot = Vec3.atBottomCenterOf(trunk);
            if (!climbing) {
                if (carcass != null) drag(position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.0)));
                if (position().distanceToSqr(foot.x, getY(), foot.z) < 2.6 * 2.6) {
                    climbing = true;
                    getNavigation().stop();
                    noPhysics = true;
                    setNoGravity(true);
                } else if (getNavigation().isDone()) {
                    getNavigation().moveTo(foot.x, foot.y, foot.z, 1.0);
                }
                return;
            }
            // climb: straight up the trunk side, then over onto the canopy top
            Vec3 goal = Vec3.atBottomCenterOf(top);
            Vec3 at = position();
            Vec3 step = at.y < goal.y - 0.05 ? new Vec3((foot.x - at.x) * 0.1, 0.22, (foot.z - at.z) * 0.1) : goal.subtract(at).scale(0.25);
            setPos(at.add(step));
            setDeltaMovement(Vec3.ZERO);
            getLookControl().setLookAt(goal.x, goal.y + 1, goal.z);
            if (carcass != null) drag(position().add(0, 0.3, 0).add(Vec3.directionFromRotation(0, getYRot()).scale(0.6)));
            if (at.y >= goal.y - 0.05 && at.distanceToSqr(goal) < 0.05) {
                setPos(goal);
                noPhysics = false;
                setNoGravity(false);
                climbing = false;
                if (carcass != null) { // leave it lying in the branches beside the trunk top
                    carcass.addTag(CACHED_TAG);
                    carcass.setPos(goal.x, goal.y + 0.1, goal.z);
                    carcass = null;
                }
                playSound(SoundEvents.AZALEA_LEAVES_STEP, 1.0F, 0.8F);
                done = true; // up top: PerchGoal keeps it there by day
            }
        }

        /** The carcass hangs from its jaws. */
        private void drag(Vec3 mouth) {
            if (carcass == null) return;
            Vec3 move = mouth.subtract(carcass.position());
            if (climbing || level().noCollision(carcass, carcass.getBoundingBox().move(move))) carcass.setPos(carcass.position().add(move));
        }

        @Override
        public void stop() {
            if (climbing) { // interrupted on the way up: let go and drop
                noPhysics = false;
                setNoGravity(false);
                climbing = false;
            }
            carcass = null;
        }
    }

    /** Up on the canopy by day: lies low and stays put (dozing comes on its own), instead of wandering off the edge. */
    class PerchGoal extends Goal {
        PerchGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && level().isBrightOutside() && onGround() && level().getBlockState(blockPosition().below()).is(BlockTags.LEAVES);
        }

        @Override
        public void tick() {
            getNavigation().stop();
        }
    }
}
