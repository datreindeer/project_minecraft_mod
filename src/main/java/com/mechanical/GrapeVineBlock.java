package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Wild grape vine (docs/flora-and-food-plan.md, "Flora 4"): climbs walls, cliffs and tree trunks as vanilla vines do (same
 * faces, same spreading up, sideways and down), but only in the growing season and where it is warm enough
 * ({@link #suits}), and much slower. Follows the year like a thicket bush ({@link #TARGET}: leaves in spring with tiny
 * flowers, green clusters in summer, ripe purple grapes among the turning leaves in autumn); in winter it drops its leaves
 * and grapes and stands as a bare woody vine ({@link #LEAFLESS}). Right-click ripe: 1-3 Grapes, back to green clusters.
 */
public class GrapeVineBlock extends VineBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty LEAFLESS = ThicketBushBlock.LEAFLESS;
    public static final int RIPE = 3;
    /** Stage wanted in spring, summer, autumn, winter: 0 leaves, 1 flowers, 2 green clusters, 3 ripe. */
    static final int[] TARGET = {1, 2, 3, 0};
    /** Per random tick: a stage moves on; the vine tries to spread (vanilla's own 1 in 4 comes on top). */
    static final int STAGE_ONE_IN = 5, SPREAD_ONE_IN = 6;
    /** Biome base temperatures it grows and spreads in (warm temperate to hot; vanilla scale). */
    static final float MIN_TEMP = 0.5F, MAX_TEMP = 2.0F;

    private final Supplier<Item> grapes;

    public GrapeVineBlock(Supplier<Item> grapes, Properties properties) {
        super(properties);
        this.grapes = grapes;
        registerDefaultState(defaultBlockState().setValue(AGE, 0).setValue(LEAFLESS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE, LEAFLESS);
    }

    public static boolean suits(float baseTemperature) {
        return baseTemperature >= MIN_TEMP && baseTemperature <= MAX_TEMP;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int season = Calendar.season(level.getOverworldClockTime());
        BlockState was = state;
        state = state.setValue(LEAFLESS, season == 3);
        int age = state.getValue(AGE), want = season == 3 ? 0 : TARGET[season];
        boolean growing = season != 3 && suits(level.getBiome(pos).value().getBaseTemperature());
        if (age > want && random.nextInt(STAGE_ONE_IN) == 0) state = state.setValue(AGE, season == 3 ? 0 : age - 1); // grapes fall
        else if (age < want && growing && random.nextInt(STAGE_ONE_IN) == 0) state = state.setValue(AGE, age + 1);
        if (state != was) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        if (growing && random.nextInt(SPREAD_ONE_IN) == 0) spread(state, level, pos, random);
    }

    /** One try at spreading, as a vanilla vine's random tick (up, sideways round corners, down); new pieces start in leaf. */
    public void spread(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
    }

    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof GrapeVineBlock && state.getValue(AGE) == RIPE && !state.getValue(LEAFLESS);
    }

    /** Picks the grapes: 1-3, and the clusters start over green. Nothing from an unripe vine. */
    public ItemStack pick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isRipe(state)) return ItemStack.EMPTY;
        level.setBlock(pos, state.setValue(AGE, 2), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.7F + level.getRandom().nextFloat() * 0.3F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        return new ItemStack(grapes.get(), 1 + level.getRandom().nextInt(3));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isRipe(state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            Block.popResource(server, pos, pick(server, pos, state));
            if (player instanceof ServerPlayer sp) Kettle.award(sp, Vineyard.PICK_GRAPES);
        }
        return InteractionResult.SUCCESS;
    }
}
