package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hanging Rack (slaktställning): two posts three blocks high, a crossbeam and a wooden gambrel hanging from it, built of
 * three blocks ({@link Part}, like a door) so each third has its own hitbox. A hauled carcass right-clicked onto any part
 * hangs head down by the hind legs from the bottom block's position ({@link CarcassEntity#hang}), its belly to the front
 * ({@code FACING}, towards whoever placed it). Needs the two blocks above free. Breaking any part removes the whole rack
 * (only the broken part drops the item) and drops the carcass.
 * <p>A Bronze Kettle or an iron Cauldron right-clicked onto the rack stands under the hook ({@link #KETTLE}) and catches
 * the blood ({@link #catchBlood}): no pool on the ground until it is full ({@link #FULL} steps of {@link #MB_PER_STEP} mB,
 * one bucket). A Bucket or Bronze Bucket scoops a full kettle into a Blood Bucket; sneaking with an empty hand takes it off.
 */
public class HangingRackBlock extends Block {
    /** What stands under the hook: a Bronze Kettle (Poor Bronze kept apart, so it comes back poor) or an iron Cauldron. */
    public enum Pot implements StringRepresentable {
        NONE, BRONZE, POOR_BRONZE, IRON;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum Part implements StringRepresentable {
        LOWER, MIDDLE, UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** Blood soaked into the ground under the rack (bottom part only): a pool that grows under a skinned carcass and dries. */
    public static final int MAX_BLOOD = 4;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty BLOOD =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("blood", 0, MAX_BLOOD);
    public static final EnumProperty<Pot> KETTLE = EnumProperty.create("kettle", Pot.class);
    /** Blood in the kettle, in steps of {@link #MB_PER_STEP} mB: {@link #FULL} is a bucket. */
    public static final int FULL = 20, MB_PER_STEP = 50;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty KETTLE_BLOOD =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("kettle_blood", 0, FULL);
    /** Chance per random tick that the pool dries a step (as {@link BloodBlock}). */
    private static final float DRY_CHANCE = 0.25F;
    /** Height of the gambrel hook above the rack's base, in blocks (the model's gambrel at y = 41 px). */
    public static final float HOOK = 41 / 16.0F;
    // per part, built facing north (beam along x): posts, and the crossbeam at 44-47 px in the top block
    private static final VoxelShape POSTS_X = Shapes.or(box(1, 0, 7, 3, 16, 9), box(13, 0, 7, 15, 16, 9));
    private static final VoxelShape POSTS_Z = Shapes.or(box(7, 0, 1, 9, 16, 3), box(7, 0, 13, 9, 16, 15));
    private static final VoxelShape TOP_X = Shapes.or(box(1, 0, 7, 3, 13, 9), box(13, 0, 7, 15, 13, 9), box(0, 12, 6.5, 16, 15, 9.5));
    private static final VoxelShape TOP_Z = Shapes.or(box(7, 0, 1, 9, 13, 3), box(7, 0, 13, 9, 13, 15), box(6.5, 12, 0, 9.5, 15, 16));

    public HangingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.LOWER).setValue(BLOOD, 0)
                .setValue(KETTLE, Pot.NONE).setValue(KETTLE_BLOOD, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, BLOOD, KETTLE, KETTLE_BLOOD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // the frame stands three blocks high
        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().isInsideBuildHeight(pos.above(2)) || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
                || !context.getLevel().getBlockState(pos.above(2)).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), Block.UPDATE_ALL);
        level.setBlock(pos.above(2), state.setValue(PART, Part.UPPER), Block.UPDATE_ALL);
    }

    /** A part whose neighbour part is gone goes too (no drop: only the part that was broken drops the rack). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Part part = state.getValue(PART);
        Part expected = direction == Direction.UP && part != Part.UPPER ? Part.values()[part.ordinal() + 1]
                : direction == Direction.DOWN && part != Part.LOWER ? Part.values()[part.ordinal() - 1] : null;
        if (expected != null && !(neighbourState.is(this) && neighbourState.getValue(PART) == expected
                && neighbourState.getValue(FACING) == state.getValue(FACING))) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** The bottom block of the rack that {@code state} at {@code pos} is part of. */
    static BlockPos base(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART).ordinal());
    }

    /** The beam runs across the front: along x when it faces north or south. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean x = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        return state.getValue(PART) == Part.UPPER ? (x ? TOP_X : TOP_Z) : x ? POSTS_X : POSTS_Z;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(BLOOD) > 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < DRY_CHANCE) level.setBlock(pos, state.setValue(BLOOD, state.getValue(BLOOD) - 1), UPDATE_ALL);
    }

    /** A drop of blood from the carcass hanging on the rack at {@code base}: into the kettle under it, else the pool under it grows, a full one runs over beside the rack. */
    static void bleedOnto(ServerLevel level, BlockPos base) {
        BlockState state = level.getBlockState(base);
        if (!state.is(CarcassHauling.HANGING_RACK.get()) || catchBlood(level, base, 1) == 0) return;
        if (state.getValue(BLOOD) < MAX_BLOOD) level.setBlock(base, state.setValue(BLOOD, state.getValue(BLOOD) + 1), UPDATE_ALL);
        else Blood.splatter(level, base, 1, 1);
    }

    /** Pours up to {@code drops} steps of blood into the kettle under the rack at {@code base}; returns what didn't fit (all of it with no kettle). */
    static int catchBlood(ServerLevel level, BlockPos base, int drops) {
        BlockState state = level.getBlockState(base);
        if (!state.is(CarcassHauling.HANGING_RACK.get()) || state.getValue(KETTLE) == Pot.NONE) return drops;
        int fill = state.getValue(KETTLE_BLOOD), caught = Math.min(drops, FULL - fill);
        if (caught > 0) level.setBlock(base, state.setValue(KETTLE_BLOOD, fill + caught), UPDATE_CLIENTS);
        return drops - caught;
    }

    /** The kettle as an item again. */
    private static ItemStack potItem(Pot pot) {
        if (pot == Pot.IRON) return new ItemStack(Items.CAULDRON);
        ItemStack kettle = new ItemStack(Bronze.BRONZE_KETTLE.get());
        if (pot == Pot.POOR_BRONZE) Bronze.markPoor(kettle);
        return kettle;
    }

    /** A Bronze Kettle or Cauldron goes under the hook; a Bucket or Bronze Bucket scoops a full kettle of blood. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState clicked, Level level, BlockPos clickedPos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = base(clickedPos, clicked);
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return InteractionResult.PASS;
        Pot pot = state.getValue(KETTLE);
        boolean kettle = stack.is(Bronze.BRONZE_KETTLE.get()), cauldron = stack.is(Items.CAULDRON);
        boolean bucket = stack.is(Items.BUCKET), bronzeBucket = stack.is(BronzeTools.BRONZE_BUCKET.get());
        if (pot == Pot.NONE && (kettle || cauldron)) {
            if (level instanceof ServerLevel server) {
                server.setBlock(pos, state.setValue(KETTLE, cauldron ? Pot.IRON : Bronze.isPoor(stack) ? Pot.POOR_BRONZE : Pot.BRONZE).setValue(KETTLE_BLOOD, 0), UPDATE_ALL);
                stack.consume(1, player);
                server.playSound(null, pos, cauldron ? SoundEvents.ANVIL_PLACE : SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.5F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (pot == Pot.NONE || !(bucket || bronzeBucket)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) {
            int fill = state.getValue(KETTLE_BLOOD);
            if (fill < FULL) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.blood_kettle.not_full", fill * MB_PER_STEP, FULL * MB_PER_STEP));
                return InteractionResult.SUCCESS;
            }
            server.setBlock(pos, state.setValue(KETTLE_BLOOD, 0), UPDATE_ALL);
            ItemStack filled = bucket ? new ItemStack(Kettle.BLOOD_BUCKET.get()) : stack.transmuteCopy(BronzeTools.BRONZE_BLOOD_BUCKET.get(), 1);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
            server.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Empty hand: how much blood the kettle holds; sneaking takes the kettle off (spilling what's in it). */
    @Override
    protected InteractionResult useWithoutItem(BlockState clicked, Level level, BlockPos clickedPos, Player player, BlockHitResult hit) {
        BlockPos pos = base(clickedPos, clicked);
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || state.getValue(KETTLE) == Pot.NONE || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            int fill = state.getValue(KETTLE_BLOOD);
            if (!player.isSecondaryUseActive()) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.blood_kettle.status", fill * MB_PER_STEP, FULL * MB_PER_STEP));
                return InteractionResult.SUCCESS;
            }
            server.setBlock(pos, state.setValue(KETTLE, Pot.NONE).setValue(KETTLE_BLOOD, 0), UPDATE_ALL);
            if (fill > 0) Blood.splatter(server, pos, 1, (fill + 3) / 4);
            player.getInventory().placeItemBackInInventory(potItem(state.getValue(KETTLE)), net.minecraft.util.Prediction.SERVER_ONLY);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The kettle comes off with the rack. Only called when the block is replaced by another (a state change of the rack
     * itself never gets here); a piston moves the state, kettle and all, so nothing drops then.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!movedByPiston && state.getValue(KETTLE) != Pot.NONE) popResource(level, pos, potItem(state.getValue(KETTLE)));
    }

    public static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(CarcassEntity.class, new AABB(pos).inflate(1, 3, 1), c -> pos.equals(c.hungAt())).isEmpty();
    }

    /** Hangs {@code carcass} (hauled by {@code player}) on the rack at {@code pos}, if it fits and the hook is free. */
    static void tryHang(ServerLevel level, BlockPos clicked, BlockState state, CarcassEntity carcass, Player player) {
        BlockPos pos = base(clicked, state);
        if (carcass.isHuge() || carcass.isColossal()) player.sendOverlayMessage(Component.translatable("message.mechanical.hanging_rack.too_big"));
        else if (occupied(level, pos)) player.sendOverlayMessage(Component.translatable("message.mechanical.hanging_rack.occupied"));
        else {
            carcass.hang(pos, state.getValue(FACING));
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
    }
}
