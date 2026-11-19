package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stretch a scraped hide on it (right-click), wait for it to dry (random ticks, a few minutes), take the leather.
 * HIDE: 0 empty, 1 drying, 2 dry, 3 phantom hide drying, 4 phantom leather, 5 rotted phantom hide.
 * A phantom hide only dries in daylight (bright sky above the rack); each random tick in the dark (night, rain, indoors)
 * counts in DARK, and on the {@link #ROT_TICKS}th it rots (undead flesh needs the sun).
 */
public class DryingRackBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty HIDE = IntegerProperty.create("hide", 0, 5);
    /** Dark random ticks (~68 s each on average) before a phantom hide rots: 8, about one full night. */
    public static final int ROT_TICKS = 8;
    public static final IntegerProperty DARK = IntegerProperty.create("dark", 0, ROT_TICKS - 1);
    private static final float DRY_CHANCE = 1.0F / 3.0F; // per random tick, about 3 minutes on average
    private static final VoxelShape SHAPE_NS = box(0, 0, 6, 16, 16, 10), SHAPE_EW = box(6, 0, 0, 10, 16, 16);

    public DryingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HIDE, 0).setValue(DARK, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HIDE, DARK);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean phantom = stack.is(Mechanical.SCRAPED_PHANTOM_HIDE.get());
        if (state.getValue(HIDE) != 0 || !(phantom || stack.is(Mechanical.SCRAPED_HIDE.get()))) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(HIDE, phantom ? 3 : 1).setValue(DARK, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int hide = state.getValue(HIDE);
        if (hide != 2 && hide < 4) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack taken = new ItemStack(hide == 2 ? Items.LEATHER : hide == 4 ? Mechanical.PHANTOM_LEATHER.get() : Items.ROTTEN_FLESH);
            player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
            level.setBlock(pos, state.setValue(HIDE, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HIDE) == 1 || state.getValue(HIDE) == 3;
    }

    /** Sunlight reaches the rack: daytime and open sky. */
    public static boolean inDaylight(Level level, BlockPos pos) {
        return level.isBrightOutside() && level.canSeeSky(pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HIDE) == 1) {
            if (random.nextFloat() < DRY_CHANCE) level.setBlock(pos, state.setValue(HIDE, 2), Block.UPDATE_ALL);
            return;
        }
        // phantom hide: dries as fast as a normal one, but only in daylight; the dark adds up until it rots
        if (inDaylight(level, pos)) {
            if (random.nextFloat() < DRY_CHANCE) level.setBlock(pos, state.setValue(HIDE, 4).setValue(DARK, 0), Block.UPDATE_ALL);
        } else if (state.getValue(DARK) + 1 >= ROT_TICKS) {
            level.setBlock(pos, state.setValue(HIDE, 5).setValue(DARK, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.6F);
        } else {
            level.setBlock(pos, state.setValue(DARK, state.getValue(DARK) + 1), Block.UPDATE_CLIENTS);
        }
    }
}
