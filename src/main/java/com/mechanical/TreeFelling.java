package com.mechanical;

import com.mechanical.skills.Skill;
import com.mechanical.skills.SkillPerks;
import com.mechanical.skills.Skills;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Chopping a log of a natural tree (logs with non-persistent leaves) with an axe fells the whole tree:
 * chopping takes longer per log, the axe loses one durability per log, and the tree falls away from the player (or to
 * a side if a wall or another tree stands in its way).
 * Sneaking chops a single log.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TreeFelling {
    private static final int MAX_LOGS = 256, MAX_LEAVES = 2048, MIN_LEAVES = 4;

    public record Tree(List<BlockPos> logs, List<BlockPos> leaves) {}

    /** Last scan per player, so the per-tick break speed check doesn't rescan the tree every tick. */
    private record Cached(BlockPos pos, long time, Tree tree) {}
    private static final Map<Player, Cached> CACHE = new WeakHashMap<>();

    private static boolean wantsToFell(Player player, BlockState state) {
        return state.is(BlockTags.LOGS) && !player.isShiftKeyDown() && player.getMainHandItem().is(ItemTags.AXES);
    }

    /** All logs connected to {@code cut} at or above it, plus the leaves that belong to them; null if it isn't a natural tree. */
    public static Tree find(Level level, BlockPos cut) {
        Set<BlockPos> logs = new HashSet<>(List.of(cut));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(logs);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            // also downwards (drooping acacia/jungle branches), but never below the cut so the stump stays
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (next.getY() >= cut.getY() && !logs.contains(next) && level.getBlockState(next).is(BlockTags.LOGS)) {
                    BlockPos immutable = next.immutable();
                    logs.add(immutable);
                    queue.add(immutable);
                }
            }
        }

        // A leaf belongs to this tree if it is as far from our logs as its DISTANCE says, i.e. no other tree is closer.
        Set<BlockPos> leaves = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>(logs);
        for (int step = 1; step <= LeavesBlock.DECAY_DISTANCE && !frontier.isEmpty() && leaves.size() < MAX_LEAVES; step++) {
            ArrayDeque<BlockPos> nextFrontier = new ArrayDeque<>();
            for (BlockPos pos : frontier) {
                for (Direction dir : Direction.values()) {
                    BlockPos next = pos.relative(dir);
                    BlockState state = level.getBlockState(next);
                    if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)
                            && state.getValue(LeavesBlock.DISTANCE) == step && !logs.contains(next) && leaves.add(next)) {
                        nextFrontier.add(next);
                    }
                }
            }
            frontier = nextFrontier;
        }
        return leaves.size() >= MIN_LEAVES ? new Tree(List.copyOf(logs), List.copyOf(leaves)) : null;
    }

    private static Tree findCached(Player player, BlockPos pos) {
        long now = player.level().getGameTime();
        Cached cached = CACHE.get(player);
        if (cached == null || !cached.pos.equals(pos) || now - cached.time > 20) {
            cached = new Cached(pos.immutable(), now, find(player.level(), pos));
            CACHE.put(player, cached);
        }
        return cached.tree;
    }

    // Runs on both sides so client-predicted mining progress matches the server.
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled() || event.getPosition().isEmpty() || !wantsToFell(event.getEntity(), event.getState())) return;
        Tree tree = findCached(event.getEntity(), event.getPosition().get());
        if (tree != null) event.setNewSpeed(event.getNewSpeed() / tree.logs().size());
    }

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || !wantsToFell(player, event.getState())) return;
        BlockPos cut = event.getPos();
        Tree tree = find(level, cut);
        CACHE.remove(player);
        if (tree == null) return;
        if (player instanceof ServerPlayer sp) Skills.addXp(sp, Skill.WOODCUTTING, 3 * tree.logs().size());

        // We take over the whole break (cancel vanilla) so the cut log falls with the tree instead of dropping at the stump.
        event.setCanceled(true);
        event.setNotifyClient(false); // otherwise Neo re-sends the old log to the client, hiding the stump
        ItemStack axe = player.getMainHandItem();
        fell(level, cut, tree, player.getDirection(), player, axe);
        if (SkillPerks.WOOD_LOG.roll(player)) Block.popResource(level, cut, new ItemStack(event.getState().getBlock()));
        // one durability per log; breaks the axe if it runs out, the tree still falls
        if (!SkillPerks.keepsTool(player, axe)) axe.hurtAndBreak(tree.logs().size(), player, EquipmentSlot.MAINHAND);
    }

    /** Fells {@code tree} cut at {@code cut}: it falls towards {@code direction} as a FallingTreeEntity, a stump is left. Also used by woodcutter NPCs. */
    public static FallingTreeEntity fell(ServerLevel level, BlockPos cut, Tree tree, net.minecraft.core.Direction direction, net.minecraft.world.entity.Entity feller, ItemStack tool) {
        BlockState cutState = level.getBlockState(cut);
        List<BlockPos> falling = new ArrayList<>(tree.logs());
        falling.addAll(tree.leaves());
        FallingTreeEntity entity = FallingTreeEntity.create(level, cut, direction, falling, feller, tool);
        for (BlockPos pos : falling) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        String log = BuiltInRegistries.BLOCK.getKey(cutState.getBlock()).getPath();
        var stump = Mechanical.STUMPS.get(log.replace("_log", ""));
        if (stump != null) level.setBlock(cut, stump.get().defaultBlockState(), Block.UPDATE_ALL);
        entity.clearPath(level); // with the tree gone: a wall or another tree in the way turns it aside
        level.addFreshEntity(entity);
        return entity;
    }

    private TreeFelling() {}
}
