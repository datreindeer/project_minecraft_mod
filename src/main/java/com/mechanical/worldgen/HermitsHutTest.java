package com.mechanical.worldgen;

import java.util.UUID;

import com.mechanical.clan.Clans;
import com.mechanical.clan.Lands;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Part of the clans game test: a Hermit's Hut is a named land, found by its hermit's mark or, in old worlds, by its shape. */
public final class HermitsHutTest {
    public static void check(GameTestHelper h, Clans data) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
        var level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(8, 1, 8));
        if (!new HermitsHut().place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin)) fail(h, "the hut places");
        var hermits = level.getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(origin).inflate(4));
        if (hermits.size() != 1) fail(h, "one hermit, not " + hermits.size());
        Villager hermit = hermits.getFirst();
        BlockPos home = HermitsHut.home(hermit);
        if (home == null) fail(h, "the hermit knows his hut");
        UUID me = UUID.randomUUID();
        Lands.Land land = Lands.landAt(data, level, me, Vec3.atCenterOf(home.offset(6, 0, 0)));
        if (land == null || land.kind() != Lands.HERMIT) fail(h, "the hut's land, got " + land);
        hermit.getPersistentData().remove("mechanical_hermit_hut"); // as in a world made before the mark
        if (!home.equals(HermitsHut.home(hermit))) fail(h, "an unmarked hermit is known by his hut, got " + HermitsHut.home(hermit));
        if (!land.equals(Lands.landAt(data, level, me, Vec3.atCenterOf(home)))) fail(h, "the same name inside the hut");
        if (Lands.landAt(data, level, me, Vec3.atCenterOf(home.offset(20, 0, 0))) != null) fail(h, "the wild beyond it");
        hermit.discard();
    }

    private static void fail(GameTestHelper h, String what) {
        throw h.assertionException("hermit's hut: " + what);
    }

    private HermitsHutTest() {}
}
