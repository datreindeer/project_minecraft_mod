package com.mechanical.wildlife.desert;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.meadow.Adder;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Rattlesnake (diamondback): a heavy, grey-brown snake of the badlands with dark diamonds down its back and a banded tail
 * ending in a rattle. It never strikes without warning: when a player comes within {@link #RATTLE_RANGE} it rears into
 * an S and buzzes its rattle ({@link #isRattling}); only someone who stays within {@link #STRIKE_RANGE} after
 * {@link #WARN_TICKS} of rattling is bitten (sneaking doesn't calm it). Backing off is enough. Snakeskin Boots stop the
 * bite (Adder). Gives snakeskin, snake meat, a Venom Gland and its Rattlesnake Rattle.
 */
public class Rattlesnake extends Adder {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/rattlesnake.png");
    private static final EntityDataAccessor<Boolean> RATTLING = SynchedEntityData.defineId(Rattlesnake.class, EntityDataSerializers.BOOLEAN);
    public static final double RATTLE_RANGE = 5, STRIKE_RANGE = 2.2;
    public static final int WARN_TICKS = 30;
    private int rattleTicks;

    public Rattlesnake(EntityType<? extends Rattlesnake> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // no strike without the warning: the adder's step-on strike goes, the rattle decides (aiStep)
        targetSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("SteppedOnGoal"));
    }

    public boolean isRattling() {
        return entityData.get(RATTLING);
    }

    /** Ticks it has been rattling at the player now near it. */
    public int rattleTicks() {
        return rattleTicks;
    }

    @Override
    public boolean isCharging() {
        return isRattling() || super.isCharging(); // reared in an S
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RATTLING, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        Player p = getTarget() != null || AnimalFear.isPanicking(this) ? null : level().getNearestPlayer(this, RATTLE_RANGE);
        rattleAt(p != null && !p.isCreative() && !p.isSpectator() ? p : null);
    }

    /** Server, every tick: rattles at {@code p} (the nearest player within range, or null: stops), and strikes once warned. */
    public void rattleAt(Player p) {
        if (p == null) {
            rattleTicks = 0;
            if (isRattling()) entityData.set(RATTLING, false);
            return;
        }
        if (!isRattling()) {
            entityData.set(RATTLING, true);
            setBasking(false);
        }
        rattleTicks++;
        getNavigation().stop();
        getLookControl().setLookAt(p, 30.0F, 30.0F);
        if (rattleTicks % 3 == 0) level().playSound(null, getX(), getY(), getZ(), SoundEvents.SILVERFISH_STEP, SoundSource.HOSTILE, 0.9F, 1.9F + getRandom().nextFloat() * 0.1F);
        if (rattleTicks >= WARN_TICKS && distanceTo(p) <= STRIKE_RANGE) {
            entityData.set(RATTLING, false);
            rattleTicks = 0;
            setTarget(p);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // the rattle is its voice
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }
}
