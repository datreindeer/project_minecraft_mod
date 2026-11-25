package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mechanical.Mechanical;
import com.mechanical.equipment.Equipment;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * The survival inventory with the accessory slots ({@link Equipment}): 22 pixels taller, drawn from our own background
 * (inventory_tall.png, scripts/gen_gui_textures.py: wider player box, a column and a row of accessory slots, crafting 2
 * pixels to the right). The recipe book button stays just under the crafting grid.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    private static final Identifier TALL = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/inventory_tall.png");
    private static final int VANILLA_HEIGHT = 166;

    private InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> book, Inventory inventory, Component title) {
        super(menu, book, inventory, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mechanical$taller(Player player, CallbackInfo ci) {
        ((AbstractContainerScreenAccessor) this).mechanical$setImageHeight(VANILLA_HEIGHT + Equipment.SCREEN_EXTRA_HEIGHT);
        titleLabelX += 2; // "Crafting" follows its grid
    }

    /** The player model is centred in the box, which is 3 pixels wider. */
    @ModifyArg(method = "extractBackground", index = 3, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
    private int mechanical$widerPlayerBox(int right) {
        return right + 3;
    }

    @WrapOperation(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void mechanical$withAccessorySlots(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
            int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
        original.call(graphics, pipeline, TALL, x, y, 0.0F, 0.0F, width, VANILLA_HEIGHT + Equipment.SCREEN_EXTRA_HEIGHT, textureWidth, textureHeight);
    }

    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    private void mechanical$recipeButton(CallbackInfoReturnable<ScreenPosition> cir) {
        cir.setReturnValue(new ScreenPosition(leftPos + 106, topPos + 61));
    }
}
