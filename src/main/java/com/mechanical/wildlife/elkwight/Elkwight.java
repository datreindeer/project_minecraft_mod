package com.mechanical.wildlife.elkwight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * The Elkwight (docs/elkwight-plan.md): a giant with an elk's head and a huge palmate antler crown (visual only; the
 * hitbox is the body, 1.2 x 3.6), a double-bladed rune axe in both hands. He waits asleep until a player comes within
 * {@link #WAKE_RANGE} blocks in sight, steps into his ruin (worldgen.ElkwightRuins#ruinAt), hurts him or calls {@link #wake()}; then he is fought with a
 * boss bar. Persistent: he never despawns and keeps his damage.
 * <ul>
 * <li><b>Scaling</b>: woken, his health ({@link #BASE_HEALTH}) grows by {@link #HEALTH_PER_PLAYER} and his damage by
 * {@link #DAMAGE_PER_PLAYER} for every player past the first within {@link #SCALE_RANGE} blocks ({@link #scaleFor}).</li>
 * <li><b>Melee</b> (each with a telegraph: a pose and a sound): Chop (axe raised overhead), Spin (axe drawn back and a
 * stomp, then everyone within {@link #SPIN_RADIUS} is hit), Antler Sweep (crown lowered, hoof scraping, then a charge
 * that hits {@link #SWEEP_WIDTH} wide; into a wall he staggers {@link #STAGGER_TICKS} ticks).</li>
 * <li><b>Ranged</b>: Axe Throw ({@link ThrownElkAxe}: it spins out and back, or sticks and he must walk to fetch it) and
 * the Ground Wave ({@link ElkwightTerrain#wave}: a line of cracks that throws players up and fells natural trees). A
 * player fleeing past {@link #FLEE_RANGE} blocks makes him charge and throw twice as often.</li>
 * <li><b>Never stuck</b>: with no path to his target he leaps (up to ~{@link #LEAP_MAX} blocks up or down), smashes
 * through natural blocks ({@link ElkwightTerrain#canBreak}), and otherwise keeps to the ranged attacks. No teleport.</li>
 * <li><b>Eats the dead</b>: a player dying within {@link #CORPSE_RANGE} blocks draws him there; he kneels and eats for
 * {@link #EAT_TICKS} ticks, healing a quarter of his max health over the meal ({@link #MEAL_SHARE}). He takes
 * {@link #EAT_DAMAGE_FACTOR}x damage while eating, and {@link #EAT_INTERRUPT} of his max health in damage ends the meal
 * early (only what was eaten counts).</li>
 * </ul>
 * Dying he leaves a colossal carcass (Butchery) and his axe as a heavy load ({@link ElkwightAxeEntity}).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public class Elkwight extends WildAnimal implements com.mechanical.wildlife.underground.Underground.Fearless {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/elkwight.png");
    private static final Identifier SCALE_ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_players");

    public static final double BASE_HEALTH = 400, HEALTH_PER_PLAYER = 0.6, DAMAGE_PER_PLAYER = 0.1;
    public static final double WAKE_RANGE = 20, SCALE_RANGE = 32, SLEEP_RANGE = 48, FLEE_RANGE = 24, CORPSE_RANGE = 16;
    static final double CHOP_RANGE = 3.6, SPIN_RADIUS = 4, SWEEP_WIDTH = 3, LEAP_MAX = 8;
    static final float SPIN_DAMAGE = 16, SWEEP_DAMAGE = 20, STOMP_DAMAGE = 6;
    public static final int EAT_TICKS = 60, STAGGER_TICKS = 20;
    public static final float MEAL_SHARE = 0.25F, EAT_DAMAGE_FACTOR = 1.5F, EAT_INTERRUPT = 0.08F;

    /** What he is doing: synced, the model poses each (ElkwightModel). */
    public static final int IDLE = 0, CHOP_WIND = 1, SPIN_WIND = 2, SPIN = 3, SWEEP_WIND = 4, CHARGE = 5, STAGGER = 6, THROW_WIND = 7,
            WAVE_WIND = 8, WAVE_SLAM = 9, LEAP_WIND = 10, LEAP = 11, BREAK_WIND = 12, EAT = 13, PICKUP = 14;
    /** Ticks each action lasts (LEAP and CHARGE end early on landing / impact). */
    static final int[] DURATION = {0, 14, 20, 10, 24, 30, STAGGER_TICKS, 20, 26, 10, 12, 40, 10, EAT_TICKS, 12};

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Elkwight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(Elkwight.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_AXE = SynchedEntityData.defineId(Elkwight.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), Component.translatable("entity.mechanical.elkwight"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    /** Ticks into the current action (server; the client counts its own from the change). */
    private int actionTicks;
    private int chopCd, spinCd = 40, sweepCd = 60, throwCd = 80, waveCd = 120, leapCd, breakCd, unreachableTicks, lonelyTicks, axeMissing;
    private BlockPos home;
    /** Where players died near him, oldest first; he goes to eat at each. */
    private final List<Vec3> corpses = new ArrayList<>();
    private int corpseWalk;
    /** Health gained so far in the current or last meal, and damage taken in it. */
    private float mealHealed, mealDamage;
    private Vec3 chargeDir = Vec3.ZERO, leapTarget = Vec3.ZERO;
    private final Set<Integer> hitThisAction = new HashSet<>();
    private final List<ElkwightTerrain.Wave> waves = new ArrayList<>();
    private UUID thrownAxe;
    /** Client: tickCount when the synced action last changed. */
    public int actionStart;

    public Elkwight(EntityType<? extends Elkwight> type, Level level) {
        super(type, level);
        xpReward = 250;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, BASE_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 20.0).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, SLEEP_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, IDLE);
        builder.define(AWAKE, false);
        builder.define(HAS_AXE, true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this)); // the rest is his own (customServerAiStep)
    }

    public int action() {
        return entityData.get(ACTION);
    }

    public boolean isAwake() {
        return entityData.get(AWAKE);
    }

    public boolean hasAxe() {
        return entityData.get(HAS_AXE);
    }

    /** Client: 0..1 through the current action, for the model. */
    public float actionProgress(float partialTicks) {
        int a = action();
        return a <= 0 ? 0 : Mth.clamp((tickCount - actionStart + partialTicks) / DURATION[a], 0.0F, 1.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ACTION.equals(key)) actionStart = tickCount;
    }

    private void act(int action) {
        entityData.set(ACTION, action);
        actionTicks = 0;
        hitThisAction.clear();
        getNavigation().stop();
    }

    /** Wakes him for a fight: the ruin calls this when a player comes in. Scales him to the players near. */
    public void wake() {
        if (level().isClientSide()) return;
        boolean was = isAwake();
        entityData.set(AWAKE, true);
        lonelyTicks = 0;
        scaleFor(fighters().size());
        if (!was) {
            playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.55F);
            playSound(SoundEvents.WARDEN_EMERGE, 2.0F, 0.8F);
        }
    }

    /** Players who count for the scaling and can be fought: within {@link #SCALE_RANGE}, not creative or spectating. */
    private List<Player> fighters() {
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(SCALE_RANGE), Elkwight::fair);
    }

    private static boolean fair(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    /** Health and damage for {@code players} (at least 1) fighting him; the share of health he had is kept. */
    public void scaleFor(int players) {
        int extra = Math.max(0, players - 1);
        float share = getHealth() / getMaxHealth();
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH), damage = getAttribute(Attributes.ATTACK_DAMAGE);
        health.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, HEALTH_PER_PLAYER * extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        damage.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, DAMAGE_PER_PLAYER * extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        setHealth(share * getMaxHealth());
    }

    /** A player died at {@code at} (within {@link #CORPSE_RANGE}): he will go and eat there. */
    public void corpseAt(Vec3 at) {
        corpses.add(at);
        if (!isAwake()) wake();
    }

    /** Dev (screenshot harness, with no AI): holds him in {@code action}. */
    public void showAction(int action) {
        entityData.set(ACTION, action);
    }

    /** For test messages: what he is up to. */
    String debug() {
        return "awake " + isAwake() + " axe " + hasAxe() + " action " + action() + " target " + getTarget() + " unreachable " + unreachableTicks
                + " leapCd " + leapCd + " breakCd " + breakCd + " path " + getNavigation().getPath() + " at " + position();
    }

    /** Health he got back from the current or last meal. */
    public float mealHealed() {
        return mealHealed;
    }

    float damageScale() {
        return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) / 20.0);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player dead) || !(dead.level() instanceof ServerLevel level)) return;
        for (Elkwight e : level.getEntitiesOfClass(Elkwight.class, dead.getBoundingBox().inflate(CORPSE_RANGE), Elkwight::isAlive))
            e.corpseAt(dead.position());
    }

    // ---------------------------------------------------------------- the fight

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = blockPosition();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        bossEvent.setVisible(isAwake());
        waves.removeIf(w -> !ElkwightTerrain.tickWave(level, this, w));
        if (!isAwake()) {
            if (tickCount % 10 == 0) {
                Player near = level.getNearestPlayer(this, WAKE_RANGE);
                if (near != null && fair(near) && hasLineOfSight(near) || someoneInRuin(level)) wake();
                else if (home != null && distanceToSqr(Vec3.atBottomCenterOf(home)) > 4) getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
            }
            return;
        }
        chopCd--;
        spinCd--;
        sweepCd--;
        throwCd--;
        waveCd--;
        leapCd--;
        breakCd--;
        if (action() != IDLE) {
            tickAction(level);
            return;
        }
        if (!hasAxe()) {
            fetchAxe(level);
            return;
        }
        if (!corpses.isEmpty()) {
            goEat();
            return;
        }
        LivingEntity t = target();
        if (t == null) {
            if (++lonelyTicks > 600) { // everyone left: back to sleep, the damage stays
                entityData.set(AWAKE, false);
                setTarget(null);
            }
            return;
        }
        lonelyTicks = 0;
        getLookControl().setLookAt(t, 30, 30);
        double d = distanceTo(t), dy = t.getY() - getY(), flat = Math.sqrt(Math.pow(t.getX() - getX(), 2) + Math.pow(t.getZ() - getZ(), 2));
        boolean far = d > FLEE_RANGE, sees = hasLineOfSight(t);
        if (far) { // he won't be outrun: charges and throws come twice as often
            sweepCd--;
            throwCd--;
        }
        if (d < CHOP_RANGE && Math.abs(dy) < 2.5) {
            if (spinCd <= 0 && (crowd(SPIN_RADIUS) > 1 || random.nextInt(3) == 0)) start(SPIN_WIND, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F);
            else if (chopCd <= 0) start(CHOP_WIND, SoundEvents.RAVAGER_ATTACK, 0.6F);
            return;
        }
        if (sweepCd <= 0 && d > 5 && d < (far ? 28 : 16) && sees && Math.abs(dy) < 2) {
            start(SWEEP_WIND, SoundEvents.HOGLIN_ANGRY, 0.5F);
            return;
        }
        if (throwCd <= 0 && d > 7 && d < (far ? 40 : 28) && sees) {
            start(THROW_WIND, SoundEvents.RAVAGER_ROAR, 0.9F);
            return;
        }
        if (waveCd <= 0 && d > 6 && d < 26 && Math.abs(dy) < 4) {
            start(WAVE_WIND, SoundEvents.WARDEN_SONIC_CHARGE, 0.7F);
            return;
        }
        // on foot; with no way there he leaps, smashes through, or keeps to the axe and the wave
        getNavigation().moveTo(t, far ? 1.25 : 1.0);
        if (tickCount % 10 == 0) {
            Path path = getNavigation().getPath();
            boolean reachable = path != null && path.canReach() || flat < 2.5 && Math.abs(dy) < 2;
            unreachableTicks = reachable ? 0 : unreachableTicks + 10;
        }
        if (unreachableTicks < 30) return;
        if (leapCd <= 0 && flat < 12 && (dy > 2 && dy <= LEAP_MAX || dy < -3 && dy >= -LEAP_MAX - 4) && ElkwightTerrain.landingFree(level, this, t.position())) {
            start(LEAP_WIND, SoundEvents.HOGLIN_ATTACK, 0.5F);
        } else if (breakCd <= 0 && ElkwightTerrain.breakableToward(level, this, t.position())) {
            start(BREAK_WIND, SoundEvents.RAVAGER_ATTACK, 0.5F);
        } else { // walled off by built blocks: the ranged attacks come sooner
            throwCd -= 2;
            waveCd -= 2;
            if (throwCd <= 0 && sees) start(THROW_WIND, SoundEvents.RAVAGER_ROAR, 0.9F);
            else if (waveCd <= 0 && d < 26) start(WAVE_WIND, SoundEvents.WARDEN_SONIC_CHARGE, 0.7F);
        }
    }

    /** A fair player has stepped into the ruin he guards (worldgen.ElkwightRuins: the bounds of his ruin, if he stands in one). */
    private boolean someoneInRuin(ServerLevel level) {
        if (tickCount % 20 != 0) return false;
        var ruin = com.mechanical.worldgen.ElkwightRuins.ruinAt(level, blockPosition());
        if (ruin.isEmpty()) return false;
        for (Player p : level.players()) if (fair(p) && ruin.get().isInside(p.blockPosition())) return true;
        return false;
    }

    /** The player he fights: the current one while fair and near, else the nearest fair one in range. */
    private LivingEntity target() {
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && !(t instanceof Player p && (p.isSpectator() || p.isCreative() && t.level().players().contains(p)))
                && distanceTo(t) < SLEEP_RANGE) return t;
        Player p = level().getNearestPlayer(getX(), getY(), getZ(), SLEEP_RANGE, e -> e instanceof Player pl && fair(pl));
        setTarget(p);
        return p;
    }

    private int crowd(double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), e -> e != this && e.isAlive()
                && (e instanceof Player p ? fair(p) : e == getTarget())).size();
    }

    private void start(int action, SoundEvent sound, float pitch) {
        act(action);
        playSound(sound, 2.5F, pitch);
    }

    private void tickAction(ServerLevel level) {
        int a = action(), t = ++actionTicks;
        LivingEntity target = getTarget();
        if (target != null && a != CHARGE && a != LEAP && a != EAT && a != STAGGER) getLookControl().setLookAt(target, 30, 30);
        switch (a) {
            case CHOP_WIND -> {
                if (t == DURATION[a]) {
                    if (target != null && distanceTo(target) < CHOP_RANGE + 0.8) doHurtTarget(level, target);
                    playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5F, 0.8F);
                    chopCd = 25;
                }
            }
            case SPIN_WIND -> {
                if (t == 12) { // the stomp
                    playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.6F);
                    dust(level, 20);
                }
                if (t == DURATION[a]) {
                    act(SPIN);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5F, 0.6F);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SPIN_RADIUS, 1, SPIN_RADIUS),
                            e -> e != this && e.isAlive() && distanceTo(e) <= SPIN_RADIUS + 0.6)) strike(level, e, SPIN_DAMAGE, 1.4, 0.45);
                    spinCd = 100;
                    return;
                }
            }
            case SWEEP_WIND -> {
                if (t % 8 == 0) { // scraping the ground with a hoof
                    playSound(SoundEvents.GRAVEL_STEP, 2.0F, 0.6F);
                    dust(level, 8);
                }
                if (t == DURATION[a]) {
                    Vec3 to = target != null ? target.position().subtract(position()) : Vec3.directionFromRotation(0, getYRot());
                    chargeDir = new Vec3(to.x, 0, to.z).normalize();
                    act(CHARGE);
                    playSound(SoundEvents.RAVAGER_ROAR, 2.5F, 1.1F);
                    return;
                }
            }
            case CHARGE -> {
                setYRot((float) Math.toDegrees(Math.atan2(-chargeDir.x, chargeDir.z)));
                yBodyRot = yHeadRot = getYRot();
                setDeltaMovement(chargeDir.x * 0.55, getDeltaMovement().y, chargeDir.z * 0.55);
                AABB front = getBoundingBox().move(chargeDir.scale(0.8)).inflate(SWEEP_WIDTH / 2 - 0.6 + 0.3, 0, SWEEP_WIDTH / 2 - 0.6 + 0.3);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, front, e -> e != this && e.isAlive()))
                    if (hitThisAction.add(e.getId())) strike(level, e, SWEEP_DAMAGE, 1.8, 0.5);
                if (horizontalCollision && t > 2) {
                    if (ElkwightTerrain.smashSoft(level, this, getBoundingBox().move(chargeDir.scale(0.9)))) return; // leaves and the like give way
                    act(STAGGER);
                    playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.5F);
                    dust(level, 25);
                    sweepCd = 140;
                    return;
                }
                if (t >= DURATION[a]) sweepCd = 140;
            }
            case THROW_WIND -> {
                if (t == DURATION[a]) {
                    if (target != null) {
                        ThrownElkAxe axe = ThrownElkAxe.thrownBy(level, this, target);
                        thrownAxe = axe.getUUID();
                        entityData.set(HAS_AXE, false);
                        axeMissing = 0;
                        playSound(SoundEvents.TRIDENT_THROW.value(), 2.0F, 0.5F);
                    }
                    throwCd = 120;
                }
            }
            case WAVE_WIND -> {
                if (t == DURATION[a]) {
                    act(WAVE_SLAM);
                    playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 3.0F, 0.5F);
                    playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.5F);
                    dust(level, 30);
                    groundWave(target != null ? target.position() : position().add(Vec3.directionFromRotation(0, getYRot())));
                    waveCd = 160;
                    return;
                }
            }
            case LEAP_WIND -> {
                if (t == DURATION[a] && target != null) {
                    leapTarget = target.position();
                    Vec3 v = ElkwightTerrain.leapVelocity(position(), leapTarget);
                    // straight up beside a tower (else he'd scrape up its side), steering over once above its top
                    boolean beside = leapTarget.y > getY() && leapTarget.subtract(position()).horizontalDistance() < 3;
                    setDeltaMovement(beside ? new Vec3(0, v.y, 0) : v);
                    needsSync = true;
                    act(LEAP);
                    playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 1.3F);
                    leapCd = 60;
                    return;
                }
            }
            case LEAP -> {
                Vec3 to = leapTarget.subtract(position());
                if (getY() >= leapTarget.y - 0.3 && to.horizontalDistance() > 0.8)
                    setDeltaMovement(Mth.clamp(to.x * 0.25, -0.6, 0.6), getDeltaMovement().y, Mth.clamp(to.z * 0.25, -0.6, 0.6));
                if (t > 4 && onGround()) { // the landing shakes everyone near
                    playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.5F, 0.6F);
                    dust(level, 30);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3, 1, 3), e -> e != this && e.isAlive()))
                        strike(level, e, STOMP_DAMAGE, 0.8, 0.4);
                    unreachableTicks = 0;
                    act(IDLE);
                    return;
                }
            }
            case BREAK_WIND -> {
                if (t == DURATION[a]) {
                    if (target != null) ElkwightTerrain.smashToward(level, this, target.position());
                    playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5F, 0.6F);
                    breakCd = 10;
                }
            }
            case EAT -> {
                getNavigation().stop();
                float heal = getMaxHealth() * MEAL_SHARE / EAT_TICKS;
                heal(heal);
                mealHealed += heal;
                if (t % 10 == 0) {
                    playSound(SoundEvents.GENERIC_EAT.value(), 1.5F, 0.5F);
                    playSound(SoundEvents.HOGLIN_AMBIENT, 1.0F, 0.6F);
                    level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM,
                            net.minecraft.world.item.Items.ROTTEN_FLESH), getX(), getY() + 1.2, getZ(), 6, 0.3, 0.2, 0.3, 0.05);
                }
                if (t >= DURATION[a] && !corpses.isEmpty()) corpses.removeFirst();
            }
            case PICKUP -> {
                if (t == DURATION[a]) {
                    entityData.set(HAS_AXE, true);
                    playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.5F, 0.7F);
                }
            }
            default -> {}
        }
        if (action() == a && t >= DURATION[a]) act(IDLE);
    }

    /** Sends a Ground Wave from his feet towards {@code toward}. */
    void groundWave(Vec3 toward) {
        Vec3 to = toward.subtract(position()), dir = new Vec3(to.x, 0, to.z).normalize();
        waves.add(new ElkwightTerrain.Wave(position().add(dir.scale(1.5)), dir, 24));
    }

    /** Damages {@code e} (scaled with the players he fights) and throws it {@code fling} away and {@code up}. */
    void strike(ServerLevel level, LivingEntity e, float damage, double fling, double up) {
        if (!e.hurtServer(level, damageSources().mobAttack(this), damage * damageScale())) return;
        Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ()).normalize().scale(fling * (1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5));
        e.setDeltaMovement(e.getDeltaMovement().add(away.x, up, away.z));
        e.needsSync = true;
    }

    private void dust(ServerLevel level, int count) {
        BlockState ground = level.getBlockState(getOnPos());
        if (!ground.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), count, 1.0, 0.1, 1.0, 0.15);
    }

    /** No axe in hand: wait for it to come back, or walk to where it stuck; it finds its way back if it goes missing. */
    private void fetchAxe(ServerLevel level) {
        Entity e = thrownAxe != null ? level.getEntity(thrownAxe) : null;
        if (!(e instanceof ThrownElkAxe axe)) {
            if (++axeMissing > 100) catchAxe(null); // unloaded or lost: a rune axe returns to its bearer
            return;
        }
        axeMissing = 0;
        if (axe.isStuck()) {
            getNavigation().moveTo(axe.getX(), axe.getY(), axe.getZ(), 1.0);
            if (distanceTo(axe) < 2.8) {
                axe.discard();
                thrownAxe = null;
                act(PICKUP);
            }
        }
    }

    /** The thrown axe is back in his hands ({@code axe}: it flew back to him, now gone). */
    void catchAxe(ThrownElkAxe axe) {
        if (axe != null) axe.discard();
        thrownAxe = null;
        entityData.set(HAS_AXE, true);
        playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.5F, 0.6F);
    }

    /** Walks to the oldest corpse and kneels to eat once there. */
    private void goEat() {
        Vec3 at = corpses.getFirst();
        if (distanceToSqr(at) < 2.6 * 2.6 || ++corpseWalk > 300) {
            corpseWalk = 0;
            if (distanceToSqr(at) >= 2.6 * 2.6) { // couldn't get there
                corpses.removeFirst();
                return;
            }
            mealHealed = mealDamage = 0;
            act(EAT);
            playSound(SoundEvents.HOGLIN_AMBIENT, 2.0F, 0.5F);
            return;
        }
        getNavigation().moveTo(at.x, at.y, at.z, 1.1);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() == this || source.getDirectEntity() instanceof ThrownElkAxe) return false;
        if (!isAwake() && source.getEntity() instanceof LivingEntity attacker) {
            wake();
            if (attacker instanceof Player || getTarget() == null) setTarget(attacker);
        }
        boolean eating = action() == EAT;
        if (eating) damage *= EAT_DAMAGE_FACTOR; // kneeling over his meal, he doesn't guard
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && eating) {
            mealDamage += damage;
            if (mealDamage >= getMaxHealth() * EAT_INTERRUPT) { // driven off: only what he had eaten counts
                if (!corpses.isEmpty()) corpses.removeFirst();
                act(STAGGER);
                playSound(SoundEvents.RAVAGER_STUNNED, 2.0F, 0.6F);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level && !isRemoved() && !dead) {
            // the axe falls beside him, or where a thrown one is
            Entity thrown = !hasAxe() && thrownAxe != null ? level.getEntity(thrownAxe) : null;
            ElkwightAxeEntity.dropAt(level, thrown != null ? thrown.position() : position().add(Vec3.directionFromRotation(0, yBodyRot + 90).scale(1.6)), yBodyRot);
            if (thrown != null) thrown.discard();
            bossEvent.removeAllPlayers();
        }
        super.die(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
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

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("awake", isAwake());
        output.putBoolean("has_axe", hasAxe());
        if (home != null) output.store("home", BlockPos.CODEC, home);
        if (thrownAxe != null) output.store("thrown_axe", net.minecraft.core.UUIDUtil.CODEC, thrownAxe);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(AWAKE, input.getBooleanOr("awake", false));
        entityData.set(HAS_AXE, input.getBooleanOr("has_axe", true));
        home = input.read("home", BlockPos.CODEC).orElse(null);
        thrownAxe = input.read("thrown_axe", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    // ---------------------------------------------------------------- the wildlife side

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.0F;
    }

    @Override
    public double fearMultiplier() {
        return 0.0;
    }

    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    public boolean isCharging() {
        return action() == SWEEP_WIND || action() == CHARGE;
    }

    @Override
    public float bleedThreshold() {
        return 0.3F;
    }

    @Override
    public boolean bloodOnlyWhenBleeding() {
        return true;
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    // shortcut: vanilla sounds pitched down (docs/elkwight-plan.md lists CC0 replacements); add own sound events when they exist
    @Override
    protected SoundEvent getAmbientSound() {
        return isAwake() ? SoundEvents.RAVAGER_AMBIENT : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 0.8F, 0.7F);
    }
}
