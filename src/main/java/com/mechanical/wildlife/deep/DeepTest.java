package com.mechanical.wildlife.deep;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mechanical.Butchery;
import com.mechanical.CarcassEntity;
import com.mechanical.TestArea;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.giants.GiantsFauna;
import com.mechanical.wildlife.mountain.MountainFauna;
import com.mechanical.worldgen.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the deep fauna and the Mammoth Steppe: where each creature may spawn (dark cave water for the olm and the
 * fish, a cave mouth for the bats, darkness for the spider), the spider's yield and the small ones' loot, the spider
 * weaving its web across a passage and the web holding a catch, the olm feeling the water move, the swarm lingering at
 * its cave mouth then leaving, cave fish on the hook only out of the sky, and the steppe's climate slot and its animals.
 */
@EventBusSubscriber(modid = MODID)
public final class DeepTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();
    /** These need the open sky above the test (daylight leaking into a cave mouth, a sky-lit pond). */
    private static final List<String> SKY = List.of("deep_cave_mouth", "deep_fishing");

    static {
        TESTS.put("deep_cave_water", DeepTest::caveWater);
        TESTS.put("deep_cave_mouth", DeepTest::caveMouth);
        TESTS.put("deep_yields", DeepTest::yields);
        TESTS.put("deep_small_loot", DeepTest::smallLoot);
        TESTS.put("deep_spider_web", DeepTest::spiderWeb);
        TESTS.put("deep_salamander_feels", DeepTest::salamanderFeels);
        TESTS.put("deep_bat_swarm", DeepTest::batSwarm);
        TESTS.put("deep_fishing", DeepTest::fishing);
        TESTS.put("steppe_biome", DeepTest::steppe);
        TESTS.forEach((name, fn) -> TEST_FUNCTIONS.register(name, () -> fn));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS.keySet()) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            var data = SKY.contains(name)
                    ? new TestData<>(env, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 600, 0, true,
                            net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, true, TestArea.PADDING)
                    : TestArea.of(env, 800, true);
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), data));
        }
    }

    // ---------------------------------------------------------------- building blocks

    /** Stone floor at y 0, air above, x/z 0..size. */
    private static void floor(GameTestHelper h, int size) {
        for (int x = 0; x <= size; x++) for (int z = 0; z <= size; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y <= 5; y++) h.setBlock(x, y, z, Blocks.AIR);
        }
    }

    /** A sealed stone box over x/z x0..x0+4, y 1..5; inside (x0+1..x0+3, y 2..4) filled with {@code fill}. */
    private static void sealed(GameTestHelper h, int x0, int z0, net.minecraft.world.level.block.Block fill) {
        for (int x = x0; x <= x0 + 4; x++) for (int y = 1; y <= 5; y++) for (int z = z0; z <= z0 + 4; z++) {
            boolean inside = x > x0 && x < x0 + 4 && y > 1 && y < 5 && z > z0 && z < z0 + 4;
            h.setBlock(x, y, z, inside ? fill : Blocks.STONE);
        }
    }

    private static AABB area(GameTestHelper h) {
        return new AABB(Vec3.atCenterOf(h.absolutePos(new BlockPos(-2, -1, -2))), Vec3.atCenterOf(h.absolutePos(new BlockPos(18, 8, 18))));
    }

    private static String light(ServerLevel level, BlockPos p) {
        return "block " + level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p) + ", sky " + level.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
    }

    // ---------------------------------------------------------------- where they spawn

    /** Olm and cave fish: a dark sealed pool yes; an open pool, a lit pool and a lone drip of water no. The spider: not in the open. */
    private static void caveWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        sealed(h, 0, 0, Blocks.WATER);
        // an open pool beside it, 3x3 water
        for (int x = 7; x <= 11; x++) for (int z = 0; z <= 4; z++) {
            h.setBlock(x, 1, z, Blocks.STONE);
            h.setBlock(x, 2, z, x > 7 && x < 11 && z > 0 && z < 4 ? Blocks.WATER : Blocks.STONE);
        }
        BlockPos dark = h.absolutePos(new BlockPos(2, 3, 2)), open = h.absolutePos(new BlockPos(9, 2, 2));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(DeepFauna.caveWater(level, dark), "a dark sealed pool is a cave lake (" + light(level, dark) + ")"))
                .thenExecute(() -> {
                    h.assertFalse(DeepFauna.caveWater(level, open), "an open pool under the sky is not (" + light(level, open) + ")");
                    h.assertFalse(DeepFauna.caveWater(level, h.absolutePos(new BlockPos(2, 6, 2))), "not air");
                    var random = level.getRandom();
                    h.assertFalse(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(DeepFauna.CAVE_SALAMANDER.get(), level,
                            net.minecraft.world.entity.EntitySpawnReason.NATURAL, open, random), "no olm in the open pool");
                    h.assertFalse(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(DeepFauna.CAVERN_SPIDER.get(), level,
                            net.minecraft.world.entity.EntitySpawnReason.NATURAL, open.above(), random), "no cavern spider under the open sky");
                    h.setBlock(1, 3, 1, Blocks.SEA_LANTERN); // light in the lake
                })
                .thenWaitUntil(() -> h.assertFalse(DeepFauna.caveWater(level, dark), "a lit pool is not (" + light(level, dark) + ")"))
                .thenSucceed();
    }

    /** A tunnel open at one end: a few blocks in it is a cave mouth; the open air outside and a sealed room are not. */
    private static void caveMouth(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // tunnel along x: floor y 1, air y 2..3, roof y 4..5, walls z 1 and 4, closed at x 0, open at x 11
        for (int x = 0; x <= 10; x++) for (int z = 1; z <= 4; z++) for (int y = 1; y <= 5; y++) {
            boolean air = x > 0 && (z == 2 || z == 3) && (y == 2 || y == 3);
            h.setBlock(x, y, z, air ? Blocks.AIR : Blocks.STONE);
        }
        for (int x = 11; x <= 14; x++) for (int z = 1; z <= 4; z++) h.setBlock(x, 1, z, Blocks.STONE);
        sealed(h, 0, 8, Blocks.AIR);
        BlockPos mouth = h.absolutePos(new BlockPos(6, 2, 2)), outside = h.absolutePos(new BlockPos(13, 2, 2)), room = h.absolutePos(new BlockPos(2, 2, 10));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(DeepFauna.caveMouth(level, mouth), "inside the tunnel mouth (" + light(level, mouth) + ")"))
                .thenExecute(() -> {
                    h.assertFalse(DeepFauna.caveMouth(level, outside), "not out under the sky (" + light(level, outside) + ")");
                    h.assertFalse(DeepFauna.caveMouth(level, room), "not a sealed room with no daylight (" + light(level, room) + ")");
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- products

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var s = Butchery.yieldFor(DeepFauna.CAVERN_SPIDER.get());
        h.assertTrue(s.meat().get() == DeepFauna.SPIDER_LEG.get() && has(s.outer(), DeepFauna.SILK_GLAND.get())
                && has(s.inner(), DeepFauna.CAVERN_SPIDER_FANG.get()) && has(s.inner(), Items.SPIDER_EYE),
                "cavern spider: legs, the silk glands first, then fangs and eyes");
        h.succeed();
    }

    /** Olm, fish and swarm leave no carcass: killed, they drop their loot. */
    private static void smallLoot(GameTestHelper h) {
        floor(h, 12);
        ServerLevel level = h.getLevel();
        Object[][] cases = {{DeepFauna.CAVE_SALAMANDER.get(), DeepFauna.CAVE_SALAMANDER_ITEM.get()}, {DeepFauna.BLIND_CAVE_FISH.get(), DeepFauna.BLIND_CAVE_FISH_ITEM.get()},
                {DeepFauna.BAT_SWARM.get(), com.mechanical.wildlife.VanillaGame.BAT_WING.get()}, {DeepFauna.BAT_SWARM.get(), com.mechanical.wildlife.VanillaGame.GUANO.get()}};
        int x = 2;
        for (int i = 0; i < 3; i++) {
            LivingEntity e = (LivingEntity) h.spawn((EntityType<?>) cases[i][0], x, 1, 4);
            ((Mob) e).setNoAi(true);
            e.hurtServer(level, level.damageSources().generic(), 100.0F);
            x += 4;
        }
        h.startSequence().thenWaitUntil(() -> {
            for (Object[] c : cases) {
                Item want = (Item) c[1];
                h.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, area(h), i -> i.getItem().is(want)).isEmpty(), "drops " + want);
            }
            h.assertTrue(level.getEntitiesOfClass(CarcassEntity.class, area(h)).isEmpty(), "no carcass for the small ones");
        }).thenExecute(() -> level.getEntitiesOfClass(ItemEntity.class, area(h)).forEach(ItemEntity::discard)).thenSucceed();
    }

    // ---------------------------------------------------------------- behaviour

    /**
     * A passage one block wide and two high (walls z 3 and 5, roof y 4): the spider webs it across, floor to roof. Nothing
     * to web out on an open floor. A hare in the web is held (Slowness) and the spider nearby comes for it.
     */
    private static void spiderWeb(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h, 14);
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 3; y++) {
                h.setBlock(x, y, 3, Blocks.STONE);
                h.setBlock(x, y, 5, Blocks.STONE);
            }
            h.setBlock(x, 4, 4, Blocks.STONE);
            h.setBlock(x, 4, 3, Blocks.STONE);
            h.setBlock(x, 4, 5, Blocks.STONE);
        }
        int placed = CavernSpider.weave(level, h.absolutePos(new BlockPos(5, 1, 4)));
        h.assertTrue(placed == 3, "the passage is webbed across, floor to roof (" + placed + " webs)");
        BlockPos web = null;
        for (int x = 1; x <= 9 && web == null; x++) if (level.getBlockState(h.absolutePos(new BlockPos(x, 1, 4))).is(DeepFauna.CAVE_WEB.get())) web = new BlockPos(x, 1, 4);
        h.assertTrue(web != null, "a web on the passage floor");
        h.assertTrue(CavernSpider.weave(level, h.absolutePos(new BlockPos(12, 1, 12))) == 0, "nothing to web across on an open floor");
        BlockPos at = web;
        Hare hare = h.spawn(Wildlife.HARE.get(), at.getX(), at.getY(), at.getZ());
        hare.setNoAi(true);
        CavernSpider spider = h.spawn(DeepFauna.CAVERN_SPIDER.get(), 12, 1, 8);
        spider.setNoAi(true);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(hare.hasEffect(MobEffects.SLOWNESS) && spider.getTarget() == hare,
                        "the hare is caught (" + hare.hasEffect(MobEffects.SLOWNESS) + ") and the spider comes for it (" + spider.getTarget() + ")"))
                .thenExecute(() -> {
                    var pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, 12, 1, 12);
                    pig.setNoAi(true);
                    spider.doHurtTarget(level, pig);
                    h.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "the bite numbs");
                    // a web cut with a knife comes away whole
                    var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(h.absolutePos(at)), level, h.absolutePos(at), null,
                            null, new ItemStack(com.mechanical.Mechanical.FLINT_KNIFE.get()));
                    h.assertTrue(drops.stream().anyMatch(s -> s.is(DeepFauna.CAVE_WEB_ITEM.get())), "a knife takes the web whole: " + drops);
                    var bare = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(h.absolutePos(at)), level, h.absolutePos(at), null,
                            null, ItemStack.EMPTY);
                    h.assertTrue(bare.stream().anyMatch(s -> s.is(Items.STRING)) && bare.stream().noneMatch(s -> s.is(DeepFauna.CAVE_WEB_ITEM.get())),
                            "by hand only String: " + bare);
                })
                .thenSucceed();
    }

    /** The olm ignores something still beside it in the water, but feels something swimming past and darts off. */
    private static void salamanderFeels(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y <= 3; y++) h.setBlock(x, y, z, x == 0 || x == 8 || z == 0 || z == 8 ? Blocks.STONE : Blocks.WATER);
        }
        CaveSalamander olm = h.spawn(DeepFauna.CAVE_SALAMANDER.get(), 4, 1, 4);
        olm.setNoAi(true); // stays put; its feeling for the water (aiStep) still runs
        Hare swimmer = h.spawn(Wildlife.HARE.get(), 6, 2, 4);
        swimmer.setNoAi(true);
        h.startSequence()
                .thenIdle(30)
                .thenExecute(() -> h.assertFalse(olm.feltMovement(), "nothing moving: it is blind to what lies still"))
                .thenWaitUntil(() -> {
                    swimmer.setDeltaMovement(0.0, 0.0, Math.sin(h.getTick() * 0.5) * 0.3);
                    h.assertTrue(olm.feltMovement(), "it feels the water move");
                })
                .thenSucceed();
    }

    /** The swarm whirls round its cave mouth for the first LINGER ticks, then streams away. */
    private static void batSwarm(GameTestHelper h) {
        floor(h, 10);
        BatSwarm swarm = h.spawn(DeepFauna.BAT_SWARM.get(), 5, 2, 5);
        BlockPos home = h.absolutePos(new BlockPos(5, 2, 5));
        swarm.setHome(home);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(swarm.tickCount > BatSwarm.LINGER - 40, "ticking (" + swarm.tickCount + ")"))
                .thenExecute(() -> h.assertTrue(swarm.lingering() && swarm.position().distanceTo(Vec3.atCenterOf(home)) < 7,
                        "still at its cave mouth (" + swarm.position().distanceTo(Vec3.atCenterOf(home)) + ")"))
                .thenWaitUntil(() -> h.assertTrue(!swarm.isAlive() || swarm.position().distanceTo(Vec3.atCenterOf(home)) > 12,
                        "then it streams away (" + swarm.position().distanceTo(Vec3.atCenterOf(home)) + ")"))
                .thenExecute(swarm::discard)
                .thenSucceed();
    }

    /** Fishing loot: a hook in a dark sealed lake brings up Blind Cave Fish; one in a sky-lit pond never does. */
    private static void fishing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        sealed(h, 0, 0, Blocks.WATER);
        for (int x = 7; x <= 11; x++) for (int z = 0; z <= 4; z++) {
            h.setBlock(x, 1, z, Blocks.STONE);
            h.setBlock(x, 2, z, x > 7 && x < 11 && z > 0 && z < 4 ? Blocks.WATER : Blocks.STONE);
        }
        BlockPos cave = h.absolutePos(new BlockPos(2, 3, 2)), pond = h.absolutePos(new BlockPos(9, 2, 2));
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.withDefaultNamespace("gameplay/fishing/fish")));
        java.util.function.Function<BlockPos, Integer> cast = p -> {
            int n = 0;
            for (int i = 0; i < 300; i++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p))
                        .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
                for (ItemStack s : table.getRandomItems(params)) if (s.is(DeepFauna.BLIND_CAVE_FISH_ITEM.get())) n++;
            }
            return n;
        };
        // wait for the light to settle (the sealed lake dark, the pond's air lit by the sky)
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.canSeeSky(pond.above()) && !level.canSeeSky(cave.above()), "light settled"))
                .thenExecute(() -> {
                    h.assertTrue(cast.apply(pond) == 0, "no cave fish in a pond under the sky");
                    if (cave.getY() <= com.mechanical.Temperature.CAVE_TOP) {
                        int n = cast.apply(cave);
                        h.assertTrue(n > 100, "a dark cave lake gives mostly cave fish (" + n + " of 300)");
                    }
                })
                .thenSucceed();
    }

    // ---------------------------------------------------------------- the Mammoth Steppe

    private static boolean spawnsIn(ServerLevel level, ResourceKey<Biome> key, EntityType<?> type) {
        Biome biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key).value();
        MobSpawnSettings s = biome.getModifiedEnvironmentAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        return s.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(w -> w.value().type() == type);
    }

    /**
     * The steppe holds the cold driest slot of the snowy plains (no snowy plains left there) and nothing else; it has the
     * mammoth, bison, wild horse, saiga and cave lion, and no polar bear.
     */
    private static void steppe(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (EntityType<?> t : List.of(GiantsFauna.WOOLLY_MAMMOTH.get(), GiantsFauna.STEPPE_BISON.get(), Wildlife.WILD_HORSE.get(),
                MountainFauna.SAIGA.get(), Wildlife.CAVE_LION.get()))
            h.assertTrue(spawnsIn(level, ModBiomes.MAMMOTH_STEPPE, t), t.getDescriptionId() + " roams the Mammoth Steppe");
        h.assertFalse(spawnsIn(level, ModBiomes.MAMMOTH_STEPPE, net.minecraft.world.entity.EntityTypes.POLAR_BEAR), "no polar bears on the steppe");
        Climate.ParameterList<Holder<Biome>> list = level.registryAccess().lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
        int steppe = 0;
        Climate.ParameterPoint one = null;
        for (var pair : list.values()) {
            Climate.ParameterPoint p = pair.getFirst();
            boolean coldDry = p.temperature().max() <= Climate.quantizeCoord(-0.45F) && p.humidity().max() <= Climate.quantizeCoord(-0.35F);
            if (pair.getSecond().is(ModBiomes.MAMMOTH_STEPPE)) {
                steppe++;
                one = p;
                h.assertTrue(coldDry, "the steppe sits only in the frozen, driest band");
            } else if (pair.getSecond().is(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS)) {
                h.assertFalse(coldDry, "no snowy plains left in the steppe's slot");
            }
        }
        h.assertTrue(steppe > 0, "the steppe is in the overworld's biome map");
        Climate.ParameterPoint p = one;
        Climate.TargetPoint mid = new Climate.TargetPoint((p.temperature().min() + p.temperature().max()) / 2, (p.humidity().min() + p.humidity().max()) / 2,
                (p.continentalness().min() + p.continentalness().max()) / 2, (p.erosion().min() + p.erosion().max()) / 2,
                (p.depth().min() + p.depth().max()) / 2, (p.weirdness().min() + p.weirdness().max()) / 2);
        h.assertTrue(list.findValue(mid).is(ModBiomes.MAMMOTH_STEPPE), "the middle of its slot resolves to the steppe");
        h.succeed();
    }

    private DeepTest() {}
}
