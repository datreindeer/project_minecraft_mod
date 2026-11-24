package com.mechanical;

import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

/**
 * Game test data on an empty 17x17x17 structure (mechanical:test_area, the relative x/y/z 0..16 the tests build in) with
 * {@link #PADDING} blocks of room all round it. Without padding the test grid packs them 6 blocks apart, so their floors,
 * barriers, items and mobs reach into each other's areas (which tests sit side by side changes from run to run); on the
 * 1-block "empty" structure the room was all on the low side and an entity just past x or z 16 stood in a chunk that
 * might not tick. The padded box is cleared and its entities removed when the test is placed, neighbours are spaced past
 * it, its chunks are force-loaded so entities in it tick, and it gets no barrier cage (TestInstanceBlockEntityMixin).
 */
public final class TestArea {
    public static final int PADDING = 16;
    public static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "test_area");

    public static <E> TestData<E> of(E environment, int maxTicks, boolean required) {
        return new TestData<>(environment, Level.OVERWORLD, STRUCTURE, maxTicks, 0, required, Rotation.NONE, false, 1, 1,
                false, PADDING);
    }

    private TestArea() {}
}
