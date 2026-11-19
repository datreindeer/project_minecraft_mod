package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mechanical.CampfireAccess;
import com.mechanical.Campfires;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Campfire burn time, embers, and food that stays on the fire after cooking and burns if left too long.
 * Replaces vanilla's cookTick (which pops cooked food off straight away).
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin implements CampfireAccess {
    @Shadow @Final private NonNullList<ItemStack> items;
    @Shadow @Final private int[] cookingProgress;
    @Shadow @Final private int[] cookingTime;

    /** Burn ticks left while lit; -1 = not started (set on the first lit tick). */
    @Unique private int mechanical$burnLeft = -1;
    @Unique private int mechanical$embersLeft;
    /** Slot holds food that is already cooked (it burns next). */
    @Unique private final boolean[] mechanical$cooked = new boolean[4];

    @Inject(method = "cookTick", at = @At("HEAD"), cancellable = true)
    private static void mechanical$cookTick(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity entity,
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipeCache, CallbackInfo ci) {
        ((CampfireBlockEntityMixin) (Object) entity).mechanical$tickLit(level, pos, state, recipeCache);
        ci.cancel();
    }

    @Inject(method = "cooldownTick", at = @At("HEAD"))
    private static void mechanical$cooldownTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity entity, CallbackInfo ci) {
        CampfireBlockEntityMixin self = (CampfireBlockEntityMixin) (Object) entity;
        if (!level.isClientSide() && Campfires.isEmbers(state) && (self.mechanical$embersLeft -= com.mechanical.Temperature.fireDrain(level, pos, level.getRandom())) <= 0) Campfires.toCold(level, pos, state);
    }

    @Unique
    private void mechanical$tickLit(ServerLevel level, BlockPos pos, BlockState state, RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes) {
        RandomSource random = level.getRandom();
        com.mechanical.NightDangers.markLit(level, pos);
        if (mechanical$burnLeft < 0) mechanical$burnLeft = Campfires.MIN_BURN + random.nextInt(Campfires.MAX_BURN - Campfires.MIN_BURN);
        if ((mechanical$burnLeft -= com.mechanical.Temperature.fireDrain(level, pos, random)) <= 0) {
            mechanical$burnLeft = -1;
            mechanical$embersLeft = Campfires.EMBER_TICKS;
            Campfires.toEmbers(level, pos, state);
            return;
        }

        boolean changed = false;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.is(Mechanical.BURNT_FOOD.get())) continue;
            changed = true;
            if (++cookingProgress[slot] < cookingTime[slot]) continue;
            if (!mechanical$cooked[slot]) {
                // done: the cooked food stays on the fire, and starts its countdown to burning
                SingleRecipeInput input = new SingleRecipeInput(stack);
                ItemStack result = recipes.getRecipeFor(input, level).map(r -> r.value().assemble(input)).orElse(stack);
                items.set(slot, result);
                mechanical$cooked[slot] = true;
            } else {
                items.set(slot, new ItemStack(Mechanical.BURNT_FOOD.get()));
                mechanical$cooked[slot] = false;
            }
            cookingProgress[slot] = 0;
            cookingTime[slot] = Campfires.MIN_BURN_FOOD + random.nextInt(Campfires.MAX_BURN_FOOD - Campfires.MIN_BURN_FOOD);
            level.sendBlockUpdated(pos, state, state, 3);
        }
        CampfireBlockEntity self = (CampfireBlockEntity) (Object) this;
        if (changed) self.setChanged();
    }

    @Override
    public void mechanical$addFuel(int ticks) {
        mechanical$burnLeft = Math.min(Campfires.MAX_FUEL, Math.max(mechanical$burnLeft, 0) + ticks);
        ((CampfireBlockEntity) (Object) this).setChanged();
    }

    @Override
    public void mechanical$relight() {
        mechanical$burnLeft = -1;
        ((CampfireBlockEntity) (Object) this).setChanged();
    }

    @Override
    public void mechanical$blowOut() {
        CampfireBlockEntity self = (CampfireBlockEntity) (Object) this;
        mechanical$burnLeft = -1;
        mechanical$embersLeft = Campfires.EMBER_TICKS;
        if (self.getLevel() != null) Campfires.toEmbers(self.getLevel(), self.getBlockPos(), self.getBlockState());
        self.setChanged();
    }

    @Override
    public boolean mechanical$hasItems() {
        return items.stream().anyMatch(s -> !s.isEmpty());
    }

    @Override
    public boolean mechanical$takeItem(Player player) {
        int slot = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) continue;
            boolean done = mechanical$cooked[i] || items.get(i).is(Mechanical.BURNT_FOOD.get());
            if (slot < 0 || done) slot = i;
            if (done) break;
        }
        if (slot < 0) return false;
        player.getInventory().placeItemBackInInventory(items.get(slot), Prediction.SERVER_ONLY);
        items.set(slot, ItemStack.EMPTY);
        cookingProgress[slot] = 0;
        mechanical$cooked[slot] = false;
        CampfireBlockEntity self = (CampfireBlockEntity) (Object) this;
        self.setChanged();
        if (self.getLevel() != null) self.getLevel().sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), 3);
        return true;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void mechanical$load(ValueInput input, CallbackInfo ci) {
        mechanical$burnLeft = input.getIntOr("mechanical_burn", -1);
        mechanical$embersLeft = input.getIntOr("mechanical_embers", 0);
        input.getIntArray("mechanical_cooked").ifPresent(a -> {
            for (int i = 0; i < Math.min(a.length, 4); i++) mechanical$cooked[i] = a[i] != 0;
        });
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void mechanical$save(ValueOutput output, CallbackInfo ci) {
        output.putInt("mechanical_burn", mechanical$burnLeft);
        output.putInt("mechanical_embers", mechanical$embersLeft);
        int[] cooked = new int[4];
        for (int i = 0; i < 4; i++) cooked[i] = mechanical$cooked[i] ? 1 : 0;
        output.putIntArray("mechanical_cooked", cooked);
    }
}
