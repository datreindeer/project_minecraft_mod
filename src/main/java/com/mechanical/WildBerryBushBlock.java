package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wild berry bush (raspberry, blueberry ...). Grows 0 (bare) -> 1 (flowers) -> 2 (unripe) -> 3 (ripe); right-click a
 * ripe bush to pick 1-3 berries, after which it starts over from flowers. No thorns, can't be planted (the berries aren't
 * seeds); broken it gives sticks and plant fibre (loot table).
 */
public class WildBerryBushBlock extends VegetationBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int RIPE = 3;
    /** Chance per random tick to grow a stage: ~6 min per stage on average, so ~12 min to ripen again after picking. */
    private static final int GROW_ONE_IN = 6;
    private static final VoxelShape SMALL = column(10, 0, 8), BIG = column(14, 0, 13);

    private final Supplier<Item> berries;

    public WildBerryBushBlock(Supplier<Item> berries, Properties properties) {
        super(properties);
        this.berries = berries;
        registerDefaultState(defaultBlockState().setValue(AGE, RIPE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SMALL : BIG;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < RIPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROW_ONE_IN) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof WildBerryBushBlock && state.getValue(AGE) == RIPE;
    }

    /** Picks the berries (also used by bears eating from it). Returns what was picked. */
    public ItemStack pick(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        return new ItemStack(berries.get(), 1 + level.getRandom().nextInt(3));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) != RIPE) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) Block.popResource(server, pos, pick(server, pos, state));
        return InteractionResult.SUCCESS;
    }
}
