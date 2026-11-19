package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Kick wheel: a stone flywheel at the bottom, an axle and a plank wheel head on top. Clay Balls go on the head (as many
 * as the shape needs), sneak + empty hand picks the shape, holding right-click with an empty hand throws it, and an
 * empty hand takes the finished greenware off ({@link PottersWheelBlockEntity}).
 */
public class PottersWheelBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 3, 15), box(7, 3, 7, 9, 11, 9), box(2, 11, 2, 14, 12, 14));

    public PottersWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PottersWheelBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.CLAY_BALL) || !(level.getBlockEntity(pos) instanceof PottersWheelBlockEntity wheel)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (wheel.steps() > 0 || wheel.clay() >= wheel.shape().clay) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.wheel.enough_clay"));
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server && wheel.addClay(server)) stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof PottersWheelBlockEntity wheel)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (wheel.done()) {
            player.getInventory().placeItemBackInInventory(wheel.take(), net.minecraft.util.Prediction.SERVER_ONLY);
        } else if (player.isSecondaryUseActive()) {
            if (wheel.steps() == 0) wheel.cycle(server, player);
        } else if (wheel.clay() < wheel.shape().clay) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wheel.needs_clay", wheel.shape().title(), wheel.shape().clay, wheel.clay()));
        } else {
            wheel.throwStep(server);
        }
        return InteractionResult.SUCCESS;
    }
}
