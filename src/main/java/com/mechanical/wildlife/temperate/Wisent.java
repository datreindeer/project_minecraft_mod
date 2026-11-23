package com.mechanical.wildlife.temperate;

import com.mechanical.wildlife.giants.SteppeBison;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * European bison (wisent): the giant of the broadleaf forest, in small herds. Darker, longer-legged and with a higher
 * shoulder than the steppe bison it shares its build and herd ways with ({@link SteppeBison}: bulls guard the herd,
 * a panicked herd stampedes). Peaceful, but dangerous when cornered: every grown wisent, cow or bull, turns on whatever
 * hurts it, and one that a player crowds ({@link #CORNER_RANGE} blocks, not sneaking) for {@link #CORNER_TICKS} charges.
 */
public class Wisent extends SteppeBison {
    private static final Identifier BULL = tex("wisent_bull"), COW = tex("wisent_cow"), CALF = tex("wisent_calf");
    static final double CORNER_RANGE = 3.0;
    static final int CORNER_TICKS = 40;
    private int crowded;

    public Wisent(EntityType<? extends Wisent> type, Level level) {
        super(type, level);
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/temperate/" + name + ".png");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return SteppeBison.createAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    /** Crowding counts up while a player stands close (sneaking players are left alone); at the limit it charges. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel) || isBaby() || getTarget() != null) return;
        Player near = level().getNearestPlayer(this, CORNER_RANGE);
        if (near != null && !near.isCreative() && !near.isSpectator() && !near.isShiftKeyDown()) {
            if (++crowded >= CORNER_TICKS) {
                setTarget(near);
                crowded = 0;
            }
        } else if (crowded > 0) {
            crowded--;
        }
    }

    /** Cows too stand their ground when hurt (the steppe bison's cows flee). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && !isBaby() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof SteppeBison)) setTarget(attacker);
        return hurt;
    }

    /** Any grown wisent with a target fights (the steppe bison's attack goal is for bulls only). */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.5, false) {
            @Override
            public boolean canUse() {
                return !isBaby() && super.canUse();
            }
        });
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CALF : isBull() ? BULL : COW;
    }

    @Override
    protected float babyChance() {
        return 0.25F;
    }

    /** Calmer than the steppe bison: it lets you come closer before it moves off. */
    @Override
    public double fearMultiplier() {
        return 0.8;
    }
}
