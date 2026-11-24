package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.RotorBlockEntity;
import com.mechanical.ShaftBlock;
import com.mechanical.WaterPower;
import com.mechanical.WaterWheelBlock;
import com.mechanical.WaterWheelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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

    /** Turns the pose (at the block centre) so local +Z runs along the world +axis: a part model's axle is its Z. */
    static void alongAxis(PoseStack pose, Direction.Axis axis) {
        if (axis == Direction.Axis.X) pose.rotateDegrees(Axis.YP, 90);
        else if (axis == Direction.Axis.Y) pose.rotateDegrees(Axis.XP, -90);
    }

    static float time(@Nullable Level level, float partialTicks) {
        return level == null ? 0 : level.getGameTime() + partialTicks;
    }

    public static class WheelState extends BlockEntityRenderState {
        ItemStackRenderState hub, spoke, rim, ring, paddle;
        Direction.Axis axis = Direction.Axis.X;
        float angle;
        /** Light in each of the wheel's 5x5 cells, [across + 2][up + 2]: each part is lit where it is, not by the hub. */
        final int[][] light = new int[5][5];
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
            for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++)
                state.light[a + 2][b + 2] = a == 0 && b == 0 || level == null ? state.lightCoords
                        : LightCoordsUtil.getLightCoords(level, WaterWheelBlockEntity.cell(wheel.getBlockPos(), state.axis, a, b));
        }

        @Override
        public void submit(WheelState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            alongAxis(pose, state.axis);
            pose.rotateDegrees(Axis.ZP, state.angle);
            state.hub.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            // the radius of each part's middle: spokes and ring boards about 1.2 blocks out, rims and paddles 2
            for (int k = 0; k < 16; k++) {
                boolean spoke = k % 2 == 0;
                if (spoke) radial(pose, collector, state, state.spoke, k * 22.5F, 1, 1.2F);          // spokes on every second paddle
                else radial(pose, collector, state, state.ring, k * 22.5F, 1, 1.2F);                 // inner ring between the spokes
                radial(pose, collector, state, state.rim, k * 22.5F + 11.25F, 2, 2);               // rim boards meet at the paddles
                radial(pose, collector, state, state.paddle, k * 22.5F, 2, 2);
            }
            pose.popPose();
        }

        private static void radial(PoseStack pose, SubmitNodeCollector collector, WheelState state, ItemStackRenderState part, float deg, float shift, float mid) {
            pose.pushPose();
            pose.rotateDegrees(Axis.ZP, deg);
            pose.translate(0, shift, 0);
            part.submit(pose, collector, light(state, deg, mid), OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        /** The light of the cell the part's middle ({@code mid} blocks out at {@code deg} round the turning wheel) is in. */
        private static int light(WheelState state, float deg, float mid) {
            float th = (state.angle + deg) * Mth.DEG_TO_RAD;
            float x = -mid * Mth.sin(th), y = mid * Mth.cos(th); // local, before alongAxis: X turned onto the axis puts local +x on world -z
            int across = Mth.clamp(Math.round(state.axis == Direction.Axis.X ? -x : x), -2, 2), up = Mth.clamp(Math.round(y), -2, 2);
            return state.light[across + 2][up + 2];
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
        boolean upright; // a gearbox with a shaft standing on it or hanging under it: its third gear
        float angle;
    }

    /** A Wooden Shaft turning on its axis, or a Gearbox's bevel gears (the Z and upright gears the other way, half a tooth on). */
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
            Level level = rotor.getLevel();
            state.upright = state.axis == null && level != null && (upright(level, rotor.getBlockPos().above()) || upright(level, rotor.getBlockPos().below()));
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
            if (state.upright) spin(pose, collector, state, state.gear, Direction.Axis.Y, -state.angle + 22.5F);
        }

        private static boolean upright(Level level, BlockPos pos) {
            var s = level.getBlockState(pos);
            return s.getBlock() instanceof ShaftBlock && s.getValue(ShaftBlock.AXIS) == Direction.Axis.Y;
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

    /**
     * Looking at a wheel's hub, a shaft or a gearbox shows the power it carries above the hotbar, once when the look lands
     * on it and again when its power changes (so it doesn't talk over the fuller right-click message).
     */
    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Hint {
        private static @Nullable BlockPos lastPos;
        private static int lastPower = -1;

        @SubscribeEvent
        static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.player.tickCount % 5 != 0) return;
            BlockPos pos = mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
            var s = pos == null ? null : mc.level.getBlockState(pos);
            if (s == null || !(s.is(WaterPower.WATER_WHEEL.get()) || s.is(WaterPower.WOODEN_SHAFT.get()) || s.is(WaterPower.GEARBOX.get()))
                    || !(mc.level.getBlockEntity(pos) instanceof RotorBlockEntity r)) {
                lastPos = null;
                return;
            }
            if (pos.equals(lastPos) && r.power() == lastPower) return;
            lastPos = pos.immutable();
            lastPower = r.power();
            mc.player.sendOverlayMessage(r.power() > 0 ? Component.translatable("message.mechanical.water_power.turning", r.power(), WaterPower.MAX_POWER)
                    : Component.translatable("message.mechanical.water_power.idle"));
        }
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(WaterPower.WATER_WHEEL_ENTITY.get(), Wheel::new);
            event.registerBlockEntityRenderer(WaterPower.ROTOR_ENTITY.get(), Rotor::new);
            event.registerEntityRenderer(WaterPower.WHEEL_HITBOX.get(), NoopRenderer::new);
        }
    }

    private WaterPowerRenderers() {}
}
