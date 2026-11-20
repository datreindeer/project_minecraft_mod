package com.mechanical.wildlife.forestbirds;

import java.util.Comparator;

import com.mechanical.CarcassEntity;
import com.mechanical.wildlife.FlockBird;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Common raven: big, black, clever and wary. A pair or a small party roams the forests and mountains. Ravens follow a
 * hunter (a player with a bow or a spear in hand) at a distance, wheeling high over him, waiting for the kill; any
 * carcass lying about ({@link CarcassEntity}, not carried or hung) draws them down to sit around it and peck at it, and
 * a scrap of meat left on the ground nearby is grabbed and carried off (shoot the thief and it drops it).
 * No carcass of its own (feathers and Raven Feathers, loot table).
 */
public class Raven extends FlockBird {
    static final Kind KIND = new Kind("raven", 0.46, 8, 18, 400, 900, 400, 1200, false, false, true, 1.3,
            () -> SoundEvents.PARROT_AMBIENT, 0.35F);
    static final double CARRION_RANGE = 48, HUNTER_RANGE = 40, FOLLOW_RADIUS = 14, STEAL_REACH = 2.0;
    private CarcassEntity carrion;
    private Player hunter;
    private ItemStack stolen = ItemStack.EMPTY;

    public Raven(EntityType<? extends Raven> type, Level level) {
        super(type, level, KIND);
    }

    /** A bow, crossbow or spear in hand: someone about to make a kill. */
    static boolean isHunter(Player p) {
        ItemStack held = p.getMainHandItem();
        return !p.isSpectator() && (held.getItem() instanceof ProjectileWeaponItem || held.is(ItemTags.SPEARS));
    }

    public ItemStack stolen() {
        return stolen;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if ((tickCount + getId()) % 40 == 0) {
            carrion = level.getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(CARRION_RANGE, 32, CARRION_RANGE),
                    c -> c.hauler() == null && c.hungAt() == null && !c.isPassenger())
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            hunter = carrion != null ? null : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(HUNTER_RANGE), Raven::isHunter)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        if (isFlying() || !onGround()) return;
        if (carrion != null && carrion.isAlive() && distanceToSqr(carrion) < 9) {
            restTimer = Math.max(restTimer, 60); // stays at the carcass
            if ((tickCount + getId()) % 30 == 0 && random.nextBoolean()) playSound(SoundEvents.GENERIC_EAT.value(), 0.4F, 1.4F);
        }
        if (stolen.isEmpty() && (tickCount + getId()) % 10 == 0) steal(level);
    }

    /** Grabs a scrap of meat lying close by and makes off with it. */
    private void steal(ServerLevel level) {
        ItemEntity scrap = level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(STEAL_REACH), e -> e.getItem().is(ForestBirdsFauna.RAVEN_SCRAPS))
                .stream().findFirst().orElse(null);
        if (scrap == null) return;
        stolen = scrap.getItem().split(1);
        if (scrap.getItem().isEmpty()) scrap.discard();
        playSound(SoundEvents.PARROT_AMBIENT, 1.0F, 0.4F);
        Player near = level.getNearestPlayer(this, 24);
        takeOff(true, near != null ? near.position() : position().add(random.nextGaussian(), 0, random.nextGaussian()));
    }

    @Override
    protected Vec3 pickTarget(Vec3 from) {
        if (from == null && carrion != null && carrion.isAlive()) return carrion.position().add(random.nextGaussian() * 6, 8 + random.nextInt(6), random.nextGaussian() * 6);
        return super.pickTarget(from);
    }

    @Override
    protected Vec3 cruise() {
        if (scared) return super.cruise();
        if (carrion != null && carrion.isAlive()) {
            if (horizontalDistSqr(carrion.position()) < 24 * 24 && (landingSpot = findLandingSpot(carrion.position(), 3)) != null) {
                landingTimer = 0;
                return approach(landingSpot);
            }
            target = carrion.position().add(0, 10, 0);
            return keepAltitude(seek(target, speed()), 2);
        }
        if (hunter != null && hunter.isAlive()) {
            // wheel high over the hunter, at a respectful distance
            flightTimer = Math.max(flightTimer, 60);
            double a = tickCount * speed() / FOLLOW_RADIUS + getId();
            target = hunter.position().add(Math.cos(a) * FOLLOW_RADIUS, 11 + getId() % 4, Math.sin(a) * FOLLOW_RADIUS);
            return keepAltitude(seek(target, speed()), 3);
        }
        return super.cruise();
    }

    /** Down again somewhere safe: the stolen scrap is eaten. */
    @Override
    protected void land() {
        stolen = ItemStack.EMPTY;
        super.land();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!stolen.isEmpty()) spawnAtLocation(level, stolen);
        stolen = ItemStack.EMPTY;
    }
}
