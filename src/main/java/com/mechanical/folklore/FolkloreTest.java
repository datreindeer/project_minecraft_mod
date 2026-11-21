package com.mechanical.folklore;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test: Faith and sight (Second Sight, thresholds, kept bounds) and the Glimmering Stump gift (one a day, answered the next morning). */
@EventBusSubscriber(modid = MODID)
public final class FolkloreTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "folklore"));

    static {
        TEST_FUNCTIONS.register("folklore", () -> FolkloreTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "folklore"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "folklore"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("folklore: " + what);
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "folklore_test"));
        // sight: Faith + 50 under Second Sight
        check(h, Faith.of(player) == 0 && !Faith.canSee(player, Faith.STUMP_SIGHT), "a new player believes nothing");
        player.addEffect(new MobEffectInstance(Folklore.SECOND_SIGHT, 400));
        check(h, Faith.sight(player) == Faith.SECOND_SIGHT_BONUS && Faith.canSee(player, Faith.WATCHER_SIGHT), "the mushroom opens the eyes");
        player.removeEffect(Folklore.SECOND_SIGHT);
        Faith.set(player, 250);
        check(h, Faith.of(player) == Faith.MAX, "faith capped at 100");
        Faith.set(player, 0);

        // the gift: berries at the stump, once a day; the present comes the morning after
        BlockPos stump = h.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(stump, Folklore.GLIMMERING_STUMP.get().defaultBlockState());
        check(h, Faith.offer(player, stump, 100) && Faith.of(player) == Faith.GIFT, "a gift gives +3 faith");
        check(h, !Faith.offer(player, stump, 100) && Faith.of(player) == Faith.GIFT, "only one gift a day");
        check(h, Faith.answerGift(player, level, 100, false) == null, "no present the same day");
        check(h, Faith.answerGift(player, level, 101, true) == null, "no present in the night");
        ItemEntity present = Faith.answerGift(player, level, 101, false);
        check(h, present != null && present.blockPosition().equals(stump), "a present on the stump the next morning");
        check(h, Faith.answerGift(player, level, 102, false) == null, "a gift is answered once");
        present.discard();

        // first sight of a kind: +5 once
        check(h, Faith.seen(player, "test_being", "folklore/above_the_clouds") && !Faith.seen(player, "test_being", "folklore/above_the_clouds")
                && Faith.of(player) == Faith.GIFT + Faith.NEW_KIND, "a new kind seen gives +5 once");
        check(h, Faith.takeFeather(player, 3) && !Faith.takeFeather(player, 3) && Faith.takeFeather(player, 4), "one feather a year");
        h.succeed();
    }

    private FolkloreTest() {}
}
