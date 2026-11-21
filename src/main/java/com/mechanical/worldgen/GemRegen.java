package com.mechanical.worldgen;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /regengems &lt;radius&gt; [ores]: puts the gemstone features (scripts/gen_gemstones.py) into chunks generated before gemstones
 * existed, including land players have lived on, which {@link Retrogen} leaves alone. Each chunk is placed with worldgen's own
 * rules (seeded per chunk) and marked {@link #HAS_GEMS}, as are newly generated chunks and chunks retrogen updates, so no chunk
 * ever gets them twice. "ores" places only the ore veins (they replace natural stone only), not the cave crystals and pebbles,
 * which can also land in player-dug tunnels. Overworld only; loads the chunks it needs, so the radius is capped.
 * <p>
 * /regenstones &lt;radius&gt;: the same for the newer stones and ores ({@link #stoneFeatures}: every mechanical ore_* vein that
 * only replaces natural stone and isn't retrogenned, e.g. marble, slate, galena). Marked {@link #HAS_STONES}; a chunk generated
 * before that marker existed but already holding one of those blocks counts as having them.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class GemRegen {
    public static final int MAX_RADIUS = 16;
    /** The chunk has its gemstones (generated with them, retrogenned or regenned). */
    public static final Supplier<AttachmentType<Boolean>> HAS_GEMS = Mechanical.ATTACHMENTS.register("has_gems", () -> AttachmentType
            .builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("value"))
            .build());
    /** The chunk has the stone veins of {@link #stoneFeatures} (generated with them or regenned). */
    public static final Supplier<AttachmentType<Boolean>> HAS_STONES = Mechanical.ATTACHMENTS.register("has_stones", () -> AttachmentType
            .builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("value"))
            .build());

    public static void init() {}

    public static List<String> features(ServerLevel level, boolean all) {
        var registry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        return Retrogen.FEATURES.stream().filter(f -> f.startsWith("gem_")).filter(f -> all || registry
                .getOptional(Identifier.fromNamespaceAndPath(Mechanical.MODID, f))
                .map(p -> p.feature().value() instanceof OreFeature).orElse(false)).toList();
    }

    /** Ore veins (mechanical:ore_*) not in {@link Retrogen#FEATURES} whose every target takes stone but not dirt: they never touch builds. */
    public static List<String> stoneFeatures(ServerLevel level) {
        RandomSource random = RandomSource.create(0);
        return level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).listElements()
                .filter(h -> h.key().identifier().getNamespace().equals(Mechanical.MODID))
                .filter(h -> h.key().identifier().getPath().startsWith("ore_") && !Retrogen.FEATURES.contains(h.key().identifier().getPath()))
                .filter(h -> h.value().feature().value() instanceof AbstractOreFeature ore && ore.targetStates().stream().allMatch(r ->
                        r.target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random)
                                && !r.target().test(Blocks.DIRT.defaultBlockState(), BlockPos.ZERO, random)))
                .map(h -> h.key().identifier().getPath()).sorted().toList();
    }

    /** What the stone veins place, to tell a chunk that already has them. */
    static Predicate<BlockState> stoneBlocks(ServerLevel level, List<String> features) {
        var registry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        var blocks = features.stream().map(f -> registry.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, f)))
                .filter(p -> p != null && p.feature().value() instanceof AbstractOreFeature)
                .flatMap(p -> ((AbstractOreFeature) p.feature().value()).targetStates().stream().map(r -> r.state().getBlock()))
                .collect(java.util.stream.Collectors.toSet());
        return s -> blocks.contains(s.getBlock());
    }

    static void markHasGems(LevelChunk chunk) {
        chunk.setData(HAS_GEMS, true);
        chunk.markUnsaved();
    }

    static void markHasStones(LevelChunk chunk) {
        chunk.setData(HAS_STONES, true);
        chunk.markUnsaved();
    }

    /** Regens the square of chunks around center; returns {chunks given gems, chunks that already had them}. */
    public static int[] regen(ServerLevel level, ChunkPos center, int radius, boolean all) {
        return regen(level, center, radius, features(level, all), HAS_GEMS.get(), c -> false);
    }

    /** As {@link #regen} for {@link #stoneFeatures}. */
    public static int[] regenStones(ServerLevel level, ChunkPos center, int radius) {
        List<String> features = stoneFeatures(level);
        Predicate<BlockState> stone = stoneBlocks(level, features);
        return regen(level, center, radius, features, HAS_STONES.get(), c -> {
            for (LevelChunkSection section : c.getSections()) if (section.maybeHas(stone) && has(section, stone)) return true;
            return false;
        });
    }

    private static boolean has(LevelChunkSection section, Predicate<BlockState> stone) {
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) if (stone.test(section.getBlockState(x, y, z))) return true;
        return false;
    }

    private static int[] regen(ServerLevel level, ChunkPos center, int radius, List<String> features, AttachmentType<Boolean> marker,
                               Predicate<LevelChunk> alreadyHas) {
        int done = 0, had = 0;
        for (int cx = center.x() - radius; cx <= center.x() + radius; cx++)
            for (int cz = center.z() - radius; cz <= center.z() + radius; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk.getData(marker) || alreadyHas.test(chunk)) {
                    chunk.setData(marker, true);
                    chunk.markUnsaved();
                    had++;
                    continue;
                }
                LevelChunk[] around = new LevelChunk[9];
                for (int i = 0; i < 9; i++) around[i] = level.getChunk(cx + i % 3 - 1, cz + i / 3 - 1); // features may reach next door
                Retrogen.place(level, chunk, around, features);
                for (LevelChunk c : around) c.markUnsaved(); // ores write sections directly
                chunk.setData(marker, true);
                done++;
            }
        return new int[] {done, had};
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regengems").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"), true))
                        .then(Commands.literal("ores").executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"), false)))));
        event.getDispatcher().register(Commands.literal("regenstones").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                        .executes(ctx -> runStones(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))));
    }

    private static int run(CommandSourceStack source, int radius, boolean all) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("Gemstones only grow in the Overworld"));
            return 0;
        }
        int[] result = regen(level, ChunkPos.containing(BlockPos.containing(source.getPosition())), radius, all);
        source.sendSuccess(() -> Component.literal("Gemstones " + (all ? "(ores, crystals, pebbles)" : "(ores only)") + " added to "
                + result[0] + " chunks; " + result[1] + " already had them"), true);
        return result[0];
    }

    private static int runStones(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("These stones only lie in the Overworld"));
            return 0;
        }
        int[] result = regenStones(level, ChunkPos.containing(BlockPos.containing(source.getPosition())), radius);
        source.sendSuccess(() -> Component.literal("Stones (" + stoneFeatures(level).size() + " veins) added to " + result[0]
                + " chunks; " + result[1] + " already had them"), true);
        return result[0];
    }

    private GemRegen() {}
}
