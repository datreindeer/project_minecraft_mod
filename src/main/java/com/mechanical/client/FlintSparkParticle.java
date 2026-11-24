package com.mechanical.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A spark of hot steel shaved off by the flint: flies off with the given velocity, falls, and cools as it goes from white-yellow
 * through orange to a dull red, shrinking, glowing at full brightness until it dies (or on the ground, after a short skid).
 */
public class FlintSparkParticle extends SingleQuadParticle {
    private final float startSize;

    FlintSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.035F;
        this.lifetime = 7 + random.nextInt(10);
        this.startSize = 0.016F + random.nextFloat() * 0.016F;
        this.quadSize = startSize;
        this.hasPhysics = true;
        setSize(0.01F, 0.01F);
        cool(0.0F);
    }

    /** Colour by age 0..1: white-hot, yellow, orange, dull red. */
    private void cool(float t) {
        float r = 1.0F, g, b;
        if (t < 0.25F) {
            g = Mth.lerp(t / 0.25F, 0.97F, 0.82F);
            b = Mth.lerp(t / 0.25F, 0.78F, 0.30F);
        } else if (t < 0.65F) {
            g = Mth.lerp((t - 0.25F) / 0.4F, 0.82F, 0.45F);
            b = Mth.lerp((t - 0.25F) / 0.4F, 0.30F, 0.08F);
        } else {
            r = Mth.lerp((t - 0.65F) / 0.35F, 1.0F, 0.62F);
            g = Mth.lerp((t - 0.65F) / 0.35F, 0.45F, 0.16F);
            b = 0.05F;
        }
        setColor(r, g, b);
    }

    @Override
    public Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 15728880; // glows
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float t = age / (float) lifetime;
        cool(t);
        quadSize = startSize * (1.0F - 0.6F * t);
        yd -= gravity;
        move(xd, yd, zd);
        if (onGround) { // skids and dies quickly
            xd *= 0.5F;
            zd *= 0.5F;
            yd = 0.0F;
            age = Math.max(age, lifetime - 2);
        }
        xd *= 0.94F;
        yd *= 0.96F;
        zd *= 0.94F;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
            return new FlintSparkParticle(level, x, y, z, xd, yd, zd, sprites.get(random));
        }
    }
}
