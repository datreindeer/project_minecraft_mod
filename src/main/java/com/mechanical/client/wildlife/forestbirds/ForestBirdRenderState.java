package com.mechanical.client.wildlife.forestbirds;

import com.mechanical.client.wildlife.BirdRenderState;

/** Forest birds: on top of the flock-bird state. */
public class ForestBirdRenderState extends BirdRenderState {
    /** Capercaillie cock on his lek: 0..1 into the display pose (tail fanned up, neck stretched, wings drooped). */
    public float display;
    /** Capercaillie hen: drawn smaller. */
    public boolean hen;
    /** Golden eagle with a snatched body in its talons: legs hang down. */
    public boolean carrying;
}
