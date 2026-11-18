package com.mechanical;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Clay kiln (lives on the LOWER half). Slots: 0 input, 1 fuel, 2 output.
 * Once lit (fire drill, or placed on a burning campfire) the fire burns fuel continuously, baking or not; when the
 * fuel runs out it goes out and must be lit again. Baking progress pauses (doesn't reset) while the fire is out.
 */
public class KilnBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT = 0, FUEL = 1, OUTPUT = 2;
    /** The firewood of the campfire the kiln was built on: three firewood's worth. */
    public static final int CAMPFIRE_BURN_TICKS = 3600;
    /** An idle lit kiln smoulders this long, then goes out. */
    public static final int IDLE_TICKS = 1200;

    /** {@code count} input items are used up per result. */
    public record Recipe(Predicate<ItemStack> input, int count, Supplier<Item> result, int ticks) {}
    public record Fuel(Predicate<ItemStack> item, int ticks) {}

    // shortcut: hardcoded like the Tool Station recipes; move to datapack recipes when modpacks need to edit them.
    public static final List<Recipe> RECIPES = List.of(
            new Recipe(s -> s.is(Items.CLAY_BALL), 1, () -> Items.BRICK, 400),
            new Recipe(s -> s.is(ItemTags.LOGS_THAT_BURN), 1, () -> Items.CHARCOAL, 600),
            new Recipe(s -> s.is(Mechanical.FIREWOOD_TAG), 2, () -> Items.CHARCOAL, 600),
            new Recipe(s -> s.is(Mechanical.UNFIRED_CRUCIBLE.get()), 1, Mechanical.CRUCIBLE, 800));

    // Our own fuel list (vanilla's fuel component needs a loot context); controls what burns in the stone age.
    public static final List<Fuel> FUELS = List.of(
            new Fuel(s -> s.is(Items.STICK) || s.is(ItemTags.SAPLINGS), 200),       // 10 s
            new Fuel(s -> s.is(Mechanical.FIREWOOD_TAG), 1200),                       // 1 min
            new Fuel(s -> s.is(ItemTags.PLANKS), 600),                                // 30 s
            new Fuel(s -> s.is(ItemTags.LOGS_THAT_BURN), 2400),                       // 2 min, as its 2 firewood
            new Fuel(s -> s.is(Items.CHARCOAL) || s.is(Items.COAL), 4800));           // 4 min

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress, total, burn, burnTotal, idle;

    /** Synced to the menu: 0 progress, 1 total, 2 burn left, 3 burn total. */
    final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> burn;
                default -> isLit() ? burnTotal : 0; // the screen shows the flame only while lit
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return 4;
        }
    };

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(Mechanical.KILN_ENTITY.get(), pos, state);
    }

    public static Optional<Recipe> recipeFor(ItemStack stack) {
        return RECIPES.stream().filter(r -> r.input().test(stack)).findFirst();
    }

    public static int burnTicks(ItemStack stack) {
        return FUELS.stream().filter(f -> f.item().test(stack)).mapToInt(Fuel::ticks).findFirst().orElse(0);
    }

    public boolean isLit() {
        return getBlockState().getValue(KilnBlock.LIT);
    }

    public void startBurning(int ticks) {
        burn = burnTotal = ticks;
        setLit(true);
        setChanged();
    }

    /** There is something to catch: fuel left from before it went out, or fuel in the slot. */
    public boolean canLight() {
        return burn > 0 || burnTicks(items.get(FUEL)) > 0;
    }

    /** Fire drill: lights the kiln, finishing any partly burnt fuel first. Returns false if there's nothing to burn. */
    public boolean tryLight() {
        if (isLit()) return true;
        idle = 0;
        if (burn > 0) {
            setLit(true);
            return true;
        }
        int ticks = burnTicks(items.get(FUEL));
        if (ticks == 0) return false;
        items.get(FUEL).shrink(1);
        startBurning(ticks);
        return true;
    }

    private void setLit(boolean lit) {
        setState(lit, lit && getBlockState().getValue(KilnBlock.ACTIVE));
    }

    /** LIT and ACTIVE live on the block state (model, light, particles); the upper half follows through updateShape. */
    private void setState(boolean lit, boolean active) {
        Level level = getLevel();
        BlockState state = getBlockState();
        if (level == null || (state.getValue(KilnBlock.LIT) == lit && state.getValue(KilnBlock.ACTIVE) == active)) return;
        level.setBlock(getBlockPos(), state.setValue(KilnBlock.LIT, lit).setValue(KilnBlock.ACTIVE, active), Block.UPDATE_ALL);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
        ItemStack input = kiln.items.get(INPUT);
        Recipe recipe = recipeFor(input).orElse(null);
        boolean canBake = recipe != null && input.getCount() >= recipe.count() && kiln.canOutput(recipe);

        // Fuel is only used while baking; an idle lit kiln smoulders for a while, then goes out.
        if (kiln.isLit() && !canBake && ++kiln.idle >= IDLE_TICKS) {
            kiln.idle = 0;
            kiln.setLit(false);
        }
        if (kiln.isLit()) NightDangers.markLit(level, pos);
        if (kiln.isLit() && canBake) {
            kiln.idle = 0;
            if (kiln.burn <= 0) {
                int ticks = burnTicks(kiln.items.get(FUEL));
                if (ticks > 0) {
                    kiln.items.get(FUEL).shrink(1);
                    kiln.burn = kiln.burnTotal = ticks;
                } else {
                    kiln.burnTotal = 0;
                    kiln.setLit(false);
                }
            }
            if (kiln.burn > 0) kiln.burn = Math.max(0, kiln.burn - Temperature.fireDrain(level, pos, level.getRandom()));
            kiln.setChanged();
        }
        kiln.setState(kiln.isLit(), kiln.isLit() && canBake);

        if (!canBake) {
            kiln.progress = 0;
            kiln.total = 0;
            return;
        }
        kiln.total = recipe.ticks();
        if (!kiln.isLit()) return;
        if (++kiln.progress >= kiln.total) {
            kiln.progress = 0;
            input.shrink(recipe.count());
            ItemStack out = kiln.items.get(OUTPUT);
            if (out.isEmpty()) kiln.items.set(OUTPUT, new ItemStack(recipe.result().get()));
            else out.grow(1);
        }
        kiln.setChanged();
    }

    private boolean canOutput(Recipe recipe) {
        ItemStack out = items.get(OUTPUT);
        return out.isEmpty() || (out.is(recipe.result().get()) && out.getCount() < out.getMaxStackSize());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> recipeFor(stack).isPresent();
            case FUEL -> burnTicks(stack) > 0;
            default -> false;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        burn = input.getIntOr("burn", 0);
        burnTotal = input.getIntOr("burn_total", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("burn", burn);
        output.putInt("burn_total", burnTotal);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mechanical.kiln");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new KilnMenu(containerId, inventory, this, data);
    }
}
