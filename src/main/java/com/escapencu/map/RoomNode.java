package com.escapencu.map;

import java.util.EnumMap;
import java.util.Map;

/** Metadata for one cell in the floor grid. Does NOT contain rendering logic. */
public class RoomNode {
    public enum Type { START, NORMAL, EXIT, BOSS }

    public final int gridX, gridY;
    public final Type type;

    private final Map<Direction, Boolean> doors = new EnumMap<>(Direction.class);
    private boolean cleared;

    public RoomNode(int gridX, int gridY, Type type) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.type  = type;
        for (Direction d : Direction.values()) doors.put(d, false);
        // START and EXIT rooms are considered cleared from the start (no enemies)
        this.cleared = (type == Type.START || type == Type.EXIT);
    }

    public boolean hasDoor(Direction dir)           { return doors.getOrDefault(dir, false); }
    public void    setDoor(Direction dir, boolean v) { doors.put(dir, v); }
    public boolean isCleared()                       { return cleared; }
    public void    setCleared(boolean c)             { cleared = c; }
}
