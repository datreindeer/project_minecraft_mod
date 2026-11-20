package com.mechanical.client;

import com.mechanical.CarcassEntity;
import com.mechanical.CarcassHauling;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * A player hauling a carcass ({@link CarcassHauling}) holds it with both hands, seen by everyone: derived from the synced
 * haulers of nearby carcasses when the player's render state is extracted. On the shoulders both arms go up to hold it; a
 * dragged one he faces, both hands down on its leg, tugging back at every step, or straining and trembling when it is
 * too heavy for the pullers on it (HumanoidModelMixin). The held items are hidden, in first person the hands too.
 * Dragging, the own player's view is locked on his grip ({@link #lockView}): he walks backwards pulling it.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HaulingClient {
    /** true: dragging a large body, false: carrying one on the shoulders; absent: not hauling. */
    private static final ContextKey<Boolean> DRAGGING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "hauling"));
    /** Present: holding a bird by its feet over the right shoulder (CarcassRenderer's SLUNG pose). */
    private static final ContextKey<Boolean> SLUNG = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "slung"));
    /** Present: dragging a body too heavy for the pullers on it (CarcassHauling#straining). */
    private static final ContextKey<Boolean> STRAIN = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "strain"));
    /** Arms down and forward to the grip (radians, model xRot), how far they tug back at full walking speed, and the strain lean back. */
    private static final float REACH = -0.95F, TUG = 0.45F, STRAIN_BACK = 0.35F;
    /** View pitch allowed while dragging (degrees, down positive): on the body, not the sky. */
    private static final float PITCH_MIN = 0.0F, PITCH_MAX = 70.0F;

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                CarcassEntity c = avatar instanceof Player player ? CarcassHauling.hauled(player) : null;
                if (c == null) return;
                if (com.mechanical.Birds.isBird(c.animal()) && !c.isLarge()) state.setRenderData(SLUNG, true);
                else state.setRenderData(DRAGGING, c.isLarge());
                if (CarcassHauling.straining(c)) state.setRenderData(STRAIN, true);
                state.rightHandItemState.clear();
                state.leftHandItemState.clear();
                state.rightArmPose = state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            }
        });
    }

    /**
     * Every frame, right after the mouse turned the view (MouseHandlerMixin): dragging a body, the own player faces his grip
     * on it and can only look up and down a little. Turned like the mouse does (yRotO with yRot), so it stays smooth; A and D
     * walk round it and steer it, S pulls it along.
     */
    public static void lockView() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isPassenger()) return;
        CarcassEntity c = CarcassHauling.hauled(player);
        if (c == null || !c.isLarge()) return;
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 to = c.gripPoint(c.slotOf(player), partialTicks).subtract(player.getPosition(partialTicks));
        if (to.horizontalDistanceSqr() > 0.04) {
            float turn = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-to.x, to.z)) - player.getYRot());
            player.setYRot(player.getYRot() + turn);
            player.yRotO += turn;
        }
        float pitch = Mth.clamp(player.getXRot(), PITCH_MIN, PITCH_MAX), tilt = pitch - player.getXRot();
        player.setXRot(pitch);
        player.xRotO += tilt;
    }

    /** First person: the hands are on the body, not in view with an item. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && CarcassHauling.hauled(player) != null) event.setCanceled(true);
    }

    /** End of HumanoidModel#setupAnim (players and their armour): arms up holding the body, or back gripping the dragged one. */
    public static void poseArms(HumanoidModel<?> model, HumanoidRenderState state) {
        if (state.getRenderData(SLUNG) != null && state.swimAmount <= 0) {
            // the right hand up by the shoulder, gripping the feet of the bird hanging down the back
            model.rightArm.xRot = -2.9F;
            model.rightArm.yRot = 0;
            model.rightArm.zRot = 0.1F;
            return;
        }
        Boolean dragging = state.getRenderData(DRAGGING);
        if (dragging == null || state.swimAmount > 0) return;
        if (dragging) {
            // facing the body: both hands down on its leg, tugging back with each step (or straining, trembling)
            float pull = state.getRenderData(STRAIN) != null ? STRAIN_BACK + Mth.sin(state.ageInTicks * 2.6F) * 0.05F
                    : TUG * Math.min(1.0F, state.walkAnimationSpeed) * Mth.abs(Mth.sin(state.walkAnimationPos * 0.6662F));
            model.rightArm.xRot = model.leftArm.xRot = REACH + pull;
            model.rightArm.zRot = 0.1F;
            model.leftArm.zRot = -0.1F;
            // hands together on the grip
            model.rightArm.yRot = -0.3F;
            model.leftArm.yRot = 0.3F;
            return;
        }
        // both hands up at either side of the body across the shoulders
        model.rightArm.xRot = model.leftArm.xRot = -2.75F;
        model.rightArm.zRot = 0.6F;
        model.leftArm.zRot = -0.6F;
        model.rightArm.yRot = model.leftArm.yRot = 0;
    }

    private HaulingClient() {}
}
