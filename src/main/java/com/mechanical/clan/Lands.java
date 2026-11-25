package com.mechanical.clan;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mechanical.Mechanical;
import com.mechanical.dashboard.Villages;
import com.mechanical.people.Tribe;
import com.mechanical.people.Tribesperson;
import com.mechanical.worldgen.HermitsHut;
import com.mechanical.worldgen.TribeVillageStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Whose land a player stands on: a player tribe's territory (Clans.owner: 5x5 chunks round its Fire Pit, more for a big one), a tribe
 * village inside its palisade, an NPC camp round the fire its people call home, a vanilla village or a Hermit's Hut (both
 * with a generated name). On stepping into one the server
 * sends an {@link Entered} once (client.clan.LandTitle shows "You are now entering X's land").
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Lands {
    public static final int HOME = 0, CLAN = 1, TRIBE = 2, VILLAGE = 3, HERMIT = 4;
    /** A vanilla village reaches this far past its houses and streets, a Hermit's Hut this far from its middle. */
    static final int VILLAGE_MARGIN = 8, HUT_LAND = 12;
    /** A camp without a wall still reaches this far from its fire. */
    public static final int CAMP_LAND = 10;
    static final int CHECK_TICKS = 10;

    /** {@code key} tells lands apart ("clan:id", "tribe:id"); {@code name} a tribe name, an NPC tribe's id, a village's or a hermit's name; {@code colour} a DyeColor id. */
    public record Land(int kind, String key, String name, int colour, UUID tribe) {}

    /** {@code rep} the player's standing with an NPC tribe (unused for the other kinds). */
    public record Entered(int kind, String name, int colour, int rep) implements CustomPacketPayload {
        public static final Type<Entered> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "land_entered"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entered> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entered::kind, ByteBufCodecs.stringUtf8(64), Entered::name, ByteBufCodecs.VAR_INT, Entered::colour,
                ByteBufCodecs.VAR_INT, Entered::rep, Entered::new);

        @Override
        public Type<Entered> type() {
            return TYPE;
        }
    }

    /** The land each online player stood on at the last check (not saved: logging in inside one shows it again). */
    private static final Map<UUID, String> LAST = new HashMap<>();

    /** The land at {@code pos} for {@code player}: player tribes, tribe villages, camps, vanilla villages, huts; null in the wild. */
    public static Land landAt(Clans data, ServerLevel level, UUID player, Vec3 pos) {
        Clans.Clan owner = data.owner(level.dimension(), ChunkPos.containing(BlockPos.containing(pos)));
        if (owner != null) return new Land(owner.member(player) != null ? HOME : CLAN, "clan:" + owner.id, owner.name, owner.colour, null);
        UUID npc = npcTribeAt(level, pos);
        if (npc != null) return tribe(npc);
        BlockPos at = BlockPos.containing(pos);
        Land village = villageLand(level, at);
        return village != null ? village : hutLand(level, at);
    }

    private static Land villageLand(ServerLevel level, BlockPos at) {
        var structures = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        // only reads the chunk's structure references, so it is cheap enough for the 10 tick check
        for (var start : level.structureManager().startsForStructure(at.getX() >> 4, at.getZ() >> 4,
                s -> structures.wrapAsHolder(s).is(net.minecraft.tags.StructureTags.VILLAGE))) {
            for (var piece : start.getPieces()) {
                if (piece.getBoundingBox().inflatedBy(VILLAGE_MARGIN).isInside(at))
                    return new Land(VILLAGE, "village:" + start.getChunkPos().pack(), Villages.name(level, start),
                            net.minecraft.world.item.DyeColor.YELLOW.getId(), null);
            }
        }
        return null;
    }

    /** Round the hut of a hermit who is still about; named from where the hut stands. */
    private static Land hutLand(ServerLevel level, BlockPos at) {
        for (var v : level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new net.minecraft.world.phys.AABB(at).inflate(32))) {
            BlockPos hut = HermitsHut.home(v);
            if (hut != null && Math.abs(hut.getY() - at.getY()) <= HUT_LAND && flat(hut, Vec3.atCenterOf(at)) <= HUT_LAND)
                return new Land(HERMIT, "hut:" + hut.asLong(), hermitName(level.getSeed() ^ hut.asLong()),
                        net.minecraft.world.item.DyeColor.BROWN.getId(), null);
        }
        return null;
    }

    /**
     * Is the centre of chunk {@code c} NPC land (a village's palisade or a camp's fire)? Clans keeps its territory off such
     * chunks. Never loads or generates a chunk (a big pit's fill asks about ~1000): an unloaded chunk counts as not NPC land
     * (getStructureAt would generate it to structure references), and a loaded one is probed at its surface height, since
     * villageAt checks 3D bounds.
     */
    public static boolean npcLandAt(ServerLevel level, ChunkPos c) {
        if (!level.hasChunk(c.x(), c.z())) return false;
        int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
        return npcTribeAt(level, new Vec3(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z), z)) != null;
    }

    /** The NPC tribe whose village or camp covers {@code pos}, or null: villages first, then camps. */
    private static UUID npcTribeAt(ServerLevel level, Vec3 pos) {
        BlockPos at = BlockPos.containing(pos);
        TribeVillageStructure.Piece village = TribeVillageStructure.villageAt(level, at);
        if (village != null && flat(village.centre(), pos) <= TribeVillageStructure.WALL_R + 2)
            return TribeVillageStructure.tribeOf(village.seed());
        for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, new net.minecraft.world.phys.AABB(at).inflate(32)))
            if (t.home() != null && flat(t.home(), pos) <= Math.max(CAMP_LAND, t.campRadius())) return t.group();
        return null;
    }

    private static final String[] HERMITS = {"Aldric", "Wren", "Osric", "Hilda", "Bartholomew", "Agnes", "Edwin", "Maud",
            "Godric", "Elspeth", "Anselm", "Rowena", "Cedric", "Winifred", "Leofric", "Ingrid", "Ulric", "Mildred", "Percival", "Gytha",
            "Alwin", "Brida", "Orrin", "Sigrun", "Tobias", "Hedda", "Emeric", "Ysolde"};

    static String hermitName(long seed) {
        return HERMITS[net.minecraft.util.RandomSource.create(seed).nextInt(HERMITS.length)];
    }

    private static Land tribe(UUID id) {
        return new Land(TRIBE, "tribe:" + id, id.toString(), Tribe.of(id).baseColour().getId(), id);
    }

    private static double flat(BlockPos centre, Vec3 pos) {
        double dx = centre.getX() + 0.5 - pos.x, dz = centre.getZ() + 0.5 - pos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_TICKS != 0 || player.isSpectator()
                || player instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        ServerLevel level = player.level();
        Land land = landAt(Clans.get(level.getServer()), level, player.getUUID(), player.position());
        String key = land == null ? null : land.key();
        if (java.util.Objects.equals(key, LAST.put(player.getUUID(), key)) || land == null) return;
        int rep = land.tribe() == null ? 0 : Tribe.of(land.tribe()).reputation(player);
        PacketDistributor.sendToPlayer(player, new Entered(land.kind(), land.name(), land.colour(), rep));
        String advancement = switch (land.kind()) {
            case CLAN, TRIBE -> "tribe_land";
            case VILLAGE -> "village_land";
            case HERMIT -> "hermit_land";
            default -> null;
        };
        if (advancement == null) return;
        var holder = level.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/" + advancement));
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    private Lands() {}
}
