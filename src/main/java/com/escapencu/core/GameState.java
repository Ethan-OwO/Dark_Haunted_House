package com.escapencu.core;

public class GameState {
    public static int     currentStage = 1;
    public static int     currentRoom  = 1;

    public static int     playerMaxHp  = 100;
    public static int     playerHp     = 100;
    public static int     score        = 0;

    /** When true: F1 toggles, N skips to next floor. */
    public static boolean devMode         = false;
    /** When true (requires devMode): infinite HP, 9999 damage, immune to effects. */
    public static boolean opMode         = false;

    /** Persistent damage multiplier — stacks across floors within a run. */
    public static double  damageMultiplier = 1.0;

    public static void reset() {
        currentStage     = 1;
        currentRoom      = 1;
        playerHp         = playerMaxHp;
        score            = 0;
        devMode          = false;
        opMode           = false;
        damageMultiplier = 1.0;
    }
}
