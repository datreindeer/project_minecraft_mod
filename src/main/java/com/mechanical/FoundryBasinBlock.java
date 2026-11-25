package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Foundry Basin (docs/steel-furnace-plan.md section 6): a deep riveted steel tub lined with silica. Empty, it casts
 * a whole block: {@link Molten#BLOCK} mB of a metal (Copper, Gold, Lead, Bronze, Steel Block) or {@link #STONE} of slag,
 * basalt, glass or obsidian (Slag Stone, Cast Basalt, Glass, Obsidian). The fill is seen rising; it sets in 10 s per
 * 1000 mB, and an empty hand takes the block out. (Loam Molds for bells and great kettles come with phase 6.)
 */
public class FoundryBasinBlock extends Block implements EntityBlock {
    public static final int STONE = 250;
    private static final VoxelShape SHAPE = Shapes.join(box(0, 0, 0, 16, 14, 16), box(2, 3, 2, 14, 14, 14), net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST);

    public FoundryBasinBlock(Properties properties) {
        super(properties);
        registerDefaultState(ChannelFlow.shut(stateDefinition.any()));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(net.minecraft.world.level.block.PipeBlock.NORTH, net.minecraft.world.level.block.PipeBlock.EAST, net.minecraft.world.level.block.PipeBlock.SOUTH,
                net.minecraft.world.level.block.PipeBlock.WEST);
    }

    // open sides where a trough joins it (ChannelFlow.connect): the rim notched or a chute hung over it there
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return ChannelFlow.connect(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
            net.minecraft.core.Direction direction, BlockPos neighbourPos, BlockState neighbourState, net.minecraft.util.RandomSource random) {
        return ChannelFlow.connect(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (state != oldState) ChannelFlow.castPlaced(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        ClayChannelBlock.refreshAround(level, pos);
    }

    /** The block a melt casts into here (the metal table's), or null. */
    public static @Nullable Item block(Molten m) {
        ItemStack b = m.block();
        return b.isEmpty() ? null : b.getItem();
    }

    public static int cast(Molten m) {
        return m.nonMetal ? STONE : Molten.BLOCK;
    }

    /** The great one-off casts a Loam Mold in the basin takes: the melt, how much, what it gives. */
    public enum Loam {
        BELL(Molten.BRONZE, 1600), CAULDRON(Molten.PIG_IRON, 1400);

        public final Molten molten;
        public final int mb;

        Loam(Molten molten, int mb) {
            this.molten = molten;
            this.mb = mb;
        }

        Item result() {
            return this == BELL ? Items.BELL : Items.CAULDRON;
        }

        Item mold() {
            return this == BELL ? HighFurnace.LOAM_MOLD_BELL.get() : HighFurnace.LOAM_MOLD_CAULDRON.get();
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Entity b) || b.molten != null || b.loam != null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        for (Loam l : Loam.values())
            if (stack.is(l.mold())) {
                if (level instanceof ServerLevel server) {
                    b.loam = l;
                    b.sync();
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    server.playSound(null, pos, SoundEvents.DECORATED_POT_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                return InteractionResult.SUCCESS;
            }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof Entity b) || b.molten == null) return InteractionResult.PASS;
        if (server.getGameTime() < b.setAt) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.ingot_mold.molten"));
            return InteractionResult.SUCCESS;
        }
        Item out = b.loam != null ? b.loam.result() : block(b.molten);
        if (b.loam != null) { // the loam is broken off the casting
            if (b.loam == Loam.BELL) Smelting.award(server, pos, HighFurnace.BELL_ADVANCEMENT, 8);
            b.loam = null;
            server.playSound(null, pos, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
        if (out != null) {
            ItemStack stack = new ItemStack(out);
            if (!player.getInventory().add(stack)) Block.popResource(level, pos.above(), stack);
        }
        b.molten = null;
        b.filled = 0;
        b.sync();
        server.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, 0.7F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    public static class Entity extends BlockEntity {
        @Nullable Molten molten;
        /** mB in it (rising while the stream runs; the whole cast once poured). */
        int filled;
        long setAt, pouredAt;
        boolean whole;
        @Nullable Loam loam;

        public @Nullable Loam loam() {
            return loam;
        }

        public Entity(BlockPos pos, BlockState state) {
            super(HighFurnace.FOUNDRY_BASIN_ENTITY.get(), pos, state);
        }

        public @Nullable Molten molten() {
            return molten;
        }

        /** 0..1 how full. */
        public float fill() {
            return molten == null ? 0 : Math.min(1, filled / (float) (loam != null ? loam.mb : cast(molten)));
        }

        public float cooled(long now) {
            return !whole || molten == null ? 0 : Math.clamp(1 - (setAt - now) / (float) Math.max(1, setAt - pouredAt), 0, 1);
        }

        void sync() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            molten = Molten.byId(input.getStringOr("molten", ""));
            filled = input.getIntOr("filled", 0);
            setAt = input.getLongOr("set_at", 0);
            pouredAt = input.getLongOr("poured_at", 0);
            whole = input.getBooleanOr("whole", false);
            String l = input.getStringOr("loam", "");
            loam = l.isEmpty() ? null : Loam.valueOf(l);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (molten != null) output.putString("molten", molten.id);
            output.putInt("filled", filled);
            output.putLong("set_at", setAt);
            output.putLong("poured_at", pouredAt);
            output.putBoolean("whole", whole);
            if (loam != null) output.putString("loam", loam.name());
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }
    }

    static final class Sink implements PouringSpoutBlockEntity.Sink {
        @Override
        public int need(ServerLevel level, BlockPos at, Molten m) {
            if (!(level.getBlockEntity(at) instanceof Entity b) || b.whole || b.molten != null && b.molten != m) return 0;
            if (b.loam != null) return b.loam.molten == m ? b.loam.mb : 0;
            return block(m) == null ? 0 : cast(m);
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb) {
            if (!(level.getBlockEntity(at) instanceof Entity b)) return;
            b.molten = m;
            b.filled = mb;
            b.whole = true;
            b.pouredAt = level.getGameTime();
            b.setAt = b.pouredAt + FoundryTableBlock.setTicks(level, at, Math.max(60, mb / 5));
            b.sync();
        }

        @Override
        public void progress(ServerLevel level, BlockPos at, Molten m, int poured, int need) {
            if (level.getBlockEntity(at) instanceof Entity b) {
                b.molten = m;
                b.filled = poured;
                b.sync();
            }
        }

        @Override
        public void abort(ServerLevel level, BlockPos at) {
            if (level.getBlockEntity(at) instanceof Entity b && !b.whole) {
                b.molten = null;
                b.filled = 0;
                b.sync();
            }
        }

        @Override
        public net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            float f = need <= 0 ? 0 : Math.min(1, poured / (float) need);
            return new net.minecraft.world.phys.Vec3(0.5, (3 + 10 * f) / 16, 0.5);
        }
    }
}
