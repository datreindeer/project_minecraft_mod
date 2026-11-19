package com.mechanical.wildlife;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Cape hare of the desert and badlands: the hare's build and habits in a pale sandy coat. */
public class DesertHare extends Hare {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/desert_hare.png");

    public DesertHare(EntityType<? extends DesertHare> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? super.texture() : TEXTURE;
    }
}
