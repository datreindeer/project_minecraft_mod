package com.mechanical.equipment;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The accessory slots added to the player inventory (see {@link Equipment}), in container order. Each takes the items of
 * its tag {@code mechanical:equipment/<tag>} (written by scripts/gen_equipment.py). {@code x, y} place the slot in the
 * survival inventory (a column right of the player model, a row under it), {@code creativeX, creativeY} in the creative
 * inventory tab (the free space right of the armour, one by the off hand).
 */
public enum Accessory {
    CLOAK("cloak", 77, 8, 127, 6),
    NECK("neck", 77, 26, 145, 6),
    BACK("back", 77, 44, 163, 6),
    RING_1("ring", 8, 84, 127, 24),
    RING_2("ring", 26, 84, 145, 24),
    HIP("hip", 44, 84, 163, 24),
    CHARM("charm", 62, 84, 9, 6);

    public static final int COUNT = values().length;

    public final TagKey<Item> tag;
    /** Grey outline shown in the empty slot: gui/sprites/container/slot/<name>.png (RING_2 shares the ring icon). */
    public final Identifier emptyIcon;
    public final int x, y, creativeX, creativeY;

    Accessory(String tag, int x, int y, int creativeX, int creativeY) {
        this.tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "equipment/" + tag));
        this.emptyIcon = Identifier.fromNamespaceAndPath(Mechanical.MODID, "container/slot/" + tag);
        this.x = x;
        this.y = y;
        this.creativeX = creativeX;
        this.creativeY = creativeY;
    }
}
