package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wild berry bush (raspberry, blueberry ...). Grows 0 (bare) -> 1 (flowers) -> 2 (unripe) -> 3 (ripe); right-click a
 * ripe bush to pick 1-3 berries, after which it starts over from flowers. No thorns, can't be planted (the berries aren't
 * seeds); broken it gives sticks and plant fibre (loot table). In winter, where it's cold enough for snow to lie, a bush
 * freezes ({@link #FROZEN}): it keeps its stage but can't be picked or grow until spring. Winter-hardy bushes
 * (Lingonberry) never freeze. Checked on random ticks and on use, so a bush in a chunk that slept all winter is right too.
 */
public class WildBerryBushBlock extends VegetationBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty FROZEN = BooleanProperty.create("frozen");
    public static final int RIPE = 3;
    static final Identifier WINTER_BERRIES = Identifier.fromNamespaceAndPath(Mechanical.MODID, "food/winter_berries");
    /** Chance per random tick to grow a stage: ~6 min per stage on average, so ~12 min to ripen again after picking. */
    private static final int GROW_ONE_IN = 6;
    /** Chance per random tick of a bush with berries that a bird nearby sows a new bush (WildNature#sowBerries). */
    private static final int SOW_ONE_IN = 200;
    private static final VoxelShape SMALL = column(10, 0, 8), BIG = column(14, 0, 13);

    private final Supplier<Item> berries;
    private final boolean hardy;

    public WildBerryBushBlock(Supplier<Item> berries, boolean hardy, Properties properties) {
        super(properties);
        this.berries = berries;
        this.hardy = hardy;
        registerDefaultState(defaultBlockState().setValue(AGE, RIPE).setValue(FROZEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FROZEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SMALL : BIG;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !hardy || state.getValue(AGE) < RIPE; // ripe bushes tick too, to freeze and thaw
    }

    /** Winter, and cold enough here that snow stays (season included): warm biomes never freeze. */
    public static boolean wintry(ServerLevel level, BlockPos pos) {
        return wintry(Calendar.season(level.getOverworldClockTime()), Thaw.temperature(level, level.getBiome(pos).value(), pos));
    }

    /** Season 0..3, biome temperature here (vanilla scale, season included). */
    static boolean wintry(int season, float temperature) {
        return season == 3 && temperature < Thaw.MELT_AT;
    }

    /** Freezes or thaws the bush to match the season; returns the state it has now. */
    BlockState updateFrost(ServerLevel level, BlockPos pos, BlockState state) {
        boolean frozen = !hardy && wintry(level, pos);
        if (state.getValue(FROZEN) == frozen) return state;
        state = state.setValue(FROZEN, frozen);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return state;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        state = updateFrost(level, pos, state);
        if (!state.getValue(FROZEN) && state.getValue(AGE) < RIPE && random.nextInt(GROW_ONE_IN) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
        // birds eat the berries and drop the seeds some way off (A10)
        else if (!state.getValue(FROZEN) && state.getValue(AGE) >= 2 && random.nextInt(SOW_ONE_IN) == 0) com.mechanical.wildlife.WildNature.sowBerries(level, pos, state, random);
    }

    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof WildBerryBushBlock && state.getValue(AGE) == RIPE && !state.getValue(FROZEN);
    }

    /** Picks the berries (also used by bears eating from it). Returns what was picked: nothing from a frozen bush. */
    public ItemStack pick(ServerLevel level, BlockPos pos, BlockState state) {
        state = updateFrost(level, pos, state);
        if (!isRipe(state)) return ItemStack.EMPTY;
        level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        return new ItemStack(berries.get(), 1 + level.getRandom().nextInt(3));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isRipe(state)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            ItemStack picked = pick(server, pos, state);
            if (picked.isEmpty()) return InteractionResult.PASS; // froze just now: never ticked since winter came
            Block.popResource(server, pos, picked);
            if (player instanceof ServerPlayer sp && Calendar.season(server.getOverworldClockTime()) == 3) Kettle.award(sp, WINTER_BERRIES);
        }
        return InteractionResult.SUCCESS;
    }
}
