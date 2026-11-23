package com.mechanical.worldgen;

import java.util.List;

import com.mechanical.FallingTreeEntity;
import com.mechanical.Kettle;
import com.mechanical.Mechanical;
import com.mechanical.TreeFelling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Storm felling (terrain-plan phase 5): in a thunderstorm the wind now and then brings down a tall tree near a player.
 * It cracks and tips over with the storm's wind (a FallingTreeEntity, as when felled by hand) and its trunk comes to rest
 * as a fallen log of log blocks ({@link #lie}) that then rots ({@link NatureRot}); the crown lands as leaf litter and a
 * stump is left. Unlike the rest of the nature clock this is meant to be seen, but most fall out of sight: a tree in a
 * player's view is passed over two times in three. Never on players' land, camps, roads or structures
 * ({@link NatureClock#free}). Advancement Windthrow for whoever sees one come down.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class StormFelling {
    /** One in this many seconds of thunder brings a tree down near each player. */
    static final int ONE_IN = 50;
    /** Tall enough to fall: trunk height; and no giants (logs). */
    static final int MIN_TRUNK = 6, MAX_LOGS = 48;
    /** How far from the player trees fall. */
    static final int NEAR = 12, FAR = 48;
    /** The longest trunk laid down as a fallen log. */
    static final int MAX_LOG = 12;
    static final Identifier WINDTHROW = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/windthrow");

    private StormFelling() {}

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0
                || !level.isThundering() || NatureClock.paused) return;
        for (ServerPlayer p : level.players())
            if (!p.isSpectator() && level.getRandom().nextInt(ONE_IN) == 0) fellNear(level, p, false);
    }

    /** The storm's wind: one direction for a few minutes, the way trees fall. */
    static Direction wind(ServerLevel level) {
        long gust = level.getGameTime() / 6000;
        return Direction.from2DDataValue((int) Math.floorMod(gust * 0x9E3779B9L ^ level.getSeed(), 4));
    }

    /** Brings down a tall tree near the player (demo: in front of them, any land, a shorter trunk will do). */
    public static boolean fellNear(ServerLevel level, ServerPlayer player, boolean demo) {
        RandomSource r = level.getRandom();
        Vec3 look = player.getLookAngle();
        double facing = Math.atan2(look.z, look.x);
        for (int tries = 0; tries < 40; tries++) {
            double angle = demo ? facing + (r.nextDouble() - 0.5) * 0.9 : r.nextDouble() * Math.PI * 2;
            double dist = demo ? 6 + r.nextInt(18) : NEAR + r.nextInt(FAR - NEAR);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist), z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            for (int i = 0; i < 6 && !upright(level.getBlockState(top)) && level.getBlockState(top).is(BlockTags.LOGS); i++) top = top.below(); // past branches
            BlockPos base = trunkBase(level, top);
            if (base == null || top.getY() - base.getY() + 1 < (demo ? 4 : MIN_TRUNK)) continue;
            if (!demo && !NatureClock.free(level, base)) continue;
            BlockPos crown = base.above((top.getY() - base.getY()) / 2);
            if (!demo && NatureClock.seen(level, crown) && r.nextInt(3) != 0) continue; // most fall where nobody looks
            TreeFelling.Tree tree = TreeFelling.find(level, base);
            if (tree == null || !demo && tree.logs().size() > MAX_LOGS) continue;
            Direction dir = demo ? player.getDirection().getClockWise() : wind(level);
            fell(level, base, tree, dir);
            return true;
        }
        return false;
    }

    /** /nature fell: the nearest tree of 4+ logs in front of pos (within 30 blocks, 25 degrees of the yaw) falls across the view. */
    public static boolean fellInFront(ServerLevel level, Vec3 pos, float yaw) {
        Vec3 look = Vec3.directionFromRotation(0, yaw);
        for (int d = 3; d <= 30; d++)
            for (int side = -d / 2; side <= d / 2; side++) {
                Vec3 at = pos.add(look.scale(d)).add(-look.z * side, 0, look.x * side);
                int x = (int) Math.floor(at.x), z = (int) Math.floor(at.z);
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                for (int i = 0; i < 8 && !upright(level.getBlockState(top)) && level.getBlockState(top).is(BlockTags.LOGS); i++) top = top.below();
                BlockPos base = trunkBase(level, top);
                if (base == null || top.getY() - base.getY() < 3) continue;
                TreeFelling.Tree tree = TreeFelling.find(level, base);
                if (tree == null) continue;
                fell(level, base, tree, Direction.fromYRot(yaw).getClockWise());
                return true;
            }
        return false;
    }

    /** The bottom log of a natural upright trunk whose top is at {@code top}, standing on the ground; null if it isn't one. */
    static BlockPos trunkBase(ServerLevel level, BlockPos top) {
        BlockState s = level.getBlockState(top);
        if (!upright(s)) return null;
        BlockPos p = top;
        for (int i = 0; i < 32 && level.getBlockState(p.below()).is(s.getBlock()); i++) p = p.below();
        return level.getBlockState(p.below()).is(BlockTags.SUBSTRATE_OVERWORLD) ? p : null;
    }

    static boolean upright(BlockState s) {
        return s.is(BlockTags.LOGS) && s.hasProperty(RotatedPillarBlock.AXIS) && s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y;
    }

    /** The tree goes over towards dir with a crack; whoever sees it gets Windthrow. */
    static FallingTreeEntity fell(ServerLevel level, BlockPos base, TreeFelling.Tree tree, Direction dir) {
        level.playSound(null, base.above(3), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.5F, 0.5F);
        level.playSound(null, base.above(3), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 3.0F, 0.4F);
        FallingTreeEntity entity = TreeFelling.fell(level, base, tree, dir, null, ItemStack.EMPTY);
        entity.windfall = true;
        Vec3 c = Vec3.atCenterOf(base.above(3));
        for (ServerPlayer p : level.players()) {
            Vec3 to = c.subtract(p.getEyePosition());
            if (to.length() < 64 && to.normalize().dot(p.getLookAngle()) > 0.5) Kettle.award(p, WINDTHROW);
        }
        return entity;
    }

    /**
     * The trunk (the logs straight above the cut, at most {@link #MAX_LOG}) lies down from the stump along dir as log blocks
     * on the ground, the way ForestDecor lays a fallen log; branches break off. False if there is no room (it lands as a
     * FelledLogEntity then).
     */
    public static boolean lie(ServerLevel level, BlockPos base, Direction dir, List<BlockPos> offsets, List<BlockState> states) {
        int n = 0;
        BlockState log = null;
        for (int i = 0; i < offsets.size(); i++) {
            BlockPos o = offsets.get(i);
            if (o.getX() != 0 || o.getZ() != 0) continue;
            n++;
            if (log == null || o.getY() == 0) log = states.get(i);
        }
        if (log == null || n < 2) return false;
        BlockPos start = base.relative(dir);
        List<BlockPos> cells = ForestDecor.logCells(level, start, dir, Math.min(n, MAX_LOG), true, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        if (cells.size() < 2) return false;
        ForestDecor.lay(level, cells, log.getBlock(), dir.getAxis());
        return true;
    }
}
