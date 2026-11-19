package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Flat stone for crushing ore. Right-click puts one ore on it, an empty hand takes it back; hitting the ore with a
 * hammerstone (attack or use) {@link StoneAnvilBlockEntity#HITS} times crushes it.
 */
public class StoneAnvilBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 2, 15, 5, 14), box(2, 0, 1, 14, 4, 15));

    public StoneAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneAnvilBlockEntity(pos, state);
    }

    private static boolean striking(BlockGetter level, BlockPos pos, ItemStack tool) {
        return tool.is(BronzeOres.HAMMERSTONES) && level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil && !anvil.item().isEmpty();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (striking(level, pos, stack)) {
            if (level instanceof ServerLevel server) anvil.strike(server, player, stack, hand);
            return InteractionResult.SUCCESS;
        }
        if (!anvil.item().isEmpty() || BronzeOres.crushed(stack).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) anvil.place(server, stack.consumeAndReturn(1, player));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil) || anvil.item().isEmpty() || !player.getMainHandItem().isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide()) player.getInventory().placeItemBackInInventory(anvil.take(), net.minecraft.util.Prediction.SERVER_ONLY);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack tool = player.getMainHandItem();
        if (level instanceof ServerLevel server && striking(level, pos, tool) && level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil)
            anvil.strike(server, player, tool, InteractionHand.MAIN_HAND);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // with ore on it a hammerstone strikes the ore instead of breaking the anvil
        return striking(level, pos, player.getMainHandItem()) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }
}
