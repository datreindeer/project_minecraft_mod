package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.mechanical.Mechanical;
import com.mechanical.dashboard.Landmarks;
import com.mechanical.people.AgeRebuild;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
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
                    StructureStart s = level.getChunk(p.x(), p.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(e.getKey());
                    if (s != null && s.isValid() && s.getBoundingBox().intersects(box)) return "Another village or Ancient Oak stands there";
                }
            }
            if (hasBuildings(chunk, centre(start).getY() - DEEPEST)) return "Buildings (a player's, or a camp or ruin) are in the way near " + chunk.getPos().getMiddleBlockX() + " "
                    + chunk.getPos().getMiddleBlockZ();
        }

        String ground = groundRefusal(level, kind, centre(start));
        if (ground != null) return ground;
        // shortcut: entities of chunks nobody is near may not be read from disk yet; those keep existing, only blocks go
        if (!level.getEntitiesOfClass(Entity.class, AABB.of(box).inflate(0, 16, 0), PlaceStructure::keep).isEmpty())
            return "A player, pet, named animal, vehicle, armor stand or hanging item is in the way";

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

    /** The village fills dips this far down; the oak's roots reach less deep. */
    private static final int DEEPEST = 14;

    /**
     * Anything built, anywhere from {@code floor} to the sky: what {@link Retrogen#playerMade} knows, plus masonry, glass,
     * fields, rails and waystones. Plain logs, rough stone and cobblestone (worldgen leaves it lying about) cannot be told from nature.
     */
    private static boolean hasBuildings(LevelChunk chunk, int floor) {
        Predicate<BlockState> built = s -> Retrogen.playerMade(s) || s.is(BlockTags.STONE_BRICKS) || s.is(BlockTags.RAILS)
                || s.is(Tags.Blocks.GLASS_BLOCKS) || s.is(Tags.Blocks.GLASS_PANES) || s.is(Blocks.BRICKS)
                || s.is(Blocks.COBBLESTONE_STAIRS) || s.is(Blocks.COBBLESTONE_SLAB) || s.is(Blocks.COBBLESTONE_WALL)
                || s.is(Blocks.FARMLAND) || s.is(Waystones.WAYSTONE.get());
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            int y0 = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            if (y0 + 15 < floor || sections[i].hasOnlyAir() || !sections[i].maybeHas(built)) continue;
            for (int y = Math.max(0, floor - y0); y < 16; y++)
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) if (built.test(sections[i].getBlockState(x, y, z))) return true;
        }
        return false;
    }

    /**
     * The structure's height comes from the world generator's noise. Land made by an older generator, or dug and filled by
     * players, can differ from it, so the real blocks are checked at the places the structure itself samples.
     */
    private static String groundRefusal(ServerLevel level, String kind, BlockPos centre) {
        boolean village = kind.equals("tribe_village");
        int reach = village ? 24 : 10;
        List<Integer> heights = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx += village ? 6 : 5)
            for (int dz = -reach; dz <= reach; dz += village ? 6 : 5) {
                if (dx * dx + dz * dz > reach * reach) continue;
                int x = centre.getX() + dx, z = centre.getZ() + dz;
                BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
                while (at.getY() > level.getMinY()) { // past trees and plants down to the ground or the water
                    BlockState s = level.getBlockState(at);
                    if (!s.getFluidState().isEmpty()) return "Can't build there: there is water";
                    if (!(s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced())) break;
                    at.move(Direction.DOWN);
                }
                heights.add(at.getY() + 1);
            }
        heights.sort(null);
        if (heights.getLast() - heights.getFirst() > 8) return "Can't build there: the ground is too uneven";
        if (Math.abs(heights.get(heights.size() / 2) - centre.getY()) > 3)
            return "Can't build there: the land has changed from what the world generator expects";
        return null;
    }

    /** Entities a structure must not be built over. */
    private static boolean keep(Entity e) {
        return e instanceof Player || e instanceof ArmorStand || e instanceof HangingEntity || e instanceof VehicleEntity || e.hasCustomName()
                || e instanceof OwnableEntity pet && pet.getOwnerReference() != null;
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
