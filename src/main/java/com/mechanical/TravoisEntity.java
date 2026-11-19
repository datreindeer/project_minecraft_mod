package com.mechanical;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Travois: two long poles lashed into a V with a hide stretched between them, dragged on the ground. Right-click with an
 * empty hand to harness it (or unharness); harnessed it follows {@link #LEAD} blocks behind you, climbing steps. Holds
 * {@link #CAPACITY} load units: one large or two medium/small carcasses (right-click it with the one you haul). Sneak +
 * right-click with an empty hand unloads the last one. Loaded carcasses are its passengers. Punch it to pick it up.
 */
public class TravoisEntity extends Entity {
    /** Entity id of the player in the harness, -1 if none. Synced (sprint check, renderer). */
    private static final EntityDataAccessor<Integer> HARNESS = SynchedEntityData.defineId(TravoisEntity.class, EntityDataSerializers.INT);
    public static final double LEAD = 2.4;
    public static final int CAPACITY = 2;
    /** Height of the hide platform above the ground. */
    static final double PLATFORM = 0.4;

    public TravoisEntity(EntityType<? extends TravoisEntity> type, Level level) {
        super(type, level);
    }

    public int harnessId() {
        return entityData.get(HARNESS);
    }

    /** Who pulls it: a player, or a tamed horse for the {@link CartEntity}. */
    public LivingEntity harness() {
        int id = harnessId();
        return id >= 0 && level().getEntity(id) instanceof LivingEntity e ? e : null;
    }

    protected void setHarness(Entity puller) {
        entityData.set(HARNESS, puller == null ? -1 : puller.getId());
    }

    /** Load units it holds. */
    public int capacity() {
        return CAPACITY;
    }

    /** How far behind {@code puller} it follows. */
    protected double lead(LivingEntity puller) {
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

    /** The travois {@code player} pulls, or null. */
    public static TravoisEntity harnessedTo(Player player) {
        for (TravoisEntity t : player.level().getEntitiesOfClass(TravoisEntity.class, player.getBoundingBox().inflate(10), t -> t.harnessId() == player.getId()))
            return t;
        return null;
    }

    /** Load units on it. */
    public int load() {
        int units = 0;
        for (Entity e : getPassengers()) if (e instanceof CarcassEntity c) units += c.loadUnits();
        return units;
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
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        LivingEntity p = harness();
        if (harnessId() >= 0 && (p == null || !p.isAlive() || p.distanceTo(this) > 10)) {
            entityData.set(HARNESS, -1); // dead, gone or stuck far behind
            p = null;
        }
        double fall = onGround() ? -0.01 : Math.max(-0.6, getDeltaMovement().y - 0.04);
        Vec3 pull = Vec3.ZERO;
        if (p != null) {
            Vec3 off = position().subtract(p.position()).multiply(1, 0, 1);
            double d = off.length();
            double lead = lead(p);
            if (d > lead) pull = off.scale((lead - d) / d);
            if (d > 0.3) setYRot((float) Math.toDegrees(Math.atan2(off.x, -off.z))); // the poles point at the one pulling
        }
        move(MoverType.SELF, new Vec3(pull.x, fall, pull.z));
        setDeltaMovement(0, onGround() ? 0 : fall, 0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        CarcassEntity hauled = CarcassHauling.hauled(player);
        if (hauled != null) {
            if (!level().isClientSide()) loadOn(hauled, player);
            return InteractionResult.SUCCESS;
        }
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return sneakUse(player);
        if (!level().isClientSide()) toggleHarness(player);
        return InteractionResult.SUCCESS;
    }

    /** Sneak + right-click with an empty hand: unloads the last body. */
    protected InteractionResult sneakUse(Player player) {
        if (getPassengers().isEmpty()) return InteractionResult.PASS;
        if (!level().isClientSide()) unloadLast();
        return InteractionResult.SUCCESS;
    }

    /** Server: right-click with an empty hand puts {@code player} in the harness, or takes them out. */
    protected void toggleHarness(Player player) {
        boolean off = harnessId() == player.getId();
        setHarness(off ? null : player);
        player.sendOverlayMessage(Component.translatable(off ? "message.mechanical.travois.unharnessed" : "message.mechanical.travois.harnessed"));
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
    }

    private void loadOn(CarcassEntity carcass, Player player) {
        if (carcass.isHuge()) player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".too_big"));
        else if (load() + carcass.loadUnits() > capacity()) player.sendOverlayMessage(Component.translatable("message.mechanical." + messageKey() + ".full"));
        else {
            carcass.setHauler(null);
            carcass.startRiding(this, true, true);
            playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.6F);
        }
    }

    private void unloadLast() {
        List<Entity> on = getPassengers();
        Entity last = on.get(on.size() - 1);
        last.stopRiding();
        // beside the travois, or where it lay if there is no room
        Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90).scale(1.3);
        Vec3 was = last.position();
        last.setPos(position().add(side).add(0, 0.2, 0));
        if (!level().noCollision(last)) last.setPos(was);
        playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 0.5F);
    }

    /** Loaded bodies lie along the hide: one in the middle, or two one behind the other. */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        List<Entity> on = getPassengers();
        int i = on.indexOf(passenger);
        double along = on.size() == 1 ? -0.4 : i == 0 ? 0.25 : -1.0;
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        moveFunction.accept(passenger, getX() + dir.x * along, getY() + PLATFORM, getZ() + dir.z * along);
        passenger.setYRot(getYRot());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof CarcassEntity c && load() + c.loadUnits() <= capacity();
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
