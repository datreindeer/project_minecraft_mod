package com.mechanical;

import java.util.Locale;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Furnace Bricks, the Smelting Furnace's walls. A plain brick block; the furnace's tap marks the middle of the bottom row
 * of the side and back walls as TUYERE (a clay blowpipe hole facing out, FACING, glowing when the furnace is LIT) while
 * the furnace is formed, to show where Bellows go ({@link FurnaceTapBlockEntity#decorate}). Always drops plain bricks.
 */
public class FurnaceBricksBlock extends Block {
    public enum Tuyere implements StringRepresentable {
        NONE, COLD, LIT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Tuyere> TUYERE = EnumProperty.create("tuyere", Tuyere.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public FurnaceBricksBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TUYERE, Tuyere.NONE).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TUYERE, FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
