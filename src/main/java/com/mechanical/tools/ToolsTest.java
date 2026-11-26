package com.mechanical.tools;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Bronze;
import com.mechanical.Mechanical;
import com.mechanical.Smithing;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game tests for the dynamic tools. */
@EventBusSubscriber(modid = MODID)
public final class ToolsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tools"));

    static {
        TEST_FUNCTIONS.register("tools", () -> ToolsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "tools"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tools"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            rodsExist(h);
            castRods(h);
            defaultsUnchanged(h);
            steelRodLonger(h);
            smithingBonus(h);
            poorOnce(h);
            station(h);
            bowsAndRods(h);
            crossbows(h);
            typePockets(h);
            castBoltHeads(h);
            looksFollowParts(h);
            h.succeed();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    /** Each rod is registered and made: bone by a crafting recipe, steel by the anvil's Tool Rod pattern, copper and bronze cast ({@link #castRods}). */
    private static void rodsExist(GameTestHelper h) {
        var level = h.getLevel();
        var input = CraftingInput.of(1, 3, List.of(new ItemStack(com.mechanical.Mechanical.IRON_KNIFE.get()), new ItemStack(Items.BONE), new ItemStack(Items.BONE)));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow(() -> h.assertionException("no bone rod recipe"));
        h.assertTrue(recipe.value().assemble(input).is(Smithing.BONE_TOOL_ROD.get()), "recipe makes the bone rod");
        h.assertTrue(recipe.value().getRemainingItems(input).get(0).is(com.mechanical.Mechanical.IRON_KNIFE.get()), "the knife stays");
        h.assertTrue(Smithing.Pattern.TOOL_ROD.result(new ItemStack(Smithing.HOT_STEEL_INGOT.get())).is(Smithing.STEEL_TOOL_ROD.get()), "a hot Steel Ingot forges a Steel Tool Rod");
        // the old 2-ingot crafting recipes are gone: copper and bronze rods are cast
        Item bronzeIngot = Bronze.BRONZE_INGOT.get();
        for (Item ingot : List.of(Items.COPPER_INGOT, bronzeIngot)) {
            var column = CraftingInput.of(1, 2, List.of(new ItemStack(ingot), new ItemStack(ingot)));
            h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, column, level).map(r -> r.value().assemble(column))
                    .filter(out -> out.is(Smithing.COPPER_TOOL_ROD.get()) || out.is(Smithing.BRONZE_TOOL_ROD.get())).isEmpty(), "no 2-ingot rod recipe for " + ingot);
        }
    }

    /** Copper and bronze rods: Mold Bench (4 Clay Balls) -> kiln -> pour a crucible into the placed mold -> take the casting -> Whetstone. */
    private static void castRods(GameTestHelper h) {
        var level = h.getLevel();
        h.assertTrue(com.mechanical.MoldBench.ALL.stream().anyMatch(r -> r.result().get() == com.mechanical.Casting.UNFIRED_CLAY_TOOL_ROD_MOLD.get()
                && r.parts().getFirst().count() == 4), "the Mold Bench shapes the rod mold from 4 Clay Balls");
        h.assertTrue(com.mechanical.KilnBlockEntity.RECIPES.stream().anyMatch(r -> r.input().test(new ItemStack(com.mechanical.Casting.UNFIRED_CLAY_TOOL_ROD_MOLD.get()))
                && r.result().get() == com.mechanical.Casting.CLAY_TOOL_ROD_MOLD_ITEM.get()), "the kiln fires the rod mold");
        var mold = new net.minecraft.core.BlockPos(2, 2, 2);
        h.setBlock(mold.below(), net.minecraft.world.level.block.Blocks.STONE);
        var smith = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (var c : List.of(new Object[] {com.mechanical.Metal.COPPER, com.mechanical.Casting.ROUGH_CAST_COPPER_TOOL_ROD.get(), Smithing.COPPER_TOOL_ROD.get()},
                new Object[] {com.mechanical.Metal.BRONZE, com.mechanical.Casting.ROUGH_CAST_BRONZE_TOOL_ROD.get(), Smithing.BRONZE_TOOL_ROD.get()},
                new Object[] {com.mechanical.Metal.POOR_BRONZE, com.mechanical.Casting.ROUGH_CAST_BRONZE_TOOL_ROD.get(), Smithing.BRONZE_TOOL_ROD.get()})) {
            var metal = (com.mechanical.Metal) c[0];
            h.setBlock(mold, com.mechanical.Casting.CLAY_TOOL_ROD_MOLD.get());
            smith.getInventory().clearContent();
            smith.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    com.mechanical.CrucibleItem.molten(com.mechanical.CrucibleContents.EMPTY.add(metal, 4), level.getGameTime()));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(mold)), net.minecraft.core.Direction.UP, h.absolutePos(mold), false);
            h.getBlockState(mold).useItemOn(smith.getMainHandItem(), level, smith, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            var state = h.getBlockState(mold);
            h.assertTrue(state.getValue(com.mechanical.ClayMoldBlock.FILL) == com.mechanical.ClayMoldBlock.Fill.MOLTEN, metal + " poured into the rod mold");
            h.assertTrue(com.mechanical.CrucibleItem.contents(smith.getMainHandItem()).total() == 2, metal + ": the rod takes 2 units");
            h.setBlock(mold, state.setValue(com.mechanical.ClayMoldBlock.FILL, com.mechanical.ClayMoldBlock.Fill.SOLID)); // set (100 ticks)
            smith.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.getBlockState(mold).useWithoutItem(level, smith, hit);
            ItemStack rough = ItemStack.EMPTY;
            for (int i = 0; i < smith.getInventory().getContainerSize(); i++) if (smith.getInventory().getItem(i).is((Item) c[1])) rough = smith.getInventory().getItem(i);
            h.assertTrue(rough.is((Item) c[1]), metal + " casts " + c[1] + ", got " + rough);
            ItemStack rod = com.mechanical.Casting.ground(rough);
            h.assertTrue(rod.is((Item) c[2]) && Bronze.isPoor(rod) == (metal == com.mechanical.Metal.POOR_BRONZE), metal + " grinds into " + rod);
        }
    }

    /**
     * Every dynamic recipe built from its own parts (smithing 0) keeps the fresh stack's durability, mining speed and attack damage,
     * and a bow's draw time and arrow damage, a fishing rod's bites, as the Tool Station shows them.
     */
    private static void defaultsUnchanged(GameTestHelper h) {
        for (var r : com.mechanical.ToolStationRecipes.ALL) {
            if (!r.dynamic()) continue;
            ItemStack fresh = new ItemStack(r.result().get()), built = fresh.copy();
            r.apply(built, r.head().get(), r.binding().get(), r.rod().get(), 0);
            for (var type : List.of(DataComponents.MAX_DAMAGE, DataComponents.TOOL, DataComponents.ATTRIBUTE_MODIFIERS))
                h.assertTrue(java.util.Objects.equals(fresh.get(type), built.get(type)), r.id() + " keeps " + type + ": " + fresh.get(type) + " vs " + built.get(type));
            h.assertTrue(built.has(ToolParts.TOOL_PARTS.get()), r.id() + " gets tool_parts");
            h.assertTrue(ToolParts.ranged(built).equals(ToolParts.Ranged.NONE) && ToolStats.of(built).equals(ToolStats.of(fresh)),
                    r.id() + " keeps its ranged stats: " + ToolParts.ranged(built) + " " + ToolStats.of(built) + " vs " + ToolStats.of(fresh));
        }
        h.assertTrue(ToolStats.drawTicks(new ItemStack(com.mechanical.BronzeTools.YEW_LONGBOW.get())) == 30 && ToolStats.drawTicks(new ItemStack(Mechanical.CRUDE_BOW.get())) == 20
                && ((com.mechanical.BoneHookRodItem) Mechanical.CRUDE_FISHING_ROD.get()).lureTicks(new ItemStack(Mechanical.CRUDE_FISHING_ROD.get())) == 0, "today's draw and lure");
    }

    /**
     * Bows: a Sinew string on the Crude Bow shoots x1.1 harder, draws in 20 / 0.95 ticks and lasts x1.2; a Cordage string on the Yew Longbow
     * shoots 1 / 1.1. Fishing rods: a String line brings bites 2 s sooner. Each slot takes only its own kind of part.
     */
    private static void bowsAndRods(GameTestHelper h) {
        Item sinew = com.mechanical.wildlife.Wildlife.SINEW.get(), cord = Mechanical.CORDAGE.get(), twine = Mechanical.TWINE.get(), rod = Mechanical.TOOL_ROD.get();
        Item yew = com.mechanical.BronzeTools.YEW_STAVE.get();
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "bows"));
        p.setData(Mechanical.UNLOCKED_TOOLS, com.mechanical.ToolStationRecipes.ALL.stream().map(com.mechanical.ToolStationRecipes.Recipe::id).toList());
        ItemStack bow = result(p, sinew, twine, rod);
        var ranged = ToolParts.ranged(bow);
        h.assertTrue(bow.is(Mechanical.CRUDE_BOW.get()) && Math.abs(ranged.power() - 1.1F) < 1e-5 && ToolStats.drawTicks(bow) == Math.round(20 / 0.95F)
                && Math.abs(bow.getMaxDamage() - 96 * 1.2F) <= 1, "sinew string (Smithing 1 may add one): " + bow + " " + ranged + " " + bow.getMaxDamage());
        h.assertTrue(Math.abs(ToolStats.bowDamage(bow) - 3 * 0.7F * 1.1F) < 1e-4, "sinew string damage x" + ToolStats.bowDamage(bow));
        h.assertTrue(bow.get(ToolParts.TOOL_PARTS.get()).head() == sinew
                && bow.get(DataComponents.CUSTOM_MODEL_DATA).strings().equals(List.of("tool_rod", "twine", "sinew")), "the string is kept and named: " + bow.get(DataComponents.CUSTOM_MODEL_DATA));
        h.assertTrue(ranged.onVanillaCurve(ToolStats.drawTicks(bow), 20) >= 20 && ranged.onVanillaCurve(ToolStats.drawTicks(bow) - 2, 20) < 20, "a full draw takes the bow's draw time");
        ItemStack longbow = result(p, cord, Items.LEATHER, yew);
        h.assertTrue(longbow.is(com.mechanical.BronzeTools.YEW_LONGBOW.get()) && Math.abs(ToolParts.ranged(longbow).power() - 1 / 1.1F) < 1e-5,
                "cordage on the longbow: " + longbow + " " + ToolParts.ranged(longbow));
        ItemStack line = result(p, Items.FLINT, Items.STRING, rod);
        h.assertTrue(line.is(Mechanical.CRUDE_FISHING_ROD.get()) && ((com.mechanical.BoneHookRodItem) line.getItem()).lureTicks(line) == 40
                && Math.abs(line.getMaxDamage() - 24 / 0.85F * 0.9F) <= 1, "string line: " + line + " " + ToolParts.ranged(line) + " " + line.getMaxDamage());
        ItemStack bone = result(p, Mechanical.BONE_FISH_HOOK.get(), sinew, Items.BAMBOO);
        h.assertTrue(bone.is(Mechanical.BONE_HOOK_FISHING_ROD.get()) && Mechanical.BONE_HOOK_FISHING_ROD.get().lureTicks(bone) == 100 + 20, "sinew line, bamboo rod: " + bone);
        // kinds: a fishing line or a stave is no bowstring, a bow stave no fishing rod, the Yew Stave only the longbow's
        h.assertTrue(result(p, Items.LEATHER, twine, rod).isEmpty(), "leather is no bowstring");
        h.assertTrue(result(p, cord, twine, Items.BAMBOO).isEmpty(), "bamboo is no stave");
        var recipes = com.mechanical.ToolStationRecipes.ALL;
        var crude = recipes.stream().filter(r -> r.result().get() == Mechanical.CRUDE_BOW.get()).findFirst().orElseThrow();
        var yewBow = recipes.stream().filter(r -> r.result().get() == com.mechanical.BronzeTools.YEW_LONGBOW.get()).findFirst().orElseThrow();
        h.assertTrue(!crude.options(2).contains(yew) && yewBow.options(2).equals(List.of(yew)) && result(p, cord, twine, yew).is(com.mechanical.BronzeTools.YEW_LONGBOW.get()),
                "the Yew Stave makes only the longbow: " + crude.options(2) + " / " + yewBow.options(2));
        h.assertTrue(result(p, Items.FLINT, Items.LEATHER, Items.BAMBOO).isEmpty() && result(p, Mechanical.BONE_FISH_HOOK.get(), twine, yew).isEmpty(), "no leather line, no yew rod");
        h.assertFalse(result(p, Items.FLINT, cord, Items.BAMBOO).isEmpty(), "a cordage line on bamboo");
    }

    /**
     * Crossbows (docs/superpowers/specs/2026-11-26-crossbows-design.md): Sinew on a Tool Rod stock is vanilla's crossbow exactly; the Steel
     * Crossbow lasts twice as long, charges in 35 ticks and shoots x1.15; a String string and a Stick stock trade durability for a quicker,
     * weaker shot; a crossbow never built at the Tool Station (every one in the world today) is untouched; stocks follow the prod's tier.
     */
    private static void crossbows(GameTestHelper h) {
        Item sinew = com.mechanical.wildlife.Wildlife.SINEW.get(), rod = Mechanical.TOOL_ROD.get();
        Item ironProd = Smithing.IRON_CROSSBOW_PROD.get(), steelProd = Smithing.STEEL_CROSSBOW_PROD.get();
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "crossbows"));
        p.setData(Mechanical.UNLOCKED_TOOLS, com.mechanical.ToolStationRecipes.ALL.stream().map(com.mechanical.ToolStationRecipes.Recipe::id).toList());
        ItemStack old = new ItemStack(Items.CROSSBOW);
        h.assertTrue(old.getMaxDamage() == 465 && ToolStats.drawTicks(old) == 25 && net.minecraft.world.item.CrossbowItem.getChargeDuration(old, p) == 25
                && Smithing.powerFactor(old) == 1 && Math.abs(ToolStats.bowDamage(old) - 3.15F) < 1e-5, "an old crossbow is vanilla's");
        // the station adds the fake player's Smithing bonus (+0.5 % a level) to every durability
        float smith = 1 + ToolParts.SMITHING_BONUS_PER_LEVEL * com.mechanical.skills.Skills.level(p, com.mechanical.skills.Skill.SMITHING);
        ItemStack iron = result(p, ironProd, sinew, rod), steel = result(p, steelProd, sinew, rod);
        h.assertTrue(iron.is(Items.CROSSBOW) && iron.getMaxDamage() == Math.round(465 * smith) && net.minecraft.world.item.CrossbowItem.getChargeDuration(iron, p) == 25
                && Smithing.powerFactor(iron) == 1, "the default Iron Crossbow is vanilla's: " + iron.getMaxDamage() + " " + ToolParts.ranged(iron));
        h.assertTrue(steel.is(Smithing.STEEL_CROSSBOW.get()) && steel.getMaxDamage() == Math.round(930 * smith) && net.minecraft.world.item.CrossbowItem.getChargeDuration(steel, p) == 35
                && ToolStats.drawTicks(steel) == 35 && Math.abs(ToolStats.bowDamage(steel) - 3.15F * 1.15F) < 1e-4,
                "the default Steel Crossbow: 930, 35 ticks, x1.15: " + steel.getMaxDamage() + " " + ToolStats.drawTicks(steel) + " " + ToolStats.bowDamage(steel));
        h.assertTrue(steel.get(DataComponents.CUSTOM_MODEL_DATA).strings().equals(List.of("tool_rod", "sinew")), "the model names stock, then string");
        ItemStack light = result(p, ironProd, Items.STRING, Items.STICK);
        int lightDraw = net.minecraft.world.item.CrossbowItem.getChargeDuration(light, p);
        h.assertTrue(light.is(Items.CROSSBOW) && light.getMaxDamage() == Math.round(465 / 1.2F * 0.9F * 0.8F * smith) && lightDraw == Math.round(25 * 0.95F / 1.05F)
                && Math.abs(Smithing.powerFactor(light) - 1.05F / 1.1F) < 1e-5,
                "String on a Stick: shorter-lived, quicker, softer: " + light.getMaxDamage() + " " + lightDraw + " " + Smithing.powerFactor(light));
        ItemStack heavy = result(p, steelProd, sinew, Smithing.IRON_TOOL_ROD.get());
        h.assertTrue(heavy.getMaxDamage() == Math.round(930 * 1.5F * smith) && net.minecraft.world.item.CrossbowItem.getChargeDuration(heavy, p) == Math.round(35 / 0.96F),
                "an Iron Tool Rod stock: x1.5 durability, a heavier charge: " + heavy.getMaxDamage());
        h.assertTrue(result(p, ironProd, sinew, Smithing.STEEL_TOOL_ROD.get()).isEmpty() && !result(p, steelProd, sinew, Smithing.STEEL_TOOL_ROD.get()).isEmpty(),
                "a steel stock only on the steel prod");
        h.assertTrue(result(p, ironProd, sinew, Items.LEATHER).isEmpty() && result(p, ironProd, Items.LEATHER, rod).isEmpty(), "leather is neither stock nor string");
        var share = ToolStats.share(ToolParts.Kind.CROSSBOW, 1, sinew, Items.STRING);
        h.assertTrue(Math.abs(share.speed() - 0.95F / 1.05F) < 1e-5 && Math.abs(share.damage() - 1.05F / 1.1F) < 1e-5, "the station's share for a String string: " + share);
    }

    /** Every recipe shows in the station: its type has a pocket, and the pockets fit the screen's grid. */
    private static void typePockets(GameTestHelper h) {
        for (var r : com.mechanical.ToolStationRecipes.ALL)
            h.assertTrue(com.mechanical.ToolStationRecipes.TYPES.contains(com.mechanical.ToolStationRecipes.type(r)), r.id() + " has no type pocket: " + com.mechanical.ToolStationRecipes.type(r));
        h.assertTrue(com.mechanical.ToolStationRecipes.TYPES.size() <= com.mechanical.ToolStationRecipes.TYPE_POCKETS, "the type pockets fit the 9x2 grid");
    }

    /** Bronze Bolt Heads: Mold Bench (3 Clay Balls) -> kiln -> pour bronze or Poor Bronze (2 units) -> rough casting -> 4 heads on the Whetstone. */
    private static void castBoltHeads(GameTestHelper h) {
        var level = h.getLevel();
        h.assertTrue(com.mechanical.MoldBench.ALL.stream().anyMatch(r -> r.result().get() == Bronze.UNFIRED_CLAY_BOLT_HEADS_MOLD.get()), "the Mold Bench shapes the bolt heads mold");
        h.assertTrue(com.mechanical.KilnBlockEntity.RECIPES.stream().anyMatch(r -> r.input().test(new ItemStack(Bronze.UNFIRED_CLAY_BOLT_HEADS_MOLD.get()))
                && r.result().get() == Bronze.CLAY_BOLT_HEADS_MOLD_ITEM.get()), "the kiln fires it");
        var mold = new net.minecraft.core.BlockPos(5, 2, 5);
        h.setBlock(mold.below(), net.minecraft.world.level.block.Blocks.STONE);
        var smith = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (var metal : List.of(com.mechanical.Metal.BRONZE, com.mechanical.Metal.POOR_BRONZE)) {
            h.setBlock(mold, Bronze.CLAY_BOLT_HEADS_MOLD.get());
            smith.getInventory().clearContent();
            smith.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    com.mechanical.CrucibleItem.molten(com.mechanical.CrucibleContents.EMPTY.add(metal, 4), level.getGameTime()));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(mold)), net.minecraft.core.Direction.UP, h.absolutePos(mold), false);
            h.getBlockState(mold).useItemOn(smith.getMainHandItem(), level, smith, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            var state = h.getBlockState(mold);
            h.assertTrue(state.getValue(com.mechanical.ClayMoldBlock.FILL) == com.mechanical.ClayMoldBlock.Fill.MOLTEN
                    && com.mechanical.CrucibleItem.contents(smith.getMainHandItem()).total() == 2, metal + " poured, 2 units");
            h.setBlock(mold, state.setValue(com.mechanical.ClayMoldBlock.FILL, com.mechanical.ClayMoldBlock.Fill.SOLID));
            smith.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.getBlockState(mold).useWithoutItem(level, smith, hit);
            ItemStack rough = ItemStack.EMPTY;
            for (int i = 0; i < smith.getInventory().getContainerSize(); i++)
                if (smith.getInventory().getItem(i).is(Bronze.ROUGH_CAST_BRONZE_BOLT_HEADS.get())) rough = smith.getInventory().getItem(i);
            ItemStack heads = com.mechanical.Casting.ground(rough);
            h.assertTrue(heads.is(Bronze.BRONZE_BOLT_HEAD.get()) && heads.getCount() == 4 && Bronze.isPoor(heads) == (metal == com.mechanical.Metal.POOR_BRONZE),
                    metal + " casts bolt heads: " + rough + " -> " + heads);
        }
    }

    /** Every dynamic recipe's tool has the composite item model scripts/gen_tool_parts.py writes (a new recipe needs a row there). */
    private static void looksFollowParts(GameTestHelper h) {
        for (var r : com.mechanical.ToolStationRecipes.ALL) {
            if (!r.dynamic()) continue;
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.result().get());
            try (var in = ToolsTest.class.getResourceAsStream("/assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json")) {
                h.assertTrue(in != null && new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("minecraft:composite"),
                        id + " has a parts-following item model (add it to gen_tool_parts.py TOOLS)");
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** A rod above the head's tier is refused; a Bronze Tool Rod and Leather on a bronze pickaxe give x1.35 x1.1 over Cordage on a Tool Rod. */
    private static void steelRodLonger(GameTestHelper h) {
        Item toolRod = Mechanical.TOOL_ROD.get();
        h.assertFalse(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), toolRod, Items.LEATHER, Smithing.STEEL_TOOL_ROD.get()), "steel rod (4) on a bronze head (2)");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), toolRod, Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get()), "bronze rod on a bronze head");
        h.assertFalse(ToolParts.allowed(Mechanical.STONE_PICKAXE_HEAD.get(), Items.STICK, Mechanical.TWINE.get(), Smithing.BONE_TOOL_ROD.get()), "stone takes only wood");
        h.assertFalse(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), toolRod, Mechanical.CORDAGE.get(), Items.LEATHER), "no Leather rod on a haft");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_SWORD_BLADE.get(), Items.LEATHER, Mechanical.CORDAGE.get(), Smithing.BRONZE_TOOL_ROD.get()), "a Bronze Tool Rod grip on a sword");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_SWORD_BLADE.get(), Items.LEATHER, Mechanical.CORDAGE.get(), Items.LEATHER), "the sword's own Leather grip");
        for (Item head : List.of(Items.FLINT, Mechanical.STONE_PICKAXE_HEAD.get()))
            h.assertTrue(java.util.Set.copyOf(ToolParts.rods(head, toolRod)).equals(java.util.Set.of(Items.STICK, toolRod)), "wood only on " + head + ": " + ToolParts.rods(head, toolRod));
        ItemStack pick = bronzePick();
        int expected = Math.round(460 * 1.35F * 1.1F);
        h.assertTrue(Math.abs(pick.getMaxDamage() - expected) <= 1, "bronze rod durability " + pick.getMaxDamage() + " vs " + expected);
        double damage = pick.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ATTACK_DAMAGE, 0, EquipmentSlot.MAINHAND);
        double base = new ItemStack(Bronze.BRONZE_PICKAXE.get()).get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ATTACK_DAMAGE, 0, EquipmentSlot.MAINHAND);
        h.assertTrue(Math.abs(damage - base - 0.5) < 1e-6, "bronze rod +0.5 damage: " + base + " -> " + damage);
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var baseTool = new ItemStack(Bronze.BRONZE_PICKAXE.get()).get(DataComponents.TOOL);
        float speed = pick.get(DataComponents.TOOL).getMiningSpeed(stone), baseSpeed = baseTool.getMiningSpeed(stone);
        h.assertTrue(Math.abs(speed - baseSpeed * 1.03F * 0.98F) < 1e-4, "stone speed x1.03 x0.98: " + baseSpeed + " -> " + speed);
        h.assertTrue(pick.get(DataComponents.TOOL).defaultMiningSpeed() == baseTool.defaultMiningSpeed(), "bare-hand default speed unchanged");
        try {
            ToolParts.apply(pick.copy(), Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), new ToolParts.Parts(Items.DIRT, Mechanical.TOOL_ROD.get(), 0));
            throw h.assertionException("dirt binding accepted");
        } catch (IllegalArgumentException refused) {}
    }

    /** Smithing 40 gives x1.2 durability. */
    private static void smithingBonus(GameTestHelper h) {
        ItemStack pick = new ItemStack(Bronze.BRONZE_PICKAXE.get());
        ToolParts.apply(pick, Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), new ToolParts.Parts(Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), 40));
        h.assertTrue(pick.getMaxDamage() == 552, "smithing 40: " + pick.getMaxDamage());
    }

    /** Poor Bronze takes 70 % once, whether marked before apply or after (as Bronze.inheritPoor does). */
    private static void poorOnce(GameTestHelper h) {
        int good = bronzePick().getMaxDamage(), expected = Math.round(good * Bronze.POOR_DURABILITY);
        ItemStack before = new ItemStack(Bronze.BRONZE_PICKAXE.get());
        Bronze.markPoor(before);
        applyBronzeRod(before);
        Bronze.markPoor(before);
        ItemStack after = bronzePick();
        Bronze.markPoor(after);
        h.assertTrue(before.getMaxDamage() == expected && after.getMaxDamage() == expected, "poor: " + before.getMaxDamage() + " / " + after.getMaxDamage() + " vs " + expected);
    }

    /** The Tool Station builds from any allowed binding and rod, refuses a rod above the head's tier, and never stacks different builds. */
    private static void station(GameTestHelper h) {
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tools"));
        p.setData(Mechanical.UNLOCKED_TOOLS, com.mechanical.ToolStationRecipes.ALL.stream().map(com.mechanical.ToolStationRecipes.Recipe::id).toList());
        ItemStack bronze = result(p, Bronze.BRONZE_PICKAXE_HEAD.get(), Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get());
        var parts = bronze.get(ToolParts.TOOL_PARTS.get());
        h.assertTrue(bronze.is(Bronze.BRONZE_PICKAXE.get()) && parts != null && parts.binding() == Items.LEATHER && parts.rod() == Smithing.BRONZE_TOOL_ROD.get(),
                "anyAllowedBinding: " + bronze + " " + parts);
        h.assertTrue(result(p, Items.FLINT, Mechanical.CORDAGE.get(), Smithing.BONE_TOOL_ROD.get()).isEmpty(), "tierBlocks: bone rod on flint (tier 1 > 0)");
        ItemStack plain = result(p, Bronze.BRONZE_PICKAXE_HEAD.get(), Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get());
        h.assertFalse(ItemStack.isSameItemSameComponents(plain, bronze), "noMerge");
        Item cord = Mechanical.CORDAGE.get(), twine = Mechanical.TWINE.get(), rod = Mechanical.TOOL_ROD.get(), plate = Bronze.BRONZE_PLATE.get();
        Item hammer = com.mechanical.Ironworking.BRONZE_HAMMER_HEAD.get();
        shares(h, p, -1, Items.FLINT, twine, Items.STICK, com.mechanical.Farming.FLINT_SICKLE.get());
        shares(h, p, -1, Items.FLINT, cord, rod, Mechanical.FLINT_SPEAR.get());
        shares(h, p, -1, Items.FLINT, twine, rod, Mechanical.CRUDE_FISHING_ROD.get());
        shares(h, p, -1, hammer, plate, rod, com.mechanical.BronzeTools.BRONZE_QUARRY_HAMMER.get());
        shares(h, p, -1, hammer, cord, rod, com.mechanical.Ironworking.BRONZE_SMITHING_HAMMER.get());
        shares(h, p, -1, Bronze.BRONZE_HOE_HEAD.get(), plate, rod, com.mechanical.FarmTools.BRONZE_ARD.get());
        shares(h, p, Items.IRON_PICKAXE, Smithing.IRON_PICKAXE_HEAD.get(), Items.LEATHER, rod, Items.IRON_PICKAXE);
        shares(h, p, Bronze.BRONZE_PICKAXE.get(), Smithing.IRON_PICKAXE_HEAD.get(), Items.LEATHER, rod, Items.IRON_PICKAXE);
        shares(h, p, Smithing.STEEL_SMITHING_HAMMER.get(), Smithing.STEEL_HAMMER_HEAD.get(), Smithing.IRON_TOOL_ROD.get(), rod, Smithing.STEEL_SMITHING_HAMMER.get());
        shares(h, p, Mechanical.CRUDE_FISHING_ROD.get(), Items.FLINT, Items.LEATHER, rod, Mechanical.FLINT_SPEAR.get()); // selection no longer fits: best fit
        shares(h, p, Mechanical.CRUDE_FISHING_ROD.get(), Items.FLINT, cord, rod, Mechanical.CRUDE_FISHING_ROD.get()); // a cordage line: the selection fits
        shares(h, p, Smithing.STEEL_QUARRY_HAMMER.get(), Smithing.STEEL_HAMMER_HEAD.get(), Smithing.IRON_TOOL_ROD.get(), rod, Smithing.STEEL_QUARRY_HAMMER.get());
        h.assertTrue(ToolParts.bindings(Items.FLINT).get(0) == Mechanical.CORDAGE.get(), "fixed order: tier, then id");
    }

    private static void shares(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, int ignored, Item head, Item binding, Item rod, Item expected) {
        shares(h, p, (Item) null, head, binding, rod, expected);
    }

    /** selected: the recipe clicked in the screen (null = nothing selected); expected null = no result. */
    private static void shares(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, Item selected, Item head, Item binding, Item rod, Item expected) {
        var menu = new com.mechanical.ToolStationMenu(1, p.getInventory());
        if (selected != null) {
            int i = 0;
            while (com.mechanical.ToolStationRecipes.ALL.get(i).result().get() != selected) i++;
            menu.clickMenuButton(p, i);
        }
        menu.slots.get(0).set(new ItemStack(head));
        menu.slots.get(1).set(new ItemStack(binding));
        menu.slots.get(2).set(new ItemStack(rod));
        ItemStack got = menu.slots.get(3).getItem();
        h.assertTrue(expected == null ? got.isEmpty() : got.is(expected), "shared head: " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(head) + " + "
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(binding) + " gave " + got + " not " + expected);
    }

    private static ItemStack result(net.minecraft.server.level.ServerPlayer p, Item head, Item binding, Item rod) {
        var menu = new com.mechanical.ToolStationMenu(1, p.getInventory());
        menu.slots.get(0).set(new ItemStack(head));
        menu.slots.get(1).set(new ItemStack(binding));
        menu.slots.get(2).set(new ItemStack(rod));
        return menu.slots.get(3).getItem().copy();
    }

    private static ItemStack bronzePick() {
        ItemStack pick = new ItemStack(Bronze.BRONZE_PICKAXE.get());
        applyBronzeRod(pick);
        if (!pick.get(DataComponents.CUSTOM_MODEL_DATA).strings().equals(List.of("bronze_tool_rod", "leather")))
            throw new IllegalStateException("the model strings name the rod, then the binding: " + pick.get(DataComponents.CUSTOM_MODEL_DATA));
        return pick;
    }

    private static void applyBronzeRod(ItemStack pick) {
        ToolParts.apply(pick, Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), new ToolParts.Parts(Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get(), 0));
    }

    private ToolsTest() {}
}
