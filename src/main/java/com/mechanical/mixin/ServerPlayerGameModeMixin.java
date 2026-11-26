package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mechanical.skills.SkillPerks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Mining / Woodcutting L60: a pickaxe or axe may skip the wear of a broken block (NeoForge has no durability event). */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void mechanical$keepTool(ItemStack stack, Level level, BlockState state, BlockPos pos, Player player, Operation<Void> original) {
        if (!SkillPerks.keepsTool(player, stack)) original.call(stack, level, state, pos, player);
    }
}
