package com.mechanical.wildlife.giants;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.function.Supplier;

import com.mechanical.BearRugBlock;
import com.mechanical.Butchery;
import com.mechanical.Butchery.Extra;
import com.mechanical.KnifeItem;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Ice Age giants: Woolly Mammoth (rare herd mini-boss), Woolly Rhinoceros, Steppe Bison and Musk Ox. Entity types,
 * what they give (meat, hides, ivory, horn, wool, fat ...), what is made from it, attributes, spawn rules and carcass
 * yields. Spawns are in data (scripts/gen_fauna_giants.py writes the biome modifiers).
 * <p>
 * Their raw hides are too big for the Drying Rack: the raw hide item places a {@link PeggedHideBlock} on the ground,
 * which cures in the sun into the hide.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class GiantsFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<WoollyMammoth>> WOOLLY_MAMMOTH = ENTITY_TYPES.registerEntityType(
            "woolly_mammoth", WoollyMammoth::new, MobCategory.CREATURE, b -> b.sized(2.4F, 3.4F).eyeHeight(2.9F).clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<WoollyRhino>> WOOLLY_RHINO = ENTITY_TYPES.registerEntityType(
            "woolly_rhino", WoollyRhino::new, MobCategory.CREATURE, b -> b.sized(1.5F, 1.6F).eyeHeight(1.0F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<SteppeBison>> STEPPE_BISON = ENTITY_TYPES.registerEntityType(
            "steppe_bison", SteppeBison::new, MobCategory.CREATURE, b -> b.sized(1.4F, 1.8F).eyeHeight(1.2F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MuskOx>> MUSK_OX = ENTITY_TYPES.registerEntityType(
            "musk_ox", MuskOx::new, MobCategory.CREATURE, b -> b.sized(1.2F, 1.4F).eyeHeight(1.0F).clientTrackingRange(10));

    // ---- meat (campfire / smoker; perishes like the other raw meat, smokable and dryable) ----
    public static final DeferredItem<Item> RAW_MAMMOTH = food("raw_mammoth", 2, 0.15F);
    public static final DeferredItem<Item> COOKED_MAMMOTH = food("cooked_mammoth", 4, 0.8F);
    public static final DeferredItem<Item> RAW_RHINO = food("raw_rhino", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_RHINO = food("cooked_rhino", 3, 0.7F);
    public static final DeferredItem<Item> RAW_BISON = food("raw_bison", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_BISON = food("cooked_bison", 3, 0.9F);
    /** The hunters' prize cut. */
    public static final DeferredItem<Item> BISON_TONGUE = food("bison_tongue", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_BISON_TONGUE = food("cooked_bison_tongue", 4, 1.1F);
    public static final DeferredItem<Item> RAW_MUSK_OX = food("raw_musk_ox", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_MUSK_OX = food("cooked_musk_ox", 3, 0.8F);

    // ---- cured hides (the raw ones are the pegged-hide block items below) ----
    public static final DeferredItem<Item> MAMMOTH_HIDE = ITEMS.registerSimpleItem("mammoth_hide");
    public static final DeferredItem<Item> RHINO_HIDE = ITEMS.registerSimpleItem("rhino_hide");
    public static final DeferredItem<Item> BISON_HIDE = ITEMS.registerSimpleItem("bison_hide");
    public static final DeferredItem<Item> MUSK_OX_HIDE = ITEMS.registerSimpleItem("musk_ox_hide");

    // ---- special parts ----
    /** Always two off a butchered mammoth. */
    public static final DeferredItem<Item> MAMMOTH_IVORY = ITEMS.registerSimpleItem("mammoth_ivory", p -> p.rarity(Rarity.UNCOMMON).stacksTo(16));
    public static final DeferredItem<Item> MAMMOTH_WOOL = ITEMS.registerSimpleItem("mammoth_wool");
    public static final DeferredItem<Item> MAMMOTH_FAT = ITEMS.registerSimpleItem("mammoth_fat");
    public static final DeferredItem<Item> RHINO_HORN = ITEMS.registerSimpleItem("rhino_horn", p -> p.rarity(Rarity.UNCOMMON).stacksTo(16));
    /** Musk ox underwool, finer and warmer than sheep's wool. */
    public static final DeferredItem<Item> QIVIUT = ITEMS.registerSimpleItem("qiviut");

    // ---- pegged raw hides: block and item share the id, so Mechanical.registerMissingBlockItems adds no second item ----
    public static final DeferredBlock<PeggedHideBlock> RAW_MAMMOTH_HIDE_BLOCK = pegged("raw_mammoth_hide", MapColor.COLOR_BROWN, 1.0F / 12.0F, () -> MAMMOTH_HIDE.get());
    public static final DeferredBlock<PeggedHideBlock> RAW_RHINO_HIDE_BLOCK = pegged("raw_rhino_hide", MapColor.TERRACOTTA_BROWN, 1.0F / 8.0F, () -> RHINO_HIDE.get());
    public static final DeferredBlock<PeggedHideBlock> RAW_BISON_HIDE_BLOCK = pegged("raw_bison_hide", MapColor.COLOR_BROWN, 1.0F / 7.0F, () -> BISON_HIDE.get());
    public static final DeferredBlock<PeggedHideBlock> RAW_MUSK_OX_HIDE_BLOCK = pegged("raw_musk_ox_hide", MapColor.TERRACOTTA_BLACK, 1.0F / 6.0F, () -> MUSK_OX_HIDE.get());
    public static final DeferredItem<BlockItem> RAW_MAMMOTH_HIDE = ITEMS.registerSimpleBlockItem("raw_mammoth_hide", RAW_MAMMOTH_HIDE_BLOCK);
    public static final DeferredItem<BlockItem> RAW_RHINO_HIDE = ITEMS.registerSimpleBlockItem("raw_rhino_hide", RAW_RHINO_HIDE_BLOCK);
    public static final DeferredItem<BlockItem> RAW_BISON_HIDE = ITEMS.registerSimpleBlockItem("raw_bison_hide", RAW_BISON_HIDE_BLOCK);
    public static final DeferredItem<BlockItem> RAW_MUSK_OX_HIDE = ITEMS.registerSimpleBlockItem("raw_musk_ox_hide", RAW_MUSK_OX_HIDE_BLOCK);

    // ---- made from them ----
    public static final TagKey<Item> IVORY_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "ivory_tool_materials"));
    public static final TagKey<Item> RHINO_HORN_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "rhino_horn_tool_materials"));
    public static final ToolMaterial IVORY = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 200, 3.0F, 0.5F, 10, IVORY_TOOL_MATERIALS);
    public static final ToolMaterial HORN_CLUB = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 240, 2.0F, 1.0F, 5, RHINO_HORN_TOOL_MATERIALS);
    /** A skinning knife as good as the Tusk Dagger (cuts bears), a little faster and with a bigger yield. */
    public static final DeferredItem<KnifeItem> IVORY_KNIFE = ITEMS.registerItem("ivory_knife", p -> new KnifeItem(p, 1.4F, 1.2F, KnifeItem.TUSK, 1.6F),
            p -> p.sword(IVORY, 1.5F, -1.8F));
    /** A heavy club with the rhino's horn lashed on as a spike: slow, hard-hitting. */
    public static final DeferredItem<Item> RHINO_HORN_CLUB = ITEMS.registerSimpleItem("rhino_horn_club", p -> p.sword(HORN_CLUB, 4.5F, -3.1F));
    public static final DeferredBlock<IvoryFigurineBlock> IVORY_FIGURINE = BLOCKS.registerBlock("ivory_figurine", IvoryFigurineBlock::new, p -> p
            .mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.BONE_BLOCK).noOcclusion());
    public static final DeferredItem<BlockItem> IVORY_FIGURINE_ITEM = ITEMS.registerSimpleBlockItem("ivory_figurine", IVORY_FIGURINE);
    public static final DeferredBlock<BearRugBlock> MAMMOTH_RUG = rug("mammoth_rug", MapColor.COLOR_BROWN);
    public static final DeferredBlock<BearRugBlock> BISON_RUG = rug("bison_rug", MapColor.COLOR_BROWN);
    public static final DeferredBlock<BearRugBlock> MUSK_OX_RUG = rug("musk_ox_rug", MapColor.TERRACOTTA_BLACK);
    public static final DeferredItem<BlockItem> MAMMOTH_RUG_ITEM = ITEMS.registerSimpleBlockItem("mammoth_rug", MAMMOTH_RUG);
    public static final DeferredItem<BlockItem> BISON_RUG_ITEM = ITEMS.registerSimpleBlockItem("bison_rug", BISON_RUG);
    public static final DeferredItem<BlockItem> MUSK_OX_RUG_ITEM = ITEMS.registerSimpleBlockItem("musk_ox_rug", MUSK_OX_RUG);

    public static final DeferredItem<SpawnEggItem> WOOLLY_MAMMOTH_SPAWN_EGG = egg("woolly_mammoth_spawn_egg", WOOLLY_MAMMOTH);
    public static final DeferredItem<SpawnEggItem> WOOLLY_RHINO_SPAWN_EGG = egg("woolly_rhino_spawn_egg", WOOLLY_RHINO);
    public static final DeferredItem<SpawnEggItem> STEPPE_BISON_SPAWN_EGG = egg("steppe_bison_spawn_egg", STEPPE_BISON);
    public static final DeferredItem<SpawnEggItem> MUSK_OX_SPAWN_EGG = egg("musk_ox_spawn_egg", MUSK_OX);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static DeferredBlock<PeggedHideBlock> pegged(String name, MapColor colour, float cureChance, Supplier<Item> cured) {
        return BLOCKS.registerBlock(name, p -> new PeggedHideBlock(p, cureChance, cured), p -> p
                .mapColor(colour).strength(0.3F).sound(SoundType.WOOL).noOcclusion().randomTicks());
    }

    private static DeferredBlock<BearRugBlock> rug(String name, MapColor colour) {
        return BLOCKS.registerBlock(name, BearRugBlock::new, p -> p.mapColor(colour).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    }

    private static Extra x(Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Loads this class so its registrations happen; call from the mod constructor. */
    public static void init() {
        // counts and chances are for a flint knife (Butchery); outer = first pass (hide, wool), inner = with the meat
        Butchery.register(() -> WOOLLY_MAMMOTH.get(), new Butchery.Yield(RAW_MAMMOTH, 8, 12,
                List.of(x(() -> RAW_MAMMOTH_HIDE.get(), 1.0F, 1, 1), x(MAMMOTH_WOOL, 0.9F, 3, 5)),
                List.of(x(MAMMOTH_IVORY, 1.0F, 2, 2), x(MAMMOTH_FAT, 0.9F, 2, 4), x(Wildlife.SINEW, 0.8F, 2, 3), x(() -> Items.BONE, 0.9F, 3, 6))));
        Butchery.register(() -> WOOLLY_RHINO.get(), new Butchery.Yield(RAW_RHINO, 4, 6,
                List.of(x(() -> RAW_RHINO_HIDE.get(), 1.0F, 1, 1)),
                List.of(x(RHINO_HORN, 1.0F, 1, 1), x(Wildlife.SINEW, 0.6F, 1, 2), x(() -> Items.BONE, 0.6F, 1, 3))));
        Butchery.register(() -> STEPPE_BISON.get(), new Butchery.Yield(RAW_BISON, 3, 6,
                List.of(x(() -> RAW_BISON_HIDE.get(), 1.0F, 1, 1)),
                List.of(x(BISON_TONGUE, 0.8F, 1, 1), x(Wildlife.HORN, 0.5F, 1, 2), x(Wildlife.SINEW, 0.6F, 1, 2))));
        Butchery.register(() -> MUSK_OX.get(), new Butchery.Yield(RAW_MUSK_OX, 2, 4,
                List.of(x(QIVIUT, 0.9F, 2, 4), x(() -> RAW_MUSK_OX_HIDE.get(), 1.0F, 1, 1)),
                List.of(x(Wildlife.HORN, 0.6F, 1, 2), x(Wildlife.SINEW, 0.5F, 1, 2))));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(WOOLLY_MAMMOTH.get(), WoollyMammoth.createAttributes().build());
        event.put(WOOLLY_RHINO.get(), WoollyRhino.createAttributes().build());
        event.put(STEPPE_BISON.get(), SteppeBison.createAttributes().build());
        event.put(MUSK_OX.get(), MuskOx.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        event.register(WOOLLY_MAMMOTH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STEPPE_BISON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // rhino and musk ox also stand on snow, stone and gravel (windswept hills, snowy slopes): the goat rule
        event.register(WOOLLY_RHINO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.goat.Goat::checkGoatSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MUSK_OX.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.goat.Goat::checkGoatSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private GiantsFauna() {}
}
