package com.mechanical.worldgen;

import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
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
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class GemRegen {
    public static final int MAX_RADIUS = 16;
    /** The chunk has its gemstones (generated with them, retrogenned or regenned). */
    public static final Supplier<AttachmentType<Boolean>> HAS_GEMS = Mechanical.ATTACHMENTS.register("has_gems", () -> AttachmentType
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

    static void markHasGems(LevelChunk chunk) {
        chunk.setData(HAS_GEMS, true);
        chunk.markUnsaved();
    }

    /** Regens the square of chunks around center; returns {chunks given gems, chunks that already had them}. */
    public static int[] regen(ServerLevel level, ChunkPos center, int radius, boolean all) {
        List<String> features = features(level, all);
        int done = 0, had = 0;
        for (int cx = center.x() - radius; cx <= center.x() + radius; cx++)
            for (int cz = center.z() - radius; cz <= center.z() + radius; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk.getData(HAS_GEMS)) {
                    had++;
                    continue;
                }
                LevelChunk[] around = new LevelChunk[9];
                for (int i = 0; i < 9; i++) around[i] = level.getChunk(cx + i % 3 - 1, cz + i / 3 - 1); // features may reach next door
                Retrogen.place(level, chunk, around, features);
                for (LevelChunk c : around) c.markUnsaved(); // ores write sections directly
                markHasGems(chunk);
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
    }

    private static int run(CommandSourceStack source, int radius, boolean all) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("Gemstones only grow in the Overworld"));
            return 0;
        }
        int[] result = regen(level, ChunkPos.containing(net.minecraft.core.BlockPos.containing(source.getPosition())), radius, all);
        source.sendSuccess(() -> Component.literal("Gemstones " + (all ? "(ores, crystals, pebbles)" : "(ores only)") + " added to "
                + result[0] + " chunks; " + result[1] + " already had them"), true);
        return result[0];
    }

    private GemRegen() {}
}
