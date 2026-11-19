package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mechanical.Wind;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Torches get random ticks so the wind can blow them out on windy nights (Wind.torchTick; only plain torches react). */
@Mixin(TorchBlock.class)
public abstract class TorchBlockMixin extends BaseTorchBlock {
    protected TorchBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Wind.torchTick(state, level, pos, random);
    }
}
