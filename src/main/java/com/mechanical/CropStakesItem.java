package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Right-click farmland, or a crop on it, to put up Crop Stakes (Fertility.STAKED). Breaking the farmland gives them back. */
public class CropStakesItem extends Item {
    public CropStakesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).hasProperty(Fertility.STAKED)) pos = pos.below(); // clicked the crop
        BlockState soil = level.getBlockState(pos);
        if (!soil.hasProperty(Fertility.STAKED) || soil.getValue(Fertility.STAKED)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, soil.setValue(Fertility.STAKED, true), Block.UPDATE_ALL);
            context.getItemInHand().consume(1, context.getPlayer());
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
