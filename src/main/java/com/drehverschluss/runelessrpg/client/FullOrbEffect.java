package com.drehverschluss.runelessrpg.client;

/** Effect of a full mana orb (as many orbs as may merge) on the client. */
public enum FullOrbEffect {
    /** Golden sparkles and a slow pulse. */
    BOTH,
    /** Only the golden sparkles. */
    SPARKLES,
    /** Only the slow pulse in brightness and size. */
    PULSE,
    /** No effect. */
    NONE;

    public boolean sparkles() {
        return this == BOTH || this == SPARKLES;
    }

    public boolean pulse() {
        return this == BOTH || this == PULSE;
    }
}
