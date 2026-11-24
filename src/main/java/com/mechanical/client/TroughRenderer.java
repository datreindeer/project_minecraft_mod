package com.mechanical.client;

import com.mechanical.ClayChannelBlock;
import com.mechanical.HighFurnace;
import com.mechanical.Mechanical;
import com.mechanical.Molten;
import com.mechanical.TroughBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jspecify.annotations.Nullable;

/**
 * The melt in a Refractory Trough or Trough Gate ({@link TroughBlockEntity}), drawn like a Clay Channel's
 * ({@link MoltenRenderer.Channel#channel}) in the melt's own colour: streaming while it runs, cooling as it stands
 * (white-hot, orange, dark red, a grey crust), frozen dark where it ran past its reach.
 */
public class TroughRenderer implements BlockEntityRenderer<TroughBlockEntity, TroughRenderer.State> {
    public static class State extends BlockEntityRenderState {
        @Nullable Molten molten;
        float fill, time, cooled, hot;
        int sides;
        @Nullable Direction up;
        boolean sand;
    }

    public TroughRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TroughBlockEntity t, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(t, state, partialTicks, camera, breakProgress);
        long now = t.getLevel() == null ? 0 : t.getLevel().getGameTime();
        state.molten = t.molten();
        state.fill = t.amount() / (float) TroughBlockEntity.CAPACITY;
        state.cooled = t.cooled(now);
        // how hot it still is: white-hot near the tap, dimming down the line to the end of its reach
        state.hot = state.molten == null ? 0 : Math.clamp(1 - t.dist() / (float) Math.max(1, com.mechanical.TroughFlow.reach(t.heat(), state.molten) + 1), 0, 1);
        state.time = ((now % 1000) + partialTicks) * (t.running(now) ? 0.04F : 0.004F);
        BlockState block = t.getBlockState();
        state.sand = block.getBlock() instanceof com.mechanical.RefractoryTroughBlock.SandRunner;
        state.up = null;
        state.sides = 0;
        if (block.getBlock() instanceof ClayChannelBlock) {
            state.up = block.getValue(ClayChannelBlock.SLOPE).up;
            for (Direction d : Direction.Plane.HORIZONTAL) if (block.getValue(ClayChannelBlock.SIDES.get(d))) state.sides |= 1 << d.get2DDataValue();
        }
    }

    /** The melt's colour cooling: its own glow, through orange and dark red to a grey crust tinted by the metal. */
    static int cooling(Molten m, float cooled) {
        int hot = 0xFF000000 | m.color, skin = ARGB.srgbLerp(0.3F, 0xFF4A4642, hot) | 0xFF000000;
        if (cooled < 0.5F) return ARGB.srgbLerp(cooled / 0.5F, hot, 0xFFE0661A) | 0xFF000000;
        if (cooled < 0.8F) return ARGB.srgbLerp((cooled - 0.5F) / 0.3F, 0xFFE0661A, 0xFF8A1E08) | 0xFF000000;
        return ARGB.srgbLerp((cooled - 0.8F) / 0.2F, 0xFF8A1E08, skin) | 0xFF000000;
    }

    /** A melt's glow by how hot it still is: toward white-hot near the source, toward a deep orange-red at the end of its reach. */
    static int glow(Molten m, float hot) {
        int base = 0xFF000000 | m.color;
        return hot > 0.5F ? ARGB.srgbLerp((hot - 0.5F) * 0.7F, base, 0xFFFFF4D2) | 0xFF000000
                : ARGB.srgbLerp((0.5F - hot) * 1.4F, base, 0xFFB8380C) | 0xFF000000;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.molten == null) return;
        int c = state.cooled < 0.05F ? glow(state.molten, state.hot) : cooling(state.molten, state.cooled);
        if (state.sand) pose.translate(0, 11 / 16F, 0); // a runner's groove is at the top of its sand block
        MoltenRenderer.Channel.channel(collector, pose, MoltenRenderer.CRUST, c, state.cooled >= 1 ? 0 : state.time, state.up, state.sides, Math.min(1, state.fill));
    }

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(HighFurnace.TROUGH_ENTITY.get(), TroughRenderer::new);
        }
    }
}
