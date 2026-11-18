package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.RecipeStationMenu;
import com.mechanical.StationRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Screen for a {@link RecipeStationMenu}, laid out like the Tool Station: on the left a grid of the unlocked recipes (lit
 * when you carry every part, hover for the parts); click one to load its parts into the inputs.
 */
public class RecipeStationScreen<M extends RecipeStationMenu> extends AbstractContainerScreen<M> {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier BUTTON_HOVER = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier BUTTON_LIT = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final int LIST_X = 8, LIST_Y = 17, COLUMNS = 3, ROWS = 3;
    private static final int SCROLL_X = 59, SCROLL_Y = 17, SCROLL_H = 54, SCROLLER_H = 15;

    private final Identifier background;
    private int startRow;
    private boolean scrolling;

    public RecipeStationScreen(M menu, Inventory inventory, Component title, Identifier background) {
        super(menu, inventory, title);
        this.background = background;
    }

    /** Indices into the menu's recipes that this player has unlocked. */
    private List<Integer> unlocked() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < menu.recipes().size(); i++) if (menu.recipes().get(i).isUnlocked(minecraft.player)) out.add(i);
        return out;
    }

    private int maxStartRow() {
        return Math.max(0, (unlocked().size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    private List<Integer> visible() {
        List<Integer> all = unlocked();
        startRow = Math.min(startRow, maxStartRow());
        int from = Math.min(all.size(), startRow * COLUMNS);
        return all.subList(from, Math.min(all.size(), from + COLUMNS * ROWS));
    }

    private void scrollTo(double mouseY) {
        double t = (mouseY - (topPos + SCROLL_Y) - SCROLLER_H / 2.0) / (SCROLL_H - SCROLLER_H);
        startRow = (int) Math.round(Math.clamp(t, 0.0, 1.0) * maxStartRow());
    }

    private int buttonAt(double mouseX, double mouseY, int count) {
        for (int pos = 0; pos < count; pos++) {
            int x = leftPos + LIST_X + pos % COLUMNS * 16, y = topPos + LIST_Y + pos / COLUMNS * 18;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 18) return pos;
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        int max = maxStartRow();
        int scrollerY = topPos + SCROLL_Y + (max == 0 ? 0 : (SCROLL_H - SCROLLER_H) * startRow / max);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, max > 0 ? SCROLLER : SCROLLER_DISABLED, leftPos + SCROLL_X, scrollerY, 12, SCROLLER_H);
        List<Integer> visible = visible();
        int hovered = buttonAt(mouseX, mouseY, visible.size());
        for (int pos = 0; pos < visible.size(); pos++) {
            StationRecipe recipe = menu.recipes().get(visible.get(pos));
            int x = leftPos + LIST_X + pos % COLUMNS * 16, y = topPos + LIST_Y + pos / COLUMNS * 18;
            boolean lit = recipe.hasParts(minecraft.player);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, lit ? BUTTON_LIT : pos == hovered ? BUTTON_HOVER : BUTTON, x, y, 16, 18);
            graphics.item(recipe.assemble(), x, y + 1);
            if (!lit) graphics.fill(x, y, x + 16, y + 18, 0x80000000);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        List<Integer> visible = visible();
        int pos = buttonAt(mouseX, mouseY, visible.size());
        if (pos < 0) return;
        StationRecipe recipe = menu.recipes().get(visible.get(pos));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(recipe.count() > 1 ? recipe.count() + "× " : "").append(recipe.assemble().getHoverName()));
        for (StationRecipe.Part part : recipe.parts()) {
            boolean has = minecraft.player.getInventory().countItem(part.item().get()) >= part.count();
            lines.add(Component.literal(part.tool() ? "" : part.count() + "× ").append(new ItemStack(part.item().get()).getHoverName())
                    .append(part.tool() ? Component.literal(" (tool, loses durability)") : Component.empty())
                    .withStyle(has ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        List<Integer> visible = visible();
        int pos = buttonAt(event.x(), event.y(), visible.size());
        if (pos >= 0 && menu.recipes().get(visible.get(pos)).hasParts(minecraft.player)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, visible.get(pos));
            return true;
        }
        if (maxStartRow() > 0 && event.x() >= leftPos + SCROLL_X && event.x() < leftPos + SCROLL_X + 12
                && event.y() >= topPos + SCROLL_Y && event.y() < topPos + SCROLL_Y + SCROLL_H) {
            scrolling = true;
            scrollTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (scrolling) {
            scrollTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) return true;
        startRow = Math.clamp(startRow - (int) Math.signum(scrollY), 0, maxStartRow());
        return true;
    }
}
