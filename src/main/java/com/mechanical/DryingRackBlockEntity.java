package com.mechanical;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The generic goods on a Drying Rack ({@link #TABLE}): one item stack per spot (slot 0 = {@link DryingRackBlock#HIDE},
 * slot 1 = {@link DryingRackBlock#HIDE2}), drawn by DryingRackRenderer. Hides, pelts and food keep living in the blockstate
 * as before; a spot holds either a blockstate value or a stack here, never both. Racks from before this block entity get
 * an empty one when first touched.
 * <p>
 * A stack dries once a second while the weather is dry ({@link DryingRackBlock#dryRate}, the same rule as food): 2 out in
 * sun and wind, 1 sheltered, 0 at night or in rain. When it has its entry's {@code seconds} at the fast rate it turns into
 * the output (same count), which hangs there until taken with an empty hand.
 */
public class DryingRackBlockEntity extends BlockEntity {
    /** {@code perSpot} of {@code input} go on one spot and dry into as many {@code output} in {@code seconds} out in sun and wind (twice that sheltered). */
    public record Drying(Supplier<Item> input, Supplier<Item> output, int seconds, int perSpot) {}

    /** What dries on the rack besides hides, pelts and food. Add a row to dry something new. */
    public static final List<Drying> TABLE = List.of(
            new Drying(EarthBlocks.WET_ADOBE_BRICK, EarthBlocks.ADOBE_BRICK, 360, 4),
            new Drying(EarthBlocks.CUT_PEAT, EarthBlocks.DRIED_PEAT, 480, 4),
            new Drying(() -> EarthBlocks.REED.get().asItem(), EarthBlocks.DRIED_REED, 240, 8),
            new Drying(() -> Items.KELP, () -> Items.DRIED_KELP, 180, 8));

    public static final Identifier FIVE_KINDS = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/five_kinds_dried");
    private static final String KINDS = "mechanical_rack_kinds";

    private final ItemStack[] items = {ItemStack.EMPTY, ItemStack.EMPTY};
    private final int[] progress = new int[2];

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(Mechanical.DRYING_RACK_ENTITY.get(), pos, state);
    }

    public static Drying of(ItemStack stack) {
        for (Drying d : TABLE) if (stack.is(d.input().get())) return d;
        return null;
    }

    static int slot(IntegerProperty spot) {
        return spot == DryingRackBlock.HIDE ? 0 : 1;
    }

    public ItemStack item(int slot) {
        return items[slot];
    }

    /** A stack (drying or dried) hangs on this spot. */
    public static boolean occupied(Level level, BlockPos pos, IntegerProperty spot) {
        return level.getBlockEntity(pos) instanceof DryingRackBlockEntity be && !be.items[slot(spot)].isEmpty();
    }

    public static boolean empty(Level level, BlockPos pos) {
        return !(level.getBlockEntity(pos) instanceof DryingRackBlockEntity be) || be.items[0].isEmpty() && be.items[1].isEmpty();
    }

    void hang(IntegerProperty spot, ItemStack stack) {
        items[slot(spot)] = stack;
        progress[slot(spot)] = 0;
        sync();
    }

    /** Dried and ready to take. */
    boolean done(int slot) {
        return !items[slot].isEmpty() && of(items[slot]) == null;
    }

    ItemStack take(int slot) {
        ItemStack out = items[slot];
        items[slot] = ItemStack.EMPTY;
        progress[slot] = 0;
        sync();
        return out;
    }

    /** One second of drying at {@code rate} (0 none, 1 sheltered, 2 out in sun and wind). */
    public void dry(int rate) {
        if (rate <= 0) return;
        boolean turned = false;
        for (int i = 0; i < 2; i++) {
            Drying d = of(items[i]);
            if (d == null) continue;
            progress[i] += rate;
            if (progress[i] >= 2 * d.seconds()) {
                items[i] = new ItemStack(d.output().get(), items[i].getCount());
                progress[i] = 0;
                turned = true;
            }
        }
        if (turned) sync();
        else setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        if (level.getGameTime() % 20 != 0 || of(rack.items[0]) == null && of(rack.items[1]) == null) return;
        rack.dry(DryingRackBlock.dryRate(level, pos, state));
    }

    /** Remembers each kind a player took off a rack (hides, food, goods); the fifth different one gives the advancement. */
    static void taken(Player player, ItemStack product) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag kept = sp.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        Set<String> kinds = new LinkedHashSet<>(Arrays.asList(kept.getStringOr(KINDS, "").split(",")));
        kinds.remove("");
        if (!kinds.add(BuiltInRegistries.ITEM.getKey(product.getItem()).toString())) return;
        kept.putString(KINDS, String.join(",", kinds));
        sp.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        if (kinds.size() >= 5) Kettle.award(sp, FIVE_KINDS);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) for (ItemStack s : items) if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 2; i++) {
            if (!items[i].isEmpty()) output.store("item" + i, ItemStack.CODEC, items[i]);
            output.putInt("progress" + i, progress[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 2; i++) {
            items[i] = input.read("item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            progress[i] = input.getIntOr("progress" + i, 0);
        }
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
