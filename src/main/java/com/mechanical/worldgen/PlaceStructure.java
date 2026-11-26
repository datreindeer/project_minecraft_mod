package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.dashboard.Landmarks;
import com.mechanical.people.AgeRebuild;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /structure tribe_village|ancient_oak &lt;x&gt; &lt;z&gt;: builds one at the middle of the chunk holding x, z. Unlike vanilla's
 * /place it saves the structure start and its references in the chunks, so the game knows it stands there (land title,
 * tribe visits, age rebuilds). Vanilla /locate still does not find it: that only searches worldgen's own placement spots.
 * The biome is not checked; water, uneven ground, buildings and other villages or oaks are refused. The ground is not levelled as worldgen's beard_box would.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class PlaceStructure {
    static final List<String> KINDS = List.of("tribe_village", "ancient_oak");

    private static Identifier id(String kind) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, kind);
    }

    /** Builds the structure; null when it stands, otherwise why not (and then no block has changed). */
    public static String place(ServerLevel level, String kind, int x, int z) {
        if (level.dimension() != Level.OVERWORLD) return "Structures can only be placed in the Overworld";
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = registry.get(id(kind)).orElseThrow();
        Structure structure = holder.value();
        var source = level.getChunkSource();
        var generator = source.getGenerator();
        var randomState = source.randomState();
        ChunkPos at = new ChunkPos(x >> 4, z >> 4);
        StructureStart start = structure.generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(),
                randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED), randomState, level.getStructureTemplateManager(), level.getSeed(),
                at, 0, level, b -> true);
        if (!start.isValid()) return "Can't build there: water, or the ground is too uneven";

        BoundingBox box = start.getBoundingBox();
        List<LevelChunk> chunks = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) chunks.add(level.getChunk(cx, cz)); // generates what is missing
        for (LevelChunk chunk : chunks) {
            for (var e : chunk.getAllReferences().entrySet()) {
                Holder<Structure> other = registry.wrapAsHolder(e.getKey());
                if (!other.is(StructureTags.VILLAGE) && KINDS.stream().noneMatch(k -> other.is(id(k)))) continue;
                for (long ref : e.getValue()) {
                    ChunkPos p = ChunkPos.unpack(ref);
                    StructureStart s = level.getChunk(p.x(), p.z()).getStartForStructure(e.getKey());
                    if (s != null && s.isValid() && s.getBoundingBox().intersects(box)) return "Another village or Ancient Oak stands there";
                }
            }
            if (!Retrogen.noPlayerBlocks(chunk)) return "Buildings (a player's, or a camp or ruin) are in the way near " + chunk.getPos().getMiddleBlockX() + " "
                    + chunk.getPos().getMiddleBlockZ();
        }

        for (LevelChunk chunk : chunks) {
            ChunkPos p = chunk.getPos();
            start.placeInChunk(level, level.structureManager(), generator, level.getRandom(), new BoundingBox(p.getMinBlockX(), level.getMinY(),
                    p.getMinBlockZ(), p.getMaxBlockX(), level.getMaxY(), p.getMaxBlockZ()), p);
        }
        level.getChunk(at.x(), at.z()).setStartForStructure(structure, start);
        for (LevelChunk chunk : chunks) {
            chunk.addReferenceForStructure(structure, at.pack());
            chunk.markUnsaved();
        }
        if (start.getPieces().getFirst() instanceof TribeVillageStructure.Piece village) AgeRebuild.found(village);
        BlockPos c = centre(start);
        Landmarks.add(level, kind, c.getX(), c.getZ());
        return null;
    }

    static BlockPos centre(StructureStart start) {
        return start.getPieces().getFirst() instanceof TribeVillageStructure.Piece village ? village.centre()
                : ((AncientOakStructure.Piece) start.getPieces().getFirst()).foot();
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("structure").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (String kind : KINDS)
            root.then(Commands.literal(kind).then(Commands.argument("x", IntegerArgumentType.integer(-29999000, 29999000))
                    .then(Commands.argument("z", IntegerArgumentType.integer(-29999000, 29999000))
                            .executes(ctx -> run(ctx.getSource(), kind, IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "z"))))));
        event.getDispatcher().register(root);
    }

    private static int run(CommandSourceStack source, String kind, int x, int z) {
        ServerLevel level = source.getLevel();
        String refusal = place(level, kind, x, z);
        if (refusal != null) {
            source.sendFailure(Component.literal(refusal));
            return 0;
        }
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(id(kind));
        BlockPos c = centre(level.getChunk(x >> 4, z >> 4).getStartForStructure(structure));
        source.sendSuccess(() -> Component.literal("Built " + kind + " at " + c.getX() + " " + c.getY() + " " + c.getZ()), true);
        return 1;
    }

    private PlaceStructure() {}
}
