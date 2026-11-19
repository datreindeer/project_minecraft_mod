package com.mechanical;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A station like the Tool Station: a list of recipes on the left (click one to pull its parts from the inventory), three
 * input slots and a result. Parts can need several items; a tool part (the bone needle) only loses durability.
 */
public class RecipeStationMenu extends AbstractContainerMenu {
    public static final int RESULT = 3;
    /** Button ids from this up mean "shift-clicked": fill as many sets of the recipe's parts as the inventory has. */
    public static final int FILL_ALL = 1000;
    private static final int INV_START = 4, INV_END = 40;

    private final ContainerLevelAccess access;
    private final Supplier<? extends Block> block;
    private final List<StationRecipe> recipes;
    private final Player player;
    private final ResultContainer result = new ResultContainer();
    private final SimpleContainer inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            RecipeStationMenu.this.slotsChanged(this);
        }
    };

    public RecipeStationMenu(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
            Supplier<? extends Block> block, List<StationRecipe> recipes) {
        super(type, containerId);
        this.access = access;
        this.block = block;
        this.recipes = recipes;
        this.player = inventory.player;
        addSlot(new Slot(inputs, 0, 80, 17));
        addSlot(new Slot(inputs, 1, 80, 35));
        addSlot(new Slot(inputs, 2, 80, 53));
        addSlot(new Slot(result, 0, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                carried.onCraftedBy(player, carried.getCount());
                current().ifPresent(RecipeStationMenu.this::consume);
                super.onTake(player, carried);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
    }

    public List<StationRecipe> recipes() {
        return recipes;
    }

    private Optional<StationRecipe> current() {
        return recipes.stream().filter(r -> r.isUnlocked(player) && r.matches(inputs.getItem(0), inputs.getItem(1), inputs.getItem(2))).findFirst();
    }

    private void consume(StationRecipe recipe) {
        for (int i = 0; i < 3; i++) {
            StationRecipe.Part part = recipe.parts().get(i);
            if (part.tool()) {
                ItemStack tool = inputs.getItem(i);
                if (player.level() instanceof ServerLevel level) tool.hurtAndBreak(1, level, player instanceof ServerPlayer sp ? sp : null, item -> {});
                inputs.setItem(i, tool);
            } else {
                inputs.removeItem(i, part.count());
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        result.setItem(0, current().map(StationRecipe::assemble).orElse(ItemStack.EMPTY));
        broadcastChanges();
    }

    /**
     * Clicking a recipe in the list: put back whatever is in the inputs, then pull the recipe's parts from the inventory;
     * shift-click (index + FILL_ALL) pulls as many sets as there are, up to a stack per slot. A tool part is always one.
     */
    @Override
    public boolean clickMenuButton(Player player, int index) {
        boolean all = index >= FILL_ALL;
        if (all) index -= FILL_ALL;
        if (index < 0 || index >= recipes.size()) return false;
        StationRecipe recipe = recipes.get(index);
        if (!recipe.isUnlocked(player)) return false;
        for (int i = 0; i < 3; i++) {
            ItemStack old = inputs.removeItemNoUpdate(i);
            if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
        }
        if (recipe.hasParts(player)) {
            int sets = all ? sets(player.getInventory(), recipe) : 1;
            for (int i = 0; i < 3; i++) {
                StationRecipe.Part part = recipe.parts().get(i);
                inputs.setItem(i, take(player.getInventory(), part.item().get(), part.tool() ? 1 : part.count() * sets));
            }
        }
        slotsChanged(inputs);
        return true;
    }

    /** How many sets of the parts the inventory holds, limited so each input slot stays within one stack. */
    private static int sets(Inventory inventory, StationRecipe recipe) {
        int sets = Integer.MAX_VALUE;
        for (StationRecipe.Part part : recipe.parts()) {
            if (part.tool()) continue;
            Item item = part.item().get();
            sets = Math.min(sets, Math.min(inventory.countItem(item), item.getDefaultMaxStackSize()) / part.count());
        }
        return Math.max(1, sets == Integer.MAX_VALUE ? 1 : sets);
    }

    private static ItemStack take(Inventory inventory, Item item, int count) {
        ItemStack out = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize() && out.getCount() < count; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item)) continue;
            ItemStack taken = inventory.removeItem(i, count - out.getCount());
            if (out.isEmpty()) out = taken;
            else out.grow(taken.getCount());
        }
        return out;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == RESULT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, RESULT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != result && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputs));
    }
}
