package com.mechanical.client;

import java.util.List;

import com.mechanical.TravoisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * A Trader Caravan's load on show on a cart's or wagon's bed (TravoisEntity#cargo, people.Caravans): laid out in a grid of
 * cells across and along the bed, front to back; blocks (logs, barrels, crates of ore) stand square as small blocks, a
 * second one in a cell stacked on the first, and anything else (tools, pelts, baskets of berries) lies flat on top, laid
 * lengthwise or crosswise in turn, so the load looks packed, not thrown in. Also: the caravan's dyed tilt textures.
 */
final class CargoRender {
    static final int MAX = 12;

    /** Render data for up to {@link #MAX} pieces. */
    static final class State {
        final ItemStackRenderState[] items = new ItemStackRenderState[MAX];
        final boolean[] block = new boolean[MAX];
        int count;

        State() {
            for (int i = 0; i < MAX; i++) items[i] = new ItemStackRenderState();
        }
    }

    static void extract(TravoisEntity bed, State state, ItemModelResolver resolver) {
        List<ItemStack> cargo = bed.cargo();
        state.count = Math.min(MAX, cargo.size());
        for (int i = 0; i < state.count; i++) {
            ItemStack s = cargo.get(i);
            state.block[i] = s.getItem() instanceof BlockItem;
            resolver.updateForNonLiving(state.items[i], s, ItemDisplayContext.NONE, bed);
        }
    }

    /**
     * Draws the load in model space (front -z, y up, the bed's floor at {@code floor}): {@code cols} cells across and
     * {@code rows} along, each {@code cell} blocks; blocks drawn at {@code scale}.
     */
    static void submit(State state, int cols, int rows, float cell, float floor, float scale, PoseStack pose, SubmitNodeCollector collector, int light, int outline) {
        int cells = cols * rows;
        float[] height = new float[cells];
        for (int i = 0; i < state.count; i++) {
            int c = i % cells;
            float x = (c % cols - (cols - 1) / 2.0F) * cell, z = (c / cols - (rows - 1) / 2.0F) * cell;
            pose.pushPose();
            if (state.block[i]) {
                // the block model is centred on the origin: stand it on what is already in the cell
                pose.translate(x, floor + height[c] + scale / 2, z);
                pose.scale(scale, scale, scale);
                height[c] += scale;
            } else {
                // a flat item lies on top, lengthwise or crosswise
                pose.translate(x, floor + height[c] + 0.03F, z);
                pose.rotate(Axis.YP.rotationDegrees(i % 2 * 90));
                pose.rotate(Axis.XP.rotationDegrees(90));
                float s = scale * 1.1F;
                pose.scale(s, s, s);
                height[c] += 0.04F;
            }
            state.items[i].submit(pose, collector, light, OverlayTexture.NO_OVERLAY, outline);
            pose.popPose();
        }
    }

    private static final java.util.Map<String, net.minecraft.resources.Identifier> TILTS = new java.util.HashMap<>();

    /**
     * The tilt texture dyed for caravan kind {@code kind} (textures/entity/&lt;cart|wagon&gt;_canopy_&lt;kind&gt;.png), or the plain
     * linen one when that kind has none.
     */
    // shortcut: looked up once per kind; a resource pack adding a tilt needs a restart
    static net.minecraft.resources.Identifier tilt(String kind, boolean wagon) {
        String base = wagon ? "wagon_canopy" : "cart_canopy";
        return TILTS.computeIfAbsent(base + kind, k -> {
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(com.mechanical.Mechanical.MODID, "textures/entity/" + base + "_" + kind + ".png");
            boolean has = !kind.isEmpty() && net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
            return has ? id : net.minecraft.resources.Identifier.fromNamespaceAndPath(com.mechanical.Mechanical.MODID, "textures/entity/" + base + ".png");
        });
    }

    private CargoRender() {}
}
