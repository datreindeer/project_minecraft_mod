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
    }

    /** The block a melt casts into here, or null. */
    public static @Nullable Item block(Molten m) {
        return switch (m) {
            case COPPER -> Items.COPPER_BLOCK.weathering().unaffected();
            case GOLD -> Items.GOLD_BLOCK;
            case LEAD -> HighFurnace.LEAD_BLOCK_ITEM.get();
            case BRONZE, POOR_BRONZE -> HighFurnace.BRONZE_BLOCK_ITEM.get();
            case STEEL -> HighFurnace.STEEL_BLOCK_ITEM.get();
            case SLAG -> HighFurnace.SLAG_STONE_ITEM.get();
            case BASALT -> HighFurnace.CAST_BASALT_ITEM.get();
            case GLASS -> Items.GLASS;
            case OBSIDIAN -> Items.OBSIDIAN;
            default -> null;
        };
    }

    public static int cast(Molten m) {
        return m.nonMetal ? STONE : Molten.BLOCK;
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
        Item out = block(b.molten);
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

        public Entity(BlockPos pos, BlockState state) {
            super(HighFurnace.FOUNDRY_BASIN_ENTITY.get(), pos, state);
        }

        public @Nullable Molten molten() {
            return molten;
        }

        /** 0..1 how full. */
        public float fill() {
            return molten == null ? 0 : Math.min(1, filled / (float) cast(molten));
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
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (molten != null) output.putString("molten", molten.id);
            output.putInt("filled", filled);
            output.putLong("set_at", setAt);
            output.putLong("poured_at", pouredAt);
            output.putBoolean("whole", whole);
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
            if (!(level.getBlockEntity(at) instanceof Entity b) || block(m) == null) return 0;
            return b.whole || b.molten != null && b.molten != m ? 0 : cast(m);
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
