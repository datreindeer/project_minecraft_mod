package com.mechanical;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Mouldboard Plough: the Iron Age's horse-drawn plough. A long beam on a small fore-carriage wheel, an iron share,
 * coulter and mouldboard, two handles. A tamed horse pulls it (right-click it with an empty hand while riding or leading
 * one; players can't pull it). With the share down ({@link #isShareDown}), every tick it moves it turns a
 * {@link #HALF_WIDTH 5-wide} strip of dirt, grass, dirt path, podzol or loess under its share into farmland at fertility
 * {@link #PLOUGHED}, one above a hand-tilled field. Only blocks with air above them: crops, snow and anything else are
 * left alone. Hitched, right-click lowers or raises the share; sneak + right-click on foot unhitches. Punch it to pick it up.
 */
public class PloughEntity extends TravoisEntity {
    /** The share is in the ground. Synced (renderer tilt). Off when placed, so it can be moved to the field first. */
    private static final EntityDataAccessor<Boolean> SHARE_DOWN = SynchedEntityData.defineId(PloughEntity.class, EntityDataSerializers.BOOLEAN);
    static final Identifier FURROW_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/first_furrow");
    /** Horse centre to plough centre (the swingletree is 1.9 ahead of the centre, PloughModel). */
    public static final double LEAD = 3.0;
    /** The share behind the plough's centre (PloughModel: share at z 0..+2 px, the centre at 0). */
    static final double SHARE_BACK = 0.1;
    /** The strip reaches this many blocks to each side of the share: 5 wide. */
    static final int HALF_WIDTH = 2;
    /** Ploughed farmland: one above a hand-tilled field, capped (podzol and loess already till to the top). */
    static final int PLOUGHED = Math.min(Fertility.MAX, Fertility.NEW_FIELD + 1);
    /** Client tilt about the wheel (degrees, front up): share digging in, or lifted clear. */
    static final float TILT_DOWN = 4.0F, TILT_UP = -5.0F;
    /** Fore-carriage wheel radius (blocks, PloughModel) for its spin. */
    static final double WHEEL_RADIUS = 5 / 16.0;
    private static final double HORSE_RANGE = 10;
    /** Client: wheel angle (radians), this tick and the last. */
    public float wheelSpin, wheelSpinO;

    public PloughEntity(EntityType<? extends PloughEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHARE_DOWN, false);
    }

    public boolean isShareDown() {
        return entityData.get(SHARE_DOWN);
    }

    void setShareDown(boolean down) {
        entityData.set(SHARE_DOWN, down);
    }

    /** Carries no bodies. */
    @Override
    public int capacity() {
        return 0;
    }

    @Override
    protected double lead(Entity puller) {
        return LEAD;
    }

    @Override
    protected float targetPitch(Entity puller) {
        return isShareDown() ? TILT_DOWN : TILT_UP;
    }

    @Override
    protected String messageKey() {
        return "plough";
    }

    @Override
    protected Item item() {
        return Carting.PLOUGH_ITEM.get();
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            Vec3 dir = Vec3.directionFromRotation(0, getYRot());
            wheelSpinO = wheelSpin;
            wheelSpin += (float) (((getX() - xo) * dir.x + (getZ() - zo) * dir.z) / WHEEL_RADIUS);
        }
        super.tick();
        if (!level().isClientSide() && isShareDown() && harness() != null && onGround() && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4)
            plough();
    }

    /** Server: tills the strip across the share; returns how many blocks turned. */
    int plough() {
        Vec3 dir = Vec3.directionFromRotation(0, getYRot()), right = Vec3.directionFromRotation(0, getYRot() + 90);
        Vec3 share = position().subtract(dir.scale(SHARE_BACK));
        int tilled = 0;
        // half-block steps: no gaps when it runs diagonally
        for (int k = -2 * HALF_WIDTH; k <= 2 * HALF_WIDTH; k++) {
            Vec3 p = share.add(right.scale(k * 0.5));
            if (till(BlockPos.containing(p.x, getY() - 0.5, p.z))) tilled++;
        }
        if (tilled > 0) {
            if (tickCount % 4 == 0) playSound(SoundEvents.HOE_TILL.value(), 0.7F, 0.8F);
            if (harness() != null && harness().getControllingPassenger() instanceof ServerPlayer rider) Kettle.award(rider, FURROW_ADVANCEMENT);
            if (harness() instanceof AbstractHorse h && h.getLeashHolder() instanceof ServerPlayer leader) Kettle.award(leader, FURROW_ADVANCEMENT);
        }
        return tilled;
    }

    private boolean till(BlockPos pos) {
        Level level = level();
        BlockState soil = level.getBlockState(pos);
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (!soil.is(BlockTags.TURNS_INTO_FARMLAND) && !soil.is(Blocks.PODZOL) && !soil.is(EarthBlocks.LOESS.get())) return false;
        level.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(Fertility.FERTILITY, PLOUGHED), Block.UPDATE_ALL);
        return true;
    }

    @Override
    public java.util.List<Hint> hints(Player player) {
        java.util.List<Hint> out = new java.util.ArrayList<>();
        out.add(new Hint("harness", harness() != null ? "share" : "hitch_horse"));
        if (harness() != null) out.add(new Hint("sneak_use", "unhitch"));
        out.add(new Hint("attack", "pick_up"));
        return out;
    }

    /** The grab key. Hitched: lowers or raises the share. Else hitches the tamed horse you ride or lead. */
    @Override
    protected void toggleHarness(Player player) {
        if (harness() != null) {
            setShareDown(!isShareDown());
            player.sendOverlayMessage(Component.translatable(isShareDown() ? "message.mechanical.plough.share_down" : "message.mechanical.plough.share_up"));
            playSound(isShareDown() ? SoundEvents.HOE_TILL.value() : SoundEvents.WOOD_PLACE, 1.0F, 0.8F);
            return;
        }
        AbstractHorse horse = player.getVehicle() instanceof AbstractHorse ridden && ridden.isTamed() ? ridden : ledHorse(player);
        if (horse == null || horse.isBaby()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.plough.needs_horse"));
            return;
        }
        if (pullsOther(horse)) {
            sayAlreadyPulling(player);
            return;
        }
        setHarness(horse);
        player.sendOverlayMessage(Component.translatable("message.mechanical.plough.hitched"));
        playSound(SoundEvents.LEAD_TIED, 1.0F, 0.8F);
    }

    private AbstractHorse ledHorse(Player player) {
        List<AbstractHorse> led = level().getEntitiesOfClass(AbstractHorse.class, player.getBoundingBox().inflate(HORSE_RANGE),
                h -> h.isTamed() && h.getLeashHolder() == player);
        return led.isEmpty() ? null : led.get(0);
    }

    /** Sneak + right-click on its horse (HorseWork): the share comes up too. */
    @Override
    public void release(AbstractHorse horse, Player player) {
        super.release(horse, player);
        if (harness() == null) setShareDown(false);
    }

    /** Sneak + right-click (on foot): unhitches, the share coming up. */
    @Override
    protected InteractionResult sneakUse(Player player) {
        if (harness() == null) return InteractionResult.PASS;
        if (!level().isClientSide()) {
            setHarness(null);
            setShareDown(false);
            player.sendOverlayMessage(Component.translatable("message.mechanical.plough.unhitched"));
            playSound(SoundEvents.LEAD_UNTIED, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setShareDown(input.getBooleanOr("share_down", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean("share_down", isShareDown());
    }
}
