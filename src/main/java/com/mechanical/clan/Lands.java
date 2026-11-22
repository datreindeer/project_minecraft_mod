package com.mechanical.clan;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mechanical.Mechanical;
import com.mechanical.people.Tribe;
import com.mechanical.people.Tribesperson;
import com.mechanical.worldgen.TribeVillageStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Whose land a player stands on: a player tribe's 3x3 chunks round its Fire Pit (the planned forceload area), a tribe
 * village inside its palisade, or an NPC camp round the fire its people call home. On stepping into one the server
 * sends an {@link Entered} once (client.clan.LandTitle shows "You are now entering X's land").
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Lands {
    public static final int HOME = 0, CLAN = 1, TRIBE = 2;
    /** Chunks out from the Fire Pit's chunk (1: 3x3). */
    public static final int PIT_CHUNKS = 1;
    /** A camp without a wall still reaches this far from its fire. */
    public static final int CAMP_LAND = 10;
    static final int CHECK_TICKS = 10;

    /** {@code key} tells lands apart ("clan:id", "tribe:id"); {@code name} a tribe name or an NPC tribe's id; {@code colour} a DyeColor id. */
    public record Land(int kind, String key, String name, int colour, UUID tribe) {}

    /** {@code rep} the player's standing with an NPC tribe (unused for player tribes). */
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

    /** The land at {@code pos} for {@code player}: player tribes first, then villages, then camps; null in the wild. */
    public static Land landAt(Clans data, ServerLevel level, UUID player, Vec3 pos) {
        int cx = SectionPos.blockToSectionCoord(pos.x), cz = SectionPos.blockToSectionCoord(pos.z);
        for (Clans.Clan c : data.clans) {
            if (c.pit.isEmpty() || c.pit.get().dimension() != level.dimension()) continue;
            BlockPos pit = c.pit.get().pos();
            if (Math.abs(SectionPos.blockToSectionCoord(pit.getX()) - cx) > PIT_CHUNKS || Math.abs(SectionPos.blockToSectionCoord(pit.getZ()) - cz) > PIT_CHUNKS) continue;
            return new Land(c.member(player) != null ? HOME : CLAN, "clan:" + c.id, c.name, c.colour, null);
        }
        BlockPos at = BlockPos.containing(pos);
        TribeVillageStructure.Piece village = TribeVillageStructure.villageAt(level, at);
        if (village != null && flat(village.centre(), pos) <= TribeVillageStructure.WALL_R + 2)
            return tribe(TribeVillageStructure.tribeOf(village.seed()));
        for (Tribesperson t : level.getEntitiesOfClass(Tribesperson.class, new net.minecraft.world.phys.AABB(at).inflate(32)))
            if (t.home() != null && flat(t.home(), pos) <= Math.max(CAMP_LAND, t.campRadius())) return tribe(t.group());
        return null;
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
        if (land.kind() != HOME) {
            var holder = level.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/tribe_land"));
            if (holder != null) player.getAdvancements().award(holder, "done");
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    private Lands() {}
}
