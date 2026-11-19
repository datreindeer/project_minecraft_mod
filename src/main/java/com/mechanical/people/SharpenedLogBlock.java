package com.mechanical.people;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The pointed top of a palisade stake: a log carved to a point (stepped taper). Landing on it hurts twice as much. */
public class SharpenedLogBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 9, 16), Block.box(2, 9, 2, 14, 12, 14),
            Block.box(4, 12, 4, 12, 14, 12), Block.box(6, 14, 6, 10, 16, 10));

    public SharpenedLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 2);
    }
}
