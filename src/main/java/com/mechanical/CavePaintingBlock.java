package com.mechanical;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A painted motif on one block face (cave painting): a thin layer in the air block in front of the face, pointing
 * {@link #FACING} away from it. Painted with a {@link PaintbrushItem} and {@link PaintItem}; scenes are two blocks wide.
 * No drops; scraped off fast with a knife. Under open sky, rain washes it away on random ticks.
 */
public class CavePaintingBlock extends Block {
    public enum Colour implements StringRepresentable {
        BLACK, RED, YELLOW, WHITE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Motifs; a two-block scene is a LEFT part (what the brush selects) followed by its RIGHT part. */
    public enum Motif implements StringRepresentable {
        AUROCHS, HORSE, DEER, BEAR, MAMMOTH, IBEX, HUNTER, SUN, SPIRAL, DOTS, HAND, HUNT_LEFT, HUNT_RIGHT, HERD_LEFT, HERD_RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean scene() {
            return this == HUNT_LEFT || this == HERD_LEFT;
        }

        /** What the brush cycles through: every single motif except the hand print, and the left part of each scene. */
        public boolean brushable() {
            return this != HAND && !name().endsWith("_RIGHT");
        }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Colour> COLOUR = EnumProperty.create("colour", Colour.class);
    public static final EnumProperty<Motif> MOTIF = EnumProperty.create("motif", Motif.class);
    private static final float RAIN_FADE = 1 / 6F;
    private static final VoxelShape[] SHAPES = new VoxelShape[6]; // by FACING (the layer lies against the opposite side)

    static {
        SHAPES[Direction.DOWN.ordinal()] = box(0, 15.5, 0, 16, 16, 16);
        SHAPES[Direction.UP.ordinal()] = box(0, 0, 0, 16, 0.5, 16);
        SHAPES[Direction.NORTH.ordinal()] = box(0, 0, 15.5, 16, 16, 16);
        SHAPES[Direction.SOUTH.ordinal()] = box(0, 0, 0, 16, 16, 0.5);
        SHAPES[Direction.WEST.ordinal()] = box(15.5, 0, 0, 16, 16, 16);
        SHAPES[Direction.EAST.ordinal()] = box(0, 0, 0, 0.5, 16, 16);
    }

    public CavePaintingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(COLOUR, Colour.RED).setValue(MOTIF, Motif.AUROCHS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLOUR, MOTIF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        return player.getMainHandItem().getItem() instanceof KnifeItem ? progress * 4 : progress;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supported(level, pos, state.getValue(FACING));
    }

    private static boolean supported(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos) && random.nextFloat() < RAIN_FADE) level.removeBlock(pos, false);
    }

    /**
     * Paints {@code motif} (a scene also takes the block to its right) where the player clicked: on the {@code face} of
     * a solid block, or over a painting already there. Returns how many blocks were painted (0 if it does not fit).
     */
    public static int paint(Level level, BlockPos clicked, Direction face, Motif motif, Colour colour) {
        BlockState old = level.getBlockState(clicked);
        BlockPos pos = clicked.relative(face);
        Direction facing = face;
        if (old.getBlock() instanceof CavePaintingBlock) {
            pos = clicked;
            facing = old.getValue(FACING);
        }
        if (!fits(level, pos, facing)) return 0;
        BlockState state = Pigments.CAVE_PAINTING.get().defaultBlockState().setValue(FACING, facing).setValue(COLOUR, colour);
        if (!motif.scene()) {
            level.setBlock(pos, state.setValue(MOTIF, motif), UPDATE_ALL);
            return 1;
        }
        if (facing.getAxis().isVertical()) return 0; // scenes are drawn upright, so only on walls
        BlockPos right = pos.relative(facing.getCounterClockWise()); // the viewer's right, looking at the wall
        if (!fits(level, right, facing)) return 0;
        level.setBlock(pos, state.setValue(MOTIF, motif), UPDATE_ALL);
        level.setBlock(right, state.setValue(MOTIF, Motif.values()[motif.ordinal() + 1]), UPDATE_ALL);
        return 2;
    }

    /** Room for a painting facing {@code facing} at {@code pos}: a solid face behind it, and air (or a painting facing the same way). */
    private static boolean fits(Level level, BlockPos pos, Direction facing) {
        BlockState there = level.getBlockState(pos);
        boolean free = there.getBlock() instanceof CavePaintingBlock ? there.getValue(FACING) == facing
                : there.canBeReplaced() && there.getFluidState().isEmpty();
        return free && supported(level, pos, facing);
    }
}
