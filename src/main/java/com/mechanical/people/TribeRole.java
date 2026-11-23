package com.mechanical.people;

/**
 * What a tribesperson does all day. {@link #roam} is how far from the home campfire they wander by day.
 * Each role has {@link #SKINS} skin variants: even ones are men (wide arms), odd ones women (slim arms).
 */
public enum TribeRole {
    HUNTER(24), GATHERER(18), FISHER(18), WOODCUTTER(16), ELDER(6),
    /** A Trader Caravan's old trader (its leader) and its guards (Caravans; HANDLER too): skins by caravan kind, whatever the climate. */
    TRADER(8), GUARD(16),
    /** The lone Miner under ground (Miners, entity mechanical:miner): one temperate skin set, men only (skins 0 and 2). */
    MINER(10),
    /** The Oak Folk at an Ancient Oak (OakFolk, F4): gatherers, worshippers, light-bearers and their keeper (leader). One skin set. */
    OAK_GATHERER(22), OAK_WORSHIPPER(14), OAK_LIGHTBEARER(20), OAK_KEEPER(10),
    /** A Trader Caravan's animal handler (Caravans): holds the draught animals' leads and stands still with them. */
    HANDLER(0);

    public static final int SKINS = 4, GREETINGS = 3;

    public final int roam;

    TribeRole(int roam) {
        this.roam = roam;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean caravan() {
        return this == TRADER || this == GUARD || this == HANDLER;
    }

    /** Dresses the same in every climate (one temperate skin set). */
    public boolean travels() {
        return caravan() || this == MINER || oak();
    }

    /** One of the Oak Folk (OakFolk): they live round an Ancient Oak, not at a camp. */
    public boolean oak() {
        return this == OAK_GATHERER || this == OAK_WORSHIPPER || this == OAK_LIGHTBEARER || this == OAK_KEEPER;
    }

    public static TribeRole byId(int i) {
        TribeRole[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
