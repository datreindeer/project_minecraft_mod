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
    public static final int TRADE = 1, YES_NO = 2;
    /** Answers. */
    public static final int CHOOSE_TRADE = 0, CHOOSE_YES = 1, CHOOSE_NO = 2;
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

    static void open(ServerPlayer player, Tribesperson envoy, Component text, int buttons) {
        PacketDistributor.sendToPlayer(player, new Open(envoy.getId(), envoy.getDisplayName(), text, buttons));
    }

    /** Server, main thread: a button was clicked. Anything stale or out of reach is ignored. */
    private static void onAnswer(Answer answer, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level().getEntity(answer.entity()) instanceof Tribesperson envoy) || !envoy.isAlive()
                || envoy.distanceToSqr(player) > REACH * REACH) return;
        answer(player, envoy, answer.choice());
    }

    /** What a click does (also used by tests): open the trade, or answer the leader's question. */
    static void answer(ServerPlayer player, Tribesperson envoy, int choice) {
        switch (choice) {
            case CHOOSE_TRADE -> envoy.openTrade(player);
            case CHOOSE_YES, CHOOSE_NO -> TribeVisits.answerStay(player, envoy, choice == CHOOSE_YES);
            default -> {}
        }
    }

    private TribeDialog() {}
}
