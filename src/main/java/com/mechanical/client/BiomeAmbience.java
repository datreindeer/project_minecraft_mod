package com.mechanical.client;

import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.people.Tribesperson;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;

/**
 * Environment audio, client-side only: every few seconds a quiet sound fitting where you stand plays somewhere
 * around you. Birdsong and rustling leaves in forests, wind in pines and on peaks, frogs in fens, crickets at night,
 * waves at the coast, drips in caves. Built from vanilla sound events since the mod ships no audio files.
 * Music per biome is the biome's own minecraft:audio/background_music attribute.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BiomeAmbience {
    private record Sound(SoundEvent event, float volume, float pitchMin, float pitchMax, int repeats) {}
    private record Layer(TagKey<Biome> tag, List<Sound> day, List<Sound> night) {}

    private static Sound s(String id, float volume, float pitchMin, float pitchMax, int repeats) {
        return new Sound(SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(id)), volume, pitchMin, pitchMax, repeats);
    }

    private static final Sound BIRDS = s("entity.parrot.ambient", 0.18F, 1.5F, 1.9F, 3);
    private static final Sound LEAVES = s("block.poplar_leaves.ambient", 0.5F, 0.9F, 1.1F, 1);
    private static final Sound CRICKETS = s("block.firefly_bush.idle", 0.35F, 0.9F, 1.2F, 1);
    private static final Sound GRASS_WIND = s("block.dry_grass.ambient", 0.4F, 0.8F, 1.0F, 1);
    private static final Sound WIND = s("entity.breeze.idle_air", 0.12F, 0.5F, 0.65F, 1);
    private static final Sound FROGS = s("entity.frog.ambient", 0.35F, 0.8F, 1.1F, 2);
    private static final Sound WATER = s("block.water.ambient", 0.35F, 0.6F, 0.8F, 1);
    private static final Sound SAND = s("block.sand.idle", 0.4F, 0.9F, 1.1F, 1);
    private static final Sound DEAD_BUSH = s("block.deadbush.idle", 0.4F, 0.9F, 1.1F, 1);
    private static final Sound DRIP = s("block.pointed_dripstone.drip_water", 0.5F, 0.8F, 1.2F, 1);
    private static final Sound DOLPHIN = s("entity.dolphin.ambient", 0.1F, 0.6F, 0.8F, 1);

    // First matching layer wins, so the narrow tags come before the broad ones.
    private static final List<Layer> LAYERS = List.of(
            new Layer(Tags.Biomes.IS_SWAMP, List.of(FROGS, WATER, BIRDS), List.of(FROGS, FROGS, CRICKETS)),
            new Layer(Tags.Biomes.IS_DESERT, List.of(SAND, DEAD_BUSH, WIND), List.of(SAND, WIND)),
            new Layer(Tags.Biomes.IS_BADLANDS, List.of(DEAD_BUSH, WIND), List.of(DEAD_BUSH, WIND)),
            new Layer(Tags.Biomes.IS_SNOWY, List.of(WIND), List.of(WIND)),
            new Layer(Tags.Biomes.IS_MOUNTAIN, List.of(WIND, GRASS_WIND), List.of(WIND)),
            new Layer(BiomeTags.IS_BEACH, List.of(WATER, WATER, WIND), List.of(WATER, WATER, CRICKETS)),
            new Layer(BiomeTags.IS_OCEAN, List.of(WATER, WIND, DOLPHIN), List.of(WATER, WIND)),
            new Layer(BiomeTags.IS_RIVER, List.of(WATER, BIRDS, FROGS), List.of(WATER, CRICKETS, FROGS)),
            new Layer(Tags.Biomes.IS_CONIFEROUS_TREE, List.of(WIND, LEAVES, BIRDS), List.of(WIND, CRICKETS)),
            new Layer(BiomeTags.IS_FOREST, List.of(BIRDS, BIRDS, LEAVES), List.of(CRICKETS, LEAVES)),
            new Layer(Tags.Biomes.IS_PLAINS, List.of(GRASS_WIND, BIRDS), List.of(CRICKETS, CRICKETS, GRASS_WIND)),
            new Layer(Tags.Biomes.IS_OVERWORLD, List.of(GRASS_WIND, BIRDS), List.of(CRICKETS)));
    private static final List<Sound> CAVE = List.of(DRIP, DRIP, WIND);

    private static final int MIN_GAP = 60, MAX_GAP = 160; // 3-8 s between sounds
    private static int ticks, next = MIN_GAP;

    private static final SoundEvent CHATTER = SoundEvents.VILLAGER_AMBIENT, DRUM = SoundEvents.NOTE_BLOCK_BASEDRUM.value();
    private static int campNext = 200;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null || mc.isPaused()) return;
        if (++ticks >= campNext) camp(mc, player);
        if (ticks < next) return;
        RandomSource random = player.getRandom();
        next = ticks + MIN_GAP + random.nextInt(MAX_GAP - MIN_GAP);
        if (!mc.level.dimensionType().hasSkyLight() || player.isUnderWater()) return;
        BlockPos at = player.blockPosition();
        List<Sound> pool = pool(mc, at);
        if (pool == null) return;
        Sound sound = pool.get(random.nextInt(pool.size()));
        double angle = random.nextDouble() * Math.PI * 2, dist = 6 + random.nextDouble() * 10;
        Vec3 from = player.position().add(Math.cos(angle) * dist, 1 + random.nextDouble() * 4, Math.sin(angle) * dist);
        for (int i = 0; i < sound.repeats(); i++) {
            float pitch = sound.pitchMin() + random.nextFloat() * (sound.pitchMax() - sound.pitchMin());
            NightAmbience.schedule(i * (4 + random.nextInt(5)), from, sound.event(), sound.volume(), pitch);
        }
    }

    /**
     * Camp life near a tribe: in daylight two people talking back and forth, in the dark (while they're still awake
     * round the fire) a slow drum. Their own work sounds come from the goals, and each mumbles on its own.
     */
    private static void camp(Minecraft mc, LocalPlayer player) {
        RandomSource random = player.getRandom();
        campNext = ticks + 300 + random.nextInt(500); // 15-40 s
        List<Tribesperson> awake = mc.level.getEntitiesOfClass(Tribesperson.class, player.getBoundingBox().inflate(40), p -> !p.isSleeping());
        if (awake.size() < 3) return;
        Tribesperson a = awake.get(random.nextInt(awake.size())), b = awake.get(random.nextInt(awake.size()));
        if (!mc.level.isDarkOutside()) {
            for (int i = 0; i < 4; i++) {
                Tribesperson who = i % 2 == 0 ? a : b;
                NightAmbience.schedule(i * (25 + random.nextInt(15)), who.position().add(0, 1.6, 0), CHATTER, 0.45F, 0.65F + random.nextFloat() * 0.25F);
            }
            return;
        }
        Vec3 fire = a.position();
        for (int beat = 0; beat < 16; beat++) { // two bars of four, accent on the first beat
            boolean accent = beat % 4 == 0;
            NightAmbience.schedule(beat * 10, fire, DRUM, accent ? 0.6F : 0.35F, accent ? 0.5F : 0.6F);
        }
    }

    /** What plays here, or null for silence (indoors, or rain drowning it out). */
    private static List<Sound> pool(Minecraft mc, BlockPos at) {
        if (!mc.level.canSeeSky(at)) return at.getY() < mc.level.getSeaLevel() - 8 ? CAVE : null;
        if (mc.level.isRaining()) return null;
        Holder<Biome> biome = mc.level.getBiome(at);
        boolean night = mc.level.isDarkOutside();
        for (Layer layer : LAYERS) {
            if (biome.is(layer.tag())) return night ? layer.night() : layer.day();
        }
        return null;
    }
}
