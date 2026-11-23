package com.mechanical.wildlife.savanna;

import com.mechanical.BloodMoon;
import com.mechanical.CarcassEntity;
import com.mechanical.wildlife.Predator;
import com.mechanical.wildlife.mountain.CaveHyena;
import com.mechanical.wildlife.warm.Onager;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Spotted hyena: the cave hyena's savanna cousin, out by day in packs of 3-5 and asleep at night. It keeps all of the cave
 * hyena's scavenging (runs in on any carcass left lying unwatched, drags it off and the pack guards it) and its fight
 * (circling, darting bites), but it hunts by daylight and is an opportunist: it trails and pulls down wounded animals
 * of any kind, and gazelles, young ostriches and aardvarks. A healthy person it leaves alone, unless he hurts one, comes
 * near the pack's kill or is badly hurt himself.
 */
public class SpottedHyena extends CaveHyena {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/spotted_hyena.png");
    /** A kill within this many blocks of a player is the pack's to defend. */
    private static final double KILL_RANGE = 14;

    public SpottedHyena(EntityType<? extends SpottedHyena> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // steal first (StealCarcassGoal, 4), eat later: the predators' carcass-scent feeding goes behind the stealing and guarding
        for (var w : java.util.List.copyOf(goalSelector.getAvailableGoals())) {
            if (w.getGoal().getClass().getSimpleName().equals("CarcassScentGoal")) {
                goalSelector.removeGoal(w.getGoal());
                goalSelector.addGoal(6, w.getGoal());
            }
        }
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    /** Below 40 % health: a wounded animal or person, fair game for a pack. */
    static boolean wounded(LivingEntity e) {
        return e.getHealth() < e.getMaxHealth() * 0.4F || com.mechanical.Blood.isBleeding(e);
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        if (e instanceof CaveHyena || e instanceof Predator && !wounded(e)) return false;
        return super.isPrey(e) || e instanceof Gazelle || e instanceof Aardvark || e instanceof Ostrich && e.isBaby()
                || e instanceof Onager && e.isBaby() || e instanceof Animal && wounded(e);
    }

    @Override
    protected boolean canHunt(LivingEntity e) {
        if (!super.canHunt(e)) return false;
        if (!(e instanceof Player p)) return true;
        CarcassEntity kill = packKill();
        return wounded(p) || p == getLastHurtByMob() || kill != null && p.distanceTo(kill) < KILL_RANGE;
    }

    @Override
    protected boolean huntingTime() {
        return level().isBrightOutside() || BloodMoon.isActive(level()) || starving() || entityTags().contains(SavannaFauna.ALWAYS_ACTIVE);
    }

    @Override
    protected boolean sleepTime() {
        return !huntingTime();
    }
}
