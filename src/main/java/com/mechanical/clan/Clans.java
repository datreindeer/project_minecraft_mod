package com.mechanical.clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mechanical.Calendar;
import com.mechanical.CampAge;
import com.mechanical.FirePit;
import com.mechanical.FirePitBlock;
import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Player tribes (docs/seasons-and-tribes.md; "Clan" in code, "Tribe" in game, since people.Tribe is the NPC tribes).
 * A tribe is founded at a Fire Pit, its centre: while the tribe lasts that pit can't be broken or blown up. Ranks
 * Leader, Elder, Member, Newcomer (a Newcomer becomes a Member after {@link #NEWCOMER_DAYS}). The leader and Elders
 * invite; the leader kicks, promotes, demotes and hands over leadership, an Elder may kick Newcomers. The leader can only
 * leave after handing over. Votes (3 days, 2/3 of the tribe): move the Fire Pit or disband (members who don't vote count
 * as no), or, when the leader hasn't been seen for {@link #ABSENT_DAYS}, a new leader (only votes cast count). The
 * tribe shares all mod advancements (and so their recipes): one member's advancement is every member's, offline ones
 * get them when they log in, and joining merges both ways. Its age is the latest age it has reached (WorldAge), and the world's age follows the tribes'. No friendly fire.
 * The leader picks the tribe's colour (a dye colour); the Tab list (client.clan.TribeTabList) groups players by tribe.
 * The locator bar shows only tribe mates (mixin.LivingEntityWaypointMixin). The Fire Pit keeps the chunks round it loaded
 * ({@link #keepPitsLoaded}), and visits and raids only come while a member is in the camp ({@link #someoneHome}). Members share
 * one standing with each NPC tribe (A4, {@link #sharedStandings}): joining folds yours into the average, leaving keeps it. Screen: client.clan.ClanScreen (G), network: {@link ClanNetwork}, lang: scripts/gen_clans.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Clans extends SavedData {
    public static final int LEADER = 0, ELDER = 1, MEMBER = 2, NEWCOMER = 3;
    public static final String[] RANKS = {"leader", "elder", "member", "newcomer"};
    public static final int VOTE_DISBAND = 0, VOTE_MOVE = 1, VOTE_LEADER = 2;
    public static final String[] VOTES = {"disband", "move", "leader"};
    public static final int VOTE_DAYS = 3, NEWCOMER_DAYS = 7, ABSENT_DAYS = 10, NAME_MAX = 24;
    /** How far from a Fire Pit you may found a tribe or make it the centre. */
    static final int PIT_REACH = 4;
    public static final String[] AGES = com.mechanical.WorldAge.AGES;
    static final Identifier FOUNDED = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/found_tribe");
    static final Identifier GREW = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/grow_tribe");
    /** Chunks a tribe's Fire Pit keeps loaded each way from its own: 1 is 3x3, which covers the camp zone (CampAge.RADIUS). */
    public static final int LOAD_RADIUS = 1;
    static final TicketController PIT_TICKETS = new TicketController(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_pit"), Clans::validateTickets);

    public static final class Member {
        final UUID id;
        String name;
        int rank;
        long joinedDay, lastSeenDay;

        Member(UUID id, String name, int rank, long joinedDay, long lastSeenDay) {
            this.id = id;
            this.name = name;
            this.rank = rank;
            this.joinedDay = joinedDay;
            this.lastSeenDay = lastSeenDay;
        }

        static final Codec<Member> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(m -> m.id), Codec.STRING.fieldOf("name").forGetter(m -> m.name),
                Codec.INT.fieldOf("rank").forGetter(m -> m.rank), Codec.LONG.fieldOf("joined").forGetter(m -> m.joinedDay),
                Codec.LONG.fieldOf("seen").forGetter(m -> m.lastSeenDay)).apply(i, Member::new));
    }

    public static final class Vote {
        final int kind;
        final UUID candidate;
        final long endsDay;
        final Map<UUID, Boolean> ballots;

        Vote(int kind, UUID candidate, long endsDay, Map<UUID, Boolean> ballots) {
            this.kind = kind;
            this.candidate = candidate;
            this.endsDay = endsDay;
            this.ballots = new HashMap<>(ballots);
        }

        static final Codec<Vote> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("kind").forGetter(v -> v.kind), UUIDUtil.CODEC.fieldOf("candidate").forGetter(v -> v.candidate),
                Codec.LONG.fieldOf("ends").forGetter(v -> v.endsDay),
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.BOOL).fieldOf("ballots").forGetter(v -> v.ballots)).apply(i, Vote::new));
    }

    public static final class Clan {
        final UUID id;
        String name;
        Optional<GlobalPos> pit;
        final List<Member> members;
        final Set<Identifier> advancements;
        Optional<Vote> vote;
        /** A DyeColor id, picked by the leader on the tribe page; tribes from before it had one get one from their id. */
        int colour;
        /** A4: the standing with each NPC tribe (tribe id -> -100..100) that all members share (people.Tribe#standings). */
        final Map<String, Integer> reputation;
        /** Members whose own standings are folded into {@link #reputation} ({@link #pool}); older saves fold members in as they show up. */
        final Set<UUID> pooled;

        Clan(UUID id, String name, Optional<GlobalPos> pit, List<Member> members, List<Identifier> advancements, Optional<Vote> vote,
             Optional<Integer> colour) {
            this(id, name, pit, members, advancements, vote, colour, Map.of(), List.of());
        }

        Clan(UUID id, String name, Optional<GlobalPos> pit, List<Member> members, List<Identifier> advancements, Optional<Vote> vote,
             Optional<Integer> colour, Map<String, Integer> reputation, List<UUID> pooled) {
            this.reputation = new HashMap<>(reputation);
            this.pooled = new HashSet<>(pooled);
            this.id = id;
            this.name = name;
            this.pit = pit;
            this.members = new ArrayList<>(members);
            this.advancements = new HashSet<>(advancements);
            this.vote = vote;
            this.colour = colour.filter(c -> c >= 0 && c < 16).orElse(Math.floorMod(id.hashCode(), 16));
        }

        static final Codec<Clan> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(c -> c.id), Codec.STRING.fieldOf("name").forGetter(c -> c.name),
                GlobalPos.CODEC.optionalFieldOf("pit").forGetter(c -> c.pit),
                Member.CODEC.listOf().fieldOf("members").forGetter(c -> c.members),
                Identifier.CODEC.listOf().fieldOf("advancements").forGetter(c -> List.copyOf(c.advancements)),
                Vote.CODEC.optionalFieldOf("vote").forGetter(c -> c.vote),
                Codec.INT.optionalFieldOf("colour").forGetter(c -> Optional.of(c.colour)),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("reputation", Map.of()).forGetter(c -> c.reputation),
                UUIDUtil.CODEC.listOf().optionalFieldOf("pooled", List.of()).forGetter(c -> List.copyOf(c.pooled))).apply(i, Clan::new));

        public String name() {
            return name;
        }

        public List<Member> members() {
            return members;
        }

        Member member(UUID player) {
            for (Member m : members) if (m.id.equals(player)) return m;
            return null;
        }

        Member leader() {
            for (Member m : members) if (m.rank == LEADER) return m;
            return members.getFirst();
        }

        /** Index into {@link #AGES}: the latest age the tribe has reached (WorldAge). */
        public int age() {
            return com.mechanical.WorldAge.of(advancements);
        }

        public int colour() {
            return colour;
        }

        /**
         * A4: folds a member's own standings into the tribe's, which become the average over all members folded in so far
         * (decided: merging takes the average). A tribe missing on either side counts at that tribe's starting standing.
         */
        boolean pool(UUID player, Map<String, Integer> own) {
            if (!pooled.add(player)) return false;
            int n = pooled.size();
            Set<String> keys = new HashSet<>(reputation.keySet());
            keys.addAll(own.keySet());
            for (String key : keys) {
                int start;
                try {
                    start = com.mechanical.people.Tribe.of(UUID.fromString(key)).temperament.start;
                } catch (IllegalArgumentException e) {
                    continue; // not a tribe id
                }
                int shared = reputation.getOrDefault(key, start), mine = own.getOrDefault(key, start);
                reputation.put(key, Math.round((shared * (n - 1) + mine) / (float) n));
            }
            return true;
        }

        /** Chunks the Fire Pit keeps loaded each way. shortcut: always LOAD_RADIUS; the big Fire Pit (F12) makes it 2 (5x5). */
        int loadRadius() {
            return LOAD_RADIUS;
        }
    }

    private static final Codec<Clans> CODEC = Clan.CODEC.listOf().xmap(Clans::new, d -> d.clans);
    private static final SavedDataType<Clans> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "clans"),
            () -> new Clans(List.of()), CODEC);

    final List<Clan> clans;
    /** Fire Pits this keeps loaded -> radius. Not saved: NeoForge saves the tickets, validateTickets refills this on load. */
    final Map<GlobalPos, Integer> loaded = new HashMap<>();
    /** Open invitations: invited player -> tribe ids. Not saved: they lapse on a restart. */
    final Map<UUID, Set<UUID>> invites = new HashMap<>();
    /** A4: standings an offline player left with (left, kicked, disbanded), given to them at login. shortcut: not saved, lost on a restart; save it if that bites. */
    final Map<UUID, Map<String, Integer>> leftWith = new HashMap<>();

    Clans(List<Clan> clans) {
        this.clans = new ArrayList<>(clans);
    }

    public static Clans get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static long today(MinecraftServer server) {
        return Calendar.day(server.overworld().getOverworldClockTime());
    }

    /** Are both players in the same tribe? (Locator bar: mixin.LivingEntityWaypointMixin.) */
    public static boolean sameTribe(ServerPlayer a, ServerPlayer b) {
        Clan clan = get(a.level().getServer()).clanOf(a.getUUID());
        return clan != null && clan.member(b.getUUID()) != null;
    }

    /** Tribes changed: rebuild every player's locator bar links, so tribe mates appear and others vanish at once. */
    static void refreshLocator(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            var waypoints = sp.level().getWaypointManager();
            waypoints.removePlayer(sp);
            waypoints.addPlayer(sp);
        }
    }

    public Clan clanOf(UUID player) {
        for (Clan c : clans) if (c.member(player) != null) return c;
        return null;
    }

    Clan byId(UUID id) {
        for (Clan c : clans) if (c.id.equals(id)) return c;
        return null;
    }

    boolean isClaimed(GlobalPos pit) {
        for (Clan c : clans) if (c.pit.isPresent() && c.pit.get().equals(pit)) return true;
        return false;
    }

    /** The controller of the nearest Fire Pit within reach that no tribe has, or null. */
    GlobalPos freePitNear(ServerPlayer player) {
        BlockPos at = player.blockPosition();
        GlobalPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-PIT_REACH, -2, -PIT_REACH), at.offset(PIT_REACH, 2, PIT_REACH))) {
            BlockState state = player.level().getBlockState(p);
            if (!FirePit.isPit(state)) continue;
            GlobalPos pit = GlobalPos.of(player.level().dimension(), FirePitBlock.controller(state, p).immutable());
            double d = pit.pos().distSqr(at);
            if (d < bestDist && !isClaimed(pit)) {
                best = pit;
                bestDist = d;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- actions (server; each returns whether it did anything)

    boolean found(ServerPlayer player, String name) {
        name = name.strip();
        if (clanOf(player.getUUID()) != null || name.isEmpty() || name.length() > NAME_MAX || nameTaken(name)) return false;
        GlobalPos pit = freePitNear(player);
        if (pit == null) return false;
        long today = today(player.level().getServer());
        Clan clan = new Clan(UUID.randomUUID(), name, Optional.of(pit),
                List.of(new Member(player.getUUID(), player.getGameProfile().name(), LEADER, today, today)), List.of(), Optional.empty(), Optional.empty());
        clans.add(clan);
        invites.remove(player.getUUID());
        mergeIn(player, clan);
        award(player, FOUNDED);
        setDirty();
        return true;
    }

    private boolean nameTaken(String name) {
        for (Clan c : clans) if (c.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    boolean claimPit(ServerPlayer player) {
        Clan clan = clanOf(player.getUUID());
        if (clan == null || clan.pit.isPresent() || clan.member(player.getUUID()).rank != LEADER) return false;
        GlobalPos pit = freePitNear(player);
        if (pit == null) return false;
        clan.pit = Optional.of(pit);
        tell(player.level().getServer(), clan, Component.translatable("message.mechanical.clan.new_centre", player.getName()));
        setDirty();
        return true;
    }

    /** The leader picks the tribe's colour (a DyeColor id), shown on the Tab list. */
    boolean colour(ServerPlayer player, int colour) {
        Clan clan = clanOf(player.getUUID());
        if (clan == null || clan.member(player.getUUID()).rank != LEADER || colour < 0 || colour >= 16 || colour == clan.colour) return false;
        clan.colour = colour;
        setDirty();
        return true;
    }

    boolean invite(ServerPlayer actor, UUID target) {
        Clan clan = clanOf(actor.getUUID());
        ServerPlayer invited = actor.level().getServer().getPlayerList().getPlayer(target);
        if (clan == null || invited == null || clan.member(actor.getUUID()).rank > ELDER || clanOf(target) != null) return false;
        invites.computeIfAbsent(target, k -> new HashSet<>()).add(clan.id);
        invited.sendSystemMessage(Component.translatable("message.mechanical.clan.invited", actor.getName(), clan.name).withStyle(ChatFormatting.GOLD));
        tell(actor.level().getServer(), clan, Component.translatable("message.mechanical.clan.invite_sent", actor.getName(), invited.getName()));
        return true;
    }

    boolean accept(ServerPlayer player, UUID clanId) {
        Set<UUID> mine = invites.get(player.getUUID());
        Clan clan = byId(clanId);
        if (clan == null || mine == null || !mine.contains(clanId) || clanOf(player.getUUID()) != null) return false;
        invites.remove(player.getUUID());
        long today = today(player.level().getServer());
        clan.members.add(new Member(player.getUUID(), player.getGameProfile().name(), NEWCOMER, today, today));
        tell(player.level().getServer(), clan, Component.translatable("message.mechanical.clan.joined", player.getName(), clan.name));
        mergeIn(player, clan);
        if (clan.members.size() >= 2) for (ServerPlayer sp : online(player.level().getServer(), clan)) award(sp, GREW);
        setDirty();
        return true;
    }

    boolean decline(ServerPlayer player, UUID clanId) {
        Set<UUID> mine = invites.get(player.getUUID());
        return mine != null && mine.remove(clanId);
    }

    boolean leave(ServerPlayer player) {
        Clan clan = clanOf(player.getUUID());
        if (clan == null || clan.member(player.getUUID()).rank == LEADER) return false;
        remove(player.level().getServer(), clan, player.getUUID(), "message.mechanical.clan.left");
        return true;
    }

    boolean kick(ServerPlayer actor, UUID target) {
        Clan clan = clanOf(actor.getUUID());
        Member a = clan == null ? null : clan.member(actor.getUUID()), t = clan == null ? null : clan.member(target);
        if (t == null || t == a || !(a.rank == LEADER || a.rank == ELDER && t.rank == NEWCOMER)) return false;
        remove(actor.level().getServer(), clan, target, "message.mechanical.clan.kicked");
        return true;
    }

    void remove(MinecraftServer server, Clan clan, UUID player, String message) {
        Member m = clan.member(player);
        tell(server, clan, Component.translatable(message, m.name, clan.name));
        keepStandings(server, clan, player);
        clan.members.remove(m);
        clan.pooled.remove(player);
        clan.vote.ifPresent(v -> v.ballots.remove(player));
        setDirty();
    }

    /** Newcomer -> Member -> Elder (leader only). */
    boolean promote(ServerPlayer actor, UUID target) {
        Member t = byLeader(actor, target);
        if (t == null || t.rank <= ELDER) return false;
        t.rank--;
        rankChanged(actor.level().getServer(), clanOf(target), t);
        return true;
    }

    /** Elder -> Member -> Newcomer (leader only). */
    boolean demote(ServerPlayer actor, UUID target) {
        Member t = byLeader(actor, target);
        if (t == null || t.rank == LEADER || t.rank == NEWCOMER) return false;
        t.rank++;
        rankChanged(actor.level().getServer(), clanOf(target), t);
        return true;
    }

    boolean handOver(ServerPlayer actor, UUID target) {
        Member t = byLeader(actor, target);
        if (t == null || t.rank == LEADER) return false;
        Clan clan = clanOf(target);
        clan.member(actor.getUUID()).rank = ELDER;
        t.rank = LEADER;
        rankChanged(actor.level().getServer(), clan, t);
        return true;
    }

    /** The target member, if the actor leads the target's tribe. */
    private Member byLeader(ServerPlayer actor, UUID target) {
        Clan clan = clanOf(actor.getUUID());
        if (clan == null || clan.member(actor.getUUID()).rank != LEADER) return null;
        return clan.member(target);
    }

    private void rankChanged(MinecraftServer server, Clan clan, Member m) {
        tell(server, clan, Component.translatable("message.mechanical.clan.rank", m.name, Component.translatable("gui.mechanical.clan.rank." + RANKS[m.rank])));
        setDirty();
        ClanNetwork.sendRoster(server);
    }

    boolean canCallLeaderVote(MinecraftServer server, Clan clan) {
        Member leader = clan.leader();
        return clan.members.size() > 1 && today(server) - leader.lastSeenDay >= ABSENT_DAYS && server.getPlayerList().getPlayer(leader.id) == null;
    }

    boolean startVote(ServerPlayer player, int kind, UUID candidate) {
        MinecraftServer server = player.level().getServer();
        Clan clan = clanOf(player.getUUID());
        if (clan == null || clan.vote.isPresent()) return false;
        switch (kind) {
            case VOTE_DISBAND -> candidate = player.getUUID();
            case VOTE_MOVE -> {
                if (clan.pit.isEmpty()) return false;
                candidate = player.getUUID();
            }
            case VOTE_LEADER -> {
                Member c = candidate == null ? null : clan.member(candidate);
                if (c == null || c.rank == LEADER || !canCallLeaderVote(server, clan)) return false;
            }
            default -> {
                return false;
            }
        }
        Vote vote = new Vote(kind, candidate, today(server) + VOTE_DAYS, Map.of(player.getUUID(), true));
        clan.vote = Optional.of(vote);
        Member c = clan.member(candidate);
        tell(server, clan, Component.translatable("message.mechanical.clan.vote_started", player.getName(),
                Component.translatable("gui.mechanical.clan.vote." + VOTES[kind], c.name)).withStyle(ChatFormatting.GOLD));
        setDirty();
        settle(server, clan, false);
        return true;
    }

    boolean ballot(ServerPlayer player, boolean yes) {
        Clan clan = clanOf(player.getUUID());
        if (clan == null || clan.vote.isEmpty() || clan.vote.get().kind == VOTE_LEADER && clan.member(player.getUUID()).rank == LEADER) return false;
        clan.vote.get().ballots.put(player.getUUID(), yes);
        setDirty();
        settle(player.level().getServer(), clan, false);
        return true;
    }

    /** Who may vote: everyone, except the (absent) leader in a leadership vote. */
    static int electorate(Clan clan, Vote v) {
        return v.kind == VOTE_LEADER ? clan.members.size() - 1 : clan.members.size();
    }

    static int count(Clan clan, Vote v, boolean yes) {
        int n = 0;
        for (Member m : clan.members) if (Boolean.valueOf(yes).equals(v.ballots.get(m.id))) n++;
        return n;
    }

    /**
     * Decide the vote if it can be decided: passed once 2/3 of the electorate say yes; failed once that can't happen
     * any more, or when time runs out (then a leadership vote passes with 2/3 of the votes cast).
     */
    void settle(MinecraftServer server, Clan clan, boolean timeUp) {
        if (clan.vote.isEmpty()) return;
        Vote v = clan.vote.get();
        int voters = electorate(clan, v), yes = count(clan, v, true), no = count(clan, v, false);
        boolean pass = yes * 3 >= voters * 2 || timeUp && v.kind == VOTE_LEADER && yes + no > 0 && yes * 3 >= (yes + no) * 2;
        boolean fail = !pass && ((voters - no) * 3 < voters * 2 || timeUp);
        if (!pass && !fail) return;
        clan.vote = Optional.empty();
        setDirty();
        Member candidate = clan.member(v.candidate);
        Component what = Component.translatable("gui.mechanical.clan.vote." + VOTES[v.kind], candidate == null ? "?" : candidate.name);
        tell(server, clan, Component.translatable(pass ? "message.mechanical.clan.vote_passed" : "message.mechanical.clan.vote_failed", what).withStyle(ChatFormatting.GOLD));
        if (!pass) return;
        ClanNetwork.sendRoster(server);
        switch (v.kind) {
            case VOTE_DISBAND -> {
                tell(server, clan, Component.translatable("message.mechanical.clan.disbanded", clan.name));
                for (Member m : clan.members) keepStandings(server, clan, m.id);
                clans.remove(clan);
                refreshLocator(server);
            }
            case VOTE_MOVE -> clan.pit = Optional.empty();
            case VOTE_LEADER -> {
                if (candidate == null) return;
                clan.leader().rank = ELDER;
                candidate.rank = LEADER;
                ClanNetwork.sendRoster(server);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- the camp lives on (docs/living-world-plan.md A2, A3)

    /**
     * Each tribe's Fire Pit force-loads its chunk and the ones round it ({@link Clan#loadRadius}), so crops, animals and
     * furnaces carry on with nobody there; pits no tribe has any more (disbanded, moved, broken) are let go. A pit that is
     * gone (only commands can do that) leaves the tribe without a centre, as a move vote does.
     */
    void keepPitsLoaded(MinecraftServer server) {
        Map<GlobalPos, Integer> want = new HashMap<>();
        for (Clan c : clans) {
            if (c.pit.isEmpty()) continue;
            GlobalPos pit = c.pit.get();
            ServerLevel level = server.getLevel(pit.dimension());
            if (level == null) continue;
            if (level.isLoaded(pit.pos()) && !FirePit.isPit(level.getBlockState(pit.pos()))) {
                c.pit = Optional.empty();
                setDirty();
                continue;
            }
            want.put(pit, c.loadRadius());
        }
        for (var e : List.copyOf(loaded.entrySet())) {
            if (e.getValue().equals(want.get(e.getKey()))) continue;
            force(server, e.getKey(), e.getValue(), false);
            loaded.remove(e.getKey());
        }
        want.forEach((pit, r) -> {
            if (loaded.putIfAbsent(pit, r) == null) force(server, pit, r, true);
        });
    }

    private static void force(MinecraftServer server, GlobalPos pit, int radius, boolean add) {
        ServerLevel level = server.getLevel(pit.dimension());
        if (level == null) return;
        ChunkPos c = ChunkPos.containing(pit.pos());
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++) PIT_TICKETS.forceChunk(level, pit.pos(), c.x() + x, c.z() + z, add, false);
    }

    /** On load: keep the tickets of pits that are still a tribe's (and note them), drop the rest. */
    static void validateTickets(ServerLevel level, TicketHelper tickets) {
        Clans data = get(level.getServer());
        tickets.getBlockTickets().forEach((pos, set) -> {
            GlobalPos pit = GlobalPos.of(level.dimension(), pos);
            if (!data.isClaimed(pit)) {
                tickets.removeAllTickets(pos);
                return;
            }
            ChunkPos c = ChunkPos.containing(pos);
            int r = 0;
            for (long chunk : set.normal()) r = Math.max(r, Math.max(Math.abs(ChunkPos.getX(chunk) - c.x()), Math.abs(ChunkPos.getZ(chunk) - c.z())));
            data.loaded.put(pit, r);
        });
    }

    @SubscribeEvent
    static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(PIT_TICKETS);
    }

    /**
     * Is someone home at the camp centred on {@code centre} (A3: tribe visits and raids only come then)? A member of the
     * tribe whose Fire Pit is in the camp zone ({@link CampAge#RADIUS}) standing in it; at a camp that is no tribe's, anyone.
     */
    public static boolean someoneHome(ServerLevel level, BlockPos centre) {
        return get(level.getServer()).homeAt(level.dimension(), centre, level.players());
    }

    boolean homeAt(ResourceKey<Level> dimension, BlockPos centre, List<? extends Player> players) {
        Clan owner = null;
        for (Clan c : clans)
            if (c.pit.isPresent() && c.pit.get().dimension() == dimension && c.pit.get().pos().closerThan(centre, CampAge.RADIUS)) owner = c;
        for (Player p : players)
            if (!p.isSpectator() && p.blockPosition().closerThan(centre, CampAge.RADIUS) && (owner == null || owner.member(p.getUUID()) != null)) return true;
        return false;
    }

    // ---------------------------------------------------------------- shared advancements

    static boolean shared(Identifier id) {
        return id.getNamespace().equals(Mechanical.MODID);
    }

    /** A player joins or founds: the tribe gets what they have, then everyone gets what the tribe has. */
    private void mergeIn(ServerPlayer player, Clan clan) {
        clan.pool(player.getUUID(), player.getData(com.mechanical.people.Tribe.REPUTATION));
        for (var holder : player.level().getServer().getAdvancements().getAllAdvancements())
            if (shared(holder.id()) && player.getAdvancements().getOrStartProgress(holder).isDone()) clan.advancements.add(holder.id());
        for (ServerPlayer sp : online(player.level().getServer(), clan)) catchUp(sp, clan);
    }

    /** Award a player every tribe advancement they lack. */
    static void catchUp(ServerPlayer player, Clan clan) {
        var manager = player.level().getServer().getAdvancements();
        for (Identifier id : List.copyOf(clan.advancements)) {
            var holder = manager.get(id);
            if (holder == null) continue;
            var progress = player.getAdvancements().getOrStartProgress(holder);
            if (progress.isDone()) continue;
            for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(holder, criterion);
        }
    }

    static void award(ServerPlayer player, Identifier id) {
        var holder = player.level().getServer().getAdvancements().get(id);
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    @SubscribeEvent
    static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !shared(event.getAdvancement().id())) return;
        Clans data = get(player.level().getServer());
        Clan clan = data.clanOf(player.getUUID());
        // already the tribe's: this is the share reaching another member, not a new one
        if (clan != null && clan.advancements.add(event.getAdvancement().id())) {
            data.setDirty();
            for (ServerPlayer sp : online(player.level().getServer(), clan)) if (sp != player) catchUp(sp, clan);
        }
        for (int a = 1; a < AGES.length; a++)
            if (com.mechanical.WorldAge.MARKERS[a].equals(event.getAdvancement().id())) ClanNetwork.sendRoster(player.level().getServer());
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Clans data = get(player.level().getServer());
        Clan clan = data.clanOf(player.getUUID());
        ClanNetwork.sendRoster(player.level().getServer());
        Map<String, Integer> kept = data.leftWith.remove(player.getUUID());
        if (kept != null && clan == null) player.setData(com.mechanical.people.Tribe.REPUTATION, kept);
        if (clan == null) return;
        if (clan.pool(player.getUUID(), player.getData(com.mechanical.people.Tribe.REPUTATION))) data.setDirty();
        Member m = clan.member(player.getUUID());
        m.name = player.getGameProfile().name();
        m.lastSeenDay = today(player.level().getServer());
        data.setDirty();
        catchUp(player, clan);
        clan.vote.ifPresent(v -> player.sendSystemMessage(Component.translatable("message.mechanical.clan.vote_open").withStyle(ChatFormatting.GOLD)));
    }

    /** The Tab list's online counts and the Wanderers group. */
    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ClanNetwork.sendRoster(player.level().getServer(), player);
    }

    /** Daily chores, checked every 5 s: who was seen today, Newcomers growing into Members, votes running out. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 100 != 0) return;
        MinecraftServer server = level.getServer();
        Clans data = get(server);
        data.keepPitsLoaded(server);
        long today = today(server);
        for (Clan clan : List.copyOf(data.clans)) {
            for (Member m : clan.members) {
                boolean on = server.getPlayerList().getPlayer(m.id) != null;
                if (on && m.lastSeenDay != today) {
                    m.lastSeenDay = today;
                    data.setDirty();
                }
                if (m.rank == NEWCOMER && today - m.joinedDay >= NEWCOMER_DAYS) {
                    m.rank = MEMBER;
                    data.rankChanged(server, clan, m);
                }
            }
            if (clan.vote.isPresent() && today >= clan.vote.get().endsDay) data.settle(server, clan, true);
        }
    }

    /** A tribe's Fire Pit can't be broken, by anyone, while the tribe lasts. */
    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !FirePit.isPit(event.getState())) return;
        if (get(level.getServer()).isClaimed(GlobalPos.of(level.dimension(), FirePitBlock.controller(event.getState(), event.getPos())))) {
            event.setCanceled(true);
            event.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.clan.pit_protected"));
        }
    }

    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Clans data = get(level.getServer());
        event.getAffectedBlocks().removeIf(p -> {
            BlockState s = level.getBlockState(p);
            return FirePit.isPit(s) && data.isClaimed(GlobalPos.of(level.dimension(), FirePitBlock.controller(s, p)));
        });
    }

    /** No friendly fire: tribe members can't hurt each other, by hand or with arrows and spears. */
    @SubscribeEvent
    static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Projectile p) attacker = p.getOwner();
        if (!(attacker instanceof Player hitter) || hitter == victim) return;
        Clan clan = get(victim.level().getServer()).clanOf(victim.getUUID());
        if (clan != null && clan.member(hitter.getUUID()) != null) event.setCanceled(true);
    }

    // ---------------------------------------------------------------- standings with NPC tribes, shared by the tribe (A4)

    /** A member leaving keeps the tribe's standings as their own (now, or at their next login). */
    private void keepStandings(MinecraftServer server, Clan clan, UUID player) {
        Map<String, Integer> kept = Map.copyOf(clan.reputation);
        ServerPlayer sp = server.getPlayerList().getPlayer(player);
        if (sp != null) sp.setData(com.mechanical.people.Tribe.REPUTATION, kept);
        else leftWith.put(player, kept);
    }

    /**
     * The NPC-tribe standings a player shares with their tribe (read only; write with {@link #setShared}), or null outside
     * a tribe (their own count then). A member not folded in yet (saves from before A4) is folded in here.
     */
    public static Map<String, Integer> sharedStandings(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        Clans data = get(level.getServer());
        Clan clan = data.clanOf(player.getUUID());
        if (clan == null) return null;
        if (clan.pool(player.getUUID(), player.getData(com.mechanical.people.Tribe.REPUTATION))) data.setDirty();
        return java.util.Collections.unmodifiableMap(clan.reputation);
    }

    /** Sets the tribe's shared standing with one NPC tribe; false when the player is in no tribe. */
    public static boolean setShared(Player player, String tribe, int value) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Clans data = get(level.getServer());
        Clan clan = data.clanOf(player.getUUID());
        if (clan == null) return false;
        clan.reputation.put(tribe, value);
        data.setDirty();
        return true;
    }

    /** Whose standing it is: the player's tribe's, or the player's own (for rate limits, and one reward per tribe). */
    public static UUID sharerOf(Player player) {
        Clan clan = player.level() instanceof ServerLevel level ? get(level.getServer()).clanOf(player.getUUID()) : null;
        return clan != null ? clan.id : player.getUUID();
    }

    // ---------------------------------------------------------------- helpers

    static List<ServerPlayer> online(MinecraftServer server, Clan clan) {
        List<ServerPlayer> out = new ArrayList<>();
        for (Member m : clan.members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(m.id);
            if (sp != null) out.add(sp);
        }
        return out;
    }

    static void tell(MinecraftServer server, Clan clan, Component message) {
        for (ServerPlayer sp : online(server, clan)) sp.sendSystemMessage(message);
    }

    /** The invitations a player has, tribe id -> name, in order. */
    Map<UUID, String> invitesFor(UUID player) {
        Map<UUID, String> out = new LinkedHashMap<>();
        for (UUID id : invites.getOrDefault(player, Set.of())) {
            Clan c = byId(id);
            if (c != null) out.put(id, c.name);
        }
        return out;
    }
}
