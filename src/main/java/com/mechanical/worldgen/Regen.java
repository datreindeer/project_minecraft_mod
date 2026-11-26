package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /regen &lt;radius&gt;: puts the newer bushes, vines, maize and orchard trees ({@link #features}) and any waystone end into chunks
 * around you that were generated before they existed, including land players have walked on (which {@link Retrogen} leaves
 * alone, and Retrogen never plants trees). A chunk whose 3x3 holds player-made blocks is left out, so nothing grows into a
 * build. Each chunk remembers which of these it has ({@link #DONE}; newly generated chunks have all), so running it twice
 * adds nothing.
 * <p>
 * /regen waystones &lt;blocks&gt;: builds every waystone end within that many blocks (loading or generating its chunk).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Regen {
    public static final int MAX_RADIUS = 16, MAX_WAYSTONE_BLOCKS = 20000;
    /** Flora 4 features (also in {@link Retrogen#FEATURES}, so a retrogenned chunk already has them). */
    static final List<String> FLORA = List.of("white_currant_thicket", "gooseberry_thicket", "cranberry_thicket", "grape_vine", "wild_maize_patch");
    /** The orchard's trees: every placed feature mechanical:&lt;tree&gt;_* except the sapling's *_checked. */
    static final List<String> TREES = List.of("pear", "sweet_cherry", "lemon", "clementine", "walnut", "chestnut", "quince", "mulberry",
            "medlar", "peach", "apricot", "almond", "pomegranate");

    /** The features of {@link #features} this chunk has. */
    public static final Supplier<AttachmentType<List<String>>> DONE = Mechanical.ATTACHMENTS.register("regen_done", () -> AttachmentType
            .<List<String>>builder(() -> List.of())
            .serialize(Codec.STRING.listOf().fieldOf("value"))
            .build());

    public static void init() {}

    private static List<String> cached;
    private static Object cachedFor;

    static List<String> features(ServerLevel level) {
        var registry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        if (cachedFor == registry) return cached;
        List<String> out = new ArrayList<>(FLORA);
        registry.listElements()
                .map(h -> h.key().identifier())
                .filter(id -> id.getNamespace().equals(Mechanical.MODID) && !id.getPath().endsWith("_checked"))
                .map(id -> id.getPath())
                .filter(p -> TREES.stream().anyMatch(t -> p.startsWith(t + "_")))
                .sorted().forEach(out::add);
        cached = List.copyOf(out);
        cachedFor = registry;
        return cached;
    }

    /** A chunk generated now has every feature already. */
    static void markNew(ServerLevel level, LevelChunk chunk) {
        chunk.setData(DONE, features(level));
        chunk.markUnsaved();
    }

    /** Retrogen just placed the flora (not the trees) in this chunk. */
    static void markRetrogenned(LevelChunk chunk) {
        List<String> have = new ArrayList<>(chunk.getData(DONE));
        for (String f : FLORA) if (!have.contains(f)) have.add(f);
        chunk.setData(DONE, have);
    }

    /** Regens the square of chunks round center; returns {chunks given something, chunks that had it all, chunks skipped as builds, waystones built}. */
    public static int[] regen(ServerLevel level, ChunkPos center, int radius) {
        List<String> all = features(level);
        int done = 0, had = 0, built = 0, stones = 0;
        for (int cx = center.x() - radius; cx <= center.x() + radius; cx++)
            for (int cz = center.z() - radius; cz <= center.z() + radius; cz++) {
                if (waystone(level, cx, cz)) stones++;
                LevelChunk chunk = level.getChunk(cx, cz);
                List<String> have = chunk.getData(DONE);
                List<String> missing = all.stream().filter(f -> !have.contains(f)).toList();
                if (missing.isEmpty()) {
                    had++;
                    continue;
                }
                LevelChunk[] around = new LevelChunk[9];
                for (int i = 0; i < 9; i++) around[i] = level.getChunk(cx + i % 3 - 1, cz + i / 3 - 1); // features reach next door
                boolean builds = false;
                for (LevelChunk c : around) builds |= !Retrogen.noPlayerBlocks(c);
                if (builds) { // not marked: a later run may find the build gone
                    built++;
                    continue;
                }
                Retrogen.place(level, chunk, around, missing);
                for (LevelChunk c : around) c.markUnsaved();
                chunk.setData(DONE, all);
                done++;
            }
        return new int[] {done, had, built, stones};
    }

    /** Builds the waystone end standing in chunk (cx, cz), if one belongs there and is missing. */
    static boolean waystone(ServerLevel level, int cx, int cz) {
        int[] other = Waystones.partner(level, cx, cz);
        if (other == null) return false;
        level.getChunk(cx, cz); // a chunk generated now builds its own stone
        int x = cx * 16 + 8, z = cz * 16 + 8;
        boolean missing = Waystones.findStone(level, x, z) == null;
        if (missing) Waystones.build(level, x, z, Waystones.toward(cx, cz, other[0], other[1]), level.getRandom());
        com.mechanical.dashboard.Landmarks.add(level, "waystone", x, z); // standing ones too: this marks stones older than the map's markers
        return missing;
    }

    /** Builds every waystone end within blocks of center; returns {ends built, ends already standing}. */
    static int[] waystones(ServerLevel level, BlockPos center, int blocks) {
        int built = 0, stood = 0;
        int r = Math.floorDiv(blocks, Waystones.REGION) + 1;
        int rx0 = Math.floorDiv(center.getX(), Waystones.REGION), rz0 = Math.floorDiv(center.getZ(), Waystones.REGION);
        for (int rx = rx0 - r; rx <= rx0 + r; rx++)
            for (int rz = rz0 - r; rz <= rz0 + r; rz++) {
                int[] p = Waystones.pair(level, rx, rz);
                for (int e = 0; e + 1 < p.length; e += 2) {
                    if (Math.hypot(p[e] * 16 + 8 - center.getX(), p[e + 1] * 16 + 8 - center.getZ()) > blocks) continue;
                    if (waystone(level, p[e], p[e + 1])) built++;
                    else stood++;
                }
            }
        return new int[] {built, stood};
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                        .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))
                .then(Commands.literal("waystones").then(Commands.argument("blocks", IntegerArgumentType.integer(0, MAX_WAYSTONE_BLOCKS))
                        .executes(ctx -> runWaystones(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "blocks"))))));
    }

    private static ServerLevel overworld(CommandSourceStack source) {
        if (source.getLevel().dimension() == Level.OVERWORLD) return source.getLevel();
        source.sendFailure(Component.literal("Regen only works in the Overworld"));
        return null;
    }

    private static int run(CommandSourceStack source, int radius) {
        ServerLevel level = overworld(source);
        if (level == null) return 0;
        int[] r = regen(level, ChunkPos.containing(BlockPos.containing(source.getPosition())), radius);
        source.sendSuccess(() -> Component.literal("New bushes, vines, maize and orchard trees added to " + r[0] + " chunks; " + r[1]
                + " already had them; " + r[2] + " left alone (player builds nearby); " + r[3] + " waystones built"), true);
        return r[0];
    }

    private static int runWaystones(CommandSourceStack source, int blocks) {
        ServerLevel level = overworld(source);
        if (level == null) return 0;
        int[] r = waystones(level, BlockPos.containing(source.getPosition()), blocks);
        source.sendSuccess(() -> Component.literal("Waystones within " + blocks + " blocks: " + r[0] + " built, " + r[1] + " already standing"), true);
        return r[0];
    }

    private Regen() {}
}
