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
    /** A Trader Caravan's old leader: stooped over his staff. */
    static final ContextKey<Boolean> OLD = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_old"));

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.<Tribesperson, AvatarRenderState>registerEntityModifier(TribespersonRenderer.class, (t, state) -> {
            state.setRenderData(IDLE, t.idle());
            if (t.role() == com.mechanical.people.TribeRole.TRADER && t.caravanKind() != null) state.setRenderData(OLD, true);
            // the Miner's sack (EquipmentLayer) is render data too: set here, where it survives
            if (t.role() == com.mechanical.people.TribeRole.MINER && t.getPose() == Pose.STANDING) com.mechanical.client.EquipmentClient.wearMinerSack(state);
            Identifier gear = gear(t.texture());
            if (gear != null) com.mechanical.client.EquipmentClient.wearTribeGear(state, gear);
        });
    }

    // shortcut: looked up once per skin; a resource pack adding or removing gear needs a restart (clear on reload if that matters)
    private static final java.util.Map<Identifier, java.util.Optional<Identifier>> GEAR = new java.util.HashMap<>();

    /** The gear texture next to a skin (skin_gear.png), if that person has one (scripts/gen_tribe_skins.py). */
    private static Identifier gear(Identifier skin) {
        return GEAR.computeIfAbsent(skin, s -> {
            Identifier g = s.withPath(p -> p.replace(".png", "_gear.png"));
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(g).map(r -> g);
        }).orElse(null);
    }

    public static void pose(HumanoidModel<?> model, HumanoidRenderState state) {
        if (Boolean.TRUE.equals(state.getRenderData(OLD)) && !state.isPassenger && state.swimAmount == 0) stoop(model, state);
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
            case CampLife.PRAY -> { // Oak Folk worshipper: hands raised to the tree, palms up, swaying slowly, face lifted
                float sway = 0.12F * Mth.sin(t * 0.06F);
                head(model, -0.45F + sway * 0.5F);
                model.body.xRot = -0.05F + sway * 0.3F;
                arm(model.rightArm, -2.3F + sway, -0.25F, 0.3F);
                arm(model.leftArm, -2.3F + sway, 0.25F, -0.3F);
            }
            case CampLife.CANDLES -> { // Oak Folk light-bearer: a lit candle carried before them in each hand, elbows in
                float bob = 0.04F * Mth.sin(t * 0.15F);
                arm(model.rightArm, -1.05F + bob, -0.2F, 0.05F);
                arm(model.leftArm, -1.05F - bob, 0.2F, -0.05F);
                head(model, 0.25F);
            }
            case CampLife.STAFF -> { // the keeper raises the Verdant Staff high
                head(model, -0.3F);
                arm(model.rightArm, -2.6F, -0.1F, 0.15F);
                arm(model.leftArm, -0.9F + 0.1F * Mth.sin(t * 0.2F), 0.4F, 0);
            }
            case CampLife.TEND -> { // Oak Folk gatherer: both hands down in the plants, picking or planting
                head(model, 0.75F);
                model.body.xRot = 0.35F;
                arm(model.rightArm, -0.9F + 0.2F * Mth.sin(t * 0.5F), -0.15F, 0);
                arm(model.leftArm, -0.9F + 0.2F * Mth.cos(t * 0.5F), 0.15F, 0);
            }
            default -> {}
        }
    }

    /**
     * An old man's walk: shoulders bent forward over the staff, head pushed forward and up to see the way, short shuffling
     * steps (the legs swing half as far) and arms that hardly swing; the staff hand held out in front.
     */
    private static void stoop(HumanoidModel<?> model, HumanoidRenderState state) {
        float bend = 0.22F;
        model.body.xRot = bend;
        model.head.z = -2.0F;
        model.head.y = 0.6F;
        model.head.xRot -= bend * 0.5F;
        for (var arm : new net.minecraft.client.model.geom.ModelPart[] {model.rightArm, model.leftArm}) {
            arm.z = -1.8F;
            arm.y = 2.4F;
            arm.xRot = arm.xRot * 0.35F - 0.3F;
        }
        model.rightArm.xRot -= 0.15F; // the staff hand out in front
        model.rightLeg.xRot *= 0.5F;
        model.leftLeg.xRot *= 0.5F;
        model.rightLeg.z = model.leftLeg.z = 0.6F;
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
