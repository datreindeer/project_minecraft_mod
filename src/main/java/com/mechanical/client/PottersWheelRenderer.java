package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Pottery;
import com.mechanical.PottersWheelBlockEntity;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The turning parts of a Potter's Wheel: the plank wheel head and the clay on it. The clay goes lump -> centred ->
 * raised -> opened as the throwing comes along, then the finished greenware. The stage models are block models drawn
 * through item model definitions (assets/mechanical/items/wheel_*.json) set on a stand-in stack, so they need no items.
 */
public class PottersWheelRenderer implements BlockEntityRenderer<PottersWheelBlockEntity, PottersWheelRenderer.State> {
    /** Top of the wheel head, in blocks. */
    private static final float HEAD_TOP = 12 / 16.0F;
    /** Degrees per tick while turning (about 1.3 turns a second). */
    private static final float SPEED = 24.0F;
    // made on first use: an ItemStack can't be built while the class loads during resource reload (data components aren't bound yet)
    private static ItemStack HEAD, LUMP, CENTRED, RAISED, OPENED;

    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState head = new ItemStackRenderState(), clay = new ItemStackRenderState();
        float angle;
        boolean finished;
    }

    private final ItemModelResolver items;

    public PottersWheelRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    private static ItemStack model(String name) {
        ItemStack stack = new ItemStack(Items.CLAY_BALL);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
        return stack;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PottersWheelBlockEntity wheel, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(wheel, state, partialTicks, camera, breakProgress);
        long now = wheel.getLevel() == null ? 0 : wheel.getLevel().getGameTime();
        if (wheel.spinning(now)) wheel.lastAngle = ((now % 360) + partialTicks) * SPEED % 360;
        state.angle = wheel.lastAngle;
        int seed = (int) wheel.getBlockPos().asLong();
        if (HEAD == null) {
            HEAD = model("wheel_head");
            LUMP = model("wheel_clay_lump");
            CENTRED = model("wheel_clay_centred");
            RAISED = model("wheel_clay_raised");
            OPENED = model("wheel_clay_opened");
        }
        items.updateForTopItem(state.head, HEAD, ItemDisplayContext.NONE, wheel.getLevel(), null, seed);
        float t = wheel.steps() / (float) PottersWheelBlockEntity.THROW_STEPS;
        state.finished = wheel.done();
        ItemStack clay = wheel.clay() == 0 ? ItemStack.EMPTY : wheel.done() ? new ItemStack(wheel.shape().greenware.get())
                : wheel.steps() == 0 ? LUMP : t < 1 / 3F ? CENTRED : t < 2 / 3F ? RAISED : OPENED;
        items.updateForTopItem(state.clay, clay, ItemDisplayContext.NONE, wheel.getLevel(), null, seed);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.rotateDegrees(Axis.YP, state.angle);
        pose.translate(0.0F, 0.5F, 0.0F); // the item transform shifts the model back by half a block
        state.head.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (!state.clay.isEmpty()) {
            pose.translate(0.0F, HEAD_TOP - 0.5F, 0.0F);
            // the finished piece is its full-size block model, scaled down to sit on the head
            float s = state.finished ? 0.6F : 1.0F;
            pose.scale(s, s, s);
            pose.translate(0.0F, 0.5F, 0.0F);
            state.clay.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        pose.popPose();
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Pottery.POTTERS_WHEEL_ENTITY.get(), PottersWheelRenderer::new);
        }
    }
}
