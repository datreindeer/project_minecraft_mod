package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The Elkwight ruins at run time (docs/elkwight-plan.md phase 2): where they are, whether their boss still lives, and the
 * ruin fog. The boss entity ({@link #BOSS}) comes from the boss phase; without it the ruins generate empty and the fog
 * never clears. Each ruin's "boss dead" flag is kept in SavedData (set when an entity of type {@link #BOSS} dies), and
 * once a second every player gets {@link Payload} when their state changes (outside, in a ruin with the boss alive, in a
 * ruin whose boss is dead); client.RuinFogClient draws the fog from it.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ElkwightRuins {
    public static final Identifier BOSS = Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight");
    /** Called once per ruin with the arena centre (one block above the dais) while it generates. Swapped by ElkwightWorldTest. */
    public static BiConsumer<WorldGenLevel, BlockPos> spawner = ElkwightRuins::spawnBoss;
    public static final int OUTSIDE = 0, FOG = 1, CLEARED = 2;
    private static final String RUIN_TAG = "mechanical_ruin";
    private static final Map<ServerPlayer, Integer> SENT = new WeakHashMap<>();

    /** Spawns one persistent {@link #BOSS} at pos; silently nothing when no such entity type is registered. */
    public static void spawnBoss(WorldGenLevel level, BlockPos pos) {
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(BOSS);
        if (type.isEmpty()) return;
        Entity boss = type.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (boss == null) return;
        boss.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        if (boss instanceof Mob mob) mob.setPersistenceRequired();
        boss.getPersistentData().putLong(RUIN_TAG, pos.asLong()); // finds its ruin again when it dies, wherever it fell
        level.addFreshEntityWithPassengers(boss);
    }

    /** The bounds of the ruin at pos, if pos is inside one. */
    public static Optional<BoundingBox> ruinAt(ServerLevel level, BlockPos pos) {
        Structure ruin = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(ElkwightRuinStructure.KEY);
        if (ruin == null || !level.isLoaded(pos)) return Optional.empty();
        StructureStart start = level.structureManager().getStructureAt(pos, ruin);
        return start.isValid() ? Optional.of(start.getBoundingBox()) : Optional.empty();
    }

    /** False once this ruin's boss has died (it never comes back). */
    public static boolean bossAlive(ServerLevel level, BoundingBox ruin) {
        return !Dead.get(level).ruins.contains(ruin.getCenter().asLong());
    }

    public static void markDead(ServerLevel level, BoundingBox ruin) {
        Dead d = Dead.get(level);
        if (d.ruins.add(ruin.getCenter().asLong())) d.setDirty();
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !BOSS.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()))) return;
        Optional<BoundingBox> ruin = e.getPersistentData().contains(RUIN_TAG) ? ruinAt(level, BlockPos.of(e.getPersistentData().getLongOr(RUIN_TAG, 0)))
                : Optional.empty();
        ruin.or(() -> ruinAt(level, e.blockPosition())).ifPresent(r -> markDead(level, r));
    }

    /** OUTSIDE, FOG or CLEARED for someone at pos. */
    public static int state(ServerLevel level, BlockPos pos) {
        return ruinAt(level, pos).map(r -> bossAlive(level, r) ? FOG : CLEARED).orElse(OUTSIDE);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 7) return;
        for (ServerPlayer player : level.players()) {
            int state = state(level, player.blockPosition());
            Integer sent = SENT.get(player);
            if (sent == null || sent != state) {
                SENT.put(player, state);
                PacketDistributor.sendToPlayer(player, new Payload(state));
            }
        }
    }

    public record Payload(int state) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "ruin_fog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = ByteBufCodecs.VAR_INT.map(Payload::new, Payload::state).cast();

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC);
    }

    /** Ruins (by the centre of their bounds) whose boss has died. */
    static final class Dead extends SavedData {
        private static final Codec<Dead> CODEC = Codec.LONG.listOf().xmap(Dead::new, d -> new ArrayList<>(d.ruins));
        private static final SavedDataType<Dead> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_ruins"),
                () -> new Dead(List.of()), CODEC);
        final LongOpenHashSet ruins;

        private Dead(List<Long> ruins) {
            this.ruins = new LongOpenHashSet(ruins);
        }

        static Dead get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }
    }

    private ElkwightRuins() {}
}
