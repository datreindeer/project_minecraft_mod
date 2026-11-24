package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Refractory Trough (docs/steel-furnace-plan.md section 6): the steel age's channel, an iron trough lined with silica
 * brick. It joins and slopes exactly like a Clay Channel (same shape and ports), holds {@link TroughBlockEntity#CAPACITY}
 * mB of any melt at any heat and moves it by {@link TroughFlow}. Set metal is knocked out with a pickaxe: it gives
 * back the ingots and nuggets it held (what is under a nugget is lost).
 */
public class RefractoryTroughBlock extends ClayChannelBlock {
    public RefractoryTroughBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TroughBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != HighFurnace.TROUGH_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<TroughBlockEntity>) (l, p, s, t) -> TroughFlow.tick((ServerLevel) l, p, t);
    }

    /** Molten metal burns what walks into it, like lava (no armour or tongs help). */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof TroughBlockEntity t && t.molten() != null && !t.solid(server.getGameTime())) {
            entity.igniteForSeconds(4);
            entity.hurtServer(server, server.damageSources().lava(), 4.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ItemTags.PICKAXES)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof TroughBlockEntity t)) return InteractionResult.SUCCESS;
        if (t.molten() == null || !t.solid(server.getGameTime())) {
            player.sendOverlayMessage(Component.translatable(t.molten() == null ? "message.mechanical.trough.empty" : "message.mechanical.trough.molten"));
            return InteractionResult.SUCCESS;
        }
        knockOut(server, pos, t);
        return InteractionResult.SUCCESS;
    }

    /** Set metal out as ingots and nuggets (22 mB each); less than a nugget is lost. */
    static void knockOut(ServerLevel level, BlockPos pos, TroughBlockEntity t) {
        Molten m = t.molten();
        int mb = t.take(t.amount(), level.getGameTime());
        ItemStack ingot = m == null ? null : IngotMoldBlock.ingot(m);
        if (ingot != null) {
            ingot.setCount(mb / Molten.INGOT);
            if (!ingot.isEmpty()) Block.popResource(level, pos, ingot);
            ItemStack nugget = HighFurnaceMelting.nugget(m);
            if (nugget != null) {
                nugget.setCount(mb % Molten.INGOT / Molten.NUGGET);
                if (!nugget.isEmpty()) Block.popResource(level, pos, nugget);
            }
        }
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.7F);
    }

    /**
     * A Sand Runner (the "sow" of a Pig Bed): a trough dug with a shovel in Casting Sand on the floor. It joins like any
     * trough at its own level (no slopes) and fills the Pig Molds beside it, but sand draws the heat: the metal sets in half
     * the time and runs half as far. Once the metal has set a shovel digs it out as ingots and nuggets, and the sand is
     * burnt: dig a fresh runner in new Casting Sand.
     */
    public static class SandRunner extends RefractoryTroughBlock {
        public SandRunner(Properties properties) {
            super(properties);
        }

        @Override
        public BlockState stateAt(LevelReader level, BlockPos pos) {
            BlockState s = defaultBlockState();
            for (var e : SIDES.entrySet()) s = s.setValue(e.getValue(), ChannelFlow.across(level, pos, ChannelFlow.port(pos, e.getKey(), pos.getY())) != null);
            return s;
        }

        @Override
        protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context) {
            return net.minecraft.world.phys.shapes.Shapes.block();
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(ItemTags.SHOVELS)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof TroughBlockEntity t)) return InteractionResult.SUCCESS;
            if (t.molten() != null && !t.solid(server.getGameTime())) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.trough.molten"));
                return InteractionResult.SUCCESS;
            }
            if (t.molten() != null) knockOut(server, pos, t);
            server.setBlock(pos, HighFurnace.BURNT_CASTING_SAND.get().defaultBlockState(), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * A Trough Gate: a straight trough along {@link #AXIS} with an iron sluice gate. Right-click raises and lowers it;
     * shut, it has no ports at all ({@link ChannelFlow#ports}), so the metal stands and waits behind it.
     */
    public static class Gate extends RefractoryTroughBlock {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

        public Gate(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(OPEN, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(AXIS, OPEN);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return shaped(context.getLevel(), context.getClickedPos(), context.getHorizontalDirection().getAxis(), false);
        }

        /** Arms only along its axis (where something joins), never a slope. */
        @Override
        public BlockState stateAt(LevelReader level, BlockPos pos) {
            BlockState here = level.getBlockState(pos);
            return shaped(level, pos, here.is(this) ? here.getValue(AXIS) : Direction.Axis.X, here.is(this) && here.getValue(OPEN));
        }

        private BlockState shaped(LevelReader level, BlockPos pos, Direction.Axis axis, boolean open) {
            BlockState s = defaultBlockState().setValue(AXIS, axis).setValue(OPEN, open);
            for (var e : SIDES.entrySet()) {
                boolean along = e.getKey().getAxis() == s.getValue(AXIS);
                s = s.setValue(e.getValue(), along && ChannelFlow.across(level, pos, ChannelFlow.port(pos, e.getKey(), pos.getY())) != null);
            }
            return s;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            boolean open = !state.getValue(OPEN);
            server.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
            server.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
            return InteractionResult.SUCCESS;
        }
    }
}
