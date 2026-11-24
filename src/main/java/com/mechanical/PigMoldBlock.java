package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Pig Mold (the "pigs" of a Pig Bed): an ingot pit pressed into Casting Sand with a Pig Pattern. A Sand Runner beside it
 * fills it ({@link IngotMoldBlock.Kind#PIG}: 400 mB, 2 ingots of the metal, Pig Iron Ingots for pig iron); once set an
 * empty hand takes them, and the sand is burnt: press the pattern into fresh Casting Sand again.
 */
public class PigMoldBlock extends IngotMoldBlock {
    private static final VoxelShape FULL = Shapes.block();

    public PigMoldBlock(Properties properties) {
        super(properties, Kind.PIG);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean had = level.getBlockEntity(pos) instanceof Entity e && e.molten() != null && level.getGameTime() >= e.setAt;
        InteractionResult r = super.useWithoutItem(state, level, pos, player, hit);
        if (had && level instanceof ServerLevel server) server.setBlock(pos, HighFurnace.BURNT_CASTING_SAND.get().defaultBlockState(), Block.UPDATE_ALL);
        return r;
    }
}
