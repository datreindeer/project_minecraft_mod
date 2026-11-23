package com.mechanical.wildlife.temperate;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Great spotted woodpecker: a black-and-white bird with a red patch, alone or in pairs. It keeps to the dead trees of the
 * wood (Grub Logs, {@link GrubLogBlock}), lands on top of one and drums on it: a loud, fast rattle you hear far off
 * ({@link #DRUM_VOLUME}). Follow the drumming and you find the rotten snag and the grubs in it (bait).
 * No carcass of its own: a feather or two and its red Woodpecker Feather (loot table).
 */
public class Woodpecker extends FlockBird {
    static final Kind KIND = new Kind("woodpecker", 0.42, 4, 10, 120, 300, 500, 1200, false, false, true, 1.1,
            () -> SoundEvents.PARROT_AMBIENT, 1.7F);
    /** How far it looks for a grub tree (blocks), how loud the drumming is (16 blocks per unit), taps per roll. */
    static final int SEARCH = 24, TAPS = 8;
    static final float DRUM_VOLUME = 3.0F;
    private BlockPos grubTree;
    private int drum, drumCooldown;

    public Woodpecker(EntityType<? extends Woodpecker> type, Level level) {
        super(type, level, KIND);
    }

    public BlockPos grubTree() {
        return grubTree;
    }

    /** Nearest Grub Log within {@link #SEARCH} blocks of {@code around}, or null. */
    BlockPos findGrubTree(Vec3 around) {
        BlockPos c = BlockPos.containing(around), best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-SEARCH, -8, -SEARCH), c.offset(SEARCH, 8, SEARCH))) {
            if (level().getBlockState(p).is(TemperateFauna.GRUB_LOG.get()) && !level().getBlockState(p.above()).is(TemperateFauna.GRUB_LOG.get())
                    && level().getBlockState(p.above()).isAir() && p.distSqr(c) < bestDist) {
                bestDist = p.distSqr(c);
                best = p.immutable();
            }
        }
        return best;
    }

    /** Comes down on top of its grub tree when there is one about. */
    @Override
    protected Vec3 findLandingSpot(Vec3 around, int radius) {
        if (grubTree == null || !level().getBlockState(grubTree).is(TemperateFauna.GRUB_LOG.get())) grubTree = findGrubTree(position());
        if (grubTree != null) {
            home = Vec3.atBottomCenterOf(grubTree);
            return Vec3.atBottomCenterOf(grubTree.above());
        }
        return super.findLandingSpot(around, radius);
    }

    /** Sitting on (or by) its grub tree: now and then a drum roll. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel) || isFlying()) return;
        if (drum > 0) {
            if (drum-- % 2 == 0) playSound(SoundEvents.WOOD_HIT, DRUM_VOLUME, 1.6F + random.nextFloat() * 0.2F);
            return;
        }
        if (--drumCooldown > 0 || grubTree == null || !level().isBrightOutside() || distanceToSqr(Vec3.atCenterOf(grubTree)) > 9) return;
        drum = TAPS * 2;
        drumCooldown = 60 + random.nextInt(100);
    }

    /** A drum roll is under way (by day, on its grub tree). */
    public boolean isDrumming() {
        return drum > 0;
    }
}
