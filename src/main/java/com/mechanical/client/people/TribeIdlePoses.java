package com.mechanical.client.people;

import com.mechanical.Mechanical;
import com.mechanical.people.CampLife;

import com.mechanical.people.Tribesperson;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * A tribesperson's idle action and seat (CampLife, synced as Tribesperson#idle) posed at the end of HumanoidModel#setupAnim
 * (HumanoidModelMixin): seated cross-legged on the ground (the whole model lowered), whittling, checking the weapon,
 * rummaging in the bag, eating, talking with gestures, stretching, warming the hands. The value goes into the render state
 * through a render state modifier: NeoForge clears render data set in extractRenderState.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TribeIdlePoses {
    static final ContextKey<Integer> IDLE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_idle"));

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.<Tribesperson, AvatarRenderState>registerEntityModifier(TribespersonRenderer.class, (t, state) -> {
            state.setRenderData(IDLE, t.idle());
            // the Miner's sack (EquipmentLayer) is render data too: set here, where it survives
            if (t.role() == com.mechanical.people.TribeRole.MINER && t.getPose() == Pose.STANDING) com.mechanical.client.EquipmentClient.wearMinerSack(state);
        });
    }

    public static void pose(HumanoidModel<?> model, HumanoidRenderState state) {
        Integer idle = state.getRenderData(IDLE);
        if (idle == null || idle == 0 || state.isPassenger || state.swimAmount > 0) return;
        float t = state.ageInTicks;
        boolean sitting = (idle & CampLife.SITTING) != 0;
        if (sitting) {
            model.root().y += 10; // hips on the ground
            model.rightLeg.xRot = model.leftLeg.xRot = -1.45F; // legs forward, crossed at the shins
            model.rightLeg.yRot = 0.45F;
            model.leftLeg.yRot = -0.45F;
            model.rightLeg.zRot = 0.1F;
            model.leftLeg.zRot = -0.1F;
            model.rightArm.xRot = model.leftArm.xRot = -0.55F; // hands on the knees
            model.rightArm.zRot = model.leftArm.zRot = 0;
        }
        switch (idle & 15) {
            case CampLife.WHITTLE -> { // stick in the left hand, knife strokes with the right, eyes on the work
                head(model, 0.6F);
                arm(model.leftArm, -0.9F, 0.45F, 0);
                arm(model.rightArm, -0.8F - 0.25F * Mth.sin(t * 0.5F), -0.35F, 0);
            }
            case CampLife.CHECK_WEAPON -> { // weapon held up, turned to look along it
                head(model, 0.25F);
                model.head.yRot = -0.35F;
                arm(model.rightArm, -1.35F, -0.3F, 0.25F * Mth.sin(t * 0.08F));
                arm(model.leftArm, -1.0F, 0.5F, 0);
            }
            case CampLife.RUMMAGE -> { // hands in the bag at the hip, head down
                head(model, 0.7F);
                model.body.xRot = sitting ? 0.25F : 0.2F;
                arm(model.rightArm, -0.45F + 0.15F * Mth.sin(t * 0.6F), 0.4F, 0);
                arm(model.leftArm, -0.45F + 0.15F * Mth.cos(t * 0.6F), -0.4F, 0);
            }
            case CampLife.EAT -> { // a bite: hand up to the mouth and down a little, chewing
                head(model, 0.15F + 0.05F * Mth.sin(t * 1.2F));
                arm(model.rightArm, -1.75F + 0.2F * Mth.sin(t * 0.3F), -0.55F, 0);
            }
            case CampLife.TALK -> { // gesturing with one hand, nodding
                model.head.xRot += 0.08F * Mth.sin(t * 0.4F);
                arm(model.rightArm, -0.7F + 0.35F * Mth.sin(t * 0.25F), -0.25F + 0.2F * Mth.sin(t * 0.17F), 0.15F);
            }
            case CampLife.STRETCH -> { // both arms up over the head, leaning back
                float s = 0.85F + 0.15F * Mth.sin(t * 0.1F);
                head(model, -0.35F);
                model.body.xRot = -0.08F;
                arm(model.rightArm, -2.9F * s, 0, 0.35F);
                arm(model.leftArm, -2.9F * s, 0, -0.35F);
            }
            case CampLife.WARM_HANDS -> { // palms out to the fire, rubbing now and then
                float rub = 0.08F * Mth.sin(t * 0.7F);
                head(model, 0.15F);
                arm(model.rightArm, -1.25F, -0.25F + rub, 0);
                arm(model.leftArm, -1.25F, 0.25F - rub, 0);
            }
            default -> {}
        }
    }

    private static void head(HumanoidModel<?> model, float xRot) {
        model.head.xRot = xRot;
    }

    private static void arm(net.minecraft.client.model.geom.ModelPart arm, float x, float y, float z) {
        arm.xRot = x;
        arm.yRot = y;
        arm.zRot = z;
    }

    private TribeIdlePoses() {}
}
