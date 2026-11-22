package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.BronzeAnvilBlockEntity;
import com.mechanical.Ironworking;
import com.mechanical.Mechanical;
import com.mechanical.Smithing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Bronze Anvil's pattern wheel: sneak-use the anvil with a smithing hammer (and hold) and every part the metal on it
 * can become lies in a ring round the crosshair; point at one and let go (or click) to pick it, let go in the middle to keep
 * the current one (outlined green). The pick goes to the server as {@link Smithing.ChoosePattern}, which checks it.
 * Drawn in the tribe page's bark colours (client.clan.ClanScreen).
 */
public class AnvilWheel extends Screen {
    static final int RADIUS = 80, SLOT = 22, DEAD = 18;
    static final int PANEL = 0xE0302418, BORDER = 0xFF8A6A44, TITLE = 0xFFF0D8A8, TEXT = 0xFFE8E0D0, DIM = 0xFF9A8A70, CURRENT = 0xFFB8E08A,
            HOVER_FILL = 0xF05A4228;

    private final BlockPos pos;
    private final Smithing.Pattern current;
    private final List<Smithing.Pattern> choices = new ArrayList<>();
    private final List<ItemStack> icons = new ArrayList<>();
    private int hover = -1;

    AnvilWheel(BlockPos pos, BronzeAnvilBlockEntity anvil) {
        super(Component.translatable("gui.mechanical.anvil_wheel"));
        this.pos = pos;
        current = anvil.pattern();
        ItemStack metal = anvil.item();
        boolean steel = metal.is(Smithing.HOT_STEEL_INGOT.get()) || metal.is(Smithing.STEEL_INGOT.get());
        // an empty anvil (or a cold ingot) shows the iron parts
        ItemStack sample = new ItemStack(steel ? Smithing.HOT_STEEL_INGOT.get() : Smithing.HOT_WROUGHT_IRON_INGOT.get());
        for (Smithing.Pattern p : Smithing.Pattern.values()) {
            if (!p.allowedFor(metal)) continue;
            choices.add(p);
            icons.add(p.result(sample));
        }
    }

    /** Where slot {@code i} of {@code n} sits: the first at the top, clockwise. */
    static double angle(int i, int n) {
        return -Math.PI / 2 + i * 2 * Math.PI / n;
    }

    /** The slot the mouse points at, or -1 in the middle. */
    static int pointed(double dx, double dy, int n) {
        if (dx * dx + dy * dy < DEAD * DEAD || n == 0) return -1;
        double a = Math.atan2(dy, dx) + Math.PI / 2;
        return Math.floorMod((int) Math.round(a / (2 * Math.PI / n)), n);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // the world stays in view: no blur, no dimming
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int cx = width / 2, cy = height / 2, n = choices.size();
        hover = pointed(mouseX - cx, mouseY - cy, n);
        for (int i = 0; i < n; i++) {
            int x = cx + (int) Math.round(Math.cos(angle(i, n)) * RADIUS), y = cy + (int) Math.round(Math.sin(angle(i, n)) * RADIUS);
            int h = SLOT / 2;
            boolean cur = choices.get(i) == current;
            graphics.fill(x - h - 1, y - h - 1, x + h + 1, y + h + 1, i == hover ? TITLE : cur ? CURRENT : BORDER);
            graphics.fill(x - h, y - h, x + h, y + h, i == hover ? HOVER_FILL : PANEL);
            graphics.item(icons.get(i), x - 8, y - 8);
        }
        // the middle: what the pointed (or current) pattern is and takes
        Smithing.Pattern shown = hover >= 0 ? choices.get(hover) : current;
        Component name = Component.translatable("pattern.mechanical." + shown.id);
        Component cost = Component.translatable("gui.mechanical.anvil_wheel.cost", shown.ingots, shown.hits);
        int w = Math.max(font.width(name), font.width(cost)) + 12;
        graphics.fill(cx - w / 2 - 1, cy - 13, cx + w / 2 + 1, cy + 13, BORDER);
        graphics.fill(cx - w / 2, cy - 12, cx + w / 2, cy + 12, PANEL);
        graphics.centeredText(font, name, cx, cy - 9, hover >= 0 ? TITLE : CURRENT);
        graphics.centeredText(font, cost, cx, cy + 2, DIM);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void pick() {
        if (hover >= 0 && choices.get(hover) != current)
            ClientPacketDistributor.sendToServer(new Smithing.ChoosePattern(pos, choices.get(hover).ordinal()));
        onClose();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (minecraft.options.keyUse.matchesMouse(event)) pick();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        pick(); // a quick click opened it without a hold: a second click picks
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (minecraft.options.keyUse.matches(event)) pick();
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Opener {
        /** Sneak-use on a Bronze Anvil with a smithing hammer opens the wheel instead of using the anvil. */
        @SubscribeEvent
        static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc = Minecraft.getInstance();
            if (!event.isUseItem() || mc.player == null || mc.level == null || !mc.player.isSecondaryUseActive()
                    || !Smithing.isHammer(mc.player.getItemInHand(event.getHand())) || !(mc.hitResult instanceof BlockHitResult hit)
                    || !mc.level.getBlockState(hit.getBlockPos()).is(Ironworking.BRONZE_ANVIL.get())
                    || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof BronzeAnvilBlockEntity anvil)) return;
            event.setCanceled(true);
            event.setSwingHand(false);
            mc.gui.setScreen(new AnvilWheel(hit.getBlockPos(), anvil));
        }
    }
}
