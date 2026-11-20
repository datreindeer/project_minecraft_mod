package com.mechanical.wildlife.coldhunters;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.BearRugBlock;
import com.mechanical.Butchery;
import com.mechanical.KnifeItem;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Cold hunters: Wolverine, Lynx, Ermine, Lemming and Pine Marten. Entity types, what they give (meat, pelts, claws), the
 * goods made from them (Wolverine-Trimmed Hood, Claw Flesher, Lynx Rug, Ermine Mantle, Marten Fur Cap), spawn eggs,
 * attributes, spawn rules (spawns themselves are data: scripts/gen_fauna_coldhunters.py) and butchering yields.
 * Raw pelts cure on the Drying Rack ({@link PeltCuring}). Docs: docs/fauna/coldhunters.md.
 */
@EventBusSubscriber(modid = MODID)
public final class ColdhuntersFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<Wolverine>> WOLVERINE = ENTITY_TYPES.registerEntityType(
            "wolverine", Wolverine::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.75F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Lynx>> LYNX = ENTITY_TYPES.registerEntityType(
            "lynx", Lynx::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Ermine>> ERMINE = ENTITY_TYPES.registerEntityType(
            "ermine", Ermine::new, MobCategory.CREATURE, b -> b.sized(0.4F, 0.35F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Lemming>> LEMMING = ENTITY_TYPES.registerEntityType(
            "lemming", Lemming::new, MobCategory.CREATURE, b -> b.sized(0.35F, 0.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<PineMarten>> PINE_MARTEN = ENTITY_TYPES.registerEntityType(
            "pine_marten", PineMarten::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.45F).clientTrackingRange(8));

    // ---- meat (raw perishes in a day, cooked in three; smokable and dryable: gen_fauna_coldhunters.py) ----
    public static final DeferredItem<Item> RAW_WOLVERINE = food("raw_wolverine", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_WOLVERINE = food("cooked_wolverine", 6, 0.6F);
    public static final DeferredItem<Item> RAW_LYNX = food("raw_lynx", 2, 0.3F);
    public static final DeferredItem<Item> COOKED_LYNX = food("cooked_lynx", 6, 0.7F);
    public static final DeferredItem<Item> RAW_ERMINE = food("raw_ermine", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_ERMINE = food("cooked_ermine", 3, 0.5F);
    public static final DeferredItem<Item> RAW_LEMMING = food("raw_lemming", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_LEMMING = food("cooked_lemming", 2, 0.4F);
    public static final DeferredItem<Item> RAW_MARTEN = food("raw_marten", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_MARTEN = food("cooked_marten", 3, 0.6F);

    // ---- pelts: raw off the carcass, cured on a Drying Rack (PeltCuring); the lemming's is too small to cure ----
    public static final DeferredItem<Item> RAW_WOLVERINE_PELT = ITEMS.registerSimpleItem("raw_wolverine_pelt");
    public static final DeferredItem<Item> WOLVERINE_FUR = ITEMS.registerSimpleItem("wolverine_fur");
    public static final DeferredItem<Item> RAW_LYNX_PELT = ITEMS.registerSimpleItem("raw_lynx_pelt");
    public static final DeferredItem<Item> LYNX_PELT = ITEMS.registerSimpleItem("lynx_pelt");
    public static final DeferredItem<Item> RAW_ERMINE_PELT = ITEMS.registerSimpleItem("raw_ermine_pelt");
    public static final DeferredItem<Item> ERMINE_FUR = ITEMS.registerSimpleItem("ermine_fur");
    public static final DeferredItem<Item> RAW_MARTEN_PELT = ITEMS.registerSimpleItem("raw_marten_pelt");
    public static final DeferredItem<Item> MARTEN_FUR = ITEMS.registerSimpleItem("marten_fur");
    public static final DeferredItem<Item> LEMMING_PELT = ITEMS.registerSimpleItem("lemming_pelt");
    public static final DeferredItem<Item> WOLVERINE_CLAW = ITEMS.registerSimpleItem("wolverine_claw");

    /** Raw pelt -> what it cures into on the Drying Rack. */
    static final Map<Supplier<Item>, Supplier<Item>> CURES = Map.of(RAW_WOLVERINE_PELT, WOLVERINE_FUR, RAW_LYNX_PELT, LYNX_PELT,
            RAW_ERMINE_PELT, ERMINE_FUR, RAW_MARTEN_PELT, MARTEN_FUR);

    // ---- goods ----
    /** Two wolverine claws lashed into a short hooked blade: a skinning knife (more pelt and hide) for the flint age. */
    public static final ToolMaterial CLAW = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 90, 2.5F, 0.0F, 5,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_claw_flesher")));
    public static final DeferredItem<KnifeItem> CLAW_FLESHER = ITEMS.registerItem("claw_flesher", p -> new KnifeItem(p, 1.1F, 1.0F, KnifeItem.FLINT, 1.4F),
            p -> p.sword(CLAW, 1.0F, -2.0F));
    /** A Fur Hood with a ruff of wolverine fur round the face: frost does not cling to it (no freezing damage). */
    public static final DeferredItem<Item> WOLVERINE_TRIMMED_HOOD = ITEMS.registerSimpleItem("wolverine_trimmed_hood",
            p -> p.humanoidArmor(fur(8, 1, 12, "wolverine_fur", "wolverine_trimmed_hood"), ArmorType.HELMET));
    /** Snow-white ermine sewn over the shoulders: a chieftain's mantle, warm as fur and easy to enchant. */
    public static final DeferredItem<Item> ERMINE_MANTLE = ITEMS.registerSimpleItem("ermine_mantle",
            p -> p.humanoidArmor(fur(7, 2, 22, "ermine_fur", "ermine_mantle"), ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> MARTEN_FUR_CAP = ITEMS.registerSimpleItem("marten_fur_cap",
            p -> p.humanoidArmor(fur(7, 1, 15, "marten_fur", "marten_fur_cap"), ArmorType.HELMET));
    public static final DeferredBlock<BearRugBlock> LYNX_RUG = BLOCKS.registerBlock("lynx_rug", BearRugBlock::new, p -> p
            .mapColor(MapColor.COLOR_BROWN).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> LYNX_RUG_ITEM = ITEMS.registerSimpleBlockItem("lynx_rug", LYNX_RUG);

    public static final DeferredItem<SpawnEggItem> WOLVERINE_SPAWN_EGG = egg("wolverine_spawn_egg", WOLVERINE);
    public static final DeferredItem<SpawnEggItem> LYNX_SPAWN_EGG = egg("lynx_spawn_egg", LYNX);
    public static final DeferredItem<SpawnEggItem> ERMINE_SPAWN_EGG = egg("ermine_spawn_egg", ERMINE);
    public static final DeferredItem<SpawnEggItem> LEMMING_SPAWN_EGG = egg("lemming_spawn_egg", LEMMING);
    public static final DeferredItem<SpawnEggItem> PINE_MARTEN_SPAWN_EGG = egg("pine_marten_spawn_egg", PINE_MARTEN);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    /** A one-piece fur armour material: its own equipment asset, repaired with {@code repair}. */
    private static ArmorMaterial fur(int durability, int defense, int enchantability, String repair, String asset) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, defense, ArmorType.CHESTPLATE, defense, ArmorType.LEGGINGS, defense,
                ArmorType.BOOTS, defense, ArmorType.BODY, defense), enchantability, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_" + repair)),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, asset)));
    }

    private static Butchery.Extra extra(Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads this class so its registrations happen, and registers the butchering yields. Call from the mod constructor. */
    public static void init() {
        Supplier<Item> bone = () -> Items.BONE;
        // first pass: the pelt; second: meat, then claws, sinew and bone. Counts for a flint knife.
        Butchery.register(WOLVERINE::get, new Butchery.Yield(RAW_WOLVERINE, 1, 3, List.of(extra(RAW_WOLVERINE_PELT, 1.0F, 1, 1)),
                List.of(extra(WOLVERINE_CLAW, 0.6F, 1, 2), extra(Wildlife.SINEW, 0.5F, 1, 1), extra(bone, 0.5F, 1, 2))));
        Butchery.register(LYNX::get, new Butchery.Yield(RAW_LYNX, 2, 3, List.of(extra(RAW_LYNX_PELT, 1.0F, 1, 1)),
                List.of(extra(Wildlife.SINEW, 0.5F, 1, 2), extra(bone, 0.6F, 1, 2))));
        Butchery.register(ERMINE::get, new Butchery.Yield(RAW_ERMINE, 1, 1, List.of(extra(RAW_ERMINE_PELT, 0.8F, 1, 1)), List.of()));
        Butchery.register(LEMMING::get, new Butchery.Yield(RAW_LEMMING, 1, 1, List.of(extra(LEMMING_PELT, 0.6F, 1, 1)), List.of()));
        Butchery.register(PINE_MARTEN::get, new Butchery.Yield(RAW_MARTEN, 1, 1, List.of(extra(RAW_MARTEN_PELT, 0.9F, 1, 1)), List.of()));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(WOLVERINE.get(), Wolverine.createAttributes().build());
        event.put(LYNX.get(), Lynx.createAttributes().build());
        event.put(ERMINE.get(), Ermine.createAttributes().build());
        event.put(LEMMING.get(), Lemming.createAttributes().build());
        event.put(PINE_MARTEN.get(), PineMarten.createAttributes().build());
    }

    /** All five on the fox's ground (grass, snow, podzol, coarse dirt) in daylight, like the fox. */
    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(WOLVERINE.get(), LYNX.get(), ERMINE.get(), LEMMING.get(), PINE_MARTEN.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> level.getBlockState(pos.below()).is(BlockTags.FOXES_SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    /** Foxes hunt lemmings (as they hunt rabbits and chickens). */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Fox fox) {
            fox.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(fox, Lemming.class, 10, false, false, null));
        }
    }

    private ColdhuntersFauna() {}
}
