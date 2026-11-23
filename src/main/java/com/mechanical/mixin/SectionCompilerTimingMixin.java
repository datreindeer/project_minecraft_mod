package com.mechanical.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mechanical.client.EdgeBlend;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

/** Dev measurement for edge blending (EdgeBlend.profile, set by people.BlendShots): CPU time of every section mesh. */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerTimingMixin {
    private static final ThreadLocal<long[]> mechanical$start = ThreadLocal.withInitial(() -> new long[1]);

    @Inject(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at = @At("HEAD"))
    private void mechanical$start(SectionPos pos, RenderSectionRegion region, VertexSorting sorting, SectionBufferBuilderPack builders,
            List<AddSectionGeometryEvent.AdditionalSectionRenderer> extra, CallbackInfoReturnable<SectionCompiler.Results> cir) {
        if (EdgeBlend.profile) mechanical$start.get()[0] = System.nanoTime();
    }

    @Inject(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at = @At("RETURN"))
    private void mechanical$end(SectionPos pos, RenderSectionRegion region, VertexSorting sorting, SectionBufferBuilderPack builders,
            List<AddSectionGeometryEvent.AdditionalSectionRenderer> extra, CallbackInfoReturnable<SectionCompiler.Results> cir) {
        if (EdgeBlend.profile) {
            EdgeBlend.COMPILE_NANOS.add(System.nanoTime() - mechanical$start.get()[0]);
            EdgeBlend.COMPILES.increment();
        }
    }
}
