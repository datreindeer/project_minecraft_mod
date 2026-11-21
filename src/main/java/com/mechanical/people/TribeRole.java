package com.mechanical.people;

/**
 * What a tribesperson does all day. {@link #roam} is how far from the home campfire they wander by day.
 * Each role has {@link #SKINS} skin variants: even ones are men (wide arms), odd ones women (slim arms).
 */
public enum TribeRole {
    HUNTER(24), GATHERER(18), FISHER(18), WOODCUTTER(16), ELDER(6),
    /** A Trader Caravan's old trader and its guards (Caravans): one temperate skin set, whatever the climate. */
    TRADER(8), GUARD(16),
    /** The lone Miner under ground (Miners, entity mechanical:miner): one temperate skin set, men only (skins 0 and 2). */
    MINER(10);

    public static final int SKINS = 4, GREETINGS = 3;

    public final int roam;

    TribeRole(int roam) {
        this.roam = roam;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean caravan() {
        return this == TRADER || this == GUARD;
    }

    /** Dresses the same in every climate (one temperate skin set). */
    public boolean travels() {
        return caravan() || this == MINER;
    }

    public static TribeRole byId(int i) {
        TribeRole[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
