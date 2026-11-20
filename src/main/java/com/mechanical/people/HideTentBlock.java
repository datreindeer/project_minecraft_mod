package com.mechanical.people;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sewn hides over poles: the side of an A-frame tent (tribe camps build them, players can too). A 45-degree hide panel
 * rising toward {@link #FACING}, with a triangular end wall on its left or right ({@link #END}) at the back of a tent. The
 * top of a tent is a {@link Ridge}. Sneak + right-click with an empty hand changes the end wall. Models: scripts/gen_tribes2.py.
 */
public class HideTentBlock extends HorizontalDirectionalBlock {
    public enum End implements StringRepresentable {
        NONE, LEFT, RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<End> END = EnumProperty.create("end", End.class);

    /** Facing north (high edge north): a stair-stepped wedge under the panel. */
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
            box(0, 0, 0, 16, 4, 16), box(0, 4, 0, 16, 8, 12), box(0, 8, 0, 16, 12, 8), box(0, 12, 0, 16, 16, 4)));

    public HideTentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(END, End.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, END);
    }

    /** Rises away from the player, like stairs. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        level.setBlock(pos, state.cycle(END), Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    /**
     * The top of a tent, over the middle: two hide panels meeting at a ridge pole that runs toward {@link #FACING} (the
     * door end); the back end is closed by a triangle of hide when {@link #CLOSED}.
     */
    public static class Ridge extends HorizontalDirectionalBlock {
        public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
        /** Pole along z: thin steps up both panels, open underneath so one can crouch inside. */
        private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
                box(0, 0, 0, 3, 3, 16), box(2, 2, 0, 5, 5, 16), box(4, 4, 0, 7, 7, 16), box(6, 6, 0, 10, 9, 16),
                box(9, 4, 0, 12, 7, 16), box(11, 2, 0, 14, 5, 16), box(13, 0, 0, 16, 3, 16)));

        public Ridge(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CLOSED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, CLOSED);
        }

        /** The door end faces the player. */
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES.get(state.getValue(FACING));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
            level.setBlock(pos, state.cycle(CLOSED), Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }
    }
}
