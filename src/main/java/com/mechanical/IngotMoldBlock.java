package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Iron Ingot Mold (docs/steel-furnace-plan.md section 6, "Iron Ingot / Plate / Nugget Mold"): a cast-iron plate with
 * four ingot pockets that never cracks. Under a Pouring Spout it takes {@link #CAST} mB of any metal the High Furnace
 * melts and gives 4 ingots of it once set ({@link #SET_TICKS}); an empty hand takes them out. The melt is kept in its
 * block entity (not a block state: the Clay Mold's METAL stays small). Drawn by client/IngotMoldRenderer.
 */
public class IngotMoldBlock extends Block implements EntityBlock {
    public static final int CAST = 800, INGOTS = 4, SET_TICKS = 100;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 4, 15);

    /** The permanent cast-iron molds: what one cast takes, what it gives, and its pockets (pixels x0 x1 z0 z1, filled in turn). */
    public enum Kind {
        INGOT(CAST, new float[][] {{2.5F, 7.5F, 2.5F, 7.5F}, {8.5F, 13.5F, 2.5F, 7.5F}, {8.5F, 13.5F, 8.5F, 13.5F}, {2.5F, 7.5F, 8.5F, 13.5F}}),
        PLATE(400, new float[][] {{2.5F, 13.5F, 2.5F, 13.5F}}),
        NUGGET(400, grid()),
        /** A Pig Mold: a trench at the top of a sand block. */
        PIG(400, new float[][] {{3, 13, 5.5F, 10.5F}}, 13, 15.5F);

        public final int cast;
        public final float[][] pockets;
        /** The metal's floor and top in a pocket (pixels). */
        public final float floor, top;

        Kind(int cast, float[][] pockets) {
            this(cast, pockets, FLOOR, TOP);
        }

        Kind(int cast, float[][] pockets, float floor, float top) {
            this.cast = cast;
            this.pockets = pockets;
            this.floor = floor;
            this.top = top;
        }

        /**
         * The gate cut through the rim on side {@code d} where a trough joins (pixels x0 x1 z0 z1 and its floor): the
         * iron molds' 6 px notch over their 1 px lip into the pockets, a Pig Mold's narrow 3 px gate down to its trench.
         */
        public float[] gate(net.minecraft.core.Direction d) {
            float w0 = this == PIG ? 6.5F : 5, w1 = 16 - w0, depth = this == PIG ? (d.getAxis() == net.minecraft.core.Direction.Axis.Z ? 5.5F : 3) : 2.5F;
            float f = this == PIG ? 14 : floor;
            return switch (d) {
                case NORTH -> new float[] {w0, w1, 0, depth, f};
                case SOUTH -> new float[] {w0, w1, 16 - depth, 16, f};
                case WEST -> new float[] {0, depth, w0, w1, f};
                default -> new float[] {16 - depth, 16, w0, w1, f};
            };
        }

        private static float[][] grid() {
            float[][] g = new float[9][];
            for (int i = 0; i < 9; i++) {
                float x = 2.5F + (i % 3) * 4, z = 2.5F + (i / 3) * 4;
                g[i] = new float[] {x, x + 3, z, z + 3};
            }
            return g;
        }

        /** What a set cast of {@code m} gives, or null if this mold takes no {@code m}. */
        public @Nullable ItemStack result(Molten m) {
            return switch (this) {
                case INGOT -> {
                    ItemStack i = ingot(m);
                    if (i != null) i.setCount(INGOTS);
                    yield i;
                }
                case PLATE -> m.plate().isEmpty() ? null : m.plate();
                case PIG -> {
                    ItemStack i = ingot(m);
                    if (i != null) i.setCount(2);
                    yield i;
                }
                case NUGGET -> {
                    ItemStack n = HighFurnaceMelting.nugget(m);
                    if (n != null) n.setCount(18);
                    yield n;
                }
            };
        }
    }

    public final Kind kind;

    public IngotMoldBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
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

    /** The ingot a melt sets into (the metal table's), or null (glass, slag ... have none). */
    public static @Nullable ItemStack ingot(Molten m) {
        ItemStack out = m.ingot();
        return out.isEmpty() ? null : out;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof Entity mold)) return InteractionResult.SUCCESS;
        if (mold.molten == null) return InteractionResult.PASS;
        if (!mold.set(server.getGameTime())) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.ingot_mold.molten"));
            return InteractionResult.SUCCESS;
        }
        ItemStack out = kind.result(mold.molten);
        if (out != null) {
            if (!player.getInventory().add(out)) net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, out);
        }
        if (mold.molten == Molten.STEEL) Smelting.award(server, pos, HighFurnace.CAST_STEEL_ADVANCEMENT, 8);
        mold.clear();
        server.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    /** The metal's floor and top in a pocket. */
    public static final float FLOOR = 1.6F, TOP = 3.6F;

    /** What lies in the pockets: the melt, how much is in while it is being poured, and the game time it has set by. */
    public static class Entity extends BlockEntity {
        @Nullable Molten molten;
        long setAt;
        /** While the stream runs: the melt coming in and how much is in (the pockets fill one after another). */
        @Nullable Molten filling;
        int filled;

        public @Nullable Molten filling() {
            return filling;
        }

        /** Does a trough join it at a side (its rim is cut there)? */
        public boolean gated() {
            BlockState s = getBlockState();
            for (var p : ClayChannelBlock.SIDES.values()) if (s.hasProperty(p) && s.getValue(p)) return true;
            return false;
        }

        public Kind kind() {
            return getBlockState().getBlock() instanceof IngotMoldBlock b ? b.kind : Kind.INGOT;
        }

        /** How full pocket {@code i} is (0..1): all of it once poured, else what the stream has put in so far (fed through a side gate, all together). */
        public float pocket(int i) {
            Kind k = kind();
            int per = k.cast / k.pockets.length, in = molten != null ? k.cast : filling != null ? filled : 0;
            if (gated()) return Math.clamp(in / (float) k.cast, 0, 1);
            return Math.clamp((in - i * per) / (float) per, 0, 1);
        }

        public Entity(BlockPos pos, BlockState state) {
            super(HighFurnace.INGOT_MOLD_ENTITY.get(), pos, state);
        }

        public @Nullable Molten molten() {
            return molten;
        }

        public boolean set(long now) {
            return molten != null && now >= setAt;
        }

        /** 0 just poured .. 1 set (for the cooling colours). */
        public float cooled(long now) {
            return molten == null ? 0 : Math.clamp(1 - (setAt - now) / (float) SET_TICKS, 0, 1);
        }

        void pour(Molten m, long now) {
            molten = m;
            filling = null;
            filled = 0;
            setAt = now + FoundryTableBlock.setTicks(level, worldPosition, SET_TICKS);
            sync();
        }

        void clear() {
            molten = null;
            sync();
        }

        private void sync() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            molten = Molten.byId(input.getStringOr("molten", ""));
            filling = Molten.byId(input.getStringOr("filling", ""));
            filled = input.getIntOr("filled", 0);
            setAt = input.getLongOr("set_at", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (molten != null) output.putString("molten", molten.id);
            if (filling != null) output.putString("filling", filling.id);
            output.putInt("filled", filled);
            output.putLong("set_at", setAt);
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

    /** As a Pouring Spout's sink: an empty mold takes a whole {@link #CAST} of any metal with an ingot. */
    static final class Sink implements PouringSpoutBlockEntity.Sink {
        @Override
        public int need(ServerLevel level, BlockPos at, Molten m) {
            return level.getBlockEntity(at) instanceof Entity e && e.molten == null && e.kind().result(m) != null ? e.kind().cast : 0;
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb) {
            if (level.getBlockEntity(at) instanceof Entity e) e.pour(m, level.getGameTime());
        }

        /** The stream lands on the metal rising in the pocket now being filled. */
        @Override
        public net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            Kind k = level.getBlockEntity(at) instanceof Entity e ? e.kind() : Kind.INGOT;
            int per = k.cast / k.pockets.length, i = Math.min(k.pockets.length - 1, poured / per);
            float f = Math.clamp((poured - i * per) / (float) per, 0, 1);
            float[] p = k.pockets[i];
            return new net.minecraft.world.phys.Vec3((p[0] + p[1]) / 32, (k.floor + (k.top - k.floor) * f) / 16, (p[2] + p[3]) / 32);
        }

        @Override
        public void progress(ServerLevel level, BlockPos at, Molten m, int poured, int need) {
            if (level.getBlockEntity(at) instanceof Entity e) {
                e.filling = m;
                e.filled = poured;
                e.sync();
            }
        }

        @Override
        public void abort(ServerLevel level, BlockPos at) {
            if (level.getBlockEntity(at) instanceof Entity e && e.molten == null) {
                e.filling = null;
                e.filled = 0;
                e.sync();
            }
        }
    }
}
