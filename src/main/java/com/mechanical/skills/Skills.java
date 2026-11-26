package com.mechanical.skills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Twelve skills, levels 1..100 from XP. Total XP for level L is 25(L-1)^2 + 75(L-1). */
public final class Skills {
    public static final int MAX_LEVEL = 100;
    /** Skill key to total XP. */
    public static final Supplier<AttachmentType<Map<String, Integer>>> XP = Mechanical.ATTACHMENTS.register("skill_xp", () -> AttachmentType
            .<Map<String, Integer>>builder(() -> Map.of())
            .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("xp"))
            .sync((holder, to) -> holder == to, ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT))
            .copyOnDeath()
            .build());

    private Skills() {}

    /** Loads the class so the attachment registers. */
    public static void init() {}

    public static int xpForLevel(int level) {
        int n = level - 1;
        return 25 * n * n + 75 * n;
    }

    public static int level(int xp) {
        int l = 1;
        while (l < MAX_LEVEL && xp >= xpForLevel(l + 1)) l++;
        return l;
    }

    public static int xp(Player p, Skill s) {
        return p.getData(XP.get()).getOrDefault(s.key(), 0);
    }

    public static int level(Player p, Skill s) {
        return level(xp(p, s));
    }

    /** Jewelry bonus; Task 4 wires it. */
    public static int bonus(Player p, Skill s) {
        return 0;
    }

    public static int effectiveLevel(Player p, Skill s) {
        return Math.min(120, level(p, s) + bonus(p, s));
    }

    public static void addXp(ServerPlayer p, Skill s, int amount) {
        if (amount <= 0) return;
        int before = level(p, s);
        Map<String, Integer> map = new HashMap<>(p.getData(XP.get()));
        map.merge(s.key(), amount, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
        p.setData(XP.get(), map);
        int after = level(p, s);
        // FakePlayers have no connection
        if (!(p instanceof FakePlayer)) PacketDistributor.sendToPlayer(p, new SkillPayload(s, amount, after > before ? after : 0));
    }

    /** Cooking XP for food a player takes out of a cooker: 5 per item; non-food and burnt food give nothing. */
    public static void cooked(Player p, ItemStack food) {
        if (!(p instanceof ServerPlayer sp) || !food.has(DataComponents.FOOD) || food.is(Mechanical.BURNT_FOOD.get()) || food.is(Items.ROTTEN_FLESH)) return;
        addXp(sp, Skill.COOKING, 5 * food.getCount());
        if (SkillPerks.COOK_EXTRA.roll(sp)) SkillPerks.give(sp, SkillPerks.extra(List.of(food)));
    }

    /** False below the unlock level; otherwise a chance of effective level times perLevel. */
    public static boolean roll(ServerPlayer p, Skill s, int unlockLevel, double perLevel) {
        int lvl = effectiveLevel(p, s);
        return lvl >= unlockLevel && p.getRandom().nextDouble() < lvl * perLevel;
    }

    /** Perks of a skill in unlock order, for the skill screen. */
    public static List<Perk> perks(Skill s) {
        return SkillPerks.ALL.stream().filter(p -> p.skill() == s).toList();
    }

    /**
     * Unlocks at {@code unlockLevel}; its chance (or bonus fraction) is effective level x {@code perLevel}, or {@code flat}
     * when that is set. Text {@code langKey} takes the current percent.
     */
    public record Perk(Skill skill, int unlockLevel, String langKey, double perLevel, double flat) {
        public boolean unlocked(Player p) {
            return effectiveLevel(p, skill) >= unlockLevel;
        }

        /** The chance or bonus now, 0 while locked. */
        public double value(Player p) {
            int lvl = effectiveLevel(p, skill);
            return lvl < unlockLevel ? 0 : flat > 0 ? flat : lvl * perLevel;
        }

        /** Rolls on the server with the level's random; never on the client. */
        public boolean roll(Player p) {
            return p instanceof ServerPlayer && p.level().getRandom().nextDouble() < value(p);
        }
    }
}
