package com.mechanical.skills;

import static com.mechanical.Mechanical.MODID;

import java.util.List;

import com.mechanical.Butchery;
import com.mechanical.skills.Skills.Perk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** The perk table; the crafts roll them where they happen, speed and damage are events here. */
@EventBusSubscriber(modid = MODID)
public final class SkillPerks {
    public static final Perk MINING_ORE = perk(Skill.MINING, 1, 10, 0.004), MINING_SPEED = perk(Skill.MINING, 2, 30, 0.003),
            MINING_KEEP = flat(Skill.MINING, 3, 60, 0.25),
            WOOD_LOG = perk(Skill.WOODCUTTING, 1, 10, 0.004), WOOD_SPEED = perk(Skill.WOODCUTTING, 2, 30, 0.003),
            WOOD_KEEP = flat(Skill.WOODCUTTING, 3, 60, 0.25),
            FARM_HARVEST = perk(Skill.FARMING, 1, 10, 0.004), FARM_GROW = perk(Skill.FARMING, 2, 40, 0.002),
            FISH_EXTRA = perk(Skill.FISHING, 1, 10, 0.003), FISH_TRAP = perk(Skill.FISHING, 2, 50, 0.003),
            SLAUGHTER_MEAT = perk(Skill.SLAUGHTER, 1, 10, 0.004), SLAUGHTER_HIDE = perk(Skill.SLAUGHTER, 2, 50, 0.002),
            HUNT_DAMAGE = perk(Skill.HUNTING, 1, 10, 0.002), HUNT_ARROW = perk(Skill.HUNTING, 2, 40, 0.003),
            SMITH_HAMMER = perk(Skill.SMITHING, 1, 10, 0.003), SMITH_INGOT = perk(Skill.SMITHING, 2, 50, 0.001),
            GOLD_FLAWLESS = perk(Skill.GOLD_SMITHING, 1, 10, 0.002), GOLD_CRACK = perk(Skill.GOLD_SMITHING, 2, 30, 0.003),
            COOK_EXTRA = perk(Skill.COOKING, 1, 10, 0.003),
            MASON_EXTRA = perk(Skill.MASONRY, 1, 10, 0.004),
            LEATHER_SAVE = perk(Skill.LEATHERWORKING, 1, 10, 0.003),
            FLETCH_EXTRA = perk(Skill.FLETCHING, 1, 10, 0.004);
    static final List<Perk> ALL = List.of(MINING_ORE, MINING_SPEED, MINING_KEEP, WOOD_LOG, WOOD_SPEED, WOOD_KEEP, FARM_HARVEST, FARM_GROW,
            FISH_EXTRA, FISH_TRAP, SLAUGHTER_MEAT, SLAUGHTER_HIDE, HUNT_DAMAGE, HUNT_ARROW, SMITH_HAMMER, SMITH_INGOT, GOLD_FLAWLESS,
            GOLD_CRACK, COOK_EXTRA, MASON_EXTRA, LEATHER_SAVE, FLETCH_EXTRA);

    private SkillPerks() {}

    private static Perk perk(Skill s, int n, int unlock, double perLevel) {
        return new Perk(s, unlock, "skill." + MODID + ".perk." + s.key() + "." + n, perLevel, 0);
    }

    private static Perk flat(Skill s, int n, int unlock, double chance) {
        return new Perk(s, unlock, "skill." + MODID + ".perk." + s.key() + "." + n, 0, chance);
    }

    /** One more of the first stackable drop (count 1); empty if none, so tools and other single items are never copied. */
    public static ItemStack extra(List<ItemStack> drops) {
        return drops.stream().filter(s -> !s.isEmpty() && s.isStackable()).findFirst().map(s -> s.copyWithCount(1)).orElse(ItemStack.EMPTY);
    }

    /** An extra drop added to a block's drops. */
    public static void addExtra(BlockDropsEvent event) {
        ItemStack extra = extra(event.getDrops().stream().map(ItemEntity::getItem).toList());
        if (extra.isEmpty()) return;
        ItemEntity first = event.getDrops().getFirst();
        event.getDrops().add(new ItemEntity(event.getLevel(), first.getX(), first.getY(), first.getZ(), extra));
    }

    /** Into the inventory, or dropped if full. */
    public static void give(Player p, ItemStack stack) {
        if (!stack.isEmpty()) p.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
    }

    /** A pickaxe or axe that skips this wear (Mining / Woodcutting L60). */
    public static boolean keepsTool(Player p, ItemStack tool) {
        return tool.is(ItemTags.PICKAXES) ? MINING_KEEP.roll(p) : tool.is(ItemTags.AXES) && WOOD_KEEP.roll(p);
    }

    // Both sides, so client-predicted mining progress matches the server.
    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        Perk perk = state.is(BlockTags.LOGS) ? WOOD_SPEED : SkillHooks.paysMining(state) ? MINING_SPEED : null;
        if (perk != null) event.setNewSpeed(event.getNewSpeed() * (float) (1 + perk.value(event.getEntity())));
    }

    @SubscribeEvent
    static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && Butchery.leavesCarcass(event.getEntity()))
            event.setAmount(event.getAmount() * (float) (1 + HUNT_DAMAGE.value(p)));
    }
}
