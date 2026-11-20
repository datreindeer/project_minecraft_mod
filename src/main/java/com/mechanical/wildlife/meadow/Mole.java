package com.mechanical.wildlife.meadow;

import com.mechanical.AnimalFear;
import com.mechanical.TrampledPathBlock;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Mole: velvety slate-black, a pink pointed snout, huge pink spade-like forepaws, eyes lost in the fur. It lives under the
 * meadow: burrowed ({@link #isBurrowed()}) it can't be seen or hit and tunnels along slowly, now and then pushing up a
 * {@link MolehillBlock} - on farmland that uproots the crop and turns the soil back to dirt, on a path it breaks the path
 * up. Now and then (mostly at night) it surfaces to forage for a while, and dives back down when it notices a player or
 * gets a fright. Breaking a fresh molehill flushes a mole close by out of its tunnel. A Snare set over its runs catches it
 * underground too. Gives the fine velvet Moleskin. Respects the mobGriefing game rule.
 */
public class Mole extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/mole.png");
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(Mole.class, EntityDataSerializers.BOOLEAN);
    /** Counting down while digging in (synced for the digging animation). */
    private static final EntityDataAccessor<Integer> DIGGING = SynchedEntityData.defineId(Mole.class, EntityDataSerializers.INT);
    public static final int DIG_TICKS = 30;
    /** Between molehill tries while burrowed (ticks), and no two molehills closer than this (blocks). */
    private static final int HILL_INTERVAL = 200, HILL_SPACING = 2;
    /** Chance per tick (x2: goal-free, checked every tick) to come up to forage: at night, and much less by day. */
    private static final float SURFACE_NIGHT = 1 / 1500F, SURFACE_DAY = 1 / 9000F;
    private int surfaceTime;

    public Mole(EntityType<? extends Mole> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.16);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    /** 0..1 progress of digging in, 0 when not digging. */
    public float digProgress() {
        int d = entityData.get(DIGGING);
        return d <= 0 ? 0 : 1 - d / (float) DIG_TICKS;
    }

    void setBurrowed(boolean burrowed) {
        entityData.set(BURROWED, burrowed);
        entityData.set(DIGGING, 0);
        setInvisible(burrowed);
        if (!burrowed) surfaceTime = 200 + random.nextInt(400);
    }

    /** Ground a mole can tunnel in. */
    static boolean diggable(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.FARMLAND) || s.is(Blocks.DIRT_PATH) || s.getBlock() instanceof TrampledPathBlock;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
        builder.define(DIGGING, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (reason != EntitySpawnReason.SPAWN_ITEM_USE && reason != EntitySpawnReason.COMMAND) setBurrowed(true); // found in its tunnels
        return data;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        MeadowFauna.removeGoals(goalSelector, "FollowHerdGoal", "DozeGoal");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        int dig = entityData.get(DIGGING);
        if (dig > 0) {
            getNavigation().stop();
            BlockState ground = level.getBlockState(blockPosition().below());
            if (dig % 4 == 0) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 4, 0.15, 0.05, 0.15, 0.05);
                level.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_HIT, SoundSource.NEUTRAL, 0.5F, 1.2F);
            }
            if (dig == 1) {
                setBurrowed(true);
                level.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.NEUTRAL, 0.7F, 1.0F);
            } else entityData.set(DIGGING, dig - 1);
            return;
        }
        boolean onDiggable = onGround() && diggable(level.getBlockState(blockPosition().below()));
        if (isBurrowed()) {
            if (onGround() && !onDiggable || isInWater()) {
                setBurrowed(false); // tunnelled out onto stone, sand or water: has to come up
                return;
            }
            if ((tickCount + getId()) % HILL_INTERVAL == 0 && random.nextInt(2) == 0) pushUpHill(level);
            if (random.nextFloat() < (level.isDarkOutside() ? SURFACE_NIGHT : SURFACE_DAY)) surface(level);
            return;
        }
        // above ground: forage a while, dive back down when frightened or done
        Player near = level.getNearestPlayer(this, 6);
        boolean scared = AnimalFear.isPanicking(this) || near != null && !near.isCreative() && !near.isSpectator() && !near.isCrouching();
        if (--surfaceTime <= 0 || scared) {
            if (onDiggable) entityData.set(DIGGING, DIG_TICKS);
        }
    }

    void surface(ServerLevel level) {
        setBurrowed(false);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockPosition().below())),
                getX(), getY() + 0.1, getZ(), 10, 0.2, 0.1, 0.2, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.NEUTRAL, 0.7F, 1.2F);
    }

    /** Flushed out of its tunnel (its molehill dug up): pops up and bolts. */
    void flush(ServerLevel level, BlockPos from) {
        surface(level);
        surfaceTime = 100;
        AnimalFear.panic(this, net.minecraft.world.phys.Vec3.atCenterOf(from));
    }

    /** Pushes up a molehill where it is, disturbing farmland and paths underneath. */
    private void pushUpHill(ServerLevel level) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) return;
        BlockPos pos = blockPosition(), below = pos.below();
        BlockState ground = level.getBlockState(below), here = level.getBlockState(pos);
        if (!diggable(ground) || nearHill(level, pos)) return;
        boolean crop = ground.is(Blocks.FARMLAND) && !here.isAir() && !here.canBeReplaced();
        if (!here.isAir() && !here.canBeReplaced() && !crop) return;
        if (crop) level.destroyBlock(pos, true, this); // the crop is uprooted and pops off
        if (ground.is(Blocks.FARMLAND) || ground.is(Blocks.DIRT_PATH) || ground.getBlock() instanceof TrampledPathBlock) {
            level.setBlockAndUpdate(below, Blocks.DIRT.defaultBlockState());
        }
        BlockState hill = MeadowFauna.MOLEHILL.get().defaultBlockState();
        if (!hill.canSurvive(level, pos)) return;
        level.setBlockAndUpdate(pos, hill);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.2,
                pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.05);
    }

    private static boolean nearHill(LevelReader level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-HILL_SPACING, -1, -HILL_SPACING), pos.offset(HILL_SPACING, 1, HILL_SPACING))) {
            if (level.getBlockState(p).is(MeadowFauna.MOLEHILL.get())) return true;
        }
        return false;
    }

    // ---- burrowed: out of sight and reach ----

    @Override
    public boolean isPickable() {
        return !isBurrowed() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !isBurrowed() && super.isPushable();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // under the ground only what reaches it there hurts it (suffocation, a snare's kill, /kill)
        if (isBurrowed() && source.getEntity() != null) return false;
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void die(DamageSource source) {
        if (isBurrowed()) setBurrowed(false); // the body comes up (snared in its run)
        super.die(source);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Burrowed", isBurrowed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setBurrowed(input.getBooleanOr("Burrowed", false));
    }

    @Override
    protected boolean sleepTime() {
        return false; // it works in bursts round the clock
    }

    @Override
    public float getSpeed() {
        return super.getSpeed() * (isBurrowed() ? 0.7F : 1.0F);
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
        return 0.6; // nearly blind
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBurrowed() ? null : SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
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

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
