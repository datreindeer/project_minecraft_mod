package com.mechanical.client.folklore;

import java.util.function.Predicate;

import com.mechanical.Mechanical;
import com.mechanical.folklore.Beings;
import com.mechanical.folklore.Spirit;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The folklore beings on the client (folklore.Beings): humanoid builds (the zombie layer, 64x64 skins from
 * scripts/gen_folklore_textures.py), drawn only for players whose sight reaches the being's ({@link Spirit#seenBy}); the
 * Dune Wraith see-through and only in a storm; the Jungle Spirit Mask just a floating head; the wisp no model at all
 * (its light is particles, Wisp.tick). The Bog Body and Frost Giant are real and everyone sees them. Lost: the view
 * drifts slowly round, so you walk in circles.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BeingsClient {
    static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/folklore/" + name + ".png");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Beings.HULDRA.get(), c -> new SpiritRenderer<>(c, "huldra", 1.0F, s -> true, false, false));
        event.registerEntityRenderer(Beings.TOMTE.get(), c -> new SpiritRenderer<>(c, "tomte", 0.45F, s -> true, false, false));
        event.registerEntityRenderer(Beings.NACKEN.get(), c -> new SpiritRenderer<>(c, "nacken", 1.0F, s -> true, false, false));
        event.registerEntityRenderer(Beings.DUNE_WRAITH.get(), c -> new SpiritRenderer<>(c, "dune_wraith", 1.05F, s -> s.level().isThundering(), true, false));
        event.registerEntityRenderer(Beings.JUNGLE_SPIRIT_MASK.get(), c -> new SpiritRenderer<>(c, "jungle_spirit_mask", 1.0F, s -> true, false, true));
        event.registerEntityRenderer(Beings.WILL_O_WISP.get(), NoopRenderer::new);
        event.registerEntityRenderer(Beings.BOG_BODY.get(), c -> new UndeadRenderer<>(c, "bog_body", 1.0F));
        event.registerEntityRenderer(Beings.FROST_GIANT.get(), c -> new UndeadRenderer<>(c, "frost_giant", 2.6F));
    }

    /** A spirit in human shape. {@code headOnly}: only the head is drawn, hovering (the mask). */
    static class SpiritRenderer<T extends Spirit> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
        private final Identifier texture;
        private final float size;
        private final Predicate<T> when;
        private final boolean seeThrough, headOnly;

        SpiritRenderer(EntityRendererProvider.Context c, String name, float size, Predicate<T> when, boolean seeThrough, boolean headOnly) {
            super(c, model(c, headOnly), 0.0F);
            this.texture = tex(name);
            this.size = size;
            this.when = when;
            this.seeThrough = seeThrough;
            this.headOnly = headOnly;
        }

        private static HumanoidModel<HumanoidRenderState> model(EntityRendererProvider.Context c, boolean headOnly) {
            return new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE)) {
                @Override
                public void setupAnim(HumanoidRenderState state) {
                    super.setupAnim(state);
                    body.visible = rightArm.visible = leftArm.visible = rightLeg.visible = leftLeg.visible = !headOnly;
                }
            };
        }

        @Override
        public HumanoidRenderState createRenderState() {
            return new HumanoidRenderState();
        }

        @Override
        public Identifier getTextureLocation(HumanoidRenderState state) {
            return texture;
        }

        @Override
        public boolean shouldRender(T spirit, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
            return spirit.seenBy(Minecraft.getInstance().player) && when.test(spirit) && super.shouldRender(spirit, frustum, camX, camY, camZ, partialTicks);
        }

        @Override
        protected RenderType getRenderType(HumanoidRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
            return seeThrough ? RenderTypes.entityTranslucent(texture) : super.getRenderType(state, bodyVisible, translucent, glowing);
        }

        @Override
        protected void scale(HumanoidRenderState state, PoseStack poseStack) {
            poseStack.scale(size, size, size);
            if (headOnly) poseStack.translate(0, 1.15 + Mth.sin(state.ageInTicks * 0.08F) * 0.08, 0); // the head down to where the small body is
        }
    }

    /** The real undead in human shape: zombie arms out when hunting. */
    static class UndeadRenderer<T extends Monster> extends HumanoidMobRenderer<T, ZombieRenderState, ZombieModel<ZombieRenderState>> {
        private final Identifier texture;
        private final float size;

        UndeadRenderer(EntityRendererProvider.Context c, String name, float size) {
            super(c, new ZombieModel<>(c.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * size);
            this.texture = tex(name);
            this.size = size;
        }

        @Override
        public ZombieRenderState createRenderState() {
            return new ZombieRenderState();
        }

        @Override
        public void extractRenderState(T entity, ZombieRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.isAggressive = entity.isAggressive();
        }

        @Override
        public Identifier getTextureLocation(ZombieRenderState state) {
            return texture;
        }

        @Override
        protected void scale(ZombieRenderState state, PoseStack poseStack) {
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
