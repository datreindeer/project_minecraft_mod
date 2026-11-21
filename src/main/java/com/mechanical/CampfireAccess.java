package com.mechanical;

import net.minecraft.world.entity.player.Player;

/** Added to the vanilla CampfireBlockEntity by a mixin: fuel and taking food off. */
public interface CampfireAccess {
    /** Adds burn time to a lit campfire. */
    void mechanical$addFuel(int ticks);

    /** Embers caught again: burning for a fresh while. */
    void mechanical$relight();
    /** Blown out by the wind: sinks to embers (which can be fed with firewood again). */
    void mechanical$blowOut();

    /** Gives the player one item off the fire (cooked or burnt first). False if there is nothing. */
    boolean mechanical$takeItem(Player player);

    boolean mechanical$hasItems();

    /** Peat (or Dried Peat) was fed to the fire: for this many burn ticks it gives peat smoke (Smoking Rack twice as fast). */
    void mechanical$addPeat(int ticks);

    /** Burning on peat right now. */
    boolean mechanical$peatSmoke();
}
