package com.mechanical.client.wildlife.warm;

import com.mechanical.client.wildlife.WildAnimalRenderState;

/**
 * The warm-country animals' render state: the wildlife state plus what their own mechanics show. {@code showsHorns}
 * (wildlife state) means the male's ornaments where there is no horn: the rooster's comb and sickle tail, the peacock's
 * crest and train; for the water buffalo, the grown animal's horns.
 */
public class WarmRenderState extends WildAnimalRenderState {
    /** Tortoise: 0..1, head, legs and tail drawn into the shell. */
    public float shell;
    /** Desert lizard: the tail has been shed (regrowing); lying flat basking in the sun. */
    public boolean tailless, basking;
    /** Water buffalo: lying in shallow water or mud. */
    public boolean wallowing;
    /** Peacock: 0..1, the train raised into a fan. */
    public float display;
    /** Tamed onager: Pack Baskets on (it has the donkey's chest). */
    public boolean packed;
}
