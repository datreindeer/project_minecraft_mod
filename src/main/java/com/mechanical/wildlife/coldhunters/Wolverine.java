package com.mechanical.wildlife.coldhunters;

import java.util.EnumSet;

import com.mechanical.Blood;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;
import com.mechanical.SnareBlock;
import com.mechanical.Traps;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Wolverine: a squat, heavy-pawed glutton of the snowy taiga and tundra. Always alone. Shy of people at a distance, but it
 * does not back off: come within {@link #TOO_CLOSE} blocks (not sneaking) and it goes for you, and anyone who comes near
 * while it is eating gets the same. A thief, by day or night and not put off by fire: it smells out an unguarded
 * carcass (on the ground, by a snare or hung on a Hanging Rack) within {@link #SCENT} blocks and eats it down to the bones,
 * and it raids baited snares, eating the bait and springing them.
 */
public class Wolverine extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/wolverine.png");
    static final double TOO_CLOSE = 4, GUARD = 8, SCENT = 32, SNARE_SEARCH = 12;
    /** A carcass with a player this close is guarded: it waits for them to leave. */
    static final double PEOPLE_GUARD = 10;

    private boolean feeding;

    public Wolverine(EntityType<? extends Wolverine> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal")); // solitary
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new RaidSnareGoal());
        goalSelector.addGoal(2, new StealCarcassGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new StandGroundGoal());
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    /** Lets you closer than most game before it moves off (and then it may turn on you instead). */
    @Override
    public double fearMultiplier() {
        return 0.45;
    }

    @Override
    public float bleedThreshold() {
        return 0.35F;
    }

    /** Sleeps in the middle of the day in its den, hunts and scavenges dawn, dusk and night. */
    @Override
    protected boolean sleepTime() {
        long t = level().getOverworldClockTime() % 24000;
        return t > 4000 && t < 8000;
    }

    @Override
    protected void onWoken(Player near) {
        setTarget(near);
    }

    public boolean isFeeding() {
        return feeding;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isCharging() || feeding ? SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.ANGRY)
                .adultSounds().growlSound().value() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.55F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP.value(), 0.2F, 0.8F);
    }

    /** Anyone (not sneaking) right on top of it, or near while it eats, is attacked. Growls first. */
    class StandGroundGoal extends TargetGoal {
        StandGroundGoal() {
            super(Wolverine.this, true);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (isBaby() || isDozing() || getTarget() != null) return false;
            Player p = level().getNearestPlayer(Wolverine.this, feeding ? GUARD : TOO_CLOSE);
            if (p == null || p.isCreative() || p.isSpectator() || (!feeding && p.isCrouching()) || !hasLineOfSight(p)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            setTarget(targetMob);
            playSound(SoundEvents.POLAR_BEAR_WARNING, 1.0F, 1.5F);
            super.start();
        }
    }

    /** Raids a set, baited snare nearby: eats the bait and springs the noose (it is far too big to be caught). */
    class RaidSnareGoal extends Goal {
        private BlockPos snare;
        private int ticks;

        RaidSnareGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || isBaby() || random.nextInt(200) != 0) return false;
            BlockPos at = blockPosition();
            int r = (int) SNARE_SEARCH;
            if (!level().hasChunksAt(at.offset(-r, 0, -r), at.offset(r, 0, r))) return false;
            snare = null;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-r, -3, -r), at.offset(r, 3, r))) {
                BlockState s = level().getBlockState(p);
                if (s.is(Traps.SNARE.get()) && !s.getValue(SnareBlock.SPRUNG) && s.getValue(SnareBlock.BAITED)
                        && level().getNearestPlayer(p.getX(), p.getY(), p.getZ(), PEOPLE_GUARD, false) == null) {
                    snare = p.immutable();
                    break;
                }
            }
            return snare != null && getNavigation().moveTo(snare.getX() + 0.5, snare.getY(), snare.getZ() + 0.5, 1.0);
        }

        @Override
        public void start() {
            ticks = 400;
        }

        @Override
        public boolean canContinueToUse() {
            return --ticks > 0 && getTarget() == null && level().getBlockState(snare).is(Traps.SNARE.get())
                    && !level().getBlockState(snare).getValue(SnareBlock.SPRUNG);
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(Vec3.atCenterOf(snare));
            if (distanceToSqr(Vec3.atBottomCenterOf(snare)) > 2.2 * 2.2) {
                if ((tickCount + getId()) % 20 == 0) getNavigation().moveTo(snare.getX() + 0.5, snare.getY(), snare.getZ() + 0.5, 1.0);
                return;
            }
            BlockState s = level().getBlockState(snare);
            level().setBlock(snare, s.setValue(SnareBlock.SPRUNG, true).setValue(SnareBlock.BAITED, false), Block.UPDATE_ALL);
            playSound(SoundEvents.TRIPWIRE_CLICK_ON, 1.0F, 0.6F);
            playSound(SoundEvents.FOX_EAT, 1.0F, 0.7F);
            ticks = 0;
        }
    }

    /**
     * Goes to the nearest unguarded carcass within SCENT blocks (not one being carried or dragged) and eats it: 8 s of
     * tearing, then it is gone. A body on the ground leaves a Bone Pile, one on a rack drops a couple of Bones.
     */
    class StealCarcassGoal extends Goal {
        private static final int FEED_TICKS = 160;
        private CarcassEntity carcass;
        private int eaten;

        StealCarcassGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean unguarded(CarcassEntity c) {
            return c.isAlive() && c.haulerId() < 0 && !c.isPassenger() && c.animal() != getType()
                    && level().getNearestPlayer(c.getX(), c.getY(), c.getZ(), PEOPLE_GUARD, false) == null;
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isDozing() || isBaby() || random.nextInt(40) != 0) return false;
            carcass = null;
            double best = Double.MAX_VALUE;
            for (CarcassEntity c : level().getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(SCENT), this::unguarded)) {
                double d = distanceToSqr(c);
                if (d < best) {
                    best = d;
                    carcass = c;
                }
            }
            return carcass != null && getNavigation().moveTo(carcass, 1.1);
        }

        @Override
        public void start() {
            eaten = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && unguarded(carcass);
        }

        @Override
        public void stop() {
            feeding = false;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 c = carcass.getBoundingBox().getCenter();
            getLookControl().setLookAt(c);
            double reach = 1.3 + (getBbWidth() + carcass.getBbWidth()) / 2;
            // hung on a rack the body hangs above: it stands under it and rears up to tear at it
            double dy = carcass.hungAt() != null ? 0 : carcass.getY() - getY();
            double dx = c.x - getX(), dz = c.z - getZ();
            if (dx * dx + dz * dz + dy * dy > reach * reach) {
                feeding = false;
                if (tickCount % 20 == 0) getNavigation().moveTo(c.x, carcass.hungAt() != null ? carcass.hungAt().getY() : carcass.getY(), c.z, 1.1);
                return;
            }
            feeding = true;
            getNavigation().stop();
            ServerLevel level = (ServerLevel) level();
            if (++eaten % 10 == 0) {
                Blood.spray(level, c.x, c.y, c.z, 3);
                playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 0.7F);
            }
            if (eaten < FEED_TICKS) return;
            BlockPos pos = carcass.blockPosition();
            if (carcass.hungAt() != null) {
                spawnAtLocation(level, new ItemStack(Items.BONE, 1 + random.nextInt(2)));
            } else {
                Blood.splatter(level, pos, 1, 4);
                BlockState pile = Mechanical.BONE_PILE.get().defaultBlockState();
                if (level.getBlockState(pos).canBeReplaced() && pile.canSurvive(level, pos)) level.setBlock(pos, pile, Block.UPDATE_ALL);
            }
            carcass.discard();
            heal(6.0F);
        }
    }
}
