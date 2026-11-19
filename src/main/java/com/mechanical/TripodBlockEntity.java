package com.mechanical;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The kettle hanging in a {@link TripodBlock}: the Bronze Kettle itself (kept as its stack, so Poor Bronze stays poor),
 * the water, what was put in, its heat and simmering progress, and once done what is left to take out (solid outputs
 * with an empty hand, bowls of stew or broth with a Bowl). Ingredients are used up exactly once, when the pot is done
 * ({@link Kettle#finish}); breaking the tripod gives back the kettle and the tripod, never the contents.
 */
public class TripodBlockEntity extends BlockEntity {
    /** Kettle geometry for the renderer and particles, in pixels: over a Fire Pit it hangs {@link #PIT_LIFT} higher (above the spits). */
    public static final float PIT_LIFT = 4, LIQUID_LOW = -3.5F, LIQUID_HIGH = 1.5F, INNER = 4;

    private ItemStack kettle = ItemStack.EMPTY;
    private boolean water;
    private final List<ItemStack> contents = new ArrayList<>();
    private float heat, progress;
    private boolean done;
    private final List<ItemStack> outputs = new ArrayList<>();
    private ItemStack portion = ItemStack.EMPTY;
    private int portions, maxPortions;
    private int colour = Kettle.colour(null, List.of());
    private Kettle.Mode mode;

    public TripodBlockEntity(BlockPos pos, BlockState state) {
        super(Kettle.TRIPOD_ENTITY.get(), pos, state);
    }

    public boolean water() {
        return water;
    }

    public List<ItemStack> contents() {
        return contents;
    }

    public int colour() {
        return colour;
    }

    /** How full the pot is, 0..1: full while cooking, going down as bowls are ladled out. */
    public float fill() {
        if (!water) return 0;
        return done && maxPortions > 0 ? 0.25F + 0.75F * portions / maxPortions : 1;
    }

    // ---- ticking ----

    /** How fast the fire below cooks: 1 for a lit campfire, more for a lit Fire Pit, a little for embers (warm only), 0 cold. */
    static float fireRate(BlockState below) {
        if (CampfireBlock.isLitCampfire(below)) return 1;
        if (FirePit.isLit(below)) return Kettle.PIT_RATE;
        return Campfires.isEmbers(below) || FirePit.smoulders(below) ? Kettle.EMBER_RATE : 0;
    }

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (kettle.isEmpty()) return;
        float rate = fireRate(level.getBlockState(pos.below()));
        float cap = rate >= 1 ? Kettle.BOIL : rate > 0 ? Kettle.WARM : 0;
        float before = heat;
        heat = heat < cap ? Math.min(cap, heat + Kettle.HEAT_UP * Math.max(rate, Kettle.EMBER_RATE)) : Math.max(cap, heat - Kettle.COOL);
        boolean hot = water && heat >= Kettle.BOIL;
        if (hot != state.getValue(TripodBlock.HOT)) level.setBlock(pos, state.setValue(TripodBlock.HOT, hot), Block.UPDATE_CLIENTS);
        if (hot && !done && mode != null) {
            progress += rate;
            if (progress >= Kettle.cookTime(mode)) finish(level);
        }
        if (heat != before || hot) setChanged();
    }

    private void finish(ServerLevel level) {
        Kettle.Result result = Kettle.finish(level, contents);
        outputs.clear();
        outputs.addAll(result.outputs());
        portion = result.portion();
        portions = maxPortions = result.portions();
        done = true;
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 0.8F);
        if (outputs.isEmpty() && portions == 0) reset(); // nothing came of it
        sync();
    }

    /** Steam and bubbles while it boils, a wisp of steam while warm; a bubbling sound now and then. */
    static void clientTick(Level level, BlockPos pos, BlockState state, TripodBlockEntity tripod) {
        if (!state.getValue(TripodBlock.KETTLE) || !tripod.water) return;
        RandomSource r = level.getRandom();
        float lift = state.getValue(TripodBlock.PIT) > 0 ? PIT_LIFT : 0;
        double x = pos.getX() + TripodBlock.centreX(state) / 16, z = pos.getZ() + TripodBlock.centreZ(state) / 16;
        double y = pos.getY() + (LIQUID_LOW + (LIQUID_HIGH - LIQUID_LOW) * tripod.fill() + lift) / 16;
        if (state.getValue(TripodBlock.HOT)) {
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.BUBBLE_POP, x + (r.nextDouble() - 0.5) * 0.45, y + 0.02, z + (r.nextDouble() - 0.5) * 0.45, 0, 0.02, 0);
            if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.WHITE_SMOKE, x + (r.nextDouble() - 0.5) * 0.4, y + 0.1, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.04, 0);
            if (r.nextInt(40) == 0) level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5F, 0.7F + r.nextFloat() * 0.3F, false);
        }
    }

    // ---- use ----

    private static boolean isWater(ItemStack s) {
        return s.is(Items.WATER_BUCKET) || s.is(Mechanical.CLAY_WATER_BUCKET.get());
    }

    /** Does this held item do something with this tripod (else the click goes on to the item, e.g. placing a block)? */
    boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Bronze.BRONZE_KETTLE.get())) return kettle.isEmpty();
        if (kettle.isEmpty()) return false;
        if (isWater(stack) || stack.is(Items.BOWL)) return true;
        if (stack.is(Mechanical.FIREWOOD_TAG) || stack.getItem() instanceof FireDrillItem) return false;
        return water && !done; // offered as an ingredient (use() says if it can't go in)
    }

    void use(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Bronze.BRONZE_KETTLE.get())) {
            kettle = stack.consumeAndReturn(1, player);
            level.setBlock(worldPosition, getBlockState().setValue(TripodBlock.KETTLE, true), Block.UPDATE_ALL);
            level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
            sync();
        } else if (isWater(stack)) {
            if (water || done) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.kettle.has_water"));
                return;
            }
            water = true;
            if (!player.hasInfiniteMaterials()) {
                ItemStack empty = stack.is(Items.WATER_BUCKET) ? new ItemStack(Items.BUCKET) : ClayBucketItem.emptied(stack);
                if (empty.isEmpty()) player.playSound(SoundEvents.DECORATED_POT_SHATTER, 0.8F, 1.1F);
                player.setItemInHand(hand, empty);
            }
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            sync();
        } else if (stack.is(Items.BOWL)) {
            ladle(level, player, stack);
        } else {
            addIngredient(level, player, stack);
        }
    }

    private void ladle(ServerLevel level, Player player, ItemStack bowl) {
        if (portions <= 0) {
            player.sendOverlayMessage(Component.translatable(done ? "message.mechanical.kettle.nothing_to_ladle" : "message.mechanical.kettle.not_done"));
            return;
        }
        bowl.consume(1, player);
        ItemStack out = portion.copy();
        player.getInventory().placeItemBackInInventory(out, Prediction.SERVER_ONLY);
        if (out.is(Kettle.STEW.get()) && player instanceof ServerPlayer sp) {
            List<Item> items = out.get(Kettle.INGREDIENTS.get());
            if (items != null && items.size() >= Kettle.HEARTY) Kettle.award(sp, Kettle.HEARTY_STEW_ADVANCEMENT);
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 0.8F);
        if (--portions <= 0 && outputs.isEmpty()) reset();
        sync();
    }

    private void addIngredient(ServerLevel level, Player player, ItemStack stack) {
        Kettle.Mode m = Kettle.modeOf(level, stack);
        if (m == null) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.kettle.cant_go_in", stack.getHoverName()));
            return;
        }
        if (mode != null && m != mode) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.kettle.wrong_mode", stack.getHoverName(), modeName(mode)));
            return;
        }
        int cap = Kettle.capacity(kettle);
        if (contents.size() >= cap) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.kettle.full", cap));
            return;
        }
        if (Preserving.tooFarGone(stack, level, player)) return;
        contents.add(stack.consumeAndReturn(1, player));
        mode = m;
        progress = 0; // whatever went in last needs its full time
        colour = Kettle.colour(mode, contents);
        level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.4F);
        sync();
    }

    void useEmptyHand(ServerLevel level, Player player) {
        if (kettle.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.tripod.empty"));
            return;
        }
        if (player.isSecondaryUseActive()) {
            if (water || !contents.isEmpty() || done) {
                level.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.1, worldPosition.getZ() + 0.5, 20, 0.3, 0.1, 0.3, 0.1);
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
                player.sendOverlayMessage(Component.translatable("message.mechanical.kettle.poured_out"));
                reset();
            } else {
                player.getInventory().placeItemBackInInventory(kettle, Prediction.SERVER_ONLY);
                kettle = ItemStack.EMPTY;
                heat = 0;
                level.setBlock(worldPosition, getBlockState().setValue(TripodBlock.KETTLE, false).setValue(TripodBlock.HOT, false), Block.UPDATE_ALL);
                level.playSound(null, worldPosition, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            sync();
            return;
        }
        if (!outputs.isEmpty()) {
            boolean leather = outputs.stream().anyMatch(s -> s.is(Items.LEATHER));
            for (ItemStack s : outputs) player.getInventory().placeItemBackInInventory(s, Prediction.SERVER_ONLY);
            outputs.clear();
            if (leather && player instanceof ServerPlayer sp) Kettle.award(sp, Kettle.TANNED_ADVANCEMENT);
            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.2F);
            if (portions <= 0) reset();
            sync();
            return;
        }
        player.sendSystemMessage(status(level));
    }

    /** One line: what is in the pot, how hot it is and how long it has left. */
    private Component status(ServerLevel level) {
        int cap = Kettle.capacity(kettle);
        if (!water) return Component.translatable("message.mechanical.kettle.status.dry", cap);
        if (done) return portions > 0 ? Component.translatable("message.mechanical.kettle.status.ladle", portions, portion.getHoverName())
                : Component.translatable("message.mechanical.kettle.status.dry", cap);
        MutableComponent line = Component.translatable("message.mechanical.kettle.status.mode", mode == null ? Component.translatable("kettle.mechanical.mode.water")
                : modeName(mode), contents.size(), cap);
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemStack s : contents) counts.merge(s.getItem(), 1, Integer::sum);
        List<Component> names = new ArrayList<>();
        counts.forEach((item, n) -> names.add(n > 1 ? Component.translatable("tooltip.mechanical.stew.many", Component.translatable(item.getDescriptionId()), n)
                : Component.translatable(item.getDescriptionId())));
        if (!names.isEmpty()) line.append(Component.literal(": ")).append(net.minecraft.network.chat.ComponentUtils.formatList(names, Component.literal(", ")));
        float rate = fireRate(level.getBlockState(worldPosition.below()));
        String temp = heat >= Kettle.BOIL ? "boiling" : rate >= 1 ? "heating" : rate > 0 ? "warm" : "cold";
        line.append(Component.literal(" | ")).append(Component.translatable("message.mechanical.kettle.status." + temp, (int) heat));
        if (mode != null && rate >= 1) {
            int seconds = (int) Math.ceil((Kettle.cookTime(mode) - progress) / rate / 20);
            line.append(Component.literal(" | ")).append(Component.translatable("message.mechanical.kettle.status.left", seconds / 60, String.format("%02d", seconds % 60)));
        }
        return line;
    }

    private static Component modeName(Kettle.Mode mode) {
        return Component.translatable("kettle.mechanical.mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** Back to an empty kettle (keeps its heat). */
    private void reset() {
        water = false;
        contents.clear();
        outputs.clear();
        portion = ItemStack.EMPTY;
        portions = maxPortions = 0;
        done = false;
        progress = 0;
        mode = null;
        colour = Kettle.colour(null, contents);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** Broken: the kettle comes back (the contents spill). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !kettle.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, kettle);
    }

    // ---- saving / syncing ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("kettle", ItemStack.CODEC, kettle.isEmpty() ? null : kettle);
        output.putBoolean("water", water);
        output.store("contents", ItemStack.CODEC.listOf(), List.copyOf(contents));
        output.putFloat("heat", heat);
        output.putFloat("progress", progress);
        output.putBoolean("done", done);
        output.store("outputs", ItemStack.CODEC.listOf(), List.copyOf(outputs));
        output.storeNullable("portion", ItemStack.CODEC, portion.isEmpty() ? null : portion);
        output.putInt("portions", portions);
        output.putInt("max_portions", maxPortions);
        output.putInt("colour", colour);
        if (mode != null) output.putString("mode", mode.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        kettle = input.read("kettle", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        water = input.getBooleanOr("water", false);
        contents.clear();
        input.read("contents", ItemStack.CODEC.listOf()).ifPresent(contents::addAll);
        heat = input.getFloatOr("heat", 0);
        progress = input.getFloatOr("progress", 0);
        done = input.getBooleanOr("done", false);
        outputs.clear();
        input.read("outputs", ItemStack.CODEC.listOf()).ifPresent(outputs::addAll);
        portion = input.read("portion", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        portions = input.getIntOr("portions", 0);
        maxPortions = input.getIntOr("max_portions", 0);
        colour = input.getIntOr("colour", Kettle.colour(null, List.of()));
        String m = input.getStringOr("mode", "");
        mode = m.isEmpty() ? null : Kettle.Mode.valueOf(m);
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
