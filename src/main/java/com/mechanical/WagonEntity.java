package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wagon: the Iron Age hauler (docs/wagon-plan.md). A long plank bed on four iron-rimmed wheels, the front axle swivelling
 * on a pole drawn by a <b>pair of tamed horses</b>; players can't pull it. One horse alone crawls with it empty and can't
 * move it loaded. The bed is a grid of {@link #SLOTS} cells, {@link #COLS} wide and {@link #ROWS} deep (cell = row * 2 + side,
 * 0 = front left), each empty ({@link #ROOM_PER_SLOT} room of carcasses: all empty is 12 room, four large bodies) or
 * holding a module: a Chest ({@link #CHEST_SIZE} slots), a Hay Bale (feeds the team: heals the horses while they're
 * hitched) or a seat (any wooden slab; a player rides along).
 * <p>
 * Right-click the wagon with a Chest to put it in the first free cell from the front, with a Hay Bale or wooden slab in the
 * last free cell from the back; punch a module to take it out (a chest spills, a half-eaten bale gives back its wheat);
 * punch the wagon where there is none to pick it up. Two chests in a row (cells 2r and 2r+1) are one double chest, drawn
 * and opened as vanilla's: right-click a chest, with anything in hand, to open the one aimed at (a pair opens the 54 slots, left cell first); sneak + right-click
 * unloads the last body (or the log). Right-click it while dragging a
 * felled log ({@link FelledLogEntity}, up to {@link FelledLogEntity#WAGON_MAX} blocks) to load it on an empty bed. Press the grab key (default H, {@link #grab}) while riding
 * or leading tamed horses to hitch them (again to unhitch): the first takes the lead place, the second is yoked beside it. The team is fixed
 * (living-world-plan D1-D2): the second horse keeps exactly beside the first, the wagon rigidly behind them, nothing
 * snaps; sneak + right-click a hitched horse to take it out ({@link #release}). Both are saved by UUID. Right-click a seat to sit down.
 */
public class WagonEntity extends TravoisEntity {
    public static final int COLS = 2, ROWS = 3, SLOTS = COLS * ROWS, ROOM_PER_SLOT = 2, CAPACITY = SLOTS * ROOM_PER_SLOT, CHEST_SIZE = 27;
    public static final int EMPTY = 0, CHEST = 1, HAY = 2, SEAT = 3;
    /** Hay Bale: bites (one wheat each, 9 to a bale), each healing a horse 2 health, one bite per horse every 5 s at most. */
    static final int HAY_BITES = 9, FEED_INTERVAL = 100;
    static final float BITE_HEAL = 2.0F;
    /** The six modules, 2 bits each, cell 0 (front left) in the lowest bits. Synced (renderer). */
    private static final EntityDataAccessor<Integer> MODULES = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.INT);
    /** Entity id of the second horse, yoked beside the lead horse ({@link #harness}), -1 if none. Synced (pole, pull point). */
    private static final EntityDataAccessor<Integer> PARTNER = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.INT);
    /** {@link #loadFraction}, synced: the client doesn't see the chests but moves the wagon too ({@link #pullFactor}). */
    private static final EntityDataAccessor<Float> FULLNESS = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.FLOAT);
    /** The second horse goes on the lead horse's right (else its left): the side it stood on when yoked. Synced (both sides place it). */
    private static final EntityDataAccessor<Boolean> PARTNER_RIGHT = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.BOOLEAN);
    static final Identifier TEAM_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/wagon_team"),
            LOADED_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/full_wagon"),
            CHESTS_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/wagon_chests");
    /** The lead horse's speed modifier while hitched alone (crawl or stand); a pair's load slows it through HorseWork. */
    private static final Identifier TEAM_SLOW = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wagon_team");
    /** Centre of the pair to the wagon centre; the pair's spacing (horse width 1.3 + a gap). */
    public static final double LEAD = 2.8, PAIR = 1.45;
    /**
     * The pole (WagonModel): fixed to the front axle {@link #FRONT_AXLE} ahead of the centre, it runs along the horses to
     * the pair's middle {@link #POLE} further on (the yoke a little ahead of that, at their chests). The front axle turns
     * up to {@link #MAX_STEER} degrees from the bed; past that the horses drag the bed round.
     */
    public static final double FRONT_AXLE = 1.0, POLE = LEAD - FRONT_AXLE;
    static final float MAX_STEER = 55.0F;
    /** The bed's rear end behind the centre: a towed cart's shafts rest there ({@link #towPoint}). */
    public static final double REAR = ROWS * 1.0 / 2;
    /** A trailer (Wheeled Cart) weighs on the team by its fullness times this (its 6 room against the wagon's 12). */
    static final double TRAILER_WEIGHT = CartEntity.CAPACITY / (double) CAPACITY;
    /** Bed floor top, wheel radius (WagonModel), row depth along the bed, cell width, seat top above the floor (blocks). */
    public static final double BED = 14 / 16.0, WHEEL_RADIUS = 8 / 16.0, ROW_DEPTH = 1.0, CELL_WIDTH = 0.75, SEAT_TOP = 6 / 16.0;
    /** One horse: share of the gap the wagon closes (empty), and the horse's speed lost (empty). Loaded, neither moves. */
    static final double CRAWL = 0.3, CRAWL_SLOW = 0.75;
    /** Search range for led horses to hitch. */
    private static final double HORSE_RANGE = 10;

    private final SimpleContainer[] chests = new SimpleContainer[SLOTS];
    private final int[] hay = new int[SLOTS];
    /** Server: the horse carrying {@link #TEAM_SLOW}. */
    private AbstractHorse slowed;
    /** Server: the team by UUID, saved, so it is found again after a reload (the entity ids above are not). */
    private java.util.UUID leadId, partnerId;
    /** A Trader Caravan's wagon (people.Caravans): locked to players while its team lives. Saved. */
    private boolean caravan;
    /** Client: wheel angle (radians) and the front axle's swing (degrees, + to the right), this tick and the last. */
    public float wheelSpin, wheelSpinO, steer, steerO;

    public WagonEntity(EntityType<? extends WagonEntity> type, Level level) {
        super(type, level);
        for (int i = 0; i < SLOTS; i++) {
            int cell = i;
            chests[i] = new SimpleContainer(CHEST_SIZE) {
                /** An open chest screen closes when the wagon is gone or far, or this chest is taken out. */
                @Override
                public boolean stillValid(Player player) {
                    return isAlive() && player.distanceTo(WagonEntity.this) < 8 && module(cell) == CHEST;
                }
            };
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODULES, 0);
        builder.define(PARTNER, -1);
        builder.define(FULLNESS, 0.0F);
        builder.define(PARTNER_RIGHT, false);
    }

    // ---- modules

    public int module(int slot) {
        return entityData.get(MODULES) >> (slot * 2) & 3;
    }

    private void setModule(int slot, int module) {
        int all = entityData.get(MODULES) & ~(3 << (slot * 2));
        entityData.set(MODULES, all | module << (slot * 2));
    }

    private int count(int module) {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) if (module(i) == module) n++;
        return n;
    }

    /** Distance of cell {@code slot}'s centre ahead of the wagon's centre (row 0 at the front). */
    public static double slotAlong(int slot) {
        return (ROWS / 2.0 - 0.5 - slot / COLS) * ROW_DEPTH;
    }

    /** Distance of cell {@code slot}'s centre to the right of the wagon's centre (the left cell is even). */
    public static double slotSide(int slot) {
        return (slot % COLS == 0 ? -0.5 : 0.5) * CELL_WIDTH;
    }

    /** The cell at {@code local} (a point relative to the wagon's position): its row by how far along the bed, its side by left or right. */
    private int slotAt(Vec3 local) {
        double along = local.dot(Vec3.directionFromRotation(0, getYRot())), side = local.dot(Vec3.directionFromRotation(0, getYRot() + 90));
        int row = Mth.clamp((int) Math.floor((ROWS / 2.0 * ROW_DEPTH - along) / ROW_DEPTH), 0, ROWS - 1);
        return row * COLS + (side < 0 ? 0 : 1);
    }

    /** How high each module (by id; empty: the bare floor) stands above the bed floor, and half its length (WagonRenderer). */
    private static final double[] MODULE_TOP = {0.05, 0.66, 0.68, SEAT_TOP};
    private static final double MODULE_HALF = 0.375;

    /** The cell whose own box (its module, or the bare floor) {@code player}'s view ray enters first, or -1. */
    private int aimedCell(Player player) {
        // in the wagon's own frame: x to its right, y up, z ahead
        Vec3 eye = player.getEyePosition().subtract(position()), view = player.getViewVector(1.0F);
        Vec3 dir = Vec3.directionFromRotation(0, getYRot()), right = Vec3.directionFromRotation(0, getYRot() + 90);
        Vec3 from = new Vec3(eye.dot(right), eye.y, eye.dot(dir)), to = from.add(new Vec3(view.dot(right), view.y, view.dot(dir)).scale(8));
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        // shortcut: the boxes are level, as on the server; the bed's tilt on a slope (client only) is ignored
        for (int i = 0; i < SLOTS; i++) {
            double half = module(i) == EMPTY ? ROW_DEPTH / 2 : MODULE_HALF;
            var hit = new AABB(slotSide(i) - CELL_WIDTH / 2, BED - 0.1, slotAlong(i) - half,
                    slotSide(i) + CELL_WIDTH / 2, BED + MODULE_TOP[module(i)], slotAlong(i) + half).clip(from, to);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < bestDist) {
                best = i;
                bestDist = hit.get().distanceToSqr(from);
            }
        }
        return best;
    }

    /** The module {@code stack} puts in, or EMPTY. */
    private static int moduleFor(ItemStack stack) {
        if (stack.is(Items.CHEST)) return CHEST;
        if (stack.is(Items.HAY_BLOCK)) return HAY;
        if (stack.is(ItemTags.WOODEN_SLABS)) return SEAT;
        return EMPTY;
    }

    @Override
    public int capacity() {
        return carriedLog() != null ? 0 : ROOM_PER_SLOT * count(EMPTY);
    }

    // ---- a felled log (FelledLogEntity) on the bed

    @Override
    public int logSlots() {
        return 3;
    }

    @Override
    public int logMax() {
        return FelledLogEntity.WAGON_MAX;
    }

    /** Logs need the whole bed: no modules, bodies or livestock. */
    @Override
    protected boolean logRoom() {
        return count(EMPTY) == SLOTS && super.logRoom();
    }

    @Override
    protected double rear() {
        return REAR;
    }

    /** As heavy as its bodies, or its chests, whichever is fuller, plus a towed cart's share ({@link #TRAILER_WEIGHT}), at most 1. */
    @Override
    public double loadFraction() {
        if (level().isClientSide()) return entityData.get(FULLNESS);
        if (carriedLog() != null) return carriedLogs().size() / 3.0; // three logs: a full load
        TravoisEntity trailer = pulling(this);
        double towed = trailer == null ? 0 : trailer.loadFraction() * TRAILER_WEIGHT;
        int filled = 0, slots = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (module(i) != CHEST) continue;
            slots += CHEST_SIZE;
            for (ItemStack s : chests[i].getItems()) if (!s.isEmpty()) filled++;
        }
        return Math.min(1, towed + Math.max(load() / (double) CAPACITY, slots == 0 ? 0 : filled / (double) slots));
    }

    /** Chests fill the first free cell from the front, hay and seats from the back; -1 if the bed has none. */
    private int freeCell(int module) {
        for (int n = 0; n < SLOTS; n++) {
            int i = module == CHEST ? n : SLOTS - 1 - n;
            if (module(i) == EMPTY) return i;
        }
        return -1;
    }

    private void placeModule(Player player, int module, ItemStack held) {
        if (carriedLog() != null) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.log_first"));
            return;
        }
        int slot = freeCell(module);
        if (slot < 0) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.place_taken"));
            return;
        }
        if (load() > capacity() - ROOM_PER_SLOT) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.unload_first"));
            return;
        }
        setModule(slot, module);
        if (module == HAY) hay[slot] = HAY_BITES;
        held.consume(1, player);
        playSound(module == HAY ? SoundEvents.GRASS_PLACE : SoundEvents.WOOD_PLACE, 1.0F, 0.9F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.module." + module));
        if (module == CHEST && count(CHEST) == SLOTS && player instanceof ServerPlayer sp) Kettle.award(sp, CHESTS_ADVANCEMENT);
    }

    /** Server: takes the module out of {@code slot}, dropping it and what it holds. */
    private void removeModule(ServerLevel level, int slot) {
        int module = module(slot);
        if (module == EMPTY) return;
        Vec3 at = position().add(Vec3.directionFromRotation(0, getYRot()).scale(slotAlong(slot))).add(Vec3.directionFromRotation(0, getYRot() + 90).scale(slotSide(slot))).add(0, BED + 0.3, 0);
        if (module == CHEST) {
            for (ItemStack s : chests[slot].removeAllItems()) Containers.dropItemStack(level, at.x, at.y, at.z, s);
            Containers.dropItemStack(level, at.x, at.y, at.z, new ItemStack(Items.CHEST));
        } else if (module == HAY) {
            ItemStack back = hay[slot] >= HAY_BITES ? new ItemStack(Items.HAY_BLOCK) : new ItemStack(Items.WHEAT, hay[slot]);
            if (!back.isEmpty()) Containers.dropItemStack(level, at.x, at.y, at.z, back);
            hay[slot] = 0;
        } else {
            // shortcut: any wooden slab makes a seat but it comes back as oak; keep the slab if players mind
            Containers.dropItemStack(level, at.x, at.y, at.z, new ItemStack(Items.OAK_SLAB));
        }
        setModule(slot, EMPTY);
        playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
    }

    /** The cell {@code player} is looking at on the wagon, or -1. */
    public int slotLookedAt(Player player) {
        int aimed = aimedCell(player);
        if (aimed >= 0) return aimed;
        // looking at its sides or wheels: the cell above where the ray meets it
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1.0F).scale(8));
        AABB box = getBoundingBox().inflate(getPickRadius());
        return box.clip(eye, end).map(hit -> slotAt(hit.subtract(position()))).orElse(-1);
    }

    @Override
    public float getPickRadius() {
        // the bed is 3 blocks long but the collision box 2: clicks reach its ends
        return 0.5F;
    }

    /** Cell {@code slot}'s chest (WagonTest). */
    SimpleContainer chest(int slot) {
        return chests[slot];
    }

    /** The chest in {@code cell} is one half of a double chest: the other cell of its row holds a chest too. */
    boolean paired(int cell) {
        return module(cell) == CHEST && module(cell ^ 1) == CHEST;
    }

    /** The screen of the chest in {@code cell}: 27 slots, or a double chest's 54 with the left cell's chest first (vanilla's sixRows). */
    AbstractContainerMenu chestMenu(int id, Inventory inventory, int cell) {
        if (!paired(cell)) return ChestMenu.threeRows(id, inventory, chests[cell]);
        int row = cell / COLS * COLS;
        return ChestMenu.sixRows(id, inventory, new CompoundContainer(chests[row], chests[row + 1]));
    }

    private void openChest(ServerPlayer player, int cell) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> chestMenu(id, inventory, cell), getDisplayName()));
        playSound(SoundEvents.CHEST_OPEN, 0.5F, 1.1F);
    }

    // ---- the team

    public AbstractHorse partner() {
        int id = entityData.get(PARTNER);
        return id >= 0 && level().getEntity(id) instanceof AbstractHorse h ? h : null;
    }

    /** {@code horse} goes in beside the lead horse, on the side it stands on now. */
    void setPartner(AbstractHorse horse) {
        LivingEntity lead = harness();
        if (horse != null && lead != null)
            entityData.set(PARTNER_RIGHT, horse.position().subtract(lead.position()).dot(Vec3.directionFromRotation(0, lead.getYRot() + 90)) > 0);
        entityData.set(PARTNER, horse == null ? -1 : horse.getId());
        partnerId = horse == null ? null : horse.getUUID();
    }

    /** From the lead horse towards the second horse's place, one block. */
    private Vec3 pairSide(Entity lead) {
        return Vec3.directionFromRotation(0, lead.getYRot() + (entityData.get(PARTNER_RIGHT) ? 90 : -90));
    }

    /** Remembers the lead horse's UUID; dropped only when it is unhitched or dies ({@link #setLead}), not when it is out of reach (unloaded). */
    @Override
    protected void setHarness(Entity puller) {
        super.setHarness(puller);
        if (puller != null) leadId = puller.getUUID();
    }

    /** Server: a caravan's wagon behind {@code lead} and {@code partner} (yoked on the side it stands); locked to players while they live. */
    public void hitchCaravan(AbstractHorse lead, AbstractHorse partner) {
        setLead(lead);
        setPartner(partner);
        caravan = true;
    }

    public boolean isCaravan() {
        return caravan;
    }

    private void setLead(AbstractHorse horse) {
        setHarness(horse);
        if (horse == null) leadId = null;
    }

    /** A horse team is fixed to the wagon: it is never torn loose or let go when stuck. */
    @Override
    protected boolean fixedHitch() {
        return harness() instanceof AbstractHorse;
    }

    /** {@code horse} is in this wagon's team. */
    public boolean inTeam(Entity horse) {
        return horse != null && (horse == harness() || horse == partner());
    }

    /** Server: takes {@code horse} out of the team (the second horse, if any, moves up to the lead place). */
    @Override
    public void release(AbstractHorse horse, Player player) {
        if (horse == harness()) {
            AbstractHorse partner = partner();
            setPartner(null);
            setLead(partner);
        } else if (horse == partner()) setPartner(null);
        else return;
        if (horse == slowed) clearSlow();
        player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.unhitched"));
        playSound(SoundEvents.LEAD_UNTIED, 1.0F, 0.8F);
    }

    /** Between the two horses (or the lead horse alone); from the lead horse only, so it is the same on both sides. */
    @Override
    protected Vec3 pullPoint(Entity puller) {
        return partner() == null ? puller.position() : puller.position().add(pairSide(puller).scale(PAIR / 2));
    }

    /**
     * The team is rigid (D1): the pole runs straight back from the pair along the horses to the front axle, and the bed
     * trails behind the front axle the way a trailer does, swung round by the horses past {@link #MAX_STEER}. Backing the
     * horses pushes it back. (A player can't pull it: the plain tow line then.)
     */
    @Override
    protected Vec3 towTarget(Entity puller) {
        if (!(puller instanceof AbstractHorse)) return super.towTarget(puller);
        float horseYaw = puller.getYRot();
        Vec3 front = pullPoint(puller).subtract(Vec3.directionFromRotation(0, horseYaw).scale(POLE));
        Vec3 u = front.subtract(position()).multiply(1, 0, 1);
        float yaw = u.lengthSqr() < 1.0E-4 ? getYRot() : (float) Math.toDegrees(Math.atan2(-u.x, u.z));
        float steer = Mth.wrapDegrees(horseYaw - yaw);
        if (Math.abs(steer) > MAX_STEER) yaw = horseYaw - Math.signum(steer) * MAX_STEER;
        setYRot(yaw);
        return front.subtract(Vec3.directionFromRotation(0, yaw).scale(FRONT_AXLE));
    }

    /** Before the last tow step each tick (both sides): the second horse exactly beside the first. */
    @Override
    protected void beforeFollow(Entity puller) {
        if (puller instanceof LivingEntity lead) keepPair(lead);
    }

    private boolean team() {
        return harness() != null && partner() != null;
    }

    @Override
    protected double pullFactor() {
        return team() ? 1.0 : loadFraction() > 0 ? 0.0 : CRAWL;
    }

    @Override
    protected double lead(Entity puller) {
        return LEAD;
    }

    /** Where a towed cart's shafts rest: the middle of the bed's rear end. */
    public Vec3 towPoint() {
        return position().subtract(Vec3.directionFromRotation(0, getYRot()).scale(REAR));
    }

    /** Wheels: on the axles {@link #FRONT_AXLE} ahead of and behind the middle, a block either side (WagonModel). */
    public static final double WHEEL_SIDE = 1.0;
    private float pitchTarget;

    /**
     * Its four wheels: the bed rests on the plane through them, at their mean height, pitched by the front pair against the
     * rear pair and rolled by the right pair against the left.
     */
    @Override
    protected void settleOnWheels() {
        double fl = groundUnder(FRONT_AXLE, -WHEEL_SIDE, WHEEL_RADIUS), fr = groundUnder(FRONT_AXLE, WHEEL_SIDE, WHEEL_RADIUS);
        double rl = groundUnder(-FRONT_AXLE, -WHEEL_SIDE, WHEEL_RADIUS), rr = groundUnder(-FRONT_AXLE, WHEEL_SIDE, WHEEL_RADIUS);
        pitchTarget = Mth.clamp((float) Math.toDegrees(Math.atan2((fl + fr - rl - rr) / 2, 2 * FRONT_AXLE)), -35.0F, 35.0F);
        settle((fl + fr + rl + rr) / 4, (float) Math.toDegrees(Math.atan2((fr + rr - fl - rl) / 2, 2 * WHEEL_SIDE)));
    }

    @Override
    protected float targetPitch(Entity puller) {
        return pitchTarget;
    }

    @Override
    protected String messageKey() {
        return "wagon";
    }

    @Override
    protected Item item() {
        return Carting.WAGON_ITEM.get();
    }

    /**
     * Hitches the tamed horses you ride and / or lead into the free places, the lead place first (the one you ride always
     * leads, so you steer the team). A full team, or no horse to hitch, only tells you how to unhitch.
     */
    @Override
    protected void toggleHarness(Player player) {
        List<AbstractHorse> horses = candidates(player, harness(), partner());
        if (horses.isEmpty() || partner() != null) {
            player.sendOverlayMessage(Component.translatable(harness() != null ? "message.mechanical.wagon.release_hint" : "message.mechanical.wagon.needs_horses"));
            return;
        }
        for (AbstractHorse horse : horses) {
            if (horse.getLeashHolder() != null) horse.dropLeash(); // fixed to the pole now: no lead to snap
            if (harness() == null) {
                setHarness(horse);
                playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
            } else if (partner() == null) {
                if (horse.getControllingPassenger() != null) { // the ridden horse leads
                    AbstractHorse first = (AbstractHorse) harness();
                    setHarness(horse);
                    horse = first;
                }
                yoke(horse, player);
            }
        }
        if (partner() == null) player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.one_horse"));
    }

    private void yoke(AbstractHorse horse, Player player) {
        setPartner(horse);
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.9F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.team"));
        if (player instanceof ServerPlayer sp) Kettle.award(sp, TEAM_ADVANCEMENT);
    }

    /** Tamed adult horses {@code player} rides or leads, not already in this team; the ridden one first. */
    private List<AbstractHorse> candidates(Player player, LivingEntity lead, AbstractHorse partner) {
        List<AbstractHorse> out = new ArrayList<>();
        if (player.getVehicle() instanceof AbstractHorse ridden && ridden.isTamed() && !ridden.isBaby() && ridden != lead && ridden != partner
                && !pullsOther(ridden))
            out.add(ridden);
        for (AbstractHorse h : level().getEntitiesOfClass(AbstractHorse.class, player.getBoundingBox().inflate(HORSE_RANGE),
                h -> h.isTamed() && !h.isBaby() && h.getLeashHolder() == player && h != lead && h != partner && !out.contains(h) && !pullsOther(h)))
            out.add(h);
        return out;
    }

    /**
     * Server: finds a saved team again after a reload, moves the second horse up when the lead one dies, and lets the
     * ridden horse lead (a rider steers from the client; the second horse is placed by the server).
     */
    private void keepTeam(ServerLevel level) {
        if (harness() == null && leadId != null && level.getEntity(leadId) instanceof AbstractHorse h && h.isAlive()) super.setHarness(h);
        if (partner() == null && partnerId != null && level.getEntity(partnerId) instanceof AbstractHorse h && h.isAlive())
            entityData.set(PARTNER, h.getId());
        LivingEntity lead = harness();
        AbstractHorse partner = partner();
        if (partner != null && !partner.isAlive()) setPartner(partner = null);
        if (lead != null && !lead.isAlive()) { // dying: TravoisEntity would drop it next
            setPartner(null);
            setLead(partner);
        } else if (lead != null && partner != null && partner.getControllingPassenger() != null && lead.getControllingPassenger() == null) {
            setHarness(partner); // the ridden horse leads; the other keeps its side of the pole
            setPartner((AbstractHorse) lead);
        }
        // an unridden team stands in the traces (HorseWork#standInTraces) instead of wandering off with the wagon
        if (partner() != null) hitch(partner());
    }

    /**
     * Keeps the second horse exactly beside the first, turning with it (D1: horse 2 follows horse 1 exactly). Both sides,
     * after everything moved: on the rider's client the lead horse is ahead of what the server sends, so the second one
     * is placed there too, its stride in step with the lead's.
     */
    private void keepPair(LivingEntity lead) {
        AbstractHorse partner = partner();
        if (partner == null || lead == null) return;
        partner.setPos(lead.position().add(pairSide(lead).scale(PAIR))); // exactly, even into a bush: the team is one unit
        partner.setDeltaMovement(lead.getDeltaMovement());
        // each turn as the lead horse's own (its body can lag its heading when it stands): the pair looks one way
        partner.setYRot(lead.getYRot());
        partner.setYBodyRot(lead.yBodyRot);
        partner.setYHeadRot(lead.yHeadRot);
        partner.setXRot(lead.getXRot());
        if (level().isClientSide()) {
            partner.getInterpolation().cancel(); // the server's (older) places for it would pull it back
            partner.yRotO = lead.yRotO;
            partner.yBodyRotO = lead.yBodyRotO;
            partner.yHeadRotO = lead.yHeadRotO;
            partner.xRotO = lead.xRotO;
            partner.setOnGround(lead.onGround());
            // the same stride as the lead horse
            var from = (com.mechanical.mixin.WalkAnimationStateAccessor) lead.walkAnimation;
            var to = (com.mechanical.mixin.WalkAnimationStateAccessor) partner.walkAnimation;
            partner.walkAnimation.setSpeed(lead.walkAnimation.speed());
            to.mechanical$setSpeedOld(from.mechanical$speedOld());
            to.mechanical$setPosition(from.mechanical$position());
        } else partner.getNavigation().stop();
    }

    /** Server: one horse crawls with it empty and can't move it loaded; a pair goes a little slower when it is full. */
    private void slowTeam(LivingEntity lead) {
        AbstractHorse horse = lead instanceof AbstractHorse h ? h : null;
        if (slowed != null && slowed != horse) clearSlow();
        if (horse == null) return;
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double slow = team() ? 0 : loadFraction() > 0 ? 1.0 : CRAWL_SLOW;
        speed.addOrUpdateTransientModifier(new AttributeModifier(TEAM_SLOW, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        slowed = horse;
    }

    private void clearSlow() {
        if (slowed == null) return;
        AttributeInstance speed = slowed.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(TEAM_SLOW);
        slowed = null;
    }

    /** Server: a hungry (hurt) horse in the team eats a bite from a Hay Bale on the bed. */
    private void feedTeam() {
        for (LivingEntity horse : new LivingEntity[] {harness(), partner()}) {
            if (!(horse instanceof AbstractHorse) || horse.getHealth() >= horse.getMaxHealth()) continue;
            for (int i = 0; i < SLOTS; i++) {
                if (module(i) != HAY || hay[i] <= 0) continue;
                horse.heal(BITE_HEAL);
                horse.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.0F);
                if (--hay[i] <= 0) setModule(i, EMPTY);
                break;
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide()) clearSlow();
        super.remove(reason);
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            Vec3 dir = Vec3.directionFromRotation(0, getYRot());
            double along = (getX() - xo) * dir.x + (getZ() - zo) * dir.z;
            wheelSpinO = wheelSpin;
            wheelSpin += (float) (along / WHEEL_RADIUS);
            steerO = steer;
        }
        super.tick();
        if (level().isClientSide()) {
            // the front axle and pole lie along the horses, exactly (the bed trails them, towTarget)
            LivingEntity lead = harness();
            steer = lead == null ? steer * 0.7F : Mth.clamp(Mth.wrapDegrees(lead.getYRot() - getYRot()), -MAX_STEER, MAX_STEER);
        }
        if (!level().isClientSide()) {
            entityData.set(FULLNESS, (float) loadFraction());
            keepTeam((ServerLevel) level());
            if (caravan && harness() == null && leadId == null) caravan = false; // the team fell: an ordinary wagon now
            LivingEntity lead = harness();
            keepPair(lead);
            slowTeam(lead);
            if (tickCount % FEED_INTERVAL == 0) feedTeam();
            // a seat taken out from under a rider
            int seated = 0;
            for (Entity e : List.copyOf(getPassengers())) if (e instanceof Player && ++seated > count(SEAT)) e.stopRiding();
        }
    }

    // ---- interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (caravan) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.cart.caravan"));
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getMainHandItem();
        // a log you lift or pull goes on the bed: before any other use of the click (hitching, modules, seats)
        FelledLogEntity dragged = FelledLogEntity.handled(player);
        if (dragged != null) {
            logClicks().put(player, level().getGameTime());
            if (level() instanceof ServerLevel server) loadLog(server, dragged, player);
            return InteractionResult.SUCCESS;
        }
        // a held right-click repeats: right after a log went on or off, the next clicks don't hitch or open anything
        if (justClickedLog(player)) return InteractionResult.SUCCESS;
        int aimed = aimedCell(player), slot = aimed >= 0 ? aimed : slotAt(location), module = moduleFor(held);
        if (module != EMPTY && CarcassHauling.heavy(player) == null) {
            if (!level().isClientSide()) placeModule(player, module, held);
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown() && module(slot) == CHEST && CarcassHauling.heavy(player) == null) { // whatever is in the hand
            if (player instanceof ServerPlayer sp) openChest(sp, slot);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && !player.isShiftKeyDown() && module(slot) == SEAT && !player.isPassenger()) {
            if (!level().isClientSide()) player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof FelledLogEntity) return super.canAddPassenger(passenger);
        if (passenger instanceof Player) {
            int seated = 0;
            for (Entity e : getPassengers()) if (e instanceof Player) seated++;
            return seated < count(SEAT);
        }
        return super.canAddPassenger(passenger);
    }

    @Override
    protected boolean locked() {
        return caravan;
    }

    @Override
    public String saddleAction(AbstractHorse horse) {
        return partner() == null && !inTeam(horse) && freeForHorse(horse) ? "attach" : null;
    }

    @Override
    public List<Hint> hints(Player player) {
        List<Hint> out = new ArrayList<>();
        int cell = slotLookedAt(player), module = cell < 0 ? EMPTY : module(cell);
        out.add(new Hint("harness", harness() != null ? "unhitch_horses" : "hitch_horses"));
        if (module == CHEST) out.add(new Hint("use", paired(cell) ? "open_double_chest" : "open_chest"));
        if (module == SEAT && !player.isPassenger()) out.add(new Hint("use", "sit"));
        if (load() > 0 || carriedLog() != null) out.add(new Hint("sneak_use", "unload"));
        out.add(new Hint("attack", module != EMPTY ? "remove_module" : "pick_up"));
        return out;
    }

    @Override
    protected boolean carriesLivestock() {
        return true;
    }

    /** A seated player gets off beside the bed, next to the seat (right side first), not on top of the wagon. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 dir = Vec3.directionFromRotation(0, getYRot()), right = Vec3.directionFromRotation(0, getYRot() + 90);
        List<Integer> seats = slotsWith(SEAT);
        double along = seats.isEmpty() ? 0 : slotAlong(seats.getFirst());
        double first = !seats.isEmpty() && seats.getFirst() % COLS == 1 ? 1.5 : -1.5; // off the seat's own side first
        for (double side : new double[] {first, -first}) {
            Vec3 at = position().add(dir.scale(along)).add(right.scale(side));
            for (double up : new double[] {0, 1}) {
                Vec3 p = at.add(0, up, 0);
                if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(p))) return p;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void onLoaded(ServerPlayer player) {
        if (load() >= CAPACITY) Kettle.award(player, LOADED_ADVANCEMENT);
    }

    /** Punching a module takes it out; punching anywhere else picks the wagon up (everything falls out). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (caravan) {
            if (source.getEntity() instanceof Player player) com.mechanical.people.Caravans.offended(this, player); // its guards notice
            return false;
        }
        if (source.getEntity() instanceof Player player) {
            int slot = slotLookedAt(player);
            if (slot >= 0 && module(slot) != EMPTY) {
                removeModule(level, slot);
                return true;
            }
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void dropContents(ServerLevel level) {
        for (int i = 0; i < SLOTS; i++) removeModule(level, i);
    }

    /**
     * Bodies lie in the empty cells, front to back in loading order; seated players sit in the seat cells in
     * order. A body's middle is mapped from its place in the row of rooms ({@link #rowOffset}) to the empty cells.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger instanceof FelledLogEntity log) {
            // drawn at LOG_SCALE: two side by side on the floor, the third on top between them; root ends at the front board
            int n = carriedLogs().indexOf(log);
            double s = LOG_SCALE, along = ROWS * ROW_DEPTH / 2 - 0.125 - log.size() * s / 2.0;
            double side = n == 0 ? -s / 2 : n == 1 ? s / 2 : 0, up = n >= 2 ? s : 0;
            Vec3 at = onBed(along, side, BED + up, 0, 0);
            moveFunction.accept(passenger, at.x, at.y, at.z);
            log.setYRot(getYRot() + 180);
            return;
        }
        if (!(passenger instanceof Player)) { // bodies and livestock
            double room = rowOffset(passenger, 1.0) + load() / 2.0; // the body's middle, in rooms from the front
            // the free cells row by row, 2 room each: a whole free row takes bodies crosswise, a half row lengthwise in its cell
            double start = 0, along = 0, side = 0;
            boolean crosswise = false, found = false;
            for (int row = 0; row < ROWS && !found; row++) {
                List<Integer> free = new ArrayList<>();
                for (int c = 0; c < COLS; c++) if (module(row * COLS + c) == EMPTY) free.add(row * COLS + c);
                if (free.isEmpty()) continue;
                double rooms = ROOM_PER_SLOT * free.size();
                crosswise = free.size() == COLS;
                along = slotAlong(row * COLS) + (0.5 - Mth.clamp((room - start) / rooms, 0, 1)) * ROW_DEPTH;
                side = crosswise ? 0 : slotSide(free.getFirst());
                found = room < start + rooms;
                start += rooms;
            }
            if (start == 0) return;
            Vec3 at = onBed(along, side, BED, 0, 0);
            moveFunction.accept(passenger, at.x, at.y, at.z);
            float yaw = getYRot() + (crosswise ? 90.0F : 0.0F);
            passenger.setYRot(yaw);
            if (passenger instanceof LivingEntity animal) {
                animal.setYBodyRot(yaw);
                animal.setYHeadRot(yaw);
            }
            return;
        }
        List<Integer> seats = slotsWith(SEAT);
        int n = 0;
        for (Entity e : getPassengers()) {
            if (e == passenger) break;
            if (e instanceof Player) n++;
        }
        if (n >= seats.size()) return;
        Vec3 offset = passenger.getVehicleAttachmentPoint(this);
        Vec3 at = onBed(slotAlong(seats.get(n)), slotSide(seats.get(n)), BED + SEAT_TOP, 0, 0);
        moveFunction.accept(passenger, at.x - offset.x, at.y - offset.y, at.z - offset.z);
    }

    private List<Integer> slotsWith(int module) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) if (module(i) == module) out.add(i);
        return out;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(MODULES, input.getIntOr("modules", 0));
        entityData.set(PARTNER_RIGHT, input.getBooleanOr("partner_right", false));
        caravan = input.getBooleanOr("caravan", false);
        // wagons saved before D2 have no team keys: they load unhitched, as their team was never saved then
        leadId = input.read("lead_horse", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        partnerId = input.read("partner_horse", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        int[] bites = input.getIntArray("hay").orElse(new int[SLOTS]);
        for (int i = 0; i < SLOTS; i++) {
            hay[i] = i < bites.length ? bites[i] : 0;
            chests[i].clearContent();
            ContainerHelper.loadAllItems(input.childOrEmpty("chest" + i), chests[i].getItems());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("modules", entityData.get(MODULES));
        output.putBoolean("partner_right", entityData.get(PARTNER_RIGHT));
        if (caravan) output.putBoolean("caravan", true);
        if (leadId != null) output.store("lead_horse", net.minecraft.core.UUIDUtil.CODEC, leadId);
        if (partnerId != null) output.store("partner_horse", net.minecraft.core.UUIDUtil.CODEC, partnerId);
        output.putIntArray("hay", hay.clone());
        for (int i = 0; i < SLOTS; i++) if (module(i) == CHEST) ContainerHelper.saveAllItems(output.child("chest" + i), chests[i].getItems());
    }
}
