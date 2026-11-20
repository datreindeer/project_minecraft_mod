package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.Farming;
import com.mechanical.Fertility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Adds FERTILITY (0-3, new fields 2) and STAKED to farmland; fallow recovery and faster drying in hot weather on its random tick. */
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
        builder.add(Fertility.FERTILITY, Fertility.STAKED);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mechanical$defaultFertility(Block baseBlock, Properties properties, CallbackInfo ci) {
        registerDefaultState(defaultBlockState().setValue(Fertility.FERTILITY, Fertility.NEW_FIELD).setValue(Fertility.STAKED, false));
    }

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void mechanical$fallowAndDrought(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        Fertility.farmlandTick(level, pos, random, isNearWater(level, pos));
    }

    /** Trampled or dried back to dirt: the Crop Stakes fall over and drop. */
    @Inject(method = "turnToBaseBlock", at = @At("HEAD"))
    private void mechanical$dropStakes(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (!level.isClientSide() && state.getValue(Fertility.STAKED)) popResource(level, pos, new ItemStack(Farming.CROP_STAKES.get()));
    }
}
