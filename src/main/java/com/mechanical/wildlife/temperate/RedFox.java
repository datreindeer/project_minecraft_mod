package com.mechanical.wildlife.temperate;

import java.util.EnumSet;

import com.mechanical.BirdNestBlock;
import com.mechanical.Birds;
import com.mechanical.CarcassEntity;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * The red fox (vanilla's fox, now also in the plains and meadows) raids farms at night: it takes eggs from nests and off
 * the ground, and kills hens ({@link RaidGoal}); then it carries the egg, or drags the dead hen, home to its den
 * ({@link CarryHomeGoal}), leaving a trail of paw prints ({@link FoxTracksBlock}) that lead there. Its den is the
 * nearest Fox Den within {@link #DEN_RANGE} of where it was born, or one it digs there (mobGriefing). Butchered it gives
 * a Raw Fox Pelt (TemperateFauna).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class RedFox {
    static final int RAID_RANGE = 24, NEST_RANGE = 12, DEN_RANGE = 48, TRACK_EVERY = 2;
    /** On a carcass a fox has brought home: not dragged off again. */
    public static final String CACHED = "mechanical:fox_cache";
    private static final String HOME = "mechanical:fox_home";
    static final TagKey<Item> BIRD_EGGS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "bird_eggs"));

    private RedFox() {}

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Fox fox)) return;
        if (!fox.getPersistentData().contains(HOME)) fox.getPersistentData().putLong(HOME, fox.blockPosition().asLong());
        fox.goalSelector.addGoal(2, new CarryHomeGoal(fox));
        fox.goalSelector.addGoal(3, new RaidGoal(fox));
    }

    static boolean isEgg(ItemStack stack) {
        return stack.is(Items.EGG) || stack.is(BIRD_EGGS);
    }

    static BlockPos home(Fox fox) {
        return BlockPos.of(fox.getPersistentData().getLongOr(HOME, fox.blockPosition().asLong()));
    }

    /** The fox's den: the nearest Fox Den around its home, or one dug there (if mobGriefing allows), or null. */
    static BlockPos den(Fox fox) {
        ServerLevel level = (ServerLevel) fox.level();
        BlockPos home = home(fox), best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-DEN_RANGE / 3, -4, -DEN_RANGE / 3), home.offset(DEN_RANGE / 3, 4, DEN_RANGE / 3))) {
            if (level.getBlockState(p).is(TemperateFauna.FOX_DEN.get()) && p.distSqr(home) < bestDist) {
                bestDist = p.distSqr(home);
                best = p.immutable();
            }
        }
        if (best != null || !level.getGameRules().get(GameRules.MOB_GRIEFING)) return best;
        BlockState den = TemperateFauna.FOX_DEN.get().defaultBlockState();
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos p = home.above(dy);
            if ((level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) && den.canSurvive(level, p)) {
                level.setBlockAndUpdate(p, den);
                return p;
            }
        }
        return null;
    }

    /** A fresh hen carcass close to the fox, not yet carried home. */
    static CarcassEntity freshHen(Fox fox) {
        return fox.level().getEntitiesOfClass(CarcassEntity.class, fox.getBoundingBox().inflate(3),
                c -> c.animal() == EntityTypes.CHICKEN && c.stage() == 0 && c.haulerId() < 0 && c.hungAt() == null && !c.isPassenger()
                        && !c.entityTags().contains(CACHED)).stream().findFirst().orElse(null);
    }

    /** Presses a paw print into the ground at the fox's feet (only on open ground). */
    static void track(ServerLevel level, BlockPos at) {
        BlockState tracks = TemperateFauna.FOX_TRACKS.get().defaultBlockState();
        if (level.getBlockState(at).isAir() && tracks.canSurvive(level, at)) level.setBlockAndUpdate(at, tracks);
    }

    /**
     * At night: eggs first (an egg on the ground, or one from a nest), else a hen to kill (the fox's own attack goal does
     * the killing). With the egg in its mouth the raid is over and CarryHomeGoal takes it home.
     */
    static class RaidGoal extends Goal {
        private final Fox fox;
        private ItemEntity egg;
        private BlockPos nest;
        private int ticks;

        RaidGoal(Fox fox) {
            this.fox = fox;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!fox.level().isDarkOutside() || fox.isBaby() || fox.isSleeping() || fox.getTarget() != null
                    || !fox.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() || (fox.tickCount + fox.getId()) % 40 != 0) return false;
            egg = fox.level().getEntitiesOfClass(ItemEntity.class, fox.getBoundingBox().inflate(NEST_RANGE), e -> isEgg(e.getItem()))
                    .stream().findFirst().orElse(null);
            nest = egg == null ? eggNest() : null;
            if (egg != null || nest != null) return true;
            Chicken hen = fox.level().getEntitiesOfClass(Chicken.class, fox.getBoundingBox().inflate(RAID_RANGE), c -> c.isAlive())
                    .stream().findFirst().orElse(null);
            if (hen != null) fox.setTarget(hen);
            return false;
        }

        private BlockPos eggNest() {
            BlockPos c = fox.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-NEST_RANGE, -3, -NEST_RANGE), c.offset(NEST_RANGE, 3, NEST_RANGE))) {
                BlockState s = fox.level().getBlockState(p);
                if (s.is(Birds.BIRD_NEST.get()) && s.getValue(BirdNestBlock.EGGS) > 0 && s.getValue(BirdNestBlock.BIRD) != BirdNestBlock.Bird.NONE) return p.immutable();
            }
            return null;
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return ++ticks < 400 && fox.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                    && (egg != null ? egg.isAlive() : nest != null && fox.level().getBlockState(nest).is(Birds.BIRD_NEST.get())
                    && fox.level().getBlockState(nest).getValue(BirdNestBlock.EGGS) > 0);
        }

        @Override
        public void tick() {
            Vec3 at = egg != null ? egg.position() : Vec3.atBottomCenterOf(nest);
            fox.getLookControl().setLookAt(at.x, at.y, at.z);
            if (fox.distanceToSqr(at) > 1.6 * 1.6) {
                if (ticks % 20 == 1) fox.getNavigation().moveTo(at.x, at.y, at.z, 1.2);
                return;
            }
            ItemStack taken;
            if (egg != null) {
                taken = egg.getItem().split(1);
                if (egg.getItem().isEmpty()) egg.discard();
            } else {
                BlockState s = fox.level().getBlockState(nest);
                taken = new ItemStack(s.getValue(BirdNestBlock.BIRD).egg());
                fox.level().setBlockAndUpdate(nest, s.setValue(BirdNestBlock.EGGS, s.getValue(BirdNestBlock.EGGS) - 1));
            }
            fox.setItemSlot(EquipmentSlot.MAINHAND, taken);
            fox.playSound(SoundEvents.FOX_EAT, 0.6F, 1.3F);
        }
    }

    /** Carries an egg in its mouth, or drags a fresh dead hen, back to the den; paw prints mark the way. */
    static class CarryHomeGoal extends Goal {
        private final Fox fox;
        private CarcassEntity hen;
        private BlockPos den, lastTrack;
        private int ticks;

        CarryHomeGoal(Fox fox) {
            this.fox = fox;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (fox.isBaby() || fox.isSleeping() || (fox.tickCount + fox.getId()) % 10 != 0) return false;
            boolean egg = isEgg(fox.getItemBySlot(EquipmentSlot.MAINHAND));
            hen = egg ? null : freshHen(fox);
            if (!egg && hen == null) return false;
            den = den(fox);
            return den != null && fox.distanceToSqr(Vec3.atCenterOf(den)) > 4;
        }

        @Override
        public void start() {
            ticks = 0;
            lastTrack = fox.blockPosition();
            fox.getNavigation().moveTo(den.getX() + 0.5, den.getY() + 1, den.getZ() + 0.5, 1.1);
        }

        @Override
        public boolean canContinueToUse() {
            return ++ticks < 2400 && (hen != null ? hen.isAlive() && hen.haulerId() < 0 : isEgg(fox.getItemBySlot(EquipmentSlot.MAINHAND)));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ServerLevel level = (ServerLevel) fox.level();
            if (hen != null) {
                Vec3 mouth = fox.position().add(Vec3.directionFromRotation(0, fox.getYRot()).scale(0.6));
                Vec3 move = mouth.subtract(hen.position());
                if (level.noCollision(hen, hen.getBoundingBox().move(move))) hen.setPos(hen.position().add(move));
            }
            BlockPos here = fox.blockPosition();
            if (here.distManhattan(lastTrack) >= TRACK_EVERY) {
                track(level, here);
                lastTrack = here;
            }
            if (fox.distanceToSqr(Vec3.atCenterOf(den)) <= 4) {
                arrive(level);
                return;
            }
            if (ticks % 40 == 0 || fox.getNavigation().isDone()) fox.getNavigation().moveTo(den.getX() + 0.5, den.getY() + 1, den.getZ() + 0.5, 1.1);
        }

        private void arrive(ServerLevel level) {
            BlockPos drop = den.relative(Direction.Plane.HORIZONTAL.getRandomDirection(fox.getRandom()));
            if (hen != null) {
                hen.addTag(CACHED);
                hen = null;
            } else {
                ItemStack egg = fox.getItemBySlot(EquipmentSlot.MAINHAND).copy();
                fox.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                ItemEntity item = new ItemEntity(level, drop.getX() + 0.5, drop.getY() + 0.2, drop.getZ() + 0.5, egg);
                item.setUnlimitedLifetime(); // the cache stays until someone finds it
                level.addFreshEntity(item);
            }
            ticks = 99999; // done
        }
    }
}
