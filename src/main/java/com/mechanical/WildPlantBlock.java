package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A plain plant on the ground (Wild Einkorn, Wild Emmer, Wild Pea, Withered Crop); what it gives is in its loot table. */
public class WildPlantBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = column(12, 0, 13);

    public WildPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
