package com.mechanical.clan;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for {@link Downed}: down instead of dead with a tribe mate online (through the real death path), dead at
 * once alone, in singleplayer or without a tribe; mobs drop a downed target, a mob's hit does nothing and a stranger's
 * finishes; a help is stopped by a hit; carried on the shoulders the clock stops; the safe-place rule; the revive (2
 * hearts, inventory kept, then the wake-up, no spirit); bleeding out and giving up kill; the spirit ends on an attack.
 * Mock players outside the player list, counted as online through Downed.TEST_ONLINE (all within this one tick).
 */
@EventBusSubscriber(modid = MODID)
public final class DownedTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "downed"));

    static {
        TEST_FUNCTIONS.register("downed", () -> DownedTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "downed"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "downed"), new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("downed: " + what);
    }

    /** A survival ServerPlayer outside the player list whose packets go nowhere (a FakePlayer's connection: FakePlayer itself won't die or ride). */
    private static ServerPlayer player(GameTestHelper h, List<ServerPlayer> made, double x, double z) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "downed-test"), ClientInformation.createDefault()) {
            @Override
            public boolean isInvulnerableTo(ServerLevel l, net.minecraft.world.damagesource.DamageSource source) {
                return false; // no client ever loads: vanilla would make it invulnerable
            }
        };
        p.connection = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "downed-test-net")).connection;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // the test server's default is creative
        p.setPos(h.absoluteVec(new Vec3(x, 1, z)));
        made.add(p);
        Downed.TEST_ONLINE.add(p);
        return p;
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        Clans data = Clans.get(server);
        List<ServerPlayer> made = new ArrayList<>();
        Clans.Clan clan = null;
        try {
            ServerPlayer a = player(h, made, 4, 4), b = player(h, made, 5, 4), stranger = player(h, made, 12, 12);
            clan = new Clans.Clan(UUID.randomUUID(), "downed test", Optional.empty(),
                    List.of(new Clans.Member(a.getUUID(), "a", Clans.LEADER, 0, 0), new Clans.Member(b.getUUID(), "b", Clans.MEMBER, 0, 0)),
                    List.of(), Optional.empty(), Optional.empty());
            data.clans.add(clan);

            // who goes down: a mate online, multiplayer
            check(h, Downed.canGoDown(data, a, List.of(a, b), true), "a mate online: goes down");
            check(h, !Downed.canGoDown(data, a, List.of(a, stranger), true), "no mate online: dies at once");
            check(h, !Downed.canGoDown(data, a, List.of(a, b), false), "singleplayer: dies at once");
            check(h, !Downed.canGoDown(data, stranger, List.of(stranger, a, b), true), "no tribe: dies at once");

            // the real death path: down, not dead (the test server is dedicated: multiplayer)
            a.getInventory().add(new ItemStack(Items.STICK, 5));
            a.die(level.damageSources().generic());
            check(h, Downed.isDowned(a) && a.getHealth() > 0 && !a.isDeadOrDying(), "with a mate online, 0 health puts you down");
            check(h, a.getForcedPose() == Pose.SLEEPING, "lies on the ground");

            // mobs leave him be; a mob's hit does nothing, a stranger's finishes (checked on another downed player below)
            Zombie z = h.spawn(EntityTypes.ZOMBIE, 8, 1, 8);
            z.setTarget(a);
            check(h, z.getTarget() == null, "a zombie won't target a downed player");
            a.hurtServer(level, level.damageSources().mobAttack(z), 6);
            check(h, Downed.isDowned(a) && a.getHealth() == 1.0F, "a mob's hit does nothing to a downed player");

            // helping up: stopped by a hit, then held through
            b.setShiftKeyDown(true);
            check(h, Downed.help(b, a), "a sneaking mate starts helping");
            for (int i = 0; i < 20; i++) {
                Downed.help(b, a);
                Downed.tick(b);
            }
            check(h, a.getVehicle() == null, "not up yet after a second");
            Downed.hurt(b);
            check(h, !Downed.HELPING.containsKey(b.getUUID()), "a hit stops the help");
            check(h, !Downed.help(stranger, a), "a stranger can't help");
            Downed.help(b, a);
            for (int i = 0; i < Downed.HELP_TICKS; i++) {
                Downed.help(b, a);
                Downed.tick(b);
            }
            check(h, a.getVehicle() == b && Downed.carrying(b) == a, "helped up onto the mate's shoulders");
            int clock = Downed.DOWNED.get(a.getUUID()).ticks;
            Downed.tick(a);
            check(h, Downed.DOWNED.get(a.getUUID()).ticks == clock, "the clock stops while carried");
            a.stopRiding();
            check(h, a.getVehicle() == b, "a downed player can't climb off");

            // the safe place: not hurt for 10 s, no hostile mob within 16
            check(h, !Downed.safe(b), "just hurt: not safe");
            Downed.LAST_HURT.put(b.getUUID(), level.getGameTime() - Downed.SAFE_HURT_TICKS - 1);
            check(h, !Downed.safe(b), "a zombie near: not safe");
            Downed.HOLDING.put(b.getUUID(), 0);
            Downed.tick(b);
            check(h, Downed.HOLDING.get(b.getUUID()) == 0, "holding R where it isn't safe does nothing");
            z.discard();
            check(h, Downed.safe(b), "safe now");

            // the revive: hold R
            for (int i = 0; i < Downed.REVIVE_TICKS; i++) Downed.tick(b);
            check(h, !Downed.isDowned(a) && a.getVehicle() == null && Downed.carrying(b) == null, "revived and put down");
            check(h, a.getHealth() == Downed.REVIVE_HEALTH && Downed.REVIVE_HEALTH == 4.0F, "wakes with 2 hearts");
            check(h, a.getInventory().countItem(Items.STICK) == 5, "keeps his inventory");
            check(h, Downed.kind(a) == Downed.WAKING && !Downed.SPIRIT.containsKey(a.getUUID()), "gets up, no spirit form after a revive");
            for (int i = 0; i < Downed.WAKE_TICKS; i++) Downed.tick(a);
            check(h, Downed.kind(a) == Downed.NONE && a.getForcedPose() == null, "up on his feet");

            // bleeding out
            a.die(level.damageSources().generic());
            check(h, Downed.isDowned(a), "down again");
            Downed.DOWNED.get(a.getUUID()).ticks = Downed.DOWN_TICKS - 1;
            Downed.tick(a);
            check(h, !Downed.isDowned(a) && a.isDeadOrDying(), "a minute without help: dead");

            // giving up (R held 1.5 s), and a stranger's hit
            ServerPlayer c = player(h, made, 6, 6), d = player(h, made, 7, 7);
            clan.members.add(new Clans.Member(c.getUUID(), "c", Clans.MEMBER, 0, 0));
            clan.members.add(new Clans.Member(d.getUUID(), "d", Clans.MEMBER, 0, 0));
            c.die(level.damageSources().generic());
            check(h, Downed.isDowned(c), "c down");
            Downed.HOLDING.put(c.getUUID(), 0);
            for (int i = 0; i < Downed.GIVE_UP_TICKS; i++) Downed.tick(c);
            check(h, !Downed.isDowned(c) && c.isDeadOrDying(), "holding R gives up: dead");
            d.die(level.damageSources().generic());
            check(h, Downed.isDowned(d), "d down");
            d.hurtServer(level, level.damageSources().playerAttack(stranger), 1);
            check(h, !Downed.isDowned(d) && d.isDeadOrDying(), "a stranger's hit finishes a downed player");

            // spirit form: woken at world spawn, mobs don't see him until he attacks
            ServerPlayer e = player(h, made, 9, 9);
            Downed.startWake(e, null, true);
            for (int i = 0; i < Downed.WAKE_TICKS; i++) Downed.tick(e);
            Zombie z2 = h.spawn(EntityTypes.ZOMBIE, 10, 1, 10);
            z2.setTarget(e);
            check(h, Downed.SPIRIT.containsKey(e.getUUID()) && z2.getTarget() == null, "a spirit isn't targeted");
            NeoForge.EVENT_BUS.post(new AttackEntityEvent(e, z2));
            z2.setTarget(e);
            check(h, !Downed.SPIRIT.containsKey(e.getUUID()) && z2.getTarget() == e, "attacking ends the spirit form");
            z2.discard();
        } finally {
            if (clan != null) data.clans.remove(clan);
            Downed.TEST_ONLINE.clear();
            for (ServerPlayer p : made) {
                Downed.DOWNED.remove(p.getUUID());
                Downed.WAKES.remove(p.getUUID());
                Downed.HELPING.remove(p.getUUID());
                Downed.HOLDING.remove(p.getUUID());
                Downed.SPIRIT.remove(p.getUUID());
                Downed.LAST_HURT.remove(p.getUUID());
            }
        }
        h.succeed();
    }
}
