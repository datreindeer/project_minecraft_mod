package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Two-block clay kiln. The item can only be placed on a campfire, which becomes the LOWER half (firebox, holds the
 * block entity); the UPPER half is the baking chamber with the chimney. Breaking either half removes both and gives
 * back the kiln and the campfire. LIT lives on both halves so the model and light follow the fire.
 */
public class KilnBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Lit and baking (fuel burning): more fire and smoke. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** How far the clay is fired: 0 raw (a new kiln, can't bake yet) .. 3 fired. Default 3 so kilns from older worlds stay fired. */
    public static final IntegerProperty FIRING = IntegerProperty.create("firing", 0, 3);
    public static final int FIRED = 3;
    /** Overheating strain shown as cracks: 1-2 strained, 3 cracked (stays under 600° until patched with Clay Balls). */
    public static final IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, 3);
    public static final int CRACKED = 3;
    /** Lit and at least {@link KilnBlockEntity#HOT}°: the fire opening glows and the flames are brighter. */
    public static final BooleanProperty HOT = BooleanProperty.create("hot");
    /** A Bronze Fire Grate in the firebox (FirePit.FIRE_GRATE); only ever true on the LOWER half. */
    public static final BooleanProperty GRATE = BooleanProperty.create("grate");

    private static final VoxelShape LOWER_SHAPE = column(15, 0, 16);
    private static final VoxelShape UPPER_SHAPE = Shapes.or(column(13, 0, 6), column(11, 6, 8), column(9, 8, 10), column(6, 10, 12), column(3, 12, 16));

    public KilnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(LIT, false).setValue(ACTIVE, false).setValue(FIRING, FIRED).setValue(CRACKS, 0).setValue(HOT, false).setValue(GRATE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, LIT, ACTIVE, FIRING, CRACKS, HOT, GRATE);
    }

    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    // ---- placement: only on top of a campfire, which turns into the firebox ----

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState campfire = context.getLevel().getBlockState(pos.below());
        if (!campfire.is(Blocks.CAMPFIRE)) {
            if (context.getPlayer() != null && context.getLevel().isClientSide()) {
                context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.kiln.needs_campfire"));
            }
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.UPPER)
                .setValue(LIT, campfire.getValue(CampfireBlock.LIT))
                .setValue(FIRING, 0); // raw clay; a broken fired kiln's item puts its firing back (block_state component)
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
        boolean lit = state.getValue(LIT);
        level.setBlock(pos.below(), state.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
        // a burning campfire's own logs keep the kiln going for a while
        if (lit && level.getBlockEntity(pos.below()) instanceof KilnBlockEntity kiln) kiln.startBurning(KilnBlockEntity.CAMPFIRE_BURN_TICKS);
    }

    // ---- the two halves live and die together ----

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (direction == (lower ? Direction.UP : Direction.DOWN)) {
            boolean partner = neighbourState.is(this) && neighbourState.getValue(HALF) != state.getValue(HALF);
            return partner ? state.setValue(LIT, neighbourState.getValue(LIT)).setValue(ACTIVE, neighbourState.getValue(ACTIVE)).setValue(FIRING, neighbourState.getValue(FIRING))
                    .setValue(CRACKS, neighbourState.getValue(CRACKS)).setValue(HOT, neighbourState.getValue(HOT))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Remove the other half ourselves without neighbour updates (so this half is still there for vanilla to break),
        // dropping its loot: the upper half gives the kiln, the lower half the campfire (+ contents via its block entity).
        if (!level.isClientSide()) {
            BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this)) {
                if (!player.preventsBlockDrops()) dropResources(otherState, level, other, level.getBlockEntity(other), player, player.getMainHandItem());
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- behaviour ----

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    // Holding a fire drill: skip opening the GUI so the drill's own use (lighting) runs.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof FireDrillItem) return InteractionResult.PASS;
        // a Bronze Fire Grate goes into the firebox
        if (stack.is(FirePit.FIRE_GRATE.get())) {
            BlockPos lower = lowerPos(state, pos);
            BlockState firebox = level.getBlockState(lower);
            if (!firebox.is(this) || firebox.getValue(GRATE)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                level.setBlock(lower, firebox.setValue(GRATE, true), Block.UPDATE_ALL);
                level.playSound(null, lower, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        // a cracked kiln is patched with Clay Balls, one per click
        if (stack.is(net.minecraft.world.item.Items.CLAY_BALL) && state.getValue(CRACKS) >= CRACKED) {
            if (!level.isClientSide() && level.getBlockEntity(lowerPos(state, pos)) instanceof KilnBlockEntity kiln) {
                stack.consume(1, player);
                int n = kiln.patch();
                level.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
                player.sendOverlayMessage(Component.translatable(n >= KilnBlockEntity.REPAIR_CLAY ? "message.mechanical.kiln.patched"
                        : "message.mechanical.kiln.patching", n, KilnBlockEntity.REPAIR_CLAY));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(lowerPos(state, pos)) instanceof KilnBlockEntity kiln) {
            player.openMenu(kiln);
            if (state.getValue(CRACKS) >= CRACKED) player.sendOverlayMessage(Component.translatable("message.mechanical.kiln.cracked"));
            else if (state.getValue(FIRING) < FIRED) player.sendOverlayMessage(Component.translatable("message.mechanical.kiln.unfired"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        boolean active = state.getValue(ACTIVE), hot = state.getValue(HOT);
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            if (random.nextInt(active ? 2 : 4) == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.07, 0.0);
            }
            return;
        }
        // Flames licking out of the fire opening at the front (the model's front is its north side, turned to FACING).
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.5, z = pos.getZ() + 0.5 + front.getStepZ() * 0.5;
        int flames = hot ? 5 : active ? 3 : random.nextInt(3) == 0 ? 1 : 0;
        for (int i = 0; i < flames; i++) {
            double side = (random.nextDouble() - 0.5) * 0.35; // across the opening
            double px = x + (front.getAxis() == Direction.Axis.Z ? side : 0), pz = z + (front.getAxis() == Direction.Axis.X ? side : 0);
            double out = 0.01 + random.nextDouble() * 0.02;
            level.addParticle(active ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, px, pos.getY() + 0.1 + random.nextDouble() * 0.3, pz,
                    front.getStepX() * out, 0.02, front.getStepZ() * out);
        }
        if (hot && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.3, z, 0, 0, 0); // sparks spitting out of the white-hot opening
        }
        if (active && random.nextInt(8) == 0) {
            level.playLocalSound(x, pos.getY() + 0.3, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F, false);
        }
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
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new KilnBlockEntity(pos, state) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Mechanical.KILN_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<KilnBlockEntity>) (l, pos, s, kiln) -> KilnBlockEntity.serverTick((ServerLevel) l, pos, s, kiln);
    }
}
