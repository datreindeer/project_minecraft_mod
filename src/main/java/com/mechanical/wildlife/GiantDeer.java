package com.mechanical.wildlife;

import net.minecraft.core.particles.ParticleTypes;
import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Giant deer (Megaloceros): rare, solitary mini-boss with huge palmate antlers and very keen eyes. Notices players far off
 * ({@link #SIGHT}), and any hit (an arrow from any range too) turns it on the attacker at once. Fights with telegraphed
 * straight-line charges ({@link HuntGoal}), rams down the tree a player climbs, hides from a player it can't reach, and
 * doesn't bleed or back off until it is down to a quarter of its health; then it bleeds and runs for cover (its own goal,
 * not AnimalFear). Against a crowd it swings its antlers in a wide arc in front ({@link #sweep}) and rears up to stamp on
 * whoever is behind it ({@link #stomp}). Always yields a giant antler.
 */
public class GiantDeer extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/giant_deer.png");
    private static final float BREAK_POINT = 0.25F;
    /** Ticks left of the antler sweep / the rear-up stomp (synced for the model). */
    private static final EntityDataAccessor<Integer> SWEEP = SynchedEntityData.defineId(GiantDeer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STOMP = SynchedEntityData.defineId(GiantDeer.class, EntityDataSerializers.INT);
    /** Sweep: head lowers and cocks aside for {@code SWEEP_HIT} ticks (the tell), then the swing lands; recovers to the end. */
    static final int SWEEP_TICKS = 20, SWEEP_HIT = 14, STOMP_TICKS = 16, STOMP_HIT = 10, AREA_COOLDOWN = 60;
    /** Foes this close to its hide set off an area attack; the sweep's reach (180 degrees in front) and the stomp's, from its centre. */
    static final double CROWD = 3.5, SWEEP_REACH = 4.5, STOMP_REACH = 3.6;
    static final float SWEEP_DAMAGE = 9.0F, STOMP_DAMAGE = 5.0F;
    private int areaCooldown;
    private HuntGoal hunt;
    /** Who it turns toward while winding up the sweep. */
    private net.minecraft.world.entity.LivingEntity sweepFacing;
    private final ServerBossEvent bossEvent = new ServerBossEvent(net.minecraft.util.Mth.createInsecureUUID(random), getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    public GiantDeer(EntityType<? extends GiantDeer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 120.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    /** Down to a quarter of its health: it gives up and runs. */
    public boolean isBroken() {
        return getHealth() <= getMaxHealth() * BREAK_POINT;
    }

    @Override
    public float bleedThreshold() {
        return BREAK_POINT;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FollowHerdGoal); // always alone
        goalSelector.addGoal(1, hunt = new HuntGoal(this));
        targetSelector.addGoal(2, new AttackNearbyPlayerGoal(this));
    }

    /** Who last hurt it: what it runs from once broken. */
    private net.minecraft.world.entity.LivingEntity lastAttacker;

    /** Any hit from a player (or another mob) turns it on the attacker at once, however far the shot came from. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && attacker != this && fair(attacker)) {
            lastAttacker = attacker;
            if (!isBroken()) {
                wake();
                if (getTarget() != attacker) bellow();
                setTarget(attacker);
            }
        }
        return hurt;
    }

    private static boolean fair(net.minecraft.world.entity.LivingEntity e) {
        return e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** A deep horn call across the forest when it turns on you. */
    private void bellow() {
        playSound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(6).value(), 5.0F, 0.6F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWEEP, 0);
        builder.define(STOMP, 0);
    }

    /** 0..1 progress of the antler sweep, for the model. */
    public float sweepAmount(float partialTick) {
        int t = entityData.get(SWEEP);
        return t <= 0 ? 0 : net.minecraft.util.Mth.clamp((SWEEP_TICKS - t + partialTick) / SWEEP_TICKS, 0, 1);
    }

    /** 0..1 progress of the rear-up stomp, for the model. */
    public float stompAmount(float partialTick) {
        int t = entityData.get(STOMP);
        return t <= 0 ? 0 : net.minecraft.util.Mth.clamp((STOMP_TICKS - t + partialTick) / STOMP_TICKS, 0, 1);
    }

    boolean areaAttacking() {
        return entityData.get(SWEEP) > 0 || entityData.get(STOMP) > 0;
    }

    /** Who it may hit: anyone fighting it (players, and mobs after it or that hurt it lately); never its kind, young, or items. */
    boolean isFoe(net.minecraft.world.entity.LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof GiantDeer || e.isBaby()) return false;
        if (e instanceof Player p) return fair(p);
        return e instanceof net.minecraft.world.entity.Mob m && (m.getTarget() == this || e == getTarget()
                || e == getLastHurtByMob() && tickCount - getLastHurtByMobTimestamp() < 100);
    }

    /** Foes whose near side is within {@code reach} of its centre, horizontally. */
    private java.util.List<net.minecraft.world.entity.LivingEntity> foes(double reach) {
        return level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, getBoundingBox().inflate(reach, 1.0, reach),
                e -> isFoe(e) && flatDistance(e) <= reach + e.getBbWidth() / 2);
    }

    private double flatDistance(Entity e) {
        return Math.sqrt(net.minecraft.util.Mth.square(e.getX() - getX()) + net.minecraft.util.Mth.square(e.getZ() - getZ()));
    }

    /** In the half-circle in front of where it faces. */
    boolean inFront(Entity e) {
        float yaw = getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        return (e.getX() - getX()) * -net.minecraft.util.Mth.sin(yaw) + (e.getZ() - getZ()) * net.minecraft.util.Mth.cos(yaw) >= 0;
    }

    /** Crowded (two or more foes at its hide, or its target at melee range): sweep the ones in front, or stomp the ones behind. */
    private void startAreaAttack() {
        var near = foes(CROWD + getBbWidth() / 2);
        var target = getTarget();
        if (near.size() < 2 && (target == null || !near.contains(target))) return;
        long behind = near.stream().filter(e -> !inFront(e) && flatDistance(e) <= STOMP_REACH + e.getBbWidth() / 2).count();
        areaCooldown = AREA_COOLDOWN;
        getNavigation().stop();
        if (behind >= 2) {
            entityData.set(STOMP, STOMP_TICKS);
            playSound(SoundEvents.HORSE_ANGRY, 2.0F, 0.4F);
        } else {
            sweepFacing = target != null && near.contains(target) ? target : near.get(0);
            entityData.set(SWEEP, SWEEP_TICKS);
            playSound(SoundEvents.GOAT_PREPARE_RAM, 2.0F, 0.4F);
            playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 1.1F);
        }
    }

    /** Runs the sweep and stomp timers, and starts one when crowded (not mid-charge, not once broken). */
    private void tickAreaAttack(ServerLevel level) {
        int sweep = entityData.get(SWEEP), stomp = entityData.get(STOMP);
        if (sweep > 0) {
            getNavigation().stop();
            if (SWEEP_TICKS - sweep < SWEEP_HIT && sweepFacing != null && sweepFacing.isAlive()) {
                float yaw = (float) (net.minecraft.util.Mth.atan2(sweepFacing.getZ() - getZ(), sweepFacing.getX() - getX()) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
                setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), yaw, 25.0F));
                yBodyRot = yHeadRot = getYRot();
            }
            entityData.set(SWEEP, --sweep);
            if (SWEEP_TICKS - sweep == SWEEP_HIT) sweep(level);
        } else if (stomp > 0) {
            getNavigation().stop();
            entityData.set(STOMP, --stomp);
            if (STOMP_TICKS - stomp == STOMP_HIT) stomp(level);
        } else if (areaCooldown > 0) {
            areaCooldown--;
        } else if (!isBroken() && !isDozing() && tickCount % 5 == 0 && (hunt == null || hunt.phase != Phase.CHARGE)) {
            startAreaAttack();
        }
    }

    /** The antler swing: every foe in the half-circle in front, out to {@link #SWEEP_REACH}, is hit hard and thrown outward. */
    int sweep(ServerLevel level) {
        int n = 0;
        DamageSource source = damageSources().mobAttack(this);
        for (var e : foes(SWEEP_REACH)) {
            if (!inFront(e) || !e.hurtServer(level, source, SWEEP_DAMAGE)) continue;
            e.knockback(1.8, getX() - e.getX(), getZ() - e.getZ(), source, SWEEP_DAMAGE);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
            e.needsSync = true;
            n++;
        }
        float yaw = getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() - net.minecraft.util.Mth.sin(yaw) * 2.5, getY() + 1.5,
                getZ() + net.minecraft.util.Mth.cos(yaw) * 2.5, 3, 1.2, 0.3, 1.2, 0);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
        if (n > 0) playSound(SoundEvents.GOAT_RAM_IMPACT, 2.0F, 0.5F);
        return n;
    }

    /** Rearing up and slamming down its forehooves: everyone close all round (mostly the ones behind it) is hit and pushed off. */
    int stomp(ServerLevel level) {
        int n = 0;
        DamageSource source = damageSources().mobAttack(this);
        for (var e : foes(STOMP_REACH)) {
            if (!e.hurtServer(level, source, STOMP_DAMAGE)) continue;
            e.knockback(1.2, getX() - e.getX(), getZ() - e.getZ(), source, STOMP_DAMAGE);
            e.needsSync = true;
            n++;
        }
        HuntGoal.stomp(this);
        playSound(SoundEvents.RAVAGER_STUNNED, 1.5F, 0.6F);
        return n;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) tickAreaAttack(server);
        if (level().isClientSide()) {
            magicMotes();
            return;
        }
        flowerTrail();
        // charging through the forest: any trunk in the way comes down
        if (isAggressive() && !isBroken() && horizontalCollision && tickCount % 4 == 0 && level() instanceof ServerLevel server
                && net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, this)) ramTree(server);
    }

    /** Charging into a tree trunk: the whole tree comes down, falling the way the deer is running. */
    private void ramTree(ServerLevel level) {
        net.minecraft.world.phys.AABB ahead = getBoundingBox().move(getDeltaMovement().x, 0, getDeltaMovement().z).inflate(0.5, 0, 0.5).setMaxY(getY() + 3);
        for (BlockPos pos : BlockPos.betweenClosed(ahead)) {
            if (fellTree(level, pos, getDirection())) return;
        }
    }

    /** Fells the natural tree whose trunk is at {@code pos} (from its base); false if there is no such tree. */
    private boolean fellTree(ServerLevel level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!isTrunk(level, pos)) return false;
        BlockPos cut = base(level, pos);
        var tree = com.mechanical.TreeFelling.find(level, cut);
        if (tree == null) return false; // a log wall or a cabin, not a natural tree
        com.mechanical.TreeFelling.fell(level, cut, tree, direction, this, net.minecraft.world.item.ItemStack.EMPTY);
        playSound(SoundEvents.GOAT_RAM_IMPACT, 2.0F, 0.5F);
        level.playSound(null, cut, SoundEvents.WOOD_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
        return true;
    }

    // the Ancient Oak's enchanted wood is wither immune and must never fall
    private static boolean isTrunk(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(net.minecraft.tags.BlockTags.LOGS) && !state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE);
    }

    private static BlockPos base(Level level, BlockPos pos) {
        BlockPos cut = pos.immutable();
        while (level.getBlockState(cut.below()).is(net.minecraft.tags.BlockTags.LOGS)) cut = cut.below(); // fell from the base
        return cut;
    }

    /** The base of a natural tree the target is perched in (logs or leaves under its feet), or null. */
    BlockPos trunkUnder(Entity target) {
        BlockPos feet = BlockPos.containing(target.getX(), target.getY() - 0.2, target.getZ());
        boolean inTree = false;
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, 0, 1))) {
            BlockState s = level().getBlockState(p);
            if (s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(net.minecraft.tags.BlockTags.LOGS)) inTree = true;
        }
        if (!inTree) return null;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int bottom = Math.min(feet.getY(), blockPosition().getY()) - 1;
        for (int dx = -TRUNK_SEARCH; dx <= TRUNK_SEARCH; dx++) for (int dz = -TRUNK_SEARCH; dz <= TRUNK_SEARCH; dz++) {
            for (int y = feet.getY(); y >= bottom; y--) {
                BlockPos p = new BlockPos(feet.getX() + dx, y, feet.getZ() + dz);
                if (!isTrunk(level(), p)) continue;
                BlockPos cut = base(level(), p);
                double d = dx * dx + dz * dz;
                if (d < bestDist && com.mechanical.TreeFelling.find(level(), cut) != null) {
                    best = cut;
                    bestDist = d;
                }
                break;
            }
        }
        return best;
    }

    /** Somewhere 20-30 blocks away from {@code threat}, preferably where it can't be seen from there; null if nowhere found. */
    Vec3 hidingSpot(Vec3 threatEye, double minDist) {
        Vec3 best = null;
        double bestScore = -1;
        for (int i = 0; i < 12; i++) {
            Vec3 c = net.minecraft.world.entity.ai.util.LandRandomPos.getPosAway(this, 20, 30, 8, threatEye);
            if (c == null) continue;
            double dist = c.distanceTo(threatEye);
            boolean hidden = level().clip(new net.minecraft.world.level.ClipContext(threatEye, c.add(0, 2.5, 0),
                    net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType()
                    != net.minecraft.world.phys.HitResult.Type.MISS;
            double score = (hidden ? 100 : 0) + Math.min(dist, minDist + 20);
            if (score > bestScore) {
                best = c;
                bestScore = score;
            }
            if (hidden && dist >= minDist) break;
        }
        return best;
    }

    /** Can {@code from}'s eye see this deer's upper body? */
    boolean seenBy(Entity from) {
        return level().clip(new net.minecraft.world.level.ClipContext(from.getEyePosition(), position().add(0, 3, 0),
                net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType()
                == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    /** Woken by a player: it goes for them instead of bolting. */
    @Override
    protected void onWoken(Player near) {
        if (!isBroken() && fair(near)) setTarget(near);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) playSound(SoundEvents.GOAT_RAM_IMPACT, 1.2F, 0.6F);
        return hit;
    }

    /** Notices any player within long range and sight (a bit closer when sneaking) and turns on them. */
    static class AttackNearbyPlayerGoal extends TargetGoal {
        private final GiantDeer deer;

        AttackNearbyPlayerGoal(GiantDeer deer) {
            super(deer, false);
            this.deer = deer;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (deer.isBroken() || deer.isDozing() || deer.getTarget() != null || (deer.tickCount + deer.getId()) % 10 != 0) return false;
            Player p = deer.level().getNearestPlayer(deer, SIGHT);
            if (p == null || !fair(p) || deer.distanceTo(p) > (p.isShiftKeyDown() ? SIGHT_SNEAKING : SIGHT) || !deer.hasLineOfSight(p)) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            deer.setTarget(targetMob);
            deer.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
            deer.bellow();
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            return !deer.isBroken() && super.canContinueToUse();
        }
    }

    /** Notices players this far in sight (sneaking: the second); a target further than {@link #GIVE_UP} is forgotten. */
    static final double SIGHT = 48, SIGHT_SNEAKING = 32, GIVE_UP = 64;
    /** Charges from at most this far in a straight line; closer than this to its hiding spot an archer can still hit it. */
    static final double CHARGE_RANGE = 18, BOW_RANGE = 36;
    /** Charge: wind-up (head down, stomping) the player can see coming, run time limit, and the pause after (its weak moment). */
    static final int WINDUP = 24, WINDUP_CLOSE = 14, CHARGE_TICKS = 50, RECOVER = 30, TRUNK_SEARCH = 3;
    static final double APPROACH_SPEED = 1.2, CHARGE_SPEED = 1.5, HIDE_SPEED = 1.1, FLEE_SPEED = 1.4;
    /** Higher than this above the deer's feet and unreachable: a tree it rams down, anything else it hides from. */
    static final double PERCHED = 2.0;

    enum Phase { DECIDE, APPROACH, WINDUP, CHARGE, RECOVER, RETREAT, LURK, FLEE }

    /**
     * The fight, one state machine: closes in on the target, winds up (head down, snorting, stomping) and charges in a
     * straight line at where the target stood, so a quick sidestep dodges it; then a short pause. A target up a tree gets
     * the tree rammed down under it; one it can't reach otherwise (a pillar, a cliff) makes it back off out of bow range
     * and out of sight, lurking until the target comes down. Broken (a quarter health left), it runs from the attacker
     * toward cover instead.
     */
    static class HuntGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final GiantDeer deer;
        Phase phase = Phase.DECIDE;
        private int timer;
        /** Charge: the point it runs at (where the target stood, a bit beyond; or the trunk base it rams). */
        private Vec3 chargeAt;
        /** Base of the tree the target is up, while it goes for that tree. */
        BlockPos trunk;
        private boolean hit;
        /** Everyone the current charge has already run down. */
        private final java.util.Set<Entity> trampled = new java.util.HashSet<>();

        HuntGoal(GiantDeer deer) {
            this.deer = deer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private net.minecraft.world.entity.LivingEntity threat() {
            if (deer.isBroken()) {
                var a = deer.lastAttacker;
                if (a != null && fair(a) && a.distanceTo(deer) < SIGHT) return a;
                Player near = deer.level().getNearestPlayer(deer, 24);
                return near != null && fair(near) ? near : null;
            }
            var t = deer.getTarget();
            return t != null && fair(t) && t.distanceTo(deer) < GIVE_UP ? t : null;
        }

        @Override
        public boolean canUse() {
            return threat() != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (threat() != null) return true;
            if (!deer.isBroken() && deer.getTarget() != null) deer.setTarget(null); // lost or out of range: forget it
            return false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            phase = deer.isBroken() ? Phase.FLEE : Phase.DECIDE;
            timer = 0;
        }

        @Override
        public void stop() {
            deer.setAggressive(false);
            deer.getNavigation().stop();
            phase = Phase.DECIDE;
            trunk = null;
        }

        private void to(Phase next, int ticks) {
            phase = next;
            timer = ticks;
            deer.setAggressive(next == Phase.WINDUP || next == Phase.CHARGE);
        }

        @Override
        public void tick() {
            var target = threat();
            if (target == null || deer.areaAttacking()) return; // the sweep/stomp has the deer for now
            timer--;
            if (deer.isBroken() && phase != Phase.FLEE) {
                deer.setTarget(null);
                to(Phase.FLEE, 0);
            }
            switch (phase) {
                case DECIDE -> decide(target);
                case APPROACH -> {
                    deer.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    if (deer.distanceTo(target) <= CHARGE_RANGE && deer.hasLineOfSight(target) || timer <= 0) to(Phase.DECIDE, 0);
                    else if ((deer.tickCount + deer.getId()) % 10 == 0 && !deer.getNavigation().moveTo(target, APPROACH_SPEED))
                        deer.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), APPROACH_SPEED);
                }
                case WINDUP -> windup(target);
                case CHARGE -> charge(target);
                case RECOVER -> {
                    deer.getLookControl().setLookAt(target, 10.0F, 10.0F);
                    if (timer == RECOVER - 5) deer.playSound(SoundEvents.HORSE_BREATHE, 1.5F, 0.4F);
                    if (timer <= 0) to(Phase.DECIDE, 0);
                }
                case RETREAT -> {
                    if (deer.getNavigation().isDone() || timer <= 0) to(Phase.LURK, 0);
                }
                case LURK -> lurk(target);
                case FLEE -> flee(target);
            }
        }

        /** What to do about the target from here: charge it, close in, ram its tree, or back off. */
        private void decide(net.minecraft.world.entity.LivingEntity target) {
            Phase next = choose(target);
            if (next == Phase.RETREAT) {
                retreat(target);
            } else if (next == Phase.APPROACH) {
                if (trunk != null) {
                    Vec3 t = Vec3.atBottomCenterOf(trunk);
                    deer.getNavigation().moveTo(t.x, t.y, t.z, APPROACH_SPEED);
                } else {
                    deer.getNavigation().moveTo(target, APPROACH_SPEED);
                }
                to(Phase.APPROACH, 100);
            } else if (next == Phase.WINDUP) {
                deer.getNavigation().stop();
                deer.playSound(SoundEvents.GOAT_PREPARE_RAM, 2.0F, 0.5F);
                to(Phase.WINDUP, trunk == null && deer.distanceTo(target) < 6 ? WINDUP_CLOSE : WINDUP);
            }
            // DECIDE: in the air, try again next tick
        }

        /** The plan, without side effects except finding {@link #trunk}; DECIDE when it can't tell yet (in the air). */
        Phase choose(net.minecraft.world.entity.LivingEntity target) {
            trunk = null;
            if (!deer.onGround() && !deer.isInWater()) return Phase.DECIDE;
            if (target.getY() - deer.getY() > PERCHED) {
                trunk = deer.trunkUnder(target);
                if (trunk != null) return Math.sqrt(deer.distanceToSqr(Vec3.atBottomCenterOf(trunk))) <= CHARGE_RANGE ? Phase.WINDUP : Phase.APPROACH;
                var path = deer.getNavigation().createPath(target, 1);
                return path == null || !path.canReach() ? Phase.RETREAT : Phase.APPROACH;
            }
            return deer.distanceTo(target) <= CHARGE_RANGE && deer.hasLineOfSight(target) ? Phase.WINDUP : Phase.APPROACH;
        }

        private Vec3 aimPoint(net.minecraft.world.entity.LivingEntity target) {
            return trunk != null ? Vec3.atBottomCenterOf(trunk) : target.position();
        }

        private void windup(net.minecraft.world.entity.LivingEntity target) {
            Vec3 aim = aimPoint(target);
            deer.getNavigation().stop();
            deer.getLookControl().setLookAt(aim.x, aim.y + 1, aim.z, 30.0F, 30.0F);
            float yaw = (float) (net.minecraft.util.Mth.atan2(aim.z - deer.getZ(), aim.x - deer.getX()) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
            deer.setYRot(net.minecraft.util.Mth.approachDegrees(deer.getYRot(), yaw, 20.0F));
            deer.yBodyRot = deer.getYRot();
            if (timer % 8 == 0) stomp(deer);
            if (timer > 0) return;
            // locked on where the target stands now, and on past it: no turning mid-charge
            Vec3 flat = new Vec3(aim.x - deer.getX(), 0, aim.z - deer.getZ());
            chargeAt = trunk != null || flat.lengthSqr() < 1.0E-4 ? aim : aim.add(flat.normalize().scale(3.0));
            hit = false;
            trampled.clear();
            deer.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.8F);
            to(Phase.CHARGE, CHARGE_TICKS);
        }

        static void stomp(GiantDeer deer) {
            deer.playSound(SoundEvents.RAVAGER_STEP, 1.5F, 0.5F);
            if (deer.level() instanceof ServerLevel server) {
                BlockState ground = server.getBlockState(deer.blockPosition().below());
                if (!ground.isAir()) server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, ground),
                        deer.getX(), deer.getY() + 0.1, deer.getZ(), 20, 1.0, 0.1, 1.0, 0.15);
            }
        }

        private void charge(net.minecraft.world.entity.LivingEntity target) {
            deer.getMoveControl().setWantedPosition(chargeAt.x, chargeAt.y, chargeAt.z, CHARGE_SPEED);
            ServerLevel level = (ServerLevel) deer.level();
            double toPoint = deer.position().subtract(chargeAt).horizontalDistance();
            if (trunk != null) {
                // ramming the tree the target is up: down it comes when the antlers reach the trunk
                if (toPoint <= deer.getBbWidth() / 2 + 1.3 || deer.horizontalCollision) {
                    var dir = net.minecraft.core.Direction.getApproximateNearest(chargeAt.x - deer.getX(), 0, chargeAt.z - deer.getZ());
                    if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, deer)) deer.fellTree(level, trunk, dir);
                    trunk = null;
                    to(Phase.RECOVER, RECOVER);
                    return;
                }
            } else {
                // runs down everyone in its path, not just the target; reaching the target ends the charge
                for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, deer.getBoundingBox().inflate(0.6),
                        e -> e != target && deer.isFoe(e) && !trampled.contains(e))) {
                    trampled.add(e);
                    deer.doHurtTarget(level, e);
                }
                if (!hit && deer.getBoundingBox().inflate(0.6).intersects(target.getBoundingBox())) hit = deer.doHurtTarget(level, target);
            }
            if (timer <= 0 || hit || trunk == null && toPoint < 1.5) to(Phase.RECOVER, RECOVER);
        }

        /** Out of bow range and out of sight of a target it can't reach. */
        private void retreat(net.minecraft.world.entity.LivingEntity target) {
            Vec3 spot = deer.hidingSpot(target.getEyePosition(), BOW_RANGE);
            if (spot != null) deer.getNavigation().moveTo(spot.x, spot.y, spot.z, HIDE_SPEED);
            to(Phase.RETREAT, 200);
        }

        /** Hidden, watching: back to the fight once the target is reachable again; off again if it's seen within bow range. */
        private void lurk(net.minecraft.world.entity.LivingEntity target) {
            deer.getLookControl().setLookAt(target, 10.0F, 10.0F);
            if ((deer.tickCount + deer.getId()) % 20 != 0) return;
            Phase next = choose(target);
            if (next != Phase.RETREAT && next != Phase.DECIDE) to(Phase.DECIDE, 0);
            else if (deer.distanceTo(target) < BOW_RANGE && deer.seenBy(target)) retreat(target);
        }

        /** Broken: away from the attacker toward cover, again whenever it's seen close. */
        private void flee(net.minecraft.world.entity.LivingEntity threat) {
            if (timer > 0 && !deer.getNavigation().isDone()) return;
            if (deer.distanceTo(threat) > BOW_RANGE && !deer.seenBy(threat)) return; // safe for now
            Vec3 spot = deer.hidingSpot(threat.getEyePosition(), BOW_RANGE);
            if (spot == null) spot = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(deer, 16, 7, threat.position());
            if (spot != null) deer.getNavigation().moveTo(spot.x, spot.y, spot.z, FLEE_SPEED);
            timer = 60;
        }
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public boolean showsHorns() {
        return true;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return isBroken() ? 2.0 : 0.0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.3F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    /** Drifting spores and glints around its antlers; a swirl of them while it charges. */
    private void magicMotes() {
        var r = getRandom();
        int n = isAggressive() ? 3 : (r.nextInt(3) == 0 ? 1 : 0);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.8 + r.nextDouble() * 2.2;
            double x = getX() + Math.cos(a) * d, y = getY() + getBbHeight() * (0.75 + r.nextDouble() * 0.35), z = getZ() + Math.sin(a) * d;
            level().addParticle(r.nextInt(5) == 0 ? ParticleTypes.END_ROD : ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z,
                    0, r.nextDouble() * 0.02, 0);
        }
        if (r.nextInt(12) == 0) level().addParticle(ParticleTypes.HAPPY_VILLAGER, getX() + (r.nextDouble() - 0.5) * 2.5,
                getY() + 0.2, getZ() + (r.nextDouble() - 0.5) * 2.5, 0, 0, 0);
    }

    private static final net.minecraft.world.level.block.Block[] TRAIL_FLOWERS = {
            net.minecraft.world.level.block.Blocks.OXEYE_DAISY, net.minecraft.world.level.block.Blocks.AZURE_BLUET,
            net.minecraft.world.level.block.Blocks.CORNFLOWER, net.minecraft.world.level.block.Blocks.LILY_OF_THE_VALLEY,
            net.minecraft.world.level.block.Blocks.FERN};

    /** Flowers spring up now and then where it walks. */
    private void flowerTrail() {
        if (!onGround() || getDeltaMovement().horizontalDistanceSqr() < 1.0E-3 || getRandom().nextInt(25) != 0) return;
        net.minecraft.core.BlockPos at = blockPosition().offset(getRandom().nextInt(3) - 1, 0, getRandom().nextInt(3) - 1);
        var flower = TRAIL_FLOWERS[getRandom().nextInt(TRAIL_FLOWERS.length)].defaultBlockState();
        if (level().getBlockState(at).isAir() && level().getBlockState(at.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                && flower.canSurvive(level(), at)) {
            level().setBlock(at, flower, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + getBbHeight() * 0.6, getZ(), 80, 1.2, 1.4, 1.2, 0.35);
            server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + getBbHeight() * 0.5, getZ(), 60, 2.0, 1.5, 2.0, 0.02);
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
        }
    }
}
