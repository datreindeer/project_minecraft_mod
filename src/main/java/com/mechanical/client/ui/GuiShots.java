package com.mechanical.client.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mechanical.Coins;
import com.mechanical.Mechanical;
import com.mechanical.client.people.TradeBoardScreen;
import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.Equipment;
import com.mechanical.skills.Skill;
import com.mechanical.skills.Skills;
import com.mechanical.equipment.PackMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev only: with {@code guishots.flag} in the game directory (first line: output folder, second, optional: comma
 * separated name prefixes to shoot) the client photographs the GUI style: the menus from the title screen, then in a flat
 * world "GuiShots" the inventories, every container and the trade screens; then quits. Nothing happens without the flag.
 * Pattern: GearShots.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class GuiShots {
    private static int stage = -1, ticks, step;
    private static File out;
    private static String[] only = {};
    private static BlockPos spot;
    private static Vec3 stand;

    /** One shot: set up on the server, show on the client, then (a few ticks later) hover at {x, y} or run more. */
    private record Shot(String name, boolean world, Consumer<ServerPlayer> server, Runnable client, Supplier<int[]> hover) {}

    private static final List<Shot> SHOTS = new ArrayList<>();

    private static void menu(String name, Supplier<Screen> screen) {
        SHOTS.add(new Shot(name, false, p -> {}, () -> Minecraft.getInstance().gui.setScreen(screen.get()), null));
    }

    private static void inWorld(String name, Consumer<ServerPlayer> server, Runnable client, Supplier<int[]> hover) {
        SHOTS.add(new Shot(name, true, server, client, hover));
    }

    private static void show(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    /** Places {@code block} before the player, right-clicks it and fills the opened menu's first slots. */
    private static void container(String name, Supplier<Block> block, ItemStack... fill) {
        inWorld(name, p -> {
            reset(p);
            open(p, block.get());
            for (int i = 0; i < fill.length; i++) if (fill[i] != null) p.containerMenu.getSlot(i).set(fill[i].copy());
        }, () -> {}, null);
    }

    private static void open(ServerPlayer p, Block block) {
        ServerLevel level = p.level();
        level.setBlock(spot, Blocks.AIR.defaultBlockState(), 3);
        BlockState state = block.defaultBlockState();
        level.setBlock(spot, state, 3);
        state.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(spot), Direction.UP, spot, false));
        // some only open with their structure built: the menu straight from the block entity shows the screen all the same
        if (p.containerMenu == p.inventoryMenu && level.getBlockEntity(spot) instanceof net.minecraft.world.MenuProvider menu)
            p.openMenu(menu, buf -> buf.writeBlockPos(spot));
    }

    /** Survival, no menu, a bag of ordinary things. */
    private static void reset(ServerPlayer p) {
        p.closeContainer();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.removeAllEffects();
        ItemStack[] things = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.TORCH, 48), new ItemStack(Items.BREAD, 11),
                new ItemStack(Items.COAL, 30), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.OAK_LOG, 17),
                new ItemStack(Coins.COPPER_COIN.get(), 48), new ItemStack(Coins.SILVER_COIN.get(), 12), new ItemStack(Items.WHEAT, 64)};
        for (int i = 0; i < things.length; i++) p.getInventory().setItem(i, things[i]);
        p.getInventory().setItem(12, new ItemStack(Items.FLINT, 20));
        p.getInventory().setItem(13, new ItemStack(Items.OBSIDIAN, 6));
        p.getInventory().setItem(14, new ItemStack(Items.WOOL.black(), 6));
        p.getInventory().setItem(15, new ItemStack(Items.LEATHER, 9));
        p.getInventory().setItem(22, new ItemStack(Items.PAPER, 24));
        p.getInventory().setItem(30, new ItemStack(Items.EMERALD, 3));
        p.teleportTo(p.level(), stand.x, stand.y, stand.z, java.util.Set.of(), 0, 20, true);
    }

    private static Villager villager;

    private static void trade(ServerPlayer p, net.minecraft.resources.ResourceKey<VillagerProfession> profession, int level, int xp, boolean hero) {
        reset(p);
        ServerLevel world = p.level();
        world.setBlock(spot, Blocks.AIR.defaultBlockState(), 3);
        if (villager != null) villager.discard();
        villager = EntityTypes.VILLAGER.create(world, EntitySpawnReason.COMMAND);
        villager.setPos(stand.x, stand.y, stand.z + 1.8);
        villager.setNoAi(true);
        villager.setYRot(180);
        villager.setYHeadRot(180);
        villager.yBodyRot = 180;
        world.addFreshEntity(villager);
        villager.setVillagerData(villager.getVillagerData().withProfession(world.registryAccess(), profession).withLevel(level));
        villager.setVillagerXp(xp);
        if (hero) p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 4000, 0));
        p.teleportTo(world, stand.x, stand.y, stand.z, java.util.Set.of(), 0, 5, true);
    }

    /** A librarian gets one of every kind of row: two inputs, an enchanted book, a dearer price, sold out, purchases. */
    private static void interact(ServerPlayer p) {
        if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
            var enchantments = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var offers = villager.getOffers();
            offers.clear();
            Item coin = Coins.COPPER_COIN.get(), silver = Coins.SILVER_COIN.get();
            offers.add(offer(new ItemCost(silver, 10), new ItemCost(Items.BOOK), EnchantmentHelper.createBook(
                    new EnchantmentInstance(enchantments.getOrThrow(Enchantments.MENDING), 1)), 3));
            offers.add(offer(new ItemCost(coin, 24), null, new ItemStack(Items.BOOKSHELF), 12));
            offers.add(offer(new ItemCost(coin, 9), null, new ItemStack(Items.LANTERN), 12));
            MerchantOffer soldOut = offer(new ItemCost(silver, 2), null, new ItemStack(Items.NAME_TAG), 2);
            soldOut.setToOutOfStock();
            offers.add(soldOut);
            offers.add(offer(new ItemCost(Items.PAPER, 24), null, new ItemStack(coin, 4), 16));
            offers.add(offer(new ItemCost(Items.BOOK, 4), null, new ItemStack(coin, 6), 12));
            offers.add(offer(new ItemCost(Items.LEATHER, 6), null, new ItemStack(silver, 1), 8));
        }
        villager.mobInteract(p, InteractionHand.MAIN_HAND);
        if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
            villager.getOffers().get(1).addToSpecialPriceDiff(14); // dearer than usual, to show both ways
            p.sendMerchantOffers(p.containerMenu.containerId, villager.getOffers(), 5, villager.getVillagerXp(), true, true);
        }
    }

    private static MerchantOffer offer(ItemCost a, ItemCost b, ItemStack result, int uses) {
        return new MerchantOffer(a, java.util.Optional.ofNullable(b), result, uses, 5, 0.05F);
    }

    /** Clicks row {@code index} as a player would: the packet, the server, the new offers and the flash. */
    private static void click(int index) {
        int[] at = row(index);
        if (at != null) Minecraft.getInstance().gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(at[0], at[1],
                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }

    /** The centre of row {@code index} on the open trade screen. */
    private static int[] row(int index) {
        if (!(Minecraft.getInstance().gui.screen() instanceof TradeBoardScreen board)) return null;
        int[] at = board.rowAt(index);
        return at == null ? null : new int[] {at[0] + 60, at[1] + 11};
    }

    private static int[] fromCentre(int dx, int dy) {
        var window = Minecraft.getInstance().getWindow();
        return new int[] {window.getGuiScaledWidth() / 2 + dx, window.getGuiScaledHeight() / 2 + dy};
    }

    private static void selectCreativeTab(CreativeModeInventoryScreen screen, net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> tab) {
        try { // selectTab is private
            var m = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            m.setAccessible(true);
            m.invoke(screen, BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tab));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void creative(String name, net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> tab) {
        inWorld(name, p -> {
            reset(p);
            p.setGameMode(GameType.CREATIVE);
        }, () -> {
            Minecraft mc = Minecraft.getInstance();
            CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
            mc.gui.setScreen(screen);
            selectCreativeTab(screen, tab);
        }, null);
    }

    private static Block mod(String name) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
    }

    private static void menus() {
        Minecraft mc = Minecraft.getInstance();
        menu("menu_title", TitleScreen::new);
        menu("menu_options", () -> new net.minecraft.client.gui.screens.options.OptionsScreen(new TitleScreen(), mc.options));
        menu("menu_video", () -> new net.minecraft.client.gui.screens.options.VideoSettingsScreen(new TitleScreen(), mc, mc.options));
        menu("menu_keys", () -> new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(new TitleScreen(), mc.options));
        menu("menu_accessibility", () -> new net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen(new TitleScreen(), mc.options));
        menu("menu_worlds", () -> new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new TitleScreen()));
        menu("menu_multiplayer", () -> new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(new TitleScreen()));
        menu("menu_confirm", () -> new net.minecraft.client.gui.screens.ConfirmScreen(yes -> {}, Component.literal("Delete world?"),
                Component.literal("It will be lost forever (a long time).")));
        SHOTS.add(new Shot("menu_create_world", false, p -> {}, () ->
                net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen())), null));

    }

    /** The shots in the world: built once it is there (item stacks need a loaded world). */
    private static void worldShots() {
        Minecraft mc = Minecraft.getInstance();
        inWorld("world_pause", GuiShots::reset, () -> show(new net.minecraft.client.gui.screens.PauseScreen(true)), null);
        inWorld("world_options", GuiShots::reset, () -> show(new net.minecraft.client.gui.screens.options.OptionsScreen(
                new net.minecraft.client.gui.screens.PauseScreen(true), mc.options)), null);
        inWorld("inventory", p -> {
            reset(p);
            Equipment.of(p).set(Accessory.BACK, new ItemStack(Mechanical.LEATHER_SATCHEL.get()));
            Equipment.of(p).set(Accessory.HIP, new ItemStack(Mechanical.LEATHER_POUCH.get()));
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            p.addEffect(new MobEffectInstance(MobEffects.SPEED, 2000, 1));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 0));
        }, () -> show(new InventoryScreen(mc.player)), () -> {
            if (!(mc.gui.screen() instanceof InventoryScreen s)) return null;
            return new int[] {s.getLeftPos() + 16, s.getTopPos() + 172}; // hover the pickaxe in the hotbar
        });
        inWorld("inventory_recipebook", GuiShots::reset, () -> {
            InventoryScreen s = new InventoryScreen(mc.player);
            show(s);
            for (var child : s.children())
                if (child instanceof net.minecraft.client.gui.components.ImageButton b) {
                    b.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                    break;
                }
        }, null);
        creative("creative_items", CreativeModeTabs.BUILDING_BLOCKS);
        creative("creative_search", CreativeModeTabs.SEARCH);
        creative("creative_inventory", CreativeModeTabs.INVENTORY);

        container("box_crafting", () -> Blocks.CRAFTING_TABLE, null, new ItemStack(Items.OAK_PLANKS), null, null, new ItemStack(Items.STICK), null, null,
                new ItemStack(Items.STICK));
        inWorld("box_furnace", p -> {
            reset(p);
            open(p, Blocks.FURNACE);
            p.containerMenu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 8));
            p.containerMenu.getSlot(1).set(new ItemStack(Items.COAL, 5));
        }, () -> {}, null);
        container("box_blast", () -> Blocks.BLAST_FURNACE);
        container("box_chest", () -> Blocks.CHEST, new ItemStack(Items.APPLE, 5), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.COAL, 64));
        inWorld("box_double_chest", p -> {
            reset(p);
            p.level().setBlock(spot.east(), Blocks.AIR.defaultBlockState(), 3);
            var container = new net.minecraft.world.SimpleContainer(54);
            for (int i = 0; i < 54; i += 5) container.setItem(i, new ItemStack(Items.BRICK, 1 + i));
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> net.minecraft.world.inventory.ChestMenu.sixRows(id, inv, container),
                    Component.translatable("container.chestDouble")));
        }, () -> {}, null);
        container("box_shulker", () -> Blocks.SHULKER_BOX.defaultBlockState().getBlock());
        container("box_dispenser", () -> Blocks.DISPENSER, new ItemStack(Items.ARROW, 20));
        container("box_hopper", () -> Blocks.HOPPER, new ItemStack(Items.IRON_INGOT, 20));
        container("box_crafter", () -> Blocks.CRAFTER);
        container("box_brewing", () -> Blocks.BREWING_STAND, new ItemStack(Items.POTION), new ItemStack(Items.POTION), null, new ItemStack(Items.NETHER_WART),
                new ItemStack(Items.BLAZE_POWDER, 8));
        container("box_enchanting", () -> Blocks.ENCHANTING_TABLE, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.LAPIS_LAZULI, 12));
        container("box_anvil", () -> Blocks.ANVIL, new ItemStack(Items.IRON_SWORD));
        container("box_anvil_empty", () -> Blocks.ANVIL);
        container("box_smithing", () -> Blocks.SMITHING_TABLE);
        container("box_grindstone", () -> Blocks.GRINDSTONE);
        container("box_loom", () -> Blocks.LOOM, new ItemStack(Items.BANNER.white()), new ItemStack(Items.DYE.red(), 4));
        container("box_stonecutter", () -> Blocks.STONECUTTER, new ItemStack(Items.STONE, 20));
        container("box_cartography", () -> Blocks.CARTOGRAPHY_TABLE);
        container("box_beacon", () -> Blocks.BEACON);
        inWorld("box_horse", p -> {
            reset(p);
            var horse = EntityTypes.DONKEY.create(p.level(), EntitySpawnReason.COMMAND);
            horse.setPos(stand.x + 1, stand.y, stand.z + 2);
            horse.setTamed(true);
            horse.setNoAi(true);
            p.level().addFreshEntity(horse);
            horse.getSlot(499).set(new ItemStack(Items.CHEST));
            horse.openCustomInventoryScreen(p);
        }, () -> {}, null);

        for (String station : List.of("tool_station", "fletching_table", "leatherworking_bench", "armor_station", "mold_bench", "kiln", "bloomery_hearth", "furnace_tap", "high_furnace_front"))
            container("mod_" + station, () -> mod(station));
        toolStation();
        toolVariants();
        crossbows();
        inWorld("mod_pack", p -> {
            reset(p);
            Equipment.of(p).set(Accessory.BACK, new ItemStack(Mechanical.LEATHER_SATCHEL.get()));
            Equipment.of(p).set(Accessory.HIP, new ItemStack(Mechanical.LEATHER_POUCH.get()));
            PackMenu.openWorn(p);
        }, () -> {}, null);

        inWorld("trade_farmer", p -> trade(p, VillagerProfession.FARMER, 2, 30, false), () -> {}, null);
        // the interact happens once the client sees the villager (it looks at it, so the screen gets its portrait)
        inWorld("trade_farmer_hover", p -> interact(p), () -> {}, () -> row(0));
        inWorld("trade_librarian", p -> trade(p, VillagerProfession.LIBRARIAN, 5, 260, true), () -> {}, null);
        inWorld("trade_librarian_hover", p -> interact(p), () -> {}, () -> row(1));
        inWorld("trade_librarian_hover_bought", p -> {}, () -> click(4), null);
        inWorld("trade_librarian_hover_bought_paper", p -> {}, () -> {}, () -> row(4));
        inWorld("trade_elder", p -> {
            reset(p);
            if (villager != null) villager.discard();
            com.mechanical.people.GuiShotHooks.elder(p, stand);
        }, () -> {}, () -> row(0));
        inWorld("trade_caravan", p -> {
            reset(p);
            com.mechanical.people.GuiShotHooks.caravan(p, stand);
        }, () -> {}, () -> row(1));

        inWorld("screen_clan", p -> {
            com.mechanical.people.GuiShotHooks.clear(p);
            reset(p);
        }, () -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new com.mechanical.clan.ClanNetwork.Request()), null);
        inWorld("screen_dialog", GuiShots::reset, () -> show(new com.mechanical.client.people.TribeDialogScreen(new com.mechanical.people.TribeDialog.Open(0,
                Component.literal("Ashka of the Reed Marsh tribe"), Component.literal(
                        "We have walked a long way, friend. May our band rest by your fire tonight? We have hides and herbs to trade."),
                com.mechanical.people.TribeDialog.TRADE | com.mechanical.people.TribeDialog.YES_NO))), null);
        inWorld("screen_advancements", GuiShots::reset, () -> show(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(
                mc.player.connection.getAdvancements())), null);
        quests();
        recipes();
        skills();
        worldMap();
        inWorld("screen_stats", GuiShots::reset, () -> show(new net.minecraft.client.gui.screens.achievement.StatsScreen(null, mc.player.getStats())), null);
    }

    /** Set while a shot's server work runs long: the shot waits for it. */
    private static volatile boolean waiting;

    /** The world map over land walked here and there. Tribe mates can't join a singleplayer world, so theirs are handed to the screen. */
    private static void worldMap() {
        Minecraft mc = Minecraft.getInstance();
        inWorld("map_a_walk", p -> {
            waiting = true;
            try {
                reset(p);
                var explored = com.mechanical.map.ExploredMap.get(p.level().getServer());
                int[][] path = {{0, 0}, {10, -3}, {19, -9}, {25, 1}, {17, 9}, {-9, 6}, {-18, 13}, {-30, 11}, {3, -14}};
                for (int i = 0; i < path.length; i++) {
                    int cx = (int) stand.x / 16 + path[i][0], cz = (int) stand.z / 16 + path[i][1];
                    explored.explore(p.getUUID(), cx, cz);
                    for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) p.level().getChunk(cx + dx, cz + dz);
                }
            } finally {
                waiting = false;
            }
        }, () -> {}, null);
        mapShot("map_b_key", 1920, 1080, 3, true, s -> {});
        mapShot("map_c_far", 1920, 1080, 3, false, s -> s.mouseScrolled(320, 180, 0, -12));
        mapShot("map_d_small", 854, 480, 2, false, s -> s.mouseScrolled(214, 120, 0, 3));
        mapShot("map_e_close_dragged", 1920, 1080, 3, false, s -> {
            s.mouseScrolled(320, 180, 0, 12);
            s.mouseScrolled(320, 180, 0, -2);
            drag(s, 320, 180, 200, 120);
        });
    }

    private static void mapShot(String name, int w, int h, int scale, boolean byKey, Consumer<com.mechanical.client.map.MapScreen> then) {
        Minecraft mc = Minecraft.getInstance();
        boolean[] done = {false};
        inWorld(name, p -> p.teleportTo(p.level(), stand.x, stand.y, stand.z, java.util.Set.of(), 35, 20, true), () -> {
            mc.getWindow().setWindowed(w, h);
            mc.options.guiScale().set(scale);
            mc.resizeGui();
            if (byKey) {
                mc.gui.setScreen(null);
                net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey(com.mechanical.client.map.MapClient.OPEN.saveString()));
            } else show(new com.mechanical.client.map.MapScreen());
        }, () -> {
            if (!(mc.gui.screen() instanceof com.mechanical.client.map.MapScreen s)) return null;
            if (!done[0]) {
                done[0] = true;
                then.accept(s);
            }
            int x = (int) stand.x, z = (int) stand.z;
            s.people(new com.mechanical.map.MapNetwork.People(net.minecraft.world.item.DyeColor.CYAN.getId(), List.of(
                    new com.mechanical.map.MapNetwork.Person("Ashka", x + 70, z - 45, 90), new com.mechanical.map.MapNetwork.Person("Torvald_the_Bold", x + 76, z - 40, -45),
                    new com.mechanical.map.MapNetwork.Person("Runa", x + 2600, z + 900, 180))));
            s.features(mapFeatures(x >> 4 << 4, z >> 4 << 4));
            return null;
        });
    }

    /** One of everything the map marks, round (x, z), in place of what the empty test world would answer. */
    private static String mapFeatures(int x, int z) {
        java.util.function.BiFunction<int[], int[], String> box = (a, s) -> "[[" + (x + a[0]) + "," + (z + a[1]) + "," + (x + a[0] + s[0]) + "," + (z + a[1]) + ","
                + (x + a[0] + s[0]) + "," + (z + a[1] + s[1]) + "," + (x + a[0]) + "," + (z + a[1] + s[1]) + "]]";
        java.util.function.BiFunction<Integer, Integer, String> at = (dx, dz) -> "'x':" + (x + dx) + ",'z':" + (z + dz);
        return ("{'tribes':[{'id':'a','name':'Ravenfolk','colour':'#169c9c','chunks':72,'rings':" + box.apply(new int[] {-96, -64}, new int[] {144, 128})
                + ",'loaded':" + box.apply(new int[] {-48, -32}, new int[] {80, 80}) + ",'label':{" + at.apply(-50, 40) + "}},"
                + "{'id':'b','name':'Wolfkin','colour':'#b02e26','chunks':16,'rings':" + box.apply(new int[] {48, -32}, new int[] {64, 64}) + ",'loaded':[],'label':{" + at.apply(80, 8) + "}}],"
                + "'tribeVillages':[{'id':'npc:1','name':'Elk Tribe','colour':'#835432','rings':" + box.apply(new int[] {-208, 16}, new int[] {80, 80}) + ",'label':{" + at.apply(-168, 56) + "}}],"
                + "'villages':[{'name':'Dunewell','kind':'desert','rings':" + box.apply(new int[] {96, 64}, new int[] {96, 80}) + ",'label':{" + at.apply(144, 104) + "}}],"
                + "'fires':[{" + at.apply(8, 12) + ",'kind':'big_fire_pit','tribe':'a'},{" + at.apply(84, -12) + ",'kind':'fire_pit','tribe':'b'},{" + at.apply(-70, -100) + ",'kind':'campfire','tribe':null}],"
                + "'landmarks':[{'kind':'ancient_oak'," + at.apply(-120, -60) + "},{'kind':'waystone'," + at.apply(40, 90) + "},{'kind':'tribe_village'," + at.apply(-168, 56) + "}]}")
                .replace('\'', '"');
    }

    private static Identifier quest(String path) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, path);
    }

    /** Earns the quests (all their criteria, or the first {@code some}). */
    private static void earn(ServerPlayer p, int some, String... paths) {
        for (String path : paths) {
            var holder = p.level().getServer().getAdvancements().get(quest(path));
            holder.value().criteria().keySet().stream().sorted().limit(some).forEach(c -> p.getAdvancements().award(holder, c));
        }
    }

    private static com.mechanical.client.quests.QuestScreen questScreen() {
        return (com.mechanical.client.quests.QuestScreen) Minecraft.getInstance().gui.screen();
    }

    /** The quest screen in a w x h window at a GUI scale, turned to a quest (clicked with {@code pick}); hovering one. */
    private static void questShot(String name, int w, int h, int scale, String at, boolean pick, String hover, int keys, int key) {
        Minecraft mc = Minecraft.getInstance();
        inWorld(name, p -> {}, () -> {
            mc.getWindow().setWindowed(w, h);
            mc.options.guiScale().set(scale);
            mc.resizeGui();
            show(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(mc.player.connection.getAdvancements()));
            questScreen().show(quest(at), pick);
            for (int i = 0; i < keys; i++) questScreen().keyPressed(new net.minecraft.client.input.KeyEvent(key, 0, 0));
        }, hover == null ? null : () -> mc.gui.screen() instanceof com.mechanical.client.quests.QuestScreen s ? s.screenPos(quest(hover)) : null);
    }

    private static void quests() {
        Minecraft mc = Minecraft.getInstance();
        inWorld("quest_00_earn", p -> {
            reset(p);
            earn(p, 99, "stone_age/root", "stone_age/plant_fibre", "stone_age/twine", "stone_age/flint_knife", "stone_age/flint_axe",
                    "stone_age/firewood", "stone_age/bark", "stone_age/campfire", "stone_age/light_fire", "stone_age/kiln",
                    "stone_age/flint_pickaxe", "stone_age/stone_heads", "stone_age/ochre", "wilderness/root", "wilderness/butcher",
                    "hunting/root", "hunting/deer", "hunting/roe_deer", "folklore/root", "food/root");
            earn(p, 7, "stone_age/bark_collector");
            earn(p, 2, "hunting/red_deer", "hunting/moose");
        }, () -> {}, null);
        questShot("quest_01_s3_overview", 1920, 1080, 3, "stone_age/root", false, null, 0, 0);
        questShot("quest_02_s3_hover", 1920, 1080, 3, "stone_age/bark", true, "stone_age/bark_basket", 0, 0);
        questShot("quest_03_s3_ready", 1920, 1080, 3, "stone_age/crucible", true, null, 0, 0);
        questShot("quest_04_s3_checklist", 1920, 1080, 3, "stone_age/bark_collector", true, null, 0, 0);
        questShot("quest_05_s2_hunting", 1920, 1080, 2, "hunting/red_deer", true, null, 0, 0);
        questShot("quest_06_s2_wilderness", 1920, 1080, 2, "wilderness/root", false, null, 0, 0);
        questShot("quest_07_s2_wilderness_far", 1920, 1080, 2, "wilderness/root", false, null, 4, 45 /* minus */);
        questShot("quest_08_s4_overview", 1920, 1080, 4, "stone_age/root", false, null, 0, 0);
        questShot("quest_09_s4_locked", 1920, 1080, 4, "stone_age/crude_stone", true, null, 0, 0);
        questShot("quest_10_small_overview", 854, 480, 2, "bronze_age/root", false, null, 0, 0);
        questShot("quest_11_small_detail", 854, 480, 2, "stone_age/light_fire", true, null, 0, 0);
        questShot("quest_12_tiny_overview", 640, 480, 2, "stone_age/root", false, null, 0, 0);
        questShot("quest_13_tiny_detail", 640, 480, 2, "hunting/moose", true, null, 0, 0);
        inWorld("quest_17_wilderness_fit", p -> {}, () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(2);
            mc.resizeGui();
            show(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(mc.player.connection.getAdvancements()));
            questScreen().show(quest("wilderness/root"), false);
            press(questScreen(), "\u26F6");
        }, null);
        // real input, once frames have drawn: a drag moves the graph, a click on a node picks it, the wheel zooms, Next walks
        // the ready quests, a sidebar row opens its chapter, and reopening comes back to where it was left
        boolean[] played = {false};
        inWorld("quest_18_input", p -> {}, () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(3);
            mc.resizeGui();
            show(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(mc.player.connection.getAdvancements()));
            questScreen().show(quest("gemstones/root"), false);
        }, () -> {
            if (played[0] || !(mc.gui.screen() instanceof com.mechanical.client.quests.QuestScreen s)) return null;
            played[0] = true;
            report("open", s);
            drag(s, 240, 200, 270, 215);
            report("dragged", s);
            int[] at = s.screenPos(quest("gemstones/root"));
            drag(s, at[0], at[1], at[0], at[1]);
            report("clicked the root", s);
            s.mouseScrolled(240, 200, 0, -1);
            report("wheel out", s);
            press(s, Component.translatable("gui.mechanical.quests.next").getString() + " ▶");
            report("next", s);
            drag(s, 200, 300, 200, 300);
            report("clicked at 200,300", s);
            s.mouseClicked(mouseEvent(20, 55), false); // the second chapter in the sidebar
            report("sidebar", s);
            s.onClose();
            show(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(mc.player.connection.getAdvancements()));
            report("reopened", questScreen());
            return null;
        });
        // the real ways in: the advancements key, and the pause menu's button
        inWorld("quest_14_key", p -> {}, () -> {
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(0);
            mc.resizeGui();
            mc.gui.setScreen(null);
            net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey(mc.options.keyAdvancements.saveString()));
        }, null);
        inWorld("quest_15_pause", p -> {}, () -> show(new net.minecraft.client.gui.screens.PauseScreen(true)), null);
        inWorld("quest_16_pause_button", p -> {}, () -> {
            show(new net.minecraft.client.gui.screens.PauseScreen(true));
            press(mc.gui.screen(), Component.translatable("gui.advancements").getString());
            System.out.println("QUESTSHOT pause button opened " + mc.gui.screen());
        }, null);
    }

    /** The recipe viewer on {@code item} (U with {@code usage}), turned {@code pages - 1} pages on, or to its last tab; {@code give} is carried first, so it counts as seen. */
    private static void recipeShot(String name, Supplier<Item> item, boolean usage, int pages, boolean lastTab, Supplier<List<Item>> give) {
        inWorld(name, p -> {
            reset(p);
            for (Item i : give.get()) p.getInventory().add(new ItemStack(i));
            com.mechanical.viewer.SeenItems.scan(p);
        }, () -> {
            var screen = com.mechanical.client.viewer.RecipeViewScreen.open(item.get(), usage);
            if (lastTab) screen.showTab(-1);
            for (int i = 1; i < pages; i++) screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT, 0, 0));
        }, null);
    }

    private static void recipes() {
        recipeShot("recipes_twine", Mechanical.TWINE, false, 1, false, () -> List.of(Mechanical.PLANT_FIBRE.get()));
        recipeShot("recipes_bronze_pickaxe", com.mechanical.Bronze.BRONZE_PICKAXE, false, 1, false, () -> List.of(com.mechanical.Bronze.BRONZE_PICKAXE_HEAD.get()));
        recipeShot("recipes_stick_usage", () -> Items.STICK, true, 2, false, () -> List.of(Items.STICK));
        // Twine is seen (a link with its icon), Oak Bark is not (???)
        recipeShot("recipes_note", Mechanical.PLANT_FIBRE, false, 1, true, () -> List.of(Mechanical.PLANT_FIBRE.get(), Mechanical.TWINE.get()));
        recipeShot("recipes_none", () -> Items.BEDROCK, false, 1, false, List::of);
        Minecraft mc = Minecraft.getInstance();
        // the panel beside the inventory, searching every bronze item (all seen), the first one hovered
        inWorld("search_panel", p -> {
            reset(p);
            p.getInventory().setItem(10, new ItemStack(com.mechanical.Bronze.BRONZE_INGOT.get(), 7));
            List<String> seen = new ArrayList<>(p.getData(com.mechanical.viewer.SeenItems.SEEN.get()));
            for (Item i : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(i).toString();
                if (id.contains("bronze") && !seen.contains(id)) seen.add(id);
            }
            p.setData(com.mechanical.viewer.SeenItems.SEEN.get(), seen);
        }, () -> {
            show(new InventoryScreen(mc.player));
            searchBox().setValue("bronze");
        }, () -> {
            var box = searchBox();
            return box == null ? null : new int[] {box.getX() + 9, box.getY() + 25 + 9};
        });
        // the box focused, then a real R key (and its letter): it types, no viewer opens, though the mouse is on the pickaxe
        inWorld("search_typing", GuiShots::reset, () -> {
            show(new InventoryScreen(mc.player));
            var box = searchBox();
            box.setValue("");
            mc.gui.screen().setFocused(box);
        }, () -> {
            if (!(mc.gui.screen() instanceof InventoryScreen s)) return null;
            if (ticks == 45) {
                typeKey(com.mojang.blaze3d.platform.InputConstants.KEY_R, 'r');
                System.out.println("GUISHOT search_typing: box '" + searchBox().getValue() + "', screen " + mc.gui.screen().getClass().getSimpleName());
            }
            return new int[] {s.getLeftPos() + 16, s.getTopPos() + 172};
        });
        // R over the Twine in the inventory opens its recipe
        inWorld("recipes_hover", p -> {
            reset(p);
            p.getInventory().setItem(10, new ItemStack(Mechanical.TWINE.get(), 5));
            p.getInventory().setItem(11, new ItemStack(Mechanical.PLANT_FIBRE.get(), 9));
            com.mechanical.viewer.SeenItems.scan(p);
        }, () -> show(new InventoryScreen(mc.player)), () -> {
            if (!(mc.gui.screen() instanceof InventoryScreen s)) return null;
            if (ticks == 45) typeKey(com.mojang.blaze3d.platform.InputConstants.KEY_R, 0);
            for (var slot : s.getMenu().slots)
                if (slot.getItem().is(Mechanical.TWINE.get())) return new int[] {s.getLeftPos() + slot.x + 8, s.getTopPos() + slot.y + 8};
            return null;
        });
    }

    /** Varied levels (set straight, so no popups lie under the screen), Fletching at the top, and a gold Topaz ring: Mining +3. */
    private static void skilled(ServerPlayer p) {
        reset(p);
        int[] levels = {22, 13, 8, 3, 15, 41, 5, 11, 1, 2, 6, 100};
        java.util.Map<String, Integer> xp = new java.util.HashMap<>();
        for (Skill s : Skill.values()) {
            int l = levels[s.ordinal()];
            xp.put(s.key(), Skills.xpForLevel(l) + (l < Skills.MAX_LEVEL ? (Skills.xpForLevel(l + 1) - Skills.xpForLevel(l)) * (s.ordinal() * 7 % 10) / 10 : 0));
        }
        p.setData(Skills.XP.get(), xp);
        Equipment.of(p).set(Accessory.RING_1, com.mechanical.Jewelry.make(com.mechanical.Jewelry.GOLD_RING.get(), new com.mechanical.Jewelry.Jewel("topaz", false)));
    }

    /** At {@code tick} of the shot, {@code amount} Mining XP (once each), through the real payload to the popup. */
    private static Supplier<int[]> mine(int tick, int amount) {
        return () -> {
            if (ticks == tick || ticks == tick + 4) {
                var server = Minecraft.getInstance().getSingleplayerServer();
                server.execute(() -> Skills.addXp(server.getPlayerList().getPlayers().get(0), Skill.MINING, amount));
            }
            return null;
        };
    }

    /** A bronze-age smith (Smithing 40) at the Tool Station: everything up to bronze known, the parts seen. */
    private static void smith(ServerPlayer p, Item... carry) {
        reset(p);
        p.setData(Mechanical.UNLOCKED_TOOLS, com.mechanical.ToolStationRecipes.ALL.stream()
                .filter(r -> com.mechanical.tools.ToolParts.tier(r.head().get()) <= 2).map(com.mechanical.ToolStationRecipes.Recipe::id).toList());
        java.util.List<String> seen = new java.util.ArrayList<>();
        for (Item i : List.of(Mechanical.TWINE.get(), Mechanical.CORDAGE.get(), Items.LEATHER, Items.STICK, Mechanical.TOOL_ROD.get(),
                com.mechanical.Kettle.HIDE_GLUE.get(), com.mechanical.Smithing.BONE_TOOL_ROD.get(), com.mechanical.Smithing.COPPER_TOOL_ROD.get(),
                com.mechanical.Smithing.BRONZE_TOOL_ROD.get(), com.mechanical.Bronze.BRONZE_PLATE.get(), com.mechanical.wildlife.Wildlife.SINEW.get(), Items.STRING,
                Items.BAMBOO, com.mechanical.BronzeTools.YEW_STAVE.get()))
            seen.add(BuiltInRegistries.ITEM.getKey(i).toString());
        p.setData(com.mechanical.viewer.SeenItems.SEEN.get(), List.copyOf(seen));
        var xp = new java.util.HashMap<>(p.getData(Skills.XP.get()));
        xp.put(Skill.SMITHING.key(), Skills.xpForLevel(40));
        p.setData(Skills.XP.get(), xp);
        for (int i = 0; i < carry.length; i++) p.getInventory().setItem(18 + i, new ItemStack(carry[i]));
        open(p, mod("tool_station"));
    }

    /** Clicks the recipe for {@code result} (as the screen would), then lays exactly these parts in the pockets. */
    private static void lay(ServerPlayer p, Item result, Item head, Item binding, Item rod) {
        var recipes = com.mechanical.ToolStationRecipes.ALL;
        int i = 0;
        while (recipes.get(i).result().get() != result) i++;
        p.containerMenu.clickMenuButton(p, i);
        Item[] parts = {head, binding, rod};
        for (int k = 0; k < 3; k++) p.containerMenu.getSlot(k).set(new ItemStack(parts[k]));
    }

    private static void toolStation() {
        Minecraft mc = Minecraft.getInstance();
        Runnable big = () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(3);
            mc.resizeGui();
        };
        inWorld("toolstation_empty", p -> smith(p), big, null);
        inWorld("toolstation_bronze_pickaxe", p -> {
            smith(p, Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get());
            lay(p, com.mechanical.Bronze.BRONZE_PICKAXE.get(), com.mechanical.Bronze.BRONZE_PICKAXE_HEAD.get(), Items.LEATHER,
                    com.mechanical.Smithing.BRONZE_TOOL_ROD.get());
        }, big, null);
        // the copper pickaxe picked, no head yet (its ghost shows), a bronze rod: the rod pocket says why
        inWorld("toolstation_tier", p -> {
            smith(p, com.mechanical.Casting.COPPER_PICKAXE_HEAD.get());
            lay(p, Items.COPPER_PICKAXE, Items.AIR, Mechanical.CORDAGE.get(), com.mechanical.Smithing.BRONZE_TOOL_ROD.get());
        }, big, null);
        // bows: a Sinew string, a Leather grip and a Stick stave on the Crude Bow (string, grip, stave; draw and power columns)
        inWorld("toolstation_bow", p -> {
            smith(p, Mechanical.CORDAGE.get());
            lay(p, Mechanical.CRUDE_BOW.get(), com.mechanical.wildlife.Wildlife.SINEW.get(), Items.LEATHER, Items.STICK);
        }, big, null);
        inWorld("toolstation_bow_fishing", p -> {
            smith(p, Mechanical.BONE_FISH_HOOK.get());
            lay(p, Mechanical.BONE_HOOK_FISHING_ROD.get(), Mechanical.BONE_FISH_HOOK.get(), Items.STRING, Items.BAMBOO);
        }, big, null);
    }

    /** A tool as the Tool Station builds it from these parts. */
    private static ItemStack built(Item tool, Item binding, Item rod) {
        return built(tool, null, binding, rod);
    }

    /** head: a bow's string (null: the recipe's own head). */
    private static ItemStack built(Item tool, Item head, Item binding, Item rod) {
        var recipe = com.mechanical.ToolStationRecipes.ALL.stream().filter(r -> r.result().get() == tool).findFirst().orElseThrow();
        ItemStack stack = new ItemStack(tool);
        recipe.apply(stack, head == null ? recipe.head().get() : head, binding, rod, 0);
        return stack;
    }

    /** Tools look like their parts: bronze pickaxes per rod (row 1) and per binding (row 2), steel axes, flint spears, bronze tongs and quarry hammers (row 3); then the steel axe in hand. */
    private static void toolVariants() {
        Minecraft mc = Minecraft.getInstance();
        Item pick = com.mechanical.Bronze.BRONZE_PICKAXE.get(), axe = com.mechanical.Smithing.STEEL_AXE.get(), spear = Mechanical.FLINT_SPEAR.get();
        Item cordage = Mechanical.CORDAGE.get(), rod = Mechanical.TOOL_ROD.get();
        Item tongs = com.mechanical.BronzeTools.BRONZE_TONGS.get(), quarry = com.mechanical.BronzeTools.BRONZE_QUARRY_HAMMER.get();
        inWorld("tool_variants", p -> {
            reset(p);
            p.getInventory().clearContent();
            ItemStack[] stacks = {
                    new ItemStack(pick), built(pick, cordage, Items.STICK), built(pick, cordage, com.mechanical.Smithing.BONE_TOOL_ROD.get()),
                    built(pick, cordage, com.mechanical.Smithing.COPPER_TOOL_ROD.get()), built(pick, cordage, com.mechanical.Smithing.BRONZE_TOOL_ROD.get()), null, null, null, null,
                    built(pick, Mechanical.TWINE.get(), rod), built(pick, com.mechanical.Kettle.HIDE_GLUE.get(), rod), built(pick, Items.LEATHER, rod),
                    built(pick, com.mechanical.wildlife.Wildlife.SINEW.get(), rod), built(pick, com.mechanical.Bronze.BRONZE_PLATE.get(), rod), built(pick, cordage, rod),
                    null, null, null,
                    new ItemStack(axe), built(axe, com.mechanical.Smithing.STEEL_PLATE.get(), com.mechanical.Smithing.STEEL_TOOL_ROD.get()),
                    built(axe, Items.LEATHER, com.mechanical.Smithing.BONE_TOOL_ROD.get()), new ItemStack(spear), built(spear, Mechanical.TWINE.get(), Items.STICK),
                    new ItemStack(tongs), built(tongs, Mechanical.CORDAGE.get(), com.mechanical.Smithing.BRONZE_TOOL_ROD.get()),
                    new ItemStack(quarry), built(quarry, Items.LEATHER, com.mechanical.Smithing.BRONZE_TOOL_ROD.get())};
            for (int i = 0; i < stacks.length; i++) if (stacks[i] != null) p.getInventory().setItem(9 + i, stacks[i]);
            p.getInventory().setItem(0, built(axe, com.mechanical.Smithing.STEEL_PLATE.get(), com.mechanical.Smithing.STEEL_TOOL_ROD.get()));
            // the hotbar: bows by string, grip and stave, then fishing rods by line and rod
            Item bow = Mechanical.CRUDE_BOW.get(), sinew = com.mechanical.wildlife.Wildlife.SINEW.get(), twine = Mechanical.TWINE.get();
            ItemStack[] ranged = {new ItemStack(bow), built(bow, sinew, Items.LEATHER, rod), built(bow, Items.STRING, twine, Items.STICK),
                    built(com.mechanical.BronzeTools.YEW_LONGBOW.get(), cordage, twine, com.mechanical.BronzeTools.YEW_STAVE.get()),
                    new ItemStack(Mechanical.CRUDE_FISHING_ROD.get()), built(Mechanical.CRUDE_FISHING_ROD.get(), Items.STRING, Items.BAMBOO),
                    built(Mechanical.BONE_HOOK_FISHING_ROD.get(), sinew, Items.STICK), built(com.mechanical.Smithing.IRON_HOOK_FISHING_ROD.get(), cordage, Items.BAMBOO)};
            for (int i = 0; i < ranged.length; i++) p.getInventory().setItem(1 + i, ranged[i]);
        }, () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(4);
            mc.resizeGui();
            show(new InventoryScreen(mc.player));
        }, null);
        inWorld("tool_variants_held", p -> {}, () -> {
            mc.gui.setScreen(null);
            mc.player.getInventory().setSelectedSlot(0);
        }, null);
    }

    /** A crossbow loaded with a bolt, as one charged on the Tool Station's parts. */
    private static ItemStack charged(ItemStack crossbow, Item bolt) {
        crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                net.minecraft.world.item.component.ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(bolt))));
        return crossbow;
    }

    /** Crossbows: the Tool Station with a steel prod; a row of crossbows by stock and string, loaded and not; a charged steel one in hand. */
    private static void crossbows() {
        Minecraft mc = Minecraft.getInstance();
        Item steelBow = com.mechanical.Smithing.STEEL_CROSSBOW.get(), sinew = com.mechanical.wildlife.Wildlife.SINEW.get(), rod = Mechanical.TOOL_ROD.get();
        inWorld("crossbow_station", p -> {
            smith(p, com.mechanical.Smithing.STEEL_CROSSBOW_PROD.get(), com.mechanical.Smithing.IRON_TOOL_ROD.get(), sinew);
            p.setData(Mechanical.UNLOCKED_TOOLS, com.mechanical.ToolStationRecipes.ALL.stream().map(com.mechanical.ToolStationRecipes.Recipe::id).toList());
            java.util.List<String> seen = new java.util.ArrayList<>(p.getData(com.mechanical.viewer.SeenItems.SEEN.get()));
            for (Item i : List.of(com.mechanical.Smithing.IRON_TOOL_ROD.get(), com.mechanical.Smithing.STEEL_TOOL_ROD.get(), com.mechanical.Smithing.STEEL_CROSSBOW_PROD.get()))
                seen.add(BuiltInRegistries.ITEM.getKey(i).toString());
            p.setData(com.mechanical.viewer.SeenItems.SEEN.get(), List.copyOf(seen));
            lay(p, steelBow, com.mechanical.Smithing.STEEL_CROSSBOW_PROD.get(), sinew, com.mechanical.Smithing.IRON_TOOL_ROD.get());
        }, () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(3);
            mc.resizeGui();
        }, null);
        inWorld("crossbow_variants", p -> {
            reset(p);
            p.getInventory().clearContent();
            Item cross = Items.CROSSBOW, cordage = Mechanical.CORDAGE.get(), twine = Mechanical.TWINE.get();
            ItemStack[] row = {new ItemStack(cross), built(cross, sinew, Items.STICK), built(cross, cordage, com.mechanical.Smithing.BRONZE_TOOL_ROD.get()),
                    built(cross, Items.STRING, com.mechanical.Smithing.IRON_TOOL_ROD.get()), built(cross, twine, com.mechanical.Smithing.BONE_TOOL_ROD.get()),
                    charged(built(cross, sinew, rod), com.mechanical.Smithing.IRON_BOLT.get()), null, null, null,
                    new ItemStack(steelBow), built(steelBow, sinew, rod), built(steelBow, sinew, com.mechanical.Smithing.STEEL_TOOL_ROD.get()),
                    built(steelBow, Items.STRING, com.mechanical.Smithing.COPPER_TOOL_ROD.get()), built(steelBow, cordage, Items.STICK),
                    charged(built(steelBow, sinew, rod), com.mechanical.Smithing.STEEL_BOLT.get()), null, null, null,
                    new ItemStack(com.mechanical.Smithing.IRON_CROSSBOW_PROD.get()), new ItemStack(com.mechanical.Smithing.STEEL_CROSSBOW_PROD.get()),
                    new ItemStack(com.mechanical.Bronze.BRONZE_BOLT_HEAD.get(), 4), new ItemStack(com.mechanical.Smithing.BRONZE_BOLT.get(), 16),
                    new ItemStack(com.mechanical.Smithing.IRON_BOLT.get(), 16), new ItemStack(com.mechanical.Smithing.STEEL_BOLT_HEAD.get(), 4),
                    new ItemStack(com.mechanical.Smithing.STEEL_BOLT.get(), 16), new ItemStack(com.mechanical.Bronze.ROUGH_CAST_BRONZE_BOLT_HEADS.get()),
                    new ItemStack(com.mechanical.Bronze.UNFIRED_CLAY_BOLT_HEADS_MOLD.get())};
            for (int i = 0; i < row.length; i++) if (row[i] != null) p.getInventory().setItem(9 + i, row[i]);
            p.getInventory().setItem(0, charged(built(steelBow, sinew, rod), com.mechanical.Smithing.STEEL_BOLT.get()));
        }, () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(4);
            mc.resizeGui();
            show(new InventoryScreen(mc.player));
        }, null);
        inWorld("crossbow_held", p -> {}, () -> {
            mc.gui.setScreen(null);
            mc.player.getInventory().setSelectedSlot(0);
        }, null);
    }

    private static void skills() {
        Minecraft mc = Minecraft.getInstance();
        for (int[] size : new int[][] {{1920, 1080, 3}, {854, 480, 2}}) {
            inWorld(size[2] == 3 ? "skills_screen" : "skills_screen_small", GuiShots::skilled, () -> {
                mc.getWindow().setWindowed(size[0], size[1]);
                mc.options.guiScale().set(size[2]);
                mc.resizeGui();
                show(new com.mechanical.client.skills.SkillsScreen(Skill.MINING));
            }, null);
        }
        // Mining 4 XP short of 14: two gains of 10 merge into one popup that says the new level
        inWorld("skills_toast", p -> {
            skilled(p);
            var xp = new java.util.HashMap<>(p.getData(Skills.XP.get()));
            xp.put(Skill.MINING.key(), Skills.xpForLevel(14) - 4);
            p.setData(Skills.XP.get(), xp);
        }, () -> {
            mc.getWindow().setWindowed(1920, 1080);
            mc.options.guiScale().set(3);
            mc.resizeGui();
        }, mine(48, 10));
        inWorld("skills_toast_gain", GuiShots::skilled, () -> {}, mine(48, 10));
    }

    private static net.minecraft.client.gui.components.EditBox searchBox() {
        for (var child : Minecraft.getInstance().gui.screen().children())
            if (child instanceof net.minecraft.client.gui.components.EditBox box) return box;
        return null;
    }

    /** A key pressed and let go through the keyboard handler, as a player would, with its letter typed when {@code ch} is set. */
    private static void typeKey(int key, int ch) {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().handle();
        var event = new net.minecraft.client.input.KeyEvent(key, 0, 0);
        mc.keyboardHandler.keyPress(window, 1, event);
        if (ch > 0) mc.keyboardHandler.charTyped(window, new net.minecraft.client.input.CharacterEvent(ch));
        mc.keyboardHandler.keyPress(window, 0, event);
    }

    private static net.minecraft.client.input.MouseButtonEvent mouseEvent(double x, double y) {
        return new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
    }

    private static void drag(Screen screen, double x, double y, double toX, double toY) {
        screen.mouseClicked(mouseEvent(x, y), false);
        if (x != toX || y != toY) screen.mouseDragged(mouseEvent(toX, toY), toX - x, toY - y);
        screen.mouseReleased(mouseEvent(toX, toY));
    }

    private static void report(String what, com.mechanical.client.quests.QuestScreen s) {
        System.out.println("QUESTSHOT " + what + ": " + s.camera() + " picked " + s.pickedId());
    }

    /** Presses the button labelled {@code label}, however deep in the screen's layout. */
    private static void press(Screen screen, String label) {
        for (var child : screen.children())
            if (child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(label)) {
                b.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                return;
            }
    }

    private static void filter(int from) {
        if (only.length > 0)
            SHOTS.subList(from, SHOTS.size()).removeIf(s -> java.util.Arrays.stream(only).noneMatch(s.name()::startsWith));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (stage == -1) {
                File flag = new File(mc.gameDirectory, "guishots.flag");
                if (!flag.exists()) {
                    stage = -2;
                    return;
                }
                List<String> lines = Files.readAllLines(flag.toPath());
                out = new File(lines.get(0).trim());
                out.mkdirs();
                if (lines.size() > 1 && !lines.get(1).isBlank()) only = lines.get(1).trim().split(",");
                stage = 0;
            }
            if (stage < 0) return;
            if (stage == 0) {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.gui.setScreen(new TitleScreen());
                if (!(mc.gui.screen() instanceof TitleScreen) || mc.gui.overlay() != null) return;
                if (++ticks < 60) return;
                menus();
                filter(0);
                mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                mc.options.pauseOnLostFocus = false;
                stage = 1;
                ticks = 0;
                return;
            }
            if (stage == 1 && step >= SHOTS.size()) { // the menus are done: make the world
                stage = 2;
                ticks = 0;
                String name = "GuiShots" + System.currentTimeMillis() % 100000;
                mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings(name, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(77L, false, false),
                        reg -> reg.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(only.length == 1 && only[0].equals("map_") ? WorldPresets.NORMAL : WorldPresets.FLAT)
                                .value().createWorldDimensions(),
                        new TitleScreen());
                return;
            }
            if (stage == 2) {
                if (mc.player == null || mc.getSingleplayerServer() == null) return;
                if (++ticks < 100) return;
                mc.gui.setScreen(null);
                int menus = SHOTS.size();
                worldShots();
                filter(menus);
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    for (String cmd : List.of("time set noon", "gamerule advance_time false", "weather clear", "gamerule spawn_mobs false",
                            "gamerule send_command_feedback false")) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
                    }
                    ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                    int ground = p.level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                    stand = new Vec3(0.5, ground, 0.5);
                    spot = BlockPos.containing(0.5, ground, 2.5);
                    p.teleportTo(p.level(), stand.x, stand.y, stand.z, java.util.Set.of(), 0, 20, true);
                });
                stage = 3;
                ticks = 0;
                return;
            }
            if (step >= SHOTS.size()) {
                mc.stop();
                return;
            }
            if (waiting) return;
            Shot shot = SHOTS.get(step);
            ticks++;
            mc.gui.toastManager().clear();
            var server = mc.getSingleplayerServer();
            if (ticks == 1 && shot.world()) {
                server.execute(() -> {
                    try {
                        shot.server().accept(server.getPlayerList().getPlayers().get(0));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
            } else if (ticks == 20) {
                shot.client().run();
            } else if (ticks >= 40) {
                int[] at = shot.hover() == null ? null : shot.hover().get();
                mouse(at == null ? new int[] {2, 2} : at);
                if (ticks == (shot.name().startsWith("box_furnace") ? 130 : shot.name().endsWith("_bought") ? 45 : 60)) {
                    Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                        try (image) {
                            image.writeToFile(new File(out, shot.name() + ".png"));
                        } catch (Exception ignored) {}
                    });
                } else if (ticks > (shot.name().startsWith("box_furnace") ? 136 : 66)) {
                    boolean keep = step + 1 < SHOTS.size() && shot.name().startsWith("trade_") && SHOTS.get(step + 1).name().startsWith(shot.name() + "_");
                    if (!keep && shot.world()) mc.gui.setScreen(null);
                    step++;
                    ticks = 0;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            mc.stop();
        }
    }

    /** Puts the mouse at a GUI position (the cursor itself stays where it is). */
    private static void mouse(int[] gui) {
        Minecraft mc = Minecraft.getInstance();
        try {
            var window = mc.getWindow();
            for (String[] f : new String[][] {{"xpos", "0"}, {"ypos", "1"}}) {
                Field field = mc.mouseHandler.getClass().getDeclaredField(f[0]);
                field.setAccessible(true);
                int i = Integer.parseInt(f[1]);
                field.setDouble(mc.mouseHandler, gui[i] * (double) (i == 0 ? window.getScreenWidth() : window.getScreenHeight())
                        / (i == 0 ? window.getGuiScaledWidth() : window.getGuiScaledHeight()));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private GuiShots() {}
}
