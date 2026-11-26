package com.mechanical.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.mechanical.ArmorStationRecipes;
import com.mechanical.FletchingRecipes;
import com.mechanical.LeatherworkingRecipes;
import com.mechanical.Mechanical;
import com.mechanical.MoldBench;
import com.mechanical.PatternBenchBlock;
import com.mechanical.StationRecipe;
import com.mechanical.ToolStationRecipes;
import com.mechanical.equipment.Equipment;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The items a player has ever carried (item ids): what the recipe viewer treats as discovered. Saved, synced, kept on death. */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class SeenItems {
    public static final Supplier<AttachmentType<List<String>>> SEEN = Mechanical.ATTACHMENTS.register("seen_items", () -> AttachmentType
            .<List<String>>builder(() -> List.of())
            .serialize(Codec.STRING.listOf().fieldOf("items"))
            .sync((holder, to) -> holder == to, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
            .copyOnDeath()
            .build());
    /** Set once old players have been given what they unlocked before the viewer existed. */
    public static final Supplier<AttachmentType<Boolean>> BACKFILLED = Mechanical.ATTACHMENTS.register("seen_items_backfilled", () -> AttachmentType
            .<Boolean>builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("done"))
            .copyOnDeath()
            .build());

    private SeenItems() {}

    /** Loads the class so the attachments register. */
    public static void init() {}

    public static Set<String> seen(Player p) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(p.getData(SEEN.get())));
    }

    /** Adds everything carried or worn right now. */
    public static void scan(Player p) {
        Set<String> found = new LinkedHashSet<>();
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) add(found, s);
        for (EquipmentSlot slot : EquipmentSlot.values()) add(found, p.getItemBySlot(slot));
        for (ItemStack s : Equipment.of(p).getItems()) add(found, s);
        merge(p, found);
    }

    /** Once per player: scan, the Ender Chest, and the key part (never the result) of each tool they had unlocked. */
    public static void backfill(ServerPlayer p) {
        if (p.getData(BACKFILLED.get())) return;
        p.setData(BACKFILLED.get(), true);
        scan(p);
        Set<String> found = new LinkedHashSet<>();
        for (ItemStack s : p.getEnderChestInventory().getItems()) add(found, s);
        List<String> unlocked = p.getData(Mechanical.UNLOCKED_TOOLS);
        for (ToolStationRecipes.Recipe r : ToolStationRecipes.ALL) {
            if (unlocked.contains(r.id())) found.add(id(r.head().get()));
        }
        for (List<StationRecipe> list : List.of(FletchingRecipes.ALL, ArmorStationRecipes.ALL, LeatherworkingRecipes.ALL, MoldBench.ALL, PatternBenchBlock.RECIPES)) {
            for (StationRecipe r : list) {
                if (unlocked.contains(r.id())) found.add(id(r.parts().get(0).item().get()));
            }
        }
        merge(p, found);
    }

    private static void add(Set<String> into, ItemStack s) {
        if (!s.isEmpty()) into.add(id(s.getItem()));
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static void merge(Player p, Set<String> found) {
        List<String> now = p.getData(SEEN.get());
        if (now.containsAll(found)) return;
        Set<String> all = new LinkedHashSet<>(now);
        all.addAll(found);
        p.setData(SEEN.get(), new ArrayList<>(all));
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 5) scan(p);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) backfill(p);
    }
}
