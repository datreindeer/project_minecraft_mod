package com.mechanical;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** The leatherworking bench: leather and fur pieces sewn with a needle, see {@link LeatherworkingRecipes}. */
public class LeatherworkingMenu extends RecipeStationMenu {
    public LeatherworkingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public LeatherworkingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(Mechanical.LEATHERWORKING_MENU.get(), containerId, inventory, access, Mechanical.LEATHERWORKING_BENCH, LeatherworkingRecipes.ALL);
    }
}
