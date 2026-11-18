package com.mechanical.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import com.mechanical.Mechanical;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side dynamic lights: anyone holding a light-emitting item (torch, lantern, glowstone ...) and dropped light items
 * light up their surroundings, for every player (held items are synced). Light floods outward block by block like vanilla
 * block light, so walls stop it but it bends around corners. The flood is computed on the main thread each tick and
 * published as an immutable map, because section meshing reads {@link #levelAt} from worker threads.
 * Sections around a source are rebuilt when it moves a block.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DynamicLights {
    /** Where a source was last tick (block + emission), so terrain is only re-meshed when that changes. */
    private record Seen(BlockPos pos, int emission) {}

    private static final double RANGE = 48; // only sources this close to the player
    private static volatile Long2ByteOpenHashMap light = new Long2ByteOpenHashMap();
    private static final Map<Integer, Seen> LAST = new HashMap<>();

    /** Dynamic block light (0..15) at a block. Thread-safe and allocation-free (the map is never mutated once published). */
    public static int levelAt(int x, int y, int z) {
        return light.get(BlockPos.asLong(x, y, z));
    }

    private static int emission(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState().getLightEmission() : com.mechanical.AncientOak.lightOf(stack);
    }

    private static int emission(Entity entity) {
        if (entity instanceof com.mechanical.wildlife.GiantDeer) return 7; // a soft glow around the giant
        if (entity instanceof LivingEntity living) return Math.max(emission(living.getMainHandItem()), emission(living.getOffhandItem()));
        if (entity instanceof ItemEntity item) return emission(item.getItem());
        return 0;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            light = new Long2ByteOpenHashMap();
            LAST.clear();
            return;
        }
        Long2ByteOpenHashMap map = new Long2ByteOpenHashMap();
        Map<Integer, Seen> seen = new HashMap<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            int emission = emission(entity);
            if (emission <= 0) continue;
            BlockPos pos = BlockPos.containing(entity.getX(), entity.getEyeY() - 0.2, entity.getZ());
            flood(mc.level, pos, emission, map);
            Seen now = new Seen(pos, emission);
            seen.put(entity.getId(), now);
            if (!now.equals(LAST.get(entity.getId()))) markDirty(mc, pos, emission);
        }
        // sources that went out, moved or were put away: re-mesh where they were
        for (Map.Entry<Integer, Seen> e : LAST.entrySet()) {
            if (!e.getValue().equals(seen.get(e.getKey()))) markDirty(mc, e.getValue().pos(), e.getValue().emission());
        }
        LAST.clear();
        LAST.putAll(seen);
        light = map;
    }

    /** Vanilla-style block light flood: -1 per step (more through leaves/water), opaque blocks stop it. Keeps the max per block. */
    private static void flood(Level level, BlockPos start, int emission, Long2ByteOpenHashMap map) {
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        Long2ByteOpenHashMap mine = new Long2ByteOpenHashMap();
        mine.put(start.asLong(), (byte) emission);
        queue.add(new long[] {start.asLong(), emission});
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            long[] node = queue.poll();
            int value = (int) node[1];
            for (Direction dir : Direction.values()) {
                next.set(node[0]).move(dir);
                int dampening = level.getBlockState(next).getLightDampening();
                if (dampening >= 15) continue;
                int lit = value - Math.max(1, dampening);
                if (lit <= 0) continue;
                long key = next.asLong();
                if (mine.get(key) >= lit) continue;
                mine.put(key, (byte) lit);
                queue.add(new long[] {key, lit});
            }
        }
        mine.long2ByteEntrySet().fastForEach(e -> {
            if (map.get(e.getLongKey()) < e.getByteValue()) map.put(e.getLongKey(), e.getByteValue());
        });
    }

    private static void markDirty(Minecraft mc, BlockPos pos, int radius) {
        mc.levelExtractor.setBlocksDirty(pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius);
    }

    private DynamicLights() {}
}
