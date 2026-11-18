package com.mechanical.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets {@link com.mechanical.Trees} register its wood as flammable (FireBlock.setFlammable is private). */
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("setFlammable")
    void mechanical$setFlammable(Block block, int igniteOdds, int burnOdds);
}
