package com.escapencu.core;

public class GameState {
    public static int currentStage = 1;   // 1, 2, 3
    public static int currentRoom  = 1;   // 1, 2, 3

    public static int playerMaxHp = 100;
    public static int playerHp    = 100;
    public static int score       = 0;

    public static void reset() {
        currentStage = 1;
        currentRoom  = 1;
        playerHp     = playerMaxHp;
        score        = 0;
    }
}
