package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.MODID;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Trampled paths: grass that players (and the horses they ride, and harnessed carts and travois) walk over again and
 * again wears to Worn Grass and then to a Trodden Path, which are faster to walk on. Left alone they grow back.
 * Snow on the way is trodden down a layer at a time first, until the ground shows.
 * Data: scripts/gen_paths.py, textures scripts/gen_paths_textures.py.
 *
 * <p>Speed uses vanilla's block speed factor. Entity.move multiplies the horizontal velocity by it every tick, before
 * ground friction (0.6 * 0.91), so it compounds: steady speed scales by (1 - 0.546) / (1 - 0.546 f). f = 1.075 gives
 * +10 %, f = 1.14 gives +20 % (a plain factor of 1.2 would be +32 %). It works for every mob and mount and on the client's
 * own movement prediction without any syncing.
 */
@EventBusSubscriber(modid = MODID)
public final class TrampledPaths {
    static final float WORN_SPEED = 1.075F, PATH_SPEED = 1.14F;
    /** Chance per step onto a block: grass to Worn Grass, Worn Grass (or bare dirt) to Trodden Path. ~30 walks in all. */
    static final float GRASS_WEAR = 1 / 20F, PATH_WEAR = 1 / 10F;
    /** Chance per step into snow that it packs down one layer: a storm's 3 layers take ~24 walks. */
    static final float SNOW_WEAR = 1 / 8F;

    /** Ground the earth of a path runs out toward (everything else frays into grass). */
    public static final TagKey<Block> CONNECTS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "path_connects"));
    /** Thin covers a path wears in under (soot, carpets, rugs, leaf litter); they stay when the path forms. Any block with a
     * collision top of at most {@link #COVER_HEIGHT} counts as a cover too (a carpet, two snow layers, a rug). */
    public static final TagKey<Block> COVERS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "path_covers"));
    static final double COVER_HEIGHT = 3 / 16.0;

    public static final DeferredBlock<TrampledPathBlock> WORN_GRASS = BLOCKS.registerBlock("worn_grass", TrampledPathBlock::new, p -> p
            .mapColor(MapColor.GRASS).strength(0.6F).sound(SoundType.GRASS).randomTicks().speedFactor(WORN_SPEED));
    public static final DeferredBlock<TrampledPathBlock> TRODDEN_PATH = BLOCKS.registerBlock("trodden_path", TrampledPathBlock::new, p -> p
            .mapColor(MapColor.DIRT).strength(0.65F).sound(SoundType.GRASS).randomTicks().speedFactor(PATH_SPEED));

    /** The block each walker last stood on, so only stepping onto a new block counts (server thread only). */
    private static final Map<Entity, Long> LAST = new WeakHashMap<>();

    public static void init() {
    }

    /** Players on foot (not sneaking: you can tiptoe over a lawn) and the horses players ride. */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.level().isClientSide() || e.isPassenger()) return;
        if (e instanceof Player p ? !p.isShiftKeyDown() && !p.isSpectator() : e.getControllingPassenger() instanceof Player) step(e);
    }

    /** Server: {@code e} moved this tick; if it stepped onto a new block on the ground, that block may wear. */
    public static void step(Entity e) {
        step(e, 1);
    }

    /** Like {@link #step(Entity)}, but a new block is worn {@code times} times (tribe folk walking between camps). */
    public static void step(Entity e, int times) {
        if (!e.onGround()) return;
        BlockPos on = e.getOnPos();
        if (isCover(e.level().getBlockState(on), e.level(), on)) on = on.below(); // standing on a carpet: the ground under it wears
        // snow is trodden down first (thick snow is what we stand on, thin snow lies on the ground above)
        BlockPos snow = e.level().getBlockState(on).is(Blocks.SNOW) ? on : e.level().getBlockState(on.above()).is(Blocks.SNOW) ? on.above() : null;
        Long last = LAST.put(e, on.asLong());
        if (last != null && last != on.asLong()) for (int i = 0; i < times; i++) {
            if (snow != null && !e.level().getBlockState(snow).is(Blocks.SNOW)) snow = null; // trodden away this step
            if (snow != null) packSnow((ServerLevel) e.level(), snow, e.getRandom());
            else wear((ServerLevel) e.level(), on, e.getRandom());
        }
    }

    /** One step into snow: maybe one layer less; the last layer goes, baring the ground for the path. */
    static void packSnow(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= SNOW_WEAR) return;
        BlockState s = level.getBlockState(pos);
        int layers = s.getValue(SnowLayerBlock.LAYERS);
        if (layers == 1) level.removeBlock(pos, false);
        else level.setBlockAndUpdate(pos, s.setValue(SnowLayerBlock.LAYERS, layers - 1));
    }

    /** One step onto {@code pos}: maybe one stage more worn; a path stepped on is RECENT and won't grow back for now. */
    static void wear(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState s = level.getBlockState(pos);
        BlockState next = null;
        if (s.is(Blocks.GRASS_BLOCK)) {
            if (random.nextFloat() < GRASS_WEAR) next = TrampledPathBlock.connect(WORN_GRASS.get().defaultBlockState()
                    .setValue(SnowyBlock.SNOWY, s.getValue(SnowyBlock.SNOWY)), level, pos);
        } else if (s.is(WORN_GRASS)) {
            if (random.nextFloat() < PATH_WEAR) next = TRODDEN_PATH.get().withPropertiesOf(s);
        } else if (s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT)) {
            if (random.nextFloat() < PATH_WEAR) next = TrampledPathBlock.connect(TRODDEN_PATH.get().defaultBlockState(), level, pos);
        }
        if (next != null) {
            level.setBlockAndUpdate(pos, next.setValue(TrampledPathBlock.RECENT, true));
            BlockState above = level.getBlockState(pos.above());
            // grass and ferns don't survive the middle of a path
            if (next.is(TRODDEN_PATH) && !above.isAir() && above.canBeReplaced() && above.getFluidState().isEmpty() && !above.is(BlockTags.SNOW) && !above.is(COVERS))
                level.destroyBlock(pos.above(), false);
        } else if (s.getBlock() instanceof TrampledPathBlock && !s.getValue(TrampledPathBlock.RECENT) && !s.getValue(TrampledPathBlock.PERSISTENT)) {
            level.setBlock(pos, s.setValue(TrampledPathBlock.RECENT, true), Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
        }
    }

    /** A thin cover on the ground: tagged mechanical:path_covers, or a solid sliver at most {@link #COVER_HEIGHT} high. */
    static boolean isCover(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (state.is(COVERS)) return true;
        if (state.isAir()) return false;
        var shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(net.minecraft.core.Direction.Axis.Y) <= COVER_HEIGHT + 1.0E-6;
    }

    private TrampledPaths() {}
}
