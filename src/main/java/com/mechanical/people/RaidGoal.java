package com.mechanical.people;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * What a raider does at a raided camp (people.Raids): walk to it, then rob a chest or barrel of its food, put torches to
 * wooden things, or prowl round it. A raider with loot runs back the way it came; everyone runs at dawn or once half the
 * party is down. Out of sight far from the camp they are gone (with the loot). Fighting is the normal attack goals:
 * hunters and the leader go for players and the camp's animals, woodcutters (thieves and fire-setters) only for a player
 * who comes close.
 */
class RaidGoal extends Goal {
    static final int STORE_RANGE = 16, FIRE_RANGE = 12, THIEF_FIGHT_RANGE = 6;
    private static final int GONE_RANGE = 48, MAX_TICKS = 14000;

    /** A raider's part in a raid; saved with the entity. */
    static final class State {
        final UUID id;
        final BlockPos camp, from;
        final int size;
        final long start;
        boolean retreating;
        int fires;
        final List<ItemStack> loot = new ArrayList<>();

        State(UUID id, BlockPos camp, BlockPos from, int size, long start, int fires) {
            this.id = id;
            this.camp = camp;
            this.from = from;
            this.size = size;
            this.start = start;
            this.fires = fires;
        }

        boolean fleeing() {
            return retreating || !loot.isEmpty();
        }

        void save(ValueOutput out) {
            ValueOutput o = out.child("raid");
            o.store("id", UUIDUtil.CODEC, id);
            o.store("camp", BlockPos.CODEC, camp);
            o.store("from", BlockPos.CODEC, from);
            o.putInt("size", size);
            o.putLong("start", start);
            o.putBoolean("retreating", retreating);
            o.putInt("fires", fires);
            o.store("loot", ItemStack.CODEC.listOf(), loot);
        }

        static State load(ValueInput in) {
            return in.child("raid").flatMap(i -> i.read("id", UUIDUtil.CODEC).map(id -> {
                State s = new State(id, i.read("camp", BlockPos.CODEC).orElse(BlockPos.ZERO), i.read("from", BlockPos.CODEC).orElse(BlockPos.ZERO),
                        i.getIntOr("size", 1), i.getLongOr("start", 0), i.getIntOr("fires", 0));
                s.retreating = i.getBooleanOr("retreating", false);
                i.read("loot", ItemStack.CODEC.listOf()).ifPresent(s.loot::addAll);
                return s;
            })).orElse(null);
        }
    }

    private enum Task { WALK, STEAL, BURN }

    private final Tribesperson mob;
    private BlockPos taskPos;
    private Task task;
    private int taskTicks;

    RaidGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.raid != null && mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true; // tick() checks tickCount % 10, which every-other-tick goals miss on odd phases
    }

    @Override
    public void stop() {
        task = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        State r = mob.raid;
        ServerLevel level = (ServerLevel) mob.level();
        if (r.fleeing()) {
            // run out past where they came from
            if (mob.tickCount % 40 == 0 || mob.getNavigation().isDone()) {
                Vec3 camp = Vec3.atCenterOf(r.camp), away = Vec3.atCenterOf(r.from).subtract(camp).normalize().scale(80).add(camp);
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.25);
            }
            return;
        }
        if (task != null) {
            Vec3 at = Vec3.atCenterOf(taskPos);
            if (mob.distanceToSqr(at) < (task == Task.WALK ? 4 * 4 : 2.8 * 2.8)) {
                mob.getLookControl().setLookAt(at);
                if (task == Task.STEAL) steal(level, r, taskPos);
                if (task == Task.BURN) burn(level, r, taskPos);
                task = null;
            } else if (++taskTicks > 400 || mob.getNavigation().isDone() && !mob.getNavigation().moveTo(at.x, at.y, at.z, 1.0)) {
                task = null; // can't get there
            }
            return;
        }
        if (mob.tickCount % 10 != 0) return;
        RandomSource random = mob.getRandom();
        if (mob.distanceToSqr(Vec3.atCenterOf(r.camp)) > 12 * 12) {
            start(Task.WALK, r.camp, 1.1);
            return;
        }
        BlockPos p;
        if (random.nextFloat() < 0.6F && (p = foodStore(level, r.camp, mob.blockPosition())) != null) start(Task.STEAL, p, 1.1);
        else if (r.fires > 0 && random.nextFloat() < 0.5F && level.getGameRules().get(GameRules.MOB_GRIEFING) && (p = flammable(level, r.camp, random)) != null) start(Task.BURN, p, 1.0);
        else start(Task.WALK, r.camp.offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8), 0.8);
    }

    private void start(Task task, BlockPos pos, double speed) {
        this.task = task;
        taskPos = pos;
        taskTicks = 0;
        if (!mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed)) this.task = null;
    }

    // ---- stealing ----

    /** The chest, barrel or other container with food nearest to {@code near}, within STORE_RANGE of the camp. */
    static BlockPos foodStore(ServerLevel level, BlockPos camp, BlockPos near) {
        BlockPos best = null;
        int r = STORE_RANGE;
        for (int cx = (camp.getX() - r) >> 4; cx <= (camp.getX() + r) >> 4; cx++) {
            for (int cz = (camp.getZ() - r) >> 4; cz <= (camp.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (!(be instanceof Container c) || Math.abs(p.getY() - camp.getY()) > 8 || p.distSqr(camp) > r * r || !hasFood(c)) continue;
                    if (best == null || p.distSqr(near) < best.distSqr(near)) best = p;
                }
            }
        }
        return best;
    }

    private static boolean hasFood(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).has(DataComponents.FOOD)) return true;
        return false;
    }

    /** Takes up to two stacks of food: the raider now runs for home with it. */
    private void steal(ServerLevel level, State r, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Container c)) return;
        for (int i = 0; i < c.getContainerSize() && r.loot.size() < 2; i++) {
            if (c.getItem(i).has(DataComponents.FOOD)) r.loot.add(c.removeItemNoUpdate(i));
        }
        if (r.loot.isEmpty()) return;
        c.setChanged();
        mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.8F, 0.9F);
        Raids.tell(level, r.camp, Component.translatable("message.mechanical.raid.stolen", mob.tribeName()));
    }

    // ---- fire ----

    /** A wooden (flammable, solid, not leaves) block near the camp with room for fire beside it, at random; null if none. */
    static BlockPos flammable(ServerLevel level, BlockPos camp, RandomSource random) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(camp.offset(-FIRE_RANGE, -3, -FIRE_RANGE), camp.offset(FIRE_RANGE, 6, FIRE_RANGE))) {
            if (canBurn(level, p) && fireSpot(level, p) != null) found.add(p.immutable());
        }
        return found.isEmpty() ? null : found.get(random.nextInt(found.size()));
    }

    private static boolean canBurn(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && !s.canBeReplaced() && !s.is(BlockTags.LEAVES) && s.isFlammable(level, p, Direction.UP);
    }

    private static BlockPos fireSpot(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockPos q = p.relative(d);
            if (BaseFireBlock.canBePlacedAt(level, q, d)) return q;
        }
        return null;
    }

    private void burn(ServerLevel level, State r, BlockPos pos) {
        BlockPos q;
        if (r.fires <= 0 || !level.getGameRules().get(GameRules.MOB_GRIEFING) || !canBurn(level, pos) || (q = fireSpot(level, pos)) == null) return;
        level.setBlock(q, BaseFireBlock.getState(level, q), net.minecraft.world.level.block.Block.UPDATE_ALL);
        r.fires--;
        mob.swing(InteractionHand.OFF_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        level.playSound(null, q, SoundEvents.FLINTANDSTEEL_USE, SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    // ---- every second, from Tribesperson ----

    /** Retreat at dawn, after half the party fell, or when a thief has its loot; gone once out of sight; palisade stakes hurt. */
    static void update(Tribesperson mob, ServerLevel level) {
        State r = mob.raid;
        long now = level.getGameTime();
        if (!r.retreating) {
            boolean dawn = !level.isDarkOutside() && now - r.start > 600, tooLong = now - r.start > MAX_TICKS;
            List<Tribesperson> party = level.getEntitiesOfClass(Tribesperson.class, mob.getBoundingBox().inflate(96),
                    t -> t.isAlive() && t.raid != null && t.raid.id.equals(r.id));
            boolean beaten = party.size() * 2 <= r.size;
            if (dawn || beaten || tooLong) {
                for (Tribesperson t : party) t.raid.retreating = true;
                r.retreating = true;
                if (beaten) Raids.defended(level, r, mob.tribe());
            }
        }
        if (r.fleeing()) {
            mob.setTarget(null);
            double campDist = mob.position().distanceTo(Vec3.atCenterOf(r.camp));
            if (campDist > GONE_RANGE && level.getNearestPlayer(mob, 24) == null || campDist > 2 * GONE_RANGE) mob.discard();
            return;
        }
        // pushing against a palisade of sharpened stakes hurts and slows
        if (mob.horizontalCollision) {
            for (BlockPos p : BlockPos.betweenClosed(mob.blockPosition().offset(-1, 0, -1), mob.blockPosition().offset(1, 3, 1))) {
                if (level.getBlockState(p).getBlock() instanceof SharpenedLogBlock) {
                    mob.hurtServer(level, level.damageSources().cactus(), 2.0F);
                    mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                    break;
                }
            }
        }
    }

    /** Hunters and the leader fight any player; thieves only one who comes close. Nobody fights while running. */
    static boolean fights(Tribesperson mob, LivingEntity target) {
        State r = mob.raid;
        if (r.fleeing()) return false;
        return mob.role() != TribeRole.WOODCUTTER || mob.isLeader() || mob.distanceToSqr(target) < THIEF_FIGHT_RANGE * THIEF_FIGHT_RANGE;
    }

    /** Tame or farm animals at the camp (not wild game): the hunters kill them. */
    static boolean campAnimal(Tribesperson mob, LivingEntity animal) {
        State r = mob.raid;
        return !r.fleeing() && mob.role() == TribeRole.HUNTER && !(animal instanceof com.mechanical.wildlife.WildAnimal)
                && animal.distanceToSqr(Vec3.atCenterOf(r.camp)) < 24 * 24;
    }
}
