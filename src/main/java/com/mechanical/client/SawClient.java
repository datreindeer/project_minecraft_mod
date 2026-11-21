package com.mechanical.client;

import com.mechanical.Bronze;
import com.mechanical.Mechanical;
import com.mechanical.SawItem;
import com.mechanical.Smithing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Sawing a fallen log with a saw (Bronze, Iron, Steel) ({@link SawItem}), drawn from the use time so the motion is in step with the
 * server's strokes. Third person (everyone: whether a player is using an item is synced): the saw arm drives the saw back
 * and forth down at the log, the other hand braced on it. First person: the held saw goes forward and back, tipped down.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SawClient {
    /** ArmPose MECHANICAL_SAWING (META-INF/enumextensions.json): two-handed, so it poses both arms itself. */
    public static final EnumProxy<HumanoidModel.ArmPose> SAWING_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class,
            true, true, (IArmPoseTransformer) SawClient::poseArms);

    private static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return entity.isUsingItem() && entity.getUsedItemHand() == hand ? SAWING_POSE.getValue() : null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, PlayerRenderState playerState, HumanoidArm arm, ItemStack item,
                float partialTick, float equipProcess, float swingProcess) {
            Player player = Minecraft.getInstance().player;
            if (player == null || !player.isUsingItem() || player.getUsedItemHand().asArm(player.getMainArm()) != arm) return false;
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float push = SawItem.push(player.getTicksUsingItem(partialTick), strokeTicks(item));
            poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F); // vanilla's held-item position
            poseStack.translate(side * -0.14F, -0.1F - 0.06F * push, 0.08F - 0.34F * push); // in front, driving forward and back
            poseStack.rotateDegrees(Axis.XP, -40.0F); // blade tipped forward, down at the log
            return true;
        }
    };

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(EXTENSIONS, Bronze.BRONZE_SAW.get(), Smithing.IRON_SAW.get(), Smithing.STEEL_SAW.get());
    }

    private static int strokeTicks(ItemStack stack) {
        return stack.getItem() instanceof SawItem saw ? saw.strokeTicks : SawItem.STROKE_TICKS;
    }

    /** Called for the saw arm only (the pose affects the other hand's too). */
    private static void poseArms(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        ModelPart saw = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart brace = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float push = SawItem.push(state.ticksUsingItem, strokeTicks(state.getUseItemStackForArm(arm)));
        // reaching down-forward to the log: pulled back the arm hangs lower and the shoulder draws back, pushed it reaches out
        saw.xRot = -0.6F - 0.5F * push;
        saw.yRot = -side * 0.12F;
        saw.zRot = 0;
        saw.z += 1.0F - 2.0F * push;
        // the other hand steadies the log beside the cut
        brace.xRot = -0.8F;
        brace.yRot = side * 0.45F;
        brace.zRot = 0;
    }

    private SawClient() {}
}
