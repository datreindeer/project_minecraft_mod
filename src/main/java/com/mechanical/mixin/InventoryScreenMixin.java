package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mechanical.equipment.Accessory;
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
 * The survival inventory with the accessory slots ({@link Equipment}): 22 pixels taller, drawn from vanilla's own
 * inventory texture (so resource packs still apply): the top part as it is, its plain row under the player model
 * stretched into a band for the row of accessory slots, then the main inventory; slot frames copied from it for the new
 * slots. The recipe book button stays just under the crafting grid.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    /** Rows 0..80 of the vanilla texture: armour, player model, off hand, crafting. Row 81 is plain background. */
    private static final int TOP = 81, VANILLA_HEIGHT = 166;

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
        int extra = Equipment.SCREEN_EXTRA_HEIGHT;
        original.call(graphics, pipeline, texture, x, y, 0.0F, 0.0F, width, TOP, textureWidth, textureHeight);
        graphics.blit(pipeline, texture, x, y + TOP, 0.0F, TOP, width, extra, width, 1, textureWidth, textureHeight);
        original.call(graphics, pipeline, texture, x, y + TOP + extra, 0.0F, (float) TOP, width, VANILLA_HEIGHT - TOP, textureWidth, textureHeight);
        // crafting: blank it (plain background at 4,10), then the grid 2 pixels, arrow and result 1 pixel to the right
        graphics.blit(pipeline, texture, x + 97, y + 17, 4.0F, 10.0F, 75, 37, 1, 1, textureWidth, textureHeight);
        original.call(graphics, pipeline, texture, x + 99, y + 17, 97.0F, 17.0F, 36, 36, textureWidth, textureHeight);
        original.call(graphics, pipeline, texture, x + 135, y + 17, 134.0F, 17.0F, 19, 36, textureWidth, textureHeight);
        original.call(graphics, pipeline, texture, x + 154, y + 27, 153.0F, 27.0F, 18, 18, textureWidth, textureHeight);
        // player box widened by 3: its right end (3 black columns and the light edge) again, 3 pixels further right
        original.call(graphics, pipeline, texture, x + 75, y + 7, 72.0F, 7.0F, 4, 72, textureWidth, textureHeight);
        original.call(graphics, pipeline, texture, x + 79, y + 61, 7.0F, 83.0F, 18, 18, textureWidth, textureHeight); // off hand
        // the first main-inventory slot's frame (7,83), copied under each accessory slot
        for (Accessory slot : Accessory.values()) {
            original.call(graphics, pipeline, texture, x + slot.x - 1, y + slot.y - 1, 7.0F, 83.0F, 18, 18, textureWidth, textureHeight);
        }
    }

    @Inject(method = "getRecipeBookButtonPosition", at = @At("HEAD"), cancellable = true)
    private void mechanical$recipeButton(CallbackInfoReturnable<ScreenPosition> cir) {
        cir.setReturnValue(new ScreenPosition(leftPos + 106, topPos + 61));
    }
}
