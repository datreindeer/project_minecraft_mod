package com.mechanical.wildlife.warm;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;

import com.mechanical.BearRugBlock;
import com.mechanical.Birds;
import com.mechanical.Butchery;
import com.mechanical.Carting;
import com.mechanical.Butchery.Extra;
import com.mechanical.Butchery.Yield;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.Predators;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Warm-country fauna: deserts, badlands, savanna, jungle and swamp. Onager (tamed into a pack animal), Tortoise, Desert
 * Lizard, Water Buffalo, Tapir, Leopard and three jungle birds (Hornbill, Jungle Fowl, Peafowl). Entity types, the items
 * they give, spawn eggs, attributes, spawn rules, butchering yields and the leopard's night arrivals. Spawns are in data
 * (biome modifiers), data and lang: scripts/gen_fauna_warm.py, textures: scripts/gen_fauna_warm_textures.py,
 * docs: docs/fauna/warm.md.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class WarmFauna {
    // ---- animals ----
    public static final DeferredHolder<EntityType<?>, EntityType<Onager>> ONAGER = ENTITY_TYPES.registerEntityType(
            "onager", Onager::new, MobCategory.CREATURE, b -> b.sized(1.1F, 1.45F).clientTrackingRange(10));
    /** A tamed onager: a vanilla donkey underneath (riding, pack, cart, breeding) with the onager's looks. */
    public static final DeferredHolder<EntityType<?>, EntityType<TameOnager>> TAME_ONAGER = ENTITY_TYPES.registerEntityType(
            "tame_onager", TameOnager::new, MobCategory.CREATURE, b -> b.sized(1.1F, 1.45F).eyeHeight(1.3F).passengerAttachments(1.2F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Tortoise>> TORTOISE = ENTITY_TYPES.registerEntityType(
            "tortoise", Tortoise::new, MobCategory.CREATURE, b -> b.sized(0.8F, 0.65F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<DesertLizard>> DESERT_LIZARD = ENTITY_TYPES.registerEntityType(
            "desert_lizard", DesertLizard::new, MobCategory.CREATURE, b -> b.sized(0.45F, 0.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<WaterBuffalo>> WATER_BUFFALO = ENTITY_TYPES.registerEntityType(
            "water_buffalo", WaterBuffalo::new, MobCategory.CREATURE, b -> b.sized(1.5F, 1.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Tapir>> TAPIR = ENTITY_TYPES.registerEntityType(
            "tapir", Tapir::new, MobCategory.CREATURE, b -> b.sized(1.1F, 1.1F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Leopard>> LEOPARD = ENTITY_TYPES.registerEntityType(
            "leopard", Leopard::new, MobCategory.CREATURE, b -> b.sized(0.9F, 0.9F).clientTrackingRange(10));
    // jungle birds: the hornbill flies in flocks and perches on the canopy; the fowl and peafowl are ground birds that flush
    public static final FlockBird.Kind HORNBILL_KIND = new FlockBird.Kind("hornbill", 0.4, 7, 16, 250, 600, 600, 1500, false, false, true, 1.1,
            () -> SoundEvents.GOAT_SCREAMING_AMBIENT, 1.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<FlockBird>> HORNBILL = ENTITY_TYPES.registerEntityType(
            "hornbill", (t, l) -> new FlockBird(t, l, HORNBILL_KIND), MobCategory.CREATURE, b -> b.sized(0.6F, 0.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<JungleFowl>> JUNGLE_FOWL = ENTITY_TYPES.registerEntityType(
            "jungle_fowl", JungleFowl::new, MobCategory.CREATURE, b -> b.sized(0.45F, 0.65F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Peafowl>> PEAFOWL = ENTITY_TYPES.registerEntityType(
            "peafowl", Peafowl::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.9F).clientTrackingRange(10));

    // ---- meat (scrap-sized like the other game; the birds' like Birds') ----
    public static final DeferredItem<Item> ONAGER_MEAT = food("onager_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_ONAGER_MEAT = food("cooked_onager_meat", 3, 0.7F);
    public static final DeferredItem<Item> TORTOISE_MEAT = food("tortoise_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_TORTOISE_MEAT = food("cooked_tortoise_meat", 3, 0.8F);
    /** The lizard's meat is its tail: also what it leaves behind when it escapes (DesertLizard). */
    public static final DeferredItem<Item> LIZARD_TAIL = food("lizard_tail", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_LIZARD_TAIL = food("cooked_lizard_tail", 2, 0.6F);
    public static final DeferredItem<Item> BUFFALO_MEAT = food("buffalo_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_BUFFALO_MEAT = food("cooked_buffalo_meat", 3, 0.8F);
    public static final DeferredItem<Item> TAPIR_MEAT = food("tapir_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_TAPIR_MEAT = food("cooked_tapir_meat", 3, 0.7F);
    public static final DeferredItem<Item> LEOPARD_MEAT = food("leopard_meat", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_LEOPARD_MEAT = food("cooked_leopard_meat", 3, 0.6F);
    public static final DeferredItem<Item> RAW_HORNBILL = rawBird("raw_hornbill", 2);
    public static final DeferredItem<Item> COOKED_HORNBILL = food("cooked_hornbill", 5, 0.7F);
    public static final DeferredItem<Item> RAW_JUNGLE_FOWL = rawBird("raw_jungle_fowl", 2);
    public static final DeferredItem<Item> COOKED_JUNGLE_FOWL = food("cooked_jungle_fowl", 6, 0.7F);
    public static final DeferredItem<Item> RAW_PEAFOWL = rawBird("raw_peafowl", 2);
    public static final DeferredItem<Item> COOKED_PEAFOWL = food("cooked_peafowl", 6, 0.7F);
    public static final DeferredItem<Item> PLUCKED_HORNBILL = ITEMS.registerSimpleItem("plucked_hornbill", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_JUNGLE_FOWL = ITEMS.registerSimpleItem("plucked_jungle_fowl", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_PEAFOWL = ITEMS.registerSimpleItem("plucked_peafowl", p -> p.stacksTo(16));

    // ---- hides and parts ----
    /** Raw hides scrape (knife, crafting grid) into Scraped Hides for the Drying Rack: onager 1, tapir 2, buffalo 3. */
    public static final DeferredItem<Item> RAW_ONAGER_HIDE = ITEMS.registerSimpleItem("raw_onager_hide");
    public static final DeferredItem<Item> RAW_TAPIR_HIDE = ITEMS.registerSimpleItem("raw_tapir_hide");
    public static final DeferredItem<Item> RAW_BUFFALO_HIDE = ITEMS.registerSimpleItem("raw_buffalo_hide");
    /** Fresh off the carcass; fat-tanned in the crafting grid (knife + #mechanical:tanning_fats) into a Leopard Pelt. */
    public static final DeferredItem<Item> RAW_LEOPARD_PELT = ITEMS.registerSimpleItem("raw_leopard_pelt");
    public static final DeferredItem<Item> LEOPARD_PELT = ITEMS.registerSimpleItem("leopard_pelt");
    public static final DeferredItem<Item> LIZARD_SKIN = ITEMS.registerSimpleItem("lizard_skin");
    public static final DeferredItem<Item> TORTOISE_SHELL = ITEMS.registerSimpleItem("tortoise_shell", p -> p.stacksTo(16));
    public static final DeferredItem<Item> BUFFALO_HORN = ITEMS.registerSimpleItem("buffalo_horn");
    public static final DeferredItem<Item> HORNBILL_CASQUE = ITEMS.registerSimpleItem("hornbill_casque");
    public static final DeferredItem<Item> PEACOCK_FEATHER = ITEMS.registerSimpleItem("peacock_feather");
    /** Laid on the ground by Jungle Fowl hens. Boils like the other eggs; thrown, it may hatch a chick: the chicken's wild ancestor. */
    public static final DeferredItem<EggItem> JUNGLE_FOWL_EGG = ITEMS.registerItem("jungle_fowl_egg", EggItem::new, p -> p.stacksTo(16));

    // ---- made from them ----
    /** Put on a tamed onager (TameOnager): gives it the pack inventory a donkey's chest would. */
    public static final DeferredItem<Item> PACK_BASKETS = ITEMS.registerSimpleItem("pack_baskets", p -> p.stacksTo(1));
    /** Held in either hand it keeps predators off like a torch (#mechanical:scares_predators). */
    public static final DeferredItem<Item> SHELL_RATTLE = ITEMS.registerSimpleItem("shell_rattle", p -> p.stacksTo(1));
    public static final DeferredBlock<BearRugBlock> LEOPARD_RUG = BLOCKS.registerBlock("leopard_rug", BearRugBlock::new, p -> p
            .mapColor(MapColor.COLOR_YELLOW).strength(0.2F).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> LEOPARD_RUG_ITEM = ITEMS.registerSimpleBlockItem("leopard_rug", LEOPARD_RUG);

    public static final DeferredItem<SpawnEggItem> ONAGER_SPAWN_EGG = egg("onager_spawn_egg", ONAGER);
    public static final DeferredItem<SpawnEggItem> TORTOISE_SPAWN_EGG = egg("tortoise_spawn_egg", TORTOISE);
    public static final DeferredItem<SpawnEggItem> DESERT_LIZARD_SPAWN_EGG = egg("desert_lizard_spawn_egg", DESERT_LIZARD);
    public static final DeferredItem<SpawnEggItem> WATER_BUFFALO_SPAWN_EGG = egg("water_buffalo_spawn_egg", WATER_BUFFALO);
    public static final DeferredItem<SpawnEggItem> TAPIR_SPAWN_EGG = egg("tapir_spawn_egg", TAPIR);
    public static final DeferredItem<SpawnEggItem> LEOPARD_SPAWN_EGG = egg("leopard_spawn_egg", LEOPARD);
    public static final DeferredItem<SpawnEggItem> HORNBILL_SPAWN_EGG = egg("hornbill_spawn_egg", HORNBILL);
    public static final DeferredItem<SpawnEggItem> JUNGLE_FOWL_SPAWN_EGG = egg("jungle_fowl_spawn_egg", JUNGLE_FOWL);
    public static final DeferredItem<SpawnEggItem> PEAFOWL_SPAWN_EGG = egg("peafowl_spawn_egg", PEAFOWL);

    /** Where leopards turn up at night (like the cave lion's country, Predators). */
    public static final TagKey<Biome> LEOPARD_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "has_leopards"));
    private static final float LEOPARD_CHANCE = 0.02F;

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Raw bird meat: like vanilla raw chicken, a chance of Hunger when eaten raw. */
    private static DeferredItem<Item> rawBird(String name, int nutrition) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.3F).build(), Consumables.CHICKEN));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static Extra x(java.util.function.Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Registers everything (class load) and the butchering yields; call from the mod constructor. */
    public static void init() {
        Butchery.register(ONAGER::get, new Yield(ONAGER_MEAT, 2, 4, List.of(x(RAW_ONAGER_HIDE, 0.6F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.5F, 1, 2))));
        // the shell comes off first (it is the outer pass); a little meat inside
        Butchery.register(TORTOISE::get, new Yield(TORTOISE_MEAT, 1, 2, List.of(x(TORTOISE_SHELL, 1.0F, 1, 1)), List.of()));
        Butchery.register(DESERT_LIZARD::get, new Yield(LIZARD_TAIL, 1, 1, List.of(x(LIZARD_SKIN, 0.6F, 1, 1)), List.of()));
        Butchery.register(WATER_BUFFALO::get, new Yield(BUFFALO_MEAT, 3, 6, List.of(x(RAW_BUFFALO_HIDE, 0.8F, 1, 1)),
                List.of(x(BUFFALO_HORN, 0.6F, 1, 2), x(Wildlife.SINEW, 0.5F, 1, 2), x(Wildlife.LARD, 0.4F, 1, 2))));
        Butchery.toughHide(WATER_BUFFALO::get); // a thick hide: tusk-grade knife or better
        Butchery.register(TAPIR::get, new Yield(TAPIR_MEAT, 2, 4, List.of(x(RAW_TAPIR_HIDE, 0.6F, 1, 1)),
                List.of(x(Wildlife.LARD, 0.5F, 1, 2), x(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.register(LEOPARD::get, new Yield(LEOPARD_MEAT, 1, 3, List.of(x(RAW_LEOPARD_PELT, 1.0F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.5F, 1, 1), x(() -> Items.BONE, 0.6F, 1, 2))));
        // birds: plucked first (the hornbill's casque comes off with the feathers), then their own meat
        Butchery.register(HORNBILL::get, new Yield(RAW_HORNBILL, 1, 1,
                List.of(x(() -> Items.FEATHER, 1.0F, 2, 4), x(HORNBILL_CASQUE, 1.0F, 1, 1)), List.of()));
        Butchery.register(JUNGLE_FOWL::get, new Yield(RAW_JUNGLE_FOWL, 1, 1, List.of(x(() -> Items.FEATHER, 1.0F, 1, 3)), List.of()));
        Butchery.register(PEAFOWL::get, new Yield(RAW_PEAFOWL, 1, 1,
                List.of(x(() -> Items.FEATHER, 1.0F, 2, 3), x(PEACOCK_FEATHER, 0.6F, 1, 3)), List.of()));
        Birds.registerEdible(HORNBILL::get, PLUCKED_HORNBILL);
        Birds.registerEdible(JUNGLE_FOWL::get, PLUCKED_JUNGLE_FOWL);
        Birds.registerEdible(PEAFOWL::get, PLUCKED_PEAFOWL);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(ONAGER.get(), Onager.createAttributes().build());
        event.put(TAME_ONAGER.get(), TameOnager.createAttributes().build());
        event.put(TORTOISE.get(), Tortoise.createAttributes().build());
        event.put(DESERT_LIZARD.get(), DesertLizard.createAttributes().build());
        event.put(WATER_BUFFALO.get(), WaterBuffalo.createAttributes().build());
        event.put(TAPIR.get(), Tapir.createAttributes().build());
        event.put(LEOPARD.get(), Leopard.createAttributes().build());
        event.put(HORNBILL.get(), FlockBird.createAttributes(8.0).build());
        event.put(JUNGLE_FOWL.get(), JungleFowl.createAttributes().build());
        event.put(PEAFOWL.get(), Peafowl.createAttributes().build());
    }

    /** Dry country: sand, red sand, terracotta, coarse dirt and grass, in daylight. Reads only this column. */
    private static boolean dryGround(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.SAND) || below.is(BlockTags.TERRACOTTA) || below.is(Blocks.COARSE_DIRT) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON))
                && level.getRawBrightness(pos, 0) > 8;
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(ONAGER.get(), TORTOISE.get(), DESERT_LIZARD.get())) {
            dry(event, type);
        }
        // jungle and swamp ground: grass, podzol, moss, mud
        for (EntityType<? extends Animal> type : List.<EntityType<? extends Animal>>of(WATER_BUFFALO.get(), TAPIR.get(), JUNGLE_FOWL.get(),
                PEAFOWL.get(), HORNBILL.get(), LEOPARD.get())) {
            wet(event, type);
        }
    }

    private static <T extends Animal> void dry(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> dryGround(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Animal> void wet(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> {
                    BlockState below = level.getBlockState(pos.below());
                    return (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(Blocks.MUD) || below.is(Blocks.MOSS_BLOCK)
                            || below.is(Blocks.PODZOL) || below.is(BlockTags.SAND)) && level.getRawBrightness(pos, 0) > 8;
                }, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /**
     * The Rope Halter (Carting) only knows the wild horse: on an onager it goes through {@link Onager#halter}, and on a
     * tame horse or donkey it leads like a Lead (its own right-click would mount it first, before the item is asked).
     */
    @SubscribeEvent
    static void onHalter(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(Carting.ROPE_HALTER.get())) return;
        if (event.getTarget() instanceof Onager onager && !onager.isBaby()) {
            event.setCancellationResult(onager.halter(event.getEntity(), event.getItemStack()));
            event.setCanceled(true);
        } else if (event.getTarget() instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse && !horse.isBaby()
                && !horse.isLeashed() && !horse.isVehicle() && horse.canBeLeashed()) {
            if (!event.getLevel().isClientSide()) {
                horse.setLeashedTo(event.getEntity(), true);
                event.getItemStack().consume(1, event.getEntity());
                horse.playSound(net.minecraft.sounds.SoundEvents.LEAD_TIED, 1.0F, 1.0F);
            }
            event.setCancellationResult(event.getLevel().isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.SUCCESS_SERVER);
            event.setCanceled(true);
        }
    }

    /** Leopard country gets a leopard now and then at night, like the cave lion's (Predators.spawnGroup). */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 600 != 300
                || !level.isDarkOutside() || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !level.getBiome(player.blockPosition()).is(LEOPARD_BIOMES)) continue;
            if (level.getRandom().nextFloat() < LEOPARD_CHANCE
                    && level.getEntitiesOfClass(Leopard.class, player.getBoundingBox().inflate(96)).isEmpty()) {
                Predators.spawnGroup(level, player.position(), player, LEOPARD.get(), 1);
            }
        }
    }

    private WarmFauna() {}
}
