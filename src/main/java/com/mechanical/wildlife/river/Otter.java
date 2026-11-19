package com.mechanical.wildlife.river;

import java.util.EnumSet;
import java.util.List;

import com.mechanical.AnimalFear;
import com.mechanical.DryingRackBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Otter: long, sleek and dark brown with a pale throat, fast in the water. Lives along rivers, lakes and rocky coasts.
 * Shy of people (flees, dives), playful with its own kind (chases another otter now and then), and fishes: it dives,
 * comes up with a fish and eats it on the bank. A thief, but a fair one: when nobody is within {@link #UNWATCHED} blocks
 * it may take a fish lying on the ground, or one drying on a Drying Rack, at most once a day.
 */
public class Otter extends SemiAquatic {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/otter.png");
    private static final EntityDataAccessor<Boolean> HOLDING_FISH = SynchedEntityData.defineId(Otter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EATING = SynchedEntityData.defineId(Otter.class, EntityDataSerializers.BOOLEAN);
    static final double UNWATCHED = 12;
    static final int STEAL_COOLDOWN = 24000;
    private long nextSteal;

    public Otter(EntityType<? extends Otter> type, Level level) {
        super(type, level, 0.2F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof WaterAvoidingRandomStrollGoal);
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.9));
        goalSelector.addGoal(4, new EatFishGoal(this));
        goalSelector.addGoal(5, new FishGoal(this));
        goalSelector.addGoal(5, new StealGoal(this));
        goalSelector.addGoal(6, new PlayGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOLDING_FISH, false);
        builder.define(EATING, false);
    }

    /** A fish in its mouth (model). */
    public boolean isHoldingFish() {
        return entityData.get(HOLDING_FISH);
    }

    /** Sitting up, eating its catch (model). */
    public boolean isEating() {
        return entityData.get(EATING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!hasHome() && !isBaby() && level() instanceof ServerLevel level && (tickCount + getId()) % 200 == 0) {
            BlockPos water = BeaverWorks.findWater(level, blockPosition(), 6);
            if (water != null) setHomeTo(water, 28);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("NextSteal", nextSteal);
        output.putBoolean("HoldingFish", isHoldingFish());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nextSteal = input.getLongOr("NextSteal", 0);
        entityData.set(HOLDING_FISH, input.getBooleanOr("HoldingFish", false));
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.4F;
    }

    @Override
    public double fearMultiplier() {
        return 1.1;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isInWater() ? null : SoundEvents.FOX_SNIFF;
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
        return super.getVoicePitch() * 1.3F;
    }

    private static boolean watched(Otter otter) {
        Player p = otter.level().getNearestPlayer(otter, UNWATCHED);
        return p != null && !p.isSpectator();
    }

    /** With a fish in its mouth: to the bank, sit up and eat it (heals). */
    static class EatFishGoal extends Goal {
        private final Otter otter;
        private int eating;

        EatFishGoal(Otter otter) {
            this.otter = otter;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return otter.isHoldingFish() && !AnimalFear.isPanicking(otter);
        }

        @Override
        public void start() {
            eating = 0;
            if (otter.isInWater()) {
                Vec3 bank = LandRandomPos.getPos(otter, 10, 5);
                if (bank != null) otter.getNavigation().moveTo(bank.x, bank.y, bank.z, 1.2);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return otter.isHoldingFish() && !AnimalFear.isPanicking(otter);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (otter.isInWater() && !otter.getNavigation().isDone()) return;
            otter.getNavigation().stop();
            otter.entityData.set(EATING, true);
            if (++eating % 12 == 0 && otter.level() instanceof ServerLevel level) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.COD), otter.getX(), otter.getY() + 0.4,
                        otter.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
                otter.playSound(SoundEvents.FOX_EAT, 0.8F, 1.3F);
            }
            if (eating >= 80) {
                otter.entityData.set(HOLDING_FISH, false);
                otter.heal(2.0F);
            }
        }

        @Override
        public void stop() {
            otter.entityData.set(EATING, false);
        }
    }

    /** In the water now and then: a dive, and up it comes with a fish (more often in rivers). */
    static class FishGoal extends Goal {
        private final Otter otter;
        private int ticks;

        FishGoal(Otter otter) {
            this.otter = otter;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return otter.isInWater() && !otter.isHoldingFish() && !otter.isBaby() && !AnimalFear.isPanicking(otter)
                    && otter.getRandom().nextInt(400) == 0;
        }

        @Override
        public void start() {
            ticks = 0;
            // down towards the bottom a few blocks off
            BlockPos at = otter.blockPosition().offset(otter.getRandom().nextInt(9) - 4, -3, otter.getRandom().nextInt(9) - 4);
            for (int i = 0; i < 3 && !otter.level().getFluidState(at).is(FluidTags.WATER); i++) at = at.above();
            otter.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 1.3);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 100 && otter.isInWater();
        }

        @Override
        public void tick() {
            ticks += 2;
        }

        @Override
        public void stop() {
            if (ticks >= 60 && otter.isInWater() && otter.getRandom().nextInt(3) != 0) {
                otter.entityData.set(HOLDING_FISH, true);
                otter.playSound(SoundEvents.FISH_SWIM, 0.6F, 1.2F);
            }
        }
    }

    /**
     * Unwatched (no player within {@link #UNWATCHED}), at most once a day: takes a raw fish lying on the ground nearby,
     * or a fish drying on a Drying Rack (fresh or dried), and runs off with it.
     */
    static class StealGoal extends Goal {
        private final Otter otter;
        private ItemEntity item;
        private BlockPos rack;
        private IntegerProperty spot;
        private int ticks;

        StealGoal(Otter otter) {
            this.otter = otter;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if ((otter.tickCount + otter.getId()) % 100 != 0 || otter.isHoldingFish() || otter.isBaby()
                    || otter.level().getGameTime() < otter.nextSteal || watched(otter) || AnimalFear.isPanicking(otter)) return false;
            item = null;
            rack = null;
            List<ItemEntity> fish = otter.level().getEntitiesOfClass(ItemEntity.class, otter.getBoundingBox().inflate(10),
                    e -> e.getItem().is(ItemTags.FISHES));
            if (!fish.isEmpty()) {
                item = fish.getFirst();
                return true;
            }
            return findRack();
        }

        /** A drying rack with fish on it within 8 blocks (a bounded scan, once every 5 s at most). */
        private boolean findRack() {
            BlockPos c = otter.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -2, -8), c.offset(8, 3, 8))) {
                BlockState s = otter.level().getBlockState(p);
                if (!(s.getBlock() instanceof DryingRackBlock)) continue;
                for (IntegerProperty prop : List.of(DryingRackBlock.HIDE, DryingRackBlock.HIDE2)) {
                    int v = s.getValue(prop);
                    if (v == DryingRackBlock.FISH || v == DryingRackBlock.FISH + 1) {
                        rack = p.immutable();
                        spot = prop;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void start() {
            ticks = 0;
            Vec3 to = item != null ? item.position() : Vec3.atBottomCenterOf(rack);
            otter.getNavigation().moveTo(to.x, to.y, to.z, 1.2);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 300 && !otter.isHoldingFish() && !watched(otter) && (item == null || item.isAlive());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            Vec3 to = item != null ? item.position() : Vec3.atCenterOf(rack);
            if (otter.position().distanceToSqr(to) > 2.5 * 2.5) {
                if (otter.getNavigation().isDone() && ticks % 20 == 0) otter.getNavigation().moveTo(to.x, to.y, to.z, 1.2);
                return;
            }
            boolean took = false;
            if (item != null) {
                item.getItem().shrink(1);
                if (item.getItem().isEmpty()) item.discard();
                took = true;
            } else {
                BlockState s = otter.level().getBlockState(rack);
                if (s.getBlock() instanceof DryingRackBlock && (s.getValue(spot) == DryingRackBlock.FISH || s.getValue(spot) == DryingRackBlock.FISH + 1)) {
                    otter.level().setBlock(rack, s.setValue(spot, DryingRackBlock.EMPTY), Block.UPDATE_ALL);
                    took = true;
                }
            }
            if (took) {
                otter.entityData.set(HOLDING_FISH, true);
                otter.nextSteal = otter.level().getGameTime() + STEAL_COOLDOWN;
                otter.level().playSound(null, otter.blockPosition(), SoundEvents.FOX_SNIFF, SoundSource.NEUTRAL, 1.0F, 1.4F);
            }
            ticks = 1000; // done either way
        }

        @Override
        public void stop() {
            otter.getNavigation().stop();
        }
    }

    /** Two otters near each other now and then romp: one chases the other for a few seconds. */
    static class PlayGoal extends Goal {
        private final Otter otter;
        private Otter other;
        private int ticks;

        PlayGoal(Otter otter) {
            this.otter = otter;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (otter.isHoldingFish() || otter.getRandom().nextInt(300) != 0 || AnimalFear.isPanicking(otter)) return false;
            other = otter.level().getEntitiesOfClass(Otter.class, otter.getBoundingBox().inflate(10), o -> o != otter && !o.isHoldingFish())
                    .stream().findFirst().orElse(null);
            return other != null;
        }

        @Override
        public void start() {
            ticks = 0;
            otter.playSound(SoundEvents.FOX_AMBIENT, 0.6F, 1.6F);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < 120 && other.isAlive() && !AnimalFear.isPanicking(otter);
        }

        @Override
        public void tick() {
            ticks += 2;
            otter.getLookControl().setLookAt(other);
            if (ticks % 10 == 0) otter.getNavigation().moveTo(other, 1.4);
            // the one being chased dashes off
            if (otter.distanceToSqr(other) < 4 && other.getNavigation().isDone()) {
                Vec3 away = other.position().add(other.position().subtract(otter.position()).normalize().scale(5));
                other.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
            }
        }

        @Override
        public void stop() {
            otter.getNavigation().stop();
        }
    }
}
