package com.mechanical.folklore;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.Set;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The folklore beings of docs/fauna-expansion-plan.md (docs/fauna/folklore.md): Huldra, Will-o'-the-wisp and Jungle Spirit
 * Mask, Tomte, Näcken, Bog Body, Frost Giant, Dune Wraith, the sleeping forest troll (Mossy Troll Stone) and Bergtagen.
 * Registrations here; the beings in their own classes. Data: scripts/gen_folklore.py, textures: gen_folklore_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Beings {
    // ---- effects
    /** Led astray: the camera drifts slowly off course (client.folklore.BeingsClient). */
    public static final DeferredHolder<MobEffect, MobEffect> LOST = Folklore.MOB_EFFECTS.register("lost",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x5A6B3A) {});
    /** A guest of the Vittra (Bergtagen): no vittra hears you while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> MOUNTAIN_GUEST = Folklore.MOB_EFFECTS.register("mountain_guest",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8A8F9A) {});
    /** Näcken's lesson: +2 Luck for three days. */
    public static final DeferredHolder<MobEffect, MobEffect> FIDDLERS_BLESSING = Folklore.MOB_EFFECTS.register("fiddlers_blessing",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x3FA7C9) {}.addAttributeModifier(Attributes.LUCK,
                    Identifier.fromNamespaceAndPath(Mechanical.MODID, "effect.fiddlers_blessing"), 2.0, AttributeModifier.Operation.ADD_VALUE));

    // ---- beings
    public static final DeferredHolder<EntityType<?>, EntityType<Huldra>> HULDRA = ENTITY_TYPES.registerEntityType(
            "huldra", Huldra::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.9F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Wisp>> WILL_O_WISP = ENTITY_TYPES.registerEntityType(
            "will_o_wisp", Wisp::new, MobCategory.MONSTER, b -> b.sized(0.4F, 0.4F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<Wisp>> JUNGLE_SPIRIT_MASK = ENTITY_TYPES.registerEntityType(
            "jungle_spirit_mask", Wisp::new, MobCategory.MONSTER, b -> b.sized(0.5F, 0.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Tomte>> TOMTE = ENTITY_TYPES.registerEntityType(
            "tomte", Tomte::new, MobCategory.CREATURE, b -> b.sized(0.5F, 1.0F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Nacken>> NACKEN = ENTITY_TYPES.registerEntityType(
            "nacken", Nacken::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.9F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<BogBody>> BOG_BODY = ENTITY_TYPES.registerEntityType(
            "bog_body", BogBody::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostGiant>> FROST_GIANT = ENTITY_TYPES.registerEntityType(
            "frost_giant", FrostGiant::new, MobCategory.MONSTER, b -> b.sized(1.6F, 5.0F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<DuneWraith>> DUNE_WRAITH = ENTITY_TYPES.registerEntityType(
            "dune_wraith", DuneWraith::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.9F).clientTrackingRange(10));

    public static final DeferredItem<SpawnEggItem> HULDRA_SPAWN_EGG = egg("huldra", HULDRA);
    public static final DeferredItem<SpawnEggItem> WILL_O_WISP_SPAWN_EGG = egg("will_o_wisp", WILL_O_WISP);
    public static final DeferredItem<SpawnEggItem> JUNGLE_SPIRIT_MASK_SPAWN_EGG = egg("jungle_spirit_mask", JUNGLE_SPIRIT_MASK);
    public static final DeferredItem<SpawnEggItem> TOMTE_SPAWN_EGG = egg("tomte", TOMTE);
    public static final DeferredItem<SpawnEggItem> NACKEN_SPAWN_EGG = egg("nacken", NACKEN);
    public static final DeferredItem<SpawnEggItem> BOG_BODY_SPAWN_EGG = egg("bog_body", BOG_BODY);
    public static final DeferredItem<SpawnEggItem> FROST_GIANT_SPAWN_EGG = egg("frost_giant", FROST_GIANT);
    public static final DeferredItem<SpawnEggItem> DUNE_WRAITH_SPAWN_EGG = egg("dune_wraith", DUNE_WRAITH);

    // ---- gifts and trophies
    /** Huldra's gift, taken from her hand: a trophy (the luck is the effect). */
    public static final DeferredItem<Item> HULDRA_COMB = trophy("huldra_comb", Rarity.RARE);
    /** Lies in a wisp's hoard: a cold, faintly glowing ember. Trophy. */
    public static final DeferredItem<Item> WISP_EMBER = trophy("wisp_ember", Rarity.RARE);
    /** Now and then left where a Jungle Spirit Mask stops. Worn on the head: +{@link Faith#MASK_SIGHT} sight. */
    public static final DeferredItem<Item> SPIRIT_MASK = ITEMS.registerSimpleItem("spirit_mask", p -> p.stacksTo(1).rarity(Rarity.RARE)
            .equippable(EquipmentSlot.HEAD));
    /** A pleased Tomte's thanks for the Yule porridge, once a year. */
    public static final DeferredItem<Item> STRAW_YULE_GOAT = trophy("straw_yule_goat", Rarity.UNCOMMON);
    /** Näcken's string, given with his lesson. Trophy. */
    public static final DeferredItem<Item> NACKEN_STRING = trophy("nacken_string", Rarity.RARE);
    /** From a Bog Body: peat-tanned skin, cut into Leather. */
    public static final DeferredItem<Item> PEAT_TANNED_HIDE = ITEMS.registerSimpleItem("peat_tanned_hide");
    /** Sometimes round a Bog Body's neck: a twisted bronze ring. Trophy. */
    public static final DeferredItem<Item> BOG_TORC = trophy("bog_torc", Rarity.UNCOMMON);
    /** From a Vittra's chest in the mountain (Bergtagen). Trophy. */
    public static final DeferredItem<Item> VITTRA_SILVER = ITEMS.registerSimpleItem("vittra_silver", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    /** Left when you strike a Dune Wraith: sand fused to glass. Trophy. */
    public static final DeferredItem<Item> DUNE_GLASS = trophy("dune_glass", Rarity.UNCOMMON);

    // ---- blocks
    /** The Frost Giant's heart: placed, ice within {@link #FROST_HEART_RANGE} blocks never thaws. */
    public static final DeferredBlock<Block> FROST_HEART = BLOCKS.registerSimpleBlock("frost_heart", p -> p.mapColor(MapColor.ICE)
            .strength(1.5F).sound(SoundType.GLASS).lightLevel(s -> 6).friction(0.98F));
    public static final DeferredItem<BlockItem> FROST_HEART_ITEM = ITEMS.registerSimpleBlockItem("frost_heart", FROST_HEART, p -> p.rarity(Rarity.EPIC));
    /** A sleeping forest troll: two of them stacked in the taiga. Chopping nearby wakes it (TrollStone). */
    public static final DeferredBlock<Block> MOSSY_TROLL_STONE = BLOCKS.registerSimpleBlock("mossy_troll_stone", p -> p.mapColor(MapColor.STONE)
            .strength(3.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> MOSSY_TROLL_STONE_ITEM = ITEMS.registerSimpleBlockItem("mossy_troll_stone", MOSSY_TROLL_STONE);

    static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Mechanical.MODID);
    /** The Frost Heart as a point of interest: thawing asks the POI manager instead of scanning blocks. */
    public static final DeferredHolder<PoiType, PoiType> FROST_HEART_POI = POI_TYPES.register("frost_heart",
            () -> new PoiType(Set.copyOf(FROST_HEART.get().getStateDefinition().getPossibleStates()), 0, 1));

    /** What Näcken takes as a gift: coins and meat. */
    public static final TagKey<Item> NACKEN_GIFTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "nacken_gifts"));

    public static final int FROST_HEART_RANGE = 8;

    /** A Frost Heart within {@link #FROST_HEART_RANGE} blocks: the ice at pos stays frozen (Thaw.melt). */
    public static boolean frostHeartNear(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return level.getPoiManager().getInRange(h -> h.is(FROST_HEART_POI), pos, FROST_HEART_RANGE,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).findAny().isPresent();
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> type) {
        return ITEMS.registerItem(name + "_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static DeferredItem<Item> trophy(String name, Rarity rarity) {
        return ITEMS.registerSimpleItem(name, p -> p.stacksTo(16).rarity(rarity));
    }

    static void init(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
        Tomte.selfCheck();
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        for (var t : java.util.List.of(HULDRA, WILL_O_WISP, JUNGLE_SPIRIT_MASK, TOMTE, NACKEN, DUNE_WRAITH))
            event.put(t.get(), Spirit.createAttributes().build());
        event.put(BOG_BODY.get(), BogBody.createAttributes().build());
        event.put(FROST_GIANT.get(), FrostGiant.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        var replace = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        var ground = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        event.register(HULDRA.get(), SpawnPlacementTypes.ON_GROUND, ground, (t, l, r, pos, rnd) -> Huldra.canSpawn(l, pos, rnd), replace);
        event.register(WILL_O_WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, ground, (t, l, r, pos, rnd) -> Wisp.canSpawn(l, pos, rnd), replace);
        event.register(JUNGLE_SPIRIT_MASK.get(), SpawnPlacementTypes.NO_RESTRICTIONS, ground, (t, l, r, pos, rnd) -> Wisp.canSpawn(l, pos, rnd), replace);
        event.register(NACKEN.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, (t, l, r, pos, rnd) -> Nacken.canSpawn(l, pos, rnd), replace);
        event.register(BOG_BODY.get(), SpawnPlacementTypes.ON_GROUND, ground, (t, l, r, pos, rnd) -> BogBody.canSpawn(l, pos, rnd), replace);
        event.register(FROST_GIANT.get(), SpawnPlacementTypes.ON_GROUND, ground, (t, l, r, pos, rnd) -> FrostGiant.canSpawn(l, pos, rnd), replace);
        event.register(DUNE_WRAITH.get(), SpawnPlacementTypes.ON_GROUND, ground, (t, l, r, pos, rnd) -> DuneWraith.canSpawn(l, pos, rnd), replace);
    }

    private Beings() {}
}
