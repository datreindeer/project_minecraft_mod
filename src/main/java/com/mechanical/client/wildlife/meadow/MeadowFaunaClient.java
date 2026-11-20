package com.mechanical.client.wildlife.meadow;

import com.google.common.reflect.TypeToken;
import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.meadow.GrassSnake;
import com.mechanical.wildlife.meadow.MeadowFauna;
import com.mechanical.wildlife.meadow.Mole;
import com.mechanical.wildlife.meadow.Snake;
import com.mechanical.wildlife.meadow.WildCat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Renderers, model layers and render-state extras for the meadow fauna (MeadowFauna). */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class MeadowFaunaClient {
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // the young (kitten, vole pups) are the adult model scaled down; the kitten has its own texture
        event.registerEntityRenderer(MeadowFauna.WILD_CAT.get(), c -> {
            WildCatModel model = new WildCatModel(c.bakeLayer(WildCatModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.55F, 0.35F);
        });
        event.registerEntityRenderer(MeadowFauna.MOLE.get(), c -> {
            MoleModel model = new MoleModel(c.bakeLayer(MoleModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.2F);
        });
        event.registerEntityRenderer(MeadowFauna.FIELD_VOLE.get(), c -> {
            FieldVoleModel model = new FieldVoleModel(c.bakeLayer(FieldVoleModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 0.6F, 0.15F);
        });
        event.registerEntityRenderer(MeadowFauna.ADDER.get(), c -> {
            SnakeModel model = new SnakeModel(c.bakeLayer(SnakeModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.2F);
        });
        event.registerEntityRenderer(MeadowFauna.GRASS_SNAKE.get(), c -> {
            SnakeModel model = new SnakeModel(c.bakeLayer(SnakeModel.LAYER));
            return new WildAnimalRenderer<>(c, model, model, 1.0F, 0.2F);
        });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WildCatModel.LAYER, WildCatModel::createBodyLayer);
        event.registerLayerDefinition(MoleModel.LAYER, MoleModel::createBodyLayer);
        event.registerLayerDefinition(FieldVoleModel.LAYER, FieldVoleModel::createBodyLayer);
        event.registerLayerDefinition(SnakeModel.LAYER, SnakeModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof WildCat cat) {
                state.crouching = cat.isStalking();
                state.leaping = !cat.onGround() && cat.isAggressive();
            } else if (animal instanceof Mole mole) {
                if (mole.digProgress() > 0) state.setRenderData(MoleModel.DIG, mole.digProgress());
            } else if (animal instanceof Snake snake) {
                if (snake.isCoiled()) state.setRenderData(SnakeModel.COILED, true);
                if (snake instanceof GrassSnake grass && grass.isPlayingDead()) state.setRenderData(SnakeModel.BELLY_UP, true);
            }
        });
    }

    private MeadowFaunaClient() {}
}
