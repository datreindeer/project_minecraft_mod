package com.mechanical.client.wildlife.waterfowl;

import com.mechanical.client.wildlife.BirdRenderState;

/** The waterfowl: on top of the flock-bird state. */
public class WaterfowlRenderState extends BirdRenderState {
    /** Mallard: 0..1 tipped up, dabbling with the head under water. */
    public float dabble;
    /** Grey heron: 0..1..0 over a spear-thrust at a fish. */
    public float strike;
    /** Common crane: 0..1 how far into its leaping, wing-spreading dance. */
    public float dance;
    /** Grey heron: standing still in the shallows, neck out over the water, watching for fish. */
    public boolean hunting;
    /** Heron or crane standing in shallow water (wading, not floating). */
    public boolean wading;
}
