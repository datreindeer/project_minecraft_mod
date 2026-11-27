package com.mechanical.viewer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.mechanical.ArmorStationRecipes;
import com.mechanical.Casting;
import com.mechanical.DryingRackBlockEntity;
import com.mechanical.BronzeOres;
import com.mechanical.FletchingRecipes;
import com.mechanical.Kitchen;
import com.mechanical.Ironworking;
import com.mechanical.LeatherworkingRecipes;
import com.mechanical.Mechanical;
import com.mechanical.MoldBench;
import com.mechanical.Molten;
import com.mechanical.PatternBenchBlock;
import com.mechanical.Smithing;
import com.mechanical.Preserving;
import com.mechanical.StationRecipe;
import com.mechanical.ToolStationRecipes;
import com.mechanical.equipment.Equipment;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The items a player has ever carried, and what those are made of (item ids): what the recipe viewer treats as discovered. Saved, synced, kept on death. */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class SeenItems {
    public static final Supplier<AttachmentType<List<String>>> SEEN = Mechanical.ATTACHMENTS.register("seen_items", () -> AttachmentType
            .<List<String>>builder(() -> List.of())
            .serialize(Codec.STRING.listOf().fieldOf("items"))
            .sync((holder, to) -> holder == to, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
            .copyOnDeath()
            .build());
    /** Set once old players have been given what they unlocked before the viewer existed. */
    public static final Supplier<AttachmentType<Boolean>> BACKFILLED = Mechanical.ATTACHMENTS.register("seen_items_backfilled", () -> AttachmentType
            .<Boolean>builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("done"))
            .copyOnDeath()
            .build());

    private SeenItems() {}

    /** Loads the class so the attachments register. */
    public static void init() {}

    public static Set<String> seen(Player p) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(p.getData(SEEN.get())));
    }

    /** Adds everything carried or worn right now. */
    public static void scan(ServerPlayer p) {
        Set<String> found = new LinkedHashSet<>();
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) add(found, s);
        for (EquipmentSlot slot : EquipmentSlot.values()) add(found, p.getItemBySlot(slot));
        for (ItemStack s : Equipment.of(p).getItems()) add(found, s);
        merge(p, found);
    }

    /** Once per player: scan, the Ender Chest, and the key part (never the result) of each tool they had unlocked. */
    public static void backfill(ServerPlayer p) {
        if (p.getData(BACKFILLED.get())) return;
        p.setData(BACKFILLED.get(), true);
        scan(p);
        Set<String> found = new LinkedHashSet<>();
        for (ItemStack s : p.getEnderChestInventory().getItems()) add(found, s);
        List<String> unlocked = p.getData(Mechanical.UNLOCKED_TOOLS);
        for (ToolStationRecipes.Recipe r : ToolStationRecipes.ALL) {
            if (unlocked.contains(r.id())) found.add(id(r.head().get()));
        }
        for (List<StationRecipe> list : List.of(FletchingRecipes.ALL, ArmorStationRecipes.ALL, LeatherworkingRecipes.ALL, MoldBench.ALL, PatternBenchBlock.RECIPES)) {
            for (StationRecipe r : list) {
                if (unlocked.contains(r.id())) found.add(id(r.parts().get(0).item().get()));
            }
        }
        merge(p, found);
    }

    private static void add(Set<String> into, ItemStack s) {
        if (!s.isEmpty()) into.add(id(s.getItem()));
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static void merge(ServerPlayer p, Set<String> found) {
        List<String> now = p.getData(SEEN.get());
        if (now.containsAll(found)) return;
        Set<String> fresh = new LinkedHashSet<>(found);
        now.forEach(fresh::remove);
        Set<String> all = new LinkedHashSet<>(now);
        all.addAll(fresh);
        addParts(p, all, fresh);
        p.setData(SEEN.get(), new ArrayList<>(all));
    }

    /** Knowing an item means knowing what it is made of: adds the parts of {@code from}, and their parts, to {@code all}. */
    private static void addParts(ServerPlayer p, Set<String> all, Collection<String> from) {
        Map<String, List<List<String>>> parts = parts(p.level().getServer());
        Deque<String> todo = new ArrayDeque<>(from);
        while (!todo.isEmpty()) {
            for (List<String> slot : parts.getOrDefault(todo.pop(), List.of())) {
                // only the default option, and none if one is known: the other alternatives would spoil later ages
                if (slot.stream().noneMatch(all::contains)) {
                    all.add(slot.get(0));
                    todo.add(slot.get(0));
                }
            }
        }
    }

    private static Map<String, List<List<String>>> partsCache;
    private static List<ViewerRecipe> partsKey;

    /**
     * Item id -> the slots (each a list of options) it is made from. One way per item, the first one added here: following
     * every way (cordage from any hide, a stick from bamboo) would show far more than the item is made of.
     */
    static Map<String, List<List<String>>> parts(MinecraftServer server) {
        List<ViewerRecipe> stations = StationSources.all(); // a new list after every reload, so it keys the cache
        if (stations == partsKey) return partsCache;
        Map<String, List<List<String>>> out = new HashMap<>();
        // work done in the world: grinding, casting, forging, crushing, washing, drying, weaving
        for (Item from : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(from);
            part(out, Casting.ground(stack), from);
        }
        for (Molten m : Molten.values()) {
            ItemStack ingot = m.ingot();
            if (ingot.isEmpty()) continue;
            for (Item cast : m.castItems()) {
                if (cast != ingot.getItem()) part(out, new ItemStack(cast), ingot.getItem());
            }
        }
        for (Smithing.Pattern pattern : Smithing.Pattern.values()) {
            part(out, pattern.result(new ItemStack(Smithing.HOT_WROUGHT_IRON_INGOT.get())), Ironworking.WROUGHT_IRON_INGOT.get());
            part(out, pattern.result(new ItemStack(Smithing.HOT_STEEL_INGOT.get())), Smithing.STEEL_INGOT.get());
        }
        for (var d : DryingRackBlockEntity.TABLE) part(out, new ItemStack(d.output().get()), d.input().get());
        for (var d : Preserving.DRYING) {
            for (var raw : BuiltInRegistries.ITEM.getTagOrEmpty(d.tag())) {
                part(out, new ItemStack(d.product().get()), raw.value());
                break;
            }
        }
        for (var dish : Kitchen.DISHES) {
            List<List<String>> slots = dish.ings().stream().map(SeenItems::options).filter(o -> !o.isEmpty()).toList();
            out.putIfAbsent(id(dish.result().get()), slots);
        }
        // crushing last among these: several rocks crush into the same grit
        for (Item from : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(from);
            part(out, BronzeOres.crushed(stack), from);
            Item washed = BronzeOres.washed(stack);
            if (washed != null) part(out, new ItemStack(washed), from);
        }
        for (Item made : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(made);
            String path = key.getPath();
            // cut gems come from the rough stone, cured hides and furs from the raw one
            String gem = path.replaceFirst("^(flawed|flawless)_", "");
            named(out, made, key.getNamespace() + ":rough_" + gem);
            if (path.matches(".*_(hide|pelt|leather)")) named(out, made, key.getNamespace() + ":raw_" + path);
            if (path.endsWith("_fur")) named(out, made, key.getNamespace() + ":raw_" + path.replace("_fur", "_pelt"));
        }
        // the rest of the work done in the world has no table of its own
        // shortcut: written by hand, so a new in-world step needs a line here until it is a recipe the viewer reads
        for (String[] step : IN_WORLD) {
            Item made = BuiltInRegistries.ITEM.getValue(Identifier.parse(step[0]));
            named(out, made, step[1]);
        }
        for (ViewerRecipe r : stations) recipe(out, r);
        // datapack recipes: the one named after the item is its main one
        List<RecipeHolder<?>> json = new ArrayList<>(server.getRecipeManager().getRecipes());
        json.sort(Comparator.comparing((RecipeHolder<?> h) -> h.id().identifier().toString()));
        for (boolean main : new boolean[] {true, false}) {
            for (RecipeHolder<?> h : json) {
                ViewerRecipe r = JsonSources.convert(h).orElse(null);
                if (r != null && h.id().identifier().getPath().equals(BuiltInRegistries.ITEM.getKey(r.output().getItem()).getPath()) == main) recipe(out, r);
            }
        }
        partsCache = out;
        partsKey = stations;
        return out;
    }

    /** {made, from}. */
    static final String[][] IN_WORLD = {
            {"mechanical:wool_cloth", "mechanical:wool_thread"}, {"mechanical:linen_cloth", "mechanical:linen_thread"},
            {"mechanical:silk_cloth", "mechanical:silk_thread"}, {"mechanical:phantom_leather", "mechanical:scraped_phantom_hide"},
            {"mechanical:gold_ring", "mechanical:gold_ring_blank"}, {"mechanical:gold_ring_blank", "minecraft:gold_ingot"},
            {"mechanical:gold_necklace", "mechanical:gold_chain"}, {"mechanical:gold_chain", "minecraft:gold_ingot"},
            {"mechanical:silver_ring", "mechanical:silver_ring_blank"}, {"mechanical:silver_ring_blank", "mechanical:silver_ingot"},
            {"mechanical:silver_necklace", "mechanical:silver_chain"}, {"mechanical:silver_chain", "mechanical:silver_ingot"},
            {"mechanical:electrum_ring", "mechanical:electrum_ring_blank"}, {"mechanical:electrum_ring_blank", "mechanical:electrum_ingot"},
            {"mechanical:electrum_necklace", "mechanical:electrum_chain"}, {"mechanical:electrum_chain", "mechanical:electrum_ingot"},
            {"mechanical:unfired_decorated_pot", "minecraft:clay_ball"}, {"mechanical:unfired_flower_pot", "minecraft:clay_ball"},
            {"mechanical:unfired_storage_jar", "minecraft:clay_ball"}, {"mechanical:unfired_water_jug", "minecraft:clay_ball"},
            {"mechanical:coke", "minecraft:coal"}, {"mechanical:dugout_canoe", "mechanical:dugout_blank"},
            {"mechanical:bronze_fire_grate", "mechanical:bronze_ingot"}, {"mechanical:bronze_water_bucket", "mechanical:bronze_bucket"},
            {"mechanical:bronze_milk_bucket", "mechanical:bronze_bucket"}, {"mechanical:clay_water_bucket", "mechanical:clay_bucket"},
            {"mechanical:smoked_haunch", "mechanical:raw_haunch"}, {"mechanical:smoked_ribs", "mechanical:raw_ribs"},
            {"mechanical:smoked_loin", "mechanical:raw_loin"},
            {"mechanical:flour", "mechanical:einkorn_grain"}, {"mechanical:salt", "mechanical:rock_salt"},
            {"mechanical:jerky", "mechanical:raw_meat_strips"}, {"mechanical:smoked_fish", "minecraft:cod"},
            {"mechanical:dried_fish", "minecraft:salmon"}, {"mechanical:dried_berries", "minecraft:sweet_berries"},
            {"mechanical:dried_mushrooms", "mechanical:porcini"}, {"mechanical:copper_coin", "minecraft:copper_ingot"},
            {"mechanical:silver_coin", "mechanical:silver_ingot"},
            {"mechanical:iron_ingot_mold", "mechanical:pig_iron_ingot"},
            {"mechanical:iron_nugget_mold", "mechanical:pig_iron_ingot"},
            {"mechanical:iron_plate_mold", "mechanical:pig_iron_ingot"},
            {"mechanical:cast_iron_gear", "mechanical:pig_iron_ingot"},
            {"mechanical:cast_iron_pot", "mechanical:pig_iron_ingot"},
    };

    private static void named(Map<String, List<List<String>>> into, Item made, String from) {
        BuiltInRegistries.ITEM.getOptional(Identifier.parse(from)).ifPresent(part -> part(into, new ItemStack(made), part));
    }

    private static void recipe(Map<String, List<List<String>>> into, ViewerRecipe r) {
        // smelting worn gear back into nuggets is not what a nugget is made of
        if (r.station() == Station.SMELTING && r.inputs().stream().anyMatch(s -> s.options().stream().anyMatch(i -> i.getDefaultInstance().isDamageableItem()))) return;
        into.putIfAbsent(id(r.output().getItem()),
                r.inputs().stream().filter(s -> !s.options().isEmpty()).map(s -> s.options().stream().map(SeenItems::id).toList()).toList());
    }

    private static List<String> options(Kitchen.Ing ing) {
        List<String> out = new ArrayList<>();
        for (String name : ing.ids()) {
            if (!name.startsWith("#")) out.add(name);
            else BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(name.substring(1))))
                    .forEach(h -> out.add(id(h.value())));
        }
        return out;
    }

    private static void part(Map<String, List<List<String>>> into, ItemStack made, Item from) {
        if (!made.isEmpty() && !made.is(from)) into.putIfAbsent(id(made.getItem()), List.of(List.of(id(from))));
    }

    /** Players who saw items before parts were followed get their parts now. */
    private static void catchUp(ServerPlayer p) {
        Set<String> all = new LinkedHashSet<>(p.getData(SEEN.get()));
        int before = all.size();
        addParts(p, all, List.copyOf(all));
        if (all.size() != before) p.setData(SEEN.get(), new ArrayList<>(all));
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 5) scan(p);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            backfill(p);
            catchUp(p);
        }
    }
}
