package com.mechanical.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Anthill (myrstack): a wood ants' mound of spruce needles and twigs, a dome modelled in fine steps so it reads round
 * (OpenDecor.anthill, docs/biomes.md). SMALL is the low cap on the crown of a mound of Podzol and Coarse Dirt half blocks; a big mound is two blocks, its
 * BASE spreading two and a half blocks wide and its TOP rounding it off. Models and texture: scripts/gen_land_decor.py.
 */
public class AnthillBlock extends Block {
    public enum Part implements StringRepresentable {
        SMALL, BASE, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape SMALL = box(1, 0, 1, 15, 11, 15), BASE = box(0, 0, 0, 16, 16, 16), TOP = box(3, 0, 3, 13, 9, 13);

    public AnthillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.SMALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case SMALL -> SMALL;
            case BASE -> BASE;
            case TOP -> TOP;
        };
    }
}
