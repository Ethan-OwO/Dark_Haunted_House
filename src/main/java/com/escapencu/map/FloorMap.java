package com.escapencu.map;

public class FloorMap {
    public static final int GRID = 4; // 4×4 grid of possible room slots

    private final RoomNode[][] grid;
    private int currentX, currentY;
    public  final int startX, startY;

    public FloorMap(RoomNode[][] grid, int startX, int startY) {
        this.grid     = grid;
        this.startX   = startX;
        this.startY   = startY;
        this.currentX = startX;
        this.currentY = startY;
    }

    public RoomNode getNode(int x, int y) {
        if (x < 0 || x >= GRID || y < 0 || y >= GRID) return null;
        return grid[y][x];
    }

    public RoomNode getCurrentNode()          { return grid[currentY][currentX]; }
    public RoomNode getNeighbor(Direction d)  { return getNode(currentX + d.dx, currentY + d.dy); }

    public void moveTo(Direction d) {
        currentX += d.dx;
        currentY += d.dy;
    }

    public int getCurrentX()      { return currentX; }
    public int getCurrentY()      { return currentY; }
    public RoomNode[][] getGrid() { return grid; }
}
