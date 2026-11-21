package com.mechanical.client;

import com.mechanical.Mechanical;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Third-person arm poses for held items (ArmPose enum extensions in META-INF/enumextensions.json). They come from
 * IClientItemExtensions#getArmPose, so players get them from vanilla and tribespeople from TribespersonRenderer.armPose().
 * Everything is drawn from the render state (walk animation, held items), which is already synced.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HoldPoses {
    /** ArmPose MECHANICAL_STAFF: one-handed, the staff held upright (its model's display transforms) and planted in step. */
    public static final EnumProxy<HumanoidModel.ArmPose> STAFF_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, false, (IArmPoseTransformer) HoldPoses::poseStaff);

    private static IClientItemExtensions pose(EnumProxy<HumanoidModel.ArmPose> pose) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return pose.getValue();
            }
        };
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(pose(STAFF_POSE), Mechanical.WALKING_STAFF.get(), Mechanical.ELDERS_STAFF.get());
    }

    /**
     * Walking: the staff arm swings with the opposite foot like a normal arm swing, but is lifted while it carries the staff
     * forward and comes down to plant the tip, then stays low while the body walks past it. Standing: at rest, upright.
     */
    static void poseStaff(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float phase = state.walkAnimationPos * 0.6662F + (arm == HumanoidArm.RIGHT ? Mth.PI : 0.0F); // vanilla's arm phase
        float speed = Math.min(1.0F, state.walkAnimationSpeed / state.speedValue);
        float swing = 0.55F * Mth.cos(phase) * speed;
        part.xRot = -Mth.PI / 10.0F + swing; // -PI/10: vanilla's held-item angle, the staff model is upright at it
        part.yRot = 0.0F;
        float sin = Mth.sin(phase); // > 0 while the arm moves forward
        // forward: lifted, tip off the ground; back: the arm drops as far as the swing raised the tip (it is 20 px down, 5 ahead
        // of the shoulder), so the tip stays planted while the body walks past it (at most 1.5 px, or the shoulder gapes)
        part.y += sin > 0.0F ? -1.5F * sin * speed : Math.min(1.5F, 20.0F * (1.0F - Mth.cos(swing)) - 5.0F * Mth.sin(swing));
    }

    private HoldPoses() {}
}
