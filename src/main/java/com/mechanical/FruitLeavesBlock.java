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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Leaves of a fruit tree (docs/flora-and-food-plan.md, section 4: Rowan, Bird Cherry, Wild Apple, Plum, Olive, Date Palm).
 * Plain leaves otherwise, with a {@link #STAGE} that follows the season like the thicket bushes: leaves, blossom, unripe,
 * ripe ({@link #target} per season), one step at a time on random ticks. Right-click ripe leaves to pick 1-2 fruit: they go
 * back to unripe and ripen again while the season lasts. Broken ripe (not sheared) they drop fruit too (loot table,
 * scripts/gen_trees.py). Decay is vanilla's.
 */
public class FruitLeavesBlock extends LeavesBlock {
    public static final IntegerProperty STAGE = BlockStateProperties.AGE_3;
    public static final int RIPE = 3;
    /** Per random tick (~68 s): a step toward the season's stage. */
    static final int STEP_ONE_IN = 5;

    private final Supplier<Item> fruit;
    private final int[] target;

    /** target: the stage wanted in spring, summer, autumn, winter. */
    public FruitLeavesBlock(Supplier<Item> fruit, int[] target, Properties properties) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
        this.fruit = fruit;
        this.target = target;
        registerDefaultState(defaultBlockState().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    public int target(int season) {
        return target[season];
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (decaying(state)) {
            super.randomTick(state, level, pos, random);
            return;
        }
        if (random.nextInt(STEP_ONE_IN) != 0) return;
        int season = Calendar.season(level.getOverworldClockTime()), stage = state.getValue(STAGE), want = target[season];
        if (stage == want) return;
        // fruit falls or blossom fades; growing on needs light, and no blossom out of its season: straight to unripe
        int next = stage > want ? (want == 0 ? 0 : stage - 1) : stage == 0 && want >= 2 && season >= 2 ? 2 : stage + 1;
        if (next > stage && level.getRawBrightness(pos, 0) < 6) return;
        level.setBlock(pos, state.setValue(STAGE, next), Block.UPDATE_CLIENTS);
    }

    /** Picks the fruit: 1-2, the leaves go back to unripe (or bare leaves in winter). Empty when not ripe. */
    public ItemStack pick(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(STAGE) != RIPE) return ItemStack.EMPTY;
        int season = Calendar.season(level.getOverworldClockTime());
        level.setBlock(pos, state.setValue(STAGE, season == 3 ? 0 : 2), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.3F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        return new ItemStack(fruit.get(), 1 + level.getRandom().nextInt(2));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(STAGE) != RIPE) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) Block.popResourceFromFace(server, pos, hit.getDirection(), pick(server, pos, state));
        return InteractionResult.SUCCESS;
    }
}
