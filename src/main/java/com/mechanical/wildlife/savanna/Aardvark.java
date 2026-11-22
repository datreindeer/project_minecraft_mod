package com.mechanical.wildlife.savanna;

import java.util.EnumSet;

import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Aardvark: a shy, pig-sized digger with long ears, a tubular snout and a thick tail, out only at night (it sleeps by
 * day). It snuffles its way to a Termite Mound within {@link #RANGE} blocks, digs at its foot for a while and leaves a
 * hole in the ground beside the mound ({@link DigGoal}). Bolts from people like other game.
 */
public class Aardvark extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/aardvark.png");
    private static final int RANGE = 24, DIG_TICKS = 80;

    public Aardvark(EntityType<? extends Aardvark> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new DigGoal());
    }

    @Override
    protected boolean sleepTime() {
        return level().isBrightOutside() && !entityTags().contains(SavannaFauna.ALWAYS_ACTIVE);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public double fearMultiplier() {
        return 1.4;
    }

    @Override
    protected float babyChance() {
        return 0.2F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // grunts only when hurt
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIG_SOUNDS.get(net.minecraft.world.entity.animal.pig.PigSoundVariants.SoundSet.MINI).adultSounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_SOUNDS.get(net.minecraft.world.entity.animal.pig.PigSoundVariants.SoundSet.MINI).adultSounds().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.ARMADILLO_STEP, 0.15F, 0.8F);
    }

    /** Natural ground beside a mound that can be dug out into a hole (open sky above it). */
    static boolean diggable(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.COARSE_DIRT)) && level.getBlockState(p.above()).isAir();
    }

    /** At night: to the nearest termite mound, dig at its foot for 4 s (head down, dirt flying), and leave a hole beside it. */
    class DigGoal extends Goal {
        private BlockPos mound, hole;
        private int ticks;

        DigGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (sleepTime() || isDozing() || getTarget() != null || random.nextInt(160) != 0) return false;
            BlockPos me = blockPosition();
            mound = null;
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-RANGE, -4, -RANGE), me.offset(RANGE, 4, RANGE))) {
                if (level().getBlockState(p).is(SavannaFauna.TERMITE_MOUND.get()) && !level().getBlockState(p.below()).is(SavannaFauna.TERMITE_MOUND.get())
                        && (mound == null || p.distSqr(me) < mound.distSqr(me))) mound = p.immutable();
            }
            if (mound == null) return false;
            hole = null;
            for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(random)) {
                BlockPos p = mound.relative(d).below();
                if (diggable(level(), p)) {
                    hole = p;
                    break;
                }
            }
            return hole != null && getNavigation().moveTo(hole.getX() + 0.5, hole.getY() + 1, hole.getZ() + 0.5, 1.0);
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < DIG_TICKS + 200 && getTarget() == null && !com.mechanical.AnimalFear.isPanicking(Aardvark.this) && diggable(level(), hole);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            if (distanceToSqr(hole.getX() + 0.5, hole.getY() + 1, hole.getZ() + 0.5) > 2.5) {
                if (getNavigation().isDone()) getNavigation().moveTo(hole.getX() + 0.5, hole.getY() + 1, hole.getZ() + 0.5, 1.0);
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(mound.getX() + 0.5, mound.getY(), mound.getZ() + 0.5);
            startGrazing(); // head down in the dirt
            if (ticks % 8 == 0 && level() instanceof ServerLevel server) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(hole)), hole.getX() + 0.5, hole.getY() + 1.1,
                        hole.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.1);
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 0.6F, 0.9F);
            }
            if (ticks >= DIG_TICKS) {
                level().setBlock(hole, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                ticks = DIG_TICKS + 1000; // done
            }
        }
    }
}
