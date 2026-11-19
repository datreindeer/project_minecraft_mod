package com.mechanical.wildlife.coast;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;

import com.mechanical.Butchery;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Coast and river fauna: the Seal and the Walrus (haul out on shores and ice, slip into the water), the Pike and the
 * Sturgeon (river fish: they swim, can be speared, and bite on the hook too, see the fishing loot table).
 * Products: seal and walrus meat, Blubber (fuel for the {@link OilLampBlock}), Sealskin (a tall Drying Rack, then the
 * Sealskin Parka and Boots: warm and waterproof), Walrus Hide (a whole tall rack; cut into Cordage or sewn into the Walrus
 * Hide Cuirass), Walrus Ivory (Ivory Knife, needles, fish hooks), whole pike and sturgeon gutted into fillets, Sturgeon
 * Roe and Isinglass (soaks into Hide Glue). Data: scripts/gen_fauna_coast.py; textures: scripts/gen_fauna_coast_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class CoastFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<Seal>> SEAL = ENTITY_TYPES.registerEntityType(
            "seal", Seal::new, MobCategory.CREATURE, b -> b.sized(1.0F, 0.6F).eyeHeight(0.5F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Walrus>> WALRUS = ENTITY_TYPES.registerEntityType(
            "walrus", Walrus::new, MobCategory.CREATURE, b -> b.sized(2.0F, 1.2F).eyeHeight(1.0F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Pike>> PIKE = ENTITY_TYPES.registerEntityType(
            "pike", Pike::new, MobCategory.WATER_AMBIENT, b -> b.sized(0.7F, 0.4F).eyeHeight(0.25F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<Sturgeon>> STURGEON = ENTITY_TYPES.registerEntityType(
            "sturgeon", Sturgeon::new, MobCategory.WATER_CREATURE, b -> b.sized(1.3F, 0.6F).eyeHeight(0.35F).clientTrackingRange(8));

    // ---- seal ----
    public static final DeferredItem<Item> RAW_SEAL_MEAT = raw("raw_seal_meat", 3);
    public static final DeferredItem<Item> COOKED_SEAL_MEAT = food("cooked_seal_meat", 7, 0.8F);
    /** Seal and walrus fat: burns in the Oil Lamp. */
    public static final DeferredItem<Item> BLUBBER = ITEMS.registerSimpleItem("blubber");
    /** Fresh off the carcass; cured on a tall Drying Rack (one block of it, like a wolf pelt). */
    public static final DeferredItem<Item> RAW_SEALSKIN = ITEMS.registerSimpleItem("raw_sealskin");
    public static final DeferredItem<Item> SEALSKIN = ITEMS.registerSimpleItem("sealskin");
    public static final ArmorMaterial SEALSKIN_MATERIAL = new ArmorMaterial(8,
            java.util.Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 3, ArmorType.LEGGINGS, 2, ArmorType.BOOTS, 1, ArmorType.BODY, 3),
            12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, itemTag("repairs_sealskin_armor"),
            net.minecraft.resources.ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mechanical.MODID, "sealskin")));
    public static final DeferredItem<Item> SEALSKIN_PARKA = ITEMS.registerSimpleItem("sealskin_parka", p -> p.humanoidArmor(SEALSKIN_MATERIAL, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> SEALSKIN_BOOTS = ITEMS.registerSimpleItem("sealskin_boots", p -> p.humanoidArmor(SEALSKIN_MATERIAL, ArmorType.BOOTS));
    /** Share of the cold each Sealskin piece keeps out (Temperature), and how much of the chill of the water it keeps out. */
    public static final float SEALSKIN_PER_PIECE = 0.2F, SEALSKIN_WATERPROOF = 0.35F;

    // ---- walrus ----
    public static final DeferredItem<Item> RAW_WALRUS_MEAT = raw("raw_walrus_meat", 3);
    public static final DeferredItem<Item> COOKED_WALRUS_MEAT = food("cooked_walrus_meat", 8, 0.8F);
    public static final DeferredItem<Item> WALRUS_IVORY = ITEMS.registerSimpleItem("walrus_ivory");
    /** Cured on a tall Drying Rack (the whole rack, like a bear pelt). */
    public static final DeferredItem<Item> RAW_WALRUS_HIDE = ITEMS.registerSimpleItem("raw_walrus_hide");
    public static final DeferredItem<Item> WALRUS_HIDE = ITEMS.registerSimpleItem("walrus_hide");
    public static final ArmorMaterial WALRUS_HIDE_MATERIAL = new ArmorMaterial(14,
            java.util.Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 5, ArmorType.LEGGINGS, 4, ArmorType.BOOTS, 1, ArmorType.BODY, 5),
            9, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.05F, itemTag("repairs_walrus_hide_armor"),
            net.minecraft.resources.ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mechanical.MODID, "walrus_hide")));
    /** Layered walrus hide, as the arctic hunters wore it: tough, but no warmth. */
    public static final DeferredItem<Item> WALRUS_HIDE_CUIRASS = ITEMS.registerSimpleItem("walrus_hide_cuirass",
            p -> p.humanoidArmor(WALRUS_HIDE_MATERIAL, ArmorType.CHESTPLATE));
    /** Ivory blade: cuts as well as the Tusk Dagger (tough hides: bear, walrus), a little less of a skinning knife. */
    public static final net.minecraft.world.item.ToolMaterial IVORY = new net.minecraft.world.item.ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 180, 3.0F, 0.5F, 6, itemTag("ivory_tool_materials"));
    public static final DeferredItem<com.mechanical.KnifeItem> IVORY_KNIFE = ITEMS.registerItem("ivory_knife",
            p -> new com.mechanical.KnifeItem(p, 1.3F, 1.1F, com.mechanical.KnifeItem.TUSK, 1.3F), p -> p.sword(IVORY, 1.5F, -1.9F));

    // ---- fish ----
    public static final DeferredItem<Item> WHOLE_PIKE = ITEMS.registerSimpleItem("whole_pike");
    public static final DeferredItem<Item> RAW_PIKE = food("raw_pike", 2, 0.1F);
    public static final DeferredItem<Item> COOKED_PIKE = food("cooked_pike", 6, 0.8F);
    public static final DeferredItem<Item> WHOLE_STURGEON = ITEMS.registerSimpleItem("whole_sturgeon", p -> p.stacksTo(16));
    public static final DeferredItem<Item> RAW_STURGEON = food("raw_sturgeon", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_STURGEON = food("cooked_sturgeon", 7, 0.9F);
    /** Caviar: eaten raw, little food but very filling. */
    public static final DeferredItem<Item> STURGEON_ROE = ITEMS.registerSimpleItem("sturgeon_roe",
            p -> p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(1.2F).build(), Consumables.DRIED_KELP));
    /** The dried swim bladder: soaks into Hide Glue (crafting). */
    public static final DeferredItem<Item> ISINGLASS = ITEMS.registerSimpleItem("isinglass");

    // ---- oil lamp ----
    public static final DeferredBlock<OilLampBlock> OIL_LAMP = BLOCKS.registerBlock("oil_lamp", OilLampBlock::new, p -> p
            .mapColor(MapColor.STONE).strength(0.8F).sound(SoundType.STONE).noOcclusion().randomTicks()
            .lightLevel(s -> s.getValue(OilLampBlock.LIT) ? OilLampBlock.LIGHT : 0));
    public static final DeferredItem<BlockItem> OIL_LAMP_ITEM = ITEMS.registerSimpleBlockItem("oil_lamp", OIL_LAMP);
    /** What the Oil Lamp burns: Blubber, Tallow, Lard, Bear Fat (data tag). */
    public static final TagKey<Item> LAMP_FUEL = itemTag("lamp_fuel");

    public static final DeferredItem<SpawnEggItem> SEAL_SPAWN_EGG = egg("seal_spawn_egg", SEAL);
    public static final DeferredItem<SpawnEggItem> WALRUS_SPAWN_EGG = egg("walrus_spawn_egg", WALRUS);
    public static final DeferredItem<SpawnEggItem> PIKE_SPAWN_EGG = egg("pike_spawn_egg", PIKE);
    public static final DeferredItem<SpawnEggItem> STURGEON_SPAWN_EGG = egg("sturgeon_spawn_egg", STURGEON);

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
    }

    /** Raw red meat: a chance of Hunger eaten raw, like raw chicken. */
    private static DeferredItem<Item> raw(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build(), Consumables.CHICKEN));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    /** Loads this class so its registrations happen, and registers the carcass yields; call from the mod constructor. */
    public static void init() {
        Butchery.register(() -> SEAL.get(), new Butchery.Yield(RAW_SEAL_MEAT, 2, 3, List.of(new Butchery.Extra(RAW_SEALSKIN, 1.0F, 1, 1)),
                List.of(new Butchery.Extra(BLUBBER, 0.9F, 1, 3), new Butchery.Extra(Wildlife.SINEW, 0.4F, 1, 1))));
        // both tusks always come out (chance 1 is never scaled into a count)
        Butchery.register(() -> WALRUS.get(), new Butchery.Yield(RAW_WALRUS_MEAT, 4, 6, List.of(new Butchery.Extra(RAW_WALRUS_HIDE, 1.0F, 1, 1)),
                List.of(new Butchery.Extra(WALRUS_IVORY, 1.0F, 2, 2), new Butchery.Extra(BLUBBER, 1.0F, 2, 4),
                        new Butchery.Extra(Wildlife.SINEW, 0.6F, 1, 2), new Butchery.Extra(() -> Items.BONE, 0.7F, 1, 3))));
        Butchery.toughHide(() -> WALRUS.get());
        // the sturgeon's tough skin is sturgeon leather; then the fillets, roe and the swim bladder
        Butchery.register(() -> STURGEON.get(), new Butchery.Yield(RAW_STURGEON, 4, 6, List.of(new Butchery.Extra(Mechanical.RAW_HIDE, 0.3F, 1, 1)),
                List.of(new Butchery.Extra(STURGEON_ROE, 0.6F, 1, 3), new Butchery.Extra(ISINGLASS, 1.0F, 1, 1),
                        new Butchery.Extra(Mechanical.FISH_BONES, 0.8F, 1, 2))));
        Butchery.alwaysCarcass(() -> STURGEON.get());
        Butchery.wholeMeat(() -> STURGEON.get());
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(SEAL.get(), Seal.createAttributes().build());
        event.put(WALRUS.get(), Walrus.createAttributes().build());
        event.put(PIKE.get(), Pike.createAttributes().build());
        event.put(STURGEON.get(), Sturgeon.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        // seals and walruses haul out on bare shores and ice: sand, gravel, stone, snow, ice (reads only this column). Not
        // ON_GROUND: ice only lets polar bears spawn on it
        event.register(SEAL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> shoreSpawn(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WALRUS.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> shoreSpawn(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PIKE.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(STURGEON.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    static boolean isShore(BlockState below) {
        return below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL) || below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.ICE)
                || below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.SNOW);
    }

    private static boolean shoreSpawn(LevelAccessor level, BlockPos pos) {
        BlockState at = level.getBlockState(pos);
        return (at.isAir() || at.is(Blocks.SNOW)) && level.canSeeSky(pos) && level.getRawBrightness(pos, 0) > 8
                && level.getFluidState(pos.below()).isEmpty() && isShore(level.getBlockState(pos.below()));
    }

    /** Gutting a whole sturgeon (whole fish + knife: fillets and Fish Bones) also gives its swim bladder, and often roe. */
    @SubscribeEvent
    static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !event.getCrafting().is(RAW_STURGEON.get())) return;
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            if (!event.getInventory().getItem(i).is(WHOLE_STURGEON.get())) continue;
            player.getInventory().placeItemBackInInventory(new ItemStack(ISINGLASS.get()), Prediction.SERVER_ONLY);
            if (player.getRandom().nextFloat() < 0.4F) player.getInventory().placeItemBackInInventory(new ItemStack(STURGEON_ROE.get()), Prediction.SERVER_ONLY);
            return;
        }
    }

    private CoastFauna() {}
}
