package com.mechanical.folklore;

import com.mechanical.EarthBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Myrmannen (Bog Body): a peat-blackened, mummified body that rises out of the peat in a Peat Bog at night (slowly, out
 * of the ground, over {@link #RISE_TICKS}) and shambles after the living. Unlike the spirits it is real: it can be killed
 * (loot entities/bog_body: Peat-Tanned Hide, sometimes a Bog Torc). At dawn it sinks back into the peat.
 */
public class BogBody extends Monster {
    static final int RISE_TICKS = 40;
    int rising;

    public BogBody(EntityType<? extends BogBody> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** At night on peat (the Peat Bog's ground); it may be dark already or just dusk. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, RandomSource random) {
        return level instanceof ServerLevelAccessor s && rule(s.getLevel().isDarkOutside(), level.getBlockState(pos.below()).is(EarthBlocks.PEAT.get()))
                && random.nextInt(3) == 0;
    }

    static boolean rule(boolean night, boolean peat) {
        return night && peat;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true; // the bog's own rule (canSpawn), not the monster light rule
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        if (reason == EntitySpawnReason.NATURAL) {
            rising = RISE_TICKS;
            setPos(getX(), getY() - 1.8, getZ());
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void aiStep() {
        if (rising > 0) {
            rising--;
            noPhysics = rising > 0;
            setNoAi(rising > 0);
            setPos(getX(), getY() + 1.8 / RISE_TICKS, getZ());
            if (level() instanceof ServerLevel level && rising % 4 == 0)
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, EarthBlocks.PEAT.get().defaultBlockState()), getX(), getY() + 1.8, getZ(), 8, 0.3, 0.1, 0.3, 0.05);
        }
        super.aiStep();
        if (level() instanceof ServerLevel level && tickCount % 40 == 0 && level.isBrightOutside() && level.canSeeSky(blockPosition())) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, EarthBlocks.PEAT.get().defaultBlockState()), getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            discard(); // sinks back into the peat
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.MUD_STEP, 0.3F, 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
