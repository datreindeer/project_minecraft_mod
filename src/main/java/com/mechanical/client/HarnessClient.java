package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.HarnessPayload;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The grab key (default H): grab or release the travois, cart, plough or wagon you look at ({@link HarnessPayload}), and a
 * small hint box beside the crosshair that lists what the keys do on it right now ({@link TravoisEntity#hints}), each with
 * the key as it is bound. Hidden with a screen open, F1, or while you ride along; from a horse's saddle it offers to attach.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class HarnessClient {
    public static final KeyMapping KEY = new KeyMapping("key.mechanical.harness", InputConstants.KEY_H, KeyMapping.Category.GAMEPLAY);
    static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "harness_hint");
    private static final int LINE = 14, PAD = 4, GAP = 5;

    private HarnessClient() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) ClientPacketDistributor.sendToServer(new HarnessPayload());
        }
    }

    private static Component key(String kind) {
        var o = Minecraft.getInstance().options;
        return switch (kind) {
            case "harness" -> KEY.getTranslatedKeyMessage();
            case "attack" -> o.keyAttack.getTranslatedKeyMessage();
            case "sneak_use" -> Component.empty().append(o.keyShift.getTranslatedKeyMessage()).append(" + ").append(o.keyUse.getTranslatedKeyMessage());
            default -> o.keyUse.getTranslatedKeyMessage();
        };
    }

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.gui.screen() != null || mc.gui.hud.isHidden()
                || player.isPassenger() && !(player.getVehicle() instanceof AbstractHorse) || player.isSpectator()) return;
        TravoisEntity target = TravoisEntity.grabTarget(player);
        if (target == null) return;
        List<TravoisEntity.Hint> hints;
        if (player.getVehicle() instanceof AbstractHorse horse) {
            // from the saddle only the grab key works; driving along, its own vehicle's hint shows only when looked at
            String action = target.saddleAction(horse);
            if (!action.equals("attach") && target != TravoisEntity.lookedAt(player)) return;
            hints = List.of(new TravoisEntity.Hint("harness", action));
        } else hints = target.hints(player);
        List<Component> keys = new ArrayList<>();
        int keyW = 0, textW = 0;
        for (TravoisEntity.Hint h : hints) {
            keys.add(key(h.key()));
            keyW = Math.max(keyW, mc.font.width(keys.getLast()) + 2 * PAD);
            textW = Math.max(textW, mc.font.width(Component.translatable("hint.mechanical." + h.action())));
        }
        int w = PAD + keyW + GAP + textW + PAD, h = PAD + hints.size() * LINE + PAD - 2;
        int x = g.guiWidth() / 2 + 16, y = g.guiHeight() / 2 - h / 2;
        g.fill(x, y, x + w, y + h, 0x88101010);
        g.fill(x, y, x + w, y + 1, 0x33FFFFFF);
        for (int i = 0; i < hints.size(); i++) {
            int ly = y + PAD + i * LINE;
            g.fill(x + PAD, ly, x + PAD + keyW, ly + LINE - 3, 0xFF2B2B2B);
            g.fill(x + PAD, ly, x + PAD + keyW, ly + 1, 0xFF6A6A6A);
            g.fill(x + PAD, ly + LINE - 4, x + PAD + keyW, ly + LINE - 3, 0xFF111111);
            g.centeredText(mc.font, keys.get(i), x + PAD + keyW / 2, ly + 2, 0xFFFFFFFF);
            g.text(mc.font, Component.translatable("hint.mechanical." + hints.get(i).action()), x + PAD + keyW + GAP, ly + 2, 0xFFD8D2C4, false);
        }
    }
}
