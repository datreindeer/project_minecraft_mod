package com.mechanical;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Wheeled Cart: the Bronze Age travois. A plank bed with side boards on two spoked wheels and a pair of shafts. Pulled by
 * a player (right-click with an empty hand, as the travois) or by a tamed horse (right-click it while riding one, or while
 * leading one on a lead). Holds {@link #CAPACITY} room of carcasses (two large, three medium or six light ones, loaded like
 * the travois, lying side by side; sneak + right-click a loaded body to take it off) and {@link #SLOTS} slots of goods
 * (sneak + right-click the cart with an empty hand). Right-click an empty cart with a Chest to mount it: a storage cart,
 * {@link #CHEST_SLOTS} slots, no room left for bodies. Punch it to pick it up: bodies, goods and the chest fall out.
 * A Trader Caravan's cart ({@link #hitchCaravan}) wears a canvas tilt, stays hitched to its horse and is locked to players
 * until the horse dies.
 * <p>
 * A trailer: right-click it with an empty hand while it stands behind a Wagon, within {@link #TOW_RANGE} of the wagon's
 * rear, and it is hitched behind (the wagon then is its {@link #puller}), its shafts resting on the wagon's back board; its
 * load weighs on the wagon's team ({@link WagonEntity#loadFraction}). Right-click again to unhitch. A wagon tows one cart.
 * A horse in the shafts and a towing wagon are saved by UUID and found again after a reload (a player in the shafts is not).
 */
public class CartEntity extends TravoisEntity {
    public static final int CAPACITY = 6, SLOTS = 18, CHEST_SLOTS = 54;
    /** A Chest mounted on the bed. Synced (renderer). */
    private static final EntityDataAccessor<Boolean> CHEST = SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.BOOLEAN);
    /** A Trader Caravan's cart (people.Caravans): covered with a canvas tilt. Synced (renderer). */
    private static final EntityDataAccessor<Boolean> CARAVAN = SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.BOOLEAN);
    /** Advancements granted from code, see scripts/gen_advancements.py. */
    static final Identifier LOADED_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "bronze_age/full_cart"),
            STORAGE_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "bronze_age/storage_cart");
    /** Behind a player (at the shaft tips) and behind a horse (between the shafts). */
    public static final double LEAD_PLAYER = 2.6, LEAD_HORSE = 2.0;
    /** Bed height, and the wheels' radius (blocks) for their spin. */
    static final double BED = 0.7, WHEEL_RADIUS = 7 / 16.0;
    /** Speed lost by a player in the shafts: empty, plus per room of load (travois: 0.1 + 0.07). Horses pull it freely. */
    static final double SLOW = 0.05, SLOW_LOAD = 0.03;
    /**
     * Shaft geometry (CartModel): the axle sits {@link #AXLE_BACK} behind the centre, {@link #AXLE_HEIGHT} up; the shaft
     * tips are {@link #SHAFT_REACH} from the axle, {@link #SHAFT_ANGLE} degrees above level when the bed is level. The bed
     * tips about the axle ({@link #pitch}): shaft tips at the puller's hip (a horse: its shoulder), on the ground when parked.
     */
    static final double AXLE_BACK = 2 / 16.0, AXLE_HEIGHT = 7 / 16.0, SHAFT_REACH = 2.75, SHAFT_ANGLE = 7.75;
    /** Behind a wagon's tow point (the shaft tips reach a little over its back board); how far from it a cart can be hitched. */
    public static final double LEAD_TOWED = 2.3, TOW_RANGE = 4.0;
    /** Shaft tips on a wagon's back board: its top, above the wagon's position (WagonModel). */
    static final double TOW_HEIGHT = 19 / 16.0;
    static final Identifier TRAILER_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/wagon_trailer");
    /** Search range for a led horse to put in the shafts. */
    private static final double HORSE_RANGE = 10;

    /** The goods box uses the first {@link #SLOTS}; a mounted chest opens all of them. */
    private final SimpleContainer goods = new SimpleContainer(CHEST_SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return isAlive() && player.distanceTo(CartEntity.this) < 8;
        }
    };
    /** A caravan cart's horse: it stays hitched to it (found again by id after a reload), never torn loose. */
    private java.util.UUID caravanHorse;
    /** The horse in the shafts or the wagon towing it, saved: found again after a reload or when it comes back into reach. */
    private java.util.UUID hitchId;
    /** Client: wheel angle (radians), this tick and the last. */
    public float wheelSpin, wheelSpinO;

    public CartEntity(EntityType<? extends CartEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHEST, false);
        builder.define(CARAVAN, false);
    }

    @Override
    protected void setHarness(Entity puller) {
        super.setHarness(puller);
        if (puller != null) hitchId = puller instanceof Player ? null : puller.getUUID(); // null (out of reach): remembered
    }

    @Override
    protected void unhitched() {
        hitchId = null;
    }

    /** The shafts are fixed to the cart: a horse backing up pushes it back (a player in them only pulls). */
    @Override
    protected Vec3 towTarget(Entity puller) {
        if (!(puller instanceof AbstractHorse)) return super.towTarget(puller);
        Vec3 at = pullPoint(puller), off = position().subtract(at).multiply(1, 0, 1);
        double d = off.length();
        if (d < 0.3) return null;
        setYRot((float) Math.toDegrees(Math.atan2(off.x, -off.z)));
        return at.add(off.scale(lead(puller) / d));
    }

    public boolean isCaravan() {
        return entityData.get(CARAVAN);
    }

    /** Server: a caravan's cart behind {@code horse}, with a chest or empty (for a carcass); locked to players while the horse lives. */
    public void hitchCaravan(AbstractHorse horse, boolean chest) {
        caravanHorse = horse.getUUID();
        entityData.set(CARAVAN, true);
        entityData.set(CHEST, chest);
        setHarness(horse);
    }

    /** Server: finds its horse again (reloads lose the harness), and keeps up with it instead of breaking loose. */
    private void keepHitched(ServerLevel level) {
        LivingEntity horse = harness();
        if (horse != null && !horse.isAlive()) {
            caravanHorse = null; // the horse fell: an ordinary cart now, free to take
            entityData.set(CARAVAN, false);
            return;
        }
        if (horse == null && level.getEntity(caravanHorse) instanceof AbstractHorse h && h.isAlive()) setHarness(horse = h);
        if (horse != null && horse.distanceTo(this) > BREAK - 1) {
            Vec3 back = Vec3.directionFromRotation(0, horse.getYRot()).scale(-LEAD_HORSE);
            setPos(horse.getX() + back.x, horse.getY(), horse.getZ() + back.z);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (caravanHorse != null && source.getEntity() instanceof Player player) com.mechanical.people.Caravans.offended(this, player); // its guards notice
        return caravanHorse == null && super.hurtServer(level, source, damage);
    }

    public boolean hasChest() {
        return entityData.get(CHEST);
    }

    @Override
    public int capacity() {
        return hasChest() ? 0 : CAPACITY;
    }

    /** A storage cart is as heavy as its chest is full. */
    @Override
    public double loadFraction() {
        if (!hasChest()) return super.loadFraction();
        int filled = 0;
        for (ItemStack s : goods.getItems()) if (!s.isEmpty()) filled++;
        return filled / (double) CHEST_SLOTS;
    }

    /** One log of up to {@link #LOG_MAX} blocks on the bed. */
    public static final int LOG_MAX = 2;

    @Override
    public int logSlots() {
        return 1;
    }

    @Override
    public int logMax() {
        return LOG_MAX;
    }

    @Override
    protected boolean logRoom() {
        return !hasChest() && super.logRoom();
    }

    /** Right-click an empty cart with a Chest: mounts it. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (caravanHorse != null) {
            if (hand == InteractionHand.MAIN_HAND) player.sendOverlayMessage(Component.translatable("message.mechanical.cart.caravan"));
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !held.is(Items.CHEST) || hasChest() || CarcassHauling.heavy(player) != null)
            return super.interact(player, hand, location);
        if (!level().isClientSide()) {
            if (!getPassengers().isEmpty()) player.sendOverlayMessage(Component.translatable("message.mechanical.cart.unload_first"));
            else {
                entityData.set(CHEST, true);
                held.consume(1, player);
                playSound(SoundEvents.WOOD_PLACE, 1.0F, 0.9F);
                player.sendOverlayMessage(Component.translatable("message.mechanical.cart.chest_mounted"));
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, STORAGE_ADVANCEMENT);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onLoaded(net.minecraft.server.level.ServerPlayer player) {
        if (getPassengers().stream().filter(e -> e instanceof CarcassEntity).count() >= 2) Kettle.award(player, LOADED_ADVANCEMENT);
    }

    @Override
    protected boolean carriesLivestock() {
        return true;
    }

    @Override
    protected double lead(Entity puller) {
        return puller instanceof Player ? LEAD_PLAYER : puller instanceof WagonEntity ? LEAD_TOWED : LEAD_HORSE;
    }

    /** Behind a wagon: its rear tow point. */
    @Override
    protected Vec3 pullPoint(Entity puller) {
        return puller instanceof WagonEntity w ? w.towPoint() : puller.position();
    }

    @Override
    public double slowdown() {
        return SLOW + SLOW_LOAD * load();
    }

    /** On wheels you can still run with a light load. */
    @Override
    public boolean blocksSprint() {
        return load() > 3;
    }

    @Override
    protected String messageKey() {
        return "cart";
    }

    @Override
    protected Item item() {
        return Carting.CART_ITEM.get();
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            // roll the wheels by how far it moved along its facing since the last tick
            Vec3 dir = Vec3.directionFromRotation(0, getYRot());
            double along = (getX() - xo) * dir.x + (getZ() - zo) * dir.z;
            wheelSpinO = wheelSpin;
            wheelSpin += (float) (along / WHEEL_RADIUS);
        } else if (caravanHorse != null) keepHitched((ServerLevel) level());
        else if (harnessId() < 0 && hitchId != null && tickCount % 10 == 0) {
            Entity e = ((ServerLevel) level()).getEntity(hitchId);
            if (e != null && e.isAlive() && e.distanceTo(this) < BREAK + WagonEntity.REAR) super.setHarness(e); // a saved hitch, or one back in reach
        }
        super.tick();
    }

    /** Wheels: 13 px either side of the middle (CartModel), on the axle. */
    public static final double WHEEL_SIDE = 13 / 16.0;

    /** Its two wheels: the axle rests at their mean height, tipped sideways by their difference. */
    @Override
    protected void settleOnWheels() {
        double left = groundUnder(-AXLE_BACK, -WHEEL_SIDE, WHEEL_RADIUS), right = groundUnder(-AXLE_BACK, WHEEL_SIDE, WHEEL_RADIUS);
        settle((left + right) / 2, (float) Math.toDegrees(Math.atan2(right - left, 2 * WHEEL_SIDE)));
    }

    @Override
    protected float targetPitch(Entity puller) {
        double axleY = groundY + AXLE_HEIGHT, tipAngle;
        if (puller != null) {
            double hitch = puller.getY() + (puller instanceof WagonEntity ? TOW_HEIGHT : puller.getBbHeight() * (puller instanceof Player ? 0.5 : 0.6));
            tipAngle = Math.atan2(hitch - axleY, position().subtract(pullPoint(puller)).horizontalDistance() + AXLE_BACK);
        } else {
            double ground = groundAhead(SHAFT_REACH - AXLE_BACK) + 1 / 16.0; // the shaft tips rest on the ground
            tipAngle = Math.asin(Mth.clamp((ground - axleY) / SHAFT_REACH, -1, 1));
        }
        return Mth.clamp((float) (Math.toDegrees(tipAngle) - SHAFT_ANGLE), -20.0F, 25.0F);
    }

    @Override
    protected boolean locked() {
        return caravanHorse != null;
    }

    @Override
    public java.util.List<Hint> hints(Player player) {
        java.util.List<Hint> out = new java.util.ArrayList<>();
        Entity puller = puller();
        out.add(new Hint("harness", puller == null ? "grab" : puller == player ? "release" : "unhitch"));
        out.add(new Hint("use", hasChest() ? "open_chest" : "open_storage"));
        if (load() > 0 || carriedLog() != null) out.add(new Hint("sneak_use", "unload"));
        out.add(new Hint("attack", "pick_up"));
        return out;
    }

    /** Right-click with an empty hand: the goods box, or the whole mounted chest. */
    @Override
    protected InteractionResult plainUse(Player player) {
        return openGoods(player);
    }

    /** Sneak + right-click with an empty hand: unloads the last log or body; an empty cart opens its goods as a plain click does. */
    @Override
    protected InteractionResult sneakUse(Player player) {
        InteractionResult logs = unloadLogUse(player);
        if (logs != null) return logs;
        if (load() > 0) {
            if (!level().isClientSide()) unloadLast();
            return InteractionResult.SUCCESS;
        }
        return openGoods(player);
    }

    private InteractionResult openGoods(Player player) {
        if (!level().isClientSide()) {
            boolean chest = hasChest();
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> chest ? ChestMenu.sixRows(id, inventory, goods)
                    : new ChestMenu(MenuType.GENERIC_9x2, id, inventory, goods, 2), getDisplayName()));
            playSound(SoundEvents.CHEST_OPEN, 0.5F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Unhitches whoever pulls it; else hitches it behind a wagon just ahead, or the tamed horse you ride, a tamed horse you lead, or you. */
    @Override
    protected void toggleHarness(Player player) {
        Entity old = puller();
        if (old != null) {
            setHarness(null);
            unhitched();
            player.sendOverlayMessage(Component.translatable(old instanceof Player ? "message.mechanical.travois.unharnessed"
                    : old instanceof WagonEntity ? "message.mechanical.cart.untowed" : "message.mechanical.cart.unhitched"));
            playSound(SoundEvents.LEAD_UNTIED, 1.0F, 0.8F);
            return;
        }
        WagonEntity wagon = wagonAhead();
        if (wagon != null) {
            if (pullsOther(wagon)) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.cart.wagon_has_trailer"));
                return;
            }
            setHarness(wagon);
            player.sendOverlayMessage(Component.translatable("message.mechanical.cart.towed"));
            playSound(SoundEvents.LEAD_TIED, 1.0F, 0.7F);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, TRAILER_ADVANCEMENT);
            return;
        }
        AbstractHorse horse = player.getVehicle() instanceof AbstractHorse ridden && ridden.isTamed() ? ridden : ledHorse(player);
        if (horse != null && horse.isBaby()) horse = null;
        if (pullsOther(horse != null ? horse : player)) {
            sayAlreadyPulling(player);
            return;
        }
        setHarness(horse != null ? horse : player);
        player.sendOverlayMessage(Component.translatable(horse != null ? "message.mechanical.cart.hitched" : "message.mechanical.travois.harnessed"));
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
    }

    /** The nearest wagon this cart stands behind, within {@link #TOW_RANGE} of its tow point, or null. */
    WagonEntity wagonAhead() {
        WagonEntity best = null;
        double bestDist = TOW_RANGE;
        for (WagonEntity w : level().getEntitiesOfClass(WagonEntity.class, getBoundingBox().inflate(TOW_RANGE + WagonEntity.REAR))) {
            double d = w.towPoint().distanceTo(position());
            boolean behind = position().subtract(w.position()).dot(Vec3.directionFromRotation(0, w.getYRot())) < 0;
            if (behind && d <= bestDist) {
                best = w;
                bestDist = d;
            }
        }
        return best;
    }

    private AbstractHorse ledHorse(Player player) {
        List<AbstractHorse> led = level().getEntitiesOfClass(AbstractHorse.class, player.getBoundingBox().inflate(HORSE_RANGE),
                h -> h.isTamed() && h.getLeashHolder() == player);
        return led.isEmpty() ? null : led.get(0);
    }

    /** Bodies lie side by side across the bed, lengthwise, each as wide as its room ({@link #rowOffset}). */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger instanceof FelledLogEntity log) { // drawn at LOG_SCALE, lengthwise, its root end at the front board
            double along = 0.75 - log.size() * LOG_SCALE / 2.0;
            Vec3 at = onBed(along, 0, BED, -AXLE_BACK, AXLE_HEIGHT);
            moveFunction.accept(passenger, at.x, at.y, at.z);
            log.setYRot(getYRot() + 180);
            return;
        }
        Vec3 at = onBed(0, rowOffset(passenger, 0.22), BED, -AXLE_BACK, AXLE_HEIGHT); // the bed tips about the axle (client only)
        moveFunction.accept(passenger, at.x, at.y, at.z);
        passenger.setYRot(getYRot());
        if (passenger instanceof LivingEntity animal) { // livestock stand facing ahead
            animal.setYBodyRot(getYRot());
            animal.setYHeadRot(getYRot());
        }
    }

    @Override
    protected void dropContents(ServerLevel level) {
        Containers.dropContents(level, this, goods);
        if (hasChest()) spawnAtLocation(level, new ItemStack(Items.CHEST));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        goods.clearContent();
        ContainerHelper.loadAllItems(input, goods.getItems());
        entityData.set(CHEST, input.getBooleanOr("chest", false));
        caravanHorse = input.read("caravan_horse", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        hitchId = input.read("hitched", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        entityData.set(CARAVAN, caravanHorse != null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ContainerHelper.saveAllItems(output, goods.getItems());
        output.putBoolean("chest", hasChest());
        if (caravanHorse != null) output.store("caravan_horse", net.minecraft.core.UUIDUtil.CODEC, caravanHorse);
        if (hitchId != null) output.store("hitched", net.minecraft.core.UUIDUtil.CODEC, hitchId);
    }
}
