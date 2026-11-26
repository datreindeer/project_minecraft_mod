package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.mechanical.Mechanical;
import com.mechanical.dashboard.Landmarks;
import com.mechanical.people.AgeRebuild;
import com.mechanical.people.ClimateZone;
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
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /structure tribe_village|ancient_oak &lt;x&gt; &lt;z&gt;: a village is built centred exactly on x, z and the land is
 * shaped to take it (hills cut, dips and water filled, a slope out to the land round it); an oak goes where worldgen would
 * put it in that chunk, and only on dry, fairly level ground. Unlike vanilla's
 * /place it saves the structure start and its references in the chunks, so the game knows it stands there (land title,
 * tribe visits, age rebuilds). Vanilla /locate still does not find it: that only searches worldgen's own placement spots.
 * The biome is not checked; buildings and other villages or oaks are refused.
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
        boolean village = kind.equals("tribe_village");
        StructureStart start = village ? villageStart(level, structure, x, z) : structure.generate(holder, level.dimension(), level.registryAccess(), generator, generator.getBiomeSource(),
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

        String ground = village ? null : groundRefusal(level, centre(start));
        if (ground != null) return ground;
        // shortcut: entities of chunks nobody is near may not be read from disk yet; those keep existing, only blocks go
        if (!level.getEntitiesOfClass(Entity.class, AABB.of(box).inflate(0, 16, 0), PlaceStructure::keep).isEmpty())
            return "A player, pet, named animal, vehicle, armor stand or hanging item is in the way";

        if (village) shape(level, centre(start), ((TribeVillageStructure.Piece) start.getPieces().getFirst()).climate());
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
        if (start.getPieces().getFirst() instanceof TribeVillageStructure.Piece placed) AgeRebuild.found(placed);
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

    /** How far out from the village's clearing the land slopes (one block per block) to meet what was there. */
    private static final int RIM = 12;

    /**
     * A village centred on x, z whatever the land is like: its floor at the middle height of the real ground under it, but
     * never below any water {@link #shape} will touch, so none can run in.
     */
    private static StructureStart villageStart(ServerLevel level, Structure structure, int x, int z) {
        int r = TribeVillageStructure.CLEAR_R + RIM;
        for (int cx = x - r >> 4; cx <= x + r >> 4; cx++)
            for (int cz = z - r >> 4; cz <= z + r >> 4; cz++) level.getChunk(cx, cz); // heights read as the world's floor otherwise
        List<Integer> heights = new ArrayList<>();
        int water = level.getMinY();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                BlockPos top = top(level, x + dx, z + dz);
                if (!level.getFluidState(top.below()).isEmpty()) water = Math.max(water, top.getY());
                if (dx * dx + dz * dz <= TribeVillageStructure.CLEAR_R * TribeVillageStructure.CLEAR_R) heights.add(top.getY());
            }
        heights.sort(null);
        BlockPos centre = new BlockPos(x, Math.max(water, heights.get(heights.size() / 2)), z);
        var piece = new TribeVillageStructure.Piece(level.getRandom().nextLong(), centre, ClimateZone.of(level.getBiome(centre)));
        return new StructureStart(structure, new ChunkPos(x >> 4, z >> 4), 0, new PiecesContainer(List.of(piece)));
    }

    /** The first free block above the ground or the water at x, z, looking past trees and plants. */
    private static BlockPos top(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        while (at.getY() > level.getMinY()) {
            BlockState s = level.getBlockState(at);
            if (!s.getFluidState().isEmpty() || !(s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced())) break;
            at.move(Direction.DOWN);
        }
        return at.above().immutable();
    }

    /**
     * What worldgen's beard_box and the piece's own ground work leave undone on finished land: under the village, hills
     * higher than the piece clears and water or caves deeper than it fills; round it, a slope to the old ground.
     */
    private static void shape(ServerLevel level, BlockPos centre, ClimateZone climate) {
        BlockState soil = (climate == ClimateZone.WARM ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState();
        int core = TribeVillageStructure.CLEAR_R, r = core + RIM, floor = centre.getY();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) continue;
                int x = centre.getX() + dx, z = centre.getZ() + dz;
                int step = Math.max(0, (int) Math.ceil(d - core - 0.5)); // 0 inside the clearing
                BlockPos top = top(level, x, z);
                int g = top.getY();
                if (g > floor + step) { // cut: everything above goes, trees too, and the old surface is laid on the new height
                    BlockState surface = level.getBlockState(top.below());
                    for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y >= floor + step; y--)
                        set(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    if (step > 0) set(level, new BlockPos(x, floor + step - 1, z), surface.getFluidState().isEmpty() ? surface : soil);
                }
                // fill: from the slope's low edge down to solid ground, through water and caves
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, floor - step - 1, z);
                for (int i = 0; i < 64 && p.getY() > level.getMinY(); i++, p.move(Direction.DOWN)) {
                    BlockState s = level.getBlockState(p);
                    if (s.isSolid() && s.getFluidState().isEmpty() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS)) break;
                    set(level, p, i == 0 ? soil : Blocks.DIRT.defaultBlockState());
                }
            }
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /**
     * An oak's height comes from the world generator's noise. Land made by an older generator, or dug and filled by
     * players, can differ from it, so the real blocks are checked at the places the structure itself samples.
     */
    private static String groundRefusal(ServerLevel level, BlockPos centre) {
        List<Integer> heights = new ArrayList<>();
        for (int dx = -10; dx <= 10; dx += 5)
            for (int dz = -10; dz <= 10; dz += 5) {
                if (dx * dx + dz * dz > 100) continue;
                BlockPos top = top(level, centre.getX() + dx, centre.getZ() + dz);
                if (!level.getFluidState(top.below()).isEmpty()) return "Can't build there: there is water";
                heights.add(top.getY());
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
