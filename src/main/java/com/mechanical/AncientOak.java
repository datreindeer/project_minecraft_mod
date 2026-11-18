package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Ancient Oak: a very rare, unbreakable giant oak (worldgen in {@link com.mechanical.worldgen.AncientOakFeature}) with an
 * Offering Altar in its hollow trunk. Offering a Giant Antler there gives one of five mythic items, whose effects live here.
 * Data, models and textures: scripts/gen_ancient_oak.py.
 */
@EventBusSubscriber(modid = MODID)
public final class AncientOak {
    // ---- the tree: unbreakable like bedrock, never decays, not pushable, kept out of the logs/leaves tags ----
    private static BlockBehaviour.Properties enchanted(BlockBehaviour.Properties p, MapColor color, SoundType sound) {
        return p.mapColor(color).strength(-1.0F, 3600000.0F).noLootTable().sound(sound).pushReaction(PushReaction.IMMOVEABLE)
                .isValidSpawn((s, g, pos, e) -> false);
    }

    public static final DeferredBlock<Log> LOG = BLOCKS.registerBlock("ancient_oak_log", Log::new, p -> enchanted(p, MapColor.WOOD, SoundType.WOOD));
    public static final DeferredBlock<Log> WOOD = BLOCKS.registerBlock("ancient_oak_wood", Log::new, p -> enchanted(p, MapColor.WOOD, SoundType.WOOD));
    public static final DeferredBlock<Branch> BRANCH = BLOCKS.registerBlock("ancient_oak_branch", p -> new Branch(8, p),
            p -> enchanted(p, MapColor.WOOD, SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<Branch> TWIG = BLOCKS.registerBlock("ancient_oak_twig", p -> new Branch(4, p),
            p -> enchanted(p, MapColor.WOOD, SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<Leaves> LEAVES = BLOCKS.registerBlock("ancient_oak_leaves", Leaves::new,
            p -> enchanted(p, MapColor.PLANT, SoundType.GRASS).noOcclusion().isSuffocating((s, g, pos) -> false)
                    .isViewBlocking((s, g, pos, box) -> false).isRedstoneConductor((s, g, pos) -> false));
    public static final DeferredBlock<Altar> ALTAR = BLOCKS.registerBlock("offering_altar", Altar::new,
            p -> enchanted(p, MapColor.STONE, SoundType.STONE).noOcclusion().lightLevel(s -> s.getValue(Altar.ACTIVE) ? 14 : 5));
    public static final Supplier<BlockEntityType<OfferingAltarBlockEntity>> ALTAR_ENTITY = Mechanical.BLOCK_ENTITIES.register("offering_altar",
            () -> new BlockEntityType<>(OfferingAltarBlockEntity::new, ALTAR.get()));

    /** What the altar accepts (the Giant Antler, as an optional entry so this loads without it). */
    public static final TagKey<Item> ALTAR_OFFERINGS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "altar_offerings"));

    // ---- the five mythic items ----
    /** Mythic item names fade from forest green to gold, letter by letter ({@code phase} shifts the fade for a shimmer). */
    public static final int MYTHIC_GREEN = 0x5FD35A, MYTHIC_GOLD = 0xF2C14E;

    public static MutableComponent mythicName(String text, float phase) {
        MutableComponent out = Component.empty();
        int n = Math.max(1, text.length() - 1);
        for (int i = 0; i < text.length(); i++) {
            // a triangle wave: green -> gold -> green across the name, sliding with the phase
            float t = Math.abs(((i / (float) n + phase) % 1.0F) * 2.0F - 1.0F);
            int r = lerp(MYTHIC_GREEN >> 16, MYTHIC_GOLD >> 16, t), g = lerp(MYTHIC_GREEN >> 8 & 255, MYTHIC_GOLD >> 8 & 255, t),
                    b = lerp(MYTHIC_GREEN & 255, MYTHIC_GOLD & 255, t);
            out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor(r << 16 | g << 8 | b).withBold(true)));
        }
        return out;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    /** English display names of the mythic items (their names are drawn as a colour fade, so not translated). */
    public static final java.util.Map<String, String> MYTHIC_NAMES = java.util.Map.of("thunderstone", "Thunderstone",
            "mistletoe_charm", "Mistletoe Charm", "ancient_amber", "Ancient Amber", "heart_of_the_oak", "Heart of the Oak",
            "antler_crown", "Antler Crown");

    private static Item.Properties mythic(Item.Properties p, String name) {
        return p.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ITEM_NAME, mythicName(MYTHIC_NAMES.get(name), 0.0F))
                .component(DataComponents.LORE,
                new ItemLore(List.of(Component.translatable("item.mechanical." + name + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC))));
    }

    /** A legendary polished stone: fastest axe in the mod's chopping (2 chops, 95 % success, +4 firewood). */
    public static final ToolMaterial THUNDERSTONE_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 3000, 10.0F, 2.0F, 15,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_thunderstone")));
    public static final DeferredItem<Item> THUNDERSTONE = ITEMS.registerSimpleItem("thunderstone",
            p -> mythic(p.axe(THUNDERSTONE_MATERIAL, 6.0F, -3.0F), "thunderstone"));
    public static final DeferredItem<Item> MISTLETOE_CHARM = ITEMS.registerSimpleItem("mistletoe_charm", p -> mythic(p, "mistletoe_charm"));
    public static final DeferredItem<Item> ANCIENT_AMBER = ITEMS.registerSimpleItem("ancient_amber", p -> mythic(p, "ancient_amber"));
    public static final DeferredItem<HeartOfTheOak> HEART_OF_THE_OAK = ITEMS.registerItem("heart_of_the_oak", HeartOfTheOak::new,
            p -> mythic(p, "heart_of_the_oak"));
    public static final ArmorMaterial ANTLER = new ArmorMaterial(40, Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 3,
            ArmorType.LEGGINGS, 3, ArmorType.BOOTS, 3, ArmorType.BODY, 3), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_antler_crown")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "antler_crown")));
    public static final DeferredItem<Item> ANTLER_CROWN = ITEMS.registerSimpleItem("antler_crown",
            p -> mythic(p.humanoidArmor(ANTLER, ArmorType.HELMET), "antler_crown"));

    public static List<DeferredItem<? extends Item>> mythics() {
        return List.of(THUNDERSTONE, MISTLETOE_CHARM, ANCIENT_AMBER, HEART_OF_THE_OAK, ANTLER_CROWN);
    }

    public static void init() {}

    // ---- animals called by the altar ritual: calm (no fleeing, no panic) until a game time ----
    private static final Map<LivingEntity, Long> CALM = new WeakHashMap<>();

    public static void calm(LivingEntity mob, long until) {
        CALM.put(mob, until);
    }

    /** AnimalFear: a calmed animal neither flees nor panics. */
    public static boolean isCalm(LivingEntity mob) {
        Long until = CALM.get(mob);
        return until != null && until > mob.level().getGameTime();
    }

    // ---- hooks for other systems ----

    /** AnimalFear: how close a wild animal lets this player come. Amber held while sneaking: never; antler crown: 3 blocks. */
    public static double noticeRange(Player player, double range) {
        if (player.isCrouching() && holding(player, ANCIENT_AMBER.get())) return -1;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ANTLER_CROWN.get())) return Math.min(range, 3.0);
        return range;
    }

    /** TreeChopping: wood from chopping is doubled while the Heart of the Oak is in the inventory. */
    public static int woodMultiplier(Player player) {
        return player.getInventory().contains(s -> s.is(HEART_OF_THE_OAK.get())) ? 2 : 1;
    }

    /** DynamicLights: the amber glows when held. */
    public static int lightOf(ItemStack stack) {
        return stack.is(ANCIENT_AMBER.get()) ? 10 : 0;
    }

    private static boolean holding(Player player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    /** Mistletoe Charm counts in the hotbar or the offhand. */
    private static boolean hasCharm(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inv.getItem(i).is(MISTLETOE_CHARM.get())) return true;
        return player.getOffhandItem().is(MISTLETOE_CHARM.get());
    }

    // ---- effects ----

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        if (hasCharm(player)) {
            player.removeEffect(MobEffects.POISON);
            if (player.tickCount % 80 == 0 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);
        }
        // night vision kept above 200 ticks so it never flickers; our own (ambient, short) effect is removed with the crown
        MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ANTLER_CROWN.get())) {
            if (nv == null || nv.isAmbient() && nv.getDuration() < 280)
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        } else if (nv != null && nv.isAmbient() && nv.getDuration() <= 300) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @SubscribeEvent
    static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.POISON) && event.getEntity() instanceof Player player && hasCharm(player))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @SubscribeEvent
    static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof Player player && player.getInventory().contains(s -> s.is(THUNDERSTONE.get()))) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onCalmHurt(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() != null) CALM.remove(event.getEntity());
    }

    /** Thunderstone: a melee hit on a monster calls down lightning one time in eight. */
    @SubscribeEvent
    static void onHit(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !(event.getEntity() instanceof Enemy)) return;
        if (event.getSource().is(DamageTypes.LIGHTNING_BOLT) || !(event.getSource().getDirectEntity() instanceof Player player)
                || !player.getMainHandItem().is(THUNDERSTONE.get()) || level.getRandom().nextInt(8) != 0) return;
        var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(event.getEntity().position());
        if (player instanceof ServerPlayer sp) bolt.setCause(sp);
        level.addFreshEntity(bolt);
    }

    // ---- blocks ----

    /** Trunk (axis) and wood (bark all round) of the tree: faint enchantment motes drift off the rune-marked bark. */
    public static class Log extends RotatedPillarBlock {
        public Log(Properties p) {
            super(p);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(40) == 0) {
                Direction d = Direction.getRandom(random);
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + d.getStepX() * 0.6, pos.getY() + random.nextDouble(),
                        pos.getZ() + 0.5 + d.getStepZ() * 0.6, (random.nextDouble() - 0.5) * 0.5, 0.3, (random.nextDouble() - 0.5) * 0.5);
            }
        }
    }

    /** Leaves that never decay, with a rare floating glow mote. */
    public static class Leaves extends Block {
        public Leaves(Properties p) {
            super(p);
        }

        @Override
        protected int getLightDampening(BlockState state) {
            return 1;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(120) == 0 && level.isEmptyBlock(pos.below()))
                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }

    /** Thinner branches (8 px) and twigs (4 px) that connect in six directions like chorus plants; worldgen sets the connections. */
    public static class Branch extends PipeBlock {
        public Branch(float size, Properties p) {
            super(size, p);
            registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                    .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
        }
    }

    /**
     * Stone altar in the hollow: a display for one item. Right-click with anything puts one on it, an empty hand takes it
     * back; a Giant Antler starts the ritual (see {@link OfferingAltarBlockEntity}). ACTIVE (ritual running) brightens it.
     */
    public static class Altar extends Block implements EntityBlock {
        public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
        private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 3, 15), box(3, 3, 3, 13, 9, 13), box(1, 9, 1, 15, 12, 15));

        public Altar(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ACTIVE);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new OfferingAltarBlockEntity(pos, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != ALTAR_ENTITY.get()) return null;
            return level.isClientSide()
                    ? (BlockEntityTicker<T>) (BlockEntityTicker<OfferingAltarBlockEntity>) OfferingAltarBlockEntity::clientTick
                    : (BlockEntityTicker<T>) (BlockEntityTicker<OfferingAltarBlockEntity>) (l, p, s, be) -> OfferingAltarBlockEntity.serverTick((ServerLevel) l, p, s, be);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof OfferingAltarBlockEntity altar) || !altar.item().isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level instanceof ServerLevel server) altar.place(server, stack.consumeAndReturn(1, player));
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof OfferingAltarBlockEntity altar)) return InteractionResult.PASS;
            if (!altar.item().isEmpty() && !player.getMainHandItem().isEmpty()) return InteractionResult.PASS; // taking needs an empty hand
            if (altar.item().isEmpty()) {
                if (level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.offering_altar.waiting"));
            } else if (altar.busy()) {
                if (level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.offering_altar.busy"));
            } else if (!level.isClientSide()) {
                player.getInventory().placeItemBackInInventory(altar.take(), net.minecraft.util.Prediction.SERVER_ONLY);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(8) == 0)
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 2, 0.6 + random.nextDouble(), (random.nextDouble() - 0.5) * 2);
        }
    }

    /** Right-click a sapling to grow it into a tree at once (30 s cooldown, nothing consumed). */
    public static class HeartOfTheOak extends Item {
        public HeartOfTheOak(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            if (!(level.getBlockState(pos).getBlock() instanceof SaplingBlock sapling) || context.getPlayer() == null) return InteractionResult.PASS;
            if (context.getPlayer().getCooldowns().isOnCooldown(context.getItemInHand())) return InteractionResult.FAIL;
            if (level instanceof ServerLevel server) {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
                for (int i = 0; i < 2 && server.getBlockState(pos).getBlock() == sapling; i++)
                    sapling.advanceTree(server, pos, server.getBlockState(pos), server.getRandom());
                if (server.getBlockState(pos).getBlock() != sapling) {
                    server.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
                    context.getPlayer().getCooldowns().addCooldown(context.getItemInHand(), 600);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    private AncientOak() {}
}
