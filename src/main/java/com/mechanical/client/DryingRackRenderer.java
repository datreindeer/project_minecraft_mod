package com.mechanical.client;

import com.mechanical.DryingRackBlock;
import com.mechanical.DryingRackBlockEntity;
import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The goods hanging on a Drying Rack (DryingRackBlockEntity): each spot's item flat against the rack like in an item frame,
 * a single rack's one spot in the middle, a tall rack's two per block left and right; a bigger pile shows a second item behind.
 */
public class DryingRackRenderer implements BlockEntityRenderer<DryingRackBlockEntity, DryingRackRenderer.State> {
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState()};
        final int[] counts = new int[2];
        Direction facing = Direction.NORTH;
        boolean single;
    }

    private final ItemModelResolver resolver;

    public DryingRackRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DryingRackBlockEntity rack, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rack, state, partialTicks, camera, breakProgress);
        var block = rack.getBlockState();
        state.facing = block.hasProperty(DryingRackBlock.FACING) ? block.getValue(DryingRackBlock.FACING) : Direction.NORTH;
        state.single = !block.hasProperty(DryingRackBlock.PART) || block.getValue(DryingRackBlock.PART) == DryingRackBlock.Part.SINGLE;
        for (int i = 0; i < 2; i++) {
            resolver.updateForTopItem(state.items[i], rack.item(i), ItemDisplayContext.FIXED, rack.getLevel(), null, (int) rack.getBlockPos().asLong() + i);
            state.counts[i] = rack.item(i).getCount();
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction right = state.facing.getClockWise();
        for (int i = 0; i < 2; i++) {
            if (state.items[i].isEmpty()) continue;
            // the spot's side as in DryingRackBlock.useItemOn: HIDE on the left (-right), HIDE2 on the right
            float side = state.single ? 0 : i == 0 ? -0.25F : 0.25F;
            for (int copy = 0; copy < (state.counts[i] > 1 ? 2 : 1); copy++) {
                pose.pushPose();
                pose.translate(0.5F + right.getStepX() * side, 0.55F - copy * 0.12F, 0.5F + right.getStepZ() * side);
                pose.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
                pose.translate(0, 0, copy == 0 ? 0.04F : -0.04F); // one on each face of the rack
                pose.rotateDegrees(Axis.ZP, copy * 20.0F);
                float s = state.single ? 0.55F : 0.42F;
                pose.scale(s, s, s);
                state.items[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Mechanical.DRYING_RACK_ENTITY.get(), DryingRackRenderer::new);
        }
    }
}
