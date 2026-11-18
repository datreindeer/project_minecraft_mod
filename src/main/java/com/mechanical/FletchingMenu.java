package com.mechanical;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Blocks;

/** The fletching table: arrows and bow upgrades, see {@link FletchingRecipes}. */
public class FletchingMenu extends RecipeStationMenu {
    public FletchingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public FletchingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(Mechanical.FLETCHING_MENU.get(), containerId, inventory, access, () -> Blocks.FLETCHING_TABLE, FletchingRecipes.ALL);
    }
}
