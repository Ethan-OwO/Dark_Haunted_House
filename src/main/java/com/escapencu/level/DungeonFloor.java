package com.escapencu.level;

import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * One dungeon floor: a collection of Rooms and Corridors placed in world space.
 *
 * World layout constants
 * ─────────────────────
 *  ROOM_W × ROOM_H      = size of every room rectangle
 *  CELL_W × CELL_H      = grid cell size (room + corridor gap)
 *  CORRIDOR_THICK       = width of every corridor (N/S/E/W)
 *
 *  Room at grid (gx, gy):
 *    worldX = gx * CELL_W + (CELL_W - ROOM_W) / 2
 *    worldY = gy * CELL_H + (CELL_H - ROOM_H) / 2
 */
public class DungeonFloor {
    // ── Layout constants ───────────────────────────────────────────────────
    public static final double ROOM_W          = 800;
    public static final double ROOM_H          = 560;
    public static final double CELL_W          = 1100;
    public static final double CELL_H          = 760;
    public static final double CORRIDOR_THICK  = 100;

    public static double roomWorldX(int gx) { return gx * CELL_W + (CELL_W - ROOM_W) / 2.0; }
    public static double roomWorldY(int gy) { return gy * CELL_H + (CELL_H - ROOM_H) / 2.0; }

    // ── Data ───────────────────────────────────────────────────────────────
    private final List<Room>     rooms;
    private final List<Corridor> corridors;
    private final Room[][]       grid;   // 4×4, null = empty cell
    public  final Room           startRoom;

    public DungeonFloor(List<Room> rooms, List<Corridor> corridors,
                        Room[][] grid, Room startRoom) {
        this.rooms     = rooms;
        this.corridors = corridors;
        this.grid      = grid;
        this.startRoom = startRoom;
    }

    // ── Collision / query ──────────────────────────────────────────────────

    /**
     * Returns true when the given rectangle is fully inside passable area.
     * Uses 4-corner point test so the player can never clip partially outside
     * the dungeon boundary.
     */
    public boolean canMoveTo(double px, double py, double pw, double ph) {
        double e = 0.5; // tiny inset to avoid floating-point edge straddle
        return isPassable(px + e,      py + e)
            && isPassable(px + pw - e, py + e)
            && isPassable(px + e,      py + ph - e)
            && isPassable(px + pw - e, py + ph - e);
    }

    private boolean isPassable(double wx, double wy) {
        for (Room r : rooms)
            if (r.containsPoint(wx, wy)) return true;
        for (Corridor c : corridors)
            if (wx >= c.worldX && wx <= c.worldX + c.worldW
             && wy >= c.worldY && wy <= c.worldY + c.worldH) return true;
        return false;
    }

    /**
     * Returns the Room whose world rectangle contains this world point,
     * or null if the point is in a corridor or void.
     */
    public Room getRoomAt(double wx, double wy) {
        for (Room r : rooms)
            if (r.containsPoint(wx, wy)) return r;
        return null;
    }

    // ── Update ─────────────────────────────────────────────────────────────
    /**
     * Update all activated rooms. Enemies outside the player's room will
     * still tick (bullets move, AI tracks) but only the player's room
     * applies contact damage / triggers clearing.
     */
    public void update(double deltaTime, Player player) {
        for (Room r : rooms)
            if (r.isActivated()) r.update(deltaTime, player);
    }

    // ── Draw (world coordinates, caller applies camera transform) ─────────
    public void draw(GraphicsContext gc) {
        for (Room r : rooms) r.draw(gc);
        for (Corridor c : corridors) c.draw(gc);
    }

    // ── Accessors ──────────────────────────────────────────────────────────
    public List<Room>     getRooms()     { return rooms; }
    public List<Corridor> getCorridors() { return corridors; }
    public Room[][]       getGrid()      { return grid; }
    public int            getGridSize()  { return grid.length; }
}
