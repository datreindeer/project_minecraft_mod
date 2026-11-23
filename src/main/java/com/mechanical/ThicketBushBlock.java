package com.mechanical;

import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thorny berry bush that grows into thickets (docs/flora-and-food-plan.md, section 4). A {@link #YOUNG} bush is small
 * and bears nothing; grown, it follows the season ({@link #target}: leaves, flowers, unripe, ripe) one stage at a time.
 * Grown bushes of the same kind side by side join up ({@link #SIDES}, like a fence) into one mass of leaves; one with at
 * least two such neighbours can grow a second block on top ({@link #HALF}), so a thicket is tall in the middle, and grown
 * bushes now and then put up a young one beside them. Growing and spreading only happen where the biome's temperature
 * suits the kind. Where snow lies in winter the leaves fall ({@link #LEAFLESS}); hardy kinds keep their berries on the
 * bare twigs. Picked in its ripe season a bush goes back to unripe and ripens again. FROZEN (from the parent) stays false.
 */
public class ThicketBushBlock extends WildBerryBushBlock {
    public static final BooleanProperty YOUNG = BooleanProperty.create("young"), LEAFLESS = BooleanProperty.create("leafless");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, BlockStateProperties.NORTH,
            Direction.EAST, BlockStateProperties.EAST, Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST, BlockStateProperties.WEST);
    /** Per random tick (~68 s): a young bush grows up, a stage moves on, a grown bush spreads or grows a top. */
    static final int GROW_ONE_IN = 8, STAGE_ONE_IN = 5, SPREAD_ONE_IN = 60, TALL_ONE_IN = 40;
    /** No new young bush when this many of the kind already stand within 2 blocks. */
    static final int MAX_CLUSTER = 9;
    /** How far outside its temperature window (biome base temperature) a bush still grows. */
    static final float TEMP_SLACK = 0.15F;
    private static final VoxelShape YOUNG_SHAPE = column(8, 0, 9), LOWER = column(14, 0, 15), UPPER = column(12, 0, 12);

    private final int[] target;
    private final float minTemp, maxTemp;
    private final boolean sand;

    /** target: the stage wanted in spring, summer, autumn, winter; minTemp..maxTemp: biome base temperatures it grows in. */
    public ThicketBushBlock(Supplier<Item> berries, int[] target, float minTemp, float maxTemp, boolean sand, Properties properties) {
        super(berries, true, properties);
        this.target = target;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.sand = sand;
        BlockState s = defaultBlockState().setValue(AGE, 0).setValue(YOUNG, false).setValue(LEAFLESS, false).setValue(HALF, DoubleBlockHalf.LOWER);
        for (BooleanProperty side : SIDES.values()) s = s.setValue(side, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(YOUNG, LEAFLESS, HALF, BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    /** The stage this kind wants in a season (0 spring .. 3 winter). */
    public int target(int season) {
        return target[season];
    }

    public boolean suits(float baseTemperature) {
        return baseTemperature >= minTemp - TEMP_SLACK && baseTemperature <= maxTemp + TEMP_SLACK;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(YOUNG) ? YOUNG_SHAPE : state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER : LOWER;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || sand && Flora.dryGround(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) return super.canSurvive(state, level, pos);
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER && !below.getValue(YOUNG);
    }

    /** Grown bushes of this kind on the same level join up. */
    boolean joins(BlockState self, BlockState other) {
        return other.is(this) && !self.getValue(YOUNG) && !other.getValue(YOUNG) && other.getValue(HALF) == self.getValue(HALF);
    }

    public BlockState withSides(LevelReader level, BlockPos pos, BlockState state) {
        for (var e : SIDES.entrySet()) state = state.setValue(e.getValue(), joins(state, level.getBlockState(pos.relative(e.getKey()))));
        return state;
    }

    public static int sides(BlockState state) {
        int n = 0;
        for (BooleanProperty side : SIDES.values()) if (state.getValue(side)) n++;
        return n;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return withSides(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
            BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        BooleanProperty side = SIDES.get(dir);
        return side == null ? state : state.setValue(side, joins(state, neighbor));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int season = Calendar.season(level.getOverworldClockTime());
        boolean bare = season == 3 && wintry(level, pos);
        boolean growing = season != 3 && suits(level.getBiome(pos).value().getBaseTemperature()) && level.getRawBrightness(pos.above(), 0) >= 9;
        BlockState was = state;
        state = state.setValue(LEAFLESS, bare);
        if (state.getValue(YOUNG)) {
            if (growing && random.nextInt(GROW_ONE_IN) == 0) {
                level.setBlock(pos, withSides(level, pos, state.setValue(YOUNG, false)), Block.UPDATE_ALL); // neighbours join it
                return;
            }
        } else {
            int age = state.getValue(AGE), want = target[season];
            if (age > want && random.nextInt(STAGE_ONE_IN) == 0) state = state.setValue(AGE, age - 1); // flowers fade, berries fall
            else if (age < want && growing && random.nextInt(STAGE_ONE_IN) == 0) state = state.setValue(AGE, age + 1);
        }
        if (state != was) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        if (!growing || state.getValue(YOUNG) || state.getValue(HALF) == DoubleBlockHalf.UPPER) return;
        if (random.nextInt(SPREAD_ONE_IN) == 0) spread(level, pos, random);
        else if (sides(state) >= 2 && random.nextInt(TALL_ONE_IN) == 0 && level.getBlockState(pos.above()).isAir()) {
            BlockPos up = pos.above();
            level.setBlock(up, withSides(level, up, state.setValue(HALF, DoubleBlockHalf.UPPER)), Block.UPDATE_ALL);
        }
    }

    /** Puts up a young bush on free ground beside this one, unless the thicket is already full. */
    void spread(ServerLevel level, BlockPos pos, RandomSource random) {
        int near = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.is(this) && s.getValue(HALF) == DoubleBlockHalf.LOWER && ++near >= MAX_CLUSTER) return;
        }
        BlockPos side = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
        for (int dy : new int[] {0, 1, -1}) {
            BlockPos p = side.above(dy);
            BlockState here = level.getBlockState(p);
            if (here.canBeReplaced() && here.getFluidState().isEmpty() && !here.is(this) && mayPlaceOn(level.getBlockState(p.below()), level, p.below())) {
                level.setBlock(p, defaultBlockState().setValue(YOUNG, true), Block.UPDATE_ALL);
                return;
            }
        }
    }

    /** In the ripe season it ripens again; picked in winter (nothing grows) it stays bare. */
    @Override
    protected int ageAfterPicking(ServerLevel level) {
        return Calendar.season(level.getOverworldClockTime()) == 3 ? 0 : 2;
    }

    /** Thorns: slows you down, and pricks now and then unless you step carefully (as Bramble). */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (!(entity instanceof LivingEntity) || state.getValue(YOUNG)) return;
        entity.makeStuckInBlock(state, new Vec3(0.8F, 0.75, 0.8F));
        if (level instanceof ServerLevel server && !entity.isSteppingCarefully() && level.getRandom().nextInt(8) == 0) {
            Vec3 move = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
            if (move.horizontalDistanceSqr() > 1.0E-5) entity.hurtServer(server, level.damageSources().sweetBerryBush(), 1.0F);
        }
    }
}
