package com.mechanical.client.wildlife.river;

import com.google.common.reflect.TypeToken;
import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.river.Beaver;
import com.mechanical.wildlife.river.Otter;
import com.mechanical.wildlife.river.RiverFauna;
import com.mechanical.wildlife.river.SemiAquatic;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Renderers, model layers and the extra render state (swimming, carrying, gnawing, fish, eating) of the river fauna. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class RiverFaunaClient {
    static final ContextKey<Boolean> SWIMMING = key("river_swimming"), CARRYING = key("beaver_carrying"), GNAWING = key("beaver_gnawing"),
            FISH = key("otter_fish"), EATING = key("otter_eating");

    private static ContextKey<Boolean> key(String name) {
        return new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
    }

    static boolean flag(WildAnimalRenderState state, ContextKey<Boolean> key) {
        return Boolean.TRUE.equals(state.getRenderData(key));
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // the young are the adult model scaled down
        event.registerEntityRenderer(RiverFauna.BEAVER.get(), c -> {
            BeaverModel model = new BeaverModel(c.bakeLayer(BeaverModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.4F);
        });
        event.registerEntityRenderer(RiverFauna.OTTER.get(), c -> {
            OtterModel model = new OtterModel(c.bakeLayer(OtterModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.35F);
        });
        event.registerEntityRenderer(RiverFauna.MOSQUITO_SWARM.get(), MosquitoSwarmRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BeaverModel.LAYER, BeaverModel::createBodyLayer);
        event.registerLayerDefinition(OtterModel.LAYER, OtterModel::createBodyLayer);
        event.registerLayerDefinition(MosquitoSwarmModel.LAYER, MosquitoSwarmModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof SemiAquatic a && a.isSwimmingNow()) state.setRenderData(SWIMMING, true);
            if (animal instanceof Beaver b) {
                if (b.isCarrying()) state.setRenderData(CARRYING, true);
                if (b.isGnawing()) state.setRenderData(GNAWING, true);
            }
            if (animal instanceof Otter o) {
                if (o.isHoldingFish()) state.setRenderData(FISH, true);
                if (o.isEating()) state.setRenderData(EATING, true);
            }
        });
    }

    private RiverFaunaClient() {}
}
