package com.mechanical.people;

import java.util.EnumSet;

import com.mechanical.Foraging;
import com.mechanical.WildBerryBushBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Tribespeople look after what they live on: when there are few trees around camp, woodcutters plant saplings (of the
 * nearest tree's kind, or one that fits the biome), and when there are few berry bushes, gatherers plant young wild
 * bushes that suit the climate. Never right by the fire, and spaced out so the camp doesn't turn into a thicket.
 */
class PlantGoal extends Goal {
    private static final int RANGE = 14, MIN_FROM_FIRE = 5, SPACING = 4;
    private static final int ENOUGH_TREES = 4, ENOUGH_BUSHES = 3;
    private final Tribesperson mob;
    private BlockPos spot;
    private BlockState plant;
    private int ticks;

    PlantGoal(Tribesperson mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        boolean woodcutter = mob.canWork(TribeRole.WOODCUTTER), gatherer = mob.canWork(TribeRole.GATHERER);
        if ((!woodcutter && !gatherer) || mob.home() == null || mob.carryingWood || mob.getRandom().nextInt(400) != 0) return false;
        ServerLevel level = (ServerLevel) mob.level();
        BlockPos home = mob.home();
        if (woodcutter) {
            if (countAround(level, mob, s -> isTrunk(s) || s.getBlock() instanceof SaplingBlock) >= ENOUGH_TREES) return false;
            plant = saplingNear(level, home);
        } else {
            if (countAround(level, mob, s -> s.getBlock() instanceof WildBerryBushBlock) >= ENOUGH_BUSHES) return false;
            plant = bushFor(level, home);
        }
        if (plant == null) return false;
        spot = findSpot(level, mob, plant);
        return spot != null;
    }

    @Override
    public void start() {
        ticks = 0;
        mob.walkTo(spot, 0.6);
    }

    @Override
    public boolean canContinueToUse() {
        return spot != null && ticks < 900 && mob.getTarget() == null && !mob.level().isDarkOutside();
    }

    @Override
    public void tick() {
        ticks++;
        Vec3 c = Vec3.atBottomCenterOf(spot);
        mob.getLookControl().setLookAt(c);
        if (mob.distanceToSqr(c) > 2.2 * 2.2) {
            if (mob.getNavigation().isDone()) mob.walkTo(spot, 0.6);
            return;
        }
        mob.getNavigation().stop();
        ServerLevel level = (ServerLevel) mob.level();
        if (plantAt(level, spot, plant)) mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        spot = null;
    }

    @Override
    public void stop() {
        spot = null;
        plant = null;
    }

    /** Places a plant if the spot is still free; dirt particles and a planting sound. */
    static boolean plantAt(ServerLevel level, BlockPos pos, BlockState plant) {
        if (!level.getBlockState(pos).canBeReplaced() || !plant.canSurvive(level, pos)) return false;
        level.setBlock(pos, plant, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
        return true;
    }

    /** Woodcutters replant: a sapling of the felled tree's kind beside its stump. */
    static void replant(ServerLevel level, BlockPos stump, BlockState log) {
        BlockState sapling = saplingFor(log);
        if (sapling == null) return;
        for (BlockPos p : BlockPos.betweenClosed(stump.offset(-2, -1, -2), stump.offset(2, 1, 2))) {
            if (p.distManhattan(stump) >= 2 && plantAt(level, p.immutable(), sapling)) return;
        }
    }

    private static boolean isTrunk(BlockState s) {
        return s.is(BlockTags.LOGS);
    }

    /** Ground-level trunks (or saplings / bushes) round the camp (outside it), sampled on the surface. */
    private static int countAround(ServerLevel level, Tribesperson mob, java.util.function.Predicate<BlockState> what) {
        int n = 0, r = mob.campRadius() + RANGE;
        BlockPos home = mob.home();
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                BlockPos col = home.offset(dx, 0, dz);
                if (!mob.outsideCamp(col) || !level.isLoaded(col)) continue;
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
                for (int y = -1; y <= 1; y++) if (what.test(level.getBlockState(ground.above(y)))) n++;
            }
        }
        return n;
    }

    /** The sapling of the nearest tree's kind; failing that, one that fits the biome. */
    private static BlockState saplingNear(ServerLevel level, BlockPos home) {
        for (int r = 4; r <= 32; r += 4) {
            for (int i = 0; i < 24; i++) {
                BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        home.offset(level.getRandom().nextInt(r * 2 + 1) - r, 0, level.getRandom().nextInt(r * 2 + 1) - r));
                for (int y = 0; y <= 2; y++) {
                    BlockState s = level.getBlockState(p.above(y));
                    if (isTrunk(s)) {
                        BlockState sapling = saplingFor(s);
                        if (sapling != null) return sapling;
                    }
                }
            }
        }
        float t = level.getBiome(home).value().getBaseTemperature();
        Block fallback = t < 0.35F ? Blocks.SPRUCE_SAPLING : t > 1.5F ? Blocks.ACACIA_SAPLING : Blocks.OAK_SAPLING;
        return fallback.defaultBlockState();
    }

    /** oak_log -> oak_sapling, mechanical:aspen_log -> mechanical:aspen_sapling; trees that need 2x2 or roots fall back to oak. */
    static BlockState saplingFor(BlockState log) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String wood = id.getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");
        if (wood.equals("dark_oak") || wood.equals("mangrove") || wood.equals("pale_oak")) wood = "oak";
        var sapling = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), wood + "_sapling"));
        if (sapling.isEmpty()) sapling = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(wood + "_sapling"));
        return sapling.filter(b -> b instanceof SaplingBlock).map(Block::defaultBlockState).orElse(null);
    }

    /** A young wild bush that suits the climate (cold: lingonberry, blackberry; mild: raspberry, blueberry, elderberry, strawberry). */
    private static BlockState bushFor(Level level, BlockPos home) {
        float t = level.getBiome(home).value().getBaseTemperature();
        String[] kinds = t < 0.35F ? new String[] {"lingonberry", "blackberry", "blueberry"}
                : t > 1.2F ? new String[] {"wild_strawberry"}
                : new String[] {"raspberry", "blueberry", "elderberry", "wild_strawberry"};
        var bush = Foraging.BUSH_BLOCKS.get(kinds[level.getRandom().nextInt(kinds.length)]);
        return bush == null ? null : bush.get().defaultBlockState().setValue(WildBerryBushBlock.AGE, 0);
    }

    /**
     * A free, open, sunny spot on natural ground round the camp (at least MIN_FROM_FIRE from home and never inside a
     * village's or camp's wall, where a tree would grow into the huts), SPACING clear of other plants.
     */
    private static BlockPos findSpot(ServerLevel level, Tribesperson mob, BlockState plant) {
        BlockPos home = mob.home();
        int r = mob.campRadius() + RANGE;
        for (int i = 0; i < 30; i++) {
            BlockPos col = home.offset(level.getRandom().nextInt(r * 2 + 1) - r, 0, level.getRandom().nextInt(r * 2 + 1) - r);
            if (!mob.outsideCamp(col) || !level.isLoaded(col)) continue;
            BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
            if (p.distSqr(home) < MIN_FROM_FIRE * MIN_FROM_FIRE) continue;
            if (!level.getBlockState(p).canBeReplaced() || !level.getFluidState(p).isEmpty() || !level.canSeeSky(p) || !plant.canSurvive(level, p)) continue;
            if (!level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) continue;
            boolean crowded = false;
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-SPACING, -1, -SPACING), p.offset(SPACING, 2, SPACING))) {
                BlockState s = level.getBlockState(q);
                if (isTrunk(s) || s.getBlock() instanceof SaplingBlock || s.getBlock() instanceof WildBerryBushBlock || s.is(Blocks.CAMPFIRE)) {
                    crowded = true;
                    break;
                }
            }
            if (!crowded) return p.immutable();
        }
        return null;
    }
}
