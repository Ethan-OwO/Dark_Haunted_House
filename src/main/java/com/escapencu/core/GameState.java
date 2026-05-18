package com.escapencu.core;

import java.util.EnumSet;
import java.util.Set;

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

    // ── Book enhancement system ────────────────────────────────────────────
    /**
     * Effects permanently added to the player's book by NPC/chest interactions.
     * Each effect stacks for the whole run (cannot be removed).
     */
    public enum BookEffect {
        /** 明年的考古題 — 攻擊力 ×1.5 */
        EXAM_QUESTIONS,
        /** 緩速射擊 — 子彈命中時使敵人緩速 2 秒 */
        SLOW_SHOT,
        /** 燃燒射擊 — 子彈命中時使敵人燃燒 2 秒 */
        BURN_SHOT
    }

    /** Active book effects for this run. */
    public static final Set<BookEffect> bookEffects = EnumSet.noneOf(BookEffect.class);

    public static void reset() {
        currentStage     = 1;
        currentRoom      = 1;
        playerHp         = playerMaxHp;
        score            = 0;
        devMode          = false;
        opMode           = false;
        damageMultiplier = 1.0;
        bookEffects.clear();
    }
}
