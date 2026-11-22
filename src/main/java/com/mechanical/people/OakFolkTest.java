package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.AncientOak;
import com.mechanical.CarcassEntity;
import com.mechanical.Foraging;
import com.mechanical.KnifeItem;
import com.mechanical.OfferingAltarBlockEntity;
import com.mechanical.TestArea;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the Oak Folk (F4, OakFolk / OakQuests).
 * <ul>
 * <li>oak_quest_shared: one tribe member's hand-in moves the line on for the other, and part hand-ins add up across members.</li>
 * <li>oak_quest_reward: finishing the last quest gives the Ancient Ritual Knife and ends the line.</li>
 * <li>oak_bad_relation: striking the tree lowers the standing; at hostile the line closes for good (no hand-ins, even after gifts).</li>
 * <li>oak_ritual_knife: the ritual knife (tier 4) cuts a giant deer's heart, fine sinew and trophy skull; an iron knife does not.</li>
 * <li>oak_group_spawn: a worldgen altar (entrance set) settles its group once: keeper (antler crown, Verdant Staff), gatherers,
 * worshippers and light-bearers, all of the Oak Folk's tribe and bound to that oak.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class OakFolkTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] NAMES = {"oak_quest_shared", "oak_quest_reward", "oak_bad_relation", "oak_ritual_knife", "oak_group_spawn"};

    static {
        TEST_FUNCTIONS.register(NAMES[0], () -> OakFolkTest::questShared);
        TEST_FUNCTIONS.register(NAMES[1], () -> OakFolkTest::questReward);
        TEST_FUNCTIONS.register(NAMES[2], () -> OakFolkTest::badRelation);
        TEST_FUNCTIONS.register(NAMES[3], () -> OakFolkTest::ritualKnife);
        TEST_FUNCTIONS.register(NAMES[4], () -> OakFolkTest::groupSpawn);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "oak_folk"), new TestEnvironmentDefinition.AllOf(List.of()));
        int[] ticks = {40, 40, 40, 100, 400};
        for (int i = 0; i < NAMES.length; i++) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, NAMES[i]));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, NAMES[i]), new FunctionGameTestInstance(key, TestArea.of(env, ticks[i], true)));
        }
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static boolean carries(Player p, Item item) {
        return p.getInventory().contains(s -> s.is(item));
    }

    private static void questShared(GameTestHelper h) {
        Player a = h.makeMockPlayer(GameType.SURVIVAL), b = h.makeMockPlayer(GameType.SURVIVAL);
        Runnable disband = com.mechanical.clan.Clans.testTribe(h.getLevel().getServer(), a.getUUID(), b.getUUID());
        try {
            h.assertTrue(OakQuests.of(a).stage() == 0 && OakQuests.of(b).stage() == 0, "a fresh tribe starts at the first quest");
            a.getInventory().add(new ItemStack(Foraging.BERRIES.get("raspberry").get(), 16));
            h.assertTrue(OakQuests.handIn(a) == 16, "a hands in the 16 raspberries");
            h.assertTrue(OakQuests.of(b).stage() == 1, "the quest is done for b too (shared by the tribe)");
            h.assertTrue(carries(a, AncientOak.GLOWCAP.get().asItem()), "the one who finished it got the reward");
            b.getInventory().add(new ItemStack(Foraging.MUSHROOM_ITEMS.get("chanterelle").get(), 3));
            OakQuests.handIn(b);
            h.assertTrue(OakQuests.of(a).delivered() == 3, "b's 3 chanterelles count for a (" + OakQuests.of(a) + ")");
            a.getInventory().add(new ItemStack(Foraging.MUSHROOM_ITEMS.get("chanterelle").get(), 10));
            OakQuests.handIn(a);
            h.assertTrue(OakQuests.of(b).stage() == 2 && carries(a, Foraging.MUSHROOM_ITEMS.get("chanterelle").get()),
                    "a's 5 finish it, the 5 left over stay with a");
            Player stranger = h.makeMockPlayer(GameType.SURVIVAL);
            h.assertTrue(OakQuests.of(stranger).stage() == 0, "someone outside the tribe has a line of their own");
            OakQuests.set(a, null);
        } finally {
            disband.run();
        }
        h.succeed();
    }

    private static void questReward(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        int last = OakQuests.QUESTS.size() - 1;
        h.assertTrue(OakQuests.QUESTS.size() >= 10 && OakQuests.QUESTS.size() <= 15, "10-15 quests");
        OakQuests.set(p, new OakQuests.Progress(last, 0, false));
        int before = Tribe.of(Tribe.OAK).reputation(p);
        p.getInventory().add(new ItemStack(com.mechanical.Smithing.STEEL_INGOT.get(), 4));
        OakQuests.handIn(p);
        h.assertTrue(carries(p, OakFolk.ANCIENT_RITUAL_KNIFE.get()), "the last quest gives the Ancient Ritual Knife");
        h.assertTrue(OakQuests.of(p).finished(), "and the line is finished");
        h.assertTrue(Tribe.of(Tribe.OAK).reputation(p) == before + OakQuests.STANDING_PER_QUEST, "a finished quest raises the standing");
        p.getInventory().add(new ItemStack(com.mechanical.Smithing.STEEL_INGOT.get(), 4));
        h.assertTrue(OakQuests.handIn(p) == 0, "nothing more to hand in");
        OakQuests.set(p, null);
        h.succeed();
    }

    private static void badRelation(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        Tribe oak = Tribe.of(Tribe.OAK);
        BlockPos log = new BlockPos(8, 1, 8);
        h.setBlock(log, AncientOak.LOG.get().defaultBlockState());
        Tribesperson w = h.spawn(People.TRIBESPERSON.get(), new BlockPos(3, 1, 3));
        w.setup(TribeRole.OAK_WORSHIPPER, null, Tribe.OAK, false, ClimateZone.TEMPERATE);
        w.oakFoot = h.absolutePos(log);
        int start = oak.reputation(p);
        h.assertTrue(OakFolk.isTree(level.getBlockState(h.absolutePos(log))), "an Ancient Oak log is the tree");
        OakFolk.harmed(p, h.absolutePos(log), OakFolk.HARM_TREE);
        h.assertTrue(oak.reputation(p) == start + OakFolk.HARM_TREE, "striking the tree lowers the standing (" + oak.reputation(p) + ")");
        h.assertFalse(OakQuests.closed(p), "not closed yet");
        oak.addReputation(p, Tribe.HOSTILE - oak.reputation(p)); // hurt and killed some of them
        h.assertTrue(OakQuests.closed(p) && OakQuests.of(p).closed(), "a hostile standing closes the quest line");
        h.assertTrue(w.isAngryAt(p), "and they turn on the player");
        oak.addReputation(p, 60); // gifts afterwards
        p.getInventory().add(new ItemStack(Foraging.BERRIES.get("raspberry").get(), 16));
        h.assertTrue(OakQuests.closed(p) && OakQuests.handIn(p) == 0, "closed for good: nothing is taken even when friendly again");
        OakQuests.set(p, null);
        w.discard();
        h.succeed();
    }

    private static void ritualKnife(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        h.assertTrue(OakFolk.ANCIENT_RITUAL_KNIFE.get().tier() == KnifeItem.RITUAL && KnifeItem.RITUAL == 4 && KnifeItem.RITUAL > KnifeItem.IRON,
                "the ritual knife is tier 4, above iron");
        cut(h, level, new BlockPos(4, 1, 8), com.mechanical.Mechanical.IRON_KNIFE.get());
        cut(h, level, new BlockPos(12, 1, 8), OakFolk.ANCIENT_RITUAL_KNIFE.get());
        h.runAfterDelay(5, () -> {
            for (Item part : List.of(OakFolk.ANIMAL_HEART.get(), OakFolk.FINE_SINEW.get(), OakFolk.TROPHY_SKULL.get())) {
                h.assertTrue(count(h, level, new BlockPos(12, 1, 8), part) > 0, "the ritual knife takes " + part);
                h.assertTrue(count(h, level, new BlockPos(4, 1, 8), part) == 0, "an iron knife never takes " + part);
            }
            h.assertTrue(com.mechanical.Butchery.ritualParts(Wildlife.HARE.get()).stream().noneMatch(e -> e.item().get() == OakFolk.TROPHY_SKULL.get()),
                    "no trophy from a hare");
            h.succeed();
        });
    }

    /** Cuts up a whole giant deer carcass at {@code rel} with {@code knife}, both passes. */
    private static void cut(GameTestHelper h, ServerLevel level, BlockPos rel, KnifeItem knife) {
        var deer = Wildlife.GIANT_DEER.get().create(level, EntitySpawnReason.COMMAND);
        BlockPos at = h.absolutePos(rel);
        deer.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        CarcassEntity carcass = CarcassEntity.of(level, deer);
        level.addFreshEntity(carcass);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(knife));
        for (int i = 0; i < 200000 && !carcass.isRemoved(); i++) carcass.butcher(level, p, knife);
        h.assertTrue(carcass.isRemoved(), "the carcass is cut up");
    }

    private static int count(GameTestHelper h, ServerLevel level, BlockPos rel, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(3), e -> e.getItem().is(item)).size();
    }

    private static void groupSpawn(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        BlockPos rel = new BlockPos(8, 1, 8);
        h.setBlock(rel, AncientOak.ALTAR.get().defaultBlockState());
        BlockPos foot = h.absolutePos(rel);
        ((OfferingAltarBlockEntity) level.getBlockEntity(foot)).setEntrance(foot.east(7));
        AABB around = new AABB(foot).inflate(24);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(level.getEntitiesOfClass(Tribesperson.class, around, t -> t.role().oak()).size() == OakFolk.GROUP.length,
                        "the altar settles its group"))
                .thenExecute(() -> {
                    List<Tribesperson> folk = level.getEntitiesOfClass(Tribesperson.class, around, t -> t.role().oak());
                    for (Tribesperson t : folk) {
                        h.assertTrue(foot.equals(t.oakFoot) && Tribe.OAK.equals(t.group()) && t.home() == null, "bound to the oak, of the Oak Folk");
                        h.assertTrue(t.colour() == -1, "their own green clothes, not a tribe colour");
                    }
                    Tribesperson keeper = folk.stream().filter(t -> t.role() == TribeRole.OAK_KEEPER).findFirst().orElseThrow();
                    h.assertTrue(keeper.isLeader() && keeper.getItemBySlot(EquipmentSlot.HEAD).is(AncientOak.ANTLER_CROWN.get())
                            && keeper.getMainHandItem().is(OakFolk.VERDANT_STAFF.get()), "the keeper: antler crown and Verdant Staff");
                    for (TribeRole r : List.of(TribeRole.OAK_GATHERER, TribeRole.OAK_WORSHIPPER, TribeRole.OAK_LIGHTBEARER))
                        h.assertTrue(folk.stream().anyMatch(t -> t.role() == r), "a " + r.id());
                })
                .thenIdle(120) // once only
                .thenExecute(() -> {
                    List<Tribesperson> folk = level.getEntitiesOfClass(Tribesperson.class, new AABB(foot).inflate(64), t -> t.role().oak());
                    h.assertTrue(folk.size() == OakFolk.GROUP.length, "no second group (" + folk.size() + ")");
                    folk.forEach(Tribesperson::discard);
                })
                .thenSucceed();
    }
}
