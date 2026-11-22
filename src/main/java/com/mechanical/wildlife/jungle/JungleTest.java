package com.mechanical.wildlife.jungle;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mechanical.AnimalFear;
import com.mechanical.Butchery;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.TestArea;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.meadow.MeadowFauna;
import com.mechanical.wildlife.warm.Tapir;
import com.mechanical.wildlife.warm.WarmFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the jungle and mangrove fauna (JungleFauna): every species spawns in its own biomes and not elsewhere;
 * the butchering yields and the small ones' loot; and each one's key behaviour, driven directly (no waiting on AI): the
 * python's and the crocodile's grip (held, hurt, knocked loose by someone else, struggled free), the crocodile lurking,
 * the tiger vanishing in the thicket, the orangutan's theft and throwing, the monitor's venom and carcass eating, the dart
 * frog's touch and the Blowpipe, the crab ducking into its hole, the mudskipper breathing and skipping, the proboscis
 * monkey's alarm.
 */
@EventBusSubscriber(modid = MODID)
public final class JungleTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();

    static {
        TESTS.put("jungle_spawn_biomes", JungleTest::spawnBiomes);
        TESTS.put("jungle_yields", JungleTest::yields);
        TESTS.put("jungle_small_loot", JungleTest::smallLoot);
        TESTS.put("jungle_python_grip", JungleTest::pythonGrip);
        TESTS.put("jungle_crocodile", JungleTest::crocodile);
        TESTS.put("jungle_tiger_thicket", JungleTest::tigerThicket);
        TESTS.put("jungle_orangutan", JungleTest::orangutan);
        TESTS.put("jungle_monitor", JungleTest::monitor);
        TESTS.put("jungle_dart_frog", JungleTest::dartFrog);
        TESTS.put("jungle_mangrove_small", JungleTest::mangroveSmall);
        TESTS.forEach((name, fn) -> TEST_FUNCTIONS.register(name, () -> fn));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS.keySet()) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), TestArea.of(env, 200, true)));
        }
    }

    /** Stone floor at y 0, air above, x/z 0..size. */
    private static void floor(GameTestHelper h, int size) {
        for (int x = 0; x <= size; x++) for (int z = 0; z <= size; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y <= 4; y++) h.setBlock(x, y, z, Blocks.AIR);
        }
    }

    private static AABB area(GameTestHelper h) {
        return new AABB(Vec3.atCenterOf(h.absolutePos(new BlockPos(-2, -1, -2))), Vec3.atCenterOf(h.absolutePos(new BlockPos(18, 8, 18))));
    }

    // ---------------------------------------------------------------- spawns

    private static final ResourceKey<Biome> MANGROVE = Biomes.MANGROVE_SWAMP;
    private static final List<ResourceKey<Biome>> JUNGLES = List.of(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE);

    private static boolean spawnsIn(ServerLevel level, ResourceKey<Biome> key, EntityType<?> type) {
        Biome biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key).value();
        MobSpawnSettings s = biome.getModifiedEnvironmentAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        return s.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(w -> w.value().type() == type);
    }

    private static void spawnBiomes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Map<EntityType<?>, List<ResourceKey<Biome>>> where = Map.of(
                JungleFauna.TIGER.get(), JUNGLES, JungleFauna.PYTHON.get(), List.of(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, MANGROVE),
                JungleFauna.ORANGUTAN.get(), List.of(Biomes.JUNGLE), JungleFauna.MONITOR_LIZARD.get(), List.of(Biomes.JUNGLE, MANGROVE),
                JungleFauna.POISON_DART_FROG.get(), JUNGLES, JungleFauna.SALTWATER_CROCODILE.get(), List.of(MANGROVE),
                JungleFauna.MUDSKIPPER.get(), List.of(MANGROVE), JungleFauna.FIDDLER_CRAB.get(), List.of(MANGROVE),
                JungleFauna.PROBOSCIS_MONKEY.get(), List.of(MANGROVE));
        List<ResourceKey<Biome>> never = List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.TAIGA, Biomes.SNOWY_PLAINS, Biomes.SWAMP);
        where.forEach((type, biomes) -> {
            for (ResourceKey<Biome> b : biomes) h.assertTrue(spawnsIn(level, b, type), type.getDescriptionId() + " spawns in " + b.identifier());
            for (ResourceKey<Biome> b : never) h.assertFalse(spawnsIn(level, b, type), type.getDescriptionId() + " never in " + b.identifier());
        });
        // the mangrove ones stay out of the jungle and the jungle ones out of the mangroves
        for (EntityType<?> t : List.of(JungleFauna.SALTWATER_CROCODILE.get(), JungleFauna.MUDSKIPPER.get(), JungleFauna.FIDDLER_CRAB.get(),
                JungleFauna.PROBOSCIS_MONKEY.get())) h.assertFalse(spawnsIn(level, Biomes.JUNGLE, t), t.getDescriptionId() + " not in the jungle");
        for (EntityType<?> t : List.of(JungleFauna.TIGER.get(), JungleFauna.ORANGUTAN.get(), JungleFauna.POISON_DART_FROG.get()))
            h.assertFalse(spawnsIn(level, MANGROVE, t), t.getDescriptionId() + " not in the mangroves");
        h.succeed();
    }

    // ---------------------------------------------------------------- products

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var t = Butchery.yieldFor(JungleFauna.TIGER.get());
        h.assertTrue(t.meat().get() == JungleFauna.TIGER_MEAT.get() && has(t.outer(), JungleFauna.RAW_TIGER_PELT.get())
                && has(t.inner(), Wildlife.SINEW.get()) && has(t.inner(), Items.BONE), "tiger: meat, the pelt first, sinew and bones");
        var p = Butchery.yieldFor(JungleFauna.PYTHON.get());
        h.assertTrue(p.meat().get() == JungleFauna.PYTHON_MEAT.get() && has(p.outer(), JungleFauna.PYTHON_SKIN.get()), "python: skin, meat");
        var o = Butchery.yieldFor(JungleFauna.ORANGUTAN.get());
        h.assertTrue(o.meat().get() == JungleFauna.ORANGUTAN_MEAT.get() && has(o.outer(), JungleFauna.ORANGUTAN_HAIR.get()), "orangutan: hair, meat");
        var m = Butchery.yieldFor(JungleFauna.MONITOR_LIZARD.get());
        h.assertTrue(m.meat().get() == JungleFauna.MONITOR_MEAT.get() && has(m.outer(), JungleFauna.MONITOR_SKIN.get())
                && has(m.inner(), MeadowFauna.VENOM_GLAND.get()), "monitor: skin, meat, venom gland");
        var c = Butchery.yieldFor(JungleFauna.SALTWATER_CROCODILE.get());
        h.assertTrue(c.meat().get() == JungleFauna.CROCODILE_MEAT.get() && has(c.outer(), JungleFauna.RAW_CROCODILE_HIDE.get()), "crocodile: hide, meat");
        h.assertTrue(Butchery.requiredKnifeTier(JungleFauna.SALTWATER_CROCODILE.get()) >= com.mechanical.KnifeItem.TUSK, "crocodile hide is tough");
        var k = Butchery.yieldFor(JungleFauna.PROBOSCIS_MONKEY.get());
        h.assertTrue(k.meat().get() == JungleFauna.MONKEY_MEAT.get(), "proboscis monkey: its own meat");
        h.succeed();
    }

    /** Frog, mudskipper and crab leave no carcass: killed, they drop their loot. */
    private static void smallLoot(GameTestHelper h) {
        floor(h, 8);
        ServerLevel level = h.getLevel();
        Object[][] cases = {{JungleFauna.POISON_DART_FROG.get(), JungleFauna.FROG_POISON.get()}, {JungleFauna.MUDSKIPPER.get(), JungleFauna.MUDSKIPPER_ITEM.get()},
                {JungleFauna.FIDDLER_CRAB.get(), JungleFauna.RAW_CRAB_MEAT.get()}};
        int x = 2;
        for (Object[] c : cases) {
            LivingEntity e = (LivingEntity) h.spawn((EntityType<?>) c[0], x, 1, 4);
            ((net.minecraft.world.entity.Mob) e).setNoAi(true);
            e.hurtServer(level, level.damageSources().generic(), 100.0F);
            x += 3;
        }
        h.startSequence().thenWaitUntil(() -> {
            for (Object[] c : cases) {
                Item want = (Item) c[1];
                h.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, area(h), i -> i.getItem().is(want)).isEmpty(), "drops " + want);
            }
            h.assertTrue(level.getEntitiesOfClass(CarcassEntity.class, area(h)).isEmpty(), "no carcass for the small ones");
        }).thenExecute(() -> level.getEntitiesOfClass(ItemEntity.class, area(h)).forEach(ItemEntity::discard)).thenSucceed();
    }

    // ---------------------------------------------------------------- grips

    private static void pythonGrip(GameTestHelper h) {
        floor(h, 10);
        ServerLevel level = h.getLevel();
        Python python = h.spawn(JungleFauna.PYTHON.get(), 5, 1, 5);
        Tapir victim = h.spawn(WarmFauna.TAPIR.get(), 5, 1, 6);
        Tapir helper = h.spawn(WarmFauna.TAPIR.get(), 2, 1, 2);
        python.setNoAi(true);
        victim.setNoAi(true);
        helper.setNoAi(true);
        python.constrict(victim);
        Vec3 anchor = victim.position();
        victim.setPos(anchor.add(3, 0, 0)); // tries to walk off
        for (int i = 0; i < Grip.INTERVAL + 2; i++) python.tick();
        h.assertTrue(python.isConstricting() && python.victim() == victim, "the python holds on");
        h.assertTrue(victim.position().distanceTo(anchor) < 0.5, "the victim is held in place (" + victim.position().distanceTo(anchor) + ")");
        h.assertTrue(victim.getHealth() < victim.getMaxHealth(), "the coils squeeze");
        // someone else's blow knocks it loose
        python.hurtServer(level, level.damageSources().mobAttack(helper), 1.0F);
        h.assertFalse(python.isConstricting(), "one blow from someone else frees the victim");
        // the victim's own blows: it struggles free on the 5th
        python.constrict(victim);
        for (int i = 1; i <= 4; i++) {
            python.hurtServer(level, level.damageSources().mobAttack(victim), 0.5F);
        }
        h.assertTrue(python.isConstricting(), "4 struggles are not enough");
        python.hurtServer(level, level.damageSources().mobAttack(victim), 0.5F);
        h.assertFalse(python.isConstricting(), "the 5th struggle breaks free");
        h.assertTrue(python.isPrey(h.makeMockPlayer(GameType.SURVIVAL)) && !python.isPrey(h.spawn(JungleFauna.TIGER.get(), 8, 1, 8)), "squeezes people, not tigers");
        level.getEntitiesOfClass(LivingEntity.class, area(h), e -> !(e instanceof Player)).forEach(e -> e.discard());
        h.succeed();
    }

    private static void crocodile(GameTestHelper h) {
        floor(h, 12);
        ServerLevel level = h.getLevel();
        // a 5x5 pool 2 deep
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) {
            h.setBlock(x, 0, z, Blocks.WATER);
            h.setBlock(x, -1, z, Blocks.WATER);
            h.setBlock(x, -2, z, Blocks.MUD);
        }
        Crocodile croc = h.spawn(JungleFauna.SALTWATER_CROCODILE.get(), 3, 0, 3);
        croc.setNoAi(true);
        for (int i = 0; i < 3; i++) croc.tick();
        h.assertTrue(croc.isLurking(), "lies still in the water, lurking");
        Tapir victim = h.spawn(WarmFauna.TAPIR.get(), 7, 1, 3); // drinking at the edge
        Tapir helper = h.spawn(WarmFauna.TAPIR.get(), 10, 1, 10);
        victim.setNoAi(true);
        helper.setNoAi(true);
        croc.seize(victim);
        for (int i = 0; i < Grip.INTERVAL + 2; i++) croc.tick();
        h.assertTrue(croc.isRolling() && croc.victim() == victim, "seized: the death roll");
        h.assertTrue(victim.distanceTo(croc) < 2.5, "the victim is dragged to its jaws (" + victim.distanceTo(croc) + ")");
        h.assertTrue(victim.getHealth() < victim.getMaxHealth(), "the roll hurts");
        croc.hurtServer(level, level.damageSources().mobAttack(helper), 1.0F);
        h.assertFalse(croc.isRolling(), "struck loose by someone else");
        h.assertTrue(croc.canBreatheUnderwater(), "holds its breath");
        level.getEntitiesOfClass(LivingEntity.class, area(h), e -> !(e instanceof Player)).forEach(e -> e.discard());
        h.succeed();
    }

    // ---------------------------------------------------------------- the others

    private static void tigerThicket(GameTestHelper h) {
        floor(h, 14);
        Tiger tiger = h.spawn(JungleFauna.TIGER.get(), 5, 1, 5);
        tiger.setNoAi(true);
        tiger.stalk(true);
        h.assertFalse(tiger.isHidden(), "in the open it can be seen");
        for (int x = 4; x <= 6; x++) for (int z = 4; z <= 6; z++) {
            if (x == 5 && z == 5) continue;
            h.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            h.setBlock(x, 1, z, Blocks.BAMBOO);
        }
        h.assertTrue(tiger.inThicket() && tiger.isHidden(), "creeping through bamboo it is hidden");
        tiger.updateCamouflage();
        h.assertTrue(tiger.isInvisible(), "and invisible");
        Tapir prey = h.spawn(WarmFauna.TAPIR.get(), 5, 1, 13);
        prey.setNoAi(true);
        tiger.setTarget(prey);
        h.assertTrue(tiger.isHidden(), "quarry 8 blocks off: still hidden");
        prey.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5, 1, 7))));
        tiger.updateCamouflage();
        h.assertTrue(!tiger.isHidden() && !tiger.isInvisible(), "within reach of its quarry it shows itself");
        tiger.discard();
        prey.discard();
        h.succeed();
    }

    private static void orangutan(GameTestHelper h) {
        floor(h, 10);
        ServerLevel level = h.getLevel();
        Orangutan ape = h.spawn(JungleFauna.ORANGUTAN.get(), 5, 1, 5);
        ape.setNoAi(true);
        ItemEntity loot = new ItemEntity(level, ape.getX() + 0.5, ape.getY(), ape.getZ(), new ItemStack(Items.IRON_INGOT, 3));
        level.addFreshEntity(loot);
        h.assertTrue(ape.take(loot) && ape.loot().is(Items.IRON_INGOT) && ape.loot().getCount() == 3 && loot.isRemoved(), "it takes what lies on the ground");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(ape.position().add(4, 0, 0));
        ape.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
        h.assertTrue(ape.loot().isEmpty(), "hit, it drops the loot");
        h.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, area(h), i -> i.getItem().is(Items.IRON_INGOT)).isEmpty(), "the loot lies on the ground again");
        ape.pelt(level, player);
        h.assertTrue(!level.getEntitiesOfClass(Snowball.class, area(h)).isEmpty(), "it throws fruit or sticks at a player close by");
        level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area(h), e -> !(e instanceof Player)).forEach(e -> e.discard());
        h.succeed();
    }

    private static void monitor(GameTestHelper h) {
        floor(h, 10);
        ServerLevel level = h.getLevel();
        MonitorLizard monitor = h.spawn(JungleFauna.MONITOR_LIZARD.get(), 4, 1, 4);
        monitor.setNoAi(true);
        Tapir bitten = h.spawn(WarmFauna.TAPIR.get(), 5, 1, 4);
        bitten.setNoAi(true);
        monitor.doHurtTarget(level, bitten);
        h.assertTrue(bitten.hasEffect(MobEffects.POISON), "the bite is venomous");
        CarcassEntity carcass = CarcassEntity.of(level, bitten);
        level.addFreshEntity(carcass);
        bitten.discard();
        boolean eaten = false;
        for (int i = 1; i <= MonitorLizard.FEED_TICKS && !eaten; i++) eaten = monitor.feed(carcass, i);
        h.assertTrue(eaten && carcass.isRemoved(), "it eats a carcass down to the bones");
        monitor.discard();
        h.succeed();
    }

    private static void dartFrog(GameTestHelper h) {
        floor(h, 10);
        ServerLevel level = h.getLevel();
        DartFrog frog = h.spawn(JungleFauna.POISON_DART_FROG.get(), 3, 1, 3);
        frog.setNoAi(true);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(frog.position().add(1, 0, 0));
        frog.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
        h.assertTrue(player.hasEffect(MobEffects.POISON), "striking the frog by hand poisons you");
        frog.discard();
        // the blowpipe: a poison dart flies with the poison on it
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JungleFauna.BLOWPIPE.get()));
        player.getInventory().add(new ItemStack(JungleFauna.DART.get(), 4));
        player.getInventory().add(new ItemStack(JungleFauna.POISON_DART.get(), 2));
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5, 1, 5))));
        JungleFauna.BLOWPIPE.get().use(level, player, InteractionHand.MAIN_HAND);
        List<Arrow> darts = level.getEntitiesOfClass(Arrow.class, area(h));
        h.assertTrue(darts.size() == 1, "one dart flies (" + darts.size() + ")");
        h.assertTrue(BlowpipeItem.ammo(player).is(JungleFauna.POISON_DART.get()) && BlowpipeItem.ammo(player).getCount() == 1, "a poison dart was used first");
        h.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "a breath between shots");
        darts.forEach(Arrow::discard);
        h.succeed();
    }

    private static void mangroveSmall(GameTestHelper h) {
        floor(h, 12);
        ServerLevel level = h.getLevel();
        // the crab ducks into its mud hole and can't be hit there; on stone it has none
        h.setBlock(3, 0, 3, Blocks.MUD);
        FiddlerCrab crab = h.spawn(JungleFauna.FIDDLER_CRAB.get(), 3, 1, 3);
        crab.setNoAi(true);
        h.assertTrue(crab.onMud() && crab.hide() && crab.isHiding(), "on mud it ducks into its hole");
        float hp = crab.getHealth();
        h.assertFalse(crab.hurtServer(level, level.damageSources().generic(), 2.0F), "down its hole it can't be hit");
        h.assertTrue(crab.getHealth() == hp, "no harm done");
        FiddlerCrab stranded = h.spawn(JungleFauna.FIDDLER_CRAB.get(), 6, 1, 6);
        h.assertFalse(stranded.hide(), "out on stone there is no hole");
        // the mudskipper breathes anywhere and skips away over the mud
        Mudskipper skipper = h.spawn(JungleFauna.MUDSKIPPER.get(), 9, 1, 3); // with AI: noAi mobs don't fall onto the ground
        h.assertTrue(skipper.canBreatheUnderwater(), "a fish that breathes in water too");
        h.startSequence().thenWaitUntil(() -> h.assertTrue(skipper.onGround(), "lands")).thenExecute(() -> {
            h.assertTrue(skipper.skip() && skipper.getDeltaMovement().y > 0.3, "it skips in a long flip");
            h.assertTrue(new ItemStack(JungleFauna.MUDSKIPPER_ITEM.get()).is(com.mechanical.Traps.SNARE_BAIT), "a mudskipper is bait");
            // the proboscis monkey's alarm sends the animals round it running
            ProboscisMonkey monkey = h.spawn(JungleFauna.PROBOSCIS_MONKEY.get(), 3, 1, 10);
            Tapir tapir = h.spawn(WarmFauna.TAPIR.get(), 8, 1, 10);
            monkey.setNoAi(true);
            tapir.setNoAi(true);
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(monkey.position().add(0, 0, -6));
            h.assertFalse(AnimalFear.isPanicking(tapir), "calm before");
            monkey.alarm(level, player);
            h.assertTrue(AnimalFear.isPanicking(tapir), "the alarm honk sends the tapir running");
            level.getEntitiesOfClass(LivingEntity.class, area(h), e -> !(e instanceof Player)).forEach(e -> e.discard());
        }).thenSucceed();
    }

    private JungleTest() {}
}
