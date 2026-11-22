package com.mechanical.people;

import java.util.List;

import com.mechanical.Calendar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/**
 * Life round a fire (docs/living-world-plan.md C): one per tribesperson. Sitting round the fire, idle actions (whittling,
 * checking the weapon, rummaging in the bag, eating, talking to each other, stretching, warming the hands), the night on
 * a Straw Bed laid out on the ground near the fire (taken up again on waking), and the warriors' watch. Driven by
 * DailyRhythmGoal (camp folk) and VisitGoal (a band at a player's fire); the idle action shows through
 * {@link Tribesperson#idle()} (synced, posed by client.people.TribeIdlePoses).
 */
public final class CampLife {
    public static final int NONE = 0, WHITTLE = 1, CHECK_WEAPON = 2, RUMMAGE = 3, EAT = 4, TALK = 5, STRETCH = 6, WARM_HANDS = 7;
    /** Added to the synced idle value while seated on the ground. */
    public static final int SITTING = 16;
    static final int MORNING = 0, DAY = 1, EVENING = 2, NIGHT = 3;
    static final double SEAT_R = 2.6, BED_R = 3, GUARD_R = 6;

    private final Tribesperson mob;
    /** For game tests: the part of the day this person acts on (-1: the real clock). */
    int forcedPhase = -1;
    private int action;
    private long actionUntil;
    private boolean sitting;
    private Vec3 lookAt;
    private Tribesperson partner;
    /** The foot of the Straw Bed this person laid out (or is walking to lay out), or null. */
    BlockPos bed;
    private Direction bedFacing;
    private FishGoal fishing;
    /** While fishing on a visit: where they stand and the water they cast at (others sit by them). */
    BlockPos fishStand, fishSpot;
    private int post, postTicks;

    CampLife(Tribesperson mob) {
        this.mob = mob;
    }

    // ---- the day ----

    /** Morning (0-1500 on the clock: eat, stretch), day (work), evening (10500-12500: at the fire), night (sleep, guards). */
    int phase() {
        if (forcedPhase >= 0) return forcedPhase;
        long t = mob.level().getOverworldClockTime() % Calendar.CLOCK_DAY;
        return t < 1500 ? MORNING : t < 10500 ? DAY : t < 12500 ? EVENING : NIGHT;
    }

    /** Hunters and guards keep watch at night; everyone else sleeps. */
    boolean warrior() {
        return mob.role() == TribeRole.HUNTER || mob.role() == TribeRole.GUARD;
    }

    // ---- every tick (Tribesperson.customServerAiStep, after the goals, before the look control) ----

    void tick() {
        ServerLevel level = (ServerLevel) mob.level();
        long now = level.getGameTime();
        if (mob.isSleeping()) {
            float yaw = bedFacing != null ? bedFacing.toYRot() : mob.getYRot();
            mob.setYRot(yaw);
            mob.yBodyRot = mob.yHeadRot = yaw;
            set(NONE);
            return;
        }
        boolean busy = mob.getTarget() != null || mob.getTradingPlayer() != null || mob.isWorking() || mob.isInWater();
        if (busy || mob.getNavigation().isInProgress()) {
            if (busy || !sitting) stand();
            set(NONE);
        } else if (action != NONE && now > actionUntil) set(NONE);
        else if (action == NONE && mob.getRandom().nextInt(sitting ? 120 : 300) == 0) idle(level, now);
        if (action == TALK && partner != null) {
            if (!partner.isAlive() || partner.distanceToSqr(mob) > 5 * 5) set(NONE);
            else mob.getLookControl().setLookAt(partner, 30, 30);
        } else if (lookAt != null) mob.getLookControl().setLookAt(lookAt);
        if (sitting) mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
        sync();
    }

    /** A new idle action fitting where they are: seated ones whittle, standing ones stretch, by a fire both warm their hands. */
    private void idle(ServerLevel level, long now) {
        BlockPos fire = fireNear();
        int[] choices = sitting ? new int[]{WHITTLE, WHITTLE, EAT, TALK, TALK, RUMMAGE, fire != null ? WARM_HANDS : WHITTLE, mob.isArmed() ? CHECK_WEAPON : EAT}
                : new int[]{STRETCH, RUMMAGE, TALK, EAT, fire != null ? WARM_HANDS : STRETCH, mob.isArmed() ? CHECK_WEAPON : RUMMAGE};
        int a = choices[mob.getRandom().nextInt(choices.length)];
        if (a == TALK) {
            List<Tribesperson> near = level.getEntitiesOfClass(Tribesperson.class, mob.getBoundingBox().inflate(4),
                    t -> t != mob && t.inGroup(mob) && !t.isSleeping() && t.life.action == NONE && t.getTarget() == null && !t.getNavigation().isInProgress());
            if (near.isEmpty()) a = sitting ? WHITTLE : STRETCH;
            else {
                Tribesperson other = near.get(mob.getRandom().nextInt(near.size()));
                long until = now + 100 + mob.getRandom().nextInt(120);
                start(TALK, until);
                partner = other;
                other.life.start(TALK, until);
                other.life.partner = mob;
                other.life.sync();
                return;
            }
        }
        if (a == WARM_HANDS && fire != null) lookAt = Vec3.atCenterOf(fire);
        start(a, now + 80 + mob.getRandom().nextInt(120));
    }

    void start(int action, long until) {
        this.action = action;
        actionUntil = until;
        if (action != TALK) partner = null;
    }

    /** Starts {@code action} now for {@code ticks} (DailyRhythmGoal's morning). */
    void start(int action, int ticks) {
        start(action, mob.level().getGameTime() + ticks);
    }

    int action() {
        return action;
    }

    boolean sitting() {
        return sitting;
    }

    private void set(int action) {
        this.action = action;
        if (action == NONE) partner = null;
    }

    private void sync() {
        mob.setIdle(action | (sitting ? SITTING : 0));
    }

    /** The camp's own fire, or the fire of the visit, within 4 blocks; or null. */
    private BlockPos fireNear() {
        BlockPos fire = mob.home() != null ? mob.home() : mob.visitFire();
        return fire != null && mob.distanceToSqr(Vec3.atCenterOf(fire)) < 4 * 4 ? fire : null;
    }

    // ---- sitting ----

    /** A spot of their own {@code r} blocks from {@code fire} (golden angle by id, so a group spreads round it). */
    Vec3 seat(BlockPos fire, double r) {
        double a = (mob.getId() * 2.399) % (Math.PI * 2);
        return new Vec3(fire.getX() + 0.5 + Math.cos(a) * r, fire.getY(), fire.getZ() + 0.5 + Math.sin(a) * r);
    }

    /** Walks to {@code seat}, then sits down there facing {@code face}. True once seated. */
    boolean sit(Vec3 seat, Vec3 face) {
        if (!approach(seat, 1.0)) {
            stand();
            return false;
        }
        mob.getNavigation().stop();
        if (mob.isWorking()) mob.setWorking(false);
        sitting = true;
        lookAt = face;
        return true;
    }

    /** Walks to {@code spot} and stands there facing {@code face}. True once there. */
    boolean standAt(Vec3 spot, Vec3 face) {
        sitting = false;
        if (!approach(spot, 1.2)) return false;
        mob.getNavigation().stop();
        lookAt = face;
        return true;
    }

    void stand() {
        sitting = false;
        lookAt = null;
    }

    /**
     * Walks to {@code p}; true once within {@code r}. The path counts a block's distance as there, so the last steps go
     * straight through the move control.
     */
    private boolean approach(Vec3 p, double r) {
        if (at(p, r)) return true;
        if (at(p, 2.5) && mob.getNavigation().isDone()) mob.getMoveControl().setWantedPosition(p.x, p.y, p.z, 0.5);
        else if (mob.getNavigation().isDone() || mob.tickCount % 60 == 0) mob.getNavigation().moveTo(p.x, p.y, p.z, 0.6);
        return false;
    }

    private boolean at(Vec3 p, double r) {
        double dx = mob.getX() - p.x, dz = mob.getZ() - p.z;
        return dx * dx + dz * dz < r * r && Math.abs(mob.getY() - p.y) < 1.5;
    }

    // ---- the night ----

    /** Sleeps through the night on a Straw Bed laid out near {@code fire}: finds a spot, walks there, lays it out, lies down. */
    void sleep(BlockPos fire) {
        ServerLevel level = (ServerLevel) mob.level();
        stand();
        if (mob.isSleeping()) return;
        if (bed != null && !placed(level) && !free(level, bed, bedFacing)) bed = null; // someone else laid theirs out there
        if (bed == null && !pickBed(level, fire)) {
            sit(seat(fire, SEAT_R), Vec3.atCenterOf(fire)); // no room for a bed: doze sitting by the fire
            return;
        }
        if (!placed(level)) {
            if (!approach(Vec3.atBottomCenterOf(bed), 1.2)) return;
            BlockState s = Blocks.STRAW_BED.defaultBlockState().setValue(AbstractBedBlock.FACING, bedFacing);
            level.setBlock(bed, s.setValue(AbstractBedBlock.PART, BedPart.FOOT), Block.UPDATE_ALL);
            level.setBlock(bed.relative(bedFacing), s.setValue(AbstractBedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
        }
        mob.getNavigation().stop();
        mob.startSleeping(bed.relative(bedFacing));
        for (var p : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, mob.getBoundingBox().inflate(16)))
            TribeVisits.award(p, "tribe_sleeping_camp");
    }

    /** Up again: out of bed and the bed taken up (both halves). */
    void wake() {
        if (mob.isSleeping()) mob.stopSleeping();
        ServerLevel level = (ServerLevel) mob.level();
        if (bed != null && level.isLoaded(bed)) {
            for (BlockPos p : new BlockPos[]{bed, bed.relative(bedFacing)})
                if (level.getBlockState(p).is(Blocks.STRAW_BED)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        bed = null;
    }

    /** Whatever they were doing round the fire is over: awake, standing, not fishing. */
    void reset() {
        wake();
        stopFishing();
        if (sitting || lookAt != null) stand();
    }

    private boolean placed(ServerLevel level) {
        BlockState s = level.getBlockState(bed), h = level.getBlockState(bed.relative(bedFacing));
        return s.is(Blocks.STRAW_BED) && s.getValue(AbstractBedBlock.PART) == BedPart.FOOT && h.is(Blocks.STRAW_BED);
    }

    /** A spot round the fire, starting at their own angle, where both halves of a bed fit, head away from the fire. */
    private boolean pickBed(ServerLevel level, BlockPos fire) {
        double a0 = (mob.getId() * 2.399) % (Math.PI * 2);
        for (int i = 0; i < 16; i++) {
            double a = a0 + i * 0.7, r = BED_R + (i / 8);
            BlockPos foot = BlockPos.containing(fire.getX() + 0.5 + Math.cos(a) * r, fire.getY(), fire.getZ() + 0.5 + Math.sin(a) * r);
            Direction out = Direction.getApproximateNearest(foot.getX() - fire.getX(), 0, foot.getZ() - fire.getZ());
            if (foot.distManhattan(fire) < 2 || !free(level, foot, out)) continue;
            bed = foot;
            bedFacing = out;
            return true;
        }
        return false;
    }

    private static boolean free(ServerLevel level, BlockPos foot, Direction facing) {
        return standable(level, foot) && standable(level, foot.relative(facing));
    }

    static boolean standable(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getFluidState(p.below()).isEmpty()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    /** A warrior's night: on watch at a post six blocks out, facing away from the fire; moves on to the next post now and then. */
    void guard(BlockPos fire) {
        if (++postTicks > 600 + (mob.getId() % 7) * 40) {
            postTicks = 0;
            post++;
        }
        double a = (mob.getId() * 2.399 + post * Math.PI / 2) % (Math.PI * 2);
        Vec3 spot = new Vec3(fire.getX() + 0.5 + Math.cos(a) * GUARD_R, fire.getY(), fire.getZ() + 0.5 + Math.sin(a) * GUARD_R);
        standAt(spot, spot.add(Math.cos(a) * 8, 1.4, Math.sin(a) * 8));
    }

    // ---- a visit by water ----

    /** A fisher on a visit fishes by water near the fire (FishGoal's shore search and casting); false if there is none or it is dark. */
    boolean fish() {
        ServerLevel level = (ServerLevel) mob.level();
        if (mob.role() != TribeRole.FISHER || phase() == NIGHT) return stopFishing();
        if (fishing == null) {
            if (mob.getRandom().nextInt(100) != 0) return false;
            FishGoal f = new FishGoal(mob);
            if (!f.findSpot(level)) return false;
            fishing = f;
            stand();
            f.start();
            fishStand = f.stand;
            fishSpot = f.spot;
        }
        if (!fishing.canContinueToUse()) return stopFishing();
        fishing.tick();
        return true;
    }

    private boolean stopFishing() {
        if (fishing != null) fishing.stop();
        fishing = null;
        fishStand = fishSpot = null;
        return false;
    }

    /** A clan mate fishing nearby (to go and sit with), or null. */
    Tribesperson fisherNear() {
        List<Tribesperson> f = mob.groupNearby(24).stream().filter(t -> t.life.fishStand != null).toList();
        return f.isEmpty() ? null : f.get(0);
    }

    /** Player the leader walks over to now and then, and until when; null when not. */
    Player visiting;
    long visitUntil;
}
