package com.mechanical.wildlife.coldhunters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.DryingRackBlock;
import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Raw cold-hunter pelts ({@link ColdhuntersFauna#CURES}) cure on the Drying Rack like a Scraped Hide: right-click a rack
 * with one and it is stretched on a free spot (the rack's own "hide drying" state, so it dries the same ~3 minutes and is
 * drawn as a stretched skin). Which pelt hangs on which spot is remembered here, per world, so taking the dry spot with an
 * empty hand gives the cured fur instead of Leather, and breaking the rack gives the pelt back instead of a hide.
 * None of these pelts is big enough to need the tall rack (they are smaller than a wolf's).
 * Records whose spot has since been emptied are dropped whenever the rack is used, so a later hide is never mistaken
 * for a pelt.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class PeltCuring extends SavedData {
    private static final Codec<PeltCuring> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(m -> new PeltCuring(new HashMap<>(m)), d -> d.spots);
    private static final SavedDataType<PeltCuring> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "pelt_curing"),
            () -> new PeltCuring(new HashMap<>()), CODEC);
    private static final int WET = DryingRackBlock.HIDE_WET, DRY = 2;

    /** "x,y,z,hide|hide2" -> id of the raw pelt hanging there. */
    private final Map<String, String> spots;

    private PeltCuring(Map<String, String> spots) {
        this.spots = spots;
    }

    private static PeltCuring of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static String key(BlockPos pos, IntegerProperty spot) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + spot.getName();
    }

    /** The cured item for a raw pelt, or null if it is not one of ours. */
    static Item curedOf(Item raw) {
        for (Map.Entry<Supplier<Item>, Supplier<Item>> e : ColdhuntersFauna.CURES.entrySet()) if (e.getKey().get() == raw) return e.getValue().get();
        return null;
    }

    private static List<IntegerProperty> spotsOf(BlockState state) {
        return state.getValue(DryingRackBlock.PART) == DryingRackBlock.Part.SINGLE ? List.of(DryingRackBlock.HIDE) : List.of(DryingRackBlock.HIDE, DryingRackBlock.HIDE2);
    }

    /** This rack block and, for a whole tall rack, its other half. */
    private static List<BlockPos> rack(Level level, BlockPos pos, BlockState state) {
        BlockPos other = switch (state.getValue(DryingRackBlock.PART)) {
            case SINGLE -> null;
            case LOWER -> pos.above();
            case UPPER -> pos.below();
        };
        return other != null && level.getBlockState(other).is(Mechanical.DRYING_RACK.get()) ? List.of(pos, other) : List.of(pos);
    }

    /** Forgets records whose spot was emptied (taken, or the rack replaced). */
    private void purge(Level level, List<BlockPos> blocks) {
        for (BlockPos at : blocks) {
            BlockState s = level.getBlockState(at);
            for (IntegerProperty p : List.of(DryingRackBlock.HIDE, DryingRackBlock.HIDE2)) {
                if (!s.is(Mechanical.DRYING_RACK.get()) || s.getValue(p) != WET && s.getValue(p) != DRY) {
                    if (spots.remove(key(at, p)) != null) setDirty();
                }
            }
        }
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Mechanical.DRYING_RACK.get())) return;
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        List<BlockPos> blocks = rack(level, pos, state);
        PeltCuring data = level instanceof ServerLevel server ? of(server) : null;
        if (data != null) data.purge(level, blocks);

        if (curedOf(held.getItem()) != null) {
            // hang it on the first free spot: this block first, then the other half
            for (BlockPos at : blocks) {
                BlockState s = level.getBlockState(at);
                if (s.getValue(DryingRackBlock.HIDE) >= DryingRackBlock.BEAR_WET) continue; // a big pelt fills this block
                for (IntegerProperty p : spotsOf(s)) {
                    if (s.getValue(p) != DryingRackBlock.EMPTY) continue;
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    if (data != null) {
                        data.spots.put(key(at, p), BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
                        data.setDirty();
                        held.consume(1, player);
                        level.setBlock(at, s.setValue(p, WET), Block.UPDATE_ALL);
                        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
                    }
                    return;
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.drying_rack.pelt_no_room"));
            return;
        }
        if (!held.isEmpty() || data == null) return;
        // empty hand: our dry pelts come off as fur; the rack's own use then takes whatever else is done
        boolean took = false;
        for (BlockPos at : blocks) {
            BlockState s = level.getBlockState(at), next = s;
            for (IntegerProperty p : spotsOf(s)) {
                String raw = data.spots.get(key(at, p));
                if (raw == null || s.getValue(p) != DRY) continue;
                Item cured = curedOf(BuiltInRegistries.ITEM.getValue(Identifier.parse(raw)));
                data.spots.remove(key(at, p));
                data.setDirty();
                next = next.setValue(p, DryingRackBlock.EMPTY);
                if (cured != null) player.getInventory().placeItemBackInInventory(new ItemStack(cured), Prediction.SERVER_ONLY);
                took = true;
            }
            if (next != s) level.setBlock(at, next, Block.UPDATE_ALL);
        }
        if (took) level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** A broken rack gives our pelts back (raw if still wet, cured if dry) in place of the hide/leather its loot table drops. */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!event.getState().is(Mechanical.DRYING_RACK.get())) return;
        PeltCuring data = of(event.getLevel());
        List<ItemEntity> drops = event.getDrops();
        for (IntegerProperty p : List.of(DryingRackBlock.HIDE, DryingRackBlock.HIDE2)) {
            String raw = data.spots.remove(key(event.getPos(), p));
            if (raw == null) continue;
            data.setDirty();
            int value = event.getState().getValue(p);
            Item rawItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(raw));
            Item instead = value == DRY ? curedOf(rawItem) : value == WET ? rawItem : null;
            if (instead == null) continue;
            Item replaced = value == DRY ? Items.LEATHER : Mechanical.SCRAPED_HIDE.get();
            for (ItemEntity e : new ArrayList<>(drops)) {
                if (!e.getItem().is(replaced)) continue;
                e.getItem().shrink(1);
                if (e.getItem().isEmpty()) drops.remove(e);
                break;
            }
            ItemEntity pelt = new ItemEntity(event.getLevel(), event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, new ItemStack(instead));
            pelt.setDefaultPickUpDelay();
            drops.add(pelt);
        }
    }
}
