package com.escapencu.level;

import com.escapencu.core.GameState;
import com.escapencu.map.FloorMap;
import com.escapencu.map.MapGenerator;
import com.escapencu.map.RoomNode;

/**
 * Tracks overall progression: Stage (1-3) × Floor (1-3).
 * Each floor is a randomly generated dungeon.
 * Floor 3 of each stage is always the boss floor.
 */
public class LevelManager {
    private int stage    = 1;  // 1-3
    private int floorNum = 1;  // 1-3 within a stage

    private FloorMap floorMap;

    public LevelManager() {
        generateFloor();
    }

    public FloorMap getFloorMap() { return floorMap; }

    /** Build a Room instance for a given RoomNode (called lazily by GameScene). */
    public Room createRoomForNode(RoomNode node) {
        Room room = switch (node.type) {
            case BOSS  -> new BossRoom(stage);
            default    -> new NormalRoom(stage, floorNum);
        };
        room.setNode(node);
        room.init();
        return room;
    }

    public boolean hasNextFloor() {
        return !(stage == 3 && floorNum == 3);
    }

    public void advanceFloor() {
        floorNum++;
        if (floorNum > 3) { floorNum = 1; stage++; }
        GameState.currentStage = stage;
        GameState.currentRoom  = floorNum;
        generateFloor();
    }

    public int getStage()    { return stage; }
    public int getFloorNum() { return floorNum; }

    private void generateFloor() {
        boolean isBossFloor = (floorNum == 3);
        int targetRooms = 5 + (int)(Math.random() * 4); // 5-8 rooms
        floorMap = MapGenerator.generate(System.currentTimeMillis(), targetRooms, isBossFloor);
    }
}
