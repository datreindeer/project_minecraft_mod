package com.mechanical.wildlife.mountain;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.Map;

import com.mechanical.BearRugBlock;
import com.mechanical.BloodMoon;
import com.mechanical.Butchery;
import com.mechanical.Butchery.Extra;
import com.mechanical.Butchery.Yield;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predators;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Mountain and steppe fauna: Ibex, Chamois, Marmot, Saiga, Cave Bear, Cave Hyena. Entity types, what they give (meat,
 * pelts, horns, fat, the skull), the things made from it (Horn Bow, Chamois Boots, Marmot Salve, rugs), attributes,
 * spawn rules, butchery yields and the night arrival of hyena packs. World spawns, recipes, tags, lang:
 * scripts/gen_fauna_mountain.py; textures: scripts/gen_fauna_mountain_textures.py; docs: docs/fauna/mountain.md.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MountainFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<Ibex>> IBEX = ENTITY_TYPES.registerEntityType(
            "ibex", Ibex::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Chamois>> CHAMOIS = ENTITY_TYPES.registerEntityType(
            "chamois", Chamois::new, MobCategory.CREATURE, b -> b.sized(0.75F, 1.15F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Marmot>> MARMOT = ENTITY_TYPES.registerEntityType(
            "marmot", Marmot::new, MobCategory.CREATURE, b -> b.sized(0.55F, 0.5F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Saiga>> SAIGA = ENTITY_TYPES.registerEntityType(
            "saiga", Saiga::new, MobCategory.CREATURE, b -> b.sized(0.8F, 1.15F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<CaveBear>> CAVE_BEAR = ENTITY_TYPES.registerEntityType(
            "cave_bear", CaveBear::new, MobCategory.CREATURE, b -> b.sized(1.6F, 1.75F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<CaveHyena>> CAVE_HYENA = ENTITY_TYPES.registerEntityType(
            "cave_hyena", CaveHyena::new, MobCategory.CREATURE, b -> b.sized(0.85F, 1.05F).clientTrackingRange(10));

    // ---- meat: raw scraps-sized pieces like venison, cooked on the campfire / smoker; spoil, smoke and dry like it ----
    public static final DeferredItem<Item> IBEX_MEAT = food("ibex_meat", 1, 0.1F), COOKED_IBEX_MEAT = food("cooked_ibex_meat", 3, 0.8F);
    public static final DeferredItem<Item> CHAMOIS_MEAT = food("chamois_meat", 1, 0.1F), COOKED_CHAMOIS_MEAT = food("cooked_chamois_meat", 3, 0.8F);
    /** Fatty: more filling cooked. */
    public static final DeferredItem<Item> MARMOT_MEAT = food("marmot_meat", 1, 0.15F), COOKED_MARMOT_MEAT = food("cooked_marmot_meat", 3, 0.9F);
    public static final DeferredItem<Item> SAIGA_MEAT = food("saiga_meat", 1, 0.1F), COOKED_SAIGA_MEAT = food("cooked_saiga_meat", 3, 0.8F);
    public static final DeferredItem<Item> CAVE_BEAR_MEAT = food("cave_bear_meat", 1, 0.1F), COOKED_CAVE_BEAR_MEAT = food("cooked_cave_bear_meat", 3, 0.9F);
    /** A scavenger's meat: tough, rank, less filling. */
    public static final DeferredItem<Item> HYENA_MEAT = food("hyena_meat", 1, 0.05F), COOKED_HYENA_MEAT = food("cooked_hyena_meat", 2, 0.5F);

    // ---- hides: raw ones are cured with a knife and fat in the grid (fat tanning), the cave bear's on a tall Drying Rack ----
    public static final DeferredItem<Item> RAW_IBEX_PELT = ITEMS.registerSimpleItem("raw_ibex_pelt"), IBEX_PELT = ITEMS.registerSimpleItem("ibex_pelt");
    public static final DeferredItem<Item> RAW_CHAMOIS_HIDE = ITEMS.registerSimpleItem("raw_chamois_hide");
    /** Soft, fat-tanned: the Chamois Boots. */
    public static final DeferredItem<Item> CHAMOIS_LEATHER = ITEMS.registerSimpleItem("chamois_leather");
    /** Small and used as it comes off (like the squirrel's): fur hoods and boots. */
    public static final DeferredItem<Item> MARMOT_PELT = ITEMS.registerSimpleItem("marmot_pelt");
    public static final DeferredItem<Item> RAW_SAIGA_PELT = ITEMS.registerSimpleItem("raw_saiga_pelt"), SAIGA_PELT = ITEMS.registerSimpleItem("saiga_pelt");
    public static final DeferredItem<Item> RAW_HYENA_PELT = ITEMS.registerSimpleItem("raw_hyena_pelt"), HYENA_PELT = ITEMS.registerSimpleItem("hyena_pelt");
    /** Cured on a tall Drying Rack, filling both blocks (DryingRackBlock.CAVE_BEAR_WET). */
    public static final DeferredItem<Item> RAW_CAVE_BEAR_PELT = ITEMS.registerSimpleItem("raw_cave_bear_pelt");
    public static final DeferredItem<Item> CAVE_BEAR_PELT = ITEMS.registerSimpleItem("cave_bear_pelt");

    // ---- special parts ----
    public static final DeferredItem<Item> IBEX_HORN = ITEMS.registerSimpleItem("ibex_horn");
    public static final DeferredItem<Item> CHAMOIS_HORN = ITEMS.registerSimpleItem("chamois_horn");
    public static final DeferredItem<Item> MARMOT_FAT = ITEMS.registerSimpleItem("marmot_fat");
    public static final DeferredItem<Item> SAIGA_HORN = ITEMS.registerSimpleItem("saiga_horn");
    public static final DeferredItem<Item> HYENA_FANG = ITEMS.registerSimpleItem("hyena_fang");

    // ---- made from them ----
    /** Marmot fat worked with yarrow-like herbs: rubbed on, it eases aches (Regeneration I, 15 s). */
    public static final DeferredItem<Item> MARMOT_SALVE = ITEMS.registerSimpleItem("marmot_salve", p -> p.stacksTo(16)
            .component(net.minecraft.core.component.DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2.0F)
                    .animation(ItemUseAnimation.BRUSH).sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.HONEYCOMB_WAX_ON))
                    .hasConsumeParticles(false)
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0))).build()));
    /** Composite bow: ibex horn glued over the Sinew Bow's belly. Harder-hitting arrows, long-lived. */
    public static final DeferredItem<HornBowItem> HORN_BOW = ITEMS.registerItem("horn_bow", HornBowItem::new, p -> p.durability(384).enchantable(1));
    /** Soft fat-tanned boots: the chamois' sure foot (+3 safe fall distance). */
    public static final ArmorMaterial CHAMOIS_LEATHER_MATERIAL = new ArmorMaterial(9,
            Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 2, ArmorType.LEGGINGS, 2, ArmorType.BOOTS, 1, ArmorType.BODY, 2),
            14, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "repairs_chamois_leather_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chamois_leather")));
    public static final DeferredItem<Item> CHAMOIS_BOOTS = ITEMS.registerSimpleItem("chamois_boots", p -> p
            .humanoidArmor(CHAMOIS_LEATHER_MATERIAL, ArmorType.BOOTS)
            .attributes(CHAMOIS_LEATHER_MATERIAL.createAttributes(ArmorType.BOOTS).withModifierAdded(Attributes.SAFE_FALL_DISTANCE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath(Mechanical.MODID, "chamois_boots_sure_foot"), 3.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.FEET)));

    // ---- blocks: rugs (flat, like the Bear Rug) and the cave bear's skull ----
    public static final DeferredBlock<BearRugBlock> IBEX_RUG = rug("ibex_rug", MapColor.COLOR_BROWN);
    public static final DeferredBlock<BearRugBlock> SAIGA_RUG = rug("saiga_rug", MapColor.SAND);
    public static final DeferredBlock<BearRugBlock> HYENA_RUG = rug("hyena_rug", MapColor.TERRACOTTA_YELLOW);
    public static final DeferredBlock<BearRugBlock> CAVE_BEAR_RUG = rug("cave_bear_rug", MapColor.TERRACOTTA_BROWN);
    public static final DeferredItem<BlockItem> IBEX_RUG_ITEM = ITEMS.registerSimpleBlockItem("ibex_rug", IBEX_RUG);
    public static final DeferredItem<BlockItem> SAIGA_RUG_ITEM = ITEMS.registerSimpleBlockItem("saiga_rug", SAIGA_RUG);
    public static final DeferredItem<BlockItem> HYENA_RUG_ITEM = ITEMS.registerSimpleBlockItem("hyena_rug", HYENA_RUG);
    public static final DeferredItem<BlockItem> CAVE_BEAR_RUG_ITEM = ITEMS.registerSimpleBlockItem("cave_bear_rug", CAVE_BEAR_RUG);
    /** Always comes out of a butchered cave bear: a trophy to put on a shelf or the floor. */
    public static final DeferredBlock<CaveBearSkullBlock> CAVE_BEAR_SKULL = BLOCKS.registerBlock("cave_bear_skull", CaveBearSkullBlock::new, p -> p
            .mapColor(MapColor.SAND).strength(1.0F).sound(SoundType.BONE_BLOCK).noOcclusion());
    public static final DeferredItem<BlockItem> CAVE_BEAR_SKULL_ITEM = ITEMS.registerSimpleBlockItem("cave_bear_skull", CAVE_BEAR_SKULL,
            p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON).stacksTo(16));

    /** Thin-scraped saiga horn set in a wooden lattice: a window that lets amber light in, long before glass. */
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> HORN_WINDOW = BLOCKS.registerBlock("horn_window",
            net.minecraft.world.level.block.TransparentBlock::new, p -> p.mapColor(MapColor.COLOR_ORANGE).strength(0.6F).sound(SoundType.WOOD)
                    .noOcclusion().isValidSpawn((s, l, pos, t) -> false).isRedstoneConductor((s, l, pos) -> false).isSuffocating((s, l, pos) -> false));
    public static final DeferredItem<BlockItem> HORN_WINDOW_ITEM = ITEMS.registerSimpleBlockItem("horn_window", HORN_WINDOW);

    public static final DeferredItem<SpawnEggItem> IBEX_SPAWN_EGG = egg("ibex_spawn_egg", IBEX);
    public static final DeferredItem<SpawnEggItem> CHAMOIS_SPAWN_EGG = egg("chamois_spawn_egg", CHAMOIS);
    public static final DeferredItem<SpawnEggItem> MARMOT_SPAWN_EGG = egg("marmot_spawn_egg", MARMOT);
    public static final DeferredItem<SpawnEggItem> SAIGA_SPAWN_EGG = egg("saiga_spawn_egg", SAIGA);
    public static final DeferredItem<SpawnEggItem> CAVE_BEAR_SPAWN_EGG = egg("cave_bear_spawn_egg", CAVE_BEAR);
    public static final DeferredItem<SpawnEggItem> CAVE_HYENA_SPAWN_EGG = egg("cave_hyena_spawn_egg", CAVE_HYENA);

    // ---- sounds (sounds.json: vanilla sounds pitched) ----
    public static final SoundEvent MARMOT_WHISTLE = SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(Mechanical.MODID, "entity.marmot.whistle"), 48.0F);
    public static final SoundEvent HYENA_LAUGH = SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(Mechanical.MODID, "entity.cave_hyena.laugh"), 48.0F);

    /** Where cave hyena packs roam at night (also their world spawns), and the night arrival chance per player each 30 s. */
    public static final TagKey<Biome> HYENA_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "has_cave_hyenas"));
    private static final float HYENA_CHANCE = 0.035F;
    /** No two cave bear dens closer than this. */
    private static final double DEN_SPACING = 48;

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static DeferredBlock<BearRugBlock> rug(String name, MapColor color) {
        return BLOCKS.registerBlock(name, BearRugBlock::new, p -> p.mapColor(color).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    }

    private static Extra x(java.util.function.Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Loads this class so its registrations happen, and registers the butchery yields; call from the mod constructor. */
    public static void init() {
        Butchery.register(() -> MountainFauna.IBEX.get(), new Yield(MountainFauna.IBEX_MEAT, 2, 3, List.of(x(MountainFauna.RAW_IBEX_PELT, 0.6F, 1, 1)),
                List.of(x(MountainFauna.IBEX_HORN, 0.5F, 1, 2), x(Wildlife.SINEW, 0.5F, 1, 2))));
        Butchery.register(() -> MountainFauna.CHAMOIS.get(), new Yield(MountainFauna.CHAMOIS_MEAT, 1, 2, List.of(x(MountainFauna.RAW_CHAMOIS_HIDE, 0.6F, 1, 1)),
                List.of(x(MountainFauna.CHAMOIS_HORN, 0.7F, 1, 2), x(Wildlife.SINEW, 0.4F, 1, 1))));
        Butchery.register(() -> MountainFauna.MARMOT.get(), new Yield(MountainFauna.MARMOT_MEAT, 1, 1, List.of(x(MountainFauna.MARMOT_PELT, 0.7F, 1, 1)),
                List.of(x(MountainFauna.MARMOT_FAT, 0.8F, 1, 2))));
        // horns only on bucks (35 %): the chance stands in for the sex (shortcut: per type, not per animal)
        Butchery.register(() -> MountainFauna.SAIGA.get(), new Yield(MountainFauna.SAIGA_MEAT, 1, 3, List.of(x(MountainFauna.RAW_SAIGA_PELT, 0.55F, 1, 1)),
                List.of(x(MountainFauna.SAIGA_HORN, 0.35F, 2, 2), x(Wildlife.SINEW, 0.4F, 1, 1))));
        Butchery.register(() -> MountainFauna.CAVE_BEAR.get(), new Yield(MountainFauna.CAVE_BEAR_MEAT, 5, 8, List.of(x(MountainFauna.RAW_CAVE_BEAR_PELT, 1.0F, 1, 1), x(Mechanical.RAW_HIDE, 0.4F, 1, 1)),
                List.of(x(() -> MountainFauna.CAVE_BEAR_SKULL.get().asItem(), 1.0F, 1, 1), x(Wildlife.BEAR_FAT, 0.9F, 2, 3), x(Wildlife.BEAR_CLAW, 0.6F, 1, 3),
                        x(Wildlife.SINEW, 0.7F, 1, 2), x(() -> Items.BONE, 0.7F, 2, 4))));
        Butchery.register(() -> MountainFauna.CAVE_HYENA.get(), new Yield(MountainFauna.HYENA_MEAT, 1, 2, List.of(x(MountainFauna.RAW_HYENA_PELT, 0.8F, 1, 1)),
                List.of(x(MountainFauna.HYENA_FANG, 0.6F, 1, 2), x(() -> Items.BONE, 0.6F, 1, 2), x(Wildlife.SINEW, 0.4F, 1, 1))));
        Butchery.toughHide(() -> MountainFauna.CAVE_BEAR.get());
    }

    @SubscribeEvent
    static void registerSounds(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, MARMOT_WHISTLE.location(), () -> MARMOT_WHISTLE);
        event.register(Registries.SOUND_EVENT, HYENA_LAUGH.location(), () -> HYENA_LAUGH);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(IBEX.get(), Ibex.createAttributes().build());
        event.put(CHAMOIS.get(), Chamois.createAttributes().build());
        event.put(MARMOT.get(), Marmot.createAttributes().build());
        event.put(SAIGA.get(), Saiga.createAttributes().build());
        event.put(CAVE_BEAR.get(), CaveBear.createAttributes().build());
        event.put(CAVE_HYENA.get(), CaveHyena.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        // rock, snow, gravel and grass (the goat's ground), in daylight
        event.register(IBEX.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Goat::checkGoatSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CHAMOIS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Goat::checkGoatSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MARMOT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Goat::checkGoatSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SAIGA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Goat::checkGoatSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // hyenas: grass, sand, red sand, terracotta, coarse dirt (badlands and savanna ground)
        event.register(CAVE_HYENA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> {
                    BlockState below = level.getBlockState(pos.below());
                    return below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.SAND) || below.is(BlockTags.TERRACOTTA) || below.is(BlockTags.DIRT);
                }, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // cave bears: only in the dark under rock, out of the sky (a den), and never near another one
        event.register(CAVE_BEAR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> reason != EntitySpawnReason.CHUNK_GENERATION && isDen(level, pos)
                        && level.getEntitiesOfClass(CaveBear.class, new AABB(pos).inflate(DEN_SPACING)).isEmpty(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** No sky, little light, rock or earth underfoot, above the deep dark. Reads only this column. */
    static boolean isDen(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return pos.getY() > 0 && !level.canSeeSky(pos) && level.getBrightness(LightLayer.SKY, pos) == 0 && level.getRawBrightness(pos, 0) <= 7
                && (below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.DIRT) || below.is(net.minecraft.world.level.block.Blocks.GRAVEL));
    }

    /**
     * Every 30 s at night (like Predators): each player in hyena country has a {@link #HYENA_CHANCE} (doubled on a blood
     * moon or with a carcass nearby - they smell it) to have a pack of 3-5 arrive 48-64 blocks away, half of them hunting.
     */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 600 != 300
                || !level.isDarkOutside() || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !level.getBiome(player.blockPosition()).is(HYENA_BIOMES)) continue;
            float f = (BloodMoon.isActive(level) ? 2 : 1)
                    * (level.getEntitiesOfClass(com.mechanical.CarcassEntity.class, player.getBoundingBox().inflate(16)).isEmpty() ? 1 : 2);
            if (level.getRandom().nextFloat() < HYENA_CHANCE * f)
                Predators.spawnGroup(level, player.position(), player, CAVE_HYENA.get(), 3 + level.getRandom().nextInt(3));
        }
    }

    private MountainFauna() {}
}
