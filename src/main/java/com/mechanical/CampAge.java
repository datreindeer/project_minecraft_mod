package com.mechanical;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Camp age ("hearth time"): how long fires have burned at a camp. A camp is the area within {@link #RADIUS} blocks of
 * where its first fire burned; any burning campfire or lit kiln inside it adds to the same count (time, not fires: two
 * fires burn no faster, except that a burning Fire Pit counts {@link FirePit#CAMP_AGE} times), so a new fire next to the old one carries on and a fire far away starts a new camp. Only fires
 * that are no tribe's home count (fires only burn in loaded chunks, i.e. near players). Old camps draw raids (people.Raids).
 * Sneak and right-click a campfire or kiln with an empty hand to see it; /camp for ops.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class CampAge extends SavedData {
    public static final int RADIUS = 24, DAY = 24000, HOUR = 1000;
    private static final int STEP = 100, TRIBE_CHECK = 32;

    public static final class Camp {
        public final BlockPos centre;
        public long ticks;
        /** Night (game time / 24000) of the last raid on this camp, so it is raided at most once a night. */
        public long raidedNight;

        Camp(BlockPos centre, long ticks, long raidedNight) {
            this.centre = centre;
            this.ticks = ticks;
            this.raidedNight = raidedNight;
        }

        static final Codec<Camp> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("centre").forGetter(c -> c.centre),
                Codec.LONG.fieldOf("ticks").forGetter(c -> c.ticks),
                Codec.LONG.optionalFieldOf("raided_night", -1L).forGetter(c -> c.raidedNight)).apply(i, Camp::new));
    }

    private static final Codec<CampAge> CODEC = Camp.CODEC.listOf().xmap(CampAge::new, d -> d.camps);
    private static final SavedDataType<CampAge> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "camp_age"),
            () -> new CampAge(List.of()), CODEC);

    private final List<Camp> camps;

    private CampAge(List<Camp> camps) {
        this.camps = new ArrayList<>(camps);
    }

    public static CampAge get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Camp> camps() {
        return camps;
    }

    /** The camp whose area holds {@code pos} (the nearest centre within RADIUS), or null. */
    public Camp find(BlockPos pos) {
        Camp best = null;
        double bestDist = RADIUS * RADIUS;
        for (Camp c : camps) {
            double d = c.centre.distSqr(pos);
            if (d <= bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    /** Adds {@code ticks} of burning at {@code pos}: to the camp there, or to a new camp centred on it. */
    public Camp burn(BlockPos pos, long ticks) {
        Camp camp = find(pos);
        if (camp == null) camps.add(camp = new Camp(pos.immutable(), 0, -1));
        camp.ticks += ticks;
        setDirty();
        return camp;
    }

    public void markRaided(Camp camp, long night) {
        camp.raidedNight = night;
        setDirty();
    }

    /** "3 days 4 h". */
    public static Component describe(long ticks) {
        return Component.translatable("message.mechanical.camp.duration", ticks / DAY, ticks % DAY / HOUR);
    }

    public static Component report(ServerLevel level, BlockPos pos) {
        Camp camp = get(level).find(pos);
        if (camp == null) return Component.translatable("message.mechanical.camp.none");
        return Component.translatable(camp.ticks >= com.mechanical.people.Raids.RAID_AGE ? "message.mechanical.camp.age_noticed" : "message.mechanical.camp.age",
                describe(camp.ticks));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % STEP != 0) return;
        Set<Camp> burning = new HashSet<>(), pits = new HashSet<>();
        CampAge data = null;
        for (BlockPos fire : NightDangers.litFires(level)) {
            // a tribe's own fire is no player camp
            if (!level.getEntitiesOfClass(com.mechanical.people.Tribesperson.class, new AABB(fire).inflate(TRIBE_CHECK), t -> fire.equals(t.home())).isEmpty()) continue;
            if (data == null) data = get(level);
            Camp camp = data.find(fire);
            if (camp == null) camp = data.burn(fire, 0);
            burning.add(camp);
            if (NightDangers.reach(level, fire) > 1) pits.add(camp);
        }
        for (Camp camp : burning) camp.ticks += pits.contains(camp) ? (long) STEP * FirePit.CAMP_AGE : STEP; // a Fire Pit ages the camp faster
        if (data != null && !burning.isEmpty()) data.setDirty();
    }

    /** Sneak + right-click a campfire or kiln with an empty hand: how long this camp has burned. */
    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isSecondaryUseActive() || event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty()) return;
        var state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.CAMPFIRE) && !(state.getBlock() instanceof KilnBlock) && !FirePit.isPit(state)) return;
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(report(level, event.getPos()));
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** /camp: camp age where you stand (ops). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("camp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
            Camp camp = get(level).find(pos);
            Component where = camp == null ? Component.empty() : Component.literal(" (centre " + camp.centre.toShortString() + ", " + camp.ticks + " ticks)");
            ctx.getSource().sendSuccess(() -> report(level, pos).copy().append(where), false);
            return camp == null ? 0 : 1;
        }));
    }
}
