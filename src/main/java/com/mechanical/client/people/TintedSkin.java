package com.mechanical.client.people;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;

/**
 * A tribesperson's skin in their tribe's colour: the skin PNG with every pixel its "_tint" mask covers replaced by the
 * mask's grey times the dye colour (headbands, sashes, coat bands, loincloths: scripts/gen_tribe_skins.py paints both).
 * Made once per skin and colour, on first use; a resource reload remakes it like any other texture.
 */
final class TintedSkin extends ReloadableTexture {
    private static final Set<Identifier> MADE = new HashSet<>();
    private final Identifier base;
    private final int colour;

    private TintedSkin(Identifier id, Identifier base, int colour) {
        super(id);
        this.base = base;
        this.colour = colour;
    }

    /** The id of {@code base} dyed {@code dye} (a DyeColor id), registering the texture the first time; base itself for -1. */
    static Identifier of(Identifier base, int dye) {
        if (dye < 0) return base;
        DyeColor colour = DyeColor.byId(dye);
        Identifier id = base.withPath(p -> p.replace(".png", "_dyed_" + colour.getSerializedName() + ".png"));
        if (MADE.add(id)) Minecraft.getInstance().getTextureManager().registerAndLoad(id, new TintedSkin(id, base, colour.getTextureDiffuseColor()));
        return id;
    }

    @Override
    public TextureContents loadContents(ResourceManager resources) throws IOException {
        NativeImage image = read(resources.getResourceOrThrow(base));
        Optional<Resource> mask = resources.getResource(base.withPath(p -> p.replace(".png", "_tint.png")));
        if (mask.isPresent()) {
            try (NativeImage m = read(mask.get())) {
                for (int y = 0; y < Math.min(image.getHeight(), m.getHeight()); y++) {
                    for (int x = 0; x < Math.min(image.getWidth(), m.getWidth()); x++) {
                        int t = m.getPixel(x, y);
                        if (ARGB.alpha(t) == 0) continue;
                        int grey = ARGB.red(t);
                        int c = ARGB.color(ARGB.alpha(image.getPixel(x, y)), ARGB.red(colour) * grey / 255, ARGB.green(colour) * grey / 255, ARGB.blue(colour) * grey / 255);
                        image.setPixel(x, y, c);
                    }
                }
            }
        }
        return new TextureContents(image, null);
    }

    private static NativeImage read(Resource resource) throws IOException {
        try (InputStream in = resource.open()) {
            return NativeImage.read(in);
        }
    }
}
