package com.mechanical.wildlife.warm;

import java.util.EnumSet;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Water Buffalo: huge, slate-grey wild cattle of jungle rivers, swamps and mangroves, in herds, with great swept-back
 * horns on bulls and cows alike. Not shy, but dangerous: every grown buffalo charges whoever hurts it or any of its herd
 * within {@link #HERD_RANGE} (a calf's cry brings them all). In the heat of the day it wallows, lying in shallow water or
 * mud for a while ({@link WallowGoal}).
 */
public class WaterBuffalo extends WildAnimal {
    private static final EntityDataAccessor<Boolean> WALLOWING = SynchedEntityData.defineId(WaterBuffalo.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/water_buffalo.png");
    private static final Identifier CALF_TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/water_buffalo_calf.png");
    private static final double HERD_RANGE = 16;

    public WaterBuffalo(EntityType<? extends WaterBuffalo> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 48.0).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WALLOWING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WallowGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
    }

    public boolean isWallowing() {
        return entityData.get(WALLOWING);
    }

    /** Hurting one turns the whole herd's grown animals on the attacker. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) entityData.set(WALLOWING, false);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof WaterBuffalo)
                && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            for (WaterBuffalo b : level.getEntitiesOfClass(WaterBuffalo.class, getBoundingBox().inflate(HERD_RANGE), b -> !b.isBaby() && b.getTarget() == null)) {
                b.setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF_TEXTURE : TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return !isBaby();
    }

    @Override
    public double fearMultiplier() {
        return 0.6;
    }

    @Override
    protected float babyChance() {
        return 0.3F;
    }

    private static CowSoundVariant sounds() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.MOODY);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return sounds().ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds().deathSound().value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(sounds().stepSound().value(), 0.25F, 0.7F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.0F : 0.65F);
    }

    /** Shallow water (one deep, ground under it) or mud: somewhere to wallow. */
    boolean wallowSpot(BlockPos pos) {
        BlockState at = level().getBlockState(pos), below = level().getBlockState(pos.below());
        boolean shallow = at.getFluidState().is(FluidTags.WATER) && !below.getFluidState().is(FluidTags.WATER)
                && level().getBlockState(pos.above()).isAir();
        return shallow || below.is(Blocks.MUD) && at.isAir();
    }

    /** By day: walks to shallow water or mud within 12 blocks and lies in it for 20-40 s. */
    class WallowGoal extends Goal {
        private BlockPos spot;
        private int ticks;

        WallowGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || getTarget() != null || !level().isBrightOutside() || random.nextInt(300) != 0 || AnimalFear.isPanicking(WaterBuffalo.this)) return false;
            BlockPos me = blockPosition();
            if (wallowSpot(me)) {
                spot = me;
                return true;
            }
            for (int i = 0; i < 24; i++) {
                BlockPos p = me.offset(random.nextInt(25) - 12, random.nextInt(5) - 2, random.nextInt(25) - 12);
                if (wallowSpot(p)) {
                    spot = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            ticks = 400 + random.nextInt(400);
            getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && getTarget() == null && !AnimalFear.isPanicking(WaterBuffalo.this);
        }

        @Override
        public void tick() {
            if (isWallowing()) {
                getNavigation().stop();
                return;
            }
            if (distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) < 2.5) {
                getNavigation().stop();
                entityData.set(WALLOWING, true);
                playSound(SoundEvents.GENERIC_SPLASH, 0.6F, 0.7F);
            } else if (getNavigation().isDone()) {
                ticks = 0;
            }
        }

        @Override
        public void stop() {
            entityData.set(WALLOWING, false);
        }
    }
}
