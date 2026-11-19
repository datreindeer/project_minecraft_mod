package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The edible birds (Greylag Goose, Rock Dove, Willow Ptarmigan, Pheasant, Chicken): each leaves a Carcass that is plucked
 * with a knife (feathers, see {@link Butchery}) and then either butchered into its own meat or picked up whole as a
 * Plucked bird (empty hand on the plucked carcass, {@link CarcassEntity#interact}). A Plucked bird cuts into its meat in
 * the crafting grid with a knife, or cooks whole on the campfire; it is also what the Bronze-age kettle will boil.
 * Nests and eggs: {@link BirdNestBlock}. Data: scripts/gen_birds2.py, textures: scripts/gen_birds2_textures.py.
 */
public final class Birds {
    public static final DeferredItem<Item> RAW_GOOSE = raw("raw_goose", 3);
    public static final DeferredItem<Item> COOKED_GOOSE = food("cooked_goose", 7, 0.8F);
    public static final DeferredItem<Item> RAW_PHEASANT = raw("raw_pheasant", 2);
    public static final DeferredItem<Item> COOKED_PHEASANT = food("cooked_pheasant", 6, 0.7F);
    public static final DeferredItem<Item> RAW_PTARMIGAN = raw("raw_ptarmigan", 2);
    public static final DeferredItem<Item> COOKED_PTARMIGAN = food("cooked_ptarmigan", 5, 0.7F);
    public static final DeferredItem<Item> RAW_PIGEON = raw("raw_pigeon", 1);
    public static final DeferredItem<Item> COOKED_PIGEON = food("cooked_pigeon", 4, 0.6F);

    public static final DeferredItem<Item> PLUCKED_GOOSE = ITEMS.registerSimpleItem("plucked_goose", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_PHEASANT = ITEMS.registerSimpleItem("plucked_pheasant", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_PTARMIGAN = ITEMS.registerSimpleItem("plucked_ptarmigan", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_PIGEON = ITEMS.registerSimpleItem("plucked_pigeon", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_CHICKEN = ITEMS.registerSimpleItem("plucked_chicken", p -> p.stacksTo(16));

    public static final DeferredItem<Item> GOOSE_EGG = ITEMS.registerSimpleItem("goose_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PHEASANT_EGG = ITEMS.registerSimpleItem("pheasant_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PTARMIGAN_EGG = ITEMS.registerSimpleItem("ptarmigan_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PIGEON_EGG = ITEMS.registerSimpleItem("pigeon_egg", p -> p.stacksTo(16));

    public static final DeferredBlock<BirdNestBlock> BIRD_NEST = BLOCKS.registerBlock("bird_nest", BirdNestBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(0.3F).sound(SoundType.GRASS).noOcclusion().randomTicks().ignitedByLava());
    public static final DeferredItem<BlockItem> BIRD_NEST_ITEM = ITEMS.registerSimpleBlockItem("bird_nest", BIRD_NEST);

    /** Bird -> the whole plucked bird you can pick up from its plucked carcass. */
    private static Map<EntityType<?>, Supplier<Item>> plucked;

    private static final List<Map.Entry<Supplier<? extends EntityType<?>>, Supplier<Item>>> REGISTERED = new ArrayList<>();

    private static Map<EntityType<?>, Supplier<Item>> plucked() {
        if (plucked == null) {
            Map<EntityType<?>, Supplier<Item>> all = new HashMap<>(Map.of(Wildlife.GOOSE.get(), PLUCKED_GOOSE, Wildlife.PHEASANT.get(), PLUCKED_PHEASANT,
                    Wildlife.PTARMIGAN.get(), PLUCKED_PTARMIGAN, Wildlife.ROCK_DOVE.get(), PLUCKED_PIGEON, EntityTypes.CHICKEN, PLUCKED_CHICKEN));
            for (var e : REGISTERED) all.put(e.getKey().get(), e.getValue());
            plucked = all;
        }
        return plucked;
    }

    /** Adds an edible bird (and its Plucked item) from its own feature class; call during mod construction. */
    public static void registerPlucked(Supplier<? extends EntityType<?>> bird, Supplier<Item> pluckedItem) {
        REGISTERED.add(Map.entry(bird, pluckedItem));
    }

    /** Same as {@link #registerPlucked}: an edible bird that leaves a carcass, is plucked, hung by the feet and taken whole. */
    public static void registerEdible(Supplier<? extends EntityType<?>> type, Supplier<Item> plucked) {
        registerPlucked(type, plucked);
    }

    /** One of the Plucked birds (the kettle boils these into broth). */
    public static boolean isPlucked(net.minecraft.world.item.ItemStack stack) {
        for (Supplier<Item> s : plucked().values()) if (stack.is(s.get())) return true;
        return false;
    }

    /** One of the edible birds that leave a carcass (light to carry, hung by the feet, can be taken whole once plucked). */
    public static boolean isBird(EntityType<?> type) {
        return plucked().containsKey(type);
    }

    /** The Plucked item of this bird, or null for other animals. */
    public static Item pluckedOf(EntityType<?> type) {
        Supplier<Item> s = plucked().get(type);
        return s == null ? null : s.get();
    }

    /** Raw bird meat: like vanilla raw chicken, a chance of Hunger when eaten raw. */
    private static DeferredItem<Item> raw(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build(), Consumables.CHICKEN));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Loads this class so its registrations happen; call from the mod constructor (before the missing block items). */
    public static void init() {}

    private Birds() {}
}
