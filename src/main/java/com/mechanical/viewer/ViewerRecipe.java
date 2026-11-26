package com.mechanical.viewer;

import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A recipe in the viewer's own shape, whatever station it comes from. {@code width} is the shaped grid width (0 otherwise). */
public record ViewerRecipe(Station station, List<Slot> inputs, ItemStack output, int width, UnlockKind unlock, String unlockId) {
    private static final StreamCodec<RegistryFriendlyByteBuf, Slot> SLOT = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()), Slot::options,
            ByteBufCodecs.VAR_INT, Slot::count,
            ByteBufCodecs.BOOL, Slot::tool,
            Slot::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, ViewerRecipe> CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Station.values()[i], Station::ordinal), ViewerRecipe::station,
            SLOT.apply(ByteBufCodecs.list()), ViewerRecipe::inputs,
            ItemStack.STREAM_CODEC, ViewerRecipe::output,
            ByteBufCodecs.VAR_INT, ViewerRecipe::width,
            ByteBufCodecs.idMapper(i -> UnlockKind.values()[i], UnlockKind::ordinal), ViewerRecipe::unlock,
            ByteBufCodecs.STRING_UTF8, ViewerRecipe::unlockId,
            ViewerRecipe::new);

    public boolean unlocked(Set<String> seen, List<String> unlockedTools) {
        return switch (unlock) {
            case ALWAYS -> true;
            case RECIPE_ID -> unlockedTools.contains(unlockId);
            case ALL_SEEN -> inputs.stream().allMatch(s -> s.options().isEmpty() || anySeen(s, seen));
            case ANY_SEEN -> !inputs.isEmpty() && anySeen(inputs.get(0), seen);
            // a station recipe with no unlock: the station item itself must be seen too, or every age's recipes would show at once
            case STATION_SEEN -> seen.contains(unlockId) && inputs.stream().allMatch(s -> s.options().isEmpty() || anySeen(s, seen));
        };
    }

    private static boolean anySeen(Slot s, Set<String> seen) {
        return s.options().stream().anyMatch(i -> seen.contains(BuiltInRegistries.ITEM.getKey(i).toString()));
    }

    public boolean makes(Item i) {
        return output.is(i);
    }

    public boolean uses(Item i) {
        return inputs.stream().anyMatch(s -> s.options().contains(i));
    }
}
