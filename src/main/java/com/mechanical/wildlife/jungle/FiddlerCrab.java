package com.mechanical.wildlife.jungle;

import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fiddler Crab: a small mud crab, the male waving one huge orange claw. It lives by its hole in the mangrove mud: when a
 * player comes within {@link #HIDE_RANGE} blocks (sneaking: 2) or it is hurt, it ducks into the hole ({@link #isHiding})
 * and stays down {@link #HIDE_TICKS} ticks, out of reach of blows. Catch it out on open ground. Too small for a carcass: it
 * drops Raw Crab Meat.
 */
public class FiddlerCrab extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/fiddler_crab.png");
    private static final EntityDataAccessor<Integer> HIDING = SynchedEntityData.defineId(FiddlerCrab.class, EntityDataSerializers.INT);
    static final double HIDE_RANGE = 5, HIDE_RANGE_SNEAKING = 2;
    static final int HIDE_TICKS = 200;

    public FiddlerCrab(EntityType<? extends FiddlerCrab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDING, 0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    /** Males wave the one huge claw (females have two small ones); from the UUID so it never changes. */
    @Override
    public boolean showsHorns() {
        return (getUUID().getLeastSignificantBits() & 1) == 0;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.0; // it hides instead of running
    }

    /** Down its hole (synced for the renderer: only the eye stalks show). */
    public boolean isHiding() {
        return entityData.get(HIDING) > 0;
    }

    /** A hole to duck into: standing on mud (or mangrove roots in mud). */
    public boolean onMud() {
        BlockState below = level().getBlockState(blockPosition().below());
        return below.is(Blocks.MUD) || below.is(Blocks.MUDDY_MANGROVE_ROOTS) || below.is(Blocks.MANGROVE_ROOTS) || below.is(Blocks.CLAY);
    }

    /** Ducks into its hole, if it has one under it. */
    public boolean hide() {
        if (!onMud()) return false;
        if (!isHiding()) playSound(SoundEvents.MUD_STEP, 0.6F, 1.5F);
        entityData.set(HIDING, HIDE_TICKS);
        getNavigation().stop();
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isHiding() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false; // out of reach in the mud
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) hide();
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        int h = entityData.get(HIDING);
        if (h > 0) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            Player p = level().getNearestPlayer(this, HIDE_RANGE);
            entityData.set(HIDING, p != null && !p.isSpectator() ? Math.max(h - 1, 40) : h - 1); // stays down while someone hovers near
            return;
        }
        if (isNoAi() || (tickCount + getId()) % 5 != 0) return;
        Player p = level().getNearestPlayer(this, HIDE_RANGE);
        if (p != null && !p.isSpectator() && !p.isCreative() && (!p.isShiftKeyDown() || distanceTo(p) < HIDE_RANGE_SNEAKING)) hide();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.1F, 2.0F);
    }
}
