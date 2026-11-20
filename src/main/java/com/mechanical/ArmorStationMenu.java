package com.mechanical;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** The armor station: bronze plates riveted onto leather with a hammer, see {@link ArmorStationRecipes}. */
public class ArmorStationMenu extends RecipeStationMenu {
    public ArmorStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ArmorStationMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(Mechanical.ARMOR_STATION_MENU.get(), containerId, inventory, access, Mechanical.ARMOR_STATION, ArmorStationRecipes.ALL);
    }
}
