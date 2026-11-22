package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Travois: two long poles lashed into a V with a hide stretched between them, dragged on the ground. Right-click with an
 * empty hand to harness it (or unharness); harnessed it follows {@link #LEAD} blocks behind you, climbing steps. Holds
 * {@link #CAPACITY} room: a body takes {@link CarcassEntity#loadUnits} (light 1, medium 2, large 3; right-click it with the
 * one you haul), so one large, a medium and a light, or three light ones. Sneak +
 * right-click with an empty hand unloads the last one. Loaded carcasses are its passengers. Punch it to pick it up.
 * <p>
 * Movement is simulated on both sides ({@link MoveSimulationType#SERVER_AND_CLIENT}, as dropped items): gravity, a rigid
 * tow line to the puller, steps up to a block; the client blends server corrections in over 3 ticks instead of jumping.
 * Stuck (a wall or tree holds it and the puller is more than 0.3 past the lead), it holds the puller back at
 * {@link #SLACK} past the lead ({@link #tether}); straining against it for {@link #STRAIN_TICKS}, or getting more than
 * {@link #BREAK} blocks away anyway, makes the puller let go. Client: {@link #pitch} tilts the poles (or the cart bed) up
 * to the puller's hip, or down to rest on the ground when nobody pulls it.
 */
public class TravoisEntity extends Entity {
    /** Entity id of the player in the harness, -1 if none. Synced (sprint check, renderer). */
    private static final EntityDataAccessor<Integer> HARNESS = SynchedEntityData.defineId(TravoisEntity.class, EntityDataSerializers.INT);
    public static final double LEAD = 2.4;
    public static final int CAPACITY = 3;
    /** Height of the hide platform above the ground. */
    static final double PLATFORM = 0.2;
    /** Blocks/tick^2 down, terminal fall speed, horizontal speed kept per tick when nobody pulls (sliding on the ground, in the air). */
    static final double GRAVITY = 0.08, MAX_FALL = 1.5, GROUND_GLIDE = 0.3, AIR_GLIDE = 0.91;
    /** Stuck: the puller is held this far past the lead; this far away (any direction) the puller lets go. */
    public static final double SLACK = 1.0, BREAK = 5.0;
    /** Straining away from a stuck load this long (3 s) makes the puller let go. */
    static final int STRAIN_TICKS = 60;
    /** Rear pole ends behind the centre and crossed front ends ahead of it (TravoisModel), and how high the front rests when dropped. */
    static final double REAR_BACK = 1.5, FRONT_REACH = 2.0, REST_LIFT = 0.25;

    private int stuckTicks, strainTicks;
    /** Client: tilt (degrees, front up) of the poles or the cart bed, this tick and the last. */
    public float pitch, pitchO;

    public TravoisEntity(EntityType<? extends TravoisEntity> type, Level level) {
        super(type, level);
    }

    public int harnessId() {
        return entityData.get(HARNESS);
    }

    /** Who pulls it: a player, or a tamed horse for the {@link CartEntity}; null when a Wagon tows it ({@link #puller}). */
    public LivingEntity harness() {
        return puller() instanceof LivingEntity e ? e : null;
    }

    /** What pulls it: {@link #harness}, or the Wagon a cart is hitched behind as a trailer. */
    public Entity puller() {
        int id = harnessId();
        return id >= 0 ? level().getEntity(id) : null;
    }

    /** The player steering {@code puller}: the puller itself, the horse's rider, or the rider of a towing wagon's lead horse. */
    static Player driver(Entity puller) {
        if (puller instanceof Player p) return p;
        if (puller instanceof TravoisEntity t) puller = t.harness();
        return puller != null && puller.getControllingPassenger() instanceof Player rider ? rider : null;
    }

    protected void setHarness(Entity puller) {
        entityData.set(HARNESS, puller == null ? -1 : puller.getId());
    }

    /** Load units it holds. */
    public int capacity() {
        return CAPACITY;
    }

    /** Where the tow line pulls from: the puller (the Wagon: between its pair of horses). */
    protected Vec3 pullPoint(Entity puller) {
        return puller.position();
    }

    /** Share of the gap it closes each tick: 1 follows rigidly, less crawls behind, 0 holds fast (the Wagon on one horse). */
    protected double pullFactor() {
        return 1.0;
    }

    /** How far behind {@code puller} it follows. */
    protected double lead(Entity puller) {
        return LEAD;
    }

    /** Speed the player in the harness loses (CarcassHauling). */
    public double slowdown() {
        return CarcassHauling.SLOW_TRAVOIS + CarcassHauling.SLOW_TRAVOIS_LOAD * load();
    }

    /** The player in the harness can't sprint (PlayerMixin via CarcassHauling.blocksSprint). */
    public boolean blocksSprint() {
        return load() > 0;
    }

    /** Lang key part for its messages ("travois", "cart"). */
    protected String messageKey() {
        return "travois";
    }

    protected Item item() {
        return CarcassHauling.TRAVOIS_ITEM.get();
    }

    /** The vehicle {@code puller} already draws (in its harness, or as a wagon's second horse), or null: one each. */
    public static TravoisEntity pulling(Entity puller) {
        for (TravoisEntity t : puller.level().getEntitiesOfClass(TravoisEntity.class, puller.getBoundingBox().inflate(16),
                t -> t.harnessId() == puller.getId() || t instanceof WagonEntity w && w.partner() == puller))
            return t;
        return null;
    }

    /** {@code puller} already draws another vehicle. */
    protected boolean pullsOther(Entity puller) {
        TravoisEntity t = pulling(puller);
        return t != null && t != this;
    }

    /** Server: tells {@code player} that what would pull this already pulls something else. */
    protected void sayAlreadyPulling(Player player) {
        player.sendOverlayMessage(Component.translatable("message.mechanical.travois.already_pulling"));
    }

    /** The travois {@code player} pulls, or null. */
    public static TravoisEntity harnessedTo(Player player) {
        for (TravoisEntity t : player.level().getEntitiesOfClass(TravoisEntity.class, player.getBoundingBox().inflate(10), t -> t.harnessId() == player.getId()))
            return t;
        return null;
    }

    /** How full it is, 0..1: what a player in the harness pays in food for (CarcassHauling). */
    public double loadFraction() {
        return capacity() == 0 ? 0 : load() / (double) capacity();
    }

    /** Load units on it. */
    public int load() {
        int units = 0;
        for (Entity e : getPassengers()) units += room(e);
        return units;
    }

    /** Tame farm animals (entity tag mechanical:domestic): they ride carts and wagons, not the travois. */
    static final net.minecraft.tags.TagKey<EntityType<?>> LIVESTOCK = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "domestic"));
    /** Advancement for loading an animal, granted from code (scripts/gen_advancements.py). */
    static final net.minecraft.resources.Identifier LIVESTOCK_ADVANCEMENT = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "bronze_age/livestock_cart");
    /** How far a led animal may be to climb on. */
    static final double LEASH_RANGE = 10;

    /** Room a passenger takes: a body its load units; livestock by size (a hen or a young one 1, a pig or sheep 2, a cow 3). */
    // ---- felled logs (FelledLogEntity) on the bed: none on a travois; a Cart and a Wagon take some (logSlots, logMax)

    /** Logs drawn at this scale on a bed, so they fit (a 4-block log is 2.8 blocks on the wagon). */
    public static final float LOG_SCALE = 0.7F;

    /** How many logs fit on the bed (0: none). */
    public int logSlots() {
        return 0;
    }

    /** The longest log it takes, in blocks. */
    public int logMax() {
        return 0;
    }

    /** May logs go on now (no bodies, livestock, chests or modules on the bed)? */
    protected boolean logRoom() {
        return getPassengers().stream().allMatch(e -> e instanceof FelledLogEntity || e instanceof Player);
    }

    /** Distance from the middle to the back of the bed, where unloaded logs roll off. */
    protected double rear() {
        return 1.0;
    }

    /** The logs riding on the bed, in loading order. */
    public java.util.List<FelledLogEntity> carriedLogs() {
        java.util.List<FelledLogEntity> out = new java.util.ArrayList<>();
        for (Entity e : getPassengers()) if (e instanceof FelledLogEntity log) out.add(log);
        return out;
    }

    /** The first log on the bed, or null. */
    public FelledLogEntity carriedLog() {
        for (Entity e : getPassengers()) if (e instanceof FelledLogEntity log) return log;
        return null;
    }

    /** When each player last loaded or unloaded a log by clicking (per side), so a held click doesn't go on to hitch or open anything. */
    private static final java.util.Map<Player, Long> LOG_CLICK = new java.util.WeakHashMap<>(), LOG_CLICK_CLIENT = new java.util.WeakHashMap<>();
    private static final int LOG_CLICK_TICKS = 10;

    protected java.util.Map<Player, Long> logClicks() {
        return level().isClientSide() ? LOG_CLICK_CLIENT : LOG_CLICK;
    }

    /** Was a log loaded or unloaded by {@code player} just now (the click is still held)? */
    protected boolean justClickedLog(Player player) {
        Long last = logClicks().get(player);
        return last != null && !player.isShiftKeyDown() && level().getGameTime() - last < LOG_CLICK_TICKS;
    }

    /** Server: the log {@code player} lifts or pulls goes on the bed. */
    protected void loadLog(ServerLevel level, FelledLogEntity log, Player player) {
        if (!logRoom()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.log_no_room"));
            return;
        }
        if (carriedLogs().size() >= logSlots()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.log_full"));
            return;
        }
        if (log.size() > logMax()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.log_too_long", logMax()));
            return;
        }
        log.untieAll(level);
        FelledLogEntity onBed = log.straight() ? log : log.straighten(level, getYRot() + 180, false);
        onBed.startRiding(this, true, true);
        playSound(SoundEvents.WOOD_PLACE, 1.0F, 0.6F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.log_loaded"));
        if (player instanceof ServerPlayer sp && this instanceof WagonEntity) Kettle.award(sp, FelledLogEntity.WAGON_ADVANCEMENT);
    }

    /** Server: the last log loaded rolls off the back and lies behind. */
    void unloadLog(FelledLogEntity log) {
        log.stopRiding();
        Vec3 dir = Vec3.directionFromRotation(0, getYRot()), right = Vec3.directionFromRotation(0, getYRot() + 90);
        Vec3 back = position().subtract(dir.scale(rear() + log.size() / 2.0 + 0.3));
        log.setYRot(getYRot() + 180);
        // behind the bed, or beside a log already rolled off there, or further back
        search:
        for (int far = 0; far <= 2; far++)
            for (int side : new int[] {0, 1, -1, 2, -2})
                for (int up = 0; up <= 1; up++) {
                    Vec3 at = back.subtract(dir.scale(far * 1.2)).add(right.scale(side * 1.05)).add(0, up, 0);
                    if (log.fits(level(), at, getYRot() + 180)) {
                        log.setPos(at);
                        break search;
                    }
                }
        playSound(SoundEvents.WOOD_FALL, 1.0F, 0.7F);
    }

    /** Sneak + right-click: unloads the last log, if any. Null if there is none. */
    protected InteractionResult unloadLogUse(Player player) {
        java.util.List<FelledLogEntity> logs = carriedLogs();
        if (logs.isEmpty()) return null;
        logClicks().put(player, level().getGameTime());
        if (!level().isClientSide()) unloadLog(logs.getLast());
        return InteractionResult.SUCCESS;
    }

    public static int room(Entity e) {
        if (e instanceof CarcassEntity c) return c.loadUnits();
        if (isLivestock(e)) {
            net.minecraft.world.entity.animal.Animal a = (net.minecraft.world.entity.animal.Animal) e;
            return a.isBaby() || a.getBbWidth() < 0.6F ? 1 : a.getBbHeight() >= 1.35F ? 3 : 2;
        }
        return 0;
    }

    public static boolean isLivestock(Entity e) {
        return e instanceof net.minecraft.world.entity.animal.Animal && e.getType().builtInRegistryHolder().is(LIVESTOCK);
    }

    /** Carts and wagons take living farm animals too. */
    protected boolean carriesLivestock() {
        return false;
    }

    /** Server: the farm animals {@code player} leads climb on while there is room; their leads are tied to it. */
    private void loadLivestock(java.util.List<net.minecraft.world.entity.animal.Animal> led, Player player) {
        int loaded = 0;
        for (net.minecraft.world.entity.animal.Animal animal : led) {
            if (capacity() == 0) {
                player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".no_room"));
                break;
            }
            if (load() + room(animal) > capacity()) {
                player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".full"));
                break;
            }
            if (tieOn(animal)) loaded++;
        }
        if (loaded > 0) {
            playSound(SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
            if (player instanceof ServerPlayer sp) Kettle.award(sp, LIVESTOCK_ADVANCEMENT);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HARNESS, -1);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.SERVER_AND_CLIENT;
    }

    /** Stuck behind something while the puller walks on (see {@link #tether}). */
    public boolean isStuck() {
        return stuckTicks >= 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) { // livestock ride only while tied on: a cut or broken lead lets them jump off
            for (Entity e : java.util.List.copyOf(getPassengers()))
                if (isLivestock(e) && ((net.minecraft.world.entity.animal.Animal) e).getLeashHolder() != this) dismount(e);
            if (carriesLivestock() && tickCount % 20 == 0) retie();
        }
        Entity p = puller();
        if (!level().isClientSide() && harnessId() >= 0) {
            if (p == null || !p.isAlive()) p = letGo(null, null); // dead or gone
            else if (p.distanceTo(this) > BREAK + (p instanceof WagonEntity ? WagonEntity.REAR : 0)) {
                if (fixedHitch()) snapBehind(p); // nothing to tear: it is pulled up behind the team
                else p = letGo(p, "torn_loose");
            }
        }
        Vec3 v = getDeltaMovement();
        double glide = onGround() ? GROUND_GLIDE : AIR_GLIDE;
        double vx = v.x * glide, vy = Math.max(-MAX_FALL, v.y - GRAVITY), vz = v.z * glide;
        if (p != null) {
            Vec3 off = position().subtract(pullPoint(p)).multiply(1, 0, 1);
            double d = off.length(), lead = lead(p);
            if (d > lead) {
                // a rigid tow line: this tick it closes the gap the puller opened
                vx = off.x * (lead - d) / d * pullFactor();
                vz = off.z * (lead - d) / d * pullFactor();
            }
            if (d > 0.3) setYRot((float) Math.toDegrees(Math.atan2(off.x, -off.z))); // the poles point at the one pulling
        }
        Vec3 from = position();
        move(MoverType.SELF, new Vec3(vx, vy, vz));
        Vec3 moved = position().subtract(from);
        setDeltaMovement(moved.x, verticalCollision ? 0 : vy, moved.z);
        if (p != null) {
            double d = position().subtract(pullPoint(p)).horizontalDistance();
            stuckTicks = d > lead(p) + 0.3 ? stuckTicks + 1 : 0;
            if (p instanceof LivingEntity && !(p instanceof Player) && p.isLocalInstanceAuthoritative()) tether(p, this); // a horse; players: CarcassHauling.onPlayerTick; a towing wagon isn't held back
            if (!level().isClientSide()) {
                TrampledPaths.step(this); // runners and wheels wear paths too
                strainTicks = isStuck() && d > lead(p) + SLACK - 0.2 && pullsAway(p) ? strainTicks + 1 : 0;
                if (strainTicks >= STRAIN_TICKS && !fixedHitch()) letGo(p, "stuck");
            }
        } else stuckTicks = strainTicks = 0;
        if (level().isClientSide()) {
            float target = targetPitch(puller());
            if (tickCount <= 1) pitch = target;
            pitchO = pitch;
            pitch += (target - pitch) * 0.4F;
        }
    }

    /** Fixed to its puller (the Wagon's team, D1): never torn loose or let go when stuck, only unhitched by hand. */
    protected boolean fixedHitch() {
        return false;
    }

    /** Server: puts it straight back behind {@code puller}, at its lead, facing it. */
    private void snapBehind(Entity puller) {
        Vec3 back = pullPoint(puller).subtract(Vec3.directionFromRotation(0, puller.getYRot()).scale(lead(puller)));
        setPos(back.x, puller.getY(), back.z);
        setYRot(puller.getYRot());
    }

    /** Server: takes {@code puller} out of the harness, telling them why ({@code why}: lang key part; null: silently). */
    private Entity letGo(Entity puller, String why) {
        setHarness(null);
        stuckTicks = strainTicks = 0;
        if (why == null) return null;
        Player told = driver(puller);
        if (told != null) told.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + "." + why));
        playSound(SoundEvents.LEAD_BREAK, 1.0F, 0.8F);
        return null;
    }

    /** Server: the puller's (or the horse's rider's) movement keys push away from this. */
    private boolean pullsAway(Entity puller) {
        if (!(driver(puller) instanceof ServerPlayer player)) return false;
        Input in = player.getLastClientInput();
        double fwd = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0), side = (in.left() ? 1 : 0) - (in.right() ? 1 : 0);
        if (fwd == 0 && side == 0) return false;
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot()), left = Vec3.directionFromRotation(0, player.getYRot() - 90);
        Vec3 wish = look.scale(fwd).add(left.scale(side)).normalize();
        Vec3 away = puller.position().subtract(position()).multiply(1, 0, 1).normalize();
        return wish.dot(away) > 0.3;
    }

    /**
     * Holds {@code mover} (the puller, on the side that moves it) back while {@code load} is stuck: it can't get further than
     * {@link #SLACK} past the lead, or than it already was. Its speed away from the load is taken away.
     */
    public static void tether(Entity mover, TravoisEntity load) {
        LivingEntity puller = load.harness();
        if (!load.isStuck() || puller == null) return;
        Vec3 off = mover.position().subtract(load.position()).multiply(1, 0, 1);
        double d = off.length(), max = load.lead(puller) + SLACK;
        if (d <= max) return;
        Vec3 n = off.scale(1 / d);
        double limit = Math.max(max, new Vec3(mover.xo - load.getX(), 0, mover.zo - load.getZ()).length());
        if (d > limit) mover.setPos(load.getX() + n.x * limit, mover.getY(), load.getZ() + n.z * limit);
        Vec3 v = mover.getDeltaMovement();
        double out = v.x * n.x + v.z * n.z;
        if (out > 0) mover.setDeltaMovement(v.subtract(n.x * out, 0, n.z * out));
    }

    /** Client: the poles' tilt: up to the puller's hip from the rear ends, or down onto the ground ahead when let go. */
    protected float targetPitch(Entity puller) {
        double rise, run;
        if (puller != null) {
            rise = puller.getY() + puller.getBbHeight() * 0.5 - getY();
            run = REAR_BACK + position().subtract(puller.position()).horizontalDistance();
        } else {
            rise = groundAhead(FRONT_REACH) + REST_LIFT - getY();
            run = REAR_BACK + FRONT_REACH;
        }
        return Mth.clamp((float) Math.toDegrees(Math.atan2(rise, run)), -25.0F, 40.0F);
    }

    /** Height of the ground {@code reach} blocks ahead (looked for from a block above down to two below; else two below). */
    protected double groundAhead(double reach) {
        Vec3 at = position().add(Vec3.directionFromRotation(0, getYRot()).scale(reach));
        HitResult hit = level().clip(new ClipContext(at.add(0, 1, 0), at.add(0, -2, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? getY() - 2 : hit.getLocation().y;
    }

    /** Client: {@link #pitch} at the frame. */
    public float pitch(float partialTicks) {
        return Mth.lerp(partialTicks, pitchO, pitch);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // hands on a felled log: it goes on the bed if this takes logs; never hitching, loading or picking up
        FelledLogEntity dragged = FelledLogEntity.handled(player);
        if (dragged != null) {
            if (logSlots() == 0) return InteractionResult.FAIL;
            logClicks().put(player, level().getGameTime());
            if (level() instanceof ServerLevel server) loadLog(server, dragged, player);
            return InteractionResult.SUCCESS;
        }
        // the click that loaded a log, still held: it doesn't go on to hitch or open anything
        if (justClickedLog(player)) return InteractionResult.SUCCESS;
        CarcassEntity hauled = CarcassHauling.hauled(player);
        if (hauled != null) {
            if (!level().isClientSide()) loadOn(hauled, player);
            return InteractionResult.SUCCESS;
        }
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (carriesLivestock() && !player.isShiftKeyDown()) {
            var led = level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, player.getBoundingBox().inflate(LEASH_RANGE),
                    a -> isLivestock(a) && a.getLeashHolder() == player && !a.isPassenger());
            if (!led.isEmpty()) {
                if (!level().isClientSide()) loadLivestock(led, player);
                return InteractionResult.SUCCESS;
            }
        }
        if (player.isShiftKeyDown()) return sneakUse(player);
        if (!level().isClientSide()) toggleHarness(player);
        return InteractionResult.SUCCESS;
    }

    /** Sneak + right-click with an empty hand: unloads the last body. */
    protected InteractionResult sneakUse(Player player) {
        InteractionResult logs = unloadLogUse(player);
        if (logs != null) return logs;
        if (load() == 0) return InteractionResult.PASS;
        if (!level().isClientSide()) unloadLast();
        return InteractionResult.SUCCESS;
    }

    /** Server: right-click with an empty hand puts {@code player} in the harness, or takes them out. */
    protected void toggleHarness(Player player) {
        boolean off = harnessId() == player.getId();
        if (!off && pullsOther(player)) {
            sayAlreadyPulling(player);
            return;
        }
        setHarness(off ? null : player);
        stuckTicks = strainTicks = 0;
        player.sendOverlayMessage(Component.translatable(off ? "message.mechanical.travois.unharnessed" : "message.mechanical.travois.harnessed"));
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
    }

    private void loadOn(CarcassEntity carcass, Player player) {
        if (capacity() == 0) player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".no_room"));
        else if (carcass.isHuge()) player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".too_big"));
        else if (load() + carcass.loadUnits() > capacity()) player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".full"));
        else {
            carcass.setHauler(null);
            carcass.startRiding(this, true, true);
            playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.6F);
            if (player instanceof ServerPlayer sp) onLoaded(sp);
        }
    }

    /** Server: a body was just loaded by {@code player} (advancements). */
    protected void onLoaded(ServerPlayer player) {
    }

    /**
     * Server: {@code animal} climbs on, its lead moved over to this. Vanilla drops a lead when a mob mounts and dismounts a
     * leashed passenger, so the lead's data is taken off for the mount and handed back with this as the holder.
     */
    private boolean tieOn(net.minecraft.world.entity.animal.Animal animal) {
        net.minecraft.world.entity.Leashable.LeashData lead = animal.getLeashData();
        if (lead == null) return false;
        animal.setLeashData(null);
        if (!animal.startRiding(this, true, true)) {
            animal.setLeashData(lead);
            return false;
        }
        lead.setLeashHolder(this);
        animal.setLeashData(lead);
        if (level() instanceof ServerLevel server)
            server.getChunkSource().sendToTrackingPlayers(animal, new net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket(animal, this));
        return true;
    }

    /** Server: animals still tied to it but not on it (a reload restores the lead after the ride and vanilla lets them off) climb back. */
    private void retie() {
        for (var animal : level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, getBoundingBox().inflate(4),
                a -> isLivestock(a) && !a.isPassenger() && a.getLeashHolder() == this))
            if (load() + room(animal) <= capacity()) tieOn(animal);
            else animal.dropLeash();
    }

    /** Server: {@code player} unties an animal riding it and leads it off. */
    public static boolean untie(net.minecraft.world.entity.animal.Animal animal, Player player) {
        if (!(animal.getVehicle() instanceof TravoisEntity vehicle) || !isLivestock(animal)) return false;
        if (!vehicle.level().isClientSide()) {
            animal.setLeashedTo(player, true);
            vehicle.dismount(animal);
            vehicle.playSound(SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
        }
        return true;
    }

    /** Server: {@code e} gets off, beside it (or where it was if there is no room). */
    private void dismount(Entity e) {
        e.stopRiding();
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90).scale(1.3);
        Vec3 was = e.position();
        e.setPos(position().add(side).add(0, 0.2, 0));
        if (!level().noCollision(e)) e.setPos(was);
    }

    /** Server: unloads the last loaded body (seated players stay). */
    protected void unloadLast() {
        Entity last = null;
        for (Entity e : getPassengers()) if (room(e) > 0) last = e;
        if (last == null) return;
        last.stopRiding();
        // beside the travois, or where it lay if there is no room
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90).scale(1.3);
        Vec3 was = last.position();
        last.setPos(position().add(side).add(0, 0.2, 0));
        if (!level().noCollision(last)) last.setPos(was);
        playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.5F);
    }

    /**
     * Where {@code passenger} lies in the row of loaded bodies, centred on 0: each body gets {@code perRoom} times the room it
     * takes, in passenger order, so they lie side by side (cart) or one behind the other (travois) without overlapping.
     */
    protected double rowOffset(Entity passenger, double perRoom) {
        int before = 0;
        for (Entity e : getPassengers()) {
            if (e == passenger) break;
            before += room(e);
        }
        int room = room(passenger);
        return (before + room / 2.0 - load() / 2.0) * perRoom;
    }

    /** Loaded bodies lie along the hide, one behind the other, the group centred a little behind the middle. */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        double along = -0.4 - rowOffset(passenger, 0.6);
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        // on the tilted hide (pitch stays 0 on the server, where the height doesn't show)
        double rise = (along + REAR_BACK) * Math.tan(Math.toRadians(pitch));
        moveFunction.accept(passenger, getX() + dir.x * along, getY() + PLATFORM + rise, getZ() + dir.z * along);
        passenger.setYRot(getYRot());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof FelledLogEntity) return carriedLogs().size() < logSlots();
        if (carriedLog() != null && !(passenger instanceof Player)) return false; // logs fill the bed
        int room = room(passenger);
        return room > 0 && (passenger instanceof CarcassEntity || carriesLivestock()) && load() + room <= capacity();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!(source.getEntity() instanceof Player player)) return false;
        ejectPassengers();
        dropContents(level);
        if (!player.isCreative()) spawnAtLocation(level, new ItemStack(item()));
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
        discard();
        return true;
    }

    /** Server, when it is broken up: spills what it holds besides the bodies. */
    protected void dropContents(ServerLevel level) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    /** The Travois (or Cart) item: right-click the ground to set it down, poles towards you. */
    public static class Placer extends Item {
        private final Supplier<? extends EntityType<? extends TravoisEntity>> type;

        public Placer(Properties properties, Supplier<? extends EntityType<? extends TravoisEntity>> type) {
            super(properties);
            this.type = type;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos at = context.getClickedPos().relative(context.getClickedFace());
            TravoisEntity travois = type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (travois == null) return InteractionResult.FAIL;
            travois.setPos(Vec3.atBottomCenterOf(at));
            travois.setYRot(context.getRotation() + 180.0F);
            if (!level.noCollision(travois)) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                level.addFreshEntity(travois);
                level.playSound(null, at, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
