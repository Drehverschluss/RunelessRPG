package com.drehverschluss.runelessrpg.client;

/** Last mana value received from the server for the local player. */
public final class ClientManaState {
    private static double mana = 0.0;

    private ClientManaState() {}

    public static double getMana() {
        return mana;
    }

    static void setMana(double value) {
        mana = value;
    }
}
