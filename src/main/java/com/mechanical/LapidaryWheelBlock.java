package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hand-cranked grinding wheel for cutting Rough gems ({@link Gemstones}). Right-click with an abrasive loads it (one item
 * = {@link #PER_GRIT} gems, a different grit replaces the old); right-click with a Rough gem {@link #PASSES} times cuts it.
 * The grit must be at least as hard as the gem; only just hard enough, it may crack it into a Flawed gem. An empty hand
 * shows what is loaded. Everything lives in the block state, so no block entity.
 */
public class LapidaryWheelBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Tier of the loaded abrasive, 0 = none. */
    public static final IntegerProperty GRIT = IntegerProperty.create("grit", 0, 5);
    public static final IntegerProperty CHARGES = IntegerProperty.create("charges", 0, 8);
    public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, 3);
    static final int PER_GRIT = 4, MAX_CHARGES = 8, PASSES = 4;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 15, 15);

    public LapidaryWheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GRIT, 0).setValue(CHARGES, 0).setValue(PROGRESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GRIT, CHARGES, PROGRESS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (Gemstones.abrasive(stack) > 0) {
            if (level instanceof ServerLevel server) addGrit(server, pos, state, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (Gemstones.hardness(stack) > 0) {
            if (level instanceof ServerLevel server) grind(server, pos, state, player, stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int grit = state.getValue(GRIT);
            player.sendOverlayMessage(grit == 0 ? Component.translatable("message.mechanical.lapidary.empty")
                    : Component.translatable("message.mechanical.lapidary.loaded", gritName(grit), state.getValue(CHARGES)));
        }
        return InteractionResult.SUCCESS;
    }

    private static Component gritName(int tier) {
        return Component.translatable("message.mechanical.lapidary.grit." + tier);
    }

    static void addGrit(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
        int tier = Gemstones.abrasive(stack);
        boolean same = state.getValue(GRIT) == tier;
        if (same && state.getValue(CHARGES) > MAX_CHARGES - PER_GRIT) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.lapidary.full"));
            return;
        }
        int charges = (same ? state.getValue(CHARGES) : 0) + PER_GRIT;
        level.setBlockAndUpdate(pos, state.setValue(GRIT, tier).setValue(CHARGES, charges).setValue(PROGRESS, 0));
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.lapidary.loaded", gritName(tier), charges));
    }

    static void grind(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack rough) {
        int grit = state.getValue(GRIT), hard = Gemstones.hardness(rough);
        if (grit == 0 || state.getValue(CHARGES) == 0) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.lapidary.empty"));
            return;
        }
        if (grit < hard) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.lapidary.too_hard", rough.getHoverName(), gritName(hard)));
            return;
        }
        int progress = state.getValue(PROGRESS) + 1;
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.05);
        if (progress < PASSES) {
            level.setBlockAndUpdate(pos, state.setValue(PROGRESS, progress));
            player.sendOverlayMessage(Component.translatable("message.mechanical.lapidary.grinding", progress, PASSES));
            return;
        }
        ItemStack out = Gemstones.cut(rough, grit, level.getRandom());
        rough.consume(1, player);
        int charges = state.getValue(CHARGES) - 1;
        level.setBlockAndUpdate(pos, state.setValue(PROGRESS, 0).setValue(CHARGES, charges).setValue(GRIT, charges == 0 ? 0 : grit));
        boolean flawed = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(out.getItem()).getPath().startsWith("flawed_");
        if (flawed) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        else level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        player.sendOverlayMessage(Component.translatable(flawed ? "message.mechanical.lapidary.cracked" : "message.mechanical.lapidary.cut", out.getHoverName()));
        player.getInventory().placeItemBackInInventory(out, net.minecraft.util.Prediction.SERVER_ONLY);
    }
}
