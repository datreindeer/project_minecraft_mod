package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Casting Sand (4 Sand + Clay Ball + Coal Dust): moulding sand, the coal dust keeping metal from burning into it. Laid in
 * the floor in front of a taphole it is a Pig Bed: a shovel digs a Sand Runner in it (the sow, a trough in the sand),
 * a Pig Pattern presses a Pig Mold (an ingot pit the runner fills). Rammed into a Molding Flask, 4 of it take a pattern.
 */
public class CastingSandBlock extends Block {
    public CastingSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        if (stack.is(ItemTags.SHOVELS)) {
            if (level instanceof ServerLevel server) {
                BlockState runner = HighFurnace.SAND_RUNNER.get().stateAt(server, pos);
                server.setBlock(pos, runner, Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F);
                stack.hurtAndBreak(1, player, slot);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(HighFurnace.PIG_PATTERN.get())) {
            if (level instanceof ServerLevel server) {
                server.setBlock(pos, HighFurnace.PIG_MOLD.get().defaultBlockState(), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
                stack.hurtAndBreak(1, player, slot);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
