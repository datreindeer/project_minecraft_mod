package com.mechanical.wildlife.temperate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Leeches in the fen: wading in the water of a swamp, fen, bog or reed delta ({@link #BIOMES}), a leech now and then
 * latches onto your leg ({@link #LATCH_CHANCE} every 5 s, up to {@link #MAX}). Each one drinks a little blood
 * ({@link #BITE} every 5 s, never below {@link #FLOOR} health) and drops off sated after {@link #FEED_CYCLES} cycles,
 * landing at your feet as a Leech: an old medicine (it draws out poison: TemperateFauna.LEECH). Leaving the water does
 * not shake them off. Not saved: they are gone after a relog.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Leeches {
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Mechanical.MODID, "has_leeches"));
    static final int MAX = 3, FEED_CYCLES = 10, INTERVAL = 100;
    static final float LATCH_CHANCE = 0.12F, BITE = 0.5F, FLOOR = 4.0F;
    /** Player -> cycles each latched leech has fed. */
    private static final Map<UUID, int[]> LATCHED = new ConcurrentHashMap<>();

    private Leeches() {}

    public static int count(LivingEntity player) {
        int[] l = LATCHED.get(player.getUUID());
        return l == null ? 0 : l.length;
    }

    /** Wading, not riding, in leech water. */
    static boolean wading(ServerPlayer player) {
        return player.isInWater() && !player.isPassenger() && player.level().getBiome(player.blockPosition()).is(BIOMES);
    }

    /** Puts a leech on (tests and the wading roll). */
    static void latch(LivingEntity player) {
        int[] old = LATCHED.getOrDefault(player.getUUID(), new int[0]);
        if (old.length >= MAX) return;
        int[] now = java.util.Arrays.copyOf(old, old.length + 1);
        LATCHED.put(player.getUUID(), now);
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("message.mechanical.leech.latched"));
    }

    /** One feeding cycle: a little blood from each leech; the sated ones drop off as items. */
    static void feed(LivingEntity player) {
        int[] l = LATCHED.get(player.getUUID());
        if (l == null || !(player.level() instanceof ServerLevel level)) return;
        int kept = 0;
        for (int i = 0; i < l.length; i++) {
            // blood drunk, not a blow: health simply goes down (no knockback, no armour), never below the floor
            if (player.getHealth() > FLOOR) player.setHealth(Math.max(FLOOR, player.getHealth() - BITE));
            if (++l[i] >= FEED_CYCLES) {
                ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.2, player.getZ(), new ItemStack(TemperateFauna.LEECH.get()));
                item.setPickUpDelay(10);
                level.addFreshEntity(item);
                level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.6F, 1.4F);
                if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("message.mechanical.leech.dropped"));
            } else {
                l[kept++] = l[i];
            }
        }
        if (kept == 0) LATCHED.remove(player.getUUID());
        else LATCHED.put(player.getUUID(), java.util.Arrays.copyOf(l, kept));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 25 || level.dimension() != Level.OVERWORLD) return;
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                LATCHED.remove(player.getUUID());
                continue;
            }
            feed(player);
            if (wading(player) && level.getRandom().nextFloat() < LATCH_CHANCE) latch(player);
        }
    }
}
