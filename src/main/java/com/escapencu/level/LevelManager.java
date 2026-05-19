package com.escapencu.level;

import com.escapencu.core.GameState;
import com.escapencu.map.MapGenerator;

/**
 * Tracks overall progression: Stage (1-3) × Floor (1-3).
 * Each floor is a randomly generated dungeon.
 * Floor 3 of each stage is always the boss floor.
 */
public class LevelManager {
    private int stage    = 1;  // 1-3
    private int floorNum = 1;  // 1-3 within a stage

    private DungeonFloor dungeonFloor;

    public LevelManager() {
        generateFloor();
    }

    public DungeonFloor getDungeonFloor() { return dungeonFloor; }

    public boolean hasNextFloor() {
        return !(stage == 3 && floorNum == 3);
    }

    public void advanceFloor() {
        floorNum++;
        if (floorNum > 3) {
            floorNum = 1;
            stage++;
            // Stage 切換：補充所有每 Stage 限用一次的效果
            GameState.talentUsedThisStage = false;
        }
        GameState.currentStage = stage;
        GameState.currentRoom  = floorNum;
        generateFloor();
    }

    public int getStage()    { return stage; }
    public int getFloorNum() { return floorNum; }

    private void generateFloor() {
        dungeonFloor = MapGenerator.generate(System.currentTimeMillis(), stage, floorNum);
    }
}
