package com.mechanical.client;

import java.util.HashSet;
import java.util.Set;

import com.mechanical.Mechanical;
import com.mechanical.clan.Downed;

import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * The client side of {@link Downed}: the R key (give up / revive), the downed screen (a red vignette and haze that
 * thicken as the minute runs out, a heartbeat that slows, the lines "a tribe mate can help you up" and "hold R to give
 * up"), the wake-up (third person, eyelids opening in blinks, then first person), and how downed players look: lying
 * sprawled (forced SLEEPING pose, {@link #poseLimbs}), across a carrier's shoulders, or in the bed they woke in. A mate
 * helping someone up reaches down to him. Downed or waking: no hotbar, no hands, no inventory, no walking.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class DownedClient {
    /** In the world only, so it shares R with the recipe viewer's key (screens only) without a conflict. */
    public static final KeyMapping KEY = new KeyMapping("key.mechanical.downed", net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD, com.mojang.blaze3d.platform.InputConstants.KEY_R, KeyMapping.Category.MULTIPLAYER);
    static final Identifier HUD = Identifier.fromNamespaceAndPath(Mechanical.MODID, "downed");
    private static final Identifier VIGNETTE = Identifier.withDefaultNamespace("textures/misc/vignette.png");
    /** Present on a downed or waking player: true when carried on someone's shoulders. */
    private static final ContextKey<Boolean> LYING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "lying"));
    /** Present on a player helping someone up: reaching down to him. */
    private static final ContextKey<Boolean> REACHING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "reaching"));
    /** How far below the carrier's head top a carried player lies (on the shoulders). */
    private static final float CARRY_DROP = 0.55F;
    private static final Set<Identifier> HIDDEN = Set.of(VanillaGuiLayers.HOTBAR, VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.CROSSHAIR,
            VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR, VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
            VanillaGuiLayers.VEHICLE_HEALTH);
    /** Players whose pose this forced (to let go of when they get up). */
    private static final Set<Integer> FORCED = new HashSet<>();
    private static boolean held, waking;
    /** Ticks R has been held (shown as the give-up bar). */
    private static int heldTicks;

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(Downed.Sync.TYPE, (s, ctx) -> {
            if (s.kind() == Downed.NONE) Downed.CLIENT.remove(s.entity());
            else Downed.CLIENT.put(s.entity(), new int[] {s.kind(), s.ticks(), s.helper()});
        });
    }

    @SubscribeEvent
    static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, HUD, DownedClient::render);
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Downed.CLIENT.clear();
        FORCED.clear();
        waking = false;
    }

    static int[] state(Player p) {
        return Downed.CLIENT.get(p.getId());
    }

    /** Before the keys are handled: a downed or waking player can't open his inventory, drop, swap or pick. */
    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Downed.locked(mc.player)) return;
        var o = mc.options;
        for (KeyMapping k : new KeyMapping[] {o.keyInventory, o.keyDrop, o.keySwapOffhand, o.keyPickItem}) while (k.consumeClick()) {}
        for (KeyMapping k : o.keyHotbarSlots) while (k.consumeClick()) {}
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        for (int[] s : Downed.CLIENT.values()) {
            if (s[0] == Downed.DOWN) s[1]++;
            else if (s[1] > 0) s[1]--;
        }
        for (Player p : mc.level.players()) {
            int[] s = state(p);
            if (s != null) {
                p.setForcedPose(s[0] == Downed.WAKING && s[1] <= Downed.STAND_TICKS ? Pose.CROUCHING : Pose.SLEEPING);
                FORCED.add(p.getId());
            } else if (FORCED.remove(p.getId())) p.setForcedPose(null);
        }
        // the R key: held or let go
        boolean down = KEY.isDown() && mc.gui.screen() == null;
        while (KEY.consumeClick()) {}
        heldTicks = down ? heldTicks + 1 : 0;
        if (down != held) {
            held = down;
            ClientPacketDistributor.sendToServer(new Downed.Key(down ? Downed.HOLD : Downed.RELEASE));
        }
        // waking: third person while lying, first person once up
        boolean wake = Downed.kind(mc.player) == Downed.WAKING;
        if (wake && !waking) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (!wake && waking) mc.options.setCameraType(CameraType.FIRST_PERSON);
        waking = wake;
    }

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (Downed.locked(event.getEntity())) event.getInput().keyPresses = Input.EMPTY;
    }

    /** No hitting or using while down or waking; with a mate on your shoulders, sneak + use in the air puts him down. */
    @SubscribeEvent
    static void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;
        boolean carrying = Downed.carrying(p) != null;
        if (!Downed.locked(p) && !carrying) return;
        boolean onBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
        if (carrying && event.isUseItem() && onBlock) return; // sneaking, the server puts him down on that block (Downed#onRightClickBlock)
        event.setCanceled(true);
        event.setSwingHand(false);
        if (carrying && event.isUseItem() && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && p.isShiftKeyDown())
            ClientPacketDistributor.sendToServer(new Downed.Key(Downed.PUT_DOWN));
    }

    @SubscribeEvent
    static void onScreen(ScreenEvent.Opening event) {
        Player p = Minecraft.getInstance().player;
        if (p != null && Downed.locked(p) && event.getNewScreen() instanceof AbstractContainerScreen<?>) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onLayer(RenderGuiLayerEvent.Pre event) {
        Player p = Minecraft.getInstance().player;
        if (p != null && Downed.locked(p) && HIDDEN.contains(event.getName())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        Player p = Minecraft.getInstance().player;
        if (p != null && Downed.locked(p)) event.setCanceled(true);
    }

    // ---------------------------------------------------------------- how they look

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (!(avatar instanceof Player p)) return;
                int[] s = state(p);
                if (s != null) {
                    boolean carried = p.getVehicle() instanceof Player;
                    state.setRenderData(LYING, carried);
                    state.rightHandItemState.clear();
                    state.leftHandItemState.clear();
                    state.rightArmPose = state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
                    if (carried && p.getVehicle() instanceof Player carrier) {
                        // across the carrier's shoulders
                        state.isPassenger = false;
                        state.bodyRot = Mth.rotLerp(state.partialTick, carrier.yBodyRotO, carrier.yBodyRot) + 90.0F;
                        state.y -= CARRY_DROP;
                    } else if (s[0] == Downed.WAKING) {
                        Direction bed = AbstractBedBlock.getBedOrientation(p.level(), p.blockPosition());
                        if (bed != null) {
                            // as vanilla does for a sleeper (LivingEntityRenderer): the head on the pillow, the body along the bed
                            state.bedOrientation = bed;
                            state.eyeHeight = p.getEyeHeight(Pose.STANDING);
                        }
                    }
                }
                for (int[] o : Downed.CLIENT.values()) if (o[0] == Downed.DOWN && o[2] == p.getId()) state.setRenderData(REACHING, true);
            }
        });
    }

    /** End of HumanoidModel#setupAnim: sprawled on the ground (limp over a carrier's shoulders), or reaching down to help. */
    public static void poseLimbs(HumanoidModel<?> model, HumanoidRenderState state) {
        Boolean carried = state.getRenderData(LYING);
        if (carried != null) {
            if (carried) {
                // limp: arms and legs hang down off the shoulders
                model.rightArm.xRot = model.leftArm.xRot = -1.4F;
                model.rightArm.zRot = 0.1F;
                model.leftArm.zRot = -0.1F;
                model.rightLeg.xRot = model.leftLeg.xRot = 1.3F;
            } else {
                model.rightArm.xRot = -0.15F;
                model.rightArm.zRot = 1.0F;
                model.leftArm.xRot = 0.1F;
                model.leftArm.zRot = -0.55F;
                model.rightLeg.xRot = -0.3F;
                model.rightLeg.zRot = 0.18F;
                model.leftLeg.xRot = 0.05F;
                model.leftLeg.zRot = -0.12F;
            }
            model.rightArm.yRot = model.leftArm.yRot = 0;
            model.rightLeg.yRot = model.leftLeg.yRot = 0;
            return;
        }
        if (state.getRenderData(REACHING) != null) {
            float tug = Mth.sin(state.ageInTicks * 0.3F) * 0.08F;
            model.rightArm.xRot = model.leftArm.xRot = -1.1F + tug;
            model.rightArm.yRot = -0.25F;
            model.leftArm.yRot = 0.25F;
            model.rightArm.zRot = model.leftArm.zRot = 0;
        }
    }

    // ---------------------------------------------------------------- the screen

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        int[] s = p == null ? null : state(p);
        if (s == null) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float age = p.tickCount + delta.getGameTimeDeltaPartialTick(false);
        if (s[0] == Downed.DOWN) downedScreen(mc, g, p, s, w, h, age);
        else eyelids(g, 1.0F - (s[1] - delta.getGameTimeDeltaPartialTick(false)) / Downed.WAKE_TICKS, w, h);
    }

    private static void downedScreen(Minecraft mc, GuiGraphicsExtractor g, Player p, int[] s, int w, int h, float age) {
        float t = Mth.clamp(s[1] / (float) Downed.DOWN_TICKS, 0, 1);
        // a heartbeat that slows as the minute runs out: a quick double thump, then rest
        float period = 16 + 30 * t, phase = (age % period) / period;
        float beat = Math.max(0, 1 - Math.abs(phase - 0.08F) * 14) + 0.6F * Math.max(0, 1 - Math.abs(phase - 0.24F) * 14);
        // dark red edges closing in: the vignette blend darkens green and blue far more than red
        float edge = 0.45F + 0.55F * t + 0.1F * beat;
        int vig = ARGB.colorFromFloat(1.0F, 0.15F * edge, Math.min(1, 0.95F * edge), Math.min(1, 0.95F * edge));
        for (int i = 0; i < 2 + (int) (t * 3); i++) g.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, w, h, w, h, vig);
        g.fill(0, 0, w, h, ARGB.colorFromFloat(0.12F + 0.4F * t + 0.06F * beat, 0.35F, 0.0F, 0.0F)); // blood haze
        g.fill(0, 0, w, h, ARGB.colorFromFloat(0.45F * t, 0.0F, 0.0F, 0.0F)); // and darker
        var font = mc.font;
        int y = h / 2 + 18;
        g.centeredText(font, Component.translatable("gui.mechanical.downed.title"), w / 2, h / 2 - 30, 0xFFE04040);
        Component status;
        if (p.getVehicle() instanceof Player carrier) status = Component.translatable("gui.mechanical.downed.carried", carrier.getDisplayName());
        else if (s[2] >= 0 && mc.level.getEntity(s[2]) instanceof Player helper) status = Component.translatable("gui.mechanical.downed.helped", helper.getDisplayName());
        else status = Component.translatable("gui.mechanical.downed.help");
        g.centeredText(font, status, w / 2, y, 0xFFF0E0D0);
        g.centeredText(font, Component.translatable("gui.mechanical.downed.give_up", KEY.getTranslatedKeyMessage()), w / 2, y + 14, 0xFFC0A8A0);
        if (!(p.getVehicle() instanceof Player)) {
            int left = Math.max(0, (Downed.DOWN_TICKS - s[1] + 19) / 20);
            g.centeredText(font, Component.translatable("gui.mechanical.downed.time", left), w / 2, y + 28, 0xFF907870);
        }
        if (heldTicks > 0) {
            int bar = 80, filled = Math.min(bar, heldTicks * bar / Downed.GIVE_UP_TICKS);
            g.fill(w / 2 - bar / 2, y + 42, w / 2 + bar / 2, y + 45, 0x80000000);
            g.fill(w / 2 - bar / 2, y + 42, w / 2 - bar / 2 + filled, y + 45, 0xFFB02020);
        }
    }

    /** How open the eyes are at {@code t} (0..1 of the wake): a slit, shut, half open, a blink, then open. */
    static float open(float t) {
        float[][] keys = {{0, 0}, {0.15F, 0}, {0.3F, 0.3F}, {0.38F, 0.02F}, {0.55F, 0.55F}, {0.62F, 0.25F}, {0.8F, 1}, {1, 1}};
        for (int i = 1; i < keys.length; i++)
            if (t <= keys[i][0]) return Mth.lerp((t - keys[i - 1][0]) / (keys[i][0] - keys[i - 1][0]), keys[i - 1][1], keys[i][1]);
        return 1;
    }

    private static void eyelids(GuiGraphicsExtractor g, float t, int w, int h) {
        float open = open(Mth.clamp(t, 0, 1));
        if (open >= 1) return;
        g.fill(0, 0, w, h, ARGB.colorFromFloat(0.6F * (1 - open), 0, 0, 0)); // still drowsy and dim
        int lid = (int) ((1 - open) * (h / 2 + 4));
        // the lids leave an eye-shaped opening: open widest in the middle
        int steps = 32;
        for (int i = 0; i < steps; i++) {
            int x0 = w * i / steps, x1 = w * (i + 1) / steps;
            float c = (i + 0.5F) / steps * 2 - 1;
            int d = lid + (int) (c * c * Math.min(lid, h / 6.0F));
            g.fill(x0, 0, x1, d, 0xFF000000);
            g.fill(x0, h - d, x1, h, 0xFF000000);
        }
    }

    private DownedClient() {}
}
