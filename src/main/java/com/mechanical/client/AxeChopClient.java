package com.mechanical.client;

import com.mechanical.FallenLogBlock;
import com.mechanical.FelledLogEntity;
import com.mechanical.Mechanical;
import com.mechanical.SawItem;
import com.mechanical.TreeChopping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Chopping a fallen tree (a Fallen Log, a felled log, a stump) with an axe: a two-handed overhead blow, the axe high over
 * the head and down onto the log (TreeChopping's heavy WHACK swing, {@link TreeChopping#CHOP_TICKS} long, which every
 * client is sent). Third person here (from HumanoidModelMixin), first person in {@link FirstPersonHolds}. And on a fallen
 * tree the left button does what the right one does with an axe or saw (chop, saw) instead of breaking it: while it is
 * held there, the use key is held for it.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class AxeChopClient {
    /** The use key is held down by us (the attack key is on a fallen tree). */
    private static boolean mirroring;

    /** Is this the chop's swing (its progress then is {@code swingAnimation})? */
    public static boolean chopping(HumanoidRenderState state) {
        var swing = state.currentSwing;
        return swing != null && swing.hand() == InteractionHand.MAIN_HAND && swing.animation().type() == SwingAnimationType.WHACK
                && swing.animation().duration() == TreeChopping.CHOP_TICKS && state.swingAnimation > 0;
    }

    /** How far up the axe is over a chop's progress {@code p}: it comes down onto the log first (0: struck), rises over the head (1) and waits there for the next. */
    public static float raise(float p) {
        if (p < 0.12F) return 1 - smooth(p / 0.12F);
        if (p < 0.7F) return smooth((p - 0.12F) / 0.58F);
        return 1;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** End of HumanoidModel#setupAnim: both arms up over the head with the axe, and down. */
    public static void poseArms(HumanoidModel<?> model, HumanoidRenderState state) {
        if (!chopping(state) || state.swimAmount > 0) return;
        float r = raise(state.swingAnimation);
        model.body.yRot = 0; // undo vanilla's swing twist and the arms it moved with it
        model.rightArm.x = -5;
        model.rightArm.z = 0;
        model.leftArm.x = 5;
        model.leftArm.z = 0;
        float x = Mth.lerp(r, -0.6F, -2.9F), in = Mth.lerp(r, 0.4F, 0.15F);
        model.rightArm.xRot = model.leftArm.xRot = x;
        model.rightArm.yRot = -in;
        model.leftArm.yRot = in;
        model.rightArm.zRot = model.leftArm.zRot = 0;
        model.body.xRot = 0.12F * (1 - r); // a little bend into the blow
    }

    /** A fallen tree under the crosshair. */
    private static boolean onFallenTree(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult e) return e.getEntity() instanceof FelledLogEntity;
        return hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK && mc.level.getBlockState(b.getBlockPos()).getBlock() instanceof FallenLogBlock;
    }

    private static boolean cutter(Player player) {
        ItemStack held = player.getMainHandItem();
        return held.is(ItemTags.AXES) || held.getItem() instanceof SawItem;
    }

    private static boolean leftOnTree(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.gui.screen() == null && mc.options.keyAttack.isDown() && cutter(mc.player) && onFallenTree(mc);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (leftOnTree(mc)) {
            mc.options.keyUse.setDown(true);
            mirroring = true;
        } else if (mirroring) {
            mirroring = false;
            mc.options.keyUse.setDown(false);
        }
    }

    /** The left button on a fallen tree with an axe or saw doesn't hit or break it (the use key does its work). */
    @SubscribeEvent
    static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && leftOnTree(Minecraft.getInstance())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private AxeChopClient() {}
}
