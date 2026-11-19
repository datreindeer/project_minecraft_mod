package com.mechanical.client;

import com.mechanical.CarcassEntity;
import com.mechanical.CarcassHauling;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
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
 * hauler of nearby carcasses when the player's render state is extracted. On the shoulders both arms go up to hold it, a
 * dragged one has both arms reaching back (HumanoidModelMixin); the held items are hidden, in first person the hands too.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HaulingClient {
    /** true: dragging a large body, false: carrying one on the shoulders; absent: not hauling. */
    private static final ContextKey<Boolean> DRAGGING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "hauling"));
    /** Present: holding a bird by its feet over the right shoulder (CarcassRenderer's SLUNG pose). */
    private static final ContextKey<Boolean> SLUNG = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "slung"));

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                CarcassEntity c = avatar instanceof Player player ? CarcassHauling.hauled(player) : null;
                if (c == null) return;
                if (com.mechanical.Birds.isBird(c.animal()) && !c.isLarge()) state.setRenderData(SLUNG, true);
                else state.setRenderData(DRAGGING, c.isLarge());
                state.rightHandItemState.clear();
                state.leftHandItemState.clear();
                state.rightArmPose = state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            }
        });
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
            // reaching back and down to the body behind
            model.rightArm.xRot = model.leftArm.xRot = 0.55F;
            model.rightArm.zRot = 0.2F;
            model.leftArm.zRot = -0.2F;
        } else {
            // both hands up at either side of the body across the shoulders
            model.rightArm.xRot = model.leftArm.xRot = -2.75F;
            model.rightArm.zRot = 0.6F;
            model.leftArm.zRot = -0.6F;
        }
        model.rightArm.yRot = model.leftArm.yRot = 0;
    }

    private HaulingClient() {}
}
