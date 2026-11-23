package com.mechanical.wildlife.deep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mechanical.AnimalFear;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.underground.Underground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cavern Spider: the mod's own cave spider, a hand-span-wide grey hunter of the shallow caves (Y 40 to 0, darkness rule).
 * It doesn't roam: it spins a great {@link CaveWebBlock} across the nearest narrow passage (wall to wall, floor to roof,
 * at most 3 by 3) and waits beside it. Whatever blunders into the web is held fast, and the spider comes for it; it also
 * goes for a player who comes within {@link #SIGHT} blocks. Its bite numbs (Slowness II). Climbs walls. Like the other
 * creatures of the dark it shuns torchlight: it leaves a torch-bearer alone and backs off.
 */
public class CavernSpider extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/cavern_spider.png");
    static final double SIGHT = 6, WEB_RANGE = 8;
    static final int BITE_TICKS = 60, MAX_SPAN = 3, WEAVE_EVERY = 600;

    public CavernSpider(EntityType<? extends CavernSpider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().equals("FollowHerdGoal")); // solitary
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (t, level) -> !Underground.inLight(t) && distanceTo(t) < SIGHT));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return horizontalCollision;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        LivingEntity t = getTarget();
        if (t != null && Underground.inLight(t) && distanceTo(t) < 6) {
            setTarget(null);
            AnimalFear.panic(this, t.position());
        }
        if (t == null && (tickCount + getId()) % WEAVE_EVERY == 0 && !webNear(level(), blockPosition())) weave(level(), blockPosition());
    }

    /** A Cave Web within {@link #WEB_RANGE} blocks. */
    static boolean webNear(Level level, BlockPos pos) {
        int r = (int) WEB_RANGE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -3, -r), pos.offset(r, 3, r)))
            if (level.getBlockState(p).is(DeepFauna.CAVE_WEB.get())) return true;
        return false;
    }

    /**
     * Spins a web across the nearest narrow passage within 4 blocks of {@code near}: a spot of air whose walls on one
     * horizontal axis are at most {@link #MAX_SPAN} apart, with a roof at most 3 up, open along the other axis (a passage,
     * not a niche). Every air block of that cross-section becomes web. Returns how many were placed (0: no passage).
     */
    public static int weave(Level level, BlockPos near) {
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(near.offset(-4, -1, -4), near.offset(4, 2, 4))) spots.add(p.immutable());
        spots.sort(Comparator.comparingDouble(p -> p.distSqr(near)));
        for (BlockPos p : spots) {
            if (!air(level, p) || !solid(level, p.below())) continue;
            for (Direction.Axis across : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                Direction neg = Direction.fromAxisAndDirection(across, Direction.AxisDirection.NEGATIVE), pos = neg.getOpposite();
                int lo = reach(level, p, neg), hi = reach(level, p, pos), up = reach(level, p, Direction.UP);
                if (lo < 0 || hi < 0 || up < 0 || lo + hi + 1 > MAX_SPAN || up + 1 > 3) continue;
                Direction along = across == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
                if (!air(level, p.relative(along)) || !air(level, p.relative(along.getOpposite()))) continue;
                int placed = 0;
                for (int a = -lo; a <= hi; a++) for (int y = 0; y <= up; y++) {
                    BlockPos w = p.relative(pos, a).above(y);
                    if (air(level, w) && level.setBlockAndUpdate(w, DeepFauna.CAVE_WEB.get().defaultBlockState())) placed++;
                }
                return placed;
            }
        }
        return 0;
    }

    /** Air blocks from {@code p} (exclusive) to the first solid block in {@code dir}, or -1 if none within 3. */
    private static int reach(Level level, BlockPos p, Direction dir) {
        for (int i = 1; i <= 3; i++) {
            BlockPos q = p.relative(dir, i);
            if (solid(level, q)) return i - 1;
            if (!air(level, q)) return -1;
        }
        return -1;
    }

    private static boolean air(Level level, BlockPos p) {
        return level.getBlockState(p).isAir();
    }

    private static boolean solid(Level level, BlockPos p) {
        return level.getBlockState(p).isSolidRender();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean bit = super.doHurtTarget(level, target);
        if (bit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, BITE_TICKS, 1), this);
        return bit;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return entityTags().contains(Predator.NIGHT_TAG);
    }

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
        return 0.0; // it doesn't run from people; light drives it off
    }

    @Override
    protected boolean sleepTime() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.2F);
    }
}
