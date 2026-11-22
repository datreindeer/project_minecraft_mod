package com.mechanical.client;

import com.mechanical.CaveDark;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Tracks whether the local player is underground ({@link CaveDark}) and fades it; DarkNightsMixin reads {@link #strength}. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class CaveDarkness {
    private static boolean underground;
    private static float fade;
    /** Dev only: CaveShots turns the effect off for before/after pictures. */
    public static boolean harnessOff;

    public static float strength() {
        return harnessOff ? 0 : CaveDark.strength(fade);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimensionType().hasSkyLight()) {
            underground = false;
            fade = 0;
            return;
        }
        BlockPos eye = BlockPos.containing(mc.player.getEyePosition());
        int depth = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, eye.getX(), eye.getZ()) - eye.getY();
        // shortcut: water counts as surface for the heightmap, so deep sea floors would go black; diving never counts as underground
        boolean wet = mc.player.isEyeInFluid(FluidTags.WATER);
        underground = !wet && CaveDark.underground(underground, mc.level.getBrightness(LightLayer.SKY, eye), depth);
        fade = CaveDark.step(fade, underground);
    }

    private CaveDarkness() {}
}
