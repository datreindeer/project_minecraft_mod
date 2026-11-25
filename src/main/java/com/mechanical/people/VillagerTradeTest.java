package com.mechanical.people;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.Coins;
import com.mechanical.TestArea;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * villager_trade: a villager's trade through our screen's packet ({@link VillagerTrade}) over vanilla's MerchantMenu:
 * forged containers and offer numbers do nothing; two inputs are both needed and taken; a cost that asks for components
 * gets only the right stack, and an enchanted result keeps its enchantment; stock runs out and comes back with a restock;
 * Shift trades until the stock is gone; no room, no trade and nothing taken; the villager gains experience and levels up
 * with new offers; Hero of the Village lowers the price that is really taken; out of reach nothing happens.
 */
@EventBusSubscriber(modid = MODID)
public final class VillagerTradeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String NAME = "villager_trade";

    static {
        TEST_FUNCTIONS.register(NAME, () -> VillagerTradeTest::test);
    }

    static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, NAME), new TestEnvironmentDefinition.AllOf(List.of()));
        ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, NAME));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, NAME), new FunctionGameTestInstance(key, TestArea.of(env, 100, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException(NAME + ": " + what);
    }

    /** A survival ServerPlayer whose packets go nowhere. */
    private static ServerPlayer player(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "trade-test"), ClientInformation.createDefault());
        p.connection = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "trade-test-net")).connection;
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5, 1, 4)));
        return p;
    }

    private static MerchantMenu open(GameTestHelper h, Villager villager, ServerPlayer p) {
        villager.mobInteract(p, InteractionHand.MAIN_HAND);
        check(h, p.containerMenu instanceof MerchantMenu, "right-clicking the villager opens a merchant menu");
        return (MerchantMenu) p.containerMenu;
    }

    private static int count(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Villager villager = h.spawnWithNoFreeWill(EntityTypes.VILLAGER, 4, 1, 4);
        villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.FARMER));
        villager.setVillagerXp(1);
        var mending = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(mending, 1));
        ItemCost deed = new ItemCost(Items.PAPER).withComponents(b -> b.expect(DataComponents.CUSTOM_NAME, Component.literal("Deed")));
        Item coin = Coins.COPPER_COIN.get();
        MerchantOffers offers = villager.getOffers();
        offers.clear();
        offers.add(new MerchantOffer(new ItemCost(Items.WHEAT, 4), Optional.of(new ItemCost(coin, 2)), new ItemStack(Items.BREAD, 3), 2, 3, 0.05F));
        offers.add(new MerchantOffer(deed, book.copy(), 4, 1, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.CARROT, 5), new ItemStack(coin), 3, 1, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.FLINT, 10), new ItemStack(Items.APPLE), 8, 0, 0.05F));

        ServerPlayer p = player(h);
        Inventory inv = p.getInventory();
        MerchantMenu menu = open(h, villager, p);
        int id = menu.containerId;
        inv.add(new ItemStack(Items.WHEAT, 20));

        // forged: another container, no such offer; two inputs: wheat alone is not enough
        check(h, VillagerTrade.trade(p, new VillagerTrade(id + 1, 0, false)) == 0, "another container's id does nothing");
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, -1, false)) == 0 && VillagerTrade.trade(p, new VillagerTrade(id, 99, true)) == 0,
                "no such offer does nothing");
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 0, false)) == 0 && count(p, Items.WHEAT) == 20, "without the second input nothing is taken");

        inv.add(new ItemStack(coin, 10));
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 0, false)) == 1, "bread for wheat and coins");
        check(h, count(p, Items.BREAD) == 3 && count(p, Items.WHEAT) == 16 && count(p, coin) == 8, "both inputs paid once, three bread");
        check(h, menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "the rest of the payment is back in the inventory");
        check(h, offers.get(0).getUses() == 1 && villager.getVillagerXp() == 4, "the offer is used and the villager gains experience");
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 0, true)) == 1 && offers.get(0).isOutOfStock(), "stock of two runs out");
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 0, false)) == 0 && count(p, Items.WHEAT) == 12 && count(p, coin) == 6, "sold out takes nothing");
        villager.restock();
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 0, false)) == 1, "a restock brings the stock back");

        // components: plain paper is not the Deed; the book keeps its enchantment
        inv.add(new ItemStack(Items.PAPER, 3));
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 1, false)) == 0 && count(p, Items.PAPER) == 3, "a cost with components refuses plain paper");
        ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Deed"));
        inv.add(named);
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 1, false)) == 1 && count(p, Items.PAPER) == 3, "the named paper alone is taken");
        check(h, inv.contains(s -> ItemStack.isSameItemSameComponents(s, book)), "the enchanted book arrives with Mending");

        // Shift: until the stock is gone
        inv.add(new ItemStack(Items.CARROT, 64));
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 2, true)) == 3 && count(p, Items.CARROT) == 49, "Shift sells until their stock is full");

        // no room for the result: nothing is taken
        inv.clearContent();
        inv.setItem(0, new ItemStack(Items.FLINT, 64));
        for (int i = 1; i < Inventory.INVENTORY_SIZE; i++) inv.setItem(i, new ItemStack(Items.STONE, 64));
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 3, false)) == 0 && count(p, Items.FLINT) == 64 && offers.get(3).getUses() == 0,
                "a full inventory trades nothing and loses nothing");

        // level up: enough experience gives level 2 and its offers, in the same open menu
        inv.clearContent();
        inv.add(new ItemStack(Items.FLINT, 10));
        int before = offers.size();
        int level0 = villager.getVillagerData().level(); // the trades above may already have earned one
        villager.setVillagerXp(VillagerData.getMaxXpPerLevel(level0) - 1);
        offers.add(new MerchantOffer(new ItemCost(Items.FLINT, 10), new ItemStack(Items.STICK), 8, 20, 0.05F));
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, before, false)) == 1, "the trade that earns the level");
        check(h, villager.getVillagerData().level() == level0 + 1 && villager.getOffers().size() > before + 1,
                "the next level brings new offers: level " + villager.getVillagerData().level() + ", offers " + villager.getOffers().size() + " was " + before);

        // out of reach
        inv.add(new ItemStack(Items.FLINT, 10));
        p.setPos(p.getX() + 30, p.getY(), p.getZ());
        check(h, VillagerTrade.trade(p, new VillagerTrade(id, 3, false)) == 0 && count(p, Items.FLINT) == 10, "out of reach nothing happens");
        p.closeContainer();
        check(h, villager.getTradingPlayer() == null, "closing frees the villager");

        // Hero of the Village: the lower price is the one taken
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5, 1, 4)));
        p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1000, 0));
        menu = open(h, villager, p);
        MerchantOffer flint = villager.getOffers().get(3);
        int price = flint.getCostA().getCount();
        check(h, price == 7, "Hero of the Village takes 3 of 10 off, got " + price);
        check(h, VillagerTrade.trade(p, new VillagerTrade(menu.containerId, 3, false)) == 1 && count(p, Items.FLINT) == 3, "the discounted price is taken");
        p.closeContainer();
        villager.discard();
        h.succeed();
    }

    private VillagerTradeTest() {}
}
