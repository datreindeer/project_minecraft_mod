package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** What one {@link FeedingTroughBlock} holds: drinks of water and one stack of fodder. The blockstate mirrors it for the model. */
public class FeedingTroughBlockEntity extends BlockEntity {
    /** A bucket fills a trough with this many drinks (each lasts an animal {@link Husbandry#WATER_TICKS}). */
    public static final int WATER_CAP = 16;
    /** Advancement for filling a trough. */
    static final Identifier FILLED = Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/feeding_trough");

    private ItemStack food = ItemStack.EMPTY;
    private int water;

    public FeedingTroughBlockEntity(BlockPos pos, BlockState state) {
        super(Husbandry.FEEDING_TROUGH_ENTITY.get(), pos, state);
    }

    public int water() {
        return water;
    }

    public ItemStack food() {
        return food;
    }

    void setWater(int water) {
        this.water = Math.clamp(water, 0, WATER_CAP);
        changed();
    }

    /** Moves what fits of {@code stack} in (same item only); returns how many. */
    int insert(ItemStack stack) {
        if (!food.isEmpty() && !ItemStack.isSameItemSameComponents(food, stack)) return 0;
        int n = Math.min(stack.getCount(), stack.getMaxStackSize() - food.getCount());
        if (n <= 0) return 0;
        if (food.isEmpty()) food = stack.copyWithCount(n);
        else food.grow(n);
        stack.shrink(n);
        changed();
        return n;
    }

    ItemStack takeFood(int n) {
        ItemStack out = food.split(n);
        changed();
        return out;
    }

    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState s = getBlockState();
        BlockState shown = s.setValue(FeedingTroughBlock.WATER, water == 0 ? 0 : 1 + (water - 1) * 3 / WATER_CAP)
                .setValue(FeedingTroughBlock.FOOD, !food.isEmpty());
        if (shown != s) level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !food.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, food);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("water", water);
        if (!food.isEmpty()) output.store("food", ItemStack.CODEC, food);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        water = input.getIntOr("water", 0);
        food = input.read("food", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}
