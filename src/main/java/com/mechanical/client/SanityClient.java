package com.mechanical.client;

import java.util.ArrayDeque;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mechanical.Mechanical;
import com.mechanical.Sanity;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * What low sanity (see {@link Sanity}) does to your senses. Everything here is client-only make-believe: nothing is
 * really there and nothing hurts you.
 * <ul>
 * <li>below 70: whispers, footsteps and twig snaps behind you; a faint vignette; colours start to drain</li>
 * <li>below 40: dark figures at the edge of view that vanish when looked at, a heartbeat, a slow swaying view,
 * fake monster sounds</li>
 * <li>below 15: heavy vignette, a hallucinated monster that dissolves when hit or approached, fake hurt jolts</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SanityClient {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "sanity");
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/misc/sanity_vignette.png");
    private static final Identifier[] SHADOWS = {sprite("shadow_0"), sprite("shadow_1"), sprite("shadow_2")};
    private static final Vector3fc GREY = new Vector3f(0.5F);
    private static final int FAKE_ID = -1_337_042; // client-only entity id the server never hands out (its ids are positive)

    private record Scheduled(int tick, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {}

    /** A figure seen from the corner of the eye: a world direction (yaw) at the edge of the view. */
    private static final class Shadow {
        final float yaw;
        final Identifier sprite;
        int age;
        float alpha;
        boolean fading;

        Shadow(float yaw, Identifier sprite) {
            this.yaw = yaw;
            this.sprite = sprite;
        }
    }

    private static final ArrayDeque<Scheduled> QUEUE = new ArrayDeque<>();
    private static int ticks, nextEvent = 400, nextBeat;
    /** Sanity as shown, eased towards the synced value so the effects fade instead of jumping. */
    private static float shown = Sanity.MAX;
    private static Shadow shadow;
    private static Mob fake;
    private static int fakeAge, fuse;

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "hud/sanity/" + name);
    }

    /** 0 at 70 sanity or more, rising to 1 at 0. */
    private static float madness() {
        return Math.clamp((Sanity.UNEASY - shown) / Sanity.UNEASY, 0, 1);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            QUEUE.clear();
            shadow = null;
            fake = null;
            shown = Sanity.MAX;
            return;
        }
        if (mc.isPaused()) return;
        float real = player.isCreative() || player.isSpectator() ? Sanity.MAX : Sanity.of(player);
        shown = Mth.approach(shown, real, 0.25F);
        ticks++;
        while (!QUEUE.isEmpty() && QUEUE.peek().tick() <= ticks) {
            Scheduled s = QUEUE.poll();
            mc.level.playLocalSound(s.pos().x, s.pos().y, s.pos().z, s.sound(), s.source(), s.volume(), s.pitch(), false);
        }
        RandomSource random = player.getRandom();
        tickFake(mc, player);
        tickShadow(mc, player, random);
        if (shown >= Sanity.UNEASY) return;

        if (shown < Sanity.AFRAID && ticks >= nextBeat) { // heartbeat, faster and louder the lower it gets
            float fear = (Sanity.AFRAID - shown) / Sanity.AFRAID;
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.25F + 0.6F * fear, 0.9F + 0.2F * fear, false);
            nextBeat = ticks + (int) (40 - 20 * fear);
        }
        if (ticks < nextEvent) return;
        nextEvent = ticks + 160 + (int) (shown * 12) + random.nextInt(240); // ~45-65 s at 70, 8-20 s at 0
        int kinds = shown < Sanity.MAD ? 9 : shown < Sanity.AFRAID ? 7 : 3;
        trick(mc, player, random, random.nextInt(kinds));
    }

    /** Somewhere behind the player (within 50 degrees of straight behind), {@code dist} blocks away. */
    private static Vec3 behind(LocalPlayer player, RandomSource random, double dist) {
        double yaw = Math.toRadians(player.getYRot() + 180 + (random.nextFloat() - 0.5F) * 100);
        return player.position().add(-Math.sin(yaw) * dist, 0.5, Math.cos(yaw) * dist);
    }

    private static void play(int delay, Vec3 at, SoundEvent sound, SoundSource source, float volume, float pitch) {
        QUEUE.add(new Scheduled(ticks + delay, at, sound, source, volume, pitch));
    }

    private static void trick(Minecraft mc, LocalPlayer player, RandomSource random, int kind) {
        switch (kind) {
            case 0 -> { // whispers: cave sound and soul wisps, pitched down, close behind
                Vec3 at = behind(player, random, 2 + random.nextDouble() * 3);
                play(0, at, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.5F, 0.5F + random.nextFloat() * 0.2F);
                for (int i = 0; i < 4; i++) play(6 + i * 7, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0F, 0.5F + random.nextFloat() * 0.15F);
            }
            case 1 -> { // footsteps following you, stopping when you would turn
                Vec3 from = behind(player, random, 7);
                Vec3 step = player.position().subtract(from).normalize().scale(0.6);
                int steps = 3 + random.nextInt(4);
                for (int i = 0; i < steps; i++) play(i * 8, from.add(step.scale(i)), SoundEvents.GRASS_STEP, SoundSource.AMBIENT, 0.4F, 0.7F + random.nextFloat() * 0.1F);
            }
            case 2 -> play(0, behind(player, random, 4 + random.nextDouble() * 4), SoundEvents.SCAFFOLDING_BREAK, SoundSource.AMBIENT, 0.6F, 0.55F); // twig snap
            case 3 -> play(0, behind(player, random, 6 + random.nextDouble() * 8), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 0.6F, 0.8F + random.nextFloat() * 0.2F);
            case 4 -> play(0, behind(player, random, 2.5), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 0.8F, 0.5F); // the hiss is the scary one
            case 5 -> play(0, behind(player, random, 6 + random.nextDouble() * 8), SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 0.6F, 0.8F + random.nextFloat() * 0.2F);
            case 6 -> play(0, behind(player, random, 4 + random.nextDouble() * 6), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 0.6F, 0.9F);
            case 7 -> { // a jolt as if hit: hurt tilt and sound, no damage
                player.animateHurt(random.nextFloat() * 360);
                mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8F, 0.9F + random.nextFloat() * 0.2F, false);
            }
            default -> spawnFake(mc, player, random);
        }
    }

    // ---- shadows at the edge of view ----

    /** Half the horizontal field of view in degrees. */
    private static float halfFovX(Minecraft mc) {
        double halfY = Math.toRadians(mc.options.fov().get() / 2.0);
        double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        return (float) Math.toDegrees(Math.atan(Math.tan(halfY) * aspect));
    }

    private static void tickShadow(Minecraft mc, LocalPlayer player, RandomSource random) {
        if (shadow == null) {
            if (shown < Sanity.AFRAID && random.nextInt(200 + (int) (shown * 15)) == 0) { // ~every 10-40 s
                float edge = halfFovX(mc) - 7;
                shadow = new Shadow(player.getYRot() + (random.nextBoolean() ? edge : -edge), SHADOWS[random.nextInt(SHADOWS.length)]);
            }
            return;
        }
        shadow.age++;
        float rel = Math.abs(Mth.wrapDegrees(shadow.yaw - player.getYRot()));
        if (rel < halfFovX(mc) - 18 || shadow.age > 80 || shown >= Sanity.AFRAID) shadow.fading = true; // looked at: gone
        shadow.alpha = shadow.fading ? shadow.alpha - 0.15F : Math.min(0.6F, shadow.alpha + 0.03F);
        if (shadow.fading && shadow.alpha <= 0) shadow = null;
    }

    // ---- the hallucinated monster ----

    private static void spawnFake(Minecraft mc, LocalPlayer player, RandomSource random) {
        if (fake != null) return;
        EntityType<? extends Mob> type = switch (random.nextInt(3)) {
            case 0 -> EntityTypes.SKELETON;
            case 1 -> EntityTypes.CREEPER;
            default -> EntityTypes.ZOMBIE;
        };
        for (int attempt = 0; attempt < 10; attempt++) { // a dark spot 9-14 blocks off, with ground under it
            double angle = random.nextDouble() * Math.PI * 2, dist = 9 + random.nextDouble() * 5;
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * dist, player.getY(), player.getZ() + Math.sin(angle) * dist);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos pos = column.above(dy);
                if (!mc.level.getBlockState(pos).isAir() || !mc.level.getBlockState(pos.above()).isAir() || !mc.level.getBlockState(pos.below()).isSolid()) continue;
                Mob mob = type.create(mc.level, EntitySpawnReason.LOAD);
                if (mob == null) return;
                mob.setId(FAKE_ID);
                mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                facePlayer(mob, player);
                mc.level.addEntity(mob);
                fake = mob;
                fakeAge = 0;
                fuse = -1;
                return;
            }
        }
    }

    private static void facePlayer(Mob mob, LocalPlayer player) {
        float yaw = (float) Math.toDegrees(Math.atan2(player.getZ() - mob.getZ(), player.getX() - mob.getX())) - 90;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
    }

    private static void tickFake(Minecraft mc, LocalPlayer player) {
        if (fake == null) return;
        if (fake.level() != mc.level || fake.isRemoved()) {
            fake = null;
            return;
        }
        fakeAge++;
        facePlayer(fake, player); // it just stands there and stares
        double dist = fake.distanceTo(player);
        if (fake instanceof Creeper && dist < 7 && fuse < 0) { // hisses, then dissolves 2 s later (unless reached first)
            mc.level.playLocalSound(fake.getX(), fake.getY(), fake.getZ(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 0.5F, false);
            fuse = 40;
        }
        if (fuse > 0 && --fuse == 0 || dist < 4 || fakeAge > 1200 || shown >= Sanity.MAD + 5) dissolve(mc);
    }

    private static void dissolve(Minecraft mc) {
        ClientLevel level = mc.level;
        if (fake == null || level == null) return;
        RandomSource random = level.getRandom();
        for (int i = 0; i < 30; i++) {
            level.addParticle(random.nextBoolean() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SQUID_INK,
                    fake.getX() + random.nextGaussian() * 0.3, fake.getY() + random.nextDouble() * fake.getBbHeight(), fake.getZ() + random.nextGaussian() * 0.3,
                    random.nextGaussian() * 0.02, 0.03, random.nextGaussian() * 0.02);
        }
        level.playLocalSound(fake.getX(), fake.getY(), fake.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.5F, 0.6F, false);
        level.removeEntity(FAKE_ID, Entity.RemovalReason.DISCARDED);
        fake = null;
    }

    /** Hitting (or using) the hallucination: it bursts into smoke, nothing is sent to the server. */
    @SubscribeEvent
    static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (fake == null || mc.crosshairPickEntity != fake) return;
        event.setCanceled(true);
        event.setSwingHand(event.isAttack());
        dissolve(mc);
    }

    // ---- view ----

    /** Below 40 the view sways slowly, up to 2.5 degrees of roll at 0. */
    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shown >= Sanity.AFRAID) return;
        float amount = (Sanity.AFRAID - shown) / Sanity.AFRAID;
        float t = (float) ((ticks + event.getPartialTick()) * 0.04);
        event.setRoll(event.getRoll() + Mth.sin(t) * 2.5F * amount);
        event.setPitch(event.getPitch() + Mth.sin(t * 0.7F + 1) * 0.8F * amount);
    }

    /** Called with BloodMoonClient.addLayers (ClientLevel mixin): the colours drain towards grey, up to 60 %. */
    public static void addLayers(EnvironmentAttributeSystem.Builder builder) {
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (c, tick) -> grey(c));
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (c, tick) -> grey(c));
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_COLOR, (c, tick) -> grey(c));
        builder.addTimeBasedLayer(EnvironmentAttributes.BLOCK_LIGHT_TINT, (c, tick) -> grey(c));
    }

    private static Vector3fc grey(Vector3fc c) {
        float k = madness() * 0.6F;
        if (k <= 0) return c;
        float luma = 0.299F * c.x() + 0.587F * c.y() + 0.114F * c.z();
        return new Vector3f(c).lerp(new Vector3f(luma), k);
    }

    // ---- HUD layer: vignette, shadows, the eye ----

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || shown >= Sanity.UNEASY) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        float m = madness();
        float pulse = shown < Sanity.AFRAID ? 0.06F * Mth.sin((ticks + delta.getGameTimeDeltaPartialTick(false)) * 0.3F) : 0;
        float v = Math.clamp(0.15F + m * 0.8F + pulse, 0, 1); // 0.15 just below 70, 0.95 at 0
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, w, h, w, h, ARGB.colorFromFloat(1, v, v, v));

        if (shadow != null && shadow.alpha > 0) {
            float halfX = halfFovX(mc);
            float rel = Mth.wrapDegrees(shadow.yaw - mc.player.getYRot());
            float x = w / 2F + (float) (Math.tan(Math.toRadians(rel)) / Math.tan(Math.toRadians(halfX))) * w / 2F;
            double halfY = Math.toRadians(mc.options.fov().get() / 2.0);
            float horizon = h / 2F - (float) (Math.tan(Math.toRadians(mc.player.getXRot())) / Math.tan(halfY)) * h / 2F;
            int sh = (int) (h * 0.6F), sw = sh / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, shadow.sprite, (int) x - sw / 2, (int) (horizon - sh * 0.25F), sw, sh, shadow.alpha);
        }

        String stage = shown <= 0.5F ? "eye_broken" : shown < Sanity.MAD ? "eye_mad" : shown < Sanity.AFRAID ? "eye_afraid" : "eye_uneasy";
        int shake = shown < Sanity.AFRAID && mc.player.getRandom().nextInt(4) == 0 ? mc.player.getRandom().nextInt(3) - 1 : 0;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(stage), w / 2 + 91 + 6 + shake, h - 19, 16, 16, 0.85F);
    }

    private SanityClient() {}
}
