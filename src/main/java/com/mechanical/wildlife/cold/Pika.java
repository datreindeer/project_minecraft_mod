package com.mechanical.wildlife.cold;

import java.util.EnumSet;

import com.mechanical.Calendar;
import com.mechanical.wildlife.GuanoPileBlock;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pika: a little round, tailless cousin of the hare that lives among the rocks of the high slopes and squeaks at anyone
 * coming near. Through summer and autumn (seasons 1 and 2) it makes hay: now and then it carries a mouthful to its
 * Pika Hay Pile and the pile grows a layer (up to 4), or it starts a new one beside it - never more than
 * {@link #MAX_PILES} within 8 blocks. The hay is good fodder, and 9 make a Hay Bale ({@link #nextPile}).
 */
public class Pika extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/pika.png");
    static final int MAX_PILES = 3, PILE_RANGE = 8;

    public Pika(EntityType<? extends Pika> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(5, new HayGoal());
    }

    /** Hay-making time: summer and autumn. */
    public static boolean haySeason(long clock) {
        int s = Calendar.season(clock);
        return s == 1 || s == 2;
    }

    /**
     * Where the next mouthful goes, near {@code at}: the nearest pile of fewer than 4 layers within PILE_RANGE, else (if
     * there are fewer than MAX_PILES) the spot itself when it is free and on solid ground; or null.
     */
    static BlockPos nextPile(Level level, BlockPos at) {
        BlockPos best = null;
        int piles = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-PILE_RANGE, -2, -PILE_RANGE), at.offset(PILE_RANGE, 2, PILE_RANGE))) {
            BlockState s = level.getBlockState(p);
            if (!s.is(ColdFauna.PIKA_HAY_PILE.get())) continue;
            piles++;
            if (s.getValue(GuanoPileBlock.LAYERS) < GuanoPileBlock.MAX_LAYERS && (best == null || p.distSqr(at) < best.distSqr(at))) best = p.immutable();
        }
        if (best != null) return best;
        BlockState pile = ColdFauna.PIKA_HAY_PILE.get().defaultBlockState();
        return piles < MAX_PILES && level.getBlockState(at).canBeReplaced() && level.getFluidState(at).isEmpty() && pile.canSurvive(level, at) ? at : null;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    @Override
    public double fearMultiplier() {
        return 1.3;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT; // the sharp "eek" of the alarm call
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.6F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    /** Walks to the pile (or the new spot) and adds its mouthful of grass there. */
    class HayGoal extends Goal {
        private BlockPos pile;
        private int ticks;

        HayGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || isDozing() || random.nextInt(600) != 0 || !haySeason(level().getOverworldClockTime())) return false;
            pile = nextPile(level(), blockPosition());
            return pile != null && getNavigation().moveTo(pile.getX() + 0.5, pile.getY(), pile.getZ() + 0.5, 1.0);
        }

        @Override
        public void start() {
            ticks = 200;
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && pile != null;
        }

        @Override
        public void tick() {
            if (distanceToSqr(pile.getX() + 0.5, pile.getY(), pile.getZ() + 0.5) > 2.0) return;
            stack(level(), pile);
            playSound(SoundEvents.GRASS_PLACE, 0.6F, 1.4F);
            pile = null;
        }
    }

    /** One more layer on the pile at {@code pos}, or a new pile there. */
    static void stack(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.is(ColdFauna.PIKA_HAY_PILE.get())) {
            if (s.getValue(GuanoPileBlock.LAYERS) < GuanoPileBlock.MAX_LAYERS) level.setBlock(pos, s.setValue(GuanoPileBlock.LAYERS, s.getValue(GuanoPileBlock.LAYERS) + 1), Block.UPDATE_ALL);
        } else if (s.canBeReplaced()) {
            level.setBlock(pos, ColdFauna.PIKA_HAY_PILE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
