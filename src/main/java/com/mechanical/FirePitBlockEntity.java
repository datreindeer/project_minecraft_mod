package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fire and food of a Fire Pit (on its PART 0 corner). Burns like a campfire (CampfireBlockEntityMixin) at {@link FirePit#DRAIN}
 * times the rate: a fresh fire burns 3-5 campfire-minutes, then the stacked firewood (up to {@link FirePit#STOCK_MAX}, one
 * piece at a time, {@link Campfires#FIREWOOD_TICKS} each), then it sinks to embers ({@link FirePit#EMBER_TICKS}), which
 * Firewood brings back and which die cold if left. Eight food slots on two spits: campfire recipes, cooked food stays
 * on and burns if left, as on the campfire.
 */
public class FirePitBlockEntity extends BlockEntity {
    /** Food on the spits, in pixels from the corner (the model spans 32 x 32): two spits along x, four places each. */
    public static final float SPIT_Y = 18.5F;
    private static final float[] SPIT_Z = {10.5F, 21.5F};

    private final NonNullList<ItemStack> items = NonNullList.withSize(FirePit.SLOTS, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[FirePit.SLOTS], cookingTime = new int[FirePit.SLOTS];
    private final boolean[] cooked = new boolean[FirePit.SLOTS];
    /** Firewood waiting to burn, one item per piece. */
    private final List<ItemStack> stock = new ArrayList<>();
    /** Campfire-burn ticks left while lit; -1 = not started (a fresh fire is rolled on its first lit tick). */
    private int burnLeft = -1;
    private int embersLeft;
    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

    public FirePitBlockEntity(BlockPos pos, BlockState state) {
        super(FirePit.FIRE_PIT_ENTITY.get(), pos, state);
    }

    public static float slotX(int slot) {
        return 8 + (slot % 4) * 16 / 3F;
    }

    public static float slotZ(int slot) {
        return SPIT_Z[slot / 4];
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public int stock() {
        return stock.size();
    }

    // ---- ticking ----

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(FirePitBlock.LIT)) {
            tickLit(level, pos, state);
            return;
        }
        boolean changed = false;
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            if (cookingProgress[slot] > 0) {
                cookingProgress[slot] = Math.max(0, cookingProgress[slot] - 2); // cools off, like vanilla's campfire
                changed = true;
            }
        }
        if (state.getValue(FirePitBlock.EMBERS) && (embersLeft -= Temperature.fireDrain(level, pos, level.getRandom())) <= 0) {
            setFire(false, false);
            changed = true;
        }
        if (changed) setChanged();
    }

    private void tickLit(ServerLevel level, BlockPos pos, BlockState state) {
        RandomSource random = level.getRandom();
        NightDangers.markLit(level, pos, FirePit.REACH);
        if (burnLeft < 0) burnLeft = Campfires.MIN_BURN + random.nextInt(Campfires.MAX_BURN - Campfires.MIN_BURN);
        if ((burnLeft -= (state.getValue(FirePitBlock.GRATE) ? FirePit.GRATE_DRAIN : FirePit.DRAIN) * Temperature.fireDrain(level, pos, random)) <= 0) {
            // nothing stacked on it: a Firewood Pile beside the pit (2x2 from this corner, so 2 blocks out) feeds it
            if (stock.isEmpty() && FirewoodPileBlock.takeNear(level, pos, 2).isEmpty()) {
                sinkToEmbers();
                return;
            }
            boolean charcoal = !stock.isEmpty() && stock.removeLast().is(Items.CHARCOAL); // the next piece of the stack catches
            burnLeft += Campfires.FIREWOOD_TICKS * (charcoal ? FirePit.CHARCOAL_BURN : 1);
            updateFuel();
        }
        boolean changed = false;
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.is(Mechanical.BURNT_FOOD.get())) continue;
            changed = true;
            if (++cookingProgress[slot] < cookingTime[slot]) continue;
            if (!cooked[slot]) {
                // done: stays on the spit and starts its countdown to burning
                SingleRecipeInput input = new SingleRecipeInput(stack);
                items.set(slot, recipes.getRecipeFor(input, level).map(r -> r.value().assemble(input)).orElse(stack));
                cooked[slot] = true;
            } else {
                items.set(slot, new ItemStack(Mechanical.BURNT_FOOD.get()));
                cooked[slot] = false;
            }
            cookingProgress[slot] = 0;
            cookingTime[slot] = Campfires.MIN_BURN_FOOD + random.nextInt(Campfires.MAX_BURN_FOOD - Campfires.MIN_BURN_FOOD);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        if (changed) setChanged();
    }

    /** Particles: a tall smoke column over a burning pit (taller with more firewood), flames and sparks, smoke off the food; embers smoulder. */
    static void clientTick(Level level, BlockPos pos, BlockState state, FirePitBlockEntity pit) {
        RandomSource r = level.getRandom();
        double cx = pos.getX() + 1, cy = pos.getY(), cz = pos.getZ() + 1;
        if (state.getValue(FirePitBlock.LIT)) {
            int fuel = state.getValue(FirePitBlock.FUEL);
            if (r.nextFloat() < (0.3F + 0.1F * fuel) * (state.getValue(FirePitBlock.GRATE) ? FirePit.GRATE_SMOKE : 1)) { // a grate burns clean
                for (int i = 0; i < 2 + r.nextInt(3); i++) {
                    level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, cx + (r.nextDouble() - 0.5) * 1.1, cy + 0.6 + r.nextDouble() * 1.4,
                            cz + (r.nextDouble() - 0.5) * 1.1, 0, 0.07 + r.nextDouble() * 0.03, 0);
                }
            }
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, cx + (r.nextDouble() - 0.5) * 0.9, cy + 0.3 + r.nextDouble() * 0.6, cz + (r.nextDouble() - 0.5) * 0.9, 0, 0.02, 0);
            if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.LAVA, cx + (r.nextDouble() - 0.5) * 0.6, cy + 0.4, cz + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            if (r.nextInt(12) == 0) {
                level.playLocalSound(cx, cy + 0.5, cz, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F + r.nextFloat(), 0.5F + r.nextFloat() * 0.6F, false);
            }
            for (int slot = 0; slot < FirePit.SLOTS; slot++) {
                if (pit.items.get(slot).isEmpty() || r.nextFloat() >= 0.2F) continue;
                double x = pos.getX() + slotX(slot) / 16, y = pos.getY() + SPIT_Y / 16, z = pos.getZ() + slotZ(slot) / 16;
                for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 5.0E-4, 0);
            }
        } else if (state.getValue(FirePitBlock.EMBERS)) {
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, cx + (r.nextDouble() - 0.5) * 1.2, cy + 0.25, cz + (r.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
            if (r.nextInt(8) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, cx + (r.nextDouble() - 0.5) * 1.0, cy + 0.2, cz + (r.nextDouble() - 0.5) * 1.0, 0, 0.01, 0);
        }
    }

    // ---- fire ----

    private void setFire(boolean lit, boolean embers) {
        if (level != null) FirePitBlock.setAll(level, worldPosition, s -> s.setValue(FirePitBlock.LIT, lit).setValue(FirePitBlock.EMBERS, embers));
        setChanged();
    }

    /** The visible log stack: 0 none, 1 up to 6 pieces, 2 up to 12, 3 more. */
    private void updateFuel() {
        int fuel = stock.isEmpty() ? 0 : Math.min(3, 1 + (stock.size() - 1) / 6);
        if (level != null) FirePitBlock.setAll(level, worldPosition, s -> s.setValue(FirePitBlock.FUEL, fuel));
        setChanged();
    }

    private void sinkToEmbers() {
        burnLeft = -1;
        embersLeft = FirePit.EMBER_TICKS;
        setFire(false, true);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.5F);
    }

    /** Lit by the Fire Drill. */
    public void light() {
        burnLeft = -1;
        setFire(true, false);
    }

    /** Wind or rain: sinks to embers (Firewood brings it back). */
    public void blowOut() {
        sinkToEmbers();
    }

    /** Shovel or a horde: out cold. */
    public void douse() {
        burnLeft = -1;
        embersLeft = 0;
        setFire(false, false);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Firewood (or Charcoal on a grated pit): blows embers back into flame (as on the campfire), else goes on the stack. False if the stack is full. */
    boolean feed(ServerLevel level, Player player, ItemStack stack) {
        if (getBlockState().getValue(FirePitBlock.EMBERS) && !getBlockState().getValue(FirePitBlock.LIT)) {
            light();
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
        } else if (stock.size() < FirePit.STOCK_MAX) {
            stock.add(stack.copyWithCount(1));
            updateFuel();
            level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        } else {
            player.sendOverlayMessage(Component.translatable("message.mechanical.fire_pit.full", FirePit.STOCK_MAX));
            return false;
        }
        stack.consume(1, player);
        return true;
    }

    // ---- food ----

    boolean placeFood(ServerLevel level, Player player, ItemStack stack) {
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            if (!items.get(slot).isEmpty()) continue;
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack), level).map(RecipeHolder::value);
            if (recipe.isEmpty()) return false;
            cookingTime[slot] = recipe.get().cookingTime();
            cookingProgress[slot] = 0;
            cooked[slot] = false;
            items.set(slot, stack.consumeAndReturn(1, player));
            level.gameEvent(GameEvent.BLOCK_CHANGE, worldPosition, GameEvent.Context.of(player, getBlockState()));
            sync();
            return true;
        }
        return false;
    }

    /** Gives the player one item off the spits (cooked or burnt first). False if there is nothing. */
    boolean takeItem(Player player) {
        int slot = -1;
        for (int i = 0; i < FirePit.SLOTS; i++) {
            if (items.get(i).isEmpty()) continue;
            boolean done = cooked[i] || items.get(i).is(Mechanical.BURNT_FOOD.get());
            if (slot < 0 || done) slot = i;
            if (done) break;
        }
        if (slot < 0) return false;
        player.getInventory().placeItemBackInInventory(items.get(slot), Prediction.SERVER_ONLY);
        items.set(slot, ItemStack.EMPTY);
        cookingProgress[slot] = 0;
        cooked[slot] = false;
        sync();
        return true;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        Containers.dropContents(level, pos, items);
        for (ItemStack wood : stock) Containers.dropItemStack(level, pos.getX() + 1, pos.getY() + 0.3, pos.getZ() + 1, wood);
    }

    // ---- saving / syncing ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
        output.putIntArray("cooking", cookingProgress);
        output.putIntArray("cooking_total", cookingTime);
        int[] done = new int[FirePit.SLOTS];
        for (int i = 0; i < FirePit.SLOTS; i++) done[i] = cooked[i] ? 1 : 0;
        output.putIntArray("cooked", done);
        output.store("fuel", ItemStack.CODEC.listOf(), List.copyOf(stock));
        output.putInt("burn", burnLeft);
        output.putInt("embers", embersLeft);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        input.getIntArray("cooking").ifPresent(a -> System.arraycopy(a, 0, cookingProgress, 0, Math.min(a.length, FirePit.SLOTS)));
        input.getIntArray("cooking_total").ifPresent(a -> System.arraycopy(a, 0, cookingTime, 0, Math.min(a.length, FirePit.SLOTS)));
        input.getIntArray("cooked").ifPresent(a -> {
            for (int i = 0; i < Math.min(a.length, FirePit.SLOTS); i++) cooked[i] = a[i] != 0;
        });
        stock.clear();
        input.read("fuel", ItemStack.CODEC.listOf()).ifPresent(stock::addAll);
        burnLeft = input.getIntOr("burn", -1);
        embersLeft = input.getIntOr("embers", 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
