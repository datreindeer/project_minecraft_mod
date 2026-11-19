package com.mechanical;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Mechanical.MODID)
public class Mechanical {
    public static final String MODID = "mechanical";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final Supplier<SimpleParticleType> BLOOD_DROP = PARTICLE_TYPES.register("blood_drop", () -> new SimpleParticleType(false));
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // Tool Station recipes the player has unlocked (result item ids), saved with the player and synced to their client.
    public static final Supplier<AttachmentType<List<String>>> UNLOCKED_TOOLS = ATTACHMENTS.register("unlocked_tools", () -> AttachmentType
            .<List<String>>builder(() -> List.of())
            .serialize(Codec.STRING.listOf().fieldOf("tools"))
            .sync(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
            .copyOnDeath()
            .build());

    // --- Tin ---
    // Tin ore: stone-like, needs at least a stone pickaxe (see the needs_stone_tool tag) and drops itself
    public static final DeferredBlock<Block> TIN_ORE = BLOCKS.registerSimpleBlock("tin_ore", p -> p
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F));
    public static final DeferredItem<BlockItem> TIN_ORE_ITEM = ITEMS.registerSimpleBlockItem("tin_ore", TIN_ORE);

    public static final DeferredBlock<Block> TIN_BLOCK = BLOCKS.registerSimpleBlock("tin_block", p -> p
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> TIN_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("tin_block", TIN_BLOCK);

    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");

    // --- Stone age: materials ---
    public static final DeferredItem<Item> PLANT_FIBRE = ITEMS.registerSimpleItem("plant_fibre");
    public static final DeferredItem<Item> TWINE = ITEMS.registerSimpleItem("twine");
    public static final DeferredItem<Item> CORDAGE = ITEMS.registerSimpleItem("cordage");
    public static final DeferredItem<Item> TOOL_ROD = ITEMS.registerSimpleItem("tool_rod");
    public static final DeferredItem<Item> STONE_CHUNK = ITEMS.registerSimpleItem("stone_chunk");
    public static final DeferredItem<Item> GRANITE_CHUNK = ITEMS.registerSimpleItem("granite_chunk");
    public static final DeferredItem<Item> DIORITE_CHUNK = ITEMS.registerSimpleItem("diorite_chunk");
    public static final DeferredItem<Item> ANDESITE_CHUNK = ITEMS.registerSimpleItem("andesite_chunk");

    // --- Stone age: flint tools (mine like wood, below stone) ---
    public static final TagKey<Item> FLINT_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "flint_tool_materials"));
    public static final ToolMaterial FLINT = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 48, 2.5F, 0.0F, 5, FLINT_TOOL_MATERIALS);

    public static final DeferredItem<FlintPickaxeItem> FLINT_PICKAXE = ITEMS.registerItem("flint_pickaxe", FlintPickaxeItem::new, p -> p.pickaxe(FLINT, 1.0F, -2.8F));
    public static final DeferredItem<Item> FLINT_AXE = ITEMS.registerSimpleItem("flint_axe", p -> p.axe(FLINT, 3.0F, -3.2F));
    public static final DeferredItem<Item> FLINT_SHOVEL = ITEMS.registerSimpleItem("flint_shovel", p -> p.shovel(FLINT, 1.5F, -3.0F));
    public static final DeferredItem<Item> FLINT_HOE = ITEMS.registerSimpleItem("flint_hoe", p -> p.hoe(FLINT, 0.0F, -3.0F));
    // Same handling as the vanilla stone spear, flint material; assembled in the Tool Station.
    /** The spear is a bound, hafted tool (cordage + tool rod): sturdier and harder-hitting than the plain flint tools, like vanilla's stone spear. */
    public static final ToolMaterial FLINT_SPEAR_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 100, 2.5F, 1.0F, 5, FLINT_TOOL_MATERIALS);
    public static final DeferredItem<Item> FLINT_SPEAR = ITEMS.registerSimpleItem("flint_spear", p -> p.spear(FLINT_SPEAR_MATERIAL, 0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F));
    public static final DeferredItem<KnifeItem> FLINT_KNIFE = ITEMS.registerItem("flint_knife", p -> new KnifeItem(p, 1.0F, 1.0F, KnifeItem.FLINT), p -> p.sword(FLINT, 1.5F, -2.0F));
    /** Iron age; no way to get it yet (no recipe). Needed for tough animals like the bear. */
    public static final DeferredItem<KnifeItem> IRON_KNIFE = ITEMS.registerItem("iron_knife", p -> new KnifeItem(p, 2.0F, 1.5F, KnifeItem.IRON),
            p -> p.sword(net.minecraft.world.item.ToolMaterial.IRON, 1.5F, -2.0F));

    // --- Stone age: stone tool heads (head + sticks + twine = stone tool) ---
    public static final DeferredItem<Item> STONE_PICKAXE_HEAD = ITEMS.registerSimpleItem("stone_pickaxe_head");
    // Single-sided pick: mines like stone but slower and weaker; a second head turns it into the vanilla stone pickaxe.
    public static final TagKey<Item> ROCK_CHUNKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "rock_chunks"));
    public static final ToolMaterial CRUDE_STONE = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 80, 3.0F, 0.5F, 5, ROCK_CHUNKS);
    public static final DeferredItem<Item> CRUDE_STONE_PICKAXE = ITEMS.registerSimpleItem("crude_stone_pickaxe", p -> p.pickaxe(CRUDE_STONE, 1.0F, -2.9F));
    public static final DeferredItem<Item> CRUDE_STONE_AXE = ITEMS.registerSimpleItem("crude_stone_axe", p -> p.axe(CRUDE_STONE, 4.0F, -3.3F));
    public static final DeferredItem<Item> DOUBLE_STONE_PICKAXE_HEAD = ITEMS.registerSimpleItem("double_stone_pickaxe_head");
    public static final DeferredItem<Item> STONE_AXE_HEAD = ITEMS.registerSimpleItem("stone_axe_head");
    public static final DeferredItem<Item> DOUBLE_STONE_AXE_HEAD = ITEMS.registerSimpleItem("double_stone_axe_head");

    // --- Stone age: loose rocks on the ground (placed by worldgen, no block items) ---
    public static final DeferredBlock<RockBlock> LOOSE_STONE = rock("loose_stone", MapColor.STONE);
    public static final DeferredBlock<RockBlock> LOOSE_GRANITE = rock("loose_granite", MapColor.DIRT);
    public static final DeferredBlock<RockBlock> LOOSE_DIORITE = rock("loose_diorite", MapColor.QUARTZ);
    public static final DeferredBlock<RockBlock> LOOSE_ANDESITE = rock("loose_andesite", MapColor.STONE);
    public static final DeferredBlock<RockBlock> LOOSE_FLINT = rock("loose_flint", MapColor.COLOR_GRAY);

    private static DeferredBlock<RockBlock> rock(String name, MapColor color) {
        return BLOCKS.registerBlock(name, RockBlock::new, p -> p
                .mapColor(color)
                .strength(1.5F)
                .sound(SoundType.STONE)
                .noOcclusion()
                .pushReaction(PushReaction.POPPED));
    }

    // --- Tool Station: assembles tools from head + binding + rod ---
    public static final DeferredBlock<ToolStationBlock> TOOL_STATION = BLOCKS.registerBlock("tool_station", ToolStationBlock::new, p -> p
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> TOOL_STATION_ITEM = ITEMS.registerSimpleBlockItem("tool_station", TOOL_STATION);
    public static final Supplier<MenuType<ToolStationMenu>> TOOL_STATION_MENU = MENUS.register("tool_station",
            () -> new MenuType<>(ToolStationMenu::new, FeatureFlags.VANILLA_SET));

    // --- Fire and clay: fire drill lights campfires, the kiln on top of a campfire bakes clay and makes charcoal ---
    public static final DeferredItem<FireDrillItem> FIRE_DRILL = ITEMS.registerItem("fire_drill", FireDrillItem::new, p -> p.durability(16));
    public static final DeferredItem<Item> UNFIRED_CRUCIBLE = ITEMS.registerSimpleItem("unfired_crucible");
    /** Holds ore for melting (CrucibleItem); one per stack since it carries its contents. */
    public static final DeferredItem<CrucibleItem> CRUCIBLE = ITEMS.registerItem("crucible", p -> new CrucibleItem(p, CrucibleItem.Kind.ORE), p -> p.stacksTo(1));
    public static final DeferredItem<Item> UNFIRED_CLAY_BUCKET = ITEMS.registerSimpleItem("unfired_clay_bucket");
    /** Fired clay buckets: water only, 16 pours before the clay cracks (ClayBucketItem). */
    public static final DeferredItem<ClayBucketItem> CLAY_BUCKET = ITEMS.registerItem("clay_bucket",
            p -> new ClayBucketItem(net.minecraft.world.level.material.Fluids.EMPTY, p), p -> p.durability(16));
    public static final DeferredItem<ClayBucketItem> CLAY_WATER_BUCKET = ITEMS.registerItem("clay_water_bucket",
            p -> new ClayBucketItem(net.minecraft.world.level.material.Fluids.WATER, p), p -> p.durability(16));
    public static final DeferredBlock<KilnBlock> KILN = BLOCKS.registerBlock("kiln", KilnBlock::new, p -> p
            .mapColor(MapColor.CLAY)
            .strength(2.0F)
            .sound(SoundType.MUD_BRICKS)
            .noOcclusion()
            .lightLevel(state -> state.getValue(KilnBlock.HOT) ? 15 : state.getValue(KilnBlock.LIT) ? 13 : 0));
    public static final DeferredItem<BlockItem> KILN_ITEM = ITEMS.registerSimpleBlockItem("kiln", KILN);
    public static final Supplier<BlockEntityType<KilnBlockEntity>> KILN_ENTITY = BLOCK_ENTITIES.register("kiln",
            () -> new BlockEntityType<>(KilnBlockEntity::new, KILN.get()));
    public static final Supplier<MenuType<KilnMenu>> KILN_MENU = MENUS.register("kiln",
            () -> new MenuType<>(KilnMenu::new, FeatureFlags.VANILLA_SET));

    // --- Tree felling ---
    public static final DeferredHolder<EntityType<?>, EntityType<FallingTreeEntity>> FALLING_TREE = ENTITY_TYPES.registerEntityType(
            "falling_tree", FallingTreeEntity::new, MobCategory.MISC, b -> b.sized(0.98F, 0.98F).clientTrackingRange(10).noSave());

    // Stumps left by felled trees, one per wood type (looked up by the log's name: oak_log -> oak_stump).
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar",
            "aspen", "alder", "beech", "linden");
    public static final Map<String, DeferredBlock<StumpBlock>> STUMPS = WOODS.stream().collect(Collectors.toMap(w -> w, w ->
            BLOCKS.registerBlock(w + "_stump", StumpBlock::new, p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion())));

    // Felled trees land as fallen logs, chopped (bark off, hewn thinner) into rough logs, which are split on a stump.
    public static final Map<String, DeferredBlock<FallenLogBlock>> FALLEN_LOGS = WOODS.stream().collect(Collectors.toMap(w -> w, w ->
            BLOCKS.registerBlock(w + "_fallen_log", p -> new FallenLogBlock(w, p), p -> p.mapColor(MapColor.WOOD).strength(6.0F).sound(SoundType.WOOD).noOcclusion())));
    public static final Map<String, DeferredItem<Item>> ROUGH_LOGS = WOODS.stream().collect(Collectors.toMap(w -> w, w ->
            ITEMS.registerSimpleItem(w + "_rough_log", p -> p.stacksTo(16))));
    public static final DeferredItem<Item> BARK = ITEMS.registerSimpleItem("bark");
    /** Leaves (block name minus "_leaves") that get their own fresh, leaf-coloured litter when their tree is felled. */
    public static final List<String> LITTER_LEAVES = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
            "pale_oak", "yellow_poplar", "orange_poplar", "red_poplar", "azalea", "flowering_azalea", "aspen", "alder", "beech", "linden");
    public static final Map<String, DeferredBlock<FreshLeafLitterBlock>> LEAF_LITTERS = LITTER_LEAVES.stream().collect(Collectors.toMap(l -> l, l ->
            BLOCKS.registerBlock(l + "_leaf_litter", FreshLeafLitterBlock::new, p -> p.mapColor(MapColor.PLANT).replaceable().noCollision()
                    .randomTicks().sound(SoundType.LEAF_LITTER).pushReaction(PushReaction.POPPED))));
    // --- Hunting gear ---
    /** Tusk Dagger (Tool Station: tusk + cordage + leather grip): a hunter's skinning knife; cuts bears and gets more hide/pelt. */
    public static final ToolMaterial TUSK = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 140, 3.0F, 0.5F, 5, FLINT_TOOL_MATERIALS);
    public static final DeferredItem<KnifeItem> TUSK_DAGGER = ITEMS.registerItem("tusk_dagger", p -> new KnifeItem(p, 1.3F, 1.1F, KnifeItem.TUSK, 1.6F),
            p -> p.sword(TUSK, 1.5F, -1.8F));
    public static final DeferredItem<Item> BONE_FISH_HOOK = ITEMS.registerSimpleItem("bone_fish_hook");
    /** Sews armour at the Armor Station (loses 1 durability per piece). */
    public static final DeferredItem<Item> BONE_NEEDLE = ITEMS.registerSimpleItem("bone_needle", p -> p.durability(32));
    public static final DeferredBlock<ArmorStationBlock> ARMOR_STATION = BLOCKS.registerBlock("armor_station", ArmorStationBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> ARMOR_STATION_ITEM = ITEMS.registerSimpleBlockItem("armor_station", ARMOR_STATION);
    public static final Supplier<MenuType<ArmorStationMenu>> ARMOR_STATION_MENU = MENUS.register("armor_station",
            () -> new MenuType<>(ArmorStationMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredItem<BoneHookRodItem> BONE_HOOK_FISHING_ROD = ITEMS.registerItem("bone_hook_fishing_rod", BoneHookRodItem::new,
            p -> p.durability(48).enchantable(1));
    public static final DeferredItem<Item> TALLOW = ITEMS.registerSimpleItem("tallow");
    /** Eggs boiled on a campfire; peeled in the crafting grid (-> Peeled Egg + Eggshell, which works like bone meal). */
    public static final DeferredItem<Item> BOILED_EGG = ITEMS.registerSimpleItem("boiled_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PEELED_EGG = food("peeled_egg", 3, 0.6F);
    public static final DeferredItem<net.minecraft.world.item.BoneMealItem> EGGSHELL = ITEMS.registerItem("eggshell", net.minecraft.world.item.BoneMealItem::new);
    public static final DeferredItem<SpecialArrowItem> BONE_ARROW = ITEMS.registerItem("bone_arrow", p -> new SpecialArrowItem(p, 1.5, false));
    public static final DeferredItem<SpecialArrowItem> BARBED_ARROW = ITEMS.registerItem("barbed_arrow", p -> new SpecialArrowItem(p, 3.0, false));
    public static final DeferredItem<SpecialArrowItem> FIRE_ARROW = ITEMS.registerItem("fire_arrow", p -> new SpecialArrowItem(p, 2.0, true));
    public static final DeferredItem<net.minecraft.world.item.BowItem> SINEW_BOW = ITEMS.registerItem("sinew_bow", net.minecraft.world.item.BowItem::new,
            p -> p.durability(200).enchantable(1));
    public static final Supplier<MenuType<FletchingMenu>> FLETCHING_MENU = MENUS.register("fletching_table",
            () -> new MenuType<>(FletchingMenu::new, FeatureFlags.VANILLA_SET));

    // --- Fur (from bear pelts): warmer than leather ---
    public static final net.minecraft.world.item.equipment.ArmorMaterial FUR = new net.minecraft.world.item.equipment.ArmorMaterial(7,
            java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET, 1, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 3,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, 2, net.minecraft.world.item.equipment.ArmorType.BOOTS, 1,
                    net.minecraft.world.item.equipment.ArmorType.BODY, 3),
            12, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_fur_armor")),
            net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "fur")));
    public static final DeferredItem<Item> FUR_HOOD = ITEMS.registerSimpleItem("fur_hood", p -> p.humanoidArmor(FUR, net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final DeferredItem<Item> FUR_COAT = ITEMS.registerSimpleItem("fur_coat", p -> p.humanoidArmor(FUR, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> FUR_LEGGINGS = ITEMS.registerSimpleItem("fur_leggings", p -> p.humanoidArmor(FUR, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final DeferredItem<Item> FUR_BOOTS = ITEMS.registerSimpleItem("fur_boots", p -> p.humanoidArmor(FUR, net.minecraft.world.item.equipment.ArmorType.BOOTS));
    public static final DeferredBlock<BearRugBlock> BEAR_RUG = BLOCKS.registerBlock("bear_rug", BearRugBlock::new, p -> p
            .mapColor(MapColor.COLOR_BROWN).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> BEAR_RUG_ITEM = ITEMS.registerSimpleBlockItem("bear_rug", BEAR_RUG);

    public static final DeferredItem<BarkBasketItem> BARK_BASKET = ITEMS.registerItem("bark_basket", BarkBasketItem::new, p -> p.stacksTo(1));
    public static final Supplier<MenuType<BarkBasketMenu>> BARK_BASKET_MENU = MENUS.register("bark_basket",
            () -> new MenuType<>(BarkBasketMenu::new, FeatureFlags.VANILLA_SET));
    /** Leather Satchel: 9 slots for anything but other containers; sewn in the Armor Station. */
    public static final DeferredItem<LeatherSatchelItem> LEATHER_SATCHEL = ITEMS.registerItem("leather_satchel", LeatherSatchelItem::new, p -> p.stacksTo(1));
    public static final Supplier<MenuType<BarkBasketMenu>> LEATHER_SATCHEL_MENU = MENUS.register("leather_satchel",
            () -> new MenuType<>(BarkBasketMenu::satchel, FeatureFlags.VANILLA_SET));

    // Firewood: what logs drop (2 each), one per wood type so 4 of them (2x2) craft back into that log.
    public static final Map<String, DeferredItem<Item>> FIREWOOD = WOODS.stream().collect(Collectors.toMap(w -> w, w -> ITEMS.registerSimpleItem(w + "_firewood")));
    public static final TagKey<Item> FIREWOOD_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "firewood"));

    // --- Food: animals leave a carcass that is butchered with a knife (see Butchery) ---
    public static final DeferredItem<Item> BEEF_SCRAPS = food("beef_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_BEEF_SCRAPS = food("cooked_beef_scraps", 3, 0.8F);
    public static final DeferredItem<Item> PORKCHOP_SCRAPS = food("porkchop_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_PORKCHOP_SCRAPS = food("cooked_porkchop_scraps", 3, 0.8F);
    public static final DeferredItem<Item> MUTTON_SCRAPS = food("mutton_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_MUTTON_SCRAPS = food("cooked_mutton_scraps", 2, 0.8F);
    public static final DeferredItem<Item> CHICKEN_SCRAPS = food("chicken_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_CHICKEN_SCRAPS = food("cooked_chicken_scraps", 2, 0.6F);
    public static final DeferredItem<Item> RABBIT_SCRAPS = food("rabbit_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_RABBIT_SCRAPS = food("cooked_rabbit_scraps", 2, 0.6F);
    public static final DeferredItem<Item> GAME_MEAT_SCRAPS = food("game_meat_scraps", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_GAME_MEAT_SCRAPS = food("cooked_game_meat_scraps", 3, 0.7F);
    /** Food left too long on a campfire. */
    public static final DeferredItem<Item> BURNT_FOOD = ITEMS.registerSimpleItem("burnt_food");
    public static final DeferredItem<Item> RAW_HIDE = ITEMS.registerSimpleItem("raw_hide");
    public static final DeferredItem<Item> SCRAPED_HIDE = ITEMS.registerSimpleItem("scraped_hide");
    public static final DeferredItem<Item> WOOL_THREAD = ITEMS.registerSimpleItem("wool_thread");
    /** Dead fish and catches: must be gutted with the flint knife (-> vanilla raw fish as the fillet, plus Fish Bones). */
    public static final DeferredItem<Item> WHOLE_COD = ITEMS.registerSimpleItem("whole_cod");
    public static final DeferredItem<Item> WHOLE_SALMON = ITEMS.registerSimpleItem("whole_salmon");
    public static final DeferredItem<Item> WHOLE_TROPICAL_FISH = ITEMS.registerSimpleItem("whole_tropical_fish");
    public static final DeferredItem<Item> WHOLE_PUFFERFISH = ITEMS.registerSimpleItem("whole_pufferfish");
    public static final DeferredItem<Item> FISH_BONES = ITEMS.registerSimpleItem("fish_bones");
    public static final TagKey<Item> WHOLE_FISH = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "whole_fish"));
    /** Stone age ranged tools from the Tool Station: weaker and shorter-lived than the real ones. */
    public static final DeferredItem<CrudeBowItem> CRUDE_BOW = ITEMS.registerItem("crude_bow", CrudeBowItem::new, p -> p.durability(96).enchantable(1));
    public static final DeferredItem<net.minecraft.world.item.FishingRodItem> CRUDE_FISHING_ROD = ITEMS.registerItem("crude_fishing_rod",
            net.minecraft.world.item.FishingRodItem::new, p -> p.durability(24).enchantable(1));

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = BLOCKS.registerBlock("drying_rack", DryingRackBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion().randomTicks());
    public static final DeferredItem<BlockItem> DRYING_RACK_ITEM = ITEMS.registerSimpleBlockItem("drying_rack", DRYING_RACK);

    public static final DeferredBlock<BonePileBlock> BONE_PILE = BLOCKS.registerBlock("bone_pile", BonePileBlock::new, p -> p
            .mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.POPPED));

    // Blood trail of a bleeding animal: replaceable, no collision, fades on random ticks.
    public static final DeferredBlock<BloodBlock> BLOOD = BLOCKS.registerBlock("blood", BloodBlock::new, p -> p
            .mapColor(MapColor.COLOR_RED).noCollision().instabreak().replaceable().randomTicks().noOcclusion()
            .sound(SoundType.SLIME_BLOCK).pushReaction(PushReaction.POPPED).noLootTable());
    public static final DeferredBlock<TwigsBlock> TWIGS = BLOCKS.registerBlock("twigs", TwigsBlock::new, p -> p
            .mapColor(MapColor.WOOD).noCollision().instabreak().replaceable().noOcclusion()
            .sound(SoundType.GRASS).pushReaction(PushReaction.POPPED));
    // Torches blown out by the wind on windy nights (Wind); relit with a torch, fire drill or flint and steel.
    public static final DeferredBlock<UnlitTorchBlock> UNLIT_TORCH = BLOCKS.registerBlock("unlit_torch", UnlitTorchBlock::new, p -> p
            .noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<UnlitTorchBlock.Wall> UNLIT_WALL_TORCH = BLOCKS.registerBlock("unlit_wall_torch", UnlitTorchBlock.Wall::new, p -> p
            .noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.POPPED));

    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS = ENTITY_TYPES.registerEntityType(
            "carcass", CarcassEntity::new, MobCategory.MISC, b -> b.sized(0.9F, 0.5F).clientTrackingRange(8));

    /** Blocks that are never items (decals, pots holding plants). Everything else gets a block item, for creative. */
    private static final java.util.Set<String> NO_ITEM = java.util.Set.of("blood", "unlit_torch", "unlit_wall_torch", "barley_crop", "einkorn_crop", "emmer_crop", "pea_crop");

    /**
     * Every mod block without an item of its own gets a plain block item, so all of them (bushes, mushrooms, twigs,
     * fresh leaf litter, the ancient oak ...) can be picked from the creative tab. In survival they still drop what their
     * loot tables say, so this adds no new way to get them.
     */
    private static void registerMissingBlockItems() {
        java.util.Set<String> items = new java.util.HashSet<>();
        ITEMS.getEntries().forEach(i -> items.add(i.getId().getPath()));
        for (var block : java.util.List.copyOf(BLOCKS.getEntries())) {
            String id = block.getId().getPath();
            if (items.contains(id) || NO_ITEM.contains(id) || id.startsWith("potted_")) continue;
            ITEMS.registerItem(id, p -> new net.minecraft.world.item.BlockItem(block.get(), p), p -> p.useBlockDescriptionPrefix());
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECHANICAL_TAB = CREATIVE_MODE_TABS.register("mechanical_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mechanical"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> FLINT_PICKAXE.get().getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    public Mechanical(IEventBus modEventBus) {
        com.mechanical.wildlife.Wildlife.init();
        Temperature.init(); Trees.init(); VillageAnimals.init(); ArrowWounds.init(); Tinder.init(); Foraging.init(); ForestUndergrowth.init(); AncientOak.init(); com.mechanical.worldgen.Structures.init(); com.mechanical.people.People.init(modEventBus); BronzeOres.init(); Farming.init(); Casting.init();
        registerMissingBlockItems();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        Casting.COMPONENTS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        com.mechanical.worldgen.Structures.FEATURE_TYPES.register(modEventBus);
    }
}
