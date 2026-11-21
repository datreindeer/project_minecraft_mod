package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mechanical.wildlife.Wildlife;

/**
 * Stretch a scraped hide on it (right-click), wait for it to dry (random ticks, a few minutes), take the leather.
 * <p>
 * A rack is one block ({@link Part#SINGLE}, one spot: {@link #HIDE}) or, after right-clicking it with another Drying Rack,
 * two blocks tall ({@link Part#LOWER} + {@link Part#UPPER}) with two spots in each block ({@link #HIDE} on the left,
 * {@link #HIDE2} on the right, seen from the front), four in all. Each spot holds one hide or one batch of food and dries
 * on its own. The contents live in the blockstate (no block entity), so racks from before the tall rack keep what hangs
 * on them: old states have no {@code part}/{@code hide2} and load as a single rack with the same {@code hide}.
 * <p>
 * Spot values: 0 empty, 1 drying, 2 dry, 3 phantom hide drying, 4 phantom leather, 5 rotted phantom hide.
 * A phantom hide only dries in daylight (bright sky above the rack); each random tick in the dark (night, rain, indoors)
 * counts in DARK, and on the {@link #ROT_TICKS}th it rots (undead flesh needs the sun). DARK is per block, so two phantom
 * hides side by side in one block share it.
 * Food dries on it too (Preserving): 6 meat strip drying, 7 jerky, 8 fish drying, 9 dried fish, 10 berries drying
 * (a handful of {@link #HANDFUL}), 11 dried berries, 12 mushrooms drying (a handful), 13 dried mushrooms. Food dries
 * only in sunlight without rain, slower than smoking, and faster out in the wind ({@link Wind#exposed}); night and rain
 * just pause it. Rotting food ({@link Preserving#ROTTING}) can't be hung up.
 * <p>
 * Big pelts only go on a tall rack and fill whole blocks: {@link #HIDE} holds them ({@link #HIDE2} stays empty), 14 raw
 * bear pelt / 15 Bear Pelt in both blocks (the whole rack), 16 raw wolf pelt / 17 Wolf Pelt in one block (one half).
 * They cure in sunlight without rain, a bit slower than a hide; night and rain just pause them. A bear pelt counts once:
 * the lower block gives it (taking it, loot), the upper one only shows its top half.
 * Coast fauna (wildlife.coast): 18 raw sealskin / 19 Sealskin in one block (like the wolf's), 20 raw walrus hide /
 * 21 Walrus Hide in both blocks (like the bear's).
 * <p>
 * Meat and fish dry into their own products ({@link Preserving#drying}): 6/7 and 8/9 are only the red meat and fish looks,
 * the product is remembered per spot ({@link RackFood}); 24 fowl drying / 25 Dried Fowl is the bird look. Big cuts and
 * fat-rich meat ({@code mechanical:not_dryable}) are refused. {@link #HIDE2} never holds a pelt, so it stores fowl as
 * 14/15: always read and write spots through {@link #spot} / {@link #withSpot}.
 * <p>
 * Everything else that dries (adobe, peat, reed, kelp: {@link DryingRackBlockEntity#TABLE}) hangs in the block entity as an
 * item stack while the spot's blockstate value stays 0; a spot is free only when both are empty.
 */
public class DryingRackBlock extends Block implements EntityBlock {
    public enum Part implements StringRepresentable {
        SINGLE, LOWER, UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final int EMPTY = 0, HIDE_WET = 1, PHANTOM_WET = 3, MEAT = 6, FISH = 8, BERRIES = 10, MUSHROOMS = 12,
            BEAR_WET = 14, BEAR_DRY = 15, WOLF_WET = 16, WOLF_DRY = 17, SEAL_WET = 18, SEAL_DRY = 19, WALRUS_WET = 20, WALRUS_DRY = 21,
            CAVE_BEAR_WET = 22, CAVE_BEAR_DRY = 23, // fauna: mountain (whole rack, like the bear)
            FOWL = 24; // woodmeat: birds hang spread out (25 Dried Fowl); HIDE2 keeps them as 14/15
    /** HIDE2 value = spot value minus this, for the fowl look. */
    private static final int HIDE2_FOWL_SHIFT = FOWL - 14;
    // shortcut: pelts only in HIDE (and fowl at 14/15 in HIDE2) keeps the state count at ~40k (a full HIDE2 would be ~65k)
    public static final IntegerProperty HIDE = IntegerProperty.create("hide", 0, FOWL + 1), HIDE2 = IntegerProperty.create("hide2", 0, 15);
    /** Dark random ticks (~68 s each on average) before a phantom hide rots: 8, about one full night. */
    public static final int ROT_TICKS = 8;
    public static final IntegerProperty DARK = IntegerProperty.create("dark", 0, ROT_TICKS - 1);
    private static final float DRY_CHANCE = 1.0F / 3.0F; // per random tick, about 3 minutes on average
    /** Food per random tick in the sun: ~7 minutes out in the wind, ~14 minutes sheltered. */
    /** Per random tick in dry sunlight: a wolf pelt ~6 minutes; a bear pelt ticks in two blocks, so half each. */
    private static final float WOLF_DRY_CHANCE = 1.0F / 5.0F, BEAR_DRY_CHANCE = 1.0F / 10.0F;
    private static final float FOOD_DRY_CHANCE = 1.0F / 6.0F, FOOD_DRY_SHELTERED = 1.0F / 12.0F;
    /** Sky light at the rack that still counts as outdoors (shade under leaves, not inside a roofed house). */
    private static final int OUTDOORS_SKYLIGHT = 10;
    /** Berries and mushrooms go up (and come down dried) by the handful. */
    public static final int HANDFUL = 4;
    private static final VoxelShape SHAPE_NS = box(0, 0, 6, 16, 16, 10), SHAPE_EW = box(6, 0, 0, 10, 16, 16);

    public DryingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.SINGLE)
                .setValue(HIDE, 0).setValue(HIDE2, 0).setValue(DARK, 0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != Mechanical.DRYING_RACK_ENTITY.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<DryingRackBlockEntity>) DryingRackBlockEntity::serverTick;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, HIDE, HIDE2, DARK);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    /** What hangs on a spot (the fowl look is stored as 14/15 in HIDE2). */
    public static int spot(BlockState state, IntegerProperty p) {
        int v = state.getValue(p);
        return p == HIDE2 && v >= 14 ? v + HIDE2_FOWL_SHIFT : v;
    }

    public static BlockState withSpot(BlockState state, IntegerProperty p, int value) {
        return state.setValue(p, p == HIDE2 && value >= FOWL ? value - HIDE2_FOWL_SHIFT : value);
    }

    /** A big pelt (bear, wolf, seal, walrus, cave bear) fills this block. */
    public static boolean bigPelt(BlockState state) {
        int v = state.getValue(HIDE);
        return v >= BEAR_WET && v <= CAVE_BEAR_DRY;
    }

    /** The spots in this block. */
    private static List<IntegerProperty> spots(BlockState state) {
        return state.getValue(PART) == Part.SINGLE ? List.of(HIDE) : List.of(HIDE, HIDE2);
    }

    /** The other half of a tall rack, or null for a single one. */
    private static BlockPos partner(BlockState state, BlockPos pos) {
        return switch (state.getValue(PART)) {
            case SINGLE -> null;
            case LOWER -> pos.above();
            case UPPER -> pos.below();
        };
    }

    private boolean whole(BlockState state, BlockState partnerState) {
        return partnerState.is(this) && partnerState.getValue(PART) == (state.getValue(PART) == Part.LOWER ? Part.UPPER : Part.LOWER);
    }

    private static boolean food(int spot) {
        return spot >= MEAT && spot < BEAR_WET || spot >= FOWL;
    }

    private static boolean drying(int spot) {
        return spot == HIDE_WET || spot == PHANTOM_WET || wetPelt(spot) || food(spot) && spot % 2 == 0;
    }

    private static boolean wetPelt(int spot) {
        return spot >= BEAR_WET && spot <= CAVE_BEAR_DRY && spot % 2 == 0;
    }

    /** What a finished spot gives (empty while it is drying or empty). */
    private static ItemStack finished(int spot) {
        return switch (spot) {
            case 2 -> new ItemStack(Items.LEATHER);
            case 4 -> new ItemStack(Mechanical.PHANTOM_LEATHER.get());
            case 5 -> new ItemStack(Items.ROTTEN_FLESH);
            case 7 -> new ItemStack(Preserving.JERKY.get());
            case 9 -> new ItemStack(Preserving.DRIED_FISH.get());
            case 11 -> new ItemStack(Preserving.DRIED_BERRIES.get(), HANDFUL);
            case 13 -> new ItemStack(Preserving.DRIED_MUSHROOMS.get(), HANDFUL);
            case FOWL + 1 -> new ItemStack(Preserving.DRIED_FOWL.get());
            case BEAR_DRY -> new ItemStack(Wildlife.BEAR_PELT.get());
            case WOLF_DRY -> new ItemStack(Wildlife.WOLF_PELT.get());
            case SEAL_DRY -> new ItemStack(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN.get());
            case CAVE_BEAR_DRY -> new ItemStack(com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR_PELT.get());
            case WALRUS_DRY -> new ItemStack(com.mechanical.wildlife.coast.CoastFauna.WALRUS_HIDE.get());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Mechanical.DRYING_RACK_ITEM.get()) && state.getValue(PART) == Part.SINGLE) {
            BlockPos up = pos.above();
            if (!level.isInsideBuildHeight(up) || !level.getBlockState(up).canBeReplaced()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) { // grow upward: the old spot becomes the lower left one
                stack.consume(1, player);
                level.setBlock(up, defaultBlockState().setValue(FACING, state.getValue(FACING)).setValue(PART, Part.UPPER), Block.UPDATE_ALL);
                level.setBlock(pos, state.setValue(PART, Part.LOWER), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        int pelt = stack.is(Wildlife.RAW_BEAR_PELT.get()) ? BEAR_WET : stack.is(Wildlife.RAW_WOLF_PELT.get()) ? WOLF_WET
                : stack.is(com.mechanical.wildlife.coast.CoastFauna.RAW_SEALSKIN.get()) ? SEAL_WET
                : stack.is(com.mechanical.wildlife.coast.CoastFauna.RAW_WALRUS_HIDE.get()) ? WALRUS_WET
                : stack.is(com.mechanical.wildlife.mountain.MountainFauna.RAW_CAVE_BEAR_PELT.get()) ? CAVE_BEAR_WET : 0;
        if (pelt != 0) return hangPelt(pelt, stack, state, level, pos, player);
        int handful = stack.is(Preserving.DRYABLE_BERRIES) ? BERRIES : stack.is(Preserving.DRYABLE_MUSHROOMS) ? MUSHROOMS : 0;
        Preserving.Drying meat = Preserving.drying(stack);
        if (meat == null && stack.is(Preserving.NOT_DRYABLE)) { // too thick or too fatty: say what to do instead
            if (level.isClientSide()) player.sendOverlayMessage(Component.translatable(CarcassHauling.smokedPieces(stack) > 1
                    ? "message.mechanical.drying_rack.too_thick" : "message.mechanical.drying_rack.too_fatty"));
            return InteractionResult.FAIL;
        }
        int hang = stack.is(Mechanical.SCRAPED_PHANTOM_HIDE.get()) ? PHANTOM_WET : stack.is(Mechanical.SCRAPED_HIDE.get()) ? HIDE_WET
                : meat != null ? meat.look() : handful != 0 && stack.getCount() >= HANDFUL ? handful : 0;
        DryingRackBlockEntity.Drying goods = hang == 0 ? DryingRackBlockEntity.of(stack) : null;
        if (hang == 0 && goods == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (Preserving.tooFarGone(stack, level, player)) return InteractionResult.FAIL;
        // the spot pointed at first, then the other one in this block, then the other half's
        Direction right = state.getValue(FACING).getClockWise();
        double along = (hit.getLocation().x - pos.getX() - 0.5) * right.getStepX() + (hit.getLocation().z - pos.getZ() - 0.5) * right.getStepZ();
        List<BlockPos> where = new ArrayList<>();
        List<IntegerProperty> which = new ArrayList<>();
        for (IntegerProperty p : state.getValue(PART) == Part.SINGLE ? List.of(HIDE) : along < 0 ? List.of(HIDE, HIDE2) : List.of(HIDE2, HIDE)) {
            where.add(pos);
            which.add(p);
        }
        BlockPos other = partner(state, pos);
        if (other != null && whole(state, level.getBlockState(other))) for (IntegerProperty p : List.of(HIDE, HIDE2)) {
            where.add(other);
            which.add(p);
        }
        for (int i = 0; i < where.size(); i++) {
            BlockState at = level.getBlockState(where.get(i));
            if (spot(at, which.get(i)) != EMPTY || bigPelt(at) || DryingRackBlockEntity.occupied(level, where.get(i), which.get(i))) continue;
            if (goods != null) { // up to a spot's worth goes into the block entity; the blockstate stays empty
                if (!level.isClientSide() && level.getBlockEntity(where.get(i)) instanceof DryingRackBlockEntity be) {
                    ItemStack put = stack.copyWithCount(Math.min(goods.perSpot(), stack.getCount()));
                    stack.consume(put.getCount(), player);
                    be.hang(which.get(i), put);
                    level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                if (meat != null) RackFood.put((ServerLevel) level, where.get(i), which.get(i), meat.product().get());
                stack.consume(handful != 0 ? HANDFUL : 1, player);
                boolean phantomThere = spots(at).stream().anyMatch(p -> at.getValue(p) == PHANTOM_WET);
                // a fresh phantom hide starts its dark count at 0, unless it joins one already drying in this block
                BlockState next = withSpot(at, which.get(i), hang);
                level.setBlock(where.get(i), hang == PHANTOM_WET && !phantomThere ? next.setValue(DARK, 0) : next, Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Pelts that fill both blocks of a tall rack (the others take one block). */
    private static boolean wholeRack(int wet) {
        return wet == BEAR_WET || wet == WALRUS_WET || wet == CAVE_BEAR_WET;
    }

    /** A big pelt takes a whole block of a tall rack: the bear both, the wolf the block pointed at, else the other one. */
    private InteractionResult hangPelt(int wet, ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos other = partner(state, pos);
        if (other == null || !whole(state, level.getBlockState(other))) {
            if (level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.drying_rack.pelt_needs_tall"));
            return InteractionResult.FAIL;
        }
        List<BlockPos> free = new ArrayList<>();
        for (BlockPos at : List.of(pos, other)) {
            BlockState s = level.getBlockState(at);
            if (s.getValue(HIDE) == EMPTY && s.getValue(HIDE2) == EMPTY && DryingRackBlockEntity.empty(level, at)) free.add(at);
        }
        boolean bear = wholeRack(wet);
        if (bear ? free.size() < 2 : free.isEmpty()) {
            if (level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.drying_rack.pelt_no_room"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            stack.consume(1, player);
            for (BlockPos at : bear ? free : free.subList(0, 1))
                level.setBlock(at, level.getBlockState(at).setValue(HIDE, wet), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Empty hand takes everything that is done, from the whole rack. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos other = partner(state, pos);
        List<BlockPos> blocks = other != null && whole(state, level.getBlockState(other)) ? List.of(pos, other) : List.of(pos);
        boolean any = false;
        for (BlockPos at : blocks) {
            BlockState s = level.getBlockState(at), next = s;
            for (IntegerProperty p : spots(s)) {
                int v = spot(s, p);
                ItemStack taken = finished(v);
                if (taken.isEmpty()) continue;
                any = true;
                next = next.setValue(p, EMPTY);
                boolean bearTop = (v == BEAR_DRY || v == WALRUS_DRY || v == CAVE_BEAR_DRY) && s.getValue(PART) == Part.UPPER; // the lower block gives the bear pelt
                if (level.isClientSide() || bearTop) continue;
                if (v == MEAT + 1 || v == FISH + 1) taken = RackFood.take((ServerLevel) level, at, p, taken); // what this spot really made
                if (v == MEAT + 1 || v == FISH + 1 || v == FOWL + 1) RackFood.taken(player, taken);
                DryingRackBlockEntity.taken(player, taken);
                player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
            }
            if (!level.isClientSide() && next != s) level.setBlock(at, next, Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof DryingRackBlockEntity be) for (IntegerProperty p : spots(s)) {
                if (!be.done(DryingRackBlockEntity.slot(p))) continue;
                any = true;
                if (level.isClientSide()) continue;
                ItemStack taken = be.take(DryingRackBlockEntity.slot(p));
                DryingRackBlockEntity.taken(player, taken);
                player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
            }
        }
        if (!any) return InteractionResult.PASS;
        if (!level.isClientSide()) level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** The halves of a tall rack go together: if one is gone, the other breaks (dropping what hangs on it) next tick. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (neighbourPos.equals(partner(state, pos)) && !whole(state, neighbourState)) ticks.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos other = partner(state, pos);
        if (other != null && !whole(state, level.getBlockState(other))) level.destroyBlock(pos, true);
    }

    /** In creative the other half goes too, without drops. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos other = partner(state, pos);
        if (!level.isClientSide() && player.isCreative() && other != null && whole(state, level.getBlockState(other)))
            level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return drying(spot(state, HIDE)) || drying(spot(state, HIDE2));
    }

    /** Sunlight reaches the rack: daytime and open sky. */
    public static boolean inDaylight(Level level, BlockPos pos) {
        return level.isBrightOutside() && level.canSeeSky(pos);
    }

    /** The upper half would block the lower one's sky, so a tall rack looks from its top. */
    private static BlockPos top(BlockState state, BlockPos pos) {
        return state.getValue(PART) == Part.LOWER ? pos.above() : pos;
    }

    /**
     * How fast food and goods dry here: 0 not at all (night, rain, indoors), 2 in the sun out in the wind, 1 otherwise.
     * Outdoors in the day is enough, shade too (under a tree skylight drops below 15); rain only reaches open racks.
     */
    public static int dryRate(Level level, BlockPos pos, BlockState state) {
        BlockPos top = top(state, pos);
        if (!level.isBrightOutside() || level.getBrightness(LightLayer.SKY, top) < OUTDOORS_SKYLIGHT || level.isRainingAt(top.above())) return 0;
        return inDaylight(level, top) && Wind.exposed(level, top) ? 2 : 1;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean sun = inDaylight(level, top(state, pos));
        int rate = dryRate(level, pos, state);
        boolean dryWeather = rate > 0;
        float foodChance = rate == 2 ? FOOD_DRY_CHANCE : FOOD_DRY_SHELTERED;
        BlockState next = state;
        boolean phantom = false;
        for (IntegerProperty p : spots(state)) {
            int spot = spot(state, p);
            if (wetPelt(spot)) {
                if (dryWeather && random.nextFloat() < (wholeRack(spot) ? BEAR_DRY_CHANCE : WOLF_DRY_CHANCE)) {
                    next = next.setValue(p, spot + 1);
                    BlockPos other = partner(state, pos); // the bear pelt is one hide over both blocks
                    if (wholeRack(spot) && other != null && level.getBlockState(other).is(this) && level.getBlockState(other).getValue(HIDE) == spot)
                        level.setBlock(other, level.getBlockState(other).setValue(HIDE, spot + 1), Block.UPDATE_ALL);
                }
            } else if (spot == HIDE_WET) {
                if (random.nextFloat() < DRY_CHANCE) next = next.setValue(p, 2);
            } else if (food(spot) && spot % 2 == 0) {
                if (dryWeather && random.nextFloat() < foodChance) next = withSpot(next, p, spot + 1);
            } else if (spot == PHANTOM_WET) { // dries as fast as a normal hide, but only in daylight; the dark adds up until it rots
                if (sun && random.nextFloat() < DRY_CHANCE) next = next.setValue(p, 4);
                else phantom = true;
            }
        }
        if (phantom && !sun) {
            if (state.getValue(DARK) + 1 >= ROT_TICKS) {
                for (IntegerProperty p : spots(state)) if (next.getValue(p) == PHANTOM_WET) next = next.setValue(p, 5);
                next = next.setValue(DARK, 0);
                level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.6F);
            } else {
                next = next.setValue(DARK, state.getValue(DARK) + 1);
            }
        } else if (!phantom) {
            next = next.setValue(DARK, 0); // no phantom hide left drying here
        }
        if (next == state) return;
        // only the dark count changed: nothing to see, no neighbour updates
        level.setBlock(pos, next, next.setValue(DARK, state.getValue(DARK)) == state ? Block.UPDATE_CLIENTS : Block.UPDATE_ALL);
    }
}
