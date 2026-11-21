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

    /** Right-click an empty cart with a Chest: mounts it. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (caravanHorse != null) {
            if (hand == InteractionHand.MAIN_HAND) player.sendOverlayMessage(Component.translatable("message.mechanical.cart.caravan"));
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !held.is(Items.CHEST) || hasChest() || CarcassHauling.hauled(player) != null)
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
        if (getPassengers().size() >= 2) Kettle.award(player, LOADED_ADVANCEMENT);
    }

    @Override
    protected double lead(LivingEntity puller) {
        return puller instanceof Player ? LEAD_PLAYER : LEAD_HORSE;
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
        super.tick();
    }

    @Override
    protected float targetPitch(LivingEntity puller) {
        double axleY = getY() + AXLE_HEIGHT, tipAngle;
        if (puller != null) {
            double hitch = puller.getY() + puller.getBbHeight() * (puller instanceof Player ? 0.5 : 0.6);
            tipAngle = Math.atan2(hitch - axleY, position().subtract(puller.position()).horizontalDistance() + AXLE_BACK);
        } else {
            double ground = groundAhead(SHAFT_REACH - AXLE_BACK) + 1 / 16.0; // the shaft tips rest on the ground
            tipAngle = Math.asin(Mth.clamp((ground - axleY) / SHAFT_REACH, -1, 1));
        }
        return Mth.clamp((float) (Math.toDegrees(tipAngle) - SHAFT_ANGLE), -20.0F, 25.0F);
    }

    /** Sneak + right-click with an empty hand: the goods box, or the whole mounted chest. */
    @Override
    protected InteractionResult sneakUse(Player player) {
        if (!level().isClientSide()) {
            boolean chest = hasChest();
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> chest ? ChestMenu.sixRows(id, inventory, goods)
                    : new ChestMenu(MenuType.GENERIC_9x2, id, inventory, goods, 2), getDisplayName()));
            playSound(SoundEvents.CHEST_OPEN, 0.5F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Unhitches whoever pulls it; else hitches the tamed horse you ride, a tamed horse you lead, or you. */
    @Override
    protected void toggleHarness(Player player) {
        LivingEntity old = harness();
        if (old != null) {
            setHarness(null);
            player.sendOverlayMessage(Component.translatable(old instanceof Player ? "message.mechanical.travois.unharnessed" : "message.mechanical.cart.unhitched"));
            playSound(SoundEvents.LEAD_UNTIED, 1.0F, 0.8F);
            return;
        }
        AbstractHorse horse = player.getVehicle() instanceof AbstractHorse ridden && ridden.isTamed() ? ridden : ledHorse(player);
        if (horse != null && horse.isBaby()) horse = null;
        setHarness(horse != null ? horse : player);
        player.sendOverlayMessage(Component.translatable(horse != null ? "message.mechanical.cart.hitched" : "message.mechanical.travois.harnessed"));
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
    }

    private AbstractHorse ledHorse(Player player) {
        List<AbstractHorse> led = level().getEntitiesOfClass(AbstractHorse.class, player.getBoundingBox().inflate(HORSE_RANGE),
                h -> h.isTamed() && h.getLeashHolder() == player);
        return led.isEmpty() ? null : led.get(0);
    }

    /** Bodies lie side by side across the bed, lengthwise, each as wide as its room ({@link #rowOffset}). */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        double along = 0, side = rowOffset(passenger, 0.22);
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
        double rise = (along + AXLE_BACK) * Math.sin(Math.toRadians(pitch)); // the bed tips about the axle (client only)
        moveFunction.accept(passenger, getX() + dir.x * along + right.x * side, getY() + BED + rise, getZ() + dir.z * along + right.z * side);
        passenger.setYRot(getYRot());
    }

    @Override
    protected void dropContents(ServerLevel level) {
        Containers.dropContents(level, this, goods);
        if (hasChest()) spawnAtLocation(level, new ItemStack(Items.CHEST));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        goods.clearContent();
        ContainerHelper.loadAllItems(input, goods.getItems());
        entityData.set(CHEST, input.getBooleanOr("chest", false));
        caravanHorse = input.read("caravan_horse", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        entityData.set(CARAVAN, caravanHorse != null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, goods.getItems());
        output.putBoolean("chest", hasChest());
        if (caravanHorse != null) output.store("caravan_horse", net.minecraft.core.UUIDUtil.CODEC, caravanHorse);
    }
}
