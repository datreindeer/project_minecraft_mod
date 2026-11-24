package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Foundry Table (docs/steel-furnace-plan.md section 6): a steel table on legs with a silica-lined top and a water
 * channel round its edge. A mold, flask or basin standing on it sets in half the time ({@link #setTicks}). (Simplified
 * from the plan, where the table holds the mold itself: here the mold stands on it.)
 */
public class FoundryTableBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 12, 0, 16, 16, 16), box(1, 0, 1, 4, 12, 4), box(12, 0, 1, 15, 12, 4),
            box(1, 0, 12, 4, 12, 15), box(12, 0, 12, 15, 12, 15));

    public FoundryTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** How long a cast at {@code pos} takes to set: {@code ticks}, or half that on a Foundry Table. */
    public static int setTicks(Level level, BlockPos pos, int ticks) {
        return level != null && level.getBlockState(pos.below()).is(HighFurnace.FOUNDRY_TABLE.get()) ? ticks / 2 : ticks;
    }
}
