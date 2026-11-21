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
import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
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
 * <li>Carrying: buckets and the water jug hang at the side, firewood / rough logs and the kettle in both arms, hammers and
 * axes on the shoulder while walking, a spear low in both hands while sneaking, the rod held out, the paddle stroking when
 * seated, the pan swirled while panning (display transforms: scripts/gen_hold_displays.py).</li>
 * <li>Strokes for click-used items (fire drill, broom, flail, paint and brush, chisel, bone flute), driven by the arm swing
 * the click makes (sent to everyone), which {@link #extract} takes off vanilla's swing.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HoldPoses {
    public static final EnumProxy<HumanoidModel.ArmPose> STAFF_POSE = pose(false, false, HoldPoses::poseStaff);
    public static final EnumProxy<HumanoidModel.ArmPose> BASKET_POSE = pose(false, false, HoldPoses::poseBasket);
    public static final EnumProxy<HumanoidModel.ArmPose> TONGS_POSE = pose(false, false, HoldPoses::poseTongs);
    /** Both arms: set on the arm holding the bag (or the main arm for a worn one) by the render-state modifier. */
    public static final EnumProxy<HumanoidModel.ArmPose> RUMMAGE_POSE = pose(true, true, HoldPoses::poseRummage);
    public static final EnumProxy<HumanoidModel.ArmPose> BUCKET_POSE = pose(false, false, HoldPoses::poseBucket);
    public static final EnumProxy<HumanoidModel.ArmPose> CARRY_POSE = pose(true, true, (m, s, a) -> poseCarry(m, s, a, false));
    public static final EnumProxy<HumanoidModel.ArmPose> HEAVY_POSE = pose(true, true, (m, s, a) -> poseCarry(m, s, a, true));
    public static final EnumProxy<HumanoidModel.ArmPose> PADDLE_POSE = pose(true, true, HoldPoses::posePaddle);
    public static final EnumProxy<HumanoidModel.ArmPose> SHOULDER_POSE = pose(false, false, HoldPoses::poseShoulder);
    public static final EnumProxy<HumanoidModel.ArmPose> SPEAR_LOW_POSE = pose(true, true, HoldPoses::poseSpearLow);
    public static final EnumProxy<HumanoidModel.ArmPose> ROD_POSE = pose(false, false, HoldPoses::poseRod);
    public static final EnumProxy<HumanoidModel.ArmPose> PAN_POSE = pose(true, true, HoldPoses::posePan);
    // strokes: one per swing of the arm (the click that uses the item swings it, and swings are sent to everyone)
    public static final EnumProxy<HumanoidModel.ArmPose> FIRE_DRILL_POSE = pose(true, true, HoldPoses::poseFireDrill);
    public static final EnumProxy<HumanoidModel.ArmPose> BROOM_POSE = pose(true, true, HoldPoses::poseBroom);
    public static final EnumProxy<HumanoidModel.ArmPose> FLAIL_POSE = pose(true, true, HoldPoses::poseFlail);
    public static final EnumProxy<HumanoidModel.ArmPose> PAINT_POSE = pose(false, false, HoldPoses::posePaint);
    public static final EnumProxy<HumanoidModel.ArmPose> CHISEL_POSE = pose(true, true, HoldPoses::poseChisel);
    public static final EnumProxy<HumanoidModel.ArmPose> FLUTE_POSE = pose(true, true, HoldPoses::poseFlute);

    /** The hot item held in the tongs, resolved for drawing at the jaws, and the tongs' arm. */
    private record Grip(ItemStackRenderState item, HumanoidArm arm) {}
    private static final ContextKey<Grip> GRIP = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tongs_grip"));
    private static final ContextKey<Byte> RUMMAGE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "rummaging"));
    /** Where the worn bag is that the hands go to (model x): the satchel hangs on the left, the pouch on the right. */
    private static final ContextKey<Float> BAG_X = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "bag_x"));
    /** Progress (0..1) of the swing that uses a stroke item, taken off vanilla's attack swing so only the stroke shows. */
    private static final ContextKey<Float> STROKE = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "stroke"));
    /** How fast the boat the paddler sits in goes (blocks/tick). */
    private static final ContextKey<Float> BOAT_SPEED = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "boat_speed"));
    /** How far past the shoulder pivot (model px along the arm) the tongs' jaws are, by bronze_tongs.json's display. */
    private static final float JAWS = 21.0F;

    private static EnumProxy<HumanoidModel.ArmPose> pose(boolean twoHanded, boolean affectsOffhand, IArmPoseTransformer transformer) {
        return new EnumProxy<>(HumanoidModel.ArmPose.class, twoHanded, affectsOffhand, transformer);
    }

    private static IClientItemExtensions extensions(EnumProxy<HumanoidModel.ArmPose> pose) {
        return extensions(pose, e -> true);
    }

    /** The pose while {@code when} holds for the holder, otherwise vanilla's. */
    private static IClientItemExtensions extensions(EnumProxy<HumanoidModel.ArmPose> pose, Predicate<LivingEntity> when) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return when.test(entity) ? pose.getValue() : null;
            }
        };
    }

    /** The pose for an item, or null for vanilla's. */
    private static @Nullable IClientItemExtensions extensionsFor(Item item) {
        if (item == Mechanical.WALKING_STAFF.get() || item == Mechanical.ELDERS_STAFF.get()) return extensions(STAFF_POSE);
        if (item == Mechanical.BARK_BASKET.get()) return extensions(BASKET_POSE);
        if (item == BronzeTools.BRONZE_TONGS.get()) return extensions(TONGS_POSE);
        if (item instanceof com.mechanical.WaterJugItem || item instanceof com.mechanical.ClayBucketItem || item instanceof com.mechanical.BronzeBucketItem
                || item == BronzeTools.BRONZE_MILK_BUCKET.get()) return extensions(BUCKET_POSE);
        if (item instanceof com.mechanical.FirewoodItem || item instanceof com.mechanical.RoughLogItem) return extensions(CARRY_POSE);
        if (item == com.mechanical.Bronze.BRONZE_KETTLE.get()) return extensions(HEAVY_POSE);
        if (item == com.mechanical.Watercraft.PADDLE.get()) return extensions(PADDLE_POSE, Entity::isPassenger);
        if (item == com.mechanical.Ironworking.BRONZE_SMITHING_HAMMER.get() || item == com.mechanical.Smithing.IRON_SMITHING_HAMMER.get()
                || item == BronzeTools.BRONZE_QUARRY_HAMMER.get() || item == com.mechanical.Smithing.STEEL_QUARRY_HAMMER.get()
                || item == Mechanical.FLINT_AXE.get() || item == Mechanical.CRUDE_STONE_AXE.get() || item == com.mechanical.Bronze.BRONZE_AXE.get())
            return extensions(SHOULDER_POSE);
        if (item == Mechanical.FLINT_SPEAR.get() || item == com.mechanical.Bronze.BRONZE_SPEAR.get() || item == com.mechanical.Smithing.STEEL_SPEAR.get())
            return extensions(SPEAR_LOW_POSE, e -> e.isCrouching() && !e.isUsingItem()); // otherwise vanilla's spear poses
        if (item == Mechanical.CRUDE_FISHING_ROD.get() || item instanceof com.mechanical.BoneHookRodItem) return extensions(ROD_POSE);
        if (item instanceof com.mechanical.PanItem) return extensions(PAN_POSE, LivingEntity::isUsingItem);
        if (item instanceof com.mechanical.FireDrillItem) return extensions(FIRE_DRILL_POSE);
        if (item instanceof com.mechanical.BroomItem) return extensions(BROOM_POSE);
        if (item instanceof com.mechanical.FlailItem) return extensions(FLAIL_POSE);
        if (item instanceof com.mechanical.PaintbrushItem || item instanceof com.mechanical.PaintItem) return extensions(PAINT_POSE);
        if (item == BronzeTools.BRONZE_CHISEL.get()) return extensions(CHISEL_POSE);
        if (item instanceof com.mechanical.wildlife.waterfowl.BoneFluteItem) return extensions(FLUTE_POSE);
        return null;
    }

    private static boolean strokes(HumanoidModel.ArmPose pose) {
        return pose == FIRE_DRILL_POSE.getValue() || pose == BROOM_POSE.getValue() || pose == FLAIL_POSE.getValue()
                || pose == PAINT_POSE.getValue() || pose == CHISEL_POSE.getValue() || pose == FLUTE_POSE.getValue();
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Mechanical.MODID)) continue;
            IClientItemExtensions extensions = extensionsFor(item);
            if (extensions != null) event.registerItem(extensions, item);
        }
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
                extract(player, state);
                rummage(player, state);
                if (player.getVehicle() != null) state.setRenderData(BOAT_SPEED, (float) player.getVehicle().getDeltaMovement().horizontalDistance());
            }
        });
    }

    // ---------------------------------------------------------------- render state

    /** For players and tribespeople, after the arm poses are picked: the tongs' grip and a stroke in progress. */
    public static void extract(LivingEntity entity, HumanoidRenderState state) {
        grip(entity, state);
        if (state.swingAnimation <= 0.0F || state.currentSwing == null) return;
        if (strokes(state.getArmPose(state.currentSwing.hand().asArm(state.mainArm)))) {
            state.setRenderData(STROKE, state.swingAnimation);
            state.swingAnimation = 0.0F;
        }
    }

    /** Tongs in one hand and something hot (or a crucible) in the other: it moves to the tongs' jaws, its hand goes empty. */
    private static void grip(LivingEntity entity, HumanoidRenderState state) {
        HumanoidArm tongs = state.rightHandItemStack.is(BronzeTools.BRONZE_TONGS.get()) ? HumanoidArm.RIGHT
                : state.leftHandItemStack.is(BronzeTools.BRONZE_TONGS.get()) ? HumanoidArm.LEFT : null;
        if (tongs == null) return;
        boolean right = tongs == HumanoidArm.LEFT;
        ItemStack hot = right ? state.rightHandItemStack : state.leftHandItemStack;
        if (hot.isEmpty() || !BronzeTools.hot(hot, entity.level().getGameTime()) && !(hot.getItem() instanceof com.mechanical.CrucibleItem)) return;
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

    /** Vanilla's ITEM arm (for a stroke item between strokes). */
    private static void rest(ModelPart part) {
        part.xRot = part.xRot * 0.5F - Mth.PI / 10.0F;
        part.yRot = 0.0F;
    }

    private static ItemStack held(HumanoidRenderState state, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? state.rightHandItemStack : state.leftHandItemStack;
    }

    private static boolean full(ItemStack stack) {
        if (stack.getItem() instanceof com.mechanical.WaterJugItem) return com.mechanical.WaterJugItem.water(stack) > 0;
        return !stack.is(Mechanical.CLAY_BUCKET.get()) && !stack.is(BronzeTools.BRONZE_BUCKET.get());
    }

    /** Hanging at the side on a straight arm, swinging with the steps; full, it swings less, drags the shoulder down and the head leans away. */
    static void poseBucket(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        boolean full = full(held(state, arm));
        part.xRot = (full ? 0.25F : 0.45F) * Mth.cos(phase(state, arm)) * speed(state);
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        if (full) {
            part.y += 1.0F;
            model.head.zRot = -side(arm) * 0.08F;
        }
    }

    /** Both arms forward round a load in front of the chest (firewood, a rough log; heavy: the kettle, lower), bobbing with the steps. */
    static void poseCarry(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm, boolean heavy) {
        float bob = 0.05F * Mth.cos(state.walkAnimationPos * 0.6662F * 2.0F) * speed(state);
        for (HumanoidArm a : HumanoidArm.values()) {
            ModelPart part = arm(model, a);
            part.xRot = (heavy ? -1.0F : -0.9F) + bob;
            part.yRot = side(a) * (heavy ? -0.45F : -0.4F);
            part.zRot = 0.0F;
            if (heavy) part.y += 0.7F;
        }
    }

    /** Seated: both hands on the paddle at the holder's side, stroking faster the faster the boat goes. */
    static void posePaddle(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        Float boat = state.getRenderData(BOAT_SPEED);
        float k = boat == null ? 0.3F : Math.min(1.0F, boat * 6.0F); // tribespeople: a slow stroke
        float stroke = Mth.sin(state.ageInTicks * (0.15F + 0.25F * k)) * (0.15F + 0.25F * k);
        ModelPart low = arm(model, arm), high = arm(model, arm.getOpposite());
        low.xRot = -0.7F + stroke;
        low.yRot = side(arm) * -0.2F;
        high.xRot = -1.15F + stroke;
        high.yRot = side(arm.getOpposite()) * -0.7F; // across the body to the top of the shaft
        low.zRot = high.zRot = 0.0F;
    }

    /** Walking: the hammer or axe rests on the shoulder, standing it is held as usual (vanilla's swing plays over both). */
    static void poseShoulder(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        float w = Math.min(1.0F, speed(state) * 2.5F);
        rest(part);
        part.xRot = Mth.lerp(w, part.xRot, -1.0F);
        part.yRot = Mth.lerp(w, 0.0F, side(arm) * -0.45F);
    }

    /** Sneaking with a spear: held low in both hands, point forward (the hunting stance). */
    static void poseSpearLow(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart front = arm(model, arm), back = arm(model, arm.getOpposite());
        front.xRot = -0.5F;
        front.yRot = side(arm) * -0.2F;
        back.xRot = -0.7F;
        back.yRot = side(arm.getOpposite()) * -0.6F;
    }

    /** The rod held out forward, line over the water. */
    static void poseRod(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        part.xRot = -0.75F + 0.1F * Mth.cos(phase(state, arm)) * speed(state);
        part.yRot = side(arm) * -0.1F;
    }

    /** Panning: the pan held flat in front in both hands, swirled round in circles. */
    static void posePan(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float t = state.ticksUsingItem * 0.6F;
        for (HumanoidArm a : HumanoidArm.values()) {
            ModelPart part = arm(model, a);
            part.xRot = -0.95F + 0.1F * Mth.sin(t);
            part.yRot = side(a) * -0.4F + 0.1F * Mth.cos(t);
            part.zRot = 0.0F;
        }
    }

    private static float stroke(HumanoidRenderState state) {
        Float p = state.getRenderData(STROKE);
        return p == null ? -1.0F : p;
    }

    /** Between strokes held as usual; a stroke: hands together low in front spinning the drill between the palms, head bent down to the tinder. */
    static void poseFireDrill(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float p = stroke(state);
        if (p < 0.0F) {
            rest(arm(model, arm));
            return;
        }
        for (HumanoidArm a : HumanoidArm.values()) {
            ModelPart part = arm(model, a);
            part.xRot = -0.55F + side(a) * 0.25F * Mth.sin(p * 4.0F * Mth.PI); // palms rubbing past each other
            part.yRot = side(a) * -0.5F;
            part.zRot = 0.0F;
        }
        model.head.xRot += 0.35F * Mth.sin(p * Mth.PI);
    }

    /** Both hands low on the handle; a stroke sweeps it across and back. */
    static void poseBroom(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float p = stroke(state), sweep = p < 0.0F ? 0.0F : 0.45F * Mth.sin(p * 2.0F * Mth.PI);
        ModelPart low = arm(model, arm), high = arm(model, arm.getOpposite());
        low.xRot = -0.55F;
        low.yRot = side(arm) * -0.3F + sweep;
        high.xRot = -0.75F;
        high.yRot = side(arm.getOpposite()) * -0.5F + sweep;
        low.zRot = high.zRot = 0.0F;
    }

    /** A stroke: both hands swing the flail up over the shoulder and down to the ground. */
    static void poseFlail(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float p = stroke(state);
        if (p < 0.0F) {
            rest(arm(model, arm));
            return;
        }
        float x = p < 0.35F ? Mth.lerp(p / 0.35F, -0.3F, -2.8F)
                : p < 0.7F ? Mth.lerp((p - 0.35F) / 0.35F, -2.8F, -0.5F) : Mth.lerp((p - 0.7F) / 0.3F, -0.5F, -0.3F);
        for (HumanoidArm a : HumanoidArm.values()) {
            ModelPart part = arm(model, a);
            part.xRot = x;
            part.yRot = side(a) * -0.3F;
            part.zRot = 0.0F;
        }
    }

    /** A stroke: small brush strokes forward at the block. */
    static void posePaint(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm(model, arm);
        float p = stroke(state);
        if (p < 0.0F) {
            rest(part);
            return;
        }
        part.xRot = -1.1F + 0.2F * Mth.sin(p * 4.0F * Mth.PI);
        part.yRot = side(arm) * -0.1F + 0.1F * Mth.cos(p * 4.0F * Mth.PI);
    }

    /** The chisel held to the stone; a stroke: the other hand (with a hammer if it holds one) strikes it. */
    static void poseChisel(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart chisel = arm(model, arm);
        chisel.xRot = -1.15F;
        chisel.yRot = side(arm) * -0.3F;
        float p = stroke(state);
        if (p < 0.0F) return;
        ModelPart striker = arm(model, arm.getOpposite());
        striker.xRot = -1.25F - 1.2F * Mth.sin(p * Mth.PI); // up and down onto the chisel's head
        striker.yRot = side(arm.getOpposite()) * -0.5F;
        striker.zRot = 0.0F;
    }

    /** A stroke (playing it): raised to the mouth in both hands, like vanilla's goat horn. */
    static void poseFlute(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        if (stroke(state) < 0.0F) {
            rest(arm(model, arm));
            return;
        }
        ModelPart near = arm(model, arm), far = arm(model, arm.getOpposite());
        near.xRot = far.xRot = -1.45F + model.head.xRot * 0.5F;
        near.yRot = side(arm) * -0.55F;
        far.yRot = side(arm.getOpposite()) * -0.4F;
        near.zRot = far.zRot = 0.0F;
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
