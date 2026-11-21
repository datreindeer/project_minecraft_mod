package com.mechanical;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What a charcoal clamp ({@link CharcoalClamp}) leaves of a wood pile: the billets charred black, in the firewood pile's
 * shape, {@link FirewoodPileBlock#LAYERS} pieces of Charcoal. An empty hand takes one, breaking it gives them all; it
 * crumbles if its footing goes.
 */
public class CharcoalHeapBlock extends Block {
    public CharcoalHeapBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FirewoodPileBlock.LAYERS, 1).setValue(FirewoodPileBlock.AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FirewoodPileBlock.LAYERS, FirewoodPileBlock.AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FirewoodPileBlock.shape(state.getValue(FirewoodPileBlock.AXIS), state.getValue(FirewoodPileBlock.LAYERS));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) ticks.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.CHARCOAL, state.getValue(FirewoodPileBlock.LAYERS)));
    }

    /** Empty hand: one piece of Charcoal. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            int n = state.getValue(FirewoodPileBlock.LAYERS);
            level.setBlock(pos, n > 1 ? state.setValue(FirewoodPileBlock.LAYERS, n - 1) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GRAVEL_HIT, SoundSource.BLOCKS, 1.0F, 1.2F);
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.CHARCOAL), Prediction.SERVER_ONLY);
        }
        return InteractionResult.SUCCESS;
    }
}
