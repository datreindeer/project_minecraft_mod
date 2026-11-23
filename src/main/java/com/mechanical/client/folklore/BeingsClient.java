package com.mechanical.client.folklore;

import java.util.function.Predicate;

import com.mechanical.Mechanical;
import com.mechanical.client.folklore.BeingModel.Kind;
import com.mechanical.folklore.Beings;
import com.mechanical.folklore.FrostGiant;
import com.mechanical.folklore.Spirit;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The folklore beings on the client (folklore.Beings): each its own model ({@link BeingModel}, boxes and skins from
 * scripts/gen_folklore_models.py). Spirits are drawn only for players whose sight reaches the being's
 * ({@link Spirit#seenBy}); the Dune Wraith see-through and only in a storm; the wisp a soft emissive orb (its particles,
 * Wisp.tick, round it). The Bog Body and Frost Giant are real and everyone sees them; the giant's eyes glow. Lost: the
 * view drifts slowly round, so you walk in circles.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BeingsClient {
    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/folklore/" + name + ".png");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Beings.HULDRA.get(), c -> new BeingRenderer<>(c, Kind.HULDRA, "huldra", 1.0F, 0.0F, s -> true));
        event.registerEntityRenderer(Beings.TOMTE.get(), c -> new BeingRenderer<>(c, Kind.TOMTE, "tomte", 0.9F, 0.0F, s -> true));
        event.registerEntityRenderer(Beings.NACKEN.get(), c -> new BeingRenderer<>(c, Kind.NACKEN, "nacken", 1.0F, 0.0F, s -> true));
        event.registerEntityRenderer(Beings.DUNE_WRAITH.get(), c -> new BeingRenderer<>(c, Kind.DUNE_WRAITH, "dune_wraith", 1.0F, 0.0F, s -> s.level().isThundering()));
        event.registerEntityRenderer(Beings.JUNGLE_SPIRIT_MASK.get(), c -> new BeingRenderer<>(c, Kind.MASK, "jungle_spirit_mask", 1.0F, 0.0F, s -> true));
        event.registerEntityRenderer(Beings.WILL_O_WISP.get(), c -> new BeingRenderer<>(c, Kind.WISP, "will_o_wisp", 1.0F, 0.0F, s -> true));
        event.registerEntityRenderer(Beings.BOG_BODY.get(), c -> new BeingRenderer<>(c, Kind.BOG_BODY, "bog_body", 1.0F, 0.5F, s -> true));
        event.registerEntityRenderer(Beings.FROST_GIANT.get(), c -> new BeingRenderer<FrostGiant>(c, Kind.FROST_GIANT, "frost_giant", 2.0F, 1.4F, s -> true)
                .glowing(tex("frost_giant_glow")));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Kind k : Kind.values()) event.registerLayerDefinition(k.layer, k.geometry);
    }

    /** A being drawn {@code size} times its model; a spirit only for those who see it, and only {@code when}. */
    static class BeingRenderer<T extends Mob> extends MobRenderer<T, BeingModel.State, BeingModel> {
        private final Identifier texture;
        private final float size;
        private final Predicate<T> when;

        BeingRenderer(EntityRendererProvider.Context c, Kind kind, String name, float size, float shadow, Predicate<T> when) {
            super(c, new BeingModel(c.bakeLayer(kind.layer), kind), shadow);
            this.texture = tex(name);
            this.size = size;
            this.when = when;
        }

        /** Adds a layer drawn full bright (the frost giant's eyes). */
        BeingRenderer<T> glowing(Identifier glow) {
            RenderType eyes = RenderTypes.eyes(glow);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return eyes;
                }
            });
            return this;
        }

        @Override
        public BeingModel.State createRenderState() {
            return new BeingModel.State();
        }

        @Override
        public void extractRenderState(T entity, BeingModel.State state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.aggressive = entity.isAggressive();
        }

        @Override
        public Identifier getTextureLocation(BeingModel.State state) {
            return texture;
        }

        @Override
        public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
            if (entity instanceof Spirit spirit && !spirit.seenBy(Minecraft.getInstance().player)) return false;
            return when.test(entity) && super.shouldRender(entity, frustum, camX, camY, camZ, partialTicks);
        }

        @Override
        protected void scale(BeingModel.State state, PoseStack poseStack) {
            poseStack.scale(size, size, size);
        }
    }

    /** Lost: the view turns slowly and unevenly, so a straight walk bends into a circle. */
    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.isPaused() || !p.hasEffect(Beings.LOST)) return;
        p.setYRot(p.getYRot() + 0.25F + Mth.sin(p.tickCount * 0.02F) * 0.6F);
    }

    private BeingsClient() {}
}
