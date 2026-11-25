package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** What one {@link FeedingTroughBlock} holds: its share of the row's water and one stack of fodder. The blockstates mirror the row. */
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

    /** Sets the share without evening the row out ({@link FeedingTroughBlock#level} does that). */
    void store(int water) {
        if (water == this.water) return;
        this.water = water;
        setChanged();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) FeedingTroughBlock.level(level, worldPosition);
    }

    /** Rows saved before the water was pooled (uneven) even out when loaded. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        if (!food.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, food);
        // its water runs into the troughs it was joined to, as far as there is room
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (FeedingTroughBlockEntity t : FeedingTroughBlock.group(level, pos.relative(d), pos)) {
                int n = Math.min(water, WATER_CAP - t.water());
                t.store(t.water() + n);
                water -= n;
            }
        }
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
