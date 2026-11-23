package com.mechanical.wildlife.deep;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Butchery;
import com.mechanical.Butchery.Extra;
import com.mechanical.Butchery.Yield;
import com.mechanical.Mechanical;
import com.mechanical.Temperature;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.underground.Underground;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The deep fauna (docs/fauna/deep.md), the rest of the underground: the Cave Salamander (olm) and the Blind Cave Fish in
 * dark cave lakes, the Bat Swarm that pours out of a cave mouth at dusk (and so shows where the caves are), and the
 * Cavern Spider that spins big webs across narrow passages. The cave-lake and cave-mouth spawners are here; the spider
 * uses {@link Underground}'s darkness rule. Data: scripts/gen_fauna_deep.py; textures: scripts/gen_fauna_deep_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class DeepFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<CaveSalamander>> CAVE_SALAMANDER = ENTITY_TYPES.registerEntityType(
            "cave_salamander", CaveSalamander::new, MobCategory.WATER_AMBIENT, b -> b.sized(0.5F, 0.2F).eyeHeight(0.12F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<BlindCaveFish>> BLIND_CAVE_FISH = ENTITY_TYPES.registerEntityType(
            "blind_cave_fish", BlindCaveFish::new, MobCategory.WATER_AMBIENT, b -> b.sized(0.4F, 0.25F).eyeHeight(0.15F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<BatSwarm>> BAT_SWARM = ENTITY_TYPES.registerEntityType(
            "bat_swarm", BatSwarm::new, MobCategory.AMBIENT, b -> b.sized(1.2F, 1.0F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<CavernSpider>> CAVERN_SPIDER = ENTITY_TYPES.registerEntityType(
            "cavern_spider", CavernSpider::new, MobCategory.CREATURE, b -> b.sized(1.1F, 0.7F).clientTrackingRange(10));

    // ---- olm ----
    public static final int BROTH_TICKS = 3600;
    /** The whole olm (what it drops): barely a mouthful raw; it goes into the Dark-Sight Broth. */
    public static final DeferredItem<Item> CAVE_SALAMANDER_ITEM = ITEMS.registerSimpleItem("cave_salamander",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()));
    /** Olm, glow lichen and water in a bowl: the old remedy against the dark (Night Vision 3 min). */
    public static final DeferredItem<Item> DARK_SIGHT_BROTH = ITEMS.registerSimpleItem("dark_sight_broth", p -> p.stacksTo(1)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).alwaysEdible().build(), Consumables.defaultFood()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, BROTH_TICKS, 0))).build())
            .usingConvertsTo(Items.BOWL));

    // ---- cave fish ----
    public static final DeferredItem<Item> BLIND_CAVE_FISH_ITEM = food("blind_cave_fish", 1, 0.1F);
    public static final DeferredItem<Item> COOKED_CAVE_FISH = food("cooked_cave_fish", 4, 0.6F);

    // ---- cavern spider ----
    public static final DeferredItem<Item> SPIDER_LEG = food("spider_leg", 2, 0.1F);
    public static final DeferredItem<Item> ROASTED_SPIDER_LEG = food("roasted_spider_leg", 5, 0.6F);
    /** Cut open with a knife: 3 String. */
    public static final DeferredItem<Item> SILK_GLAND = ITEMS.registerSimpleItem("silk_gland");
    /** Ringed with 8 Arrows: 8 Arrows of Slowness (the spider's numbing bite). */
    public static final DeferredItem<Item> CAVERN_SPIDER_FANG = ITEMS.registerSimpleItem("cavern_spider_fang");
    /** The great web: whoever walks in is held fast. Taken whole with shears or a knife, and set again as a trap. */
    public static final DeferredBlock<CaveWebBlock> CAVE_WEB = BLOCKS.registerBlock("cave_web", CaveWebBlock::new, p -> p
            .mapColor(MapColor.WOOL).sound(SoundType.COBWEB).forceSolidOn().noCollision().strength(4.0F)
            .pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> CAVE_WEB_ITEM = ITEMS.registerSimpleBlockItem("cave_web", CAVE_WEB);

    public static final DeferredItem<SpawnEggItem> CAVE_SALAMANDER_SPAWN_EGG = egg("cave_salamander_spawn_egg", CAVE_SALAMANDER);
    public static final DeferredItem<SpawnEggItem> BLIND_CAVE_FISH_SPAWN_EGG = egg("blind_cave_fish_spawn_egg", BLIND_CAVE_FISH);
    public static final DeferredItem<SpawnEggItem> BAT_SWARM_SPAWN_EGG = egg("bat_swarm_spawn_egg", BAT_SWARM);
    public static final DeferredItem<SpawnEggItem> CAVERN_SPIDER_SPAWN_EGG = egg("cavern_spider_spawn_egg", CAVERN_SPIDER);

    /** Spider band: the shallow caves, where the passages are narrow (Underground's darkness rule). */
    public static final int SPIDER_MIN_Y = 0, SPIDER_MAX_Y = 40;
    static final int INTERVAL = 200;
    static final float LAKE_CHANCE = 0.3F, MOUTH_CHANCE = 0.25F;
    static final int MAX_SALAMANDERS = 3, MAX_FISH = 8, MAX_SWARMS = 1;

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static Extra x(Supplier<Item> item, float chance, int min, int max) {
        return new Extra(item, chance, min, max);
    }

    /** Registers everything (class load), the spider's carcass and its depth band; call from the mod constructor. */
    public static void init() {
        Butchery.register(CAVERN_SPIDER::get, new Yield(SPIDER_LEG, 2, 4, List.of(x(SILK_GLAND, 0.9F, 1, 2)),
                List.of(x(CAVERN_SPIDER_FANG, 0.7F, 1, 2), x(() -> Items.SPIDER_EYE, 0.5F, 1, 2))));
        Butchery.neverCarcass(BAT_SWARM::get);
        Underground.addBand(CAVERN_SPIDER, SPIDER_MIN_Y, SPIDER_MAX_Y, 2, 1, 1);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(CAVE_SALAMANDER.get(), CaveSalamander.createAttributes().build());
        event.put(BLIND_CAVE_FISH.get(), BlindCaveFish.createAttributes().build());
        event.put(BAT_SWARM.get(), BatSwarm.createAttributes().build());
        event.put(CAVERN_SPIDER.get(), CavernSpider.createAttributes().build());
    }

    /** Natural spawns come from the spawners below and Underground's; these rules cover spawners and commands. */
    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        event.register(CAVE_SALAMANDER.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                (t, level, reason, pos, random) -> caveWater(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BLIND_CAVE_FISH.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                (t, level, reason, pos, random) -> caveWater(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BAT_SWARM.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> caveMouth(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CAVERN_SPIDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> Underground.canSpawn(level, pos, SPIDER_MIN_Y, SPIDER_MAX_Y), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // ---------------------------------------------------------------- the rules

    /**
     * A dark cave lake: water at {@code pos} under {@link Temperature#CAVE_TOP}, no sky light and no block light, and at
     * least 5 of the 9 blocks at that level round it water (a lake, not a drip pool).
     */
    public static boolean caveWater(LevelReader level, BlockPos pos) {
        if (pos.getY() >= Temperature.CAVE_TOP || !level.getFluidState(pos).is(FluidTags.WATER)) return false;
        if (level.getBrightness(LightLayer.SKY, pos) > 0 || level.getBrightness(LightLayer.BLOCK, pos) > 0) return false;
        int water = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (level.getFluidState(pos.offset(dx, 0, dz)).is(FluidTags.WATER)) water++;
        return water >= 5;
    }

    /**
     * Just inside a cave mouth: open air (two blocks of it), out of the open sky but with some daylight leaking in (sky light
     * 1..12), and rock overhead (natural stone within 5 blocks above). Deep inside the sky light is 0; out in the open, or
     * under trees and roofs, there is no rock over it.
     */
    public static boolean caveMouth(LevelReader level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
        int sky = level.getBrightness(LightLayer.SKY, pos);
        if (sky < 1 || sky > 12 || level.canSeeSky(pos)) return false;
        for (int dy = 2; dy <= 6; dy++) if (level.getBlockState(pos.above(dy)).is(BlockTags.BASE_STONE_OVERWORLD)) return true;
        return false;
    }

    /** Dusk on the overworld clock: the bats come out. */
    public static boolean dusk(ServerLevel level) {
        long time = level.getOverworldClockTime() % 24000;
        return time >= 11800 && time <= 13800;
    }

    // ---------------------------------------------------------------- the spawners

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % INTERVAL != 37
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) return;
        boolean dusk = dusk(level);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            if (Underground.inCave(level, player.blockPosition())) {
                if (level.getRandom().nextFloat() < LAKE_CHANCE) spawnInLake(level, player.blockPosition());
            } else if (dusk && level.getRandom().nextFloat() < MOUTH_CHANCE) {
                spawnAtMouth(level, player.blockPosition());
            }
        }
    }

    /** One try at olms (1 in 3) or a school of cave fish in a dark lake 8-32 blocks from {@code at}; returns how many. */
    static int spawnInLake(ServerLevel level, BlockPos at) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2, dist = 8 + random.nextInt(25);
        BlockPos start = BlockPos.containing(at.getX() + Math.cos(angle) * dist, at.getY() + 6, at.getZ() + Math.sin(angle) * dist);
        if (!level.isLoaded(start)) return 0;
        for (int dy = 0; dy < 18; dy++) {
            BlockPos p = start.below(dy);
            if (!caveWater(level, p)) continue;
            boolean olm = random.nextInt(3) == 0;
            return olm ? spawnGroup(level, CAVE_SALAMANDER.get(), p, 1 + random.nextInt(2), MAX_SALAMANDERS, DeepFauna::caveWater)
                    : spawnGroup(level, BLIND_CAVE_FISH.get(), p, 3 + random.nextInt(3), MAX_FISH, DeepFauna::caveWater);
        }
        return 0;
    }

    /** At dusk: a swarm at a cave mouth 12-48 blocks from {@code at} (searched down from the surface); returns how many. */
    static int spawnAtMouth(ServerLevel level, BlockPos at) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, dist = 12 + random.nextInt(37);
            int x = (int) Math.floor(at.getX() + Math.cos(angle) * dist), z = (int) Math.floor(at.getZ() + Math.sin(angle) * dist);
            if (!level.isLoaded(new BlockPos(x, at.getY(), z))) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            for (int y = top - 3; y > top - 30; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (caveMouth(level, p)) return spawnGroup(level, BAT_SWARM.get(), p, 1, MAX_SWARMS, DeepFauna::caveMouth);
            }
        }
        return 0;
    }

    /** {@code count} of {@code type} at and around {@code pos} where {@code rule} holds, fewer if {@code max} are already within 48 blocks. */
    static <T extends Mob> int spawnGroup(ServerLevel level, EntityType<T> type, BlockPos pos, int count, int max,
            java.util.function.BiPredicate<LevelReader, BlockPos> rule) {
        int near = level.getEntities(type, new AABB(pos).inflate(48), e -> e.isAlive()).size();
        count = Math.min(count, max - near);
        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 8, false) != null) return 0;
        RandomSource random = level.getRandom();
        int placed = 0;
        for (int i = 0; i < count * 4 && placed < count; i++) {
            BlockPos p = placed == 0 ? pos : pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            if (!rule.test(level, p)) continue;
            T mob = type.create(level, EntitySpawnReason.NATURAL);
            if (mob == null) return placed;
            mob.snapTo(p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5, random.nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.NATURAL, null);
            mob.addTag(Predator.NIGHT_TAG);
            if (mob instanceof BatSwarm swarm) swarm.setHome(p);
            level.addFreshEntity(mob);
            placed++;
        }
        return placed;
    }

    private DeepFauna() {}
}
