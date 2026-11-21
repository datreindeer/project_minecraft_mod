package com.mechanical.client;

import org.jspecify.annotations.Nullable;

import com.mechanical.BronzeTools;
import com.mechanical.Mechanical;
import com.mechanical.equipment.PackMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Third-person arm poses for held items (ArmPose enum extensions in META-INF/enumextensions.json). They come from
 * IClientItemExtensions#getArmPose, so players get them from vanilla and tribespeople from TribespersonRenderer.armPose().
 * Everything is drawn from what already syncs (walk animation, held items) plus PackMenu.RUMMAGING for an open bag.
 * <ul>
 * <li>Staffs: upright, planted in step.</li>
 * <li>Bark Basket: in the crook of the arm against the hip, swaying with the steps.</li>
 * <li>Bronze Tongs: held out forward. With something hot in the other hand ({@link BronzeTools#hot}) the tongs grip it:
 * {@link GripLayer} draws it at the jaws and the other hand is empty (third person only).</li>
 * <li>Rummaging in an open satchel, pouch or basket: head down, hands in the bag (WornSatchelModel / WornPouchModel
 * pull the worn bag forward).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HoldPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> STAFF_POSE = pose(false, false, HoldPoses::poseStaff);
    public static final EnumProxy<HumanoidModel.ArmPose> BASKET_POSE = pose(false, false, HoldPoses::poseBasket);
    public static final EnumProxy<HumanoidModel.ArmPose> TONGS_POSE = pose(false, false, HoldPoses::poseTongs);
    /** Both arms: set on the arm holding the bag (or the main arm for a worn one) by the render-state modifier. */
    public static final EnumProxy<HumanoidModel.ArmPose> RUMMAGE_POSE = pose(true, true, HoldPoses::poseRummage);

    /** The hot item held in the tongs, resolved for drawing at the jaws, and the tongs' arm. */
    private record Grip(ItemStackRenderState item, HumanoidArm arm) {}
    private static final ContextKey<Grip> GRIP = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tongs_grip"));
    private static final ContextKey<Byte> RUMMAGE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "rummaging"));
    /** Where the worn bag is that the hands go to (model x): the satchel hangs on the left, the pouch on the right. */
    private static final ContextKey<Float> BAG_X = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "bag_x"));
    /** How far past the shoulder pivot (model px along the arm) the tongs' jaws are, by bronze_tongs.json's display. */
    private static final float JAWS = 21.0F;

    private static EnumProxy<HumanoidModel.ArmPose> pose(boolean twoHanded, boolean affectsOffhand, IArmPoseTransformer transformer) {
        return new EnumProxy<>(HumanoidModel.ArmPose.class, twoHanded, affectsOffhand, transformer);
    }

    private static IClientItemExtensions extensions(EnumProxy<HumanoidModel.ArmPose> pose) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return pose.getValue();
            }
        };
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(extensions(STAFF_POSE), Mechanical.WALKING_STAFF.get(), Mechanical.ELDERS_STAFF.get());
        event.registerItem(extensions(BASKET_POSE), Mechanical.BARK_BASKET.get());
        event.registerItem(extensions(TONGS_POSE), BronzeTools.BRONZE_TONGS.get());
    }

    @SubscribeEvent
    static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            AvatarRenderer<?> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) renderer.addLayer(new GripLayer(renderer));
        }
    }

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (!(avatar instanceof Player player)) return;
                grip(player, state);
                rummage(player, state);
            }
        });
    }

    // ---------------------------------------------------------------- render state

    /** Tongs in one hand and something hot in the other: the hot piece moves to the tongs' jaws, its hand goes empty. */
    public static void grip(LivingEntity entity, HumanoidRenderState state) {
        HumanoidArm tongs = state.rightHandItemStack.is(BronzeTools.BRONZE_TONGS.get()) ? HumanoidArm.RIGHT
                : state.leftHandItemStack.is(BronzeTools.BRONZE_TONGS.get()) ? HumanoidArm.LEFT : null;
        if (tongs == null) return;
        boolean right = tongs == HumanoidArm.LEFT;
        ItemStack hot = right ? state.rightHandItemStack : state.leftHandItemStack;
        if (hot.isEmpty() || !BronzeTools.hot(hot, entity.level().getGameTime())) return;
        ItemStackRenderState item = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(item, hot, ItemDisplayContext.GROUND, entity);
        state.setRenderData(GRIP, new Grip(item, tongs));
        (right ? state.rightHandItemState : state.leftHandItemState).clear();
        if (right) state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        else state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
    }

    private static void rummage(Player player, AvatarRenderState state) {
        byte how = player.getData(PackMenu.RUMMAGING);
        if (how == 0) return;
        state.setRenderData(RUMMAGE, how);
        state.xRot = Math.max(state.xRot, 45.0F); // looking down into it
        if (how == PackMenu.WORN) {
            state.setRenderData(BAG_X, EquipmentClient.satchel(state) ? 2.5F : -2.3F);
            if (state.mainArm == HumanoidArm.RIGHT) state.rightArmPose = RUMMAGE_POSE.getValue();
            else state.leftArmPose = RUMMAGE_POSE.getValue();
        } else if (bag(state.rightHandItemStack)) {
            state.rightArmPose = RUMMAGE_POSE.getValue();
        } else {
            state.leftArmPose = RUMMAGE_POSE.getValue();
        }
    }

    private static boolean bag(ItemStack stack) {
        return stack.is(Mechanical.BARK_BASKET.get()) || stack.is(Mechanical.LEATHER_SATCHEL.get()) || stack.is(Mechanical.LEATHER_POUCH.get())
                || stack.getItem() instanceof com.mechanical.equipment.QuiverItem;
    }

    /** Rummaging in the satchel or pouch they wear (WornSatchelModel / WornPouchModel pull it forward). */
    static boolean rummagingWorn(HumanoidRenderState state) {
        Byte how = state.getRenderData(RUMMAGE);
        return how != null && how == PackMenu.WORN;
    }

    // ---------------------------------------------------------------- poses

    private static ModelPart arm(HumanoidModel<?> model, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    /** +1 for the right arm, -1 for the left: a positive zRot swings the right arm out, the left one in. */
    private static int side(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? 1 : -1;
    }

    /** Vanilla's arm swing phase for this arm (it swings with the opposite foot). */
    private static float phase(HumanoidRenderState state, HumanoidArm arm) {
        return state.walkAnimationPos * 0.6662F + (arm == HumanoidArm.RIGHT ? Mth.PI : 0.0F);
    }

    private static float speed(HumanoidRenderState state) {
        return Math.min(1.0F, state.walkAnimationSpeed / state.speedValue);
    }

    /**
     * Walking: the staff arm swings with the opposite foot like a normal arm swing, but is lifted while it carries the staff
     * forward and comes down to plant the tip, then stays low while the body walks past it. Standing: at rest, upright.
     */
    static void poseStaff(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        float phase = phase(state, arm), speed = speed(state);
        float swing = 0.55F * Mth.cos(phase) * speed;
        part.xRot = -Mth.PI / 10.0F + swing; // -PI/10: vanilla's held-item angle, the staff model is upright at it
        part.yRot = 0.0F;
        float sin = Mth.sin(phase); // > 0 while the arm moves forward
        // forward: lifted, tip off the ground; back: the arm drops as far as the swing raised the tip (it is 20 px down, 5 ahead
        // of the shoulder), so the tip stays planted while the body walks past it (at most 1.5 px, or the shoulder gapes)
        part.y += sin > 0.0F ? -1.5F * sin * speed : Math.min(1.5F, 20.0F * (1.0F - Mth.cos(swing)) - 5.0F * Mth.sin(swing));
    }

    /** The basket in the crook of the arm (bark_basket.json's display): arm a little forward and out round it, swaying with the steps, breathing when still. */
    static void poseBasket(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        float phase = phase(state, arm), speed = speed(state);
        part.xRot = -0.55F + 0.12F * Mth.cos(phase) * speed;
        part.yRot = side(arm) * -0.15F;
        part.zRot = side(arm) * (0.12F + 0.04F * Mth.sin(phase) * speed);
        part.y += 0.3F * Mth.sin(state.ageInTicks * 0.08F); // breathing
    }

    /** Tongs held out forward, jaws first (bronze_tongs.json's display); a little higher while they grip something hot. */
    static void poseTongs(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        boolean gripping = state.getRenderData(GRIP) != null;
        part.xRot = (gripping ? -1.15F : -0.8F) + 0.12F * Mth.cos(phase(state, arm)) * speed(state);
        part.yRot = side(arm) * -0.25F;
    }

    /** Head down (the modifier), hands rummaging: in the worn bag at BAG_X, or one hand holds the bag up and the other digs in. */
    static void poseRummage(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float t = state.ageInTicks;
        Float bagX = state.getRenderData(BAG_X);
        for (HumanoidArm a : HumanoidArm.values()) {
            ModelPart part = arm(model, a);
            int side = side(a);
            float wiggleX = 0.12F * Mth.sin(t * 0.8F + side), wiggleY = 0.1F * Mth.cos(t * 0.6F + side * 2);
            if (bagX != null) {
                // reach down-forward to the bag: yRot turns the forward arm across towards it (7 px horizontal reach)
                float shoulder = -5.0F * side;
                part.xRot = -0.8F + wiggleX;
                part.yRot = (float) -Math.atan2((bagX - shoulder) * side, 7.0F) * side + wiggleY;
            } else if (a == arm) {
                part.xRot = -0.9F; // holds the bag up in front
                part.yRot = side * -0.35F;
            } else {
                part.xRot = -1.05F + wiggleX; // digs in
                part.yRot = side * -0.55F + wiggleY;
            }
            part.zRot = 0.0F;
        }
    }

    // ---------------------------------------------------------------- the gripped piece

    /** Draws the hot piece held in the tongs at their jaws (render data from {@link #grip}). Players and tribespeople. */
    public static final class GripLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        public GripLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
            Grip grip = state.getRenderData(GRIP);
            if (grip == null || state.isInvisible) return;
            poseStack.pushPose();
            getParentModel().translateToHand(state, grip.arm(), poseStack);
            poseStack.translate(0.0F, JAWS / 16.0F, -0.0625F); // out along the arm to the jaws
            poseStack.rotateDegrees(Axis.XP, -90.0F); // the piece lies across the jaws, level with the arm
            grip.item().submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }

    private HoldPoses() {}
}
