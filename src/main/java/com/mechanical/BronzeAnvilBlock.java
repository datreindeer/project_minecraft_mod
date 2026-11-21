package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cast bronze anvil (horn along FACING's clockwise side). Right-click puts an Iron Bloom or a Copper Ingot on it, an empty hand takes it
 * back; the Bronze Smithing Hammer (attack or use, {@link Ironworking#HAMMER_COOLDOWN} ticks apart) works a hot one
 * ({@link BronzeAnvilBlockEntity#strike}).
 */
public class BronzeAnvilBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape X = Shapes.or(box(2, 0, 3, 14, 5, 13), box(5, 5, 6, 11, 9, 10), box(0, 9, 4, 16, 13, 12));
    private static final VoxelShape Z = Shapes.or(box(3, 0, 2, 13, 5, 14), box(6, 5, 5, 10, 9, 11), box(4, 9, 0, 12, 13, 16));

    public BronzeAnvilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? X : Z;
    }

    private static boolean striking(BlockGetter level, BlockPos pos, ItemStack tool) {
        return Smithing.isHammer(tool) && level.getBlockEntity(pos) instanceof BronzeAnvilBlockEntity anvil && !anvil.item().isEmpty();
    }

    private static void strike(ServerLevel level, BlockPos pos, Player player, ItemStack hammer, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(hammer) || !(level.getBlockEntity(pos) instanceof BronzeAnvilBlockEntity anvil)) return;
        player.getCooldowns().addCooldown(hammer, Ironworking.HAMMER_COOLDOWN);
        anvil.strike(level, player, hammer, hand);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BronzeAnvilBlockEntity anvil)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        // sneak-use with a hammer: what hot iron or steel is forged into (Smithing.Pattern)
        if (player.isSecondaryUseActive() && Smithing.isHammer(stack)) {
            if (level instanceof ServerLevel) anvil.nextPattern(player);
            return InteractionResult.SUCCESS;
        }
        if (striking(level, pos, stack)) {
            if (level instanceof ServerLevel server) strike(server, pos, player, stack, hand);
            return InteractionResult.SUCCESS;
        }
        // Bronze Ingots pile up (FirePit.GRATE_INGOTS) for a Bronze Fire Grate
        if (stack.is(Bronze.BRONZE_INGOT.get()) && anvil.item().is(Bronze.BRONZE_INGOT.get()) && anvil.item().getCount() < FirePit.GRATE_INGOTS) {
            if (level instanceof ServerLevel server) anvil.add(server, stack.consumeAndReturn(1, player));
            return InteractionResult.SUCCESS;
        }
        if (!anvil.item().isEmpty() || !(stack.getItem() instanceof IronBloomItem || Coins.mintable(stack) || stack.is(Bronze.BRONZE_INGOT.get())
                || Smithing.forgeable(stack)))
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            anvil.place(server, stack.consumeAndReturn(1, player));
            if (Smithing.forgeable(anvil.item())) anvil.tellPattern(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BronzeAnvilBlockEntity anvil) || anvil.item().isEmpty() || !player.getMainHandItem().isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide()) player.getInventory().placeItemBackInInventory(anvil.take(), net.minecraft.util.Prediction.SERVER_ONLY);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack tool = player.getMainHandItem();
        if (level instanceof ServerLevel server && striking(level, pos, tool)) strike(server, pos, player, tool, InteractionHand.MAIN_HAND);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // with a bloom on it the hammer works the bloom instead of breaking the anvil
        return striking(level, pos, player.getMainHandItem()) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BronzeAnvilBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Ironworking.BRONZE_ANVIL_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<BronzeAnvilBlockEntity>) (l, pos, s, anvil) -> {
            if (l.getGameTime() % 20 == 0) anvil.cool((ServerLevel) l);
        };
    }
}
