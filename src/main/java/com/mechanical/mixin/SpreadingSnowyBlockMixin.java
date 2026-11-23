package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Grass (and mycelium) under a bottom slab or stairs stays alive (terrain plan phase 2): the half steps worldgen.SmoothGround
 * lays on slopes would otherwise turn the grass under them to dirt, and its side, which shows above the step below, brown.
 */
@Mixin(SpreadingSnowyBlock.class)
public abstract class SpreadingSnowyBlockMixin {
    @Inject(method = "canStayAlive", at = @At("HEAD"), cancellable = true)
    private static void mechanical$aliveUnderHalfStep(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() instanceof SlabBlock ? above.getValue(SlabBlock.TYPE) == SlabType.BOTTOM && !above.getValue(SlabBlock.WATERLOGGED)
                : above.getBlock() instanceof StairBlock && above.getValue(StairBlock.HALF) == Half.BOTTOM && !above.getValue(StairBlock.WATERLOGGED))
            cir.setReturnValue(true);
    }
}
