package com.drehverschluss.runelessrpg.client;

/** Last mana value received from the server for the local player (no client classes: also read by common code). */
public final class ClientManaState {
    private static double mana = 0.0;
    private static double storageMax = Double.NaN;

    private ClientManaState() {}

    public static double getMana() {
        return mana;
    }

    /** Max mana of another mod's storage as sent by the server; NaN with the built-in storage (the attribute applies). */
    public static double getStorageMax() {
        return storageMax;
    }

    static void set(double value, double max) {
        mana = value;
        storageMax = max;
    }

    /** Back to the built-in state when leaving a server. */
    public static void reset() {
        storageMax = Double.NaN;
    }
}
