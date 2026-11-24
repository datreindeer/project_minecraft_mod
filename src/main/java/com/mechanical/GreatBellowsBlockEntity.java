package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Great Bellows' turn (see {@link GreatBellowsBlock}): a {@link RotorBlockEntity} fed by a Water Wheel's shaft. Its
 * power (its share of the line's, {@link WaterPower#share}) is the blast it gives a Firebox; the client works the
 * boards from its angle, two strokes per turn of the cam shaft.
 */
public class GreatBellowsBlockEntity extends RotorBlockEntity {
    public GreatBellowsBlockEntity(BlockPos pos, BlockState state) {
        super(HighFurnace.GREAT_BELLOWS_ENTITY.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(GreatBellowsBlock.FACING);
    }
}
