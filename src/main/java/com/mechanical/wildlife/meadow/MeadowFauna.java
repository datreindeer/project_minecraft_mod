package com.mechanical.wildlife.meadow;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.Map;

import com.mechanical.Butchery;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Meadow fauna: Wild Cat, Mole, Field Vole, Adder and Grass Snake - entity types, what they give (meat, pelts, snakeskin,
 * venom), the Molehill, spawn eggs, attributes, spawn rules and butchering yields. Spawns are in data
 * (scripts/gen_fauna_meadow.py, biome modifiers add_*_meadow), textures in scripts/gen_fauna_meadow_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MeadowFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<WildCat>> WILD_CAT = ENTITY_TYPES.registerEntityType(
            "wild_cat", WildCat::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Mole>> MOLE = ENTITY_TYPES.registerEntityType(
            "mole", Mole::new, MobCategory.CREATURE, b -> b.sized(0.35F, 0.25F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<FieldVole>> FIELD_VOLE = ENTITY_TYPES.registerEntityType(
            "field_vole", FieldVole::new, MobCategory.CREATURE, b -> b.sized(0.3F, 0.25F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Adder>> ADDER = ENTITY_TYPES.registerEntityType(
            "adder", Adder::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.25F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<GrassSnake>> GRASS_SNAKE = ENTITY_TYPES.registerEntityType(
            "grass_snake", GrassSnake::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.25F).clientTrackingRange(8));

    /** What voles eat out of chests and barrels (FieldVole.RaidStoreGoal). */
    public static final TagKey<Item> VOLE_FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "vole_food"));

    // ---- meat (cooked on the campfire / smoker; spoil like the scraps) ----
    public static final DeferredItem<Item> RAW_WILDCAT = food("raw_wildcat", 2, 0.3F);
    public static final DeferredItem<Item> COOKED_WILDCAT = food("cooked_wildcat", 5, 0.7F);
    public static final DeferredItem<Item> RAW_MOLE = food("raw_mole", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_MOLE = food("cooked_mole", 3, 0.6F);
    public static final DeferredItem<Item> RAW_VOLE = food("raw_vole", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_VOLE = food("cooked_vole", 2, 0.5F);
    public static final DeferredItem<Item> RAW_SNAKE = food("raw_snake", 1, 0.2F);
    public static final DeferredItem<Item> COOKED_SNAKE = food("cooked_snake", 4, 0.6F);

    // ---- pelts: fleshed with a knife in the grid (raw -> cured) ----
    public static final DeferredItem<Item> RAW_WILDCAT_PELT = ITEMS.registerSimpleItem("raw_wildcat_pelt");
    public static final DeferredItem<Item> WILDCAT_PELT = ITEMS.registerSimpleItem("wildcat_pelt");
    public static final DeferredItem<Item> RAW_MOLE_PELT = ITEMS.registerSimpleItem("raw_mole_pelt");
    public static final DeferredItem<Item> MOLESKIN = ITEMS.registerSimpleItem("moleskin");
    public static final DeferredItem<Item> SNAKESKIN = ITEMS.registerSimpleItem("snakeskin");
    /** Adder only: poison arrows and the Snakebite Poultice. */
    public static final DeferredItem<Item> VENOM_GLAND = ITEMS.registerSimpleItem("venom_gland");
    /** Eaten: cures poison (an adder bite). */
    public static final DeferredItem<Item> SNAKEBITE_POULTICE = ITEMS.registerSimpleItem("snakebite_poultice", p -> p.stacksTo(16)
            .component(DataComponents.CONSUMABLE, Consumables.defaultFood().consumeSeconds(1.0F).onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.POISON)).build()));

    // ---- clothing ----
    /** Snakeskin over leather boots: an adder's strike can't get through (Adder#doHurtTarget). */
    public static final ArmorMaterial SNAKESKIN_MATERIAL = material(5, 1, 10, "snakeskin", "repairs_snakeskin");
    public static final DeferredItem<Item> SNAKESKIN_BOOTS = ITEMS.registerSimpleItem("snakeskin_boots", p -> p.humanoidArmor(SNAKESKIN_MATERIAL, ArmorType.BOOTS));
    public static final ArmorMaterial WILDCAT_FUR = material(5, 1, 12, "wildcat_fur", "repairs_wildcat_fur");
    public static final DeferredItem<Item> WILDCAT_FUR_CAP = ITEMS.registerSimpleItem("wildcat_fur_cap", p -> p.humanoidArmor(WILDCAT_FUR, ArmorType.HELMET));

    /** Small dirt mound a mole pushes up (Mole); it settles back into the ground after a while. */
    public static final DeferredBlock<MolehillBlock> MOLEHILL = BLOCKS.registerBlock("molehill", MolehillBlock::new, p -> p
            .mapColor(MapColor.DIRT).strength(0.3F).sound(SoundType.ROOTED_DIRT).noOcclusion().randomTicks().pushReaction(PushReaction.POPPED));

    public static final DeferredItem<SpawnEggItem> WILD_CAT_SPAWN_EGG = egg("wild_cat_spawn_egg", WILD_CAT);
    public static final DeferredItem<SpawnEggItem> MOLE_SPAWN_EGG = egg("mole_spawn_egg", MOLE);
    public static final DeferredItem<SpawnEggItem> FIELD_VOLE_SPAWN_EGG = egg("field_vole_spawn_egg", FIELD_VOLE);
    public static final DeferredItem<SpawnEggItem> ADDER_SPAWN_EGG = egg("adder_spawn_egg", ADDER);
    public static final DeferredItem<SpawnEggItem> GRASS_SNAKE_SPAWN_EGG = egg("grass_snake_spawn_egg", GRASS_SNAKE);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static ArmorMaterial material(int durability, int defense, int enchant, String asset, String repairTag) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, defense, ArmorType.CHESTPLATE, defense, ArmorType.LEGGINGS, defense,
                ArmorType.BOOTS, defense, ArmorType.BODY, defense), enchant, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, repairTag)),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mechanical.MODID, asset)));
    }

    private static Butchery.Extra extra(java.util.function.Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    /** Loads this class so its registrations happen, and adds the butchering yields; call from the mod constructor. */
    public static void init() {
        // pelts and snakeskin come off first; the adder's venom gland comes out with the meat
        Butchery.register(() -> WILD_CAT.get(), new Butchery.Yield(RAW_WILDCAT, 1, 2, List.of(extra(RAW_WILDCAT_PELT, 0.9F, 1, 1)),
                List.of(extra(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.register(() -> MOLE.get(), new Butchery.Yield(RAW_MOLE, 1, 1, List.of(extra(RAW_MOLE_PELT, 0.85F, 1, 1)), List.of()));
        // a vole is a mouthful: no pelt worth taking, just the little body to roast
        Butchery.register(() -> FIELD_VOLE.get(), new Butchery.Yield(RAW_VOLE, 1, 1, List.of(), List.of()));
        Butchery.register(() -> ADDER.get(), new Butchery.Yield(RAW_SNAKE, 1, 1, List.of(extra(SNAKESKIN, 0.85F, 1, 1)),
                List.of(extra(VENOM_GLAND, 0.75F, 1, 1))));
        Butchery.register(() -> GRASS_SNAKE.get(), new Butchery.Yield(RAW_SNAKE, 1, 2, List.of(extra(SNAKESKIN, 0.85F, 1, 1)), List.of()));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(WILD_CAT.get(), WildCat.createAttributes().build());
        event.put(MOLE.get(), Mole.createAttributes().build());
        event.put(FIELD_VOLE.get(), FieldVole.createAttributes().build());
        event.put(ADDER.get(), Adder.createAttributes().build());
        event.put(GRASS_SNAKE.get(), GrassSnake.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (var type : List.of(WILD_CAT.get(), MOLE.get(), FIELD_VOLE.get(), GRASS_SNAKE.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        // adders bask on sunny rocks too: grass or bare stone, in daylight
        event.register(ADDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> (level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON)
                        || level.getBlockState(pos.below()).is(BlockTags.BASE_STONE_OVERWORLD)) && level.getRawBrightness(pos, 0) > 8,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Foxes, cats and ocelots go after voles (and surfaced moles) too. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Fox || event.getEntity() instanceof Cat || event.getEntity() instanceof Ocelot) {
            Animal hunter = (Animal) event.getEntity();
            hunter.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(hunter, Animal.class, true,
                    (target, level) -> target instanceof FieldVole || target instanceof Mole mole && !mole.isBurrowed()));
        }
    }

    /** Removes WildAnimal's built-in goals by class name (they are package-private in com.mechanical.wildlife). */
    static void removeGoals(net.minecraft.world.entity.ai.goal.GoalSelector selector, String... simpleNames) {
        List<String> names = List.of(simpleNames);
        selector.removeAllGoals(g -> names.contains(g.getClass().getSimpleName()));
    }

    private MeadowFauna() {}
}
