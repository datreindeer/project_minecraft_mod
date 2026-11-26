package com.mechanical.viewer;

import java.util.List;

import com.mechanical.Mechanical;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server -> client: the datapack recipes the recipe viewer shows. Sent on join and after every reload; the client side is {@code ClientCatalog}. */
@EventBusSubscriber(modid = Mechanical.MODID)
public record CatalogPayload(List<ViewerRecipe> recipes) implements CustomPacketPayload {
    public static final Type<CatalogPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "recipe_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CatalogPayload> CODEC = StreamCodec.composite(
            ViewerRecipe.CODEC.apply(ByteBufCodecs.list()), CatalogPayload::recipes, CatalogPayload::new);

    @Override
    public Type<CatalogPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC);
    }

    @SubscribeEvent
    static void onSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> players = event.getRelevantPlayers().toList();
        if (players.isEmpty()) return;
        var server = event.getPlayerList().getServer();
        var payload = new CatalogPayload(JsonSources.from(server.getRecipeManager(), server.registryAccess()));
        players.forEach(p -> PacketDistributor.sendToPlayer(p, payload));
    }
}
