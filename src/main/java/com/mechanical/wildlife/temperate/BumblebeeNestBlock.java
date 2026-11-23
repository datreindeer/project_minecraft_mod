package com.mechanical.wildlife.temperate;

import com.mechanical.Calendar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bumblebee Nest: a mossy tussock over an old mouse hole in the meadow, where a bumblebee colony keeps a few waxy pots of
 * honey (generated in the plains and meadows). In spring and summer the honey builds up ({@link #FILL_CHANCE} per random
 * tick, about once a minute, to {@link #MAX_HONEY}); right-click with an empty hand to take Wild Honey (one per level).
 * Bumblebees are gentle: sneak, and they leave you be; stand over them and they may sting ({@link #STING_CHANCE}).
 * In autumn and winter the colony is dying off and no honey is made.
 */
public class BumblebeeNestBlock extends Block {
    public static final int MAX_HONEY = 3;
    public static final IntegerProperty HONEY = IntegerProperty.create("honey", 0, MAX_HONEY);
    static final float FILL_CHANCE = 0.2F, STING_CHANCE = 0.5F;
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 6, 14);

    public BumblebeeNestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HONEY, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HONEY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** Spring and summer (seasons 0 and 1) make honey. */
    static boolean honeySeason(int season) {
        return season <= 1;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HONEY) < MAX_HONEY;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (honeySeason(Calendar.season(level.getOverworldClockTime())) && random.nextFloat() < FILL_CHANCE) {
            level.setBlock(pos, state.setValue(HONEY, state.getValue(HONEY) + 1), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HONEY) > 0 && level.isBrightOutside() && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.FALLING_NECTAR, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
            if (random.nextInt(6) == 0) level.playLocalSound(pos, SoundEvents.BEE_LOOP, SoundSource.BLOCKS, 0.3F, 0.7F, false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int honey = state.getValue(HONEY);
        if (honey == 0) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            player.getInventory().placeItemBackInInventory(new ItemStack(TemperateFauna.WILD_HONEY.get(), honey), Prediction.SERVER_ONLY);
            level.setBlock(pos, state.setValue(HONEY, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BEE_HURT, SoundSource.BLOCKS, 0.8F, 0.7F);
            if (!player.isShiftKeyDown() && !player.isCreative() && server.getRandom().nextFloat() < STING_CHANCE) {
                player.hurtServer(server, server.damageSources().sting(null), 1.0F);
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
