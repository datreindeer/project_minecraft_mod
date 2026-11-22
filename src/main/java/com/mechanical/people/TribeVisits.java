package com.mechanical.people;

import com.mechanical.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.mechanical.CampAge;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;
import com.mechanical.NightDangers;
import com.mechanical.people.Neighbours.CampInfo;
import com.mechanical.people.Neighbours.Visit;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Tribes that seek out a player camp (CampAge) that has burned long enough. The first band comes once the camp is
 * {@link #FIRST_VISIT} old, then one about every {@link #INTERVAL} (randomised), by day and only while a player is at the
 * camp (a member of the tribe whose Fire Pit it is, in the camp zone: Clans#someoneHome): a leader (an Elder with the tribe's banner) and 3-6 others walk in from 80-120 blocks away toward the camp's fire.
 * They mill about the fire and trade; right-click the leader to talk (TribeDialog). In the evening the leader asks to stay
 * the night: yes and they keep to the fire until dawn, then leave a gift (better the higher the standing) and go; no and
 * they leave at once. After a good night they may settle nearby ({@link Settlements}): by chance (better the higher the
 * standing), and for sure the {@link #SURE_SETTLE}th band to stay the night since a tribe last settled there (CampInfo#stays).
 * A band that means to settle never gives up: with no fit spot at hand it roams round the camp ({@link Visit#SEEKING}),
 * ranging wider the longer it looks, until it finds one. Bands are checked every
 * {@link #CHECK} ticks (no scans per tick); members find their visit by id ({@link Tribesperson#visit}, VisitGoal).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TribeVisits {
    public static final long FIRST_VISIT = 3L * CampAge.DAY, INTERVAL = 5L * CampAge.DAY;
    static final int CHECK = 100, PLAYER_RANGE = 48, SPAWN_MIN = 80, SPAWN_MAX = 120, SPAWN_LOADED_MIN = 40, KNOWN = 6;
    /** Day times: bands set out between these, the leader asks at ASK, nobody answering by GIVE_UP sends them on, dawn. */
    static final long SET_OUT = 3000, SET_OUT_END = 8000, ASK = 11000, GIVE_UP = 13800, DAWN = 23000;
    static final long ARRIVE_TIMEOUT = 8000, LEAVE_TIMEOUT = 9000, LOST = 36000;
    /** The band that stays the night this many times since the last settling settles whatever the standing. */
    static final int SURE_SETTLE = 4;
    /** A band looking for land walks about this far at a time, and ranges up to this much beyond Settlements.MAX_DIST. */
    static final int ROAM_STEP = 32, ROAM_EXTRA = 100;

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK != 0) return;
        Neighbours data = Neighbours.get(level);
        long now = level.getGameTime();
        schedule(level, data, now);
        for (Visit v : List.copyOf(data.visits)) update(level, data, v, now);
        Settlements.tick(level, data, now);
        // Peoples of the World: a player inside a tribe village has seen that climate's villages
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            var village = com.mechanical.worldgen.TribeVillageStructure.villageAt(level, p.blockPosition());
            if (village != null) award(p, "tribe_climates", village.climate().id());
        }
    }

    private static long dayTime(ServerLevel level) {
        return level.getOverworldClockTime() % Calendar.CLOCK_DAY;
    }

    static List<ServerPlayer> playersNear(ServerLevel level, BlockPos pos, double range) {
        return level.players().stream().filter(p -> !p.isSpectator() && p.blockPosition().closerThan(pos, range)).toList();
    }

    // ---- when bands come ----

    private static void schedule(ServerLevel level, Neighbours data, long now) {
        long dayTime = dayTime(level);
        for (CampAge.Camp camp : CampAge.get(level).camps()) {
            if (camp.ticks < FIRST_VISIT) continue;
            CampInfo info = data.camp(camp.centre);
            if (info == null) {
                data.camps.add(new CampInfo(camp.centre, now + level.getRandom().nextInt(CampAge.DAY / 4), List.of(), -1, 0));
                data.setDirty();
                continue;
            }
            if (now < info.nextVisit || dayTime < SET_OUT || dayTime >= SET_OUT_END || data.visitAt(camp.centre) != null) continue;
            List<ServerPlayer> players = playersNear(level, camp.centre, PLAYER_RANGE);
            if (players.isEmpty() || !com.mechanical.clan.Clans.someoneHome(level, camp.centre)) continue; // they come when someone is home
            UUID tribe = chooseTribe(level, data, info, players);
            boolean started = tribe != null && start(level, data, info, tribe) != null;
            info.nextVisit = started ? now + INTERVAL - CampAge.DAY + level.getRandom().nextInt(2 * CampAge.DAY) : now + 1200;
            data.setDirty();
        }
    }

    /** Half the time a tribe that came before (if still on speaking terms and not settled), else a new one (not raiders). */
    private static UUID chooseTribe(ServerLevel level, Neighbours data, CampInfo info, List<ServerPlayer> players) {
        RandomSource r = level.getRandom();
        List<UUID> back = info.known.stream().filter(t -> data.settlement(t) == null
                && players.stream().mapToInt(Tribe.of(t)::reputation).min().orElse(0) > Tribe.HOSTILE + 20).toList();
        if (!back.isEmpty() && r.nextBoolean()) return back.get(r.nextInt(back.size()));
        UUID id;
        do id = new UUID(r.nextLong(), r.nextLong()); while (Tribe.of(id).temperament == Tribe.Temperament.RAIDERS);
        return id;
    }

    /** The lit fire nearest the camp's centre, or the centre. */
    static BlockPos campFire(ServerLevel level, BlockPos centre) {
        BlockPos best = centre;
        double bestDist = CampAge.RADIUS * CampAge.RADIUS;
        for (BlockPos f : NightDangers.litFires(level)) {
            double d = f.distSqr(centre);
            if (d <= bestDist) { bestDist = d; best = f; }
        }
        return best;
    }

    /** A band of {@code tribe} sets out for the camp from a loaded spot 80-120 (at least 40) blocks away; null if there is none. */
    static Visit start(ServerLevel level, Neighbours data, CampInfo info, UUID tribe) {
        RandomSource r = level.getRandom();
        BlockPos fire = campFire(level, info.camp);
        BlockPos from = null;
        double angle = r.nextDouble() * Math.PI * 2;
        for (int a = 0; a < 8 && from == null; a++, angle += 2.4) {
            for (int dist = SPAWN_MIN + r.nextInt(SPAWN_MAX - SPAWN_MIN + 1); dist >= SPAWN_LOADED_MIN && from == null; dist -= 10) {
                BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        fire.offset((int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist)));
                if (level.isPositionEntityTicking(p) && level.getFluidState(p).isEmpty() && level.getFluidState(p.below()).isEmpty()) from = p;
            }
        }
        if (from == null) return null;
        Visit v = new Visit(UUID.randomUUID(), tribe, info.camp, from, fire, Visit.ARRIVING, level.getGameTime(),
                level.getOverworldClockTime() / Calendar.CLOCK_DAY, false, Optional.empty(), 0);
        int size = 3 + r.nextInt(4);
        for (int i = 0; i <= size; i++) {
            TribeRole role = i == 0 ? TribeRole.ELDER : switch (r.nextInt(4)) {
                case 0, 1 -> TribeRole.HUNTER;
                case 2 -> TribeRole.GATHERER;
                default -> TribeRole.WOODCUTTER;
            };
            Tribesperson t = spawn(level, from.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2), role, null, tribe, i == 0);
            if (t != null) t.visit = v.id;
        }
        data.visits.add(v);
        info.known.remove(tribe);
        info.known.add(tribe);
        while (info.known.size() > KNOWN) info.known.remove(0);
        data.setDirty();
        Component dir = direction(fire, from);
        for (ServerPlayer p : playersNear(level, info.camp, PLAYER_RANGE))
            p.sendSystemMessage(Component.translatable("message.mechanical.visit.approach", dir).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        return v;
    }

    /** A new tribesperson standing on the ground at {@code at}. */
    static Tribesperson spawn(ServerLevel level, BlockPos at, TribeRole role, BlockPos home, UUID tribe, boolean leader) {
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        Tribesperson t = People.TRIBESPERSON.get().create(level, EntitySpawnReason.EVENT);
        if (t == null) return null;
        t.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        t.setup(role, home, tribe, leader);
        t.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
        level.addFreshEntity(t);
        return t;
    }

    static Component direction(BlockPos from, BlockPos to) {
        return Component.translatable("message.mechanical.raid.dir." + Direction.getApproximateNearest(to.getX() - from.getX(), 0, to.getZ() - from.getZ()).getName());
    }

    // ---- a band at the camp ----

    static List<Tribesperson> members(ServerLevel level, Visit v) {
        return level.getEntitiesOfClass(Tribesperson.class, new AABB(v.fire).inflate(160, 64, 160), t -> v.id.equals(t.visit) && t.isAlive());
    }

    private static void setState(Neighbours data, Visit v, int state, long now) {
        v.state = state;
        v.since = now;
        data.setDirty();
    }

    private static void update(ServerLevel level, Neighbours data, Visit v, long now) {
        List<Tribesperson> members = members(level, v);
        if (members.isEmpty()) {
            // gone (walked off, or all fell), or just not loaded: forget it once it can't matter any more (a band looking for land
            // only when its spot is loaded and nobody is there: unloaded it just waits)
            if (v.state == Visit.LEAVING || level.isPositionEntityTicking(v.fire) && now - v.since > 600 || v.state != Visit.SEEKING && now - v.since > LOST) {
                data.visits.remove(v);
                data.setDirty();
            }
            return;
        }
        Tribesperson leader = members.stream().filter(Tribesperson::isLeader).findFirst().orElse(null);
        if (leader == null) (leader = members.get(0)).setLeader(true);
        long clock = level.getOverworldClockTime(), dayTime = clock % Calendar.CLOCK_DAY;
        switch (v.state) {
            case Visit.ARRIVING -> {
                if (leader.distanceToSqr(Vec3.atCenterOf(v.fire)) < 10 * 10) arrive(level, data, v, leader, now);
                else if (now - v.since > ARRIVE_TIMEOUT) setState(data, v, Visit.LEAVING, now);
            }
            case Visit.AT_CAMP -> {
                if (!v.asked && dayTime >= ASK && dayTime < GIVE_UP) {
                    v.asked = true;
                    data.setDirty();
                    for (ServerPlayer p : playersNear(level, v.fire, PLAYER_RANGE))
                        leader.say(p, Component.translatable("chat.mechanical.visit.ask", leader.tribeName()));
                } else if (dayTime >= GIVE_UP || dayTime < SET_OUT || clock / Calendar.CLOCK_DAY > v.day) {
                    // nobody answered (or they came too late to ask): they move on, no hard feelings
                    for (ServerPlayer p : playersNear(level, v.fire, PLAYER_RANGE)) leader.say(p, Component.translatable("chat.mechanical.visit.move_on"));
                    setState(data, v, Visit.LEAVING, now);
                }
            }
            case Visit.STAYING -> {
                if (clock >= (v.day + 1) * Calendar.CLOCK_DAY - (Calendar.CLOCK_DAY - DAWN)) dawn(level, data, v, leader, members, now);
            }
            case Visit.LEAVING -> {
                if (now - v.since > LEAVE_TIMEOUT) members.forEach(Tribesperson::discard); // walked out of sight
            }
            case Visit.SEEKING -> seek(level, data, v, leader, members, now);
            default -> {}
        }
    }

    private static void arrive(ServerLevel level, Neighbours data, Visit v, Tribesperson leader, long now) {
        v.fire = campFire(level, v.camp);
        setState(data, v, Visit.AT_CAMP, now);
        Tribe tribe = Tribe.of(v.tribe);
        for (ServerPlayer p : playersNear(level, v.fire, PLAYER_RANGE)) {
            leader.say(p, Component.translatable("chat.mechanical.visit.greet." + tribe.temperament.id(), tribe.name()));
            award(p, "tribe_visit");
        }
    }

    /** A player answered the leader's question (TribeDialog): first answer counts. */
    static void answerStay(ServerPlayer player, Tribesperson leader, boolean yes) {
        ServerLevel level = (ServerLevel) leader.level();
        Neighbours data = Neighbours.get(level);
        Visit v = data.visit(leader.visit);
        if (v == null || !leader.isLeader() || v.state != Visit.AT_CAMP || !v.asked) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.visit.answered"));
            return;
        }
        Tribe tribe = Tribe.of(v.tribe);
        long now = level.getGameTime();
        List<ServerPlayer> near = playersNear(level, v.fire, PLAYER_RANGE);
        if (yes) {
            v.host = Optional.of(player.getUUID());
            setState(data, v, Visit.STAYING, now);
            tribe.addReputation(player, Tribe.STAY);
        } else {
            setState(data, v, Visit.LEAVING, now);
            tribe.addReputation(player, Tribe.REFUSE);
        }
        for (ServerPlayer p : near) {
            if (p != player) p.sendSystemMessage(Component.translatable(yes ? "message.mechanical.visit.invited" : "message.mechanical.visit.sent_away",
                    player.getDisplayName(), tribe.name()).withStyle(ChatFormatting.GRAY));
            leader.say(p, Component.translatable(yes ? "chat.mechanical.visit.thanks" : "chat.mechanical.visit.refused"));
        }
    }

    /** Dawn after a night by the fire: the gift, then they go on, or settle nearby. */
    static void dawn(ServerLevel level, Neighbours data, Visit v, Tribesperson leader, List<Tribesperson> members, long now) {
        Tribe tribe = Tribe.of(v.tribe);
        ServerPlayer host = v.host.map(id -> level.getServer().getPlayerList().getPlayer(id)).orElse(null);
        int rep = host != null ? tribe.reputation(host) : tribe.temperament.start;
        List<ItemStack> gift = gift(rep, level.getRandom());
        boolean handed = host != null && host.level() == level && host.distanceToSqr(leader) < 32 * 32;
        for (ItemStack s : gift) {
            if (handed) {
                if (!host.getInventory().add(s)) host.spawnAtLocation(level, s);
            } else {
                level.addFreshEntity(new ItemEntity(level, v.fire.getX() + 0.5, v.fire.getY() + 1.2, v.fire.getZ() + 1.5, s));
            }
        }
        if (host != null) award(host, "tribe_guests");
        for (ServerPlayer p : playersNear(level, v.fire, PLAYER_RANGE))
            leader.say(p, Component.translatable(handed && p == host ? "chat.mechanical.visit.gift" : "chat.mechanical.visit.gift_left"));
        // a good night may make them want to stay in these parts; the 4th band since the last to settle stays for sure
        CampInfo info = data.camp(v.camp);
        float chance = rep >= 70 ? 0.5F : rep >= Tribe.FRIENDLY ? 0.35F : rep >= 0 ? 0.15F : 0;
        if (info == null) {
            setState(data, v, Visit.LEAVING, now);
            return;
        }
        boolean settle = Settlements.canSettleNear(data, info, now) && (info.stays >= SURE_SETTLE - 1 || level.getRandom().nextFloat() < chance);
        info.stays = settle ? 0 : info.stays + 1;
        data.setDirty();
        if (!settle) {
            setState(data, v, Visit.LEAVING, now);
            return;
        }
        BlockPos site = Settlements.findSite(level, data, v.camp);
        if (site != null) {
            settle(level, data, v, site, leader, members, now);
            return;
        }
        // no fit spot in sight: they look about for one (seek), and say so
        for (ServerPlayer p : playersNear(level, v.fire, PLAYER_RANGE)) leader.say(p, Component.translatable("chat.mechanical.visit.seek"));
        v.tries = 0;
        setState(data, v, Visit.SEEKING, now);
    }

    /** The band founds its camp at {@code site}; players near the leader hear where (seen from the player camp). */
    private static void settle(ServerLevel level, Neighbours data, Visit v, BlockPos site, Tribesperson leader, List<Tribesperson> members, long now) {
        Settlements.found(level, data, v, site, members, now);
        CampInfo info = data.camp(v.camp);
        if (info != null) info.lastSettled = now;
        Component dir = direction(v.camp, site);
        for (ServerPlayer p : playersNear(level, leader.blockPosition(), PLAYER_RANGE)) leader.say(p, Component.translatable("chat.mechanical.visit.settle", dir));
        data.visits.remove(v);
        data.setDirty();
    }

    /**
     * A band looking for land (every CHECK ticks while loaded): a fit spot round the camp or where they are, and they found
     * their camp there; else on to the next spot of their walk round the camp. They give up only if the camp's neighbours
     * fill up meanwhile (Settlements.MAX_NEAR).
     */
    static void seek(ServerLevel level, Neighbours data, Visit v, Tribesperson leader, List<Tribesperson> members, long now) {
        v.since = now; // last seen
        data.setDirty();
        if (data.settlementsNear(v.camp, Settlements.AREA).size() >= Settlements.MAX_NEAR) {
            setState(data, v, Visit.LEAVING, now);
            return;
        }
        int maxDist = Settlements.MAX_DIST + Math.min(ROAM_EXTRA, v.tries * 5);
        BlockPos site = Settlements.findSite(level, data, v.camp, v.camp, Settlements.MIN_DIST, maxDist, maxDist);
        if (site == null) site = Settlements.findSite(level, data, v.camp, leader.blockPosition(), 0, ROAM_STEP, maxDist);
        if (site != null) {
            settle(level, data, v, site, leader, members, now);
            return;
        }
        // there, or stuck a while: on to the next spot
        if (leader.distanceToSqr(Vec3.atBottomCenterOf(v.fire)) < 8 * 8 || (now / CHECK) % 6 == 0) {
            v.fire = nextSpot(level, v, leader.blockPosition(), maxDist);
            v.tries++;
        }
    }

    /** The next spot of a band's walk round the camp: about ROAM_STEP on (always the same way round), drifting in or out, loaded and dry; else where it was. */
    static BlockPos nextSpot(ServerLevel level, Visit v, BlockPos at, int maxDist) {
        RandomSource r = level.getRandom();
        double dx = at.getX() - v.camp.getX(), dz = at.getZ() - v.camp.getZ(), dist = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx), way = (v.id.getLeastSignificantBits() & 1) == 0 ? 1 : -1;
        for (int i = 0; i < 8; i++) {
            double d = Math.max(Settlements.MIN_DIST, Math.min(maxDist, dist + r.nextInt(41) - 15)); // out a little more often than in
            double a = angle + way * ROAM_STEP / d + r.nextGaussian() * 0.2 * i;
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    v.camp.offset((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d)));
            if (level.isPositionEntityTicking(p) && level.getFluidState(p).isEmpty() && level.getFluidState(p.below()).isEmpty()) return p;
        }
        return v.fire;
    }

    /** A thank-you gift: two everyday things from strangers, three better ones from friends, plus a rare one when honoured. */
    static List<ItemStack> gift(int rep, RandomSource r) {
        List<Supplier<ItemStack>> plain = List.of(() -> new ItemStack(Mechanical.CORDAGE.get(), 3), () -> new ItemStack(Items.FLINT, 6),
                () -> new ItemStack(Mechanical.BONE_ARROW.get(), 4), () -> new ItemStack(Wildlife.COOKED_VENISON_SCRAPS.get(), 4),
                () -> new ItemStack(Mechanical.RAW_HIDE.get(), 2), () -> new ItemStack(Mechanical.TWINE.get(), 4),
                () -> new ItemStack(Foraging.BERRIES.get("blueberry").get(), 8));
        List<Supplier<ItemStack>> good = List.of(() -> new ItemStack(Mechanical.SCRAPED_HIDE.get(), 3), () -> new ItemStack(Mechanical.BONE_NEEDLE.get()),
                () -> new ItemStack(Mechanical.FLINT_KNIFE.get()), () -> new ItemStack(Mechanical.BONE_ARROW.get(), 8),
                () -> new ItemStack(Wildlife.SINEW.get(), 3), () -> new ItemStack(Items.LEATHER, 2), () -> new ItemStack(Wildlife.ANTLER.get(), 2),
                () -> new ItemStack(Mechanical.BONE_FISH_HOOK.get(), 3));
        List<Supplier<ItemStack>> rare = List.of(() -> new ItemStack(Mechanical.CRUDE_BOW.get()), () -> new ItemStack(Mechanical.TUSK_DAGGER.get()),
                () -> new ItemStack(Items.POTATO, 3), () -> new ItemStack(com.mechanical.BronzeOres.MALACHITE.get(), 3),
                () -> new ItemStack(com.mechanical.BronzeOres.CASSITERITE.get(), 2), () -> new ItemStack(com.mechanical.Casting.COPPER_KNIFE_BLADE.get()));
        List<Supplier<ItemStack>> pool = new ArrayList<>(rep >= Tribe.FRIENDLY ? good : plain);
        List<ItemStack> out = new ArrayList<>();
        for (int i = rep >= Tribe.FRIENDLY ? 3 : 2; i > 0 && !pool.isEmpty(); i--) out.add(pool.remove(r.nextInt(pool.size())).get());
        if (rep >= 70) out.add(rare.get(r.nextInt(rare.size())).get());
        return out;
    }

    // ---- talking ----

    /** Right-click on an envoy: what they say and which buttons (TribeDialog). */
    static void openDialog(ServerPlayer player, Tribesperson t) {
        Tribe tribe = t.tribe();
        if (tribe == null) return;
        ServerLevel level = (ServerLevel) t.level();
        Visit v = Neighbours.get(level).visit(t.visit);
        boolean trades = tribe.trades(player);
        int buttons = trades ? TribeDialog.TRADE : 0;
        String key;
        Object arg = tribe.name();
        if (v != null && v.state == Visit.AT_CAMP && v.asked) {
            key = "ask";
            buttons |= TribeDialog.YES_NO;
        } else if (v != null && v.state == Visit.STAYING) {
            key = "staying";
        } else if (v != null && v.state == Visit.SEEKING) {
            key = "seeking";
        } else if (v != null && v.state == Visit.LEAVING) {
            key = "leaving";
            buttons = 0;
        } else if (v != null) {
            key = "greet." + tribe.temperament.id();
        } else if (t.errand == null && t.isLeader()) { // a settled camp's leader at home: roads (RoadBuilding), no barter
            key = "leader";
            buttons = 0;
        } else if (t.role() == TribeRole.TRADER) {
            key = "caravan";
            award(player, "caravan");
        } else {
            key = "trader";
            Neighbours.Settlement s = Neighbours.get(level).settlement(t.group());
            arg = s != null ? direction(player.blockPosition(), s.centre) : Component.empty();
        }
        if (!key.equals("leaving")) buttons |= RoadBuilding.dialogButtons(player, tribe, level);
        Component text = Component.translatable("dialog.mechanical.tribe." + key, arg)
                .append("\n\n").append(Component.translatable("dialog.mechanical.tribe.standing", tribe.name(), Tribe.standing(tribe.reputation(player))));
        if (!trades) text = text.copy().append("\n").append(Component.translatable("dialog.mechanical.tribe.no_trade"));
        TribeDialog.open(player, t, text, buttons);
    }

    /** Grants the advancement mechanical:wilderness/{@code id} (not to fake players). */
    static void award(ServerPlayer player, String id) {
        award(player, id, "done");
    }

    /** Grants one criterion of mechanical:wilderness/{@code id}. */
    static void award(ServerPlayer player, String id, String criterion) {
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        var holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/" + id));
        if (holder != null) player.getAdvancements().award(holder, criterion);
    }

    /** /tribes visit: a band sets out for the camp where you stand now (ops, for testing); /tribes info: the camps nearby. */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tribes").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("visit").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getServer().overworld();
                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                    CampAge.Camp camp = CampAge.get(level).find(pos);
                    if (camp == null) camp = CampAge.get(level).burn(pos, 0);
                    Neighbours data = Neighbours.get(level);
                    CampInfo info = data.camp(camp.centre);
                    if (info == null) data.camps.add(info = new CampInfo(camp.centre, 0, List.of(), -1, 0));
                    if (data.visitAt(camp.centre) != null) {
                        ctx.getSource().sendFailure(Component.literal("A band is already visiting this camp"));
                        return 0;
                    }
                    UUID tribe = chooseTribe(level, data, info, playersNear(level, camp.centre, PLAYER_RANGE));
                    Visit v = start(level, data, info, tribe);
                    if (v == null) {
                        ctx.getSource().sendFailure(Component.literal("No loaded spot to come from"));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Tribe.of(tribe).name().copy().append(" set out from " + v.from.toShortString()), true);
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getServer().overworld();
                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                    Neighbours data = Neighbours.get(level);
                    long now = level.getGameTime();
                    for (CampInfo c : data.camps)
                        ctx.getSource().sendSuccess(() -> Component.literal("Camp " + c.camp.toShortString() + ": next band in "
                                + Math.max(0, c.nextVisit - now) + " ticks, " + c.known.size() + " tribes known"), false);
                    for (Visit v : data.visits)
                        ctx.getSource().sendSuccess(() -> Component.literal("Visit ").append(Tribe.of(v.tribe).name())
                                .append(" at " + v.fire.toShortString() + " state " + v.state + (v.asked ? " asked" : "")), false);
                    for (Neighbours.Settlement s : data.settlementsNear(pos, 400))
                        ctx.getSource().sendSuccess(() -> Settlements.describe(s), false);
                    return data.visits.size() + data.settlements.size();
                })));
    }

    private TribeVisits() {}
}
