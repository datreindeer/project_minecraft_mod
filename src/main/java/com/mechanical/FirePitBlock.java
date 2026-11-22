package com.mechanical;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The four blocks of a Fire Pit (see {@link FirePit}). PART 0 is the north-west corner and holds the block entity and the
 * whole 2x2 model; 1 is east of it, 2 south, 3 south-east (x offset = part & 1, z offset = part >> 1). LIT, EMBERS and FUEL
 * (firewood stacked on it: 0 none .. 3 a full stack) are kept the same on all four, so light and smoke checks work on any
 * of them. Placed from the item onto a flat 2x2 of solid ground in front of the player; the four live and die together.
 * <p>
 * BIG: grown to 3x3 with a Steel Hearth Ring ({@link #upgrade}); PART is then 0..8 (x = part % 3, z = part / 3), the
 * north-west corner still holds the block entity and the centre (part 4) draws the whole big model.
 */
public class FirePitBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    /** Grown to 3x3 (FirePit F12); kept on all nine parts. */
    public static final BooleanProperty BIG = BooleanProperty.create("big");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty EMBERS = Campfires.EMBERS;
    public static final IntegerProperty FUEL = IntegerProperty.create("fuel", 0, 3);
    /** A Bronze Fire Grate lies in it (FirePit.FIRE_GRATE); kept on all four parts like LIT. */
    public static final BooleanProperty GRATE = BooleanProperty.create("grate");
    private static final VoxelShape SHAPE = column(16, 0, 5);

    public FirePitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(LIT, false).setValue(EMBERS, false).setValue(FUEL, 0).setValue(GRATE, false).setValue(BIG, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, LIT, EMBERS, FUEL, GRATE, BIG);
    }

    /** Blocks along each side: 2, or 3 for a big pit. */
    public static int size(BlockState state) {
        return state.getValue(BIG) ? 3 : 2;
    }

    public static BlockPos controller(BlockState state, BlockPos pos) {
        int part = state.getValue(PART), n = size(state);
        return pos.offset(-(part % n), 0, -(part / n));
    }

    private static BlockPos partPos(BlockPos controller, int part, int n) {
        return controller.offset(part % n, 0, part / n);
    }

    /** Changes all blocks of the pit at {@code controller} the same way (LIT, EMBERS, FUEL). */
    public static void setAll(Level level, BlockPos controller, UnaryOperator<BlockState> change) {
        BlockState c = level.getBlockState(controller);
        int n = c.getBlock() instanceof FirePitBlock ? size(c) : 2;
        for (int part = 0; part < n * n; part++) {
            BlockPos p = partPos(controller, part, n);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FirePitBlock) {
                BlockState next = change.apply(s);
                if (next != s) level.setBlock(p, next, UPDATE_ALL);
            }
        }
    }

    // ---- placement: the clicked block and the three in front of / to the right of it ----

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction front = context.getHorizontalDirection(), right = front.getClockWise();
        BlockPos[] four = {pos, pos.relative(front), pos.relative(right), pos.relative(front).relative(right)};
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (BlockPos p : four) {
            boolean free = p.equals(pos) || level.getBlockState(p).canBeReplaced(context);
            if (!free || !level.getWorldBorder().isWithinBounds(p) || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                if (context.getPlayer() != null && level.isClientSide()) context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.fire_pit.no_room"));
                return null;
            }
            minX = Math.min(minX, p.getX());
            minZ = Math.min(minZ, p.getZ());
        }
        return defaultBlockState().setValue(PART, (pos.getX() - minX) + 2 * (pos.getZ() - minZ));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
        BlockPos controller = controller(state, pos);
        for (int part = 0; part < 4; part++) {
            BlockPos p = partPos(controller, part, 2);
            if (!p.equals(pos)) level.setBlock(p, state.setValue(PART, part), UPDATE_ALL);
        }
    }

    // ---- the four live and die together ----

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        int part = state.getValue(PART), n = size(state), x = part % n, z = part / n;
        int expected = switch (direction) {
            case EAST -> x < n - 1 ? part + 1 : -1;
            case WEST -> x > 0 ? part - 1 : -1;
            case SOUTH -> z < n - 1 ? part + n : -1;
            case NORTH -> z > 0 ? part - n : -1;
            default -> -1;
        };
        if (expected >= 0 && !(neighbourState.getBlock() instanceof FirePitBlock && neighbourState.getValue(PART) == expected
                && neighbourState.getValue(BIG) == state.getValue(BIG))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // breaking a side part: the corner with the block entity gives the item (and drops the food and firewood) itself,
        // removed without shape updates so this part is still there for vanilla to break; the rest then fall apart
        BlockPos controller = controller(state, pos);
        if (!level.isClientSide() && !controller.equals(pos)) {
            BlockState c = level.getBlockState(controller);
            if (c.is(this)) {
                if (!player.preventsBlockDrops()) dropResources(c, level, controller, level.getBlockEntity(controller), player, player.getMainHandItem());
                level.setBlock(controller, Blocks.AIR.defaultBlockState(), UPDATE_CLIENTS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- use ----

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        FirePitBlockEntity pit = FirePit.entity(level, pos, state);
        if (pit == null || stack.getItem() instanceof FireDrillItem) return InteractionResult.PASS; // the drill lights it (FireDrillItem)
        if (stack.is(FirePit.FIRE_GRATE.get())) {
            if (state.getValue(GRATE)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                setAll(level, pit.getBlockPos(), s -> s.setValue(GRATE, true));
                level.playSound(null, pit.getBlockPos(), net.minecraft.sounds.SoundEvents.CHAIN_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(FirePit.HEARTH_RING.get())) {
            if (state.getValue(BIG)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!upgrade(server, pit.getBlockPos(), player)) return InteractionResult.FAIL;
            stack.consume(1, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(net.minecraft.world.item.Items.CHARCOAL)) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!state.getValue(GRATE)) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.fire_pit.needs_grate"));
                return InteractionResult.FAIL;
            }
            return pit.feed(server, player, stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        if (stack.is(Mechanical.FIREWOOD_TAG)) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            return pit.feed(server, player, stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        String wood = TreeChopping.roughLogWood(stack);
        if (wood != null) { // a whole Rough Log burns as FirePit.LOG_FIREWOOD pieces of its wood's firewood
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            return pit.feedLog(server, player, stack, new ItemStack(Mechanical.FIREWOOD.get(wood).get())) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        if (level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(stack)) {
            if (level instanceof ServerLevel server && pit.placeFood(server, player, stack)) return InteractionResult.SUCCESS_SERVER;
            return InteractionResult.CONSUME;
        }
        if (stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHOVEL_DOUSE) && state.getValue(LIT)) {
            if (!level.isClientSide()) {
                level.levelEvent(null, 1009, pit.getBlockPos(), 0);
                pit.douse();
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Empty hand: takes food off the spits (done or burnt first), else tells how much firewood is stacked on it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        FirePitBlockEntity pit = FirePit.entity(level, pos, state);
        if (pit == null || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!pit.takeRoast(player) && !pit.takeItem(player))
                player.sendOverlayMessage(Component.translatable("message.mechanical.fire_pit.fuel", pit.stock(), pit.stockMax()));
        }
        return InteractionResult.SUCCESS;
    }

    // ---- the big pit: a Steel Hearth Ring grows a 2x2 to 3x3 ----

    /**
     * Grows the pit at {@code controller} to 3x3: of the four 3x3s that hold it, the first (farthest from the player first,
     * so it grows away from you) whose five new blocks are free (replaceable, no fluid, solid ground under them) is taken.
     * None free: tells what stands in the way of the one closest to fitting. The block entity (food, firewood, fire) moves
     * to the new north-west corner, a tribe's centre moves with it (Clans.pitMoved), tripods that no longer stand right fall.
     */
    public static boolean upgrade(ServerLevel level, BlockPos controller, Player player) {
        List<BlockPos> corners = new ArrayList<>(List.of(controller.offset(-1, 0, -1), controller.offset(0, 0, -1), controller.offset(-1, 0, 0), controller));
        corners.sort(Comparator.comparingDouble(c -> -player.position().distanceToSqr(c.getX() + 1.5, c.getY(), c.getZ() + 1.5)));
        List<BlockPos> best = null;
        for (BlockPos corner : corners) {
            List<BlockPos> blocked = blockers(level, corner, controller);
            if (blocked.isEmpty()) {
                grow(level, controller, corner);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) FirePit.award(sp, "iron_age/big_fire_pit");
                return true;
            }
            if (best == null || blocked.size() < best.size()) best = blocked;
        }
        net.minecraft.network.chat.MutableComponent list = Component.empty();
        for (int i = 0; i < Math.min(3, best.size()); i++) {
            BlockPos p = best.get(i);
            if (i > 0) list.append(", ");
            list.append(Component.translatable("message.mechanical.fire_pit.big_blocker", level.getBlockState(p).getBlock().getName(), p.getX(), p.getY(), p.getZ()));
        }
        if (best.size() > 3) list.append(Component.translatable("message.mechanical.fire_pit.big_more", best.size() - 3));
        player.sendSystemMessage(Component.translatable("message.mechanical.fire_pit.big_blocked", list));
        return false;
    }

    /** What keeps the 3x3 at {@code corner} from being built round the 2x2 at {@code small}: blocks in the way, or the air under a new block. */
    static List<BlockPos> blockers(Level level, BlockPos corner, BlockPos small) {
        List<BlockPos> out = new ArrayList<>();
        for (int part = 0; part < 9; part++) {
            BlockPos p = partPos(corner, part, 3);
            int dx = p.getX() - small.getX(), dz = p.getZ() - small.getZ();
            if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) continue; // the pit itself
            BlockState s = level.getBlockState(p);
            if (!s.canBeReplaced() || !level.getFluidState(p).isEmpty() || !level.getWorldBorder().isWithinBounds(p)) out.add(p);
            else if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) out.add(p.below());
        }
        return out;
    }

    private static void grow(ServerLevel level, BlockPos oldController, BlockPos corner) {
        BlockState old = level.getBlockState(oldController);
        FirePitBlockEntity held = null;
        if (level.getBlockEntity(oldController) instanceof FirePitBlockEntity pit) {
            held = new FirePitBlockEntity(oldController, old);
            pit.moveTo(held); // out first, so nothing can drop while the blocks change
        }
        BlockState big = old.setValue(BIG, true).setValue(FUEL, 0);
        for (int part = 0; part < 9; part++) level.setBlock(partPos(corner, part, 3), big.setValue(PART, part), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE);
        if (!corner.equals(oldController) && level.getBlockEntity(oldController) != null) level.removeBlockEntity(oldController);
        if (held != null && level.getBlockEntity(corner) instanceof FirePitBlockEntity pit) {
            held.moveTo(pit);
            pit.refresh();
        }
        for (int part = 0; part < 9; part++) {
            BlockPos up = partPos(corner, part, 3).above();
            BlockState t = level.getBlockState(up);
            if (t.getBlock() instanceof TripodBlock && !t.canSurvive(level, up)) level.destroyBlock(up, true);
        }
        com.mechanical.clan.Clans.pitMoved(level, oldController, corner);
        level.playSound(null, corner.offset(1, 0, 1), net.minecraft.sounds.SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.7F);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (state.getValue(LIT) && entity instanceof LivingEntity) entity.hurt(level.damageSources().campfire(), 1.0F);
        super.entityInside(state, level, pos, entity, effects, isPrecise);
    }

    /** Wind and rain (Wind.firePitTick), on the corner only so the pit isn't rolled four times. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT) && state.getValue(PART) == 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Wind.firePitTick(level, pos, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Fire for pathfinding, lit or not: mobs keep off it and tribespeople (who can't path through fire) stop at its edge. */
    @Override
    public net.minecraft.world.level.pathfinder.PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.Mob mob) {
        return net.minecraft.world.level.pathfinder.PathType.FIRE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new FirePitBlockEntity(pos, state) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != FirePit.FIRE_PIT_ENTITY.get()) return null;
        return level.isClientSide()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<FirePitBlockEntity>) FirePitBlockEntity::clientTick
                : (BlockEntityTicker<T>) (BlockEntityTicker<FirePitBlockEntity>) (l, p, s, pit) -> pit.serverTick((ServerLevel) l, p, s);
    }
}
