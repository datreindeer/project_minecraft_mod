package com.mechanical.client.viewer;

import org.jspecify.annotations.Nullable;

import com.mechanical.Mechanical;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * The ways into the recipe viewer: R (recipes) and U (uses) over an item in any container screen or on the viewer's own
 * page, and the {@link SearchPanel} beside every container screen but the creative inventory.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class RecipeViewerClient {
    public static final KeyMapping RECIPE = new KeyMapping("key.mechanical.recipe", KeyConflictContext.GUI, InputConstants.Type.KEYBOARD, InputConstants.KEY_R,
            KeyMapping.Category.INVENTORY);
    public static final KeyMapping USAGE = new KeyMapping("key.mechanical.usage", KeyConflictContext.GUI, InputConstants.Type.KEYBOARD, InputConstants.KEY_U,
            KeyMapping.Category.INVENTORY);

    private static @Nullable SearchPanel panel;
    /** The player inventory's 2x2 crafting result, which InventoryMenu.removed clears on the client when the viewer opens over it. */
    private static @Nullable Screen leftFrom;
    private static ItemStack leftResult = ItemStack.EMPTY;

    private RecipeViewerClient() {}

    /** Opens the viewer over {@code from}, keeping what its menu would lose. */
    static void open(Screen from, Item item, boolean usage) {
        if (from instanceof AbstractContainerScreen<?> s && s.getMenu() instanceof InventoryMenu menu) {
            leftFrom = s;
            leftResult = menu.getSlot(InventoryMenu.RESULT_SLOT).getItem().copy();
        }
        RecipeViewScreen.open(item, usage);
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RECIPE);
        event.register(USAGE);
    }

    private static @Nullable SearchPanel panel(Screen screen) {
        return panel != null && panel.screen == screen ? panel : null;
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> s) || s instanceof CreativeModeInventoryScreen) return;
        panel = new SearchPanel(s, event::addListener);
        if (s == leftFrom && s.getMenu() instanceof InventoryMenu menu) { // back from the viewer
            menu.getSlot(InventoryMenu.RESULT_SLOT).set(leftResult);
            leftFrom = null;
        }
    }

    /** Drawn with the screen's background, so the box and buttons (widgets) come on top. */
    @SubscribeEvent
    static void onBackground(ScreenEvent.Render.Background event) {
        SearchPanel p = panel(event.getScreen());
        if (p != null) p.draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    /** Beside the player inventory the effects shrink to their icons, in the column the panel leaves them. */
    @SubscribeEvent
    static void onEffects(ScreenEvent.RenderInventoryMobEffects event) {
        SearchPanel p = panel(event.getScreen());
        if (p != null && p.shown()) event.setCompact(true);
    }

    @SubscribeEvent
    static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        SearchPanel p = panel(event.getScreen());
        if (p != null && p.click(event.getMouseX(), event.getMouseY(), event.getButton())) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        SearchPanel p = panel(event.getScreen());
        if (p != null && p.release()) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        SearchPanel p = panel(event.getScreen());
        if (p != null && p.scroll(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) event.setCanceled(true);
    }

    /** Typing in the search box: the container must not see the keys too (E would close it, Q drop, 1-9 swap). */
    @SubscribeEvent
    static void onKeyPre(ScreenEvent.KeyPressed.Pre event) {
        SearchPanel p = panel(event.getScreen());
        if (p != null && p.box.canConsumeInput() && !event.getKeyEvent().isEscape()) {
            p.box.keyPressed(event.getKeyEvent());
            event.setCanceled(true);
        }
    }

    /** R and U after the screen passed on the key, so a focused text box (the recipe book's, an anvil's) keeps its letters. */
    @SubscribeEvent
    static void onKeyPost(ScreenEvent.KeyPressed.Post event) {
        InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
        boolean usage = USAGE.isActiveAndMatches(key);
        if (!usage && !RECIPE.isActiveAndMatches(key) || event.getScreen().getFocused() instanceof EditBox) return;
        Item item = hovered(event.getScreen());
        if (item == null) return;
        open(event.getScreen(), item, usage);
        event.setCanceled(true);
    }

    private static @Nullable Item hovered(Screen screen) {
        if (screen instanceof RecipeViewScreen viewer) return viewer.hoveredItem();
        if (!(screen instanceof AbstractContainerScreen<?> s)) return null;
        Slot slot = s.getHoveredSlot();
        if (slot != null && slot.hasItem()) return slot.getItem().getItem();
        SearchPanel p = panel(s);
        return p == null ? null : p.hovered();
    }
}
