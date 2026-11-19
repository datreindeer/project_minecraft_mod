package com.mechanical.client.wildlife;

/** Flocking birds: on top of {@code flying} / {@code flap} from the wildlife state. */
public class BirdRenderState extends WildAnimalRenderState {
    /** 0 = flapping, 1 = gliding on still wings. */
    public float glide;
    /** Climb (+) or dive (-) angle in radians. */
    public float pitch;
    /** Floating on water. */
    public boolean swimming;
}
