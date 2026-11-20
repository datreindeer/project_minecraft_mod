package com.mechanical.wildlife.waterfowl;

import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.mechanical.Birds;
import com.mechanical.Butchery;
import com.mechanical.Mechanical;
import com.mechanical.Tinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Waterfowl: Mallard, Mute Swan, Grey Heron and Common Crane. Entity types, their meat, plucked birds, eggs and parts
 * (Down, Swan Quill, Heron Plume, Heron Bill, Hollow Bird Bone) and what is made from them (Bone Flute, Plume Headdress),
 * attributes, spawn rules, carcass yields (Butchery) and the plucked-bird hook (Birds). Nests: BirdNestBlock.
 * Data: scripts/gen_fauna_waterfowl.py; textures and models: scripts/gen_fauna_waterfowl_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class WaterfowlFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<Mallard>> MALLARD = ENTITY_TYPES.registerEntityType(
            "mallard", Mallard::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.6F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MuteSwan>> MUTE_SWAN = ENTITY_TYPES.registerEntityType(
            "mute_swan", MuteSwan::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.25F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<GreyHeron>> GREY_HERON = ENTITY_TYPES.registerEntityType(
            "grey_heron", GreyHeron::new, MobCategory.CREATURE, b -> b.sized(0.6F, 1.5F).eyeHeight(1.35F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<CommonCrane>> COMMON_CRANE = ENTITY_TYPES.registerEntityType(
            "common_crane", CommonCrane::new, MobCategory.CREATURE, b -> b.sized(0.7F, 1.75F).eyeHeight(1.6F).clientTrackingRange(12));

    // meat: raw (chance of Hunger, like raw chicken) and cooked
    public static final DeferredItem<Item> RAW_MALLARD = raw("raw_mallard", 2);
    public static final DeferredItem<Item> COOKED_MALLARD = food("cooked_mallard", 6, 0.7F);
    public static final DeferredItem<Item> RAW_SWAN = raw("raw_swan", 3);
    public static final DeferredItem<Item> COOKED_SWAN = food("cooked_swan", 8, 0.8F);
    public static final DeferredItem<Item> RAW_HERON = raw("raw_heron", 2);
    public static final DeferredItem<Item> COOKED_HERON = food("cooked_heron", 5, 0.6F);
    public static final DeferredItem<Item> RAW_CRANE = raw("raw_crane", 3);
    public static final DeferredItem<Item> COOKED_CRANE = food("cooked_crane", 7, 0.8F);

    public static final DeferredItem<Item> PLUCKED_MALLARD = ITEMS.registerSimpleItem("plucked_mallard", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_SWAN = ITEMS.registerSimpleItem("plucked_swan", p -> p.stacksTo(8));
    public static final DeferredItem<Item> PLUCKED_HERON = ITEMS.registerSimpleItem("plucked_heron", p -> p.stacksTo(16));
    public static final DeferredItem<Item> PLUCKED_CRANE = ITEMS.registerSimpleItem("plucked_crane", p -> p.stacksTo(8));

    public static final DeferredItem<Item> MALLARD_EGG = ITEMS.registerSimpleItem("mallard_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> SWAN_EGG = ITEMS.registerSimpleItem("swan_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> HERON_EGG = ITEMS.registerSimpleItem("heron_egg", p -> p.stacksTo(16));
    public static final DeferredItem<Item> CRANE_EGG = ITEMS.registerSimpleItem("crane_egg", p -> p.stacksTo(16));

    // parts
    /** Plucked from ducks and swans: the best tinder there is (see onRightClick), and the lining of the Plume Headdress. */
    public static final DeferredItem<Item> DOWN = ITEMS.registerSimpleItem("down");
    public static final DeferredItem<Item> SWAN_QUILL = ITEMS.registerSimpleItem("swan_quill");
    public static final DeferredItem<Item> HERON_PLUME = ITEMS.registerSimpleItem("heron_plume");
    public static final DeferredItem<Item> HERON_BILL = ITEMS.registerSimpleItem("heron_bill");
    public static final DeferredItem<Item> HOLLOW_BIRD_BONE = ITEMS.registerSimpleItem("hollow_bird_bone");
    public static final DeferredItem<BoneFluteItem> BONE_FLUTE = ITEMS.registerItem("bone_flute", BoneFluteItem::new, p -> p.stacksTo(1));

    /** A light helmet (1 armour); wading birds let its wearer come much closer (WadingBird). */
    public static final ArmorMaterial PLUME = new ArmorMaterial(5, Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 1,
            ArmorType.LEGGINGS, 1, ArmorType.BOOTS, 1, ArmorType.BODY, 1), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "repairs_plume_headdress")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Mechanical.MODID, "plume_headdress")));
    public static final DeferredItem<Item> PLUME_HEADDRESS = ITEMS.registerSimpleItem("plume_headdress", p -> p.humanoidArmor(PLUME, ArmorType.HELMET));

    public static final DeferredItem<SpawnEggItem> MALLARD_SPAWN_EGG = egg("mallard_spawn_egg", MALLARD);
    public static final DeferredItem<SpawnEggItem> MUTE_SWAN_SPAWN_EGG = egg("mute_swan_spawn_egg", MUTE_SWAN);
    public static final DeferredItem<SpawnEggItem> GREY_HERON_SPAWN_EGG = egg("grey_heron_spawn_egg", GREY_HERON);
    public static final DeferredItem<SpawnEggItem> COMMON_CRANE_SPAWN_EGG = egg("common_crane_spawn_egg", COMMON_CRANE);

    private static Butchery.Extra feathers(int min, int max) {
        return new Butchery.Extra(() -> Items.FEATHER, 1.0F, min, max);
    }

    /** Loads this class so its registrations happen; call from the mod constructor. */
    public static void init() {
        // plucking (first knife pass) gives feathers, down, quills and plumes; the second pass the meat and the inner parts
        Butchery.register(MALLARD::get, new Butchery.Yield(RAW_MALLARD, 1, 1,
                List.of(feathers(1, 2), new Butchery.Extra(DOWN, 0.6F, 1, 1)), List.of()));
        Butchery.register(MUTE_SWAN::get, new Butchery.Yield(RAW_SWAN, 3, 4,
                List.of(feathers(3, 5), new Butchery.Extra(DOWN, 1.0F, 2, 3), new Butchery.Extra(SWAN_QUILL, 0.8F, 1, 2)),
                List.of(new Butchery.Extra(HOLLOW_BIRD_BONE, 0.6F, 1, 2))));
        Butchery.register(GREY_HERON::get, new Butchery.Yield(RAW_HERON, 1, 2,
                List.of(feathers(2, 3), new Butchery.Extra(HERON_PLUME, 0.9F, 1, 2)), List.of(new Butchery.Extra(HERON_BILL, 0.8F, 1, 1))));
        Butchery.register(COMMON_CRANE::get, new Butchery.Yield(RAW_CRANE, 2, 3,
                List.of(feathers(3, 4)), List.of(new Butchery.Extra(HOLLOW_BIRD_BONE, 0.8F, 1, 2))));
        Birds.registerPlucked(MALLARD::get, PLUCKED_MALLARD);
        Birds.registerPlucked(MUTE_SWAN::get, PLUCKED_SWAN);
        Birds.registerPlucked(GREY_HERON::get, PLUCKED_HERON);
        Birds.registerPlucked(COMMON_CRANE::get, PLUCKED_CRANE);
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

    /** The player wears a Plume Headdress. */
    public static boolean wearsPlumes(Player player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(PLUME_HEADDRESS.get());
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(MALLARD.get(), Mallard.createAttributes(5.0).build());
        event.put(MUTE_SWAN.get(), MuteSwan.createAttributes().build());
        event.put(GREY_HERON.get(), GreyHeron.createAttributes(8.0).build());
        event.put(COMMON_CRANE.get(), CommonCrane.createAttributes(12.0).build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        // mallards on the water; out of world generation also on a grassy or sandy bank right by the water
        event.register(MALLARD.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> waterBirdSpawn(level, pos, s -> false)
                        || reason != EntitySpawnReason.CHUNK_GENERATION && waterBirdSpawn(level, pos, WaterfowlFauna::bank) && waterNear(level, pos.below(), 2),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // swans only on open water
        event.register(MUTE_SWAN.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                (type, level, reason, pos, random) -> waterBirdSpawn(level, pos, s -> false),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // herons and cranes on the shore, in marsh and meadow, or in shallow water
        for (var type : List.of(GREY_HERON.get(), COMMON_CRANE.get())) {
            event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                    (t, level, reason, pos, random) -> waterBirdSpawn(level, pos, WaterfowlFauna::bank),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    private static boolean bank(BlockState s) {
        return s.is(BlockTags.ANIMALS_SPAWNABLE_ON) || s.is(BlockTags.SAND) || s.is(Blocks.MUD) || s.is(Blocks.GRAVEL);
    }

    /** Open sky, daylight, air here, and water or the right shore underneath (same rule as the goose's in Wildlife). */
    private static boolean waterBirdSpawn(LevelAccessor level, BlockPos pos, Predicate<BlockState> shore) {
        BlockState below = level.getBlockState(pos.below());
        return level.getBlockState(pos).isAir() && level.canSeeSky(pos) && level.getRawBrightness(pos, 0) > 8
                && (below.getFluidState().is(FluidTags.WATER) || shore.test(below));
    }

    private static boolean waterNear(LevelAccessor level, BlockPos pos, int r) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, 0, -r), pos.offset(r, 0, r))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    /** Down laid in an unlit campfire, Fire Pit or kiln works as tinder, like Leaf Litter or Bark (Tinder). */
    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(DOWN.get())) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity holder = Tinder.holder(level, pos, level.getBlockState(pos));
        if (holder == null) return;
        event.setCanceled(true);
        if (Tinder.has(holder)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level instanceof ServerLevel server) {
            Tinder.set(holder, true);
            stack.consume(1, event.getEntity());
            server.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            server.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.0);
        }
    }

    private WaterfowlFauna() {}
}
