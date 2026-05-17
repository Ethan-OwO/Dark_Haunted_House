package com.escapencu.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a random dungeon floor using a random-walk algorithm.
 * Guarantees all rooms are reachable from the start room.
 */
public class MapGenerator {
    private static final int   GRID = FloorMap.GRID;
    private static final int[] DX   = { 0,  0, 1, -1};
    private static final int[] DY   = {-1,  1, 0,  0};

    /**
     * @param seed        random seed (use System.currentTimeMillis() for true random)
     * @param targetRooms rooms to place, recommended 5-8
     * @param hasBoss     true → end room is a BOSS; false → end room is an EXIT (next floor)
     */
    public static FloorMap generate(long seed, int targetRooms, boolean hasBoss) {
        Random rand = new Random(seed);
        int[][] typeGrid = new int[GRID][GRID]; // 0=empty, 1=normal, 2=end

        // Always start at (1,1)
        int startX = 1, startY = 1;
        typeGrid[startY][startX] = 1;
        int cx = startX, cy = startY;
        int placed = 1;
        int lastX = cx, lastY = cy;

        // Random walk with occasional jumps to create branches
        int attempts = 0;
        while (placed < targetRooms && attempts < 400) {
            attempts++;
            int dir = rand.nextInt(4);
            int nx = cx + DX[dir], ny = cy + DY[dir];

            if (inBounds(nx, ny) && typeGrid[ny][nx] == 0) {
                typeGrid[ny][nx] = 1;
                lastX = nx;
                lastY = ny;
                cx = nx; cy = ny;
                placed++;
            }

            // Jump to a random existing room to create branches
            if (rand.nextInt(3) == 0) {
                List<int[]> rooms = findRooms(typeGrid);
                int[] r = rooms.get(rand.nextInt(rooms.size()));
                cx = r[0]; cy = r[1];
            }
        }

        // Mark end room (last visited, guaranteed != start)
        if (lastX == startX && lastY == startY && placed > 1) {
            // fallback: pick any room that isn't start
            for (int[] r : findRooms(typeGrid)) {
                if (r[0] != startX || r[1] != startY) { lastX = r[0]; lastY = r[1]; break; }
            }
        }
        typeGrid[lastY][lastX] = 2;

        // Build RoomNode grid
        RoomNode[][] nodes = new RoomNode[GRID][GRID];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (typeGrid[y][x] == 0) continue;
                RoomNode.Type type;
                if (x == startX && y == startY)   type = RoomNode.Type.START;
                else if (typeGrid[y][x] == 2)     type = hasBoss ? RoomNode.Type.BOSS : RoomNode.Type.EXIT;
                else                               type = RoomNode.Type.NORMAL;
                nodes[y][x] = new RoomNode(x, y, type);
            }
        }

        // Wire up doors between adjacent rooms
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (nodes[y][x] == null) continue;
                if (y > 0       && nodes[y-1][x] != null) { nodes[y][x].setDoor(Direction.NORTH, true); nodes[y-1][x].setDoor(Direction.SOUTH, true); }
                if (x < GRID-1  && nodes[y][x+1] != null) { nodes[y][x].setDoor(Direction.EAST,  true); nodes[y][x+1].setDoor(Direction.WEST,  true); }
            }
        }

        return new FloorMap(nodes, startX, startY);
    }

    private static boolean inBounds(int x, int y) { return x >= 0 && x < GRID && y >= 0 && y < GRID; }

    private static List<int[]> findRooms(int[][] grid) {
        List<int[]> list = new ArrayList<>();
        for (int y = 0; y < GRID; y++)
            for (int x = 0; x < GRID; x++)
                if (grid[y][x] > 0) list.add(new int[]{x, y});
        return list;
    }
}
