package com.mechanical;

import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * Game test data on the 1-block "empty" structure with {@link #PADDING} blocks of room all round. The tests build in
 * relative x/y/z 0..16; without padding the test grid packs them 6 blocks apart, so their floors, barriers, items and mobs
 * reach into each other's areas (which tests sit side by side changes from run to run). The padded box is cleared and its
 * entities removed when the test is placed, neighbours are spaced past it, and its chunks are force-loaded so entities in
 * it tick (TestInstanceBlockEntityMixin).
 */
public final class TestArea {
    public static final int PADDING = 16;

    public static <E> TestData<E> of(E environment, int maxTicks, boolean required) {
        return new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), maxTicks, 0, required, Rotation.NONE, false, 1, 1,
                false, PADDING);
    }

    private TestArea() {}
}
