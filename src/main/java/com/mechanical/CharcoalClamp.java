package com.mechanical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The charcoal clamp (kolmila, docs/ages/03-iron-age.md): wood piles ({@link FirewoodPileBlock}, {@link RoughLogPileBlock})
 * stacked together and covered on every side and on top with earth ({@link #COVER}) or solid stone, but for one open
 * face: the vent. A Fire Drill on the vent lights it (as often as a campfire catches without tinder). It smoulders
 * {@link #BURN_TICKS}, smoke rising from the vent, then every pile is a {@link CharcoalHeapBlock}: one Charcoal per piece
 * of firewood, two per rough log (the Clay Kiln gives one per two firewood). A pile opened to the air while it smoulders
 * flares up and burns away. Kept in the level's saved data so a clamp outlives a restart; it waits while unloaded.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class CharcoalClamp extends SavedData {
    /** Covers a clamp besides any sturdy block that doesn't burn: dirt and turf of all kinds, gravel, clay, mud. */
    public static final TagKey<Block> COVER = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mechanical.MODID, "clamp_cover"));
    /** 5 minutes. */
    public static final int BURN_TICKS = 6000;
    /** Pile blocks one clamp may hold. */
    public static final int MAX_BLOCKS = 64;
    /** Charcoal per rough log (per piece of firewood: 1). */
    public static final int CHARCOAL_PER_LOG = 2;
    static final Identifier ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/charcoal_clamp");
    /** What a clamp's piles become: charred billets that are Charcoal ({@link CharcoalHeapBlock}). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<CharcoalHeapBlock> CHARCOAL_HEAP = Mechanical.BLOCKS.registerBlock("charcoal_heap",
            CharcoalHeapBlock::new, p -> p.mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK).strength(0.8F)
                    .sound(net.minecraft.world.level.block.SoundType.GRAVEL).noOcclusion().noLootTable());

    public static void init() {}

    record Clamp(List<BlockPos> blocks, BlockPos vent, Direction ventFace, long endTick, Optional<UUID> owner) {
        static final Codec<Clamp> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.listOf().fieldOf("blocks").forGetter(Clamp::blocks),
                BlockPos.CODEC.fieldOf("vent").forGetter(Clamp::vent),
                Direction.CODEC.fieldOf("vent_face").forGetter(Clamp::ventFace),
                Codec.LONG.fieldOf("end").forGetter(Clamp::endTick),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Clamp::owner)).apply(i, Clamp::new));
    }

    private static final Codec<CharcoalClamp> CODEC = Clamp.CODEC.listOf().xmap(CharcoalClamp::new, d -> d.clamps);
    private static final SavedDataType<CharcoalClamp> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "charcoal_clamps"),
            () -> new CharcoalClamp(List.of()), CODEC);

    private final List<Clamp> clamps;

    private CharcoalClamp(List<Clamp> clamps) {
        this.clamps = new ArrayList<>(clamps);
    }

    static CharcoalClamp get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    static boolean isPile(BlockState state) {
        return state.getBlock() instanceof FirewoodPileBlock || state.getBlock() instanceof RoughLogPileBlock;
    }

    /** Is {@code pos} part of a smouldering clamp? */
    public static boolean smouldering(ServerLevel level, BlockPos pos) {
        return get(level).clamps.stream().anyMatch(c -> c.blocks.contains(pos));
    }

    /** The wood piles connected to {@code start} (faces touching), at most {@link #MAX_BLOCKS} + 1. */
    static Set<BlockPos> piles(Level level, BlockPos start) {
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start.immutable()));
        while (!queue.isEmpty() && found.size() <= MAX_BLOCKS) {
            BlockPos p = queue.poll();
            if (found.contains(p) || !isPile(level.getBlockState(p))) continue;
            found.add(p);
            for (Direction d : Direction.values()) queue.add(p.relative(d));
        }
        return found;
    }

    /** Does what lies at {@code pos} (seen from the pile through {@code face}) close the clamp there? */
    static boolean covers(Level level, BlockPos pos, Direction face) {
        BlockState s = level.getBlockState(pos);
        return s.is(COVER) || (s.isFaceSturdy(level, pos, face.getOpposite()) && !s.ignitedByLava() && !isPile(s));
    }

    /** Faces of {@code piles} open to the air (not the vent), as the pile block and the side. */
    static List<BlockPos> openFaces(Level level, Set<BlockPos> piles, BlockPos vent, Direction ventFace) {
        List<BlockPos> open = new ArrayList<>();
        for (BlockPos p : piles) {
            for (Direction d : Direction.values()) {
                if (p.equals(vent) && d == ventFace) continue;
                BlockPos n = p.relative(d);
                if (!piles.contains(n) && !covers(level, n, d)) open.add(p);
            }
        }
        return open;
    }

    /**
     * A Fire Drill on {@code face} of the pile at {@code pos}: lights the clamp when it is covered all round but for that face
     * (and the drill catches: {@code catches}). Messages say what is missing. True if it is smouldering now.
     */
    public static boolean tryLight(ServerLevel level, BlockPos pos, Direction face, Player player, boolean catches) {
        if (smouldering(level, pos)) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.clamp.burning"));
            return false;
        }
        Set<BlockPos> piles = piles(level, pos);
        if (piles.size() > MAX_BLOCKS) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.clamp.too_big", MAX_BLOCKS));
            return false;
        }
        if (!level.getBlockState(pos.relative(face)).canBeReplaced()) return false; // the vent must be open
        int open = openFaces(level, piles, pos, face).size();
        if (open > 0) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.clamp.open", open));
            return false;
        }
        if (!catches) return false;
        get(level).add(new Clamp(List.copyOf(piles), pos.immutable(), face, level.getGameTime() + BURN_TICKS,
                player == null ? Optional.empty() : Optional.of(player.getUUID())));
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        if (player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.clamp.lit", BURN_TICKS / 1200));
        return true;
    }

    private void add(Clamp clamp) {
        clamps.add(clamp);
        setDirty();
    }

    @SubscribeEvent
    static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        CharcoalClamp data = level.getDataStorage().get(TYPE);
        if (data != null && !data.clamps.isEmpty()) data.tick(level);
    }

    /** Smoke from the vents; every 5 s piles open to the air burn away; finished clamps turn to charcoal. */
    void tick(ServerLevel level) {
        long now = level.getGameTime();
        boolean check = now % 100 == 0;
        for (int i = clamps.size() - 1; i >= 0; i--) {
            Clamp c = clamps.get(i);
            if (!level.isLoaded(c.vent)) { // waits while its chunk is unloaded
                clamps.set(i, new Clamp(c.blocks, c.vent, c.ventFace, c.endTick + 20, c.owner));
                setDirty();
                continue;
            }
            BlockPos out = c.vent.relative(c.ventFace);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, out.getX() + 0.5, out.getY() + 0.3, out.getZ() + 0.5, 2, 0.15, 0.1, 0.15, 0.01);
            List<BlockPos> blocks = new ArrayList<>(c.blocks.stream().filter(p -> isPile(level.getBlockState(p))).toList());
            if (check || blocks.size() != c.blocks.size()) {
                for (BlockPos p : openFaces(level, Set.copyOf(blocks), c.vent, c.ventFace).stream().distinct().toList()) flare(level, p);
                blocks.removeIf(p -> !isPile(level.getBlockState(p)));
            }
            if (blocks.isEmpty()) {
                clamps.remove(i);
                setDirty();
                continue;
            }
            if (now < c.endTick) {
                if (blocks.size() != c.blocks.size()) {
                    clamps.set(i, new Clamp(List.copyOf(blocks), c.vent, c.ventFace, c.endTick, c.owner));
                    setDirty();
                }
                continue;
            }
            for (BlockPos p : blocks) char_(level, p);
            level.playSound(null, c.vent, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.8F);
            c.owner.map(level::getPlayerByUUID).filter(ServerPlayer.class::isInstance)
                    .ifPresent(p -> Kettle.award((ServerPlayer) p, ADVANCEMENT));
            clamps.remove(i);
            setDirty();
        }
    }

    /** Opened to the air: the pile flares up and is gone. */
    private static void flare(ServerLevel level, BlockPos p) {
        level.removeBlock(p, false);
        level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.7, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, p, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** A finished pile turns into a heap of charcoal, keeping its shape. */
    private static void char_(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        int charcoal = s.getBlock() instanceof RoughLogPileBlock ? s.getValue(RoughLogPileBlock.LOGS) * CHARCOAL_PER_LOG : s.getValue(FirewoodPileBlock.LAYERS);
        Direction.Axis axis = s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS);
        level.removeBlockEntity(p); // the wood piles' block entity has no place in the heap
        level.setBlock(p, CHARCOAL_HEAP.get().defaultBlockState()
                .setValue(FirewoodPileBlock.LAYERS, Math.min(FirewoodPileBlock.MAX, charcoal)).setValue(FirewoodPileBlock.AXIS, axis), Block.UPDATE_ALL);
    }

    /** Ends every clamp now (game test). */
    void finishAll(ServerLevel level) {
        for (int i = 0; i < clamps.size(); i++) {
            Clamp c = clamps.get(i);
            clamps.set(i, new Clamp(c.blocks, c.vent, c.ventFace, level.getGameTime(), c.owner));
        }
        tick(level);
    }

    /** A breach check on every clamp now (game test): piles open to the air burn away. */
    void checkNow(ServerLevel level) {
        for (Clamp c : clamps) {
            List<BlockPos> blocks = c.blocks.stream().filter(p -> isPile(level.getBlockState(p))).toList();
            for (BlockPos p : openFaces(level, Set.copyOf(blocks), c.vent, c.ventFace).stream().distinct().toList()) flare(level, p);
        }
    }

    int count() {
        return clamps.size();
    }
}
