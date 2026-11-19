package com.mechanical;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Smoke indoors: a lit campfire (or a Fire Pit, three times as fast, or a burning Clay Kiln) in a closed room slowly fills it with smoke. The room is the
 * air flood-filled from just above the fire; it is vented if the column above the fire reaches open sky (a smoke hole
 * or chimney) or the fill gets out (open sky over more than {@link #LEAKS} column, e.g. through a doorway or
 * window, or bigger than {@link #MAX_ROOM}; a single gap in the roof away from the fire only leaks, halving the
 * build-up). Smoke builds up over about a minute: fog (client.SmokeClient), then coughing and Slowness, then choking
 * damage; after minutes at full smoke wood in the room catches fire. It clears slowly once vented or put out.
 * Fires are found by scanning block entities near players, so the kiln needs no hook. Burning fires also leave soot
 * on the ceiling and walls around them ({@link SootBlock}), faster in smoke.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Smoke {
    private static final int MAX_ROOM = 500, LEAKS = 1, CHIMNEY = 32;
    private static final int SCAN_TICKS = 100, SCAN_CHUNKS = 3;
    private static final float CLEAR_SECONDS = 90, COUGH = 0.5F, CHOKE = 0.9F;
    /** Seconds at full smoke before wood may catch, then a 1 in IGNITE_CHANCE chance each second. */
    private static final int IGNITE_AFTER = 180, IGNITE_CHANCE = 20;
    /** Soot (SootBlock): none for the first SOOT_AFTER seconds a fire burns, then one deposit with this chance each second, up to 2x in a smoky room. */
    private static final int SOOT_AFTER = 120;
    private static final float SOOT_CHANCE = 1 / 15F;
    /** The ground around a fire (SootBlock.scorch): after SCORCH_AFTER seconds of burning, one step with this chance each second. */
    private static final int SCORCH_AFTER = 60;
    private static final float SCORCH_CHANCE = 1 / 20F;
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "smoke"));
    public static final SoundEvent COUGH_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Mechanical.MODID, "smoke.cough"));

    private static final class Source {
        LongOpenHashSet room = new LongOpenHashSet();
        AABB box;
        boolean lit, enclosed;
        int leaks, fullSeconds, burnSeconds;
        float smoke;
    }

    private static final Map<Level, Map<BlockPos, Source>> SOURCES = new WeakHashMap<>();
    private static final Map<Player, Float> SENT = new WeakHashMap<>();

    public record Payload(float smoke) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "smoke"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = ByteBufCodecs.FLOAT.map(Payload::new, Payload::smoke).cast();

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    /** A burning fire that smokes: lit campfire / soul campfire, or the lower half of a lit Clay Kiln. Returns where the smoke comes out, or null. */
    private static BlockPos smokeStart(BlockState state, BlockPos pos) {
        if (CampfireBlock.isLitCampfire(state)) return pos.above();
        if (FirePit.isLit(state) && state.getValue(FirePitBlock.PART) == 0) return pos.above();
        if (state.is(Mechanical.KILN.get()) && state.getValue(KilnBlock.LIT) && state.getValue(KilnBlock.HALF) == DoubleBlockHalf.LOWER) return pos.above(2);
        return null;
    }

    /** Smoke passes through anything that isn't a wall, and through open doors and trapdoors. */
    private static boolean passable(BlockState state) {
        return !state.isSolid() || (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) && state.getValue(BlockStateProperties.OPEN);
    }

    /** Fills {@code src.room} with the air around the fire; false if the smoke gets out. */
    private static boolean flood(ServerLevel level, BlockPos start, Source src) {
        for (BlockPos p = start; passable(level.getBlockState(p)) && p.getY() - start.getY() < CHIMNEY; p = p.above()) {
            if (level.canSeeSky(p)) return false; // smoke hole / chimney over the fire
        }
        if (!passable(level.getBlockState(start))) return false;
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start.asLong());
        queue.add(start);
        LongOpenHashSet leaks = new LongOpenHashSet(); // columns open to the sky
        BlockPos.MutableBlockPos min = start.mutable(), max = start.mutable();
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            // Under open sky: a gap in the roof (one column) only leaks; outside a wall opening the fill spreads to more columns
            boolean sky = level.canSeeSky(p);
            if (sky && leaks.add(BlockPos.asLong(p.getX(), 0, p.getZ())) && leaks.size() > LEAKS) return false;
            min.set(Math.min(min.getX(), p.getX()), Math.min(min.getY(), p.getY()), Math.min(min.getZ(), p.getZ()));
            max.set(Math.max(max.getX(), p.getX()), Math.max(max.getY(), p.getY()), Math.max(max.getZ(), p.getZ()));
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (sky && d == Direction.UP || seen.contains(n.asLong())) continue;
                if (!level.isLoaded(n)) return false;
                if (!passable(level.getBlockState(n))) continue;
                if (seen.size() >= MAX_ROOM) return false;
                seen.add(n.asLong());
                queue.add(n);
            }
        }
        src.room = seen;
        src.leaks = leaks.size();
        src.box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        return true;
    }

    /** Picks up the fires in the chunks around {@code c}. */
    private static void scan(ServerLevel level, ChunkPos c, Map<BlockPos, Source> sources) {
        for (int x = c.x() - SCAN_CHUNKS; x <= c.x() + SCAN_CHUNKS; x++) {
            for (int z = c.z() - SCAN_CHUNKS; z <= c.z() + SCAN_CHUNKS; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (BlockEntity be : level.getChunk(x, z).getBlockEntities().values()) {
                    if (smokeStart(be.getBlockState(), be.getBlockPos()) != null) sources.computeIfAbsent(be.getBlockPos(), p -> new Source());
                }
            }
        }
    }

    /** Smoke level (0-1) at a block: the smokiest room it is in. */
    public static float smokeAt(Level level, BlockPos pos) {
        float smoke = 0;
        for (Source src : SOURCES.getOrDefault(level, Map.of()).values()) {
            if (src.smoke > smoke && src.room.contains(pos.asLong())) smoke = src.smoke;
        }
        return smoke;
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, COUGH_SOUND.location(), () -> COUGH_SOUND);
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        Map<BlockPos, Source> sources = SOURCES.computeIfAbsent(level, l -> new HashMap<>());
        boolean scan = level.getGameTime() % SCAN_TICKS == 0;
        if (scan) for (ServerPlayer player : level.players()) scan(level, player.chunkPosition(), sources);
        RandomSource random = level.getRandom();
        var it = sources.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            BlockPos pos = entry.getKey();
            Source src = entry.getValue();
            if (!level.isLoaded(pos)) {
                it.remove();
                continue;
            }
            BlockState fire = level.getBlockState(pos);
            BlockPos start = smokeStart(fire, pos);
            boolean pit = FirePit.isPit(fire); // the Fire Pit smokes and soots FirePit.SMOKE times as much, scorches a wider hearth
            src.lit = start != null;
            src.burnSeconds = src.lit ? src.burnSeconds + 1 : 0;
            if (src.burnSeconds > SOOT_AFTER && random.nextFloat() < SOOT_CHANCE * (1 + src.smoke) * (pit ? FirePit.SMOKE : 1)) SootBlock.deposit(level, start, random);
            if (src.burnSeconds > SCORCH_AFTER && random.nextFloat() < SCORCH_CHANCE * (pit ? FirePit.SCORCH : 1)) {
                SootBlock.scorch(level, pit ? pos.offset(random.nextInt(2), 0, random.nextInt(2)) : pos, random);
            }
            if (!src.lit) src.enclosed = false;
            else if (scan) src.enclosed = flood(level, start, src); // a vented room keeps its old extent while it clears
            if (src.lit && src.enclosed) src.smoke = Math.min(1, src.smoke + (src.leaks == 0 ? 1 : 0.5F) * (pit ? FirePit.SMOKE : 1) / (30 + src.room.size() / 5F));
            else src.smoke -= 1 / CLEAR_SECONDS;
            if (src.smoke <= 0) {
                src.smoke = 0;
                if (!src.lit) it.remove();
                continue;
            }
            src.fullSeconds = src.smoke >= CHOKE ? src.fullSeconds + 1 : 0;
            if (src.fullSeconds >= IGNITE_AFTER && random.nextInt(IGNITE_CHANCE) == 0) ignite(level, src, random);
            if (src.smoke >= COUGH && src.box != null) hurt(level, src, random);
        }
        for (ServerPlayer player : level.players()) {
            float smoke = smokeAt(level, BlockPos.containing(player.getEyePosition()));
            Float sent = SENT.get(player);
            if (sent == null || Math.abs(sent - smoke) > 0.02F || smoke == 0 && sent != 0) {
                SENT.put(player, smoke);
                PacketDistributor.sendToPlayer(player, new Payload(smoke));
            }
        }
    }

    /** Living things in the smoke: Slowness and coughing, and at full smoke ½ heart every 2 seconds. */
    private static void hurt(ServerLevel level, Source src, RandomSource random) {
        boolean choke = src.smoke >= CHOKE && level.getGameTime() % 40 == 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, src.box)) {
            if (!src.room.contains(BlockPos.containing(e.getEyePosition()).asLong())) continue;
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, false, true));
            if (random.nextInt(4) == 0) level.playSound(null, e.getX(), e.getEyeY(), e.getZ(), COUGH_SOUND, SoundSource.PLAYERS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            if (choke) e.hurtServer(level, level.damageSources().source(DAMAGE), 1.0F);
        }
    }

    /** After a long while at full smoke: fire starts in the air next to something flammable in the room. */
    private static void ignite(ServerLevel level, Source src, RandomSource random) {
        long[] room = src.room.toLongArray();
        for (int i = 0; i < 16; i++) {
            BlockPos p = BlockPos.of(room[random.nextInt(room.length)]);
            if (!level.getBlockState(p).isAir()) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (level.getBlockState(n).isFlammable(level, n, d.getOpposite())) {
                    level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
                    return;
                }
            }
        }
    }

    /** /smoke <pos>: smoke level there and the fires being tracked (testing). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("smoke").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                    scan(level, ChunkPos.containing(pos), SOURCES.computeIfAbsent(level, l -> new HashMap<>())); // fires near pos count even with no player there
                    StringBuilder msg = new StringBuilder(String.format("Smoke at %s: %.2f", pos.toShortString(), smokeAt(level, pos)));
                    SOURCES.getOrDefault(level, Map.of()).forEach((p, s) -> msg.append(String.format("%n fire %s: lit=%s enclosed=%s room=%d leaks=%d smoke=%.2f full=%ds",
                            p.toShortString(), s.lit, s.enclosed, s.room.size(), s.leaks, s.smoke, s.fullSeconds)));
                    ctx.getSource().sendSuccess(() -> Component.literal(msg.toString()), false);
                    return 1;
                })));
    }

    private Smoke() {}
}
