package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A woven stick basket with a funnel mouth, set in water. On random ticks (~68 s each) it may catch a fish, up to
 * {@link #CAPACITY}; right-click to take them (whole fish: gut them with a knife). Only in a real body of water
 * ({@link #MIN_WATER}+ water blocks around it); rivers and moving water catch the most. The species follows the biome
 * and is picked when the trap is emptied.
 */
public class FishTrapBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int CAPACITY = 3;
    public static final IntegerProperty FISH = IntegerProperty.create("fish", 0, CAPACITY);
    /** Water blocks needed within 3 blocks sideways and 2 down; a small pond has no fish to catch. */
    static final int MIN_WATER = 12;
    /** Catch chance per random tick: still lake/pond water, then multipliers. Capped at {@link #MAX_CHANCE}. */
    static final float BASE_CHANCE = 0.2F, RIVER = 2.0F, FLOWING = 1.5F, SEA = 1.25F, MAX_CHANCE = 0.6F;
    private static final VoxelShape SHAPE = box(2, 0, 1, 14, 11, 15);

    public FishTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(FISH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, FISH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // set in still or flowing water (it holds a water source then); on land it is just a basket that catches nothing
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).is(FluidTags.WATER);
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, water);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(WATERLOGGED) && state.getValue(FISH) < CAPACITY;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < catchChance(level, pos)) level.setBlock(pos, state.setValue(FISH, state.getValue(FISH) + 1), Block.UPDATE_ALL);
    }

    /** Chance per random tick to catch a fish here, 0 in too little water. */
    static float catchChance(Level level, BlockPos pos) {
        if (waterAround(level, pos) < MIN_WATER) return 0;
        Holder<Biome> biome = level.getBiome(pos);
        float chance = BASE_CHANCE;
        if (biome.is(BiomeTags.IS_RIVER)) chance *= RIVER;
        else if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) chance *= SEA;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            FluidState f = level.getFluidState(pos.relative(d));
            if (f.is(FluidTags.WATER) && !f.isSource()) {
                chance *= FLOWING;
                break;
            }
        }
        return Math.min(chance, MAX_CHANCE);
    }

    static int waterAround(LevelReader level, BlockPos pos) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 0, 3))) {
            if (level.getFluidState(p).is(FluidTags.WATER) && ++n >= MIN_WATER) return n;
        }
        return n;
    }

    /** River: salmon 70 %, cod 30 %. Warm seas: tropical fish 50 %, pufferfish 10 %, cod 40 %. Elsewhere: cod 70 %, salmon 30 %. */
    static Item fishFor(Level level, BlockPos pos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(pos);
        float r = random.nextFloat();
        if (biome.is(BiomeTags.IS_RIVER)) return (r < 0.7F ? Mechanical.WHOLE_SALMON : Mechanical.WHOLE_COD).get();
        if (biome.is(Biomes.WARM_OCEAN) || biome.is(Biomes.LUKEWARM_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)) {
            return (r < 0.5F ? Mechanical.WHOLE_TROPICAL_FISH : r < 0.6F ? Mechanical.WHOLE_PUFFERFISH : Mechanical.WHOLE_COD).get();
        }
        return (r < 0.7F ? Mechanical.WHOLE_COD : Mechanical.WHOLE_SALMON).get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(FISH) == 0) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            for (int i = 0; i < state.getValue(FISH); i++) {
                player.getInventory().placeItemBackInInventory(new ItemStack(fishFor(level, pos, level.getRandom())), Prediction.SERVER_ONLY);
            }
            level.setBlock(pos, state.setValue(FISH, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // the catch spills out when the trap is broken
        if (!level.isClientSide() && !player.isCreative()) {
            for (int i = 0; i < state.getValue(FISH); i++) popResource(level, pos, new ItemStack(fishFor(level, pos, level.getRandom())));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
