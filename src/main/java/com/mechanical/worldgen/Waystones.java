package com.mechanical.worldgen;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mechanical.FirePit;
import com.mechanical.Mechanical;
import com.mojang.serialization.MapCodec;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Waystones: carved standing stones in linked pairs, 1000 to 10000 blocks apart. Right-click one and you stand beside the
 * other. Pairs come from the world seed alone (one try per {@link #REGION}-block region, both ends on land), so either end
 * knows where the other is before that chunk exists. Each end stands at a chunk centre and is built by the feature when its
 * chunk generates; a far end in a chunk that was generated before (an old world) is built when someone first travels to it.
 * The stone takes its look from the biome it stands in. Data, models, advancements: scripts/gen_waystone.py;
 * textures: scripts/gen_waystone_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Waystones {
    /** One pair per region of this many blocks: roughly one stone per 2900 x 2900 blocks. */
    public static final int REGION = 4096, MIN = 1000, MAX = 10000;
    static final int REGION_CHUNKS = REGION / 16, TRIES = 12, COOLDOWN_TICKS = 100;
    /** Regions round a chunk that can hold an end in it: the partner is at most MAX away from an anchor anywhere in its region. */
    static final int REACH = MAX / REGION + 1;

    public enum Look implements StringRepresentable {
        MOSSY(0x7CFFB0, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.MOSSY_COBBLESTONE),
        SAND(0xFFC860, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SANDSTONE),
        FROST(0x9EE8FF, Blocks.CALCITE, Blocks.DIORITE_SLAB, Blocks.STONE),
        RED(0xFF8A4A, Blocks.CUT_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE),
        DARK(0xC08CFF, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.BLACKSTONE),
        CHALK(0x8FB8FF, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE_SLAB, Blocks.STONE),
        GREY(0x7FE0E0, Blocks.STONE_BRICKS, Blocks.STONE_BRICK_SLAB, Blocks.COBBLESTONE);

        public final int glow;
        final Block dais, edge, fill;

        Look(int glow, Block dais, Block edge, Block fill) {
            this.glow = glow;
            this.dais = dais;
            this.edge = edge;
            this.fill = fill;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Look> LOOK = EnumProperty.create("look", Look.class);
    /** Three blocks tall: footing and lower runes, middle runes, the tapering head. */
    public enum Part implements StringRepresentable {
        LOWER, MIDDLE, UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** Unbreakable like bedrock: no hardness, blast proof, pistons cannot move it, no drop. */
    public static final DeferredBlock<WaystoneBlock> WAYSTONE = Mechanical.BLOCKS.registerBlock("waystone", WaystoneBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.IMMOVEABLE)
                    .sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> s.getValue(PART) == Part.LOWER ? 3 : 6).noOcclusion()
                    .isValidSpawn((s, l, pos, e) -> false));

    /** Pair of ends per (seed, region), in chunk coordinates {ax, az, bx, bz}; empty when the region has none. */
    private static final Map<Key, int[]> PAIRS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> USED = new ConcurrentHashMap<>();

    private record Key(long seed, int rx, int rz) {}

    static {
        Structures.FEATURE_TYPES.register("waystone", () -> Site.CODEC);
    }

    public static void init() {}

    // ------------------------------------------------------------------ pairing

    /** The pair of the region (chunk coordinates {ax, az, bx, bz}), or an empty array when no try put both ends on land. */
    static int[] pair(long seed, ChunkGenerator generator, RandomState randomState, int rx, int rz) {
        return PAIRS.computeIfAbsent(new Key(seed, rx, rz), k -> {
            RandomSource r = RandomSource.create(seed ^ rx * 341873128712L ^ rz * 132897987541L ^ 0x57415953544F4E45L);
            BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(randomState);
            int qy = QuartPos.fromBlock(generator.getSeaLevel());
            for (int i = 0; i < TRIES; i++) {
                int ax = rx * REGION_CHUNKS + 8 + r.nextInt(REGION_CHUNKS - 16), az = rz * REGION_CHUNKS + 8 + r.nextInt(REGION_CHUNKS - 16);
                double angle = r.nextDouble() * Math.PI * 2, d = MIN + 32 + r.nextDouble() * (MAX - MIN - 64);
                int bx = Math.floorDiv(ax * 16 + 8 + (int) Math.round(Math.cos(angle) * d), 16);
                int bz = Math.floorDiv(az * 16 + 8 + (int) Math.round(Math.sin(angle) * d), 16);
                double dist = 16 * Math.hypot(bx - ax, bz - az);
                if (dist < MIN || dist > MAX) continue;
                if (land(biomes, ax, az, qy) && land(biomes, bx, bz, qy)) return new int[] {ax, az, bx, bz};
            }
            return new int[0];
        });
    }

    private static boolean land(BiomeResolver biomes, int cx, int cz, int qy) {
        Holder<Biome> b = biomes.getNoiseBiome(cx * 4 + 2, qy, cz * 4 + 2);
        return !b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_DEEP_OCEAN) && !b.is(BiomeTags.IS_RIVER);
    }

    /** The chunk {x, z} of the other end of a waystone standing in chunk (cx, cz), or null if no end is there. */
    static int[] partner(long seed, ChunkGenerator generator, RandomState randomState, int cx, int cz) {
        int rx0 = Math.floorDiv(cx, REGION_CHUNKS), rz0 = Math.floorDiv(cz, REGION_CHUNKS);
        for (int rx = rx0 - REACH; rx <= rx0 + REACH; rx++) {
            for (int rz = rz0 - REACH; rz <= rz0 + REACH; rz++) {
                int[] p = pair(seed, generator, randomState, rx, rz);
                if (p.length == 0) continue;
                if (p[0] == cx && p[1] == cz) return new int[] {p[2], p[3]};
                if (p[2] == cx && p[3] == cz) return new int[] {p[0], p[1]};
            }
        }
        return null;
    }

    public static int[] partner(ServerLevel level, int cx, int cz) {
        var source = level.getChunkSource();
        return partner(level.getSeed(), source.getGenerator(), source.randomState(), cx, cz);
    }

    public static int[] pair(ServerLevel level, int rx, int rz) {
        var source = level.getChunkSource();
        return pair(level.getSeed(), source.getGenerator(), source.randomState(), rx, rz);
    }

    // ------------------------------------------------------------------ the site

    /** Which stone suits the biome. */
    static Look look(Holder<Biome> h) {
        String p = h.unwrapKey().map(k -> k.identifier().getPath()).orElse("");
        if (h.value().getBaseTemperature() < 0.15F || p.contains("frozen") || p.contains("snow") || p.contains("ice")) return Look.FROST;
        if (h.is(BiomeTags.IS_BADLANDS) || h.is(BiomeTags.IS_SAVANNA) || p.contains("badlands") || p.contains("savanna")) return Look.RED;
        if (p.equals("scorched_barrens") || p.contains("dark_forest") || p.contains("pale_garden") || p.contains("basalt") || p.contains("slate"))
            return Look.DARK;
        if (p.contains("desert") || p.equals("beach") || p.contains("gypsum")) return Look.SAND;
        if (p.contains("chalk") || p.contains("limestone") || p.contains("marble") || p.contains("plains") || p.contains("meadow") || p.contains("cherry"))
            return Look.CHALK;
        if (h.is(BiomeTags.IS_FOREST) || h.is(BiomeTags.IS_TAIGA) || h.is(BiomeTags.IS_JUNGLE) || p.contains("swamp") || p.contains("fen")
                || p.contains("bog") || p.contains("delta") || p.contains("soapstone") || p.contains("heath")) return Look.MOSSY;
        return Look.GREY;
    }

    /** First free block above the real ground at x, z: past leaves, logs and plants (water counts as free). */
    static BlockPos ground(WorldGenLevel level, int x, int z) {
        BlockPos p = StoneAgeFeature.surface(level, x, z);
        for (int i = 0; i < 48 && p.getY() > level.getMinY() + 1; i++) {
            BlockState below = level.getBlockState(p.below());
            if (!(below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS) || below.canBeReplaced() || below.isAir()) || !below.getFluidState().isEmpty()) break;
            p = p.below();
        }
        return p;
    }

    /**
     * Builds a waystone on a little dais at column x, z: a 3x3 of dressed stone one step up, a ring of slabs round it, all
     * founded down to solid ground (or up out of water), the room above cleared, a few loose rocks about. Returns the
     * lower half's position.
     */
    public static BlockPos build(WorldGenLevel level, int x, int z, Direction face, RandomSource random) {
        BlockPos g = ground(level, x, z);
        int y = g.getY();
        while (!level.getFluidState(new BlockPos(x, y, z)).isEmpty() && y < g.getY() + 16) y++;
        BlockPos c = new BlockPos(x, y, z);
        Look look = look(level.getBiome(c));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 5) continue;
                BlockPos p = c.offset(dx, 0, dz);
                for (int h = 1; h <= 5; h++) if (!level.getBlockState(p.above(h)).isAir()) StoneAgeFeature.set(level, p.above(h), StoneAgeFeature.AIR);
                BlockState top = d2 <= 2 ? daisBlock(look, random) : look.edge.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                StoneAgeFeature.set(level, p, top);
                BlockPos q = p.below();
                for (int i = 0; i < 24 && q.getY() > level.getMinY() && (!level.getBlockState(q).isSolid() || !level.getFluidState(q).isEmpty()); i++, q = q.below())
                    StoneAgeFeature.set(level, q, look.fill.defaultBlockState());
            }
        }
        BlockState stone = WAYSTONE.get().defaultBlockState().setValue(LOOK, look).setValue(HorizontalDirectionalBlock.FACING, face);
        level.setBlock(c.above(), stone.setValue(PART, Part.LOWER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(c.above(2), stone.setValue(PART, Part.MIDDLE), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(c.above(3), stone.setValue(PART, Part.UPPER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        Decor.rocks(level, random, c.above(), 2 + random.nextInt(3), 3, 4.5, Decor.looseFor(look.fill.defaultBlockState()));
        return c.above();
    }


    private static BlockState daisBlock(Look look, RandomSource random) {
        if (look == Look.GREY && random.nextInt(4) == 0) return (random.nextBoolean() ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
        if (look == Look.MOSSY && random.nextInt(4) == 0) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (look == Look.DARK && random.nextInt(4) == 0) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        return look.dais.defaultBlockState();
    }

    static Direction toward(int fromX, int fromZ, int toX, int toZ) {
        return Direction.getApproximateNearest(toX - fromX, 0, toZ - fromZ);
    }

    /** The feature: in the overworld every chunk asks whether an end of a pair stands in it, and builds it. */
    static final class Site extends StoneAgeFeature {
        static final MapCodec<Site> CODEC = MapCodec.unit(Site::new);

        @Override
        public MapCodec<Site> codec() {
            return CODEC;
        }

        @Override
        public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
            int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
            int[] other = partner(level.getSeed(), generator, level.getLevel().getChunkSource().randomState(), cx, cz);
            if (other == null) return false;
            build(level, cx * 16 + 8, cz * 16 + 8, toward(cx, cz, other[0], other[1]), random);
            return true;
        }
    }

    // ------------------------------------------------------------------ travel

    /** The lower half of a waystone in column x, z, or null. */
    public static BlockPos findStone(Level level, int x, int z) {
        for (int y = level.getMaxY(); y >= level.getMinY(); y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.is(WAYSTONE.get()) && s.getValue(PART) == Part.LOWER) return p;
        }
        return null;
    }

    private static boolean free(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE);
    }

    /** Somewhere to stand by the stone: in front of its runes first, then round it; on the stone as the last resort. */
    static BlockPos standingSpot(Level level, BlockPos base, Direction face) {
        BlockPos[] tries = {base.relative(face), base.relative(face.getClockWise()), base.relative(face.getCounterClockWise()),
                base.relative(face.getOpposite()), base.relative(face).relative(face.getClockWise()), base.relative(face).relative(face.getCounterClockWise()),
                base.relative(face, 2), base.relative(face, 2).above()};
        for (BlockPos p : tries) {
            BlockPos below = p.below();
            BlockState under = level.getBlockState(below);
            if (free(level, p) && free(level, p.above()) && !under.getCollisionShape(level, below).isEmpty() && under.getFluidState().isEmpty()
                    && !under.is(Blocks.MAGMA_BLOCK) && !under.is(BlockTags.CAMPFIRES)) return p;
        }
        return base.above(3);
    }

    /**
     * Sends the entity to the waystone in column x, z: loads (or generates) its chunk, builds the stone if the chunk was made
     * before it existed, and puts the entity on its feet beside it, facing the runes. Returns where it lands.
     */
    public static BlockPos teleport(Entity entity, ServerLevel level, int x, int z, Direction faceIfBuilt) {
        level.getChunk(x >> 4, z >> 4);
        BlockPos base = findStone(level, x, z);
        if (base == null) base = build(level, x, z, faceIfBuilt, level.getRandom());
        Direction face = level.getBlockState(base).getValue(HorizontalDirectionalBlock.FACING);
        BlockPos stand = standingSpot(level, base, face);
        Look look = level.getBlockState(base).getValue(LOOK);
        if (entity.level() instanceof ServerLevel from) burst(from, entity.blockPosition(), look);
        if (entity.isPassenger()) entity.stopRiding();
        entity.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), face.getOpposite().toYRot(), 10, true);
        entity.resetFallDistance();
        burst(level, stand, look);
        return stand;
    }

    private static void burst(ServerLevel level, BlockPos at, Look look) {
        double x = at.getX() + 0.5, y = at.getY() + 1, z = at.getZ() + 0.5;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 50, 0.35, 0.8, 0.35, 0.04);
        level.sendParticles(new DustParticleOptions(look.glow, 1.0F), x, y, z, 30, 0.5, 0.9, 0.5, 0.0);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.5, z, 40, 0.6, 0.6, 0.6, 0.6);
        level.playSound(null, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.7F);
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /** /waystone: the nearest pair to you (both ends and how far apart), for admins and for checking worldgen. */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("waystone").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            var pos = ctx.getSource().getPosition();
            int rx0 = Math.floorDiv((int) pos.x, REGION), rz0 = Math.floorDiv((int) pos.z, REGION);
            int[] best = null;
            double bestD = Double.MAX_VALUE;
            for (int rx = rx0 - 1; rx <= rx0 + 1; rx++)
                for (int rz = rz0 - 1; rz <= rz0 + 1; rz++) {
                    int[] p = pair(level, rx, rz);
                    if (p.length == 0) continue;
                    for (int e = 0; e < 4; e += 2) {
                        double d = Math.hypot(p[e] * 16 + 8 - pos.x, p[e + 1] * 16 + 8 - pos.z);
                        if (d < bestD) {
                            bestD = d;
                            best = e == 0 ? p : new int[] {p[2], p[3], p[0], p[1]};
                        }
                    }
                }
            if (best == null) {
                ctx.getSource().sendFailure(Component.literal("No waystone pair nearby"));
                return 0;
            }
            int[] b = best;
            int dist = (int) Math.round(16 * Math.hypot(b[2] - b[0], b[3] - b[1]));
            ctx.getSource().sendSuccess(() -> Component.literal("Waystone " + (b[0] * 16 + 8) + " " + (b[1] * 16 + 8) + " <-> "
                    + (b[2] * 16 + 8) + " " + (b[3] * 16 + 8) + " distance " + dist), false);
            return dist;
        }));
    }

    // ------------------------------------------------------------------ the block

    /** Three blocks tall (lower with its footing, middle, upper with the tapering head), runes on the face that points to the partner. */
    public static final class WaystoneBlock extends HorizontalDirectionalBlock {
        private static final VoxelShape LOWER_NS = Shapes.or(box(1, 0, 1, 15, 3, 15), box(3, 3, 3.5, 13, 16, 12.5));
        private static final VoxelShape LOWER_EW = Shapes.or(box(1, 0, 1, 15, 3, 15), box(3.5, 3, 3, 12.5, 16, 13));
        private static final VoxelShape MIDDLE_NS = box(3, 0, 3.5, 13, 16, 12.5), MIDDLE_EW = box(3.5, 0, 3, 12.5, 16, 13);
        private static final VoxelShape UPPER_NS = Shapes.or(box(3, 0, 3.5, 13, 8, 12.5), box(3.5, 8, 4, 12.5, 12, 12), box(4.5, 12, 5, 11.5, 15, 11));
        private static final VoxelShape UPPER_EW = Shapes.or(box(3.5, 0, 3, 12.5, 8, 13), box(4, 8, 3.5, 12, 12, 12.5), box(5, 12, 4.5, 11, 15, 11.5));

        public WaystoneBlock(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, Part.LOWER).setValue(LOOK, Look.GREY));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, PART, LOOK);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
            return switch (state.getValue(PART)) {
                case LOWER -> ns ? LOWER_NS : LOWER_EW;
                case MIDDLE -> ns ? MIDDLE_NS : MIDDLE_EW;
                case UPPER -> ns ? UPPER_NS : UPPER_EW;
            };
        }

        /** Creative placement: all three parts, runes toward the player, the look of the biome. */
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            BlockPos pos = ctx.getClickedPos();
            if (pos.getY() + 2 > ctx.getLevel().getMaxY() || !ctx.getLevel().getBlockState(pos.above()).canBeReplaced(ctx)
                    || !ctx.getLevel().getBlockState(pos.above(2)).canBeReplaced(ctx)) return null;
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(LOOK, look(ctx.getLevel().getBiome(pos)));
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity by, net.minecraft.world.item.ItemStack stack) {
            level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos.above(2), state.setValue(PART, Part.UPPER), Block.UPDATE_ALL);
        }

        /** A part whose neighbour part is gone goes too (only creative can remove a part). */
        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos,
                BlockState neighbour, RandomSource random) {
            Part part = state.getValue(PART);
            boolean needUp = part != Part.UPPER, needDown = part != Part.LOWER;
            if (dir == Direction.UP && needUp && !(neighbour.is(this) && neighbour.getValue(PART).ordinal() == part.ordinal() + 1)
                    || dir == Direction.DOWN && needDown && !(neighbour.is(this) && neighbour.getValue(PART).ordinal() == part.ordinal() - 1))
                return Blocks.AIR.defaultBlockState();
            return super.updateShape(state, level, ticks, pos, dir, nPos, neighbour, random);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            BlockPos base = pos.below(state.getValue(PART).ordinal());
            long now = server.getGameTime();
            Long last = USED.get(sp.getUUID());
            if (last != null && now - last >= 0 && now - last < COOLDOWN_TICKS) return InteractionResult.CONSUME;
            int[] other = partner(server, base.getX() >> 4, base.getZ() >> 4);
            if (other == null || (base.getX() & 15) != 8 || (base.getZ() & 15) != 8) {
                sp.sendOverlayMessage(Component.translatable("block.mechanical.waystone.silent"));
                return InteractionResult.CONSUME;
            }
            USED.put(sp.getUUID(), now);
            int tx = other[0] * 16 + 8, tz = other[1] * 16 + 8;
            teleport(sp, server, tx, tz, toward(tx, tz, base.getX(), base.getZ()));
            FirePit.award(sp, "wilderness/waystone");
            return InteractionResult.SUCCESS;
        }

        /** Rune light drawing in, motes of the stone's colour rising, now and then a low hum and a chime. */
        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            Look look = state.getValue(LOOK);
            Direction face = state.getValue(FACING);
            boolean upper = state.getValue(PART) == Part.UPPER;
            if (state.getValue(PART) == Part.MIDDLE && random.nextBoolean()) return;
            for (Direction side : new Direction[] {face, face.getOpposite()}) {
                double fx = pos.getX() + 0.5 + side.getStepX() * 0.32, fy = pos.getY() + (upper ? 0.45 : 0.6), fz = pos.getZ() + 0.5 + side.getStepZ() * 0.32;
                if (random.nextInt(3) == 0) // enchant glyphs fly from (start + delta) into (start)
                    level.addParticle(ParticleTypes.ENCHANT, fx, fy, fz, side.getStepX() * (0.8 + random.nextFloat()) + random.nextFloat() - 0.5,
                            random.nextFloat() * 1.2 - 0.3, side.getStepZ() * (0.8 + random.nextFloat()) + random.nextFloat() - 0.5);
                if (random.nextInt(5) == 0)
                    level.addParticle(new DustParticleOptions(look.glow, 0.5F + random.nextFloat() * 0.4F), fx + (random.nextFloat() - 0.5) * 0.5,
                            fy + (random.nextFloat() - 0.5) * 0.6, fz + (random.nextFloat() - 0.5) * 0.5, 0, 0.02, 0);
            }
            if (upper && random.nextInt(14) == 0)
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextFloat() - 0.5) * 0.3, pos.getY() + 1.0, pos.getZ() + 0.5 + (random.nextFloat() - 0.5) * 0.3,
                        0, 0.01 + random.nextFloat() * 0.02, 0);
            if (!upper && random.nextInt(50) == 0)
                level.playLocalSound(pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.35F, 0.5F + random.nextFloat() * 0.1F, false);
            if (upper && random.nextInt(90) == 0)
                level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 0.5F + random.nextFloat() * 0.3F, false);
        }
    }

    private Waystones() {}
}
