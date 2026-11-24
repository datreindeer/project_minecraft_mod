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

    public IngotMoldBlock(Properties properties) {
        super(properties);
    }

    /** The ingot a melt sets into, or null (glass, slag ... have none). */
    public static @Nullable ItemStack ingot(Molten m) {
        Item i = switch (m) {
            case GOLD -> Items.GOLD_INGOT;
            case LEAD -> IronOres.LEAD_INGOT.get();
            case SILVER -> IronOres.SILVER_INGOT.get();
            case COPPER -> Items.COPPER_INGOT;
            case BRONZE, POOR_BRONZE -> Bronze.BRONZE_INGOT.get();
            case TIN -> Mechanical.TIN_INGOT.get();
            case PIG_IRON -> HighFurnace.PIG_IRON_INGOT.get();
            case STEEL -> Smithing.STEEL_INGOT.get();
            case WROUGHT_IRON -> Ironworking.WROUGHT_IRON_INGOT.get();
            default -> null;
        };
        if (i == null) return null;
        ItemStack out = new ItemStack(i);
        if (m == Molten.POOR_BRONZE) Bronze.markPoor(out);
        return out;
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
        ItemStack out = ingot(mold.molten);
        if (out != null) {
            out.setCount(INGOTS);
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

    /** The four pockets (pixels, x0 x1 z0 z1), filled in this order; and the metal's floor and top in a pocket. */
    public static final float[][] POCKETS = {{2.5F, 7.5F, 2.5F, 7.5F}, {8.5F, 13.5F, 2.5F, 7.5F}, {8.5F, 13.5F, 8.5F, 13.5F}, {2.5F, 7.5F, 8.5F, 13.5F}};
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

        /** mB in pocket {@code i}: all of it once poured, else what the stream has put in so far. */
        public float pocket(int i) {
            int per = CAST / INGOTS, in = molten != null ? CAST : filling != null ? filled : 0;
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
            setAt = now + SET_TICKS;
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
            return level.getBlockEntity(at) instanceof Entity e && e.molten == null && ingot(m) != null ? CAST : 0;
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb) {
            if (level.getBlockEntity(at) instanceof Entity e) e.pour(m, level.getGameTime());
        }

        /** The stream lands on the metal rising in the pocket now being filled. */
        @Override
        public net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            int per = CAST / INGOTS, i = Math.min(INGOTS - 1, poured / per);
            float f = Math.clamp((poured - i * per) / (float) per, 0, 1);
            float[] p = POCKETS[i];
            return new net.minecraft.world.phys.Vec3((p[0] + p[1]) / 32, (FLOOR + (TOP - FLOOR) * f) / 16, (p[2] + p[3]) / 32);
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
