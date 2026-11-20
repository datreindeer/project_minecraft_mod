package com.mechanical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Which dried meat or fish each Drying Rack spot is making ({@link Preserving#drying}): the blockstate only knows the look
 * (red meat, fish, fowl), so the product is remembered here per world, keyed by spot. Hanging overwrites the record,
 * taking or breaking removes it; a spot without one (racks from before) gives plain Jerky / Dried Fish.
 * Also counts the kinds a player has taken off a rack, for the "three kinds" advancement.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class RackFood extends SavedData {
    private static final Codec<RackFood> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(m -> new RackFood(new HashMap<>(m)), d -> d.spots);
    private static final SavedDataType<RackFood> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "rack_food"),
            () -> new RackFood(new HashMap<>()), CODEC);
    public static final Identifier THREE_KINDS = Identifier.fromNamespaceAndPath(Mechanical.MODID, "food/three_kinds_dried");
    private static final String KINDS = "mechanical_dried_kinds";

    /** "x,y,z,hide|hide2" -> id of the product drying there. */
    private final Map<String, String> spots;

    private RackFood(Map<String, String> spots) {
        this.spots = spots;
    }

    private static RackFood of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static String key(BlockPos pos, IntegerProperty spot) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + spot.getName();
    }

    static void put(ServerLevel level, BlockPos pos, IntegerProperty spot, Item product) {
        RackFood data = of(level);
        data.spots.put(key(pos, spot), BuiltInRegistries.ITEM.getKey(product).toString());
        data.setDirty();
    }

    /** The product recorded for this spot (forgotten now), or {@code fallback}. */
    static ItemStack take(ServerLevel level, BlockPos pos, IntegerProperty spot, ItemStack fallback) {
        RackFood data = of(level);
        String id = data.spots.remove(key(pos, spot));
        if (id == null) return fallback;
        data.setDirty();
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).map(ItemStack::new).orElse(fallback);
    }

    /** Remembers a dried meat or fish the player took off a rack; the third different kind gives the advancement. */
    static void taken(Player player, ItemStack product) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag kept = sp.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        Set<String> kinds = new LinkedHashSet<>(Arrays.asList(kept.getStringOr(KINDS, "").split(",")));
        kinds.remove("");
        if (!kinds.add(BuiltInRegistries.ITEM.getKey(product.getItem()).getPath())) return;
        kept.putString(KINDS, String.join(",", kinds));
        sp.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        if (kinds.size() >= 3) Kettle.award(sp, THREE_KINDS);
    }

    /** A broken rack drops what each meat or fish spot was really making, in place of the plain Jerky / Dried Fish of its loot table. */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!event.getState().is(Mechanical.DRYING_RACK.get())) return;
        List<ItemEntity> drops = event.getDrops();
        for (IntegerProperty p : List.of(DryingRackBlock.HIDE, DryingRackBlock.HIDE2)) {
            int value = DryingRackBlock.spot(event.getState(), p);
            Item plain = value == DryingRackBlock.MEAT + 1 ? Preserving.JERKY.get() : value == DryingRackBlock.FISH + 1 ? Preserving.DRIED_FISH.get() : null;
            ItemStack real = take(event.getLevel(), event.getPos(), p, ItemStack.EMPTY);
            if (plain == null || real.isEmpty() || real.is(plain)) continue;
            for (ItemEntity e : new ArrayList<>(drops)) {
                if (!e.getItem().is(plain)) continue;
                e.getItem().shrink(1);
                if (e.getItem().isEmpty()) drops.remove(e);
                break;
            }
            ItemEntity out = new ItemEntity(event.getLevel(), event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, real);
            out.setDefaultPickUpDelay();
            drops.add(out);
        }
    }
}
