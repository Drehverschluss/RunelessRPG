package com.drehverschluss.runelessrpg.client.hud;

/** Look of the mana bar. */
public enum ManaBarStyle {
    /** MIKI_DIAMOND when Miki's HUD is loaded, otherwise THIN. */
    AUTO,
    /** Narrow bar like the experience bar. */
    THIN,
    /** Framed bar that matches Miki's HUD. */
    MIKI,
    /** Upright bar that fills from the bottom. */
    VERTICAL,
    /** Diamond behind Miki's hunger / health diamond (only the outer half shows); a whole diamond elsewhere. */
    MIKI_DIAMOND,
    /** Band along the outer edges of Miki's hunger / health diamond; MIKI elsewhere. */
    MIKI_BAND
}
