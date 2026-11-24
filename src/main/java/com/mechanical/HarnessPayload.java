package com.mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client -> server: the grab key was pressed. Grab or release the travois, cart, plough or wagon looked at ({@link TravoisEntity#grabKey}). */
@EventBusSubscriber(modid = Mechanical.MODID)
public record HarnessPayload() implements CustomPacketPayload {
    public static final Type<HarnessPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "harness"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HarnessPayload> CODEC = StreamCodec.unit(new HarnessPayload());

    @Override
    public Type<HarnessPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> TravoisEntity.grabKey(context.player()));
    }
}
