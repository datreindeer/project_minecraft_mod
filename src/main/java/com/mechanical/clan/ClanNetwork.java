package com.mechanical.clan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mechanical.Mechanical;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The tribe screen's traffic. The client asks ({@link Request}, the G key) and clicks ({@link Action}); the server checks
 * every click against the rules in {@link Clans} and answers with a {@link View} of what this player may see and do.
 * After any change every online player gets a fresh view; a client only uses it while the screen is open.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ClanNetwork {
    public static final int FOUND = 0, CLAIM_PIT = 1, INVITE = 2, ACCEPT = 3, DECLINE = 4, LEAVE = 5, KICK = 6, PROMOTE = 7,
            DEMOTE = 8, HAND_OVER = 9, VOTE_DISBAND = 10, VOTE_MOVE = 11, VOTE_LEADER = 12, YES = 13, NO = 14;
    public static final UUID NONE = new UUID(0, 0);

    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "clan_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Action(int action, UUID target, String text) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "clan_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::action, UUIDUtil.STREAM_CODEC, Action::target,
                ByteBufCodecs.stringUtf8(64), Action::text, Action::new);

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    public record Row(UUID id, String name, int rank, boolean online) {}

    public record Entry(UUID id, String name) {}

    /**
     * What one player sees. {@code voteKind} -1 when no vote runs; {@code myBallot} -1 not voted, 0 no, 1 yes.
     * {@code open}: open the screen (an answer to the G key) rather than only refresh it.
     */
    public record View(boolean open, boolean inClan, String name, int age, int myRank, boolean hasPit, boolean canPit,
                       List<Row> members, int voteKind, String voteWho, int yes, int no, int voters, long daysLeft, int myBallot,
                       boolean canLeaderVote, List<Entry> invites, List<Entry> invitable) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "clan_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.of(View::write, View::read);

        private static void write(RegistryFriendlyByteBuf b, View v) {
            b.writeBoolean(v.open);
            b.writeBoolean(v.inClan);
            b.writeUtf(v.name);
            b.writeVarInt(v.age);
            b.writeVarInt(v.myRank);
            b.writeBoolean(v.hasPit);
            b.writeBoolean(v.canPit);
            b.writeVarInt(v.members.size());
            for (Row r : v.members) {
                b.writeUUID(r.id);
                b.writeUtf(r.name);
                b.writeVarInt(r.rank);
                b.writeBoolean(r.online);
            }
            b.writeVarInt(v.voteKind);
            b.writeUtf(v.voteWho);
            b.writeVarInt(v.yes);
            b.writeVarInt(v.no);
            b.writeVarInt(v.voters);
            b.writeVarLong(v.daysLeft);
            b.writeVarInt(v.myBallot);
            b.writeBoolean(v.canLeaderVote);
            writeEntries(b, v.invites);
            writeEntries(b, v.invitable);
        }

        private static View read(RegistryFriendlyByteBuf b) {
            return new View(b.readBoolean(), b.readBoolean(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(),
                    readRows(b),
                    b.readVarInt(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarInt(),
                    b.readBoolean(), readEntries(b), readEntries(b));
        }

        private static void writeEntries(RegistryFriendlyByteBuf b, List<Entry> entries) {
            b.writeVarInt(entries.size());
            for (Entry e : entries) {
                b.writeUUID(e.id);
                b.writeUtf(e.name);
            }
        }

        private static List<Entry> readEntries(RegistryFriendlyByteBuf b) {
            List<Entry> out = new ArrayList<>();
            for (int n = b.readVarInt(); n > 0; n--) out.add(new Entry(b.readUUID(), b.readUtf()));
            return out;
        }

        private static List<Row> readRows(RegistryFriendlyByteBuf b) {
            List<Row> out = new ArrayList<>();
            for (int n = b.readVarInt(); n > 0; n--) out.add(new Row(b.readUUID(), b.readUtf(), b.readVarInt(), b.readBoolean()));
            return out;
        }

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
                .playToServer(Request.TYPE, Request.CODEC, (p, ctx) -> {
                    if (ctx.player() instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, view(sp, true));
                })
                .playToServer(Action.TYPE, Action.CODEC, ClanNetwork::onAction)
                .playToClient(View.TYPE, View.CODEC);
    }

    private static void onAction(Action a, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        Clans data = Clans.get(player.level().getServer());
        boolean changed = switch (a.action()) {
            case FOUND -> data.found(player, a.text());
            case CLAIM_PIT -> data.claimPit(player);
            case INVITE -> data.invite(player, a.target());
            case ACCEPT -> data.accept(player, a.target());
            case DECLINE -> data.decline(player, a.target());
            case LEAVE -> data.leave(player);
            case KICK -> data.kick(player, a.target());
            case PROMOTE -> data.promote(player, a.target());
            case DEMOTE -> data.demote(player, a.target());
            case HAND_OVER -> data.handOver(player, a.target());
            case VOTE_DISBAND -> data.startVote(player, Clans.VOTE_DISBAND, null);
            case VOTE_MOVE -> data.startVote(player, Clans.VOTE_MOVE, null);
            case VOTE_LEADER -> data.startVote(player, Clans.VOTE_LEADER, a.target());
            case YES -> data.ballot(player, true);
            case NO -> data.ballot(player, false);
            default -> false;
        };
        if (changed) refreshAll(player.level().getServer());
        else PacketDistributor.sendToPlayer(player, view(player, false));
    }

    /** Send every online player a fresh view (players are few; only open screens use it). */
    static void refreshAll(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(sp, view(sp, false));
    }

    static View view(ServerPlayer player, boolean open) {
        MinecraftServer server = player.level().getServer();
        Clans data = Clans.get(server);
        Clans.Clan clan = data.clanOf(player.getUUID());
        List<Entry> invites = new ArrayList<>();
        data.invitesFor(player.getUUID()).forEach((id, name) -> invites.add(new Entry(id, name)));
        if (clan == null) {
            return new View(open, false, "", 0, -1, false, data.freePitNear(player) != null, List.of(), -1, "", 0, 0, 0, 0, -1,
                    false, invites, List.of());
        }
        List<Row> rows = new ArrayList<>();
        for (Clans.Member m : clan.members) rows.add(new Row(m.id, m.name, m.rank, server.getPlayerList().getPlayer(m.id) != null));
        rows.sort((x, y) -> x.rank() != y.rank() ? Integer.compare(x.rank(), y.rank()) : x.name().compareToIgnoreCase(y.name()));
        int myRank = clan.member(player.getUUID()).rank;
        List<Entry> invitable = new ArrayList<>();
        if (myRank <= Clans.ELDER) for (ServerPlayer sp : server.getPlayerList().getPlayers())
            if (data.clanOf(sp.getUUID()) == null) invitable.add(new Entry(sp.getUUID(), sp.getGameProfile().name()));
        int kind = -1, yes = 0, no = 0, voters = 0, ballot = -1;
        long left = 0;
        String who = "";
        if (clan.vote.isPresent()) {
            Clans.Vote v = clan.vote.get();
            kind = v.kind;
            Clans.Member c = clan.member(v.candidate);
            who = c == null ? "?" : c.name;
            yes = Clans.count(clan, v, true);
            no = Clans.count(clan, v, false);
            voters = Clans.electorate(clan, v);
            left = Math.max(0, v.endsDay - Clans.today(server));
            Boolean mine = v.ballots.get(player.getUUID());
            ballot = mine == null ? -1 : mine ? 1 : 0;
        }
        boolean canPit = clan.pit.isEmpty() && myRank == Clans.LEADER && data.freePitNear(player) != null;
        return new View(open, true, clan.name, clan.age(), myRank, clan.pit.isPresent(), canPit, rows, kind, who, yes, no, voters, left,
                ballot, data.canCallLeaderVote(server, clan), invites, invitable);
    }

    private ClanNetwork() {}
}
