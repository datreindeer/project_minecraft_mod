package com.mechanical.client.folklore;

import java.util.function.Function;

import com.mechanical.Mechanical;
import com.mechanical.client.wildlife.BirdRenderState;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Mountain Watcher: the forest birds' part tree (it flies, glides and perches like the golden eagle) with its own
 * parts on top, from scripts/gen_small_models.py (SmallGeometry.mountainWatcher): six finger feathers at each wing's
 * end that fan wider on still wings and curl up at the tips, a crest that lies back in flight, and two long tail
 * streamers that ripple behind it. Perched, the long wings fold back along the body and the fingers close.
 */
public class MountainWatcherModel extends ForestBirdModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "mountain_watcher"), "main");
    static final int FINGERS = 6;

    private final ModelPart body, leftWing, rightWing, leftTip, rightTip;
    private final ModelPart[] leftFingers = new ModelPart[FINGERS], rightFingers = new ModelPart[FINGERS], crest = new ModelPart[5];
    private final ModelPart[] streamers, streamerEnds;

    public MountainWatcherModel(ModelPart root) {
        super(root, 0.4F, 0.5F, 0.45F);
        Function<String, ModelPart> part = root.createPartLookup();
        body = part.apply("body");
        leftWing = part.apply("left_wing");
        rightWing = part.apply("right_wing");
        leftTip = part.apply("left_wing_tip");
        rightTip = part.apply("right_wing_tip");
        for (int i = 0; i < FINGERS; i++) {
            leftFingers[i] = part.apply("left_finger" + i);
            rightFingers[i] = part.apply("right_finger" + i);
        }
        for (int i = 0; i < crest.length; i++) crest[i] = part.apply("crest" + i);
        streamers = new ModelPart[] {part.apply("left_streamer"), part.apply("right_streamer")};
        streamerEnds = new ModelPart[] {part.apply("left_streamer_end"), part.apply("right_streamer_end")};
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks + state.entityId * 13;
        if (state.flying) {
            float glide = ((BirdRenderState) state).glide;
            // riding the air: the long wings breathe with it and the body rolls a little into its wide circles
            float breathe = Mth.sin(t * 0.06F) * 0.035F * glide;
            leftWing.zRot -= breathe;
            rightWing.zRot += breathe;
            body.zRot = Mth.sin(t * 0.021F) * 0.07F * glide;
            for (int i = 0; i < FINGERS; i++) {
                float spread = 1.0F + 0.35F * glide, curl = 0.04F + 0.1F * glide + 0.02F * i;
                leftFingers[i].xRot *= spread;
                rightFingers[i].xRot *= spread;
                leftFingers[i].zRot = -curl;
                rightFingers[i].zRot = curl;
            }
            for (ModelPart c : crest) c.xRot -= 0.3F; // laid back in the wind
            for (int i = 0; i < 2; i++) {
                float k = i * 0.7F;
                streamers[i].xRot = Mth.sin(t * 0.13F + k) * 0.07F;
                streamerEnds[i].xRot = Mth.sin(t * 0.13F + k - 1.1F) * 0.16F;
                streamers[i].yRot += Mth.sin(t * 0.07F + k) * 0.04F;
            }
            return;
        }
        // perched: the broad arms hang along the flanks, a little back and drawn in (folded, they are shorter); the hands
        // fold back over them, a quarter proud so the primaries lie on the secondaries, and cross behind the tail
        for (ModelPart w : new ModelPart[] {leftWing, rightWing}) {
            w.xRot += 0.2F;
            w.yScale = 0.75F;
        }
        leftTip.xRot = rightTip.xRot = 1.85F;
        leftTip.x = 0.3F;
        rightTip.x = -0.3F;
        leftTip.yScale = rightTip.yScale = 0.8F; // shorter folded (the arm's squash narrows its chord)
        leftTip.zRot = -0.04F;
        rightTip.zRot = 0.04F;
        for (int i = 0; i < FINGERS; i++) leftFingers[i].xRot = rightFingers[i].xRot = 0.04F * i;
        for (int i = 0; i < crest.length; i++) crest[i].xRot += Mth.sin(t * 0.05F + i) * 0.04F;
        for (int i = 0; i < 2; i++) {
            streamers[i].xRot = -0.04F + Mth.sin(t * 0.04F + i) * 0.03F; // hanging from the tail (minus: down)
            streamerEnds[i].xRot = -0.04F;
        }
    }
}
