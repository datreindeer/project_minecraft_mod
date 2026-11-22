package com.mechanical.client;

import com.mechanical.FelledLogEntity;
import com.mechanical.Mechanical;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * A player lifting an end of a felled log ({@link FelledLogEntity}), seen by everyone (from the log's synced lifters):
 * facing the log (it lies ahead) both hands reach forward and down to the end; walking ahead of it (it trails behind)
 * both arms hang back and down gripping it. Overloaded, the arms strain and tremble (HumanoidModelMixin). Hand items
 * are hidden; first person: {@link FirstPersonHolds#liftHands}.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class LogLiftClient {
    /** true: the log lies ahead (facing it), false: it trails behind; absent: not lifting. */
    private static final ContextKey<Boolean> FACING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "log_lift"));
    private static final ContextKey<Boolean> STRAIN = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "log_strain"));

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (!(avatar instanceof Player player)) return;
                FelledLogEntity log = FelledLogEntity.liftedBy(player);
                if (log == null) return;
                state.setRenderData(FACING, facing(player, log));
                if (log.liftLoad() > 1) state.setRenderData(STRAIN, true);
                state.rightHandItemState.clear();
                state.leftHandItemState.clear();
                state.rightArmPose = state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            }
        });
    }

    /**
     * Lifting: a click only reaches the log itself (put it down) or a Wagon (load it); anything else is stopped here,
     * before the client acts or sends anything (LogLifting stops it on the server too).
     */
    @SubscribeEvent
    static void onClick(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || FelledLogEntity.liftedBy(mc.player) == null) return;
        if (event.isUseItem() && mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && (hit.getEntity() instanceof FelledLogEntity || hit.getEntity() instanceof com.mechanical.WagonEntity || hit.getEntity() instanceof com.mechanical.CartEntity)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    /** Does the log lie ahead of {@code player} (they face it)? */
    static boolean facing(Player player, FelledLogEntity log) {
        Vec3 look = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 to = log.position().subtract(player.position());
        return look.x * to.x + look.z * to.z > 0;
    }

    /** End of HumanoidModel#setupAnim: arms forward-down to the log ahead, or back-down to the log behind. */
    public static void poseArms(HumanoidModel<?> model, HumanoidRenderState state) {
        Boolean facing = state.getRenderData(FACING);
        if (facing == null || state.swimAmount > 0) return;
        boolean strain = state.getRenderData(STRAIN) != null;
        float tremble = strain ? Mth.sin(state.ageInTicks * 2.6F) * 0.05F : 0;
        float step = Math.min(1.0F, state.walkAnimationSpeed) * Mth.sin(state.walkAnimationPos * 0.6662F) * 0.08F;
        if (facing) {
            // hands together low in front, on the end of the log (straining: pulled further down and back)
            model.rightArm.xRot = model.leftArm.xRot = -0.85F + (strain ? 0.3F : 0) + tremble + step;
            model.rightArm.yRot = -0.3F;
            model.leftArm.yRot = 0.3F;
            model.rightArm.zRot = 0.1F;
            model.leftArm.zRot = -0.1F;
        } else {
            // both arms back and down at the sides, gripping the log behind (straining: further back)
            model.rightArm.xRot = model.leftArm.xRot = 0.45F + (strain ? 0.25F : 0) + tremble - step;
            model.rightArm.yRot = model.leftArm.yRot = 0;
            model.rightArm.zRot = 0.15F;
            model.leftArm.zRot = -0.15F;
        }
    }

    private LogLiftClient() {}
}
