package com.mechanical.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Vanilla hides the hunger bar while riding something with hearts (its health takes the slot); this draws it anyway. */
final class RidingFoodHud {
    private RidingFoodHud() {}

    /** Runs right after the vanilla food layer, which drew nothing when the vehicle shows health. */
    static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        if (!(player.getVehicle() instanceof LivingEntity mount) || !mount.showVehicleHealth()) return;
        String hunger = player.hasEffect(MobEffects.HUNGER) ? "_hunger" : "";
        int food = player.getFoodData().getFoodLevel();
        int xRight = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight() - mc.gui.hud.rightHeight;
        for (int i = 0; i < 10; i++) {
            int x = xRight - i * 8 - 9;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/food_empty" + hunger), x, y, 9, 9);
            if (i * 2 + 1 < food) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/food_full" + hunger), x, y, 9, 9);
            else if (i * 2 + 1 == food) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/food_half" + hunger), x, y, 9, 9);
        }
        mc.gui.hud.rightHeight += 10;
    }
}
