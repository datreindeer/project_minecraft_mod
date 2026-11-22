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
import net.minecraft.world.entity.player.Player;
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
 * move it loaded. The bed has {@link #SLOTS} places front to back, each empty ({@link #ROOM_PER_SLOT} room of carcasses:
 * all empty is 12 room, four large bodies) or holding a module: a Chest ({@link #CHEST_SIZE} slots), a Hay Bale (feeds
 * the team: heals the horses while they're hitched) or a seat (any wooden slab; a player rides along).
 * <p>
 * Right-click a place with a Chest, Hay Bale or wooden slab to put the module there; punch a module to take it out (a
 * chest spills, a half-eaten bale gives back its wheat); punch the wagon where there is none to pick it up. Sneak +
 * right-click a chest place to open it, anywhere else to unload the last body (or the log). Right-click it while dragging a
 * felled log ({@link FelledLogEntity}, up to {@link FelledLogEntity#WAGON_MAX} blocks) to load it on an empty bed. Right-click with an empty hand while riding
 * or leading tamed horses to hitch them: the first takes the lead place, the second is yoked beside it. The team is fixed
 * (living-world-plan D1-D2): the second horse keeps exactly beside the first, the wagon rigidly behind them, nothing
 * snaps; sneak + right-click a hitched horse to take it out ({@link #release}). Both are saved by UUID. On a seat place,
 * with no horses about, you sit down.
 */
public class WagonEntity extends TravoisEntity {
    public static final int SLOTS = 4, ROOM_PER_SLOT = 3, CAPACITY = SLOTS * ROOM_PER_SLOT, CHEST_SIZE = 27;
    public static final int EMPTY = 0, CHEST = 1, HAY = 2, SEAT = 3;
    /** Hay Bale: bites (one wheat each, 9 to a bale), each healing a horse 2 health, one bite per horse every 5 s at most. */
    static final int HAY_BITES = 9, FEED_INTERVAL = 100;
    static final float BITE_HEAL = 2.0F;
    /** The four modules, 2 bits each, place 0 (front) in the lowest bits. Synced (renderer). */
    private static final EntityDataAccessor<Integer> MODULES = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.INT);
    /** Entity id of the second horse, yoked beside the lead horse ({@link #harness}), -1 if none. Synced (pole, pull point). */
    private static final EntityDataAccessor<Integer> PARTNER = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.INT);
    /** {@link #loadFraction}, synced: the client doesn't see the chests but moves the wagon too ({@link #pullFactor}). */
    private static final EntityDataAccessor<Float> FULLNESS = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.FLOAT);
    static final Identifier TEAM_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/wagon_team"),
            LOADED_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/full_wagon"),
            CHESTS_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/wagon_chests");
    /** The lead horse's speed modifier while hitched alone (crawl or stand); a pair's load slows it through HorseWork. */
    private static final Identifier TEAM_SLOW = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wagon_team");
    /** Centre of the pair to the wagon centre; the pair's spacing (horse width 1.3 + a gap). */
    public static final double LEAD = 2.8, PAIR = 1.45;
    /** The bed's rear end behind the centre: a towed cart's shafts rest there ({@link #towPoint}). */
    public static final double REAR = SLOTS * 0.75 / 2;
    /** A trailer (Wheeled Cart) weighs on the team by its fullness times this (its 6 room against the wagon's 12). */
    static final double TRAILER_WEIGHT = CartEntity.CAPACITY / (double) CAPACITY;
    /** Bed floor top, wheel radius (WagonModel), place depth along the bed, seat top above the floor (blocks). */
    public static final double BED = 14 / 16.0, WHEEL_RADIUS = 8 / 16.0, SLOT_DEPTH = 0.75, SEAT_TOP = 6 / 16.0;
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
    /** Client: wheel angle (radians) and the front axle's swing (degrees, + to the right), this tick and the last. */
    public float wheelSpin, wheelSpinO, steer, steerO;

    public WagonEntity(EntityType<? extends WagonEntity> type, Level level) {
        super(type, level);
        for (int i = 0; i < SLOTS; i++) {
            chests[i] = new SimpleContainer(CHEST_SIZE) {
                @Override
                public boolean stillValid(Player player) {
                    return isAlive() && player.distanceTo(WagonEntity.this) < 8;
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

    /** Distance of place {@code slot}'s centre ahead of the wagon's centre (place 0 at the front). */
    public static double slotAlong(int slot) {
        return (SLOTS / 2.0 - 0.5 - slot) * SLOT_DEPTH;
    }

    /** The place at {@code local} (a point relative to the wagon's position), by how far along the bed it is. */
    private int slotAt(Vec3 local) {
        double along = local.dot(Vec3.directionFromRotation(0, getYRot()));
        return Mth.clamp((int) Math.floor((SLOTS / 2.0 * SLOT_DEPTH - along) / SLOT_DEPTH), 0, SLOTS - 1);
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

    private void placeModule(Player player, int slot, int module, ItemStack held) {
        if (carriedLog() != null) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.wagon.log_first"));
            return;
        }
        if (module(slot) != EMPTY) {
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
        Vec3 at = position().add(Vec3.directionFromRotation(0, getYRot()).scale(slotAlong(slot))).add(0, BED + 0.3, 0);
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

    /** The place {@code player} is looking at on the wagon, or -1. */
    private int slotLookedAt(Player player) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1.0F).scale(8));
        AABB box = getBoundingBox().inflate(getPickRadius());
        return box.clip(eye, end).map(hit -> slotAt(hit.subtract(position()))).orElse(-1);
    }

    @Override
    public float getPickRadius() {
        // the bed is 3 blocks long but the collision box 2: clicks reach its ends
        return 0.5F;
    }

    /** Place {@code slot}'s chest (WagonTest). */
    SimpleContainer chest(int slot) {
        return chests[slot];
    }

    // ---- the team

    public AbstractHorse partner() {
        int id = entityData.get(PARTNER);
        return id >= 0 && level().getEntity(id) instanceof AbstractHorse h ? h : null;
    }

    void setPartner(AbstractHorse horse) {
        entityData.set(PARTNER, horse == null ? -1 : horse.getId());
        partnerId = horse == null ? null : horse.getUUID();
    }

    /** Remembers the lead horse's UUID; dropped only when it is unhitched or dies ({@link #setLead}), not when it is out of reach (unloaded). */
    @Override
    protected void setHarness(Entity puller) {
        super.setHarness(puller);
        if (puller != null) leadId = puller.getUUID();
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

    /** Between the two horses (or the lead horse alone). */
    @Override
    protected Vec3 pullPoint(Entity puller) {
        AbstractHorse partner = partner();
        return partner == null ? puller.position() : puller.position().add(partner.position()).scale(0.5);
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

    /** Wheels on both axles: the bed stays level. */
    @Override
    protected float targetPitch(Entity puller) {
        return 0;
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
            setPartner((AbstractHorse) lead);
            setHarness(partner);
        }
        // an unridden team stands in the traces instead of wandering off with the wagon
        if (harness() instanceof AbstractHorse h && h.getControllingPassenger() == null) h.getNavigation().stop();
    }

    /** Server: keeps the second horse exactly beside the first, turning with it (D1: horse 2 follows horse 1 exactly). */
    private void keepPair(LivingEntity lead) {
        AbstractHorse partner = partner();
        if (partner == null || lead == null) return;
        Vec3 to = lead.position().add(Vec3.directionFromRotation(0, lead.getYRot() - 90).scale(PAIR));
        partner.getNavigation().stop();
        partner.setPos(to); // exactly, even into a bush: the team is one unit
        // its own tick moves it on as far as the lead horse goes
        partner.setDeltaMovement(lead.getDeltaMovement());
        partner.setYRot(lead.getYRot());
        partner.setYBodyRot(lead.getYRot());
        partner.setYHeadRot(lead.getYRot());
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
            LivingEntity lead = harness();
            float target = 0;
            if (lead != null) {
                // the bed already faces the team; the front axle and pole turn on with the horses
                target = Mth.clamp(Mth.wrapDegrees(lead.getYRot() - getYRot()), -40.0F, 40.0F);
            }
            steer += (target - steer) * 0.3F;
        }
        super.tick();
        if (!level().isClientSide()) {
            entityData.set(FULLNESS, (float) loadFraction());
            keepTeam((ServerLevel) level());
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
        int slot = slotAt(location), module = moduleFor(held);
        if (module != EMPTY && CarcassHauling.hauled(player) == null) {
            if (!level().isClientSide()) placeModule(player, slot, module, held);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && player.isShiftKeyDown() && module(slot) == CHEST) {
            if (!level().isClientSide()) {
                SimpleContainer chest = chests[slot];
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest), getDisplayName()));
                playSound(SoundEvents.CHEST_OPEN, 0.5F, 1.1F);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && !player.isShiftKeyDown() && module(slot) == SEAT && !player.isPassenger()
                && candidates(player, harness(), partner()).isEmpty()) {
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
    protected boolean carriesLivestock() {
        return true;
    }

    @Override
    protected void onLoaded(ServerPlayer player) {
        if (load() >= CAPACITY) Kettle.award(player, LOADED_ADVANCEMENT);
    }

    /** Punching a module takes it out; punching anywhere else picks the wagon up (everything falls out). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
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
     * Bodies lie crosswise in the empty places, front to back in loading order; seated players sit in the seat places in
     * order. A body's middle is mapped from its place in the row of rooms ({@link #rowOffset}) to the empty places.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        if (passenger instanceof FelledLogEntity log) {
            // drawn at LOG_SCALE: two side by side on the floor, the third on top between them; root ends at the front board
            int n = carriedLogs().indexOf(log);
            double s = LOG_SCALE, along = SLOTS * SLOT_DEPTH / 2 - 0.125 - log.size() * s / 2.0;
            double side = n == 0 ? -s / 2 : n == 1 ? s / 2 : 0, up = n >= 2 ? s : 0;
            Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
            moveFunction.accept(passenger, getX() + dir.x * along + right.x * side, getY() + BED + up, getZ() + dir.z * along + right.z * side);
            log.setYRot(getYRot() + 180);
            return;
        }
        if (!(passenger instanceof Player)) { // bodies and livestock
            List<Integer> empty = slotsWith(EMPTY);
            if (empty.isEmpty()) return;
            double room = rowOffset(passenger, 1.0) + load() / 2.0; // the body's middle, in rooms from the front
            int i = Mth.clamp((int) Math.floor(room / ROOM_PER_SLOT), 0, empty.size() - 1);
            double along = slotAlong(empty.get(i)) - (room - i * ROOM_PER_SLOT - ROOM_PER_SLOT / 2.0) * SLOT_DEPTH / ROOM_PER_SLOT;
            moveFunction.accept(passenger, getX() + dir.x * along, getY() + BED, getZ() + dir.z * along);
            passenger.setYRot(getYRot() + 90.0F);
            if (passenger instanceof LivingEntity animal) {
                animal.setYBodyRot(getYRot() + 90.0F);
                animal.setYHeadRot(getYRot() + 90.0F);
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
        double along = slotAlong(seats.get(n));
        Vec3 offset = passenger.getVehicleAttachmentPoint(this);
        moveFunction.accept(passenger, getX() + dir.x * along - offset.x, getY() + BED + SEAT_TOP - offset.y, getZ() + dir.z * along - offset.z);
    }

    private List<Integer> slotsWith(int module) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) if (module(i) == module) out.add(i);
        return out;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(MODULES, input.getIntOr("modules", 0));
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
        output.putInt("modules", entityData.get(MODULES));
        if (leadId != null) output.store("lead_horse", net.minecraft.core.UUIDUtil.CODEC, leadId);
        if (partnerId != null) output.store("partner_horse", net.minecraft.core.UUIDUtil.CODEC, partnerId);
        output.putIntArray("hay", hay.clone());
        for (int i = 0; i < SLOTS; i++) if (module(i) == CHEST) ContainerHelper.saveAllItems(output.child("chest" + i), chests[i].getItems());
    }
}
