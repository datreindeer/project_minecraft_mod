package com.mechanical.client;

import com.mechanical.CarcassEntity;
import com.mechanical.CarcassHauling;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * A player hauling a carcass ({@link CarcassHauling}) holds it with both hands, seen by everyone: derived from the synced
 * haulers of nearby carcasses when the player's render state is extracted. On the shoulders both arms go up to hold it; a
 * dragged one he holds by a rope lashed round its leg ({@link #hands}, drawn by CarcassRenderer: he is anywhere from on
 * top of it to a few blocks off), both fists on it in front of him, or trailing behind when he walks off, tugging at every
 * step, or straining and trembling when it is too heavy for the pullers on it (HumanoidModelMixin). The held items are hidden, in first person the hands too.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HaulingClient {
    /** true: dragging a large body, false: carrying one on the shoulders; absent: not hauling. */
    private static final ContextKey<Boolean> DRAGGING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "hauling"));
    /** Present: holding a bird by its feet over the right shoulder (CarcassRenderer's SLUNG pose). */
    private static final ContextKey<Boolean> SLUNG = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "slung"));
    /** Present: dragging a body too heavy for the pullers on it (CarcassHauling#straining). */
    private static final ContextKey<Boolean> STRAIN = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "strain"));
    /** Present: the dragged carcass is behind the player (he walks off with the rope over his hips: arms back). */
    private static final ContextKey<Boolean> BEHIND = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "drag_behind"));
    /** Arms back and down to a body behind (radians, model xRot). */
    private static final float TRAIL = 0.65F;
    /** Arms down and forward to the grip (radians, model xRot), how far they tug back at full walking speed, and the strain lean back. */
    private static final float REACH = -0.95F, TUG = 0.45F, STRAIN_BACK = 0.35F;

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                com.mechanical.HeavyLoad c = avatar instanceof Player player ? CarcassHauling.heavy(player) : null;
                // a downed tribe mate on the shoulders is held like a carcass (clan.Downed)
                boolean mate = avatar instanceof Player player && com.mechanical.clan.Downed.carrying(player) != null;
                if (c == null && !mate) return;
                if (c == null) state.setRenderData(DRAGGING, false);
                else if (c instanceof CarcassEntity body && body.isSlung() && !body.isDragged()) state.setRenderData(SLUNG, true);
                else state.setRenderData(DRAGGING, c.isDragged());
                if (c instanceof CarcassEntity body && body.isDragged() && behind((Player) avatar, body, state.partialTick))
                    state.setRenderData(BEHIND, true);
                if (c != null && CarcassHauling.straining(c)) state.setRenderData(STRAIN, true);
                state.rightHandItemState.clear();
                state.leftHandItemState.clear();
                state.rightArmPose = state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            }
        });
    }

    private static boolean behind(Player player, CarcassEntity body, float partialTicks) {
        float yaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        return body.getPosition(partialTicks).subtract(player.getPosition(partialTicks)).dot(Vec3.directionFromRotation(0, yaw)) < 0;
    }

    /** A dragging player's arms: {xRot, yRot} of the right one (the left one's yRot mirrored). */
    private static float[] dragArms(boolean behind, boolean strain, float age, float walkSpeed, float walkPos) {
        float pull = strain ? STRAIN_BACK + Mth.sin(age * 2.6F) * 0.05F : TUG * Math.min(1.0F, walkSpeed) * Mth.abs(Mth.sin(walkPos * 0.6662F));
        // facing it the arms reach down to the rope and tug back; walking off they trail back and the tug draws them in
        return behind ? new float[] {TRAIL - pull * 0.5F, 0.2F} : new float[] {REACH + pull, -0.3F};
    }

    /**
     * Where the two fists (right, left) of {@code player} dragging {@code body} are, from {@code origin}: the arm posed as
     * {@link #poseArms} does it, through the player model's transforms. The drag ropes end in them (CarcassRenderer).
     */
    static Vec3[] hands(Player player, CarcassEntity body, float partialTicks, Vec3 origin) {
        boolean behind = behind(player, body, partialTicks);
        float[] arm = dragArms(behind, CarcassHauling.straining(body), player.tickCount + partialTicks,
                player.walkAnimation.speed(partialTicks), player.walkAnimation.position(partialTicks));
        float yaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        boolean crouching = player.isCrouching();
        Vec3 feet = player.getPosition(partialTicks).subtract(origin).add(0, crouching ? -0.125 : 0, 0);
        Vec3[] out = new Vec3[2];
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson()) {
            // own hands aren't drawn: the rope comes from his waist, under the view, instead of ending in thin air
            Vec3 right = Vec3.directionFromRotation(0, yaw + 90).scale(0.18);
            return new Vec3[] {feet.add(right).add(0, 0.75, 0), feet.subtract(right).add(0, 0.75, 0)};
        }
        for (int side = 0; side < 2; side++) {
            float mirror = side == 0 ? 1 : -1;
            // shoulder pivot at (-+5, 2, 0) px, the fist 9 px down the arm and 1 px outwards
            org.joml.Vector3f p = new org.joml.Quaternionf().rotationZYX(0.1F * mirror, arm[1] * mirror, arm[0])
                    .transform(new org.joml.Vector3f(-mirror / 16.0F, 9 / 16.0F, 0)).add(-5 * mirror / 16.0F, (crouching ? 5.2F : 2) / 16.0F, 0);
            // as the player renderer draws it: scaled 0.9375, the model flipped and stood on its feet, turned to the body's yaw
            org.joml.Vector3f w = new org.joml.Vector3f(-p.x, 1.501F - p.y, p.z).mul(0.9375F).rotateY((180.0F - yaw) * Mth.DEG_TO_RAD);
            out[side] = feet.add(w.x, w.y, w.z);
        }
        return out;
    }

    /** First person: the hands are on the body, not in view with an item. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && (CarcassHauling.heavy(player) != null || com.mechanical.clan.Downed.carrying(player) != null)) event.setCanceled(true);
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
            // both hands closed on the drag rope (or the axe), tugging with each step (or straining, trembling)
            float[] arm = dragArms(state.getRenderData(BEHIND) != null, state.getRenderData(STRAIN) != null, state.ageInTicks,
                    state.walkAnimationSpeed, state.walkAnimationPos);
            model.rightArm.xRot = model.leftArm.xRot = arm[0];
            model.rightArm.zRot = 0.1F;
            model.leftArm.zRot = -0.1F;
            // hands together on the grip
            model.rightArm.yRot = arm[1];
            model.leftArm.yRot = -arm[1];
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
