package com.mechanical;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Something too heavy to pick up that players drag along by hand ({@link CarcassHauling}): sneak + right-click with an
 * empty hand takes hold, up to {@link #MAX_HAULERS} players each at their own grip ({@link #gripPoint}). Fewer than
 * {@link #pullersToBudge} can't move it at all, fewer than {@link #pullersNeeded} only make it creep. It can go on a
 * Travois, Cart or Wagon ({@link #loadUnits}, {@link #fitsOnTravois}). Carcasses ({@link CarcassEntity}) and the
 * Elkwight's axe (wildlife.elkwight.ElkwightAxeEntity) are heavy loads.
 */
public abstract class HeavyLoad extends Entity {
    /** Players who can drag one load together, each holding his own grip. */
    public static final int MAX_HAULERS = 4;
    /** Entity ids of the players holding it, -1 for a free grip. Synced for the renderers, the sprint check and CarcassHauling#heavy. */
    private static final List<EntityDataAccessor<Integer>> HAULERS = java.util.stream.IntStream.range(0, MAX_HAULERS)
            .mapToObj(i -> SynchedEntityData.defineId(HeavyLoad.class, EntityDataSerializers.INT)).toList();
    /** Server: the players holding it, so one that isn't (yet) looked up by id in the level still counts. */
    private final Player[] holding = new Player[MAX_HAULERS];

    protected HeavyLoad(EntityType<? extends HeavyLoad> type, Level level) {
        super(type, level);
    }

    /** Where the player holding grip {@code slot} takes hold, at the frame. */
    public abstract Vec3 gripPoint(int slot, float partialTicks);

    /** Dragged behind its pullers (true), or carried on one player's shoulders (small carcasses). */
    public boolean isDragged() {
        return true;
    }

    /** Pullers it takes to drag it at full pace (shared slowdown); fewer only creep (CarcassHauling#STRAIN_CREEP). */
    public int pullersNeeded() {
        return 1;
    }

    /** Pullers it takes to move it at all: fewer can only strain at it. */
    public int pullersToBudge() {
        return 1;
    }

    /** Room it takes on a travois (holds 3), a cart (6) or a wagon. */
    public abstract int loadUnits();

    /** Goes on a travois, cart or wagon at all. */
    public boolean fitsOnTravois() {
        return true;
    }

    /** Held, it stops its haulers sprinting. */
    public boolean blocksSprint() {
        return true;
    }

    /**
     * Server: right-clicking block {@code pos} (face {@code face}) while holding it: true if it did something of its own
     * there (the axe stands up as a trophy); false puts it down as usual.
     */
    public boolean putDownOn(ServerLevel level, BlockPos pos, Direction face, Player player) {
        return false;
    }

    /** Entity id of the first player hauling it, -1 if none. */
    public int haulerId() {
        for (var slot : HAULERS) if (entityData.get(slot) >= 0) return entityData.get(slot);
        return -1;
    }

    /** Entity id of the player holding grip {@code slot}, -1 if free. */
    public int haulerId(int slot) {
        return entityData.get(HAULERS.get(slot));
    }

    /** The grip {@code player} holds, or -1. */
    public int slotOf(Player player) {
        for (int i = 0; i < MAX_HAULERS; i++) if (haulerId(i) == player.getId()) return i;
        return -1;
    }

    public int haulerCount() {
        int n = 0;
        for (int i = 0; i < MAX_HAULERS; i++) if (haulerId(i) >= 0) n++;
        return n;
    }

    /** The player holding grip {@code slot}, or null. */
    public Player hauler(int slot) {
        int id = haulerId(slot);
        if (id < 0) return null;
        if (holding[slot] != null && holding[slot].getId() == id) return holding[slot];
        return level().getEntity(id) instanceof Player p ? p : null;
    }

    /** The (first) player carrying or dragging it, or null. */
    public Player hauler() {
        for (int i = 0; i < MAX_HAULERS; i++) if (haulerId(i) >= 0) return hauler(i);
        return null;
    }

    /** null: everyone lets go; a player: he alone holds it. */
    void setHauler(Player player) {
        for (int i = 0; i < MAX_HAULERS; i++) removeHauler(i);
        if (player != null) {
            entityData.set(HAULERS.get(0), player.getId());
            holding[0] = player;
        }
    }

    /** Takes the first free grip; false when all are held (or a carried one is already held by someone). */
    boolean addHauler(Player player) {
        for (int i = 0; i < (isDragged() ? MAX_HAULERS : 1); i++) {
            if (haulerId(i) < 0) {
                entityData.set(HAULERS.get(i), player.getId());
                holding[i] = player;
                return true;
            }
        }
        return false;
    }

    void removeHauler(int slot) {
        entityData.set(HAULERS.get(slot), -1);
        holding[slot] = null;
    }

    /** Server tick: lets go of haulers who died, left or fell behind, then follows the rest. False: nobody holds it. */
    protected boolean tickHaulers() {
        if (haulerId() < 0) return false;
        for (int i = 0; i < MAX_HAULERS; i++) {
            Player hauler = hauler(i);
            if (haulerId(i) >= 0 && (hauler == null || !hauler.isAlive() || hauler.distanceTo(this) >= 12)) removeHauler(i);
        }
        if (haulerId() < 0) return false;
        CarcassHauling.follow(this);
        return true;
    }

    /** Server, lying free: falls and settles like a dropped item; in water it rises and floats half under, drifting. */
    protected void settle() {
        Vec3 v = getDeltaMovement();
        if (getFluidHeight(FluidTags.WATER) > getBbHeight() * 0.5) setDeltaMovement(v.x, Math.min(v.y + 0.01, 0.06), v.z);
        else if (!onGround()) setDeltaMovement(v.add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.5, 0.98, 0.5));
    }

    /** Moves it by {@code delta} as far as blocks allow (dragging). */
    void drag(Vec3 delta) {
        move(MoverType.SELF, delta);
    }

    /** Dragged, it slides up steps and over roots. */
    @Override
    public float maxUpStep() {
        return haulerId() >= 0 ? 1.0F : 0.0F;
    }

    /** Client: glides between the server's positions instead of jumping, a step up smoothed over 4 ticks. */
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 4);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        for (var slot : HAULERS) builder.define(slot, -1);
    }
}
