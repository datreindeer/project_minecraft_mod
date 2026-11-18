package com.mechanical.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** A drop of blood: flies off with the given velocity, falls, and is gone when it lands. Uses the vanilla drip sprite, tinted. */
public class BloodDropParticle extends SingleQuadParticle {
    BloodDropParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.06F;
        this.lifetime = 40 + random.nextInt(20);
        this.quadSize = 0.06F + random.nextFloat() * 0.05F;
        float shade = 0.75F + random.nextFloat() * 0.25F;
        setColor(0.55F * shade, 0.03F * shade, 0.03F * shade);
        setSize(0.01F, 0.01F);
    }

    @Override
    public Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (lifetime-- <= 0 || onGround) {
            remove();
            return;
        }
        yd -= gravity;
        move(xd, yd, zd);
        xd *= 0.98F;
        yd *= 0.98F;
        zd *= 0.98F;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
            return new BloodDropParticle(level, x, y, z, xd, yd, zd, sprites.get(random));
        }
    }
}
