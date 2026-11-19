package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Smelting Furnace's tap, in the middle of the bottom layer's front wall, spout (FACING) out. Use: the furnace's
 * status screen (or, if the furnace isn't complete, what is missing). Sneak + use with an empty hand: pull or push the
 * clay plug (OPEN). A Fire Drill lights it. LIT: the fire burns; POURING: metal is running out of the spout.
 */
public class FurnaceTapBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POURING = BooleanProperty.create("pouring");

    public FurnaceTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(LIT, false).setValue(POURING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, LIT, POURING);
    }

    /** Spout toward the player: the tap is set into the wall from outside. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof FireDrillItem) return InteractionResult.PASS; // the drill lights it
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof FurnaceTapBlockEntity tap)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            boolean open = !state.getValue(OPEN);
            server.setBlock(pos, state.setValue(OPEN, open).setValue(POURING, false), Block.UPDATE_ALL);
            server.playSound(null, pos, open ? SoundEvents.MUD_BREAK : SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.9F, open ? 1.2F : 0.9F);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(open ? "message.mechanical.furnace.tap_open" : "message.mechanical.furnace.tap_closed"));
            return InteractionResult.SUCCESS;
        }
        tap.check(server);
        if (tap.problem() != null) player.sendOverlayMessage(tap.problem());
        else player.openMenu(tap);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof FurnaceTapBlockEntity tap) tap.recheck();
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction front = state.getValue(FACING);
        if (state.getValue(LIT)) {
            // smoke and flames out of the shaft's mouth, two blocks up behind the tap
            BlockPos top = pos.relative(front.getOpposite()).above(3);
            double x = top.getX() + 0.3 + random.nextDouble() * 0.4, z = top.getZ() + 0.3 + random.nextDouble() * 0.4;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, top.getY(), z, 0, 0.07, 0);
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, x, top.getY() - 0.1, z, 0, 0.03, 0);
            if (random.nextInt(10) == 0) level.addParticle(ParticleTypes.LAVA, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0, 0);
            if (random.nextInt(12) == 0) level.playLocalSound(pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 0.9F, false);
        }
        if (state.getValue(POURING) && random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + front.getStepX() * 0.85, z = pos.getZ() + 0.5 + front.getStepZ() * 0.85;
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.35, z, 0, 0.02, 0);
            if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.3, z, 0, 0, 0);
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
        return new FurnaceTapBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Smelting.FURNACE_TAP_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<FurnaceTapBlockEntity>) (l, pos, s, tap) -> tap.serverTick((ServerLevel) l, s);
    }
}
