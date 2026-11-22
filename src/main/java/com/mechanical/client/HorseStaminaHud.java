package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.HorseWork;
import com.mechanical.Mechanical;
import com.mechanical.TravoisEntity;
import com.mechanical.WagonEntity;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Stamina bars (HorseWork) above the armour row, left of the hotbar: the horse you ride, or the team of the wagon you
 * drive or sit on (lead horse first). Each bar has a horse heart in front; green, yellow when tired, red near the end,
 * blinking dark red while the horse lies exhausted.
 */
final class HorseStaminaHud {
    static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "horse_stamina");
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/vehicle_full");
    private static final int WIDTH = 71, HEIGHT = 5;

    private HorseStaminaHud() {}

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        List<AbstractHorse> horses = horses(player);
        if (horses.isEmpty()) return;
        int x = graphics.guiWidth() / 2 - 91;
        for (AbstractHorse horse : horses) {
            int y = graphics.guiHeight() - mc.gui.hud.leftHeight;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, x, y - 2, 9, 9);
            bar(graphics, x + 10, y, HorseWork.stamina(horse) / HorseWork.MAX, HorseWork.exhausted(horse), player.tickCount);
            mc.gui.hud.leftHeight += 10;
        }
    }

    /** The ridden horse, plus the wagon team it is in; or the team of the wagon you sit on. */
    static List<AbstractHorse> horses(Player player) {
        List<AbstractHorse> out = new ArrayList<>();
        WagonEntity wagon = null;
        if (player.getVehicle() instanceof AbstractHorse ridden) {
            out.add(ridden);
            if (TravoisEntity.pulling(ridden) instanceof WagonEntity w) wagon = w;
        } else if (player.getVehicle() instanceof WagonEntity w) wagon = w;
        if (wagon != null) {
            if (wagon.harness() instanceof AbstractHorse lead && !out.contains(lead)) out.add(lead);
            if (wagon.partner() != null && !out.contains(wagon.partner())) out.add(wagon.partner());
        }
        return out;
    }

    /** A vanilla-like bar: dark frame, coloured fill with a lighter top row. */
    private static void bar(GuiGraphicsExtractor graphics, int x, int y, float share, boolean exhausted, int ticks) {
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF000000);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF3A2A1A);
        int fill = Math.round(WIDTH * Math.clamp(share, 0, 1));
        int colour = exhausted ? (ticks / 10 % 2 == 0 ? 0xFF8B1A1A : 0xFF5A1010)
                : share * HorseWork.MAX < HorseWork.TIRED ? 0xFFD83A2A : share < 0.5F ? 0xFFE8C030 : 0xFF6CC24A;
        if (fill <= 0) return;
        graphics.fill(x, y, x + fill, y + HEIGHT, colour);
        graphics.fill(x, y, x + fill, y + 1, (colour & 0xFF000000) | ((colour & 0xFEFEFE) >> 1) + 0x7F7F7F); // a lighter top row
    }
}
