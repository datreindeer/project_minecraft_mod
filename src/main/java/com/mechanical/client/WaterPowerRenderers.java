package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.RotorBlockEntity;
import com.mechanical.ShaftBlock;
import com.mechanical.WaterPower;
import com.mechanical.WaterWheelBlock;
import com.mechanical.WaterWheelBlockEntity;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The turning parts of water power ({@link WaterPower}): the 5-block Water Wheel, Wooden Shafts and the Gearbox's bevel
 * gears. Each part is a block model (scripts/gen_waterpower.py) drawn through an item model definition on a stand-in
 * stack, as the Potter's Wheel does; a model is centred on the pose, so the wheel is put together from one hub and
 * repeated spokes, rim boards, inner ring boards and paddles, each turned into place round the axle. The wheel's parts
 * reach past a model's 3-block limit, so each part model is drawn {@code shift} blocks out along its radius.
 */
public final class WaterPowerRenderers {
    private static ItemStack stack(String model) {
        ItemStack s = new ItemStack(Items.STICK);
        s.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(Mechanical.MODID, model));
        return s;
    }

    static ItemStackRenderState part(ItemModelResolver items, String model, Level level, int seed) {
        ItemStackRenderState state = new ItemStackRenderState();
        items.updateForTopItem(state, stack(model), ItemDisplayContext.NONE, level, null, seed);
        return state;
    }

    /** Turns the pose (at the block centre) so local +Z runs along the world axis: a part model's axle is its Z. */
    static void alongAxis(PoseStack pose, Direction.Axis axis) {
        if (axis == Direction.Axis.X) pose.rotateDegrees(Axis.YP, 90);
    }

    static float time(@Nullable Level level, float partialTicks) {
        return level == null ? 0 : level.getGameTime() + partialTicks;
    }

    public static class WheelState extends BlockEntityRenderState {
        ItemStackRenderState hub, spoke, rim, ring, paddle;
        Direction.Axis axis = Direction.Axis.X;
        float angle;
    }

    /** The wheel: 8 double spokes, 16 rim boards each side, an inner ring, 16 paddles with iron cleats. */
    public static class Wheel implements BlockEntityRenderer<WaterWheelBlockEntity, WheelState> {
        private final ItemModelResolver items;

        public Wheel(BlockEntityRendererProvider.Context context) {
            items = context.itemModelResolver();
        }

        @Override
        public WheelState createRenderState() {
            return new WheelState();
        }

        @Override
        public void extractRenderState(WaterWheelBlockEntity wheel, WheelState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(wheel, state, partialTicks, camera, breakProgress);
            int seed = (int) wheel.getBlockPos().asLong();
            Level level = wheel.getLevel();
            if (state.hub == null) {
                state.hub = part(items, "water_wheel_hub", level, seed);
                state.spoke = part(items, "water_wheel_spoke", level, seed);
                state.rim = part(items, "water_wheel_rim", level, seed);
                state.ring = part(items, "water_wheel_ring", level, seed);
                state.paddle = part(items, "water_wheel_paddle", level, seed);
            }
            state.axis = wheel.getBlockState().getValue(WaterWheelBlock.AXIS);
            state.angle = wheel.angle(time(level, partialTicks));
        }

        @Override
        public void submit(WheelState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            alongAxis(pose, state.axis);
            pose.rotateDegrees(Axis.ZP, state.angle);
            state.hub.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            for (int k = 0; k < 16; k++) {
                boolean spoke = k % 2 == 0;
                if (spoke) radial(pose, collector, state, state.spoke, k * 22.5F, 1);          // spokes on every second paddle
                else radial(pose, collector, state, state.ring, k * 22.5F, 1);                 // inner ring between the spokes
                radial(pose, collector, state, state.rim, k * 22.5F + 11.25F, 2);             // rim boards meet at the paddles
                radial(pose, collector, state, state.paddle, k * 22.5F, 2);
            }
            pose.popPose();
        }

        private static void radial(PoseStack pose, SubmitNodeCollector collector, WheelState state, ItemStackRenderState part, float deg, float shift) {
            pose.pushPose();
            pose.rotateDegrees(Axis.ZP, deg);
            pose.translate(0, shift, 0);
            part.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(WaterWheelBlockEntity wheel) {
            return new AABB(wheel.getBlockPos()).inflate(2.0);
        }

        @Override
        public int getViewDistance() {
            return 128;
        }
    }

    public static class RotorState extends BlockEntityRenderState {
        ItemStackRenderState shaft, gear;
        Direction.@Nullable Axis axis; // null: a gearbox
        float angle;
    }

    /** A Wooden Shaft turning on its axis, or a Gearbox's two bevel gears (the Z gear the other way, half a tooth on). */
    public static class Rotor implements BlockEntityRenderer<RotorBlockEntity, RotorState> {
        private final ItemModelResolver items;

        public Rotor(BlockEntityRendererProvider.Context context) {
            items = context.itemModelResolver();
        }

        @Override
        public RotorState createRenderState() {
            return new RotorState();
        }

        @Override
        public void extractRenderState(RotorBlockEntity rotor, RotorState state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(rotor, state, partialTicks, camera, breakProgress);
            int seed = (int) rotor.getBlockPos().asLong();
            if (state.shaft == null) {
                state.shaft = part(items, "wooden_shaft_rotor", rotor.getLevel(), seed);
                state.gear = part(items, "gearbox_gear", rotor.getLevel(), seed);
            }
            var block = rotor.getBlockState();
            state.axis = block.getBlock() instanceof ShaftBlock ? block.getValue(ShaftBlock.AXIS) : null;
            state.angle = rotor.angle(time(rotor.getLevel(), partialTicks));
        }

        @Override
        public void submit(RotorState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (state.axis != null) {
                spin(pose, collector, state, state.shaft, state.axis, state.angle);
                return;
            }
            spin(pose, collector, state, state.gear, Direction.Axis.X, state.angle);
            spin(pose, collector, state, state.gear, Direction.Axis.Z, -state.angle + 22.5F);
        }

        private static void spin(PoseStack pose, SubmitNodeCollector collector, RotorState state, ItemStackRenderState part, Direction.Axis axis, float angle) {
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            alongAxis(pose, axis);
            pose.rotateDegrees(Axis.ZP, angle);
            part.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(WaterPower.WATER_WHEEL_ENTITY.get(), Wheel::new);
            event.registerBlockEntityRenderer(WaterPower.ROTOR_ENTITY.get(), Rotor::new);
        }
    }

    private WaterPowerRenderers() {}
}
