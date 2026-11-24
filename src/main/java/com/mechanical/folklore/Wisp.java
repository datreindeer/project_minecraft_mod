package com.mechanical.folklore;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Lyktgubbe (Will-o'-the-wisp) over bog and dark forest at night, and its jungle kin the Jungle Spirit Mask: a light (or a
 * floating carved mask) that stays {@link #LEAD} blocks ahead of whoever follows, always toward the place it chose when it
 * appeared, {@link #MIN_TRIP}-{@link #MAX_TRIP} blocks away. Most lead astray: there it goes out and the followers are
 * Lost. Some ({@link #treasureChance}) lead to treasure: the wisp to a sunken Bog Grave (a chest, loot
 * chests/bog_grave, with a Wisp Ember), the mask to a Spirit Mask lying on the ground. Seen with sight
 * {@link Faith#WISP_SIGHT} (the mask {@link Faith#HULDRA_SIGHT}); the client draws the light as particles.
 */
public class Wisp extends Spirit {
    static final ResourceKey<LootTable> BOG_GRAVE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/bog_grave"));
    static final double LEAD = 9, FOLLOW_RANGE = 20, ARRIVED = 2, STEP = 0.18;
    static final int MIN_TRIP = 24, MAX_TRIP = 48, LOST_TICKS = 20 * 60, ALONE_TICKS = 1200;
    private Vec3 goal;
    private boolean treasure;
    private int alone;

    public Wisp(EntityType<? extends Wisp> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    boolean mask() {
        return getType() == Beings.JUNGLE_SPIRIT_MASK.get();
    }

    float treasureChance() {
        return mask() ? 0.2F : 0.3F;
    }

    @Override
    public float sight() {
        return mask() ? Faith.HULDRA_SIGHT : Faith.WISP_SIGHT;
    }

    @Override
    String kind() {
        return mask() ? "jungle_spirit_mask" : "will_o_wisp";
    }

    /** Night, open sky, rare; autumn nights (the wisp's season) twice as often. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevelAccessor s)) return false;
        ServerLevel l = s.getLevel();
        return rule(l.isDarkOutside(), level.canSeeSky(pos), Calendar.season(l.getOverworldClockTime()) == 2, random.nextFloat());
    }

    static boolean rule(boolean night, boolean sky, boolean autumn, float roll) {
        return night && sky && roll < (autumn ? 0.3F : 0.15F);
    }

    /** Where it leads, and whether there is anything there. */
    void choose(boolean treasure, Vec3 goal) {
        this.treasure = treasure;
        this.goal = goal;
    }

    Vec3 goal() {
        if (goal == null) {
            RandomSource r = getRandom();
            double a = r.nextDouble() * Math.PI * 2, d = MIN_TRIP + r.nextDouble() * (MAX_TRIP - MIN_TRIP);
            choose(r.nextFloat() < treasureChance(), position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
        }
        return goal;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // the wisp's light is its orb (BeingsClient); a few pale motes drift off it
            if (seenBy(Faith.localPlayer.get()) && random.nextInt(mask() ? 4 : 5) == 0)
                level().addParticle(mask() ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.GLOW, getX() + (random.nextDouble() - 0.5) * 0.3,
                        getY() + 0.2 + random.nextDouble() * 0.2, getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (level.isBrightOutside()) {
            vanish();
            return;
        }
        Vec3 g = goal();
        Player p = nearestSeer(FOLLOW_RANGE);
        alone = p == null ? alone + 1 : 0;
        if (alone > ALONE_TICKS) {
            vanish();
            return;
        }
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(getX()), (int) Math.floor(getZ()));
        double bob = Math.sin((tickCount + getId()) * 0.1) * 0.25;
        Vec3 step = Vec3.ZERO;
        if (p != null && !isNoAi() && p.distanceTo(this) < LEAD) step = g.subtract(position()).horizontal().normalize().scale(STEP);
        setPos(getX() + step.x, getY() + (ground + 1.2 + bob - getY()) * 0.2, getZ() + step.z);
        if (position().horizontal().distanceTo(g.horizontal()) < ARRIVED) arrive(level, level.getPlayers(pl -> pl.distanceTo(this) < FOLLOW_RANGE && seenBy(pl)));
    }

    /** At the end of the trail: treasure for the followers, or nothing and they are Lost. */
    void arrive(ServerLevel level, java.util.List<ServerPlayer> followers) {
        // a Will-o'-Wisp Lily carried by a follower answers the light: it leads true, and the lily is spent
        ServerPlayer lily = mask() ? null : followers.stream().filter(Wisp::carriesLily).findFirst().orElse(null);
        if (!treasure && lily != null) {
            spendLily(lily);
            treasure = true;
            Faith.award(lily, "folklore/wisp_lily_true");
        }
        if (treasure) {
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition());
            if (mask()) {
                ItemEntity item = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, Beings.SPIRIT_MASK.toStack(), 0, 0, 0);
                item.setUnlimitedLifetime();
                level.addFreshEntity(item);
            } else {
                bogGrave(level, at);
            }
            for (ServerPlayer pl : followers) Faith.award(pl, "folklore/follow_the_light");
        } else {
            for (ServerPlayer pl : followers) {
                pl.addEffect(new MobEffectInstance(Beings.LOST, LOST_TICKS, 0));
                pl.sendOverlayMessage(Component.translatable("message.mechanical.wisp.astray").withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        vanish();
    }

    /** A sunken grave at the surface pos: a chest one down with the bog's old things, mossy stones and a skull-bone around. */
    static BlockPos bogGrave(ServerLevel level, BlockPos surface) {
        BlockPos chest = surface.below();
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity c) c.setLootTable(BOG_GRAVE, level.getRandom().nextLong());
        level.setBlockAndUpdate(surface, Blocks.MOSS_CARPET.defaultBlockState());
        return chest;
    }

    /** A Will-o'-Wisp Lily in either hand. */
    static boolean carriesLily(net.minecraft.world.entity.player.Player p) {
        return p.getMainHandItem().is(com.mechanical.Flora.WILL_O_WISP_LILY.get().asItem()) || p.getOffhandItem().is(com.mechanical.Flora.WILL_O_WISP_LILY.get().asItem());
    }

    static void spendLily(net.minecraft.world.entity.player.Player p) {
        (p.getMainHandItem().is(com.mechanical.Flora.WILL_O_WISP_LILY.get().asItem()) ? p.getMainHandItem() : p.getOffhandItem()).consume(1, p);
    }

    boolean treasure() {
        return treasure;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("treasure", treasure);
        if (goal != null) {
            output.putDouble("goal_x", goal.x);
            output.putDouble("goal_z", goal.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        treasure = input.getBooleanOr("treasure", false);
        double x = input.getDoubleOr("goal_x", Double.NaN), z = input.getDoubleOr("goal_z", Double.NaN);
        if (!Double.isNaN(x)) goal = new Vec3(x, 0, z);
    }
}
