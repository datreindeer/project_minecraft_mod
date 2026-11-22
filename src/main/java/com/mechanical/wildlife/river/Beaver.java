package com.mechanical.wildlife.river;

import com.mechanical.Calendar;
import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.FallenLogBlock;
import com.mechanical.TreeFelling;

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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Beaver: a heavy, brown rodent with a flat scaly tail, at home by forest streams and lakes. Shy (flees, slaps its tail on
 * the water when it does). When mobGriefing is on it works its pond ({@link BeaverWorks}): gnaws down a small tree now
 * and then (felled like a player's tree: {@link TreeFelling}), gnaws the fallen logs into building stuff, and carries it
 * to a narrow stream to build a dam across it, which raises the water behind it if that pond is closed in, then a lodge.
 * <p>
 * Bounded and saved: its home is the Mob home (set once near water), the dam line and lodge centre are found once and
 * kept, it carries at most {@link #MAX_MATERIAL}, fells at most one small tree a day and checks for work about once a
 * second; dam and lodge together are ~30 blocks, after that it only repairs them.
 */
public class Beaver extends SemiAquatic {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/beaver.png");
    private static final EntityDataAccessor<Boolean> CARRYING = SynchedEntityData.defineId(Beaver.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GNAWING = SynchedEntityData.defineId(Beaver.class, EntityDataSerializers.BOOLEAN);
    static final int MAX_MATERIAL = 12, MATERIAL_PER_LOG = 3, HOME_RADIUS = 20;

    // saved work state (see BeaverWorks)
    BlockPos damStart, lodge, lastFell;
    Direction damDir = Direction.EAST;
    int damLength, material;
    long nextFell;
    boolean raised;
    /** Not saved: when the next search for a dam site / lodge spot may run (they scan the area). */
    long nextSiteSearch;
    /** Not saved: when it last felled a tree. */
    long fellAt;
    private int slapCooldown;

    public Beaver(EntityType<? extends Beaver> type, Level level) {
        super(type, level, 0.12F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof WaterAvoidingRandomStrollGoal);
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8)); // in and out of the water, around its home
        goalSelector.addGoal(5, new WorkGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRYING, false);
        builder.define(GNAWING, false);
    }

    /** Has sticks and mud in its mouth (model). */
    public boolean isCarrying() {
        return entityData.get(CARRYING);
    }

    /** Gnawing a trunk or log (model). */
    public boolean isGnawing() {
        return entityData.get(GNAWING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (slapCooldown > 0) slapCooldown--;
        // alarm: a loud slap of the tail on the water as it dives
        if (isInWater() && slapCooldown == 0 && AnimalFear.isPanicking(this)) {
            slapCooldown = 120;
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.4F, 0.7F);
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 12, 0.3, 0.1, 0.3, 0.1);
        }
        if (!hasHome() && !isBaby() && (tickCount + getId()) % 200 == 0) {
            BlockPos water = BeaverWorks.findWater(level, blockPosition(), 6);
            if (water != null) setHomeTo(water, HOME_RADIUS);
        }
    }

    static boolean griefing(ServerLevel level) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("DamStart", BlockPos.CODEC, damStart);
        output.putInt("DamDir", damDir.get3DDataValue());
        output.putInt("DamLength", damLength);
        output.storeNullable("Lodge", BlockPos.CODEC, lodge);
        output.storeNullable("LastFell", BlockPos.CODEC, lastFell);
        output.putInt("Material", material);
        output.putLong("NextFell", nextFell);
        output.putBoolean("Raised", raised);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        damStart = input.read("DamStart", BlockPos.CODEC).orElse(null);
        damDir = Direction.from3DDataValue(input.getIntOr("DamDir", Direction.EAST.get3DDataValue()));
        damLength = input.getIntOr("DamLength", 0);
        lodge = input.read("Lodge", BlockPos.CODEC).orElse(null);
        lastFell = input.read("LastFell", BlockPos.CODEC).orElse(null);
        material = input.getIntOr("Material", 0);
        nextFell = input.getLongOr("NextFell", 0);
        raised = input.getBooleanOr("Raised", false);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.35F; // a family: the pair and a kit or two
    }

    @Override
    public double fearMultiplier() {
        return 1.2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isGnawing() ? null : SoundEvents.RABBIT_AMBIENT;
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
        return super.getVoicePitch() * 0.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    /** Gnaw a tree down, gnaw a fallen log into building stuff, or carry stuff to the dam / lodge and pack it in. */
    static class WorkGoal extends Goal {
        private enum Task { FELL, LOG, BUILD }
        private static final int TIMEOUT = 600, FELL_TICKS = 200, LOG_TICKS = 100;
        private final Beaver beaver;
        private Task task;
        private BlockPos target;
        private int ticks, gnawed;
        private boolean done;

        WorkGoal(Beaver beaver) {
            this.beaver = beaver;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // about once a second (goals only tick on even tickCount + id), and only a grown beaver at peace
            if ((beaver.tickCount + beaver.getId()) % 20 != 0 || beaver.isBaby() || beaver.isDozing() || !beaver.hasHome()
                    || AnimalFear.isPanicking(beaver) || beaver.getRandom().nextInt(3) != 0
                    || !(beaver.level() instanceof ServerLevel level) || !griefing(level)) return false;
            long now = level.getGameTime();
            if (beaver.material > 0) {
                target = BeaverWorks.nextBuild(level, beaver);
                task = Task.BUILD;
            } else if (beaver.lastFell != null) {
                target = BeaverWorks.findFallenLog(level, beaver.lastFell);
                if (target == null && now - beaver.fellAt > 100) beaver.lastFell = null; // (the tree takes a moment to come down)
                task = Task.LOG;
            } else if (now >= beaver.nextFell) {
                target = BeaverWorks.findTree(level, beaver.getHomePosition(), beaver.getRandom());
                task = Task.FELL;
                if (target == null) beaver.nextFell = now + 2400; // nothing small enough nearby: look again later
            } else {
                target = null;
            }
            return target != null;
        }

        @Override
        public void start() {
            ticks = gnawed = 0;
            done = false;
            beaver.entityData.set(CARRYING, task == Task.BUILD);
            moveTo();
        }

        private void moveTo() {
            BlockPos to = target;
            if (task != Task.BUILD) {
                // a trunk or log is solid: walk to open ground beside it, the side nearest the beaver first
                double best = Double.MAX_VALUE;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos side = target.relative(d);
                    if (beaver.level().getBlockState(side).getCollisionShape(beaver.level(), side).isEmpty()
                            && side.distSqr(beaver.blockPosition()) < best) {
                        best = side.distSqr(beaver.blockPosition());
                        to = side;
                    }
                }
            }
            beaver.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return !done && ticks < TIMEOUT && !AnimalFear.isPanicking(beaver) && !beaver.isDozing();
        }

        @Override
        public void stop() {
            beaver.entityData.set(CARRYING, false);
            beaver.entityData.set(GNAWING, false);
            beaver.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            ServerLevel level = (ServerLevel) beaver.level();
            Vec3 centre = Vec3.atCenterOf(target);
            double reach = 3.0; // the path stops within a block of where it was sent
            if (beaver.position().distanceToSqr(centre.x, beaver.getY(), centre.z) > reach * reach
                    || Math.abs(beaver.getY() - target.getY()) > 2.5) {
                if (beaver.getNavigation().isDone() && ticks % 20 == 0) moveTo();
                return;
            }
            beaver.getNavigation().stop();
            beaver.getLookControl().setLookAt(centre.x, centre.y, centre.z);
            if (task == Task.BUILD) {
                if (BeaverWorks.place(level, beaver, target)) beaver.material--;
                done = true;
                return;
            }
            // gnawing: chips fly, teeth rasp
            beaver.entityData.set(GNAWING, true);
            gnawed++;
            BlockState state = level.getBlockState(target);
            if (state.isAir()) state = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(); // a felled log's block (an entity)
            if (gnawed % 8 == 0) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), centre.x, target.getY() + 0.4, centre.z, 5, 0.3, 0.2, 0.3, 0.05);
                level.playSound(null, target, SoundEvents.AXE_STRIP.value(), SoundSource.NEUTRAL, 0.45F, 1.5F + beaver.getRandom().nextFloat() * 0.3F);
            }
            if (task == Task.FELL && gnawed >= FELL_TICKS) {
                TreeFelling.Tree tree = BeaverWorks.smallTree(level, target);
                if (tree != null) {
                    // it falls towards the water, the way beavers bring their trees down
                    BlockPos home = beaver.getHomePosition();
                    Direction fall = Direction.getApproximateNearest(home.getX() - target.getX(), 0, home.getZ() - target.getZ());
                    if (fall.getAxis().isVertical()) fall = beaver.getDirection();
                    TreeFelling.fell(level, target, tree, fall, beaver, ItemStack.EMPTY);
                    beaver.lastFell = target;
                    beaver.fellAt = level.getGameTime();
                }
                beaver.nextFell = level.getGameTime() + Calendar.DAY + beaver.getRandom().nextInt(12000);
                done = true;
            } else if (task == Task.LOG && gnawed >= LOG_TICKS) {
                if (state.getBlock() instanceof FallenLogBlock) {
                    level.destroyBlock(target, false, beaver);
                    beaver.material = Math.min(MAX_MATERIAL, beaver.material + MATERIAL_PER_LOG);
                } else if (com.mechanical.FelledLogEntity.removeAt(level, target)) {
                    beaver.material = Math.min(MAX_MATERIAL, beaver.material + MATERIAL_PER_LOG);
                }
                done = true;
            }
        }
    }
}

