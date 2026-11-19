package com.mechanical;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Wheeled Cart: the Bronze Age travois. A plank bed with side boards on two spoked wheels and a pair of shafts. Pulled by
 * a player (right-click with an empty hand, as the travois) or by a tamed horse (right-click it while riding one, or while
 * leading one on a lead). Holds {@link #CAPACITY} load units of carcasses (two large or four medium, loaded like the
 * travois; sneak + right-click a loaded body to take it off) and {@link #SLOTS} slots of goods (sneak + right-click the
 * cart with an empty hand). Punch it to pick it up: bodies and goods fall out.
 */
public class CartEntity extends TravoisEntity {
    public static final int CAPACITY = 4, SLOTS = 18;
    /** Behind a player (at the shaft tips) and behind a horse (between the shafts). */
    public static final double LEAD_PLAYER = 2.6, LEAD_HORSE = 2.0;
    /** Bed height, and the wheels' radius (blocks) for their spin. */
    static final double BED = 0.7, WHEEL_RADIUS = 7 / 16.0;
    /** Speed lost by a player in the shafts: empty, plus per load unit (travois: 0.1 + 0.1). Horses pull it freely. */
    static final double SLOW = 0.05, SLOW_LOAD = 0.04;
    /** Search range for a led horse to put in the shafts. */
    private static final double HORSE_RANGE = 10;

    private final SimpleContainer goods = new SimpleContainer(SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return isAlive() && player.distanceTo(CartEntity.this) < 8;
        }
    };
    /** Client: wheel angle (radians), this tick and the last. */
    public float wheelSpin, wheelSpinO;

    public CartEntity(EntityType<? extends CartEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int capacity() {
        return CAPACITY;
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
        return load() > 2;
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
        }
        super.tick();
    }

    /** Sneak + right-click with an empty hand: the goods box. */
    @Override
    protected InteractionResult sneakUse(Player player) {
        if (!level().isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inventory, goods, 2), getDisplayName()));
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

    /**
     * Bodies lie on the bed in two halves (front, back). A large one takes a whole half, medium/small ones take a side
     * each; the layout is laid out again in passenger order, so whatever {@link #canAddPassenger} let on always fits.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        boolean[] used = new boolean[4]; // front left, front right, back left, back right
        double along = 0, side = 0;
        for (Entity e : getPassengers()) {
            boolean large = e instanceof CarcassEntity c && c.loadUnits() > 1;
            int slot = -1;
            for (int s = 0; s < 4 && slot < 0; s++) {
                if (large ? s % 2 == 0 && !used[s] && !used[s + 1] : !used[s]) slot = s;
            }
            if (slot < 0) slot = 0; // can't happen within capacity
            used[slot] = true;
            if (large) used[slot + 1] = true;
            if (e == passenger) {
                along = slot < 2 ? 0.4 : -0.45;
                side = large ? 0 : slot % 2 == 0 ? 0.3 : -0.3;
                break;
            }
        }
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
        moveFunction.accept(passenger, getX() + dir.x * along + right.x * side, getY() + BED, getZ() + dir.z * along + right.z * side);
        passenger.setYRot(getYRot());
    }

    @Override
    protected void dropContents(ServerLevel level) {
        Containers.dropContents(level, this, goods);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        goods.clearContent();
        ContainerHelper.loadAllItems(input, goods.getItems());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, goods.getItems());
    }
}
