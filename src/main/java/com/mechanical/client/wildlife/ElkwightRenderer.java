package com.mechanical.client.wildlife;

import com.mechanical.Mechanical;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.wildlife.elkwight.ElkwightAxeEntity;
import com.mechanical.wildlife.elkwight.Elkwight;
import com.mechanical.wildlife.elkwight.Elkwights;
import com.mechanical.wildlife.elkwight.ThrownElkAxe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The Elkwight drawn {@link #SCALE} times his model (3.6 blocks tall, the crown 4 wide), with the glow layer (eyes, axe
 * runes); his action, its progress, the axe in hand and whether he is awake go to the model as render data. Both axe
 * entities draw the trophy block's model ({@link AxeRenderer}): spinning in flight, stuck at a slant, or lying flat.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public class ElkwightRenderer extends WildAnimalRenderer<Elkwight> {
    static final float SCALE = 1.8F;
    /** Dev (BossShots): every Elkwight drawn at this point of his action, null for the real one. */
    public static Float shotProgress;
    static final ContextKey<Integer> ACTION = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_action"));
    static final ContextKey<Float> PROGRESS = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_progress"));
    static final ContextKey<Boolean> HAS_AXE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_axe"));
    static final ContextKey<Boolean> AWAKE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "elkwight_awake"));

    public ElkwightRenderer(EntityRendererProvider.Context c) {
        this(c, new ElkwightModel(c.bakeLayer(ElkwightModel.LAYER)));
    }

    private ElkwightRenderer(EntityRendererProvider.Context c, ElkwightModel model) {
        super(c, model, model, 1.0F, 1.4F);
        addLayer(new AngryEyesLayer(this, Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/elkwight_glow.png"),
                state -> !CarcassRenderer.isCarcass(state) && !CarcassRenderer.isHung(state)));
    }


    @Override
    protected void scale(WildAnimalRenderState state, PoseStack pose) {
        super.scale(state, pose);
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected boolean shouldShowName(Elkwight entity, double distanceSqr) {
        return false;
    }

    /** The axe as the trophy block's model: in flight it spins end over end, stuck it leans in at a slant, dropped it lies flat. */
    public static class AxeRenderer<T extends Entity> extends EntityRenderer<T, AxeRenderer.State> {
        public static class State extends EntityRenderState {
            final MovingBlockRenderState block = new MovingBlockRenderState();
            float yaw, spin;
            boolean flying, stuck;
        }

        public AxeRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(T entity, State state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.block.blockState = Elkwights.AXE_TROPHY.get().defaultBlockState();
            state.block.blockPos = entity.blockPosition().above();
            state.block.randomSeedPos = entity.blockPosition();
            if (entity.level() instanceof ClientLevel level) {
                state.block.biome = level.getBiome(entity.blockPosition());
                state.block.cardinalLighting = level.cardinalLighting();
                state.block.lightEngine = level.getLightEngine();
            }
            state.yaw = entity.getViewYRot(partialTicks);
            state.stuck = entity instanceof ThrownElkAxe t && t.isStuck();
            state.flying = entity instanceof ThrownElkAxe && !state.stuck;
            state.spin = (entity.tickCount + partialTicks) * 40.0F;
        }

        @Override
        public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            pose.pushPose();
            pose.rotateDegrees(Axis.YP, -state.yaw);
            if (state.flying) { // end over end round its middle
                pose.translate(0, 0.6, 0);
                pose.rotateDegrees(Axis.XP, state.spin);
                pose.translate(0, -0.9, 0);
            } else if (state.stuck) { // a bit buried, the haft sticking out behind
                pose.rotateDegrees(Axis.XP, -120);
                pose.translate(0, -1.6, 0);
            } else { // lying on its side, the haft along its yaw, the head beside it
                pose.translate(0, 0.08, 0);
                pose.rotateDegrees(Axis.XP, 90);
                pose.rotateDegrees(Axis.ZP, 90);
                pose.translate(0, -1.0, 0);
            }
            pose.translate(-0.5, 0, -0.5);
            collector.submitMovingBlock(pose, state.block, state.outlineColor);
            pose.popPose();
            super.submit(state, pose, collector, camera);
        }
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Elkwights.ELKWIGHT.get(), ElkwightRenderer::new);
        event.registerEntityRenderer(Elkwights.THROWN_AXE.get(), AxeRenderer<ThrownElkAxe>::new);
        event.registerEntityRenderer(Elkwights.AXE.get(), AxeRenderer<ElkwightAxeEntity>::new);
        CarcassRenderer.ON_BACK.add(Elkwights.ELKWIGHT.get());
    }

    /** Render data must be set by a modifier: NeoForge clears it after extractRenderState. */
    @SubscribeEvent
    static void renderStateModifiers(net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new com.google.common.reflect.TypeToken<WildAnimalRenderer<com.mechanical.wildlife.WildAnimal>>() {},
                (com.mechanical.wildlife.WildAnimal animal, WildAnimalRenderState state) -> {
                    if (!(animal instanceof Elkwight e)) return;
                    state.setRenderData(ACTION, e.action());
                    state.setRenderData(PROGRESS, shotProgress != null ? shotProgress : e.actionProgress(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
                    state.setRenderData(HAS_AXE, e.hasAxe());
                    state.setRenderData(AWAKE, e.isAwake());
                });
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ElkwightModel.LAYER, ElkwightModel::createBodyLayer);
    }
}
