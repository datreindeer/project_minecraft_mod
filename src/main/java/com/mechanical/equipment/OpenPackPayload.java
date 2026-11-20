package com.mechanical.equipment;

import com.mechanical.Mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client -> server: the B key was pressed, open what is worn on the back and at the hip ({@link PackMenu#openWorn}). */
@EventBusSubscriber(modid = Mechanical.MODID)
public record OpenPackPayload() implements CustomPacketPayload {
    public static final Type<OpenPackPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "open_pack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPackPayload> CODEC = StreamCodec.unit(new OpenPackPayload());

    @Override
    public Type<OpenPackPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> {
            if (!context.player().isSpectator() && context.player().isAlive()) PackMenu.openWorn(context.player());
        });
    }
}
