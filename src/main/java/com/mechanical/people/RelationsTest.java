package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.CarcassEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for relations with NPC tribes (docs/living-world-plan.md C; the shared standing, A4, is in clan.ClanTest).
 * <ul>
 * <li>tribe_carcass_gift: a carcass put down at a camp of a tribe you stand Neutral or better with raises the standing by its
 * meat (more for bigger game), and one of them comes for it; a second one the same day is taken but counts for nothing;
 * below Neutral it is a threat (- standing, their armed folk watch you); away from any camp it is nothing.</li>
 * <li>tribe_defence: a visiting band's warrior goes for a monster in the camp zone round the player's fire, further from
 * him than his own guard range; without the visit he would leave it be.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class RelationsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> GIFT = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_carcass_gift"));
    private static final ResourceKey<Consumer<GameTestHelper>> DEFENCE = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tribe_defence"));

    static {
        TEST_FUNCTIONS.register("tribe_carcass_gift", () -> RelationsTest::gift);
        TEST_FUNCTIONS.register("tribe_defence", () -> RelationsTest::defence);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "relations"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_carcass_gift"), new FunctionGameTestInstance(GIFT, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tribe_defence"), new FunctionGameTestInstance(DEFENCE, com.mechanical.TestArea.of(env, 200, true)));
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++)
            for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    /** A tribe id whose temperament starts at or above (true) or below (false) Neutral. */
    private static UUID tribe(boolean neutral) {
        for (int i = 0; ; i++) {
            UUID id = UUID.nameUUIDFromBytes(("relations" + i).getBytes());
            if (Tribe.of(id).temperament.start >= 0 == neutral) return id;
        }
    }

    /** A dead animal's body lying at relative {@code at}. */
    private static CarcassEntity carcass(GameTestHelper h, net.minecraft.world.entity.EntityType<? extends Mob> animal, BlockPos at) {
        Mob dead = h.spawn(animal, at);
        CarcassEntity c = CarcassEntity.of(h.getLevel(), dead);
        dead.discard();
        h.getLevel().addFreshEntity(c);
        return c;
    }

    private static void gift(GameTestHelper h) {
        floor(h);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos fire = h.absolutePos(new BlockPos(8, 1, 8));

        // a friendly camp: a gift raises the standing by the meat, and one of them comes for it
        UUID friendly = tribe(true);
        Tribe ft = Tribe.of(friendly);
        Tribesperson host = h.spawn(People.TRIBESPERSON.get(), new BlockPos(9, 1, 9));
        host.setup(TribeRole.GATHERER, fire, friendly, false);
        CarcassEntity hare = carcass(h, EntityTypes.RABBIT, new BlockPos(5, 1, 8)), cow = carcass(h, EntityTypes.COW, new BlockPos(11, 1, 8));
        h.assertTrue(CarcassGifts.worth(cow) > CarcassGifts.worth(hare), "a cow is worth more than a rabbit");
        int before = ft.reputation(player);
        h.assertTrue(CarcassGifts.left(cow, player), "a carcass at the camp is a gift");
        h.assertTrue(ft.reputation(player) == before + CarcassGifts.worth(cow), "gift raises the standing by its worth");
        h.assertTrue(host.killPos != null, "someone comes for it");
        h.assertTrue(!CarcassGifts.left(cow, player), "the same body counts once");
        // rate limit: the second gift today is taken with thanks but counts for nothing
        int after = ft.reputation(player);
        h.assertTrue(CarcassGifts.left(hare, player), "a second gift is still taken");
        h.assertTrue(ft.reputation(player) == after, "only the first gift a day counts");

        // away from any camp: nothing
        CarcassEntity far = carcass(h, EntityTypes.PIG, new BlockPos(8, 1, 8));
        far.setPos(far.getX() + 40, far.getY(), far.getZ());
        h.assertTrue(!CarcassGifts.left(far, player), "no camp, no gift");

        // a camp that dislikes you: a threat
        host.discard();
        UUID wary = tribe(false);
        Tribe wt = Tribe.of(wary);
        Tribesperson guard = h.spawn(People.TRIBESPERSON.get(), new BlockPos(9, 1, 9));
        guard.setup(TribeRole.HUNTER, fire, wary, false);
        CarcassEntity pig = carcass(h, EntityTypes.PIG, new BlockPos(7, 1, 8));
        int rep = wt.reputation(player);
        h.assertTrue(CarcassGifts.left(pig, player), "a carcass below Neutral is noticed");
        h.assertTrue(wt.reputation(player) == rep + CarcassGifts.THREAT, "below Neutral it is a threat");
        h.assertTrue(!pig.gifted, "a threat is not taken");
        for (CarcassEntity c : List.of(hare, cow, far, pig)) c.discard();
        guard.discard();
        h.succeed();
    }

    private static void defence(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos fire = h.absolutePos(new BlockPos(8, 1, 8));
        h.setBlock(new BlockPos(8, 1, 8), Blocks.CAMPFIRE.defaultBlockState());
        UUID band = tribe(true);
        Neighbours data = Neighbours.get(level);
        Neighbours.Visit v = new Neighbours.Visit(UUID.randomUUID(), band, fire, fire, fire, Neighbours.Visit.AT_CAMP, level.getGameTime(), 0, false, Optional.empty(), 0);
        data.visits.add(v);
        Tribesperson warrior = h.spawn(People.TRIBESPERSON.get(), new BlockPos(1, 1, 8));
        warrior.setup(TribeRole.HUNTER, null, band, false);
        Mob husk = h.spawn(EntityTypes.HUSK, new BlockPos(15, 1, 8));
        husk.setNoAi(true);
        h.assertTrue(warrior.distanceToSqr(husk) > 12 * 12, "the monster is beyond his own guard range");
        h.assertTrue(!warrior.defends(husk), "not visiting, he leaves it be");
        warrior.visit = v.id;
        h.assertTrue(warrior.defends(husk), "visiting, he guards the camp zone round the fire");
        h.succeedWhen(() -> {
            h.assertTrue(warrior.getTarget() == husk, "the warrior goes for the monster near the fire");
            data.visits.remove(v);
            warrior.discard();
            husk.discard();
        });
    }

    private RelationsTest() {}
}
