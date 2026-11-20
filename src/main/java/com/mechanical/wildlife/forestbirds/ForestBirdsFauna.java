package com.mechanical.wildlife.forestbirds;

import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mechanical.Birds;
import com.mechanical.Butchery;
import com.mechanical.Mechanical;
import com.mechanical.Sanity;
import com.mechanical.SpecialArrowItem;
import com.mechanical.wildlife.FlockBird;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Forest and mountain birds: Capercaillie and Great Bustard (edible ground birds: carcass, plucked, own meat and nests with
 * their own eggs), Owl, Raven, Golden Eagle and Vulture (no carcass: like the crow they drop feathers and their own parts;
 * nobody hunted them for meat, and a carrion eater's flesh is not food). All are {@link FlockBird}s.
 * Data: scripts/gen_fauna_forestbirds.py, textures and model geometry: scripts/gen_fauna_forestbirds_textures.py,
 * docs: docs/fauna/forestbirds.md.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ForestBirdsFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<Capercaillie>> CAPERCAILLIE = ENTITY_TYPES.registerEntityType(
            "capercaillie", Capercaillie::new, MobCategory.CREATURE, b -> b.sized(0.7F, 0.9F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Bustard>> BUSTARD = ENTITY_TYPES.registerEntityType(
            "great_bustard", Bustard::new, MobCategory.CREATURE, b -> b.sized(0.8F, 1.1F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Owl>> OWL = ENTITY_TYPES.registerEntityType(
            "owl", Owl::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.8F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Raven>> RAVEN = ENTITY_TYPES.registerEntityType(
            "raven", Raven::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.6F).clientTrackingRange(10));
    /** Carries snatched game under it (the carcass rides as its passenger). */
    public static final DeferredHolder<EntityType<?>, EntityType<GoldenEagle>> GOLDEN_EAGLE = ENTITY_TYPES.registerEntityType(
            "golden_eagle", GoldenEagle::new, MobCategory.CREATURE, b -> b.sized(0.7F, 0.9F).passengerAttachments(new Vec3(0, -0.55, 0)).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<Vulture>> VULTURE = ENTITY_TYPES.registerEntityType(
            "vulture", Vulture::new, MobCategory.CREATURE, b -> b.sized(0.8F, 1.0F).clientTrackingRange(16));

    // ---- capercaillie and bustard: meat, the whole plucked bird, eggs; the cock's broad tail feathers
    public static final DeferredItem<Item> RAW_CAPERCAILLIE = raw("raw_capercaillie", 3);
    public static final DeferredItem<Item> COOKED_CAPERCAILLIE = food("cooked_capercaillie", 7, 0.8F);
    public static final DeferredItem<Item> RAW_BUSTARD = raw("raw_bustard", 3);
    public static final DeferredItem<Item> COOKED_BUSTARD = food("cooked_bustard", 8, 0.8F);
    public static final DeferredItem<Item> PLUCKED_CAPERCAILLIE = ITEMS.registerSimpleItem("plucked_capercaillie", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_BUSTARD = ITEMS.registerSimpleItem("plucked_bustard", p -> p.stacksTo(16));
    public static final DeferredItem<Item> CAPERCAILLIE_EGG = ITEMS.registerSimpleItem("capercaillie_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> BUSTARD_EGG = ITEMS.registerSimpleItem("bustard_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> CAPERCAILLIE_TAIL_FEATHER = ITEMS.registerSimpleItem("capercaillie_tail_feather");
    // ---- owl, raven, eagle, vulture: feathers and parts
    public static final DeferredItem<Item> OWL_FEATHER = ITEMS.registerSimpleItem("owl_feather");
    /** Coughed up under an owl's day roost: fur and tiny bones, picked apart into bone meal. */
    public static final DeferredItem<Item> OWL_PELLET = ITEMS.registerSimpleItem("owl_pellet");
    public static final DeferredItem<Item> RAVEN_FEATHER = ITEMS.registerSimpleItem("raven_feather");
    /** Carried anywhere in the inventory: darkness wears the mind down more slowly (see {@link #onPlayerTick}). */
    public static final DeferredItem<Item> RAVEN_TALISMAN = ITEMS.registerSimpleItem("raven_talisman", p -> lore(p.stacksTo(1), "raven_talisman"));
    public static final DeferredItem<Item> EAGLE_FEATHER = ITEMS.registerSimpleItem("eagle_feather");
    public static final DeferredItem<Item> EAGLE_TALON = ITEMS.registerSimpleItem("eagle_talon");
    public static final DeferredItem<Item> VULTURE_WING_BONE = ITEMS.registerSimpleItem("vulture_wing_bone");
    // the Bone Flute is WaterfowlFauna.BONE_FLUTE; the vulture wing bone is a second recipe for it
    // ---- arrows (Fletching Table): the eagle's stiff feathers hit hardest, the owl's soft fringed ones land without a sound
    public static final DeferredItem<SpecialArrowItem> EAGLE_FLETCHED_ARROW = ITEMS.registerItem("eagle_fletched_arrow", p -> new SpecialArrowItem(p, 2.75, false));
    public static final DeferredItem<SpecialArrowItem> SILENT_ARROW = ITEMS.registerItem("silent_arrow", p -> new SpecialArrowItem(p, 2.0, false));

    public static final DeferredItem<SpawnEggItem> CAPERCAILLIE_SPAWN_EGG = egg("capercaillie_spawn_egg", CAPERCAILLIE);
    public static final DeferredItem<SpawnEggItem> BUSTARD_SPAWN_EGG = egg("great_bustard_spawn_egg", BUSTARD);
    public static final DeferredItem<SpawnEggItem> OWL_SPAWN_EGG = egg("owl_spawn_egg", OWL);
    public static final DeferredItem<SpawnEggItem> RAVEN_SPAWN_EGG = egg("raven_spawn_egg", RAVEN);
    public static final DeferredItem<SpawnEggItem> GOLDEN_EAGLE_SPAWN_EGG = egg("golden_eagle_spawn_egg", GOLDEN_EAGLE);
    public static final DeferredItem<SpawnEggItem> VULTURE_SPAWN_EGG = egg("vulture_spawn_egg", VULTURE);

    /** Small animals the owl takes at night / the eagle dives on (entity type tags, so other groups' rodents can join). */
    public static final TagKey<EntityType<?>> OWL_PREY = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "owl_prey"));
    public static final TagKey<EntityType<?>> EAGLE_PREY = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "eagle_prey"));
    /** What a raven grabs off the ground by a carcass. */
    public static final TagKey<Item> RAVEN_SCRAPS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "raven_scraps"));

    /** Share of the sanity lost in the dark that a Raven Talisman gives back. */
    static final float TALISMAN_REFUND = 0.4F;

    /** A grey tooltip line saying what it does (lang item.mechanical.<name>.lore). */
    private static Item.Properties lore(Item.Properties p, String name) {
        return p.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                net.minecraft.network.chat.Component.translatable("item.mechanical." + name + ".lore").withStyle(net.minecraft.ChatFormatting.GRAY))));
    }

    private static DeferredItem<Item> raw(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build(), Consumables.CHICKEN));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    /** Registers everything (class load) and the carcass rules; call from the mod constructor. */
    public static void init() {
        Birds.registerEdible(CAPERCAILLIE::get, PLUCKED_CAPERCAILLIE);
        Birds.registerEdible(BUSTARD::get, PLUCKED_BUSTARD);
        // pluck first (feathers; the cock's tail fan), then the meat; a bustard is a big bird with a lot of meat on it
        Butchery.register(CAPERCAILLIE::get, new Butchery.Yield(RAW_CAPERCAILLIE, 1, 2,
                List.of(new Butchery.Extra(() -> Items.FEATHER, 1.0F, 2, 4), new Butchery.Extra(CAPERCAILLIE_TAIL_FEATHER, 0.6F, 1, 2)), List.of()));
        Butchery.register(BUSTARD::get, new Butchery.Yield(RAW_BUSTARD, 2, 3,
                List.of(new Butchery.Extra(() -> Items.FEATHER, 1.0F, 3, 5)), List.of()));
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(CAPERCAILLIE.get(), FlockBird.createAttributes(8.0).build());
        event.put(BUSTARD.get(), FlockBird.createAttributes(10.0).add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.26).build());
        event.put(OWL.get(), FlockBird.createAttributes(6.0).build());
        event.put(RAVEN.get(), FlockBird.createAttributes(5.0).build());
        event.put(GOLDEN_EAGLE.get(), FlockBird.createAttributes(10.0).build());
        event.put(VULTURE.get(), FlockBird.createAttributes(10.0).build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(CAPERCAILLIE.get(), BUSTARD.get(), OWL.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        // the open-sky birds: on rock, gravel, snow and terracotta too (mountains, badlands)
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(RAVEN.get(), GOLDEN_EAGLE.get(), VULTURE.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> openGround(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    /** Air here, solid ground (not leaves) below, open sky and daylight. Reads only this column (safe at chunk generation). */
    private static boolean openGround(LevelAccessor level, BlockPos pos) {
        var below = level.getBlockState(pos.below());
        return level.getBlockState(pos).isAir() && below.isSolid() && !below.is(BlockTags.LEAVES) && level.canSeeSky(pos)
                && level.getRawBrightness(pos, 0) > 8;
    }

    // ---- Raven Talisman: refund part of the sanity the dark took this second
    private static final Map<Player, Float> LAST_SANITY = new WeakHashMap<>();

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 15) return;
        float now = Sanity.of(player);
        Float last = LAST_SANITY.put(player, now);
        if (last == null || now >= last || last - now > 2.0F) return; // only the slow drain of darkness, not a sudden scare
        if (player.getInventory().contains(s -> s.is(RAVEN_TALISMAN.get()))) {
            float refunded = now + (last - now) * TALISMAN_REFUND;
            Sanity.set(player, refunded);
            LAST_SANITY.put(player, refunded);
        }
    }

    /** True at dawn (the capercaillie lek), by the overworld clock. */
    static boolean dawn(net.minecraft.world.level.Level level) {
        long t = level.getOverworldClockTime() % 24000;
        return t >= 22500 || t < 3000;
    }

    private ForestBirdsFauna() {}
}
