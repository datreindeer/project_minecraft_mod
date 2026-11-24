package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stone Age: the Clay Cooking Pot sits in the embers of a campfire, no tripod. It is the kettle's pot without the
 * tripod: the same block entity ({@link TripodBlockEntity}, its vessel the pot itself, so it holds less and cooks only
 * the clay pot's dishes, Kitchen) and the same liquid renderer, at the height a kettle hangs over a campfire. Sneak with
 * an empty hand on an empty pot to lift it off the fire.
 */
public class ClayPotBlock extends TripodBlock {
    private static final VoxelShape SHAPE = box(2, -8, 2, 14, 2.5, 14);

    public ClayPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(KETTLE, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos().below()).is(BlockTags.CAMPFIRES)) {
            if (context.getPlayer() != null && context.getLevel().isClientSide())
                context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.clay_pot.no_fire"));
            return null;
        }
        return defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.CAMPFIRES);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof TripodBlockEntity pot) pot.setVessel(stack.copyWithCount(1));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TripodBlockEntity pot) || !pot.accepts(stack) || TripodBlockEntity.isHangingPot(stack))
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) pot.use(server, player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TripodBlockEntity pot) || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (player.isSecondaryUseActive() && pot.isEmpty()) {
            if (level instanceof ServerLevel server) {
                ItemStack vessel = pot.takeVessel();
                server.removeBlock(pos, false);
                player.getInventory().placeItemBackInInventory(vessel.isEmpty() ? new ItemStack(Kitchen.CLAY_COOKING_POT_ITEM.get()) : vessel, Prediction.SERVER_ONLY);
                server.playSound(null, pos, SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
