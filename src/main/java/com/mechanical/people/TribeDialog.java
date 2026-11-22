package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The talk screen of a tribe's envoy (a visiting band's leader, or a trader from a camp nearby): the server sends
 * {@link Open} (who, what they say, which buttons), the client shows client.people.TribeDialogScreen, and a click comes
 * back as {@link Answer}, which the server checks again (the envoy still there and close, the question still open), so
 * on a server whoever answers first decides.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TribeDialog {
    /** Button bits. Farewell is always there. */
    public static final int TRADE = 1, YES_NO = 2, ROAD = 4, UPGRADE = 8;
    /** Answers. */
    public static final int CHOOSE_TRADE = 0, CHOOSE_YES = 1, CHOOSE_NO = 2, CHOOSE_ROAD = 3, CHOOSE_UPGRADE = 4;
    static final double REACH = 10;

    public record Open(int entity, Component name, Component text, int buttons) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_dialog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::entity, ComponentSerialization.TRUSTED_STREAM_CODEC, Open::name,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Open::text, ByteBufCodecs.VAR_INT, Open::buttons, Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    public record Answer(int entity, int choice) implements CustomPacketPayload {
        public static final Type<Answer> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_answer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Answer> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Answer::entity, ByteBufCodecs.VAR_INT, Answer::choice, Answer::new);

        @Override
        public Type<Answer> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Open.TYPE, Open.CODEC).playToServer(Answer.TYPE, Answer.CODEC, TribeDialog::onAnswer);
    }

    /** The dialog last sent to each player (envoy entity id, button bits): an answer must match it (no forged choices). */
    private record Offered(int entity, int buttons) {}
    private static final java.util.Map<ServerPlayer, Offered> OFFERED = new java.util.WeakHashMap<>();

    static void open(ServerPlayer player, Tribesperson envoy, Component text, int buttons) {
        OFFERED.put(player, new Offered(envoy.getId(), buttons));
        PacketDistributor.sendToPlayer(player, new Open(envoy.getId(), envoy.getDisplayName(), text, buttons));
    }

    /** Was this choice on the screen the server last opened for this player, for this envoy? */
    static boolean wasOffered(ServerPlayer player, int entity, int choice) {
        Offered o = OFFERED.get(player);
        if (o == null || o.entity() != entity) return false;
        return switch (choice) {
            case CHOOSE_TRADE -> (o.buttons() & TRADE) != 0;
            case CHOOSE_YES, CHOOSE_NO -> (o.buttons() & YES_NO) != 0;
            case CHOOSE_ROAD -> (o.buttons() & ROAD) != 0;
            case CHOOSE_UPGRADE -> (o.buttons() & UPGRADE) != 0;
            default -> false;
        };
    }

    /** Server, main thread: a button was clicked. Anything stale or out of reach is ignored. */
    private static void onAnswer(Answer answer, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level().getEntity(answer.entity()) instanceof Tribesperson envoy) || !envoy.isAlive()
                || envoy.distanceToSqr(player) > REACH * REACH || !wasOffered(player, answer.entity(), answer.choice())) return;
        OFFERED.remove(player); // one answer per opened dialog
        answer(player, envoy, answer.choice());
    }

    /** What a click does (also used by tests): open the trade, or answer the leader's question. */
    static void answer(ServerPlayer player, Tribesperson envoy, int choice) {
        switch (choice) {
            case CHOOSE_TRADE -> envoy.openTrade(player);
            case CHOOSE_YES, CHOOSE_NO -> TribeVisits.answerStay(player, envoy, choice == CHOOSE_YES);
            case CHOOSE_ROAD -> RoadBuilding.askRoad(player, envoy);
            case CHOOSE_UPGRADE -> RoadBuilding.askUpgrade(player, envoy);
            default -> {}
        }
    }

    private TribeDialog() {}
}
