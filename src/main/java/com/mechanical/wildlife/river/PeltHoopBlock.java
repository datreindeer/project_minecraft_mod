package com.mechanical.wildlife.river;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stretching Hoop: a bent willow ring on two feet that a small, cased pelt (beaver, otter) is laced into to dry flat,
 * the old trapper's way. Right-click with a raw pelt to lace it in; it cures outdoors by day in dry weather (about
 * {@link #DRY_CHANCE} per random tick, ~4 minutes); empty hand takes it off (raw or cured). The pelt sits in the
 * blockstate; breaking the hoop drops it too (loot table).
 */
public class PeltHoopBlock extends Block {
    public enum Pelt implements StringRepresentable {
        EMPTY(() -> null), BEAVER(() -> RiverFauna.RAW_BEAVER_PELT.get()), BEAVER_DRY(() -> RiverFauna.BEAVER_PELT.get()),
        OTTER(() -> RiverFauna.RAW_OTTER_PELT.get()), OTTER_DRY(() -> RiverFauna.OTTER_PELT.get());

        final Supplier<Item> item;

        Pelt(Supplier<Item> item) {
            this.item = item;
        }

        boolean wet() {
            return this == BEAVER || this == OTTER;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Pelt> PELT = EnumProperty.create("pelt", Pelt.class);
    static final float DRY_CHANCE = 1.0F / 4.0F;
    private static final int OUTDOORS_SKYLIGHT = 10;
    private static final VoxelShape SHAPE_NS = box(1, 0, 6.5, 15, 15, 9.5), SHAPE_EW = box(6.5, 0, 1, 9.5, 15, 15);

    public PeltHoopBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PELT, Pelt.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PELT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Pelt pelt = stack.is(RiverFauna.RAW_BEAVER_PELT.get()) ? Pelt.BEAVER : stack.is(RiverFauna.RAW_OTTER_PELT.get()) ? Pelt.OTTER : null;
        if (pelt == null || state.getValue(PELT) != Pelt.EMPTY) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(PELT, pelt), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Pelt pelt = state.getValue(PELT);
        if (pelt == Pelt.EMPTY) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(pelt.item.get()), net.minecraft.util.Prediction.SERVER_ONLY);
            level.setBlock(pos, state.setValue(PELT, Pelt.EMPTY), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PELT).wet();
    }

    /** Outdoors (shade is fine) in the day, no rain on it: like a pelt on the Drying Rack. */
    static boolean dryWeather(Level level, BlockPos pos) {
        return level.isBrightOutside() && level.getBrightness(LightLayer.SKY, pos) >= OUTDOORS_SKYLIGHT && !level.isRainingAt(pos.above());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Pelt pelt = state.getValue(PELT);
        if (pelt.wet() && dryWeather(level, pos) && random.nextFloat() < DRY_CHANCE) {
            level.setBlock(pos, state.setValue(PELT, pelt == Pelt.BEAVER ? Pelt.BEAVER_DRY : Pelt.OTTER_DRY), Block.UPDATE_ALL);
        }
    }
}
