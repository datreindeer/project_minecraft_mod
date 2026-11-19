package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.Fertility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Adds FERTILITY (0-3, new fields 2) to farmland; fallow recovery and faster drying in hot weather on its random tick. */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin extends Block {
    protected FarmlandBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow
    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        return false;
    }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void mechanical$addFertility(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(Fertility.FERTILITY);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mechanical$defaultFertility(Block baseBlock, Properties properties, CallbackInfo ci) {
        registerDefaultState(defaultBlockState().setValue(Fertility.FERTILITY, Fertility.NEW_FIELD));
    }

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void mechanical$fallowAndDrought(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        Fertility.farmlandTick(level, pos, random, isNearWater(level, pos));
    }
}
