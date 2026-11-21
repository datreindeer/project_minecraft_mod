package com.mechanical.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mechanical.BronzeTools;
import com.mechanical.Mechanical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * First-person counterparts of the {@link HoldPoses} poses, for the local player (the kind is the {@code fp} column of
 * HoldPoses' mapping table). One-handed holds move the held item ({@link #transform}, from
 * IClientItemExtensions#applyForgeHandTransform: vanilla's item position, then the hold); holds that need arms are drawn
 * from RenderHandEvent ({@link #onRenderHand}): loads on both forearms (firewood, rough logs, the kettle) or on both hands
 * (buckets, crucibles), the basket hanging from the forearm, the fire drill spun between both palms for as long as the
 * drilling goes on, the tongs gripping the hot piece at their jaws with the other hand empty, and the chisel struck by the
 * other hand. Walking motion comes from the player's walk animation (in step with the third-person poses), strokes from
 * the attack swing the click makes. The fishing rod keeps vanilla's (the cast line is tied to vanilla's hand position);
 * rummaging in a bag shows in third person only.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FirstPersonHolds {
    public enum Fp { STAFF, BASKET, BUCKET, CARRY, HEAVY, UNDER, SHOULDER, SPEAR_LOW, PADDLE, PAN, FIRE_DRILL, BROOM, FLAIL, PAINT, CHISEL, FLUTE }

    /** bronze_tongs' first-person display (vanilla handheld's; ItemTransform mirrors the left hand's back to these angles) and its jaws (sprite px, y up). */
    private static final float[] TONGS_ROT = {0.0F, -90.0F, 25.0F}, TONGS_AT = {1.13F, 3.2F, 1.13F}, JAWS = {12.8F, 13.8F, 8.0F};
    private static final float TONGS_SCALE = 0.68F;

    /** The first-person kind of a held item, or null. */
    static @Nullable Fp kind(ItemStack stack) {
        return IClientItemExtensions.of(stack) instanceof HoldPoses.Held held ? held.fp() : null;
    }

    private static int side(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? 1 : -1;
    }

    /** Vanilla's held-item position (FirstPersonHandsAndItemsRenderer#applyItemArmTransform). */
    private static void itemPos(PoseStack ps, int s, float equip) {
        ps.translate(s * 0.56F, -0.52F - 0.6F * equip, -0.72F);
    }

    /** Vanilla's attack swing of a held item (FirstPersonHandsAndItemsRenderer#swingArm). */
    private static void swingArm(PoseStack ps, int s, float a) {
        float sq = Mth.sqrt(a);
        ps.translate(s * -0.4F * Mth.sin(sq * Mth.PI), 0.2F * Mth.sin(sq * Mth.TWO_PI), -0.2F * Mth.sin(a * Mth.PI));
        ps.rotateDegrees(Axis.YP, s * (45.0F + Mth.sin(a * a * Mth.PI) * -20.0F));
        ps.rotateDegrees(Axis.ZP, s * Mth.sin(sq * Mth.PI) * -20.0F);
        ps.rotateDegrees(Axis.XP, Mth.sin(sq * Mth.PI) * -80.0F);
        ps.rotateDegrees(Axis.YP, s * -45.0F);
    }

    /**
     * The held item's transform for a one-handed hold; false leaves it to vanilla (attacking with a staff or hammer, a
     * stroke item between strokes, the two-armed holds that {@link #onRenderHand} draws).
     */
    static boolean transform(Fp fp, PoseStack ps, Player player, HumanoidArm arm, ItemStack stack, float pt, float equip, float swing) {
        if (fp != Fp.PAN && player.isUsingItem() && player.getUsedItemHand().asArm(player.getMainArm()) == arm) return false; // drinking from the jug...
        int s = side(arm);
        float phase = player.walkAnimation.position(pt) * 0.6662F, speed = Math.min(1.0F, player.walkAnimation.speed(pt));
        float age = player.tickCount + pt, step = Mth.abs(Mth.sin(phase)) * speed;
        switch (fp) {
            case STAFF -> { // carried forward lifted, planted, the tip swinging with the steps
                if (swing > 0.0F) return false;
                itemPos(ps, s, equip);
                ps.translate(0.0F, 0.07F * Math.max(0.0F, Mth.sin(phase)) * speed + 0.006F * Mth.sin(age * 0.08F), -0.07F * Mth.cos(phase) * speed);
                ps.rotateDegrees(Axis.XP, -7.0F * Mth.cos(phase) * speed);
            }
            case BUCKET -> { // hanging low, swinging from the handle with the steps; full: lower, less and later
                boolean full = HoldPoses.full(stack);
                ps.translate(s * 0.56F, (full ? -0.5F : -0.44F) - 0.02F * step - 0.6F * equip, -0.8F);
                ps.translate(0.0F, 0.22F, 0.0F);
                ps.rotateDegrees(Axis.XP, (full ? 7.0F : 14.0F) * Mth.sin(phase - (full ? 0.8F : 0.4F)) * speed);
                ps.rotateDegrees(Axis.ZP, s * (full ? 2.0F : 4.0F) * Mth.cos(phase) * speed);
                ps.translate(0.0F, -0.22F, 0.0F);
            }
            case SHOULDER -> { // walking: the head back over the shoulder, the handle coming down in front
                if (swing > 0.0F) return false;
                float w = Math.min(1.0F, speed * 2.5F);
                itemPos(ps, s, equip);
                ps.translate(s * -0.08F * w, 0.0F, 0.04F * w);
                ps.rotateDegrees(Axis.XP, 20.0F * w);
                ps.rotateDegrees(Axis.ZP, s * -25.0F * w);
            }
            case SPEAR_LOW -> { // sneaking: lower, point forward
                if (swing > 0.0F) return false;
                itemPos(ps, s, equip);
                ps.translate(s * -0.06F, -0.12F, 0.04F);
                ps.rotateDegrees(Axis.XP, -15.0F);
            }
            case PADDLE -> { // seated: stroking, faster the faster the boat goes
                float k = player.getVehicle() == null ? 0.3F : Math.min(1.0F, (float) player.getVehicle().getDeltaMovement().horizontalDistance() * 6.0F);
                float stroke = Mth.sin(age * (0.15F + 0.25F * k)) * (0.15F + 0.25F * k);
                itemPos(ps, s, equip);
                ps.translate(s * -0.1F, -0.05F, 0.3F * stroke);
                ps.rotateDegrees(Axis.XP, 50.0F * stroke);
            }
            case PAN -> { // panning: in towards the middle, swirled round
                float t = player.getTicksUsingItem(pt) * 0.6F;
                itemPos(ps, s, equip);
                ps.translate(s * -0.2F + 0.04F * Mth.cos(t), 0.05F, 0.04F * Mth.sin(t));
                ps.rotateDegrees(Axis.YP, 8.0F * Mth.sin(t));
            }
            case BROOM, FLAIL, PAINT, CHISEL, FLUTE -> {
                if (swing <= 0.0F) return false;
                itemPos(ps, s, equip);
                stroke(fp, ps, s, swing);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** One stroke of a click-used item, p 0..1 over the swing. */
    private static void stroke(Fp fp, PoseStack ps, int s, float p) {
        switch (fp) {
            case BROOM -> { // down to the floor and swept across
                float k = Mth.sin(p * Mth.PI);
                ps.translate(s * -0.12F * k, -0.08F * k, -0.05F * k);
                ps.rotateDegrees(Axis.XP, -30.0F * k);
                ps.rotateDegrees(Axis.YP, s * 40.0F * Mth.sin(p * Mth.TWO_PI));
            }
            case FLAIL -> { // lifted back over the shoulder, then down onto the floor in front
                float x = p < 0.35F ? p / 0.35F * 70.0F : p < 0.7F ? 70.0F - (p - 0.35F) / 0.35F * 130.0F : -60.0F + (p - 0.7F) / 0.3F * 60.0F;
                float up = Math.max(0.0F, x) / 70.0F;
                ps.translate(0.0F, 0.1F * up, 0.05F * up);
                ps.rotateDegrees(Axis.XP, x);
            }
            case PAINT -> { // small strokes at the block
                ps.translate(0.0F, 0.04F * Mth.sin(p * 4.0F * Mth.PI), -0.1F * Mth.sin(p * Mth.PI));
                ps.rotateDegrees(Axis.XP, -12.0F * Mth.sin(p * Mth.PI));
            }
            case CHISEL -> { // held to the stone, jolted forward when the other hand hits it
                float hit = Math.max(0.0F, 1.0F - Mth.abs(p - 0.5F) * 6.0F);
                ps.translate(s * -0.15F, 0.02F, -0.08F - 0.05F * hit);
                ps.rotateDegrees(Axis.XP, -40.0F);
            }
            case FLUTE -> { // up to the mouth in the middle, turned to face us
                float k = Math.min(1.0F, Mth.sin(p * Mth.PI) * 1.8F);
                ps.translate(s * -0.56F * k, 0.2F * k, 0.15F * k);
                ps.rotateDegrees(Axis.YP, s * 90.0F * k);
                ps.rotateDegrees(Axis.ZP, s * -45.0F * k);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- holds drawn with the arms

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        AvatarRenderState avatar = event.getPlayerState().avatarRenderState;
        if (player == null || avatar == null) return;
        FirstPersonHandsAndItemsRenderState hands = event.getPlayerState().firstPersonHandsAndItems;
        boolean main = event.getHand() == InteractionHand.MAIN_HAND;
        HumanoidArm arm = main ? avatar.mainArm : avatar.mainArm.getOpposite();
        float mainSwing = hands.attackHand == InteractionHand.MAIN_HAND ? avatar.swingAnimation : 0.0F;
        ItemStack mainItem = player.getMainHandItem(), offItem = player.getOffhandItem();
        Fp mainKind = kind(mainItem);

        // tongs in one hand, something hot in the other: the piece at the jaws, its hand empty
        boolean tongsMain = mainItem.is(BronzeTools.TONGS), tongsOff = offItem.is(BronzeTools.TONGS);
        ItemStack hot = tongsMain ? offItem : mainItem;
        if (tongsMain != tongsOff && HoldPoses.grippable(hot, player.level().getGameTime())) {
            event.setCanceled(true);
            if (main == tongsMain) {
                grip(event, player, arm, hot, main ? hands.mainHandRenderState : hands.offHandRenderState);
            } else if (main && !avatar.isInvisible) {
                playerArm(event, avatar, arm, event.getEquipProgress(), event.getSwingProgress());
            }
            return;
        }

        // a load in both arms, or the fire drill spun between both palms: drawn with the main hand, the off hand's item hidden
        boolean drilling = mainKind == Fp.FIRE_DRILL && HoldPoses.drilling(player, mainSwing > 0.0F || usingOnBlock(player));
        Fp offKind = kind(offItem);
        ItemStack load = drilling || twoArmed(mainKind) ? mainItem : twoArmed(offKind) ? offItem : null;
        if (load != null) {
            event.setCanceled(true);
            if (main) bothArms(event, player, avatar, load == mainItem ? mainKind : offKind, load);
            return;
        }
        // the basket hanging from the forearm, whichever hand holds it
        if (kind(main ? mainItem : offItem) == Fp.BASKET) {
            event.setCanceled(true);
            basket(event, player, avatar, arm, main ? mainItem : offItem);
            return;
        }

        // the chisel's stroke: the other hand (empty, or with its hammer) strikes it
        if (!main && mainKind == Fp.CHISEL && mainSwing > 0.0F) {
            int s = side(arm);
            PoseStack ps = event.getPoseStack();
            if (offItem.isEmpty()) {
                if (avatar.isInvisible) return;
                ps.pushPose();
                ps.translate(s * -0.18F, 0.0F, 0.0F); // in towards the chisel
                playerArm(event, avatar, arm, event.getEquipProgress(), mainSwing);
                ps.popPose();
            } else {
                event.setCanceled(true);
                ps.pushPose();
                itemPos(ps, s, event.getEquipProgress());
                swingArm(ps, s, mainSwing);
                hands.offHandRenderState.submit(ps, event.getSubmitNodeCollector(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
                ps.popPose();
            }
        }
    }

    /** The local player holding right-click on a block (the fire drill's repeated use, before its swings come back). */
    static boolean usingOnBlock(net.minecraft.world.entity.Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        return entity == mc.player && mc.options.keyUse.isDown() && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
    }

    private static boolean twoArmed(@Nullable Fp fp) {
        return fp == Fp.CARRY || fp == Fp.HEAVY || fp == Fp.UNDER;
    }

    /** The held item unturned (ItemDisplayContext.NONE: centred, item x across, y up, face towards us). */
    private static ItemStackRenderState unturned(ItemStack stack, Player player) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(state, stack, ItemDisplayContext.NONE, player);
        return state;
    }

    /** Both hands up in front (vanilla's two-handed map hands), the load lying on the forearms or standing on the hands, bobbing with the steps. */
    private static void bothArms(RenderHandEvent event, Player player, AvatarRenderState avatar, Fp fp, ItemStack stack) {
        PoseStack ps = event.getPoseStack();
        float pt = event.getPartialTick(), speed = Math.min(1.0F, player.walkAnimation.speed(pt));
        boolean drill = fp == Fp.FIRE_DRILL;
        float p = ((player.tickCount + pt) / 10.0F) % 1.0F; // the drill's spin, continuous while drilling
        float bob = drill ? 0.0F : -0.025F * Mth.abs(Mth.sin(player.walkAnimation.position(pt) * 0.6662F)) * speed;
        ps.pushPose();
        ps.translate(0.0F, 0.04F - 1.2F * event.getEquipProgress() + bob + (fp == Fp.CARRY || fp == Fp.HEAVY ? 0.1F : 0.0F), -0.72F);
        if (!avatar.isInvisible) {
            float rub = drill ? 0.06F * Mth.sin(p * 4.0F * Mth.PI) : 0.0F; // palms rubbing past each other
            ps.pushPose();
            ps.rotateDegrees(Axis.YP, 90.0F);
            mapHand(event, avatar, HumanoidArm.RIGHT, -rub);
            mapHand(event, avatar, HumanoidArm.LEFT, rub);
            ps.popPose();
        }
        switch (fp) {
            case FIRE_DRILL -> { // upright between the palms, point down, spinning
                ps.translate(0.0F, -0.15F, -0.05F);
                ps.rotateDegrees(Axis.YP, p * 720.0F);
                ps.scale(0.55F, 0.55F, 0.55F);
            }
            case CARRY -> { // lying across both forearms, close and low
                ps.translate(0.0F, -0.2F, 0.1F);
                ps.rotateDegrees(Axis.XP, 15.0F);
                ps.scale(0.5F, 0.5F, 0.5F);
            }
            case HEAVY -> {
                ps.translate(0.0F, -0.36F, 0.12F);
                ps.rotateDegrees(Axis.YP, 20.0F);
                ps.scale(0.6F, 0.6F, 0.6F);
            }
            default -> { // UNDER: standing on both hands
                ps.translate(0.0F, -0.3F, 0.08F);
                ps.scale(0.5F, 0.5F, 0.5F);
            }
        }
        unturned(stack, player).submit(ps, event.getSubmitNodeCollector(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    /** The bare forearm low at the side with the basket hanging from it by the handle, swaying with the steps. */
    private static void basket(RenderHandEvent event, Player player, AvatarRenderState avatar, HumanoidArm arm, ItemStack stack) {
        PoseStack ps = event.getPoseStack();
        int s = side(arm);
        float pt = event.getPartialTick(), speed = Math.min(1.0F, player.walkAnimation.speed(pt));
        float phase = player.walkAnimation.position(pt) * 0.6662F, age = player.tickCount + pt;
        float lift = 0.03F * Mth.abs(Mth.sin(phase)) * speed - 0.008F * Mth.sin(age * 0.08F);
        ps.pushPose();
        ps.translate(0.0F, -lift, 0.0F);
        if (!avatar.isInvisible) playerArm(event, avatar, arm, event.getEquipProgress() + 0.1F, 0.0F);
        ps.translate(s * 0.36F, -0.4F - 0.6F * event.getEquipProgress(), -0.85F); // the handle round the forearm
        ps.rotateDegrees(Axis.ZP, s * 3.0F * Mth.sin(phase) * speed);
        ps.rotateDegrees(Axis.YP, s * -25.0F);
        ps.scale(0.6F, 0.6F, 0.6F);
        unturned(stack, player).submit(ps, event.getSubmitNodeCollector(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    /** The tongs at vanilla's place, a little in and jaws up, with the hot piece hanging upright from the jaws. */
    private static void grip(RenderHandEvent event, Player player, HumanoidArm arm, ItemStack hot, ItemStackRenderState tongs) {
        PoseStack ps = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        int s = side(arm), light = event.getPackedLight();
        PoseStack.Pose view = ps.last().copy(); // the hand space's own turn (GameRenderer starts it from the camera's)
        ps.pushPose();
        itemPos(ps, s, event.getEquipProgress());
        if (event.getSwingProgress() > 0.0F) swingArm(ps, s, event.getSwingProgress());
        ps.translate(s * -0.2F, -0.05F, 0.0F); // in and down, so the jaws and the piece are in view
        ps.rotateDegrees(Axis.ZP, s * 35.0F);
        ps.rotateDegrees(Axis.YP, s * 60.0F); // turned so the arms and jaws face us, not edge-on
        tongs.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, 0);
        // to the jaws: the tongs' display transform, then the model point
        ps.translate(s * TONGS_AT[0] / 16.0F, TONGS_AT[1] / 16.0F, TONGS_AT[2] / 16.0F);
        ps.rotate(new Quaternionf().rotationXYZ(TONGS_ROT[0] * Mth.DEG_TO_RAD, TONGS_ROT[1] * Mth.DEG_TO_RAD, TONGS_ROT[2] * Mth.DEG_TO_RAD));
        ps.scale(TONGS_SCALE, TONGS_SCALE, TONGS_SCALE);
        ps.translate(JAWS[0] / 16.0F - 0.5F, JAWS[1] / 16.0F - 0.5F, JAWS[2] / 16.0F - 0.5F);
        Vector3f at = ps.last().pose().getTranslation(new Vector3f());
        ps.last().set(view); // upright in view, whatever way the tongs point
        ps.last().pose().setTranslation(at);
        ps.rotateDegrees(Axis.YP, s * -30.0F);
        ps.translate(0.0F, -0.05F, 0.0F); // hanging from the jaws
        ps.scale(0.4F, 0.4F, 0.4F);
        ItemStackRenderState piece = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(piece, hot, ItemDisplayContext.GROUND, player);
        piece.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    private static AvatarRenderer<?> renderer(AvatarRenderState avatar) {
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(avatar);
    }

    private static void hand(RenderHandEvent event, AvatarRenderState avatar, HumanoidArm arm) {
        var texture = avatar.skin.body().texturePath();
        if (arm == HumanoidArm.RIGHT) renderer(avatar).renderRightHand(event.getPoseStack(), event.getSubmitNodeCollector(), event.getPackedLight(), texture, avatar.showRightSleeve);
        else renderer(avatar).renderLeftHand(event.getPoseStack(), event.getSubmitNodeCollector(), event.getPackedLight(), texture, avatar.showLeftSleeve);
    }

    /** Vanilla's two-handed map hand (FirstPersonHandsAndItemsRenderer#renderMapHand), shifted dx. */
    private static void mapHand(RenderHandEvent event, AvatarRenderState avatar, HumanoidArm arm, float dx) {
        PoseStack ps = event.getPoseStack();
        int s = side(arm);
        ps.pushPose();
        ps.translate(dx, 0.0F, 0.0F);
        ps.rotateDegrees(Axis.YP, 92.0F);
        ps.rotateDegrees(Axis.XP, 45.0F);
        ps.rotateDegrees(Axis.ZP, s * -41.0F);
        ps.translate(s * 0.3F, -1.1F, 0.45F);
        hand(event, avatar, arm);
        ps.popPose();
    }

    /** Vanilla's bare arm (FirstPersonHandsAndItemsRenderer#renderPlayerArm), punching with {@code attack}. */
    private static void playerArm(RenderHandEvent event, AvatarRenderState avatar, HumanoidArm arm, float equip, float attack) {
        PoseStack ps = event.getPoseStack();
        int s = side(arm);
        float sq = Mth.sqrt(attack);
        ps.pushPose();
        ps.translate(s * (-0.3F * Mth.sin(sq * Mth.PI) + 0.64000005F), 0.4F * Mth.sin(sq * Mth.TWO_PI) - 0.6F - 0.6F * equip,
                -0.4F * Mth.sin(attack * Mth.PI) - 0.71999997F);
        ps.rotateDegrees(Axis.YP, s * 45.0F);
        ps.rotateDegrees(Axis.YP, s * Mth.sin(sq * Mth.PI) * 70.0F);
        ps.rotateDegrees(Axis.ZP, s * Mth.sin(attack * attack * Mth.PI) * -20.0F);
        ps.translate(s * -1.0F, 3.6F, 3.5F);
        ps.rotateDegrees(Axis.ZP, s * 120.0F);
        ps.rotateDegrees(Axis.XP, 200.0F);
        ps.rotateDegrees(Axis.YP, s * -135.0F);
        ps.translate(s * 5.6F, 0.0F, 0.0F);
        hand(event, avatar, arm);
        ps.popPose();
    }

    private FirstPersonHolds() {}
}
