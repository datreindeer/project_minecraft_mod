package com.mechanical.client;

import java.util.List;

import com.mechanical.Kettle;
import com.mechanical.Mechanical;
import com.mechanical.TripodBlock;
import com.mechanical.TripodBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * What is in a hung Bronze Kettle: the water or stew surface (a greyscale ripple texture tinted with the pot's colour,
 * drifting slowly and faster while it boils), lower as bowls are ladled out, and up to {@link #CHUNKS} of the
 * ingredients floating in it, bobbing while it boils. The pot, tripod and chain are block models (gen_kettle.py).
 */
public class KettleRenderer implements BlockEntityRenderer<TripodBlockEntity, KettleRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/block/kettle_liquid.png");
    private static final RenderType TYPE = RenderTypes.entityTranslucent(TEXTURE);
    private static final int CHUNKS = 4;
    /** Where the chunks float, in pixels from the pot's middle, and how they are turned. */
    private static final float[][] SPOTS = {{-1.8F, -1.2F, 20}, {1.6F, 1.4F, 110}, {1.4F, -1.9F, 200}, {-1.5F, 1.8F, 300}};

    public static class State extends BlockEntityRenderState {
        boolean show, hot;
        float cx, cz, y, time;
        int colour;
        final ItemStackRenderState[] chunks = new ItemStackRenderState[CHUNKS];

        State() {
            for (int i = 0; i < CHUNKS; i++) chunks[i] = new ItemStackRenderState();
        }
    }

    private final ItemModelResolver resolver;

    public KettleRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TripodBlockEntity tripod, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tripod, state, partialTicks, camera, breakProgress);
        var block = tripod.getBlockState();
        state.show = block.getValue(TripodBlock.KETTLE) && tripod.water();
        state.hot = block.getValue(TripodBlock.HOT);
        state.cx = TripodBlock.centreX(block) / 16;
        state.cz = TripodBlock.centreZ(block) / 16;
        float lift = block.getValue(TripodBlock.PIT) > 0 ? TripodBlockEntity.PIT_LIFT : 0;
        state.y = (TripodBlockEntity.LIQUID_LOW + (TripodBlockEntity.LIQUID_HIGH - TripodBlockEntity.LIQUID_LOW) * tripod.fill() + lift) / 16;
        state.colour = tripod.colour();
        long now = tripod.getLevel() == null ? 0 : tripod.getLevel().getGameTime();
        state.time = (now % 2400) + partialTicks;
        List<ItemStack> contents = tripod.contents();
        int seed = (int) tripod.getBlockPos().asLong();
        for (int i = 0; i < CHUNKS; i++) {
            ItemStack s = state.show && i < contents.size() ? contents.get(contents.size() - 1 - i) : ItemStack.EMPTY; // the latest on top
            resolver.updateForTopItem(state.chunks[i], s, ItemDisplayContext.FIXED, tripod.getLevel(), null, seed + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.show) return;
        // the texture is a 16 px tile repeated 2x2, so wrapping the drift at half its size is seamless
        float h = TripodBlockEntity.INNER / 16, speed = state.time * (state.hot ? 0.004F : 0.0008F), du = speed % 0.5F, dv = speed * 0.6F % 0.5F;
        int argb = 0xE0000000 | state.colour;
        float x0 = state.cx - h, x1 = state.cx + h, z0 = state.cz - h, z1 = state.cz + h, y = state.y;
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, TYPE, (p, buf) -> {
            float[][] c = {{x0, z0, 0, 0}, {x0, z1, 0, 1}, {x1, z1, 1, 1}, {x1, z0, 1, 0}};
            for (float[] v : c) {
                buf.addVertex(p, v[0], y, v[1]).setColor(argb).setUv(v[2] * 0.5F + du, v[3] * 0.5F + dv).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(p, 0, 1, 0);
            }
        });
        for (int i = 0; i < CHUNKS; i++) {
            ItemStackRenderState chunk = state.chunks[i];
            if (chunk.isEmpty()) continue;
            float bob = state.hot ? (float) Math.sin(state.time * 0.15F + i * 1.7F) * 0.25F : 0;
            pose.pushPose();
            pose.translate(state.cx + SPOTS[i][0] / 16, state.y + (bob - 0.2F) / 16, state.cz + SPOTS[i][1] / 16);
            pose.rotateDegrees(Axis.YP, SPOTS[i][2] + (state.hot ? state.time * 0.6F : 0));
            pose.rotateDegrees(Axis.XP, 90); // lying in the liquid, half under
            pose.scale(0.28F, 0.28F, 0.28F);
            chunk.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /** Over a Fire Pit the pot hangs at the block's corner. */
    @Override
    public AABB getRenderBoundingBox(TripodBlockEntity tripod) {
        return new AABB(tripod.getBlockPos()).inflate(0.5, 0.5, 0.5);
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Kettle.TRIPOD_ENTITY.get(), KettleRenderer::new);
        }
    }
}
