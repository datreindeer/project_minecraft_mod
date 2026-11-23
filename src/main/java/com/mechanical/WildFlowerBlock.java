package com.mechanical;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wild flower that blooms in its seasons ({@link Bloom}) and is a plain green plant the rest of the year
 * ({@link #BLOOMING}, checked on random ticks). Desert kinds also grow on sand. A magic kind glows and gives off motes
 * while it blooms (light level in its block properties, particle here). Plants drop themselves (loot table); the magic
 * ones only when picked in bloom (docs/flora-and-food-plan.md, section 3).
 */
public class WildFlowerBlock extends VegetationBlock {
    public static final BooleanProperty BLOOMING = BooleanProperty.create("blooming");

    /** When a kind blooms: seasons is a bit mask (1 spring, 2 summer, 4 autumn, 8 winter); night = only after dark. */
    public record Bloom(int seasons, boolean night) {
        public boolean at(int season, boolean dark) {
            return (seasons >> season & 1) != 0 && (!night || dark);
        }

        public boolean now(Level level) {
            return at(Calendar.season(level.getOverworldClockTime()), level.isDarkOutside());
        }
    }

    private final Bloom bloom;
    private final boolean dry;
    private final VoxelShape shape;
    private final @Nullable ParticleOptions glow;

    public WildFlowerBlock(Bloom bloom, boolean dry, int height, @Nullable ParticleOptions glow, Properties properties) {
        super(properties);
        this.bloom = bloom;
        this.dry = dry;
        this.shape = column(10, 0, height);
        this.glow = glow;
        registerDefaultState(defaultBlockState().setValue(BLOOMING, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BLOOMING);
    }

    public Bloom bloom() {
        return bloom;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || dry && Flora.dryGround(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BLOOMING, bloom.now(context.getLevel()));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean now = bloom.now(level);
        if (state.getValue(BLOOMING) != now) level.setBlock(pos, state.setValue(BLOOMING, now), Block.UPDATE_CLIENTS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (glow == null || !state.getValue(BLOOMING) || random.nextInt(7) != 0) return; // a few slow motes, not a fountain
        level.addParticle(glow, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) * 0.7
                + random.nextDouble() * 0.3, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.004 + random.nextDouble() * 0.006, 0);
    }
}
