package com.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Feeding Trough (D6): a plank trough for livestock ({@link Husbandry}). A water bucket (or a clay or bronze one) fills it
 * with {@link FeedingTroughBlockEntity#WATER_CAP} drinks; fodder (wheat, carrots, seeds, hay ...) goes in by the stack, an
 * empty hand takes the fodder back out. Troughs side by side join into one long trough: the rims between them go, and
 * filling, eating and drinking use the whole row ({@link #group}). The blockstate shows the water level and whether there is
 * fodder; the contents live in the block entity. 10 px high, so sheep, cows and pigs can't step onto it.
 */
public class FeedingTroughBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = PipeBlock.NORTH, EAST = PipeBlock.EAST, SOUTH = PipeBlock.SOUTH, WEST = PipeBlock.WEST;
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 3);
    public static final BooleanProperty FOOD = BooleanProperty.create("food");
    /** At most this many troughs make one. */
    static final int MAX_GROUP = 16;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);

    public FeedingTroughBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(WATER, 0).setValue(FOOD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATER, FOOD);
    }

    static BooleanProperty side(Direction d) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(d);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.Plane.HORIZONTAL)
            s = s.setValue(side(d), context.getLevel().getBlockState(context.getClickedPos().relative(d)).getBlock() instanceof FeedingTroughBlock);
        return s;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction.getAxis().isHorizontal()) return state.setValue(side(direction), neighbourState.getBlock() instanceof FeedingTroughBlock);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The connected troughs (this one first), up to {@link #MAX_GROUP}. */
    public static List<FeedingTroughBlockEntity> group(Level level, BlockPos start) {
        List<FeedingTroughBlockEntity> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && out.size() < MAX_GROUP) {
            BlockPos p = queue.poll();
            if (!(level.getBlockEntity(p) instanceof FeedingTroughBlockEntity t)) continue;
            out.add(t);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        return out;
    }

    /** Fills the first trough of the group that isn't full; false if all are. */
    public static boolean fillWater(Level level, BlockPos pos) {
        for (FeedingTroughBlockEntity t : group(level, pos)) {
            if (t.water() < FeedingTroughBlockEntity.WATER_CAP) {
                t.setWater(FeedingTroughBlockEntity.WATER_CAP);
                return true;
            }
        }
        return false;
    }

    /** Puts as much of {@code stack} as fits into the group (shrinks it); returns how much went in. */
    public static int addFood(Level level, BlockPos pos, ItemStack stack) {
        int moved = 0;
        for (FeedingTroughBlockEntity t : group(level, pos)) {
            if (stack.isEmpty()) break;
            moved += t.insert(stack);
        }
        return moved;
    }

    /** One drink from the group: false if it is dry. */
    public static boolean drink(ServerLevel level, BlockPos pos) {
        for (FeedingTroughBlockEntity t : group(level, pos)) {
            if (t.water() > 0) {
                t.setWater(t.water() - 1);
                return true;
            }
        }
        return false;
    }

    /** One item of what this animal eats from the group: false if there is none. */
    public static boolean eat(ServerLevel level, BlockPos pos, Animal animal) {
        for (FeedingTroughBlockEntity t : group(level, pos)) {
            if (animal.isFood(t.food())) {
                t.takeFood(1);
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (TripodBlockEntity.isWater(stack)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!fillWater(level, pos)) return InteractionResult.FAIL;
            if (!player.hasInfiniteMaterials()) {
                ItemStack empty = stack.is(Items.WATER_BUCKET) ? new ItemStack(Items.BUCKET)
                        : stack.is(BronzeTools.BRONZE_WATER_BUCKET.get()) ? stack.transmuteCopy(BronzeTools.BRONZE_BUCKET.get(), 1) : ClayBucketItem.emptied(stack);
                if (empty.isEmpty()) player.playSound(SoundEvents.DECORATED_POT_SHATTER, 0.8F, 1.1F);
                player.setItemInHand(hand, empty);
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, FeedingTroughBlockEntity.FILLED);
            return InteractionResult.SUCCESS;
        }
        if (Husbandry.fodder(stack)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            ItemStack in = player.hasInfiniteMaterials() ? stack.copy() : stack;
            if (addFood(level, pos, in) == 0) return InteractionResult.FAIL;
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 0.8F);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, FeedingTroughBlockEntity.FILLED);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity t) || t.food().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) player.getInventory().placeItemBackInInventory(t.takeFood(t.food().getCount()), net.minecraft.util.Prediction.SERVER_ONLY);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeedingTroughBlockEntity(pos, state);
    }
}
