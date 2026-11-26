package com.mechanical;

import com.mechanical.skills.Skill;
import com.mechanical.skills.Skills;
import com.mechanical.tools.ToolParts;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Slots 0-2: head, binding, rod. Slot 3: result. Then the player inventory. */
public class ToolStationMenu extends AbstractContainerMenu {
    public static final int HEAD = 0, BINDING = 1, ROD = 2, RESULT = 3;
    private static final int INV_START = 4, INV_END = 40;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ToolStationMenu.this.slotsChanged(this);
        }
    };
    private final DataSlot selected = DataSlot.standalone();
    private final ResultContainer result = new ResultContainer();

    public ToolStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ToolStationMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(Mechanical.TOOL_STATION_MENU.get(), containerId);
        this.access = access;
        this.player = inventory.player;
        addSlot(new Slot(inputs, HEAD, 80, 17));
        addSlot(new Slot(inputs, BINDING, 80, 35));
        addSlot(new Slot(inputs, ROD, 80, 53));
        addSlot(new Slot(result, 0, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                carried.onCraftedBy(player, carried.getCount());
                if (player instanceof ServerPlayer sp) Skills.addXp(sp, Skill.SMITHING, 10);
                for (int i = 0; i < 3; i++) inputs.removeItem(i, 1);
                super.onTake(player, carried);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        selected.set(-1);
        addDataSlot(selected);
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack head = inputs.getItem(HEAD), binding = inputs.getItem(BINDING), rod = inputs.getItem(ROD);
        int chosen = selected.get();
        boolean constrained = chosen >= 0 && head.is(ToolStationRecipes.ALL.get(chosen).head().get()); // another head ignores the selection
        // a clicked recipe is the only one that may produce; otherwise the recipe whose own binding and rod fit best (ties: first in ALL)
        ItemStack out = ToolStationRecipes.ALL.stream()
                .filter(r -> r.matches(head, binding, rod) && ToolStationRecipes.isUnlocked(player, r)
                        && (!constrained || ToolStationRecipes.ALL.get(chosen) == r))
                .max(java.util.Comparator.comparingInt(r -> r.score(binding, rod)))
                .map(r -> Bronze.inheritPoor(build(r, head, binding, rod), inputs))
                .orElse(ItemStack.EMPTY);
        result.setItem(0, out);
        broadcastChanges();
    }

    /** Dynamic recipes write the chosen parts and this player's Smithing level onto the tool. */
    private ItemStack build(ToolStationRecipes.Recipe r, ItemStack head, ItemStack binding, ItemStack rod) {
        ItemStack tool = new ItemStack(r.result().get());
        if (r.dynamic()) ToolParts.apply(tool, r.binding().get(), r.rod().get(),
                new ToolParts.Parts(binding.getItem(), rod.getItem(), Skills.level(player, Skill.SMITHING)));
        return tool;
    }

    /** Clicking a tool in the list: put back whatever is in the inputs, then pull the recipe's parts (shift: as many sets as there are). */
    @Override
    public boolean clickMenuButton(Player player, int index) {
        boolean all = index >= RecipeStationMenu.FILL_ALL;
        if (all) index -= RecipeStationMenu.FILL_ALL;
        if (index < 0 || index >= ToolStationRecipes.ALL.size()) return false;
        ToolStationRecipes.Recipe recipe = ToolStationRecipes.ALL.get(index);
        if (!ToolStationRecipes.isUnlocked(player, recipe)) return false;
        selected.set(index);
        for (int i = 0; i < 3; i++) {
            ItemStack old = inputs.removeItemNoUpdate(i);
            if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old, Prediction.SERVER_ONLY);
        }
        if (!ToolStationRecipes.hasParts(player, recipe)) {
            slotsChanged(inputs);
            return true;
        }
        List<Item> parts = recipe.parts(player.getInventory());
        int sets = 1;
        if (all) {
            sets = Integer.MAX_VALUE;
            for (Item part : parts) sets = Math.min(sets, Math.min(player.getInventory().countItem(part), part.getDefaultMaxStackSize()));
            sets = Math.max(1, sets);
        }
        for (int i = 0; i < 3; i++) inputs.setItem(i, take(player.getInventory(), parts.get(i), sets));
        return true;
    }

    private static ItemStack take(Inventory inventory, Item item, int count) {
        ItemStack out = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize() && out.getCount() < count; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item) || !out.isEmpty() && !ItemStack.isSameItemSameComponents(out, stack)) continue; // don't merge Poor Bronze into good
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
        } else if (!moveItemStackTo(stack, HEAD, RESULT, false)) {
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
        return stillValid(access, player, Mechanical.TOOL_STATION.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, inputs));
    }
}
