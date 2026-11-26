package com.mechanical.viewer;

import java.util.List;

import net.minecraft.world.item.Item;

/** One input of a recipe: any of {@code options}, {@code count} of it; a {@code tool} only loses durability. */
public record Slot(List<Item> options, int count, boolean tool) {}
