package com.escapencu.map;

import com.escapencu.level.*;
import com.escapencu.level.RewardRoom.RewardType;

import java.util.*;

/**
 * Generates a dungeon floor as a tree on a 5×5 grid.
 *
 * Algorithm
 * ─────────
 *  1. START at (0, startY), first NORMAL east of it at (1, startY).
 *  2. BFS from the first NORMAL: each room tries 1/2/3 branches (equal prob).
 *     A candidate cell is skipped when it would be adjacent to any room
 *     other than its intended parent — this keeps the map tree-shaped and
 *     prevents rooms from forming a dense blob on the mini-map.
 *  3. Deepest leaf (BFS distance from START) → BOSS (floor 3) or EXIT.
 *  4. 1-2 other leaves → REWARD.
 */
public class MapGenerator {

    private static final int GRID = 5;
    //  0=NORTH(dy=-1)  1=SOUTH(dy=+1)  2=EAST(dx=+1)  3=WEST(dx=-1)
    private static final int[] DX = { 0,  0,  1, -1 };
    private static final int[] DY = { -1,  1,  0,  0 };

    // ── instance state ────────────────────────────────────────────────────
    private Random rand;
    private Room[][] grid;
    private final List<int[]> connections = new ArrayList<>();
    private int stage, floorNum, totalRooms, targetRooms;
    private int startX, startY;

    // ── Public entry point ────────────────────────────────────────────────
    public static DungeonFloor generate(long seed, int stage, int floorNum) {
        return new MapGenerator().build(seed, stage, floorNum);
    }

    private DungeonFloor build(long seed, int stage, int floorNum) {
        this.rand        = new Random(seed);
        this.grid        = new Room[GRID][GRID];
        this.connections.clear();
        this.stage       = stage;
        this.floorNum    = floorNum;
        this.targetRooms = 8 + rand.nextInt(3);  // 8, 9, or 10
        this.totalRooms  = 0;

        // START on left edge; y randomised inside 1..GRID-2
        this.startX = 0;
        this.startY = 1 + rand.nextInt(GRID - 2);

        // ── Step 1: START ─────────────────────────────────────────────────
        placeRoom(startX, startY,
            new NormalRoom(startX, startY, Room.Type.START, stage, floorNum));

        // ── Step 2: one forced NORMAL east of START ───────────────────────
        placeRoom(startX + 1, startY,
            new NormalRoom(startX + 1, startY, Room.Type.NORMAL, stage, floorNum));
        addConnection(startX, startY, startX + 1, startY);

        // ── Step 3: BFS tree — each room gets 1-3 branches ───────────────
        buildTree(startX + 1, startY);

        // ── Step 4: deepest leaf → BOSS or EXIT ───────────────────────────
        int[] deepest = findDeepestLeaf();
        Room endRoom = (floorNum == 3)
            ? new BossRoom(deepest[0], deepest[1], stage)
            : new NormalRoom(deepest[0], deepest[1], Room.Type.EXIT, stage, floorNum);
        replaceRoom(deepest[0], deepest[1], endRoom);

        // ── Step 5: 1-2 other leaves → REWARD ────────────────────────────
        List<int[]> leaves = getLeaves();
        leaves.removeIf(p -> Arrays.equals(p, deepest)
                          || (p[0] == startX && p[1] == startY));
        int rewards = Math.min(1 + rand.nextInt(2), leaves.size());
        for (int i = 0; i < rewards; i++) {
            int[] pos = leaves.remove(rand.nextInt(leaves.size()));
            replaceRoom(pos[0], pos[1],
                new RewardRoom(pos[0], pos[1], randomRewardType(), stage));
        }

        // ── Step 6: corridor flags ────────────────────────────────────────
        applyCorridorFlags();

        // ── Step 7: pre-generate obstacles (needs corridors to be known) ──
        for (int gy = 0; gy < GRID; gy++)
            for (int gx = 0; gx < GRID; gx++)
                if (grid[gy][gx] instanceof NormalRoom nr)
                    nr.initObstacles();

        // ── Step 8: assemble ──────────────────────────────────────────────
        return assembleDungeonFloor();
    }

    // ── BFS tree expansion ────────────────────────────────────────────────

    /**
     * Expands the dungeon tree using BFS so every room at depth D gets its
     * branches placed before depth D+1 rooms are processed.  This guarantees
     * the "branches" setting actually manifests on the mini-map instead of
     * being consumed by a single deep chain (the DFS failure mode).
     *
     * Adjacency constraint: a candidate cell is rejected when any of its four
     * neighbours is already occupied by a room other than the current parent.
     * This prevents two separate branches from ending up side-by-side, which
     * would look like a blob on the mini-map rather than a tree.
     */
    private void buildTree(int firstX, int firstY) {
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{firstX, firstY, 3}); // {x, y, cameFrom=WEST}

        while (!q.isEmpty() && totalRooms < targetRooms) {
            int[] curr = q.poll();
            int cx = curr[0], cy = curr[1], cameFrom = curr[2];

            List<Integer> dirs = shuffledDirs();
            dirs.remove(Integer.valueOf(cameFrom));

            int branches = 1 + rand.nextInt(3); // 1, 2, or 3 — equal probability
            int placed   = 0;

            for (int dir : dirs) {
                if (placed >= branches || totalRooms >= targetRooms) break;
                int nx = cx + DX[dir], ny = cy + DY[dir];
                if (!inBounds(nx, ny) || grid[ny][nx] != null) continue;
                // Keep tree shape: reject cells adjacent to any non-parent room
                if (hasNonParentNeighbor(nx, ny, cx, cy)) continue;

                placeRoom(nx, ny, new NormalRoom(nx, ny, Room.Type.NORMAL, stage, floorNum));
                addConnection(cx, cy, nx, ny);
                placed++;
                q.add(new int[]{nx, ny, opposite(dir)});
            }
        }
    }

    /** True when (nx,ny) has an occupied grid neighbour that is NOT (px,py). */
    private boolean hasNonParentNeighbor(int nx, int ny, int px, int py) {
        for (int dir = 0; dir < 4; dir++) {
            int ax = nx + DX[dir], ay = ny + DY[dir];
            if (!inBounds(ax, ay)) continue;
            if (ax == px && ay == py) continue; // parent is allowed
            if (grid[ay][ax] != null) return true;
        }
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void placeRoom(int gx, int gy, Room r) {
        grid[gy][gx] = r;
        totalRooms++;
    }

    private void replaceRoom(int gx, int gy, Room r) {
        grid[gy][gx] = r;
    }

    private void addConnection(int x1, int y1, int x2, int y2) {
        connections.add(new int[]{x1, y1, x2, y2});
    }

    /** BFS from START; returns grid pos of farthest non-start leaf. */
    private int[] findDeepestLeaf() {
        int[][] dist = new int[GRID][GRID];
        for (int[] row : dist) Arrays.fill(row, -1);
        dist[startY][startX] = 0;
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{startX, startY});

        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int[] c : connections) {
                int[] n = null;
                if (c[0] == p[0] && c[1] == p[1]) n = new int[]{c[2], c[3]};
                else if (c[2] == p[0] && c[3] == p[1]) n = new int[]{c[0], c[1]};
                if (n != null && dist[n[1]][n[0]] == -1) {
                    dist[n[1]][n[0]] = dist[p[1]][p[0]] + 1;
                    q.add(n);
                }
            }
        }

        int[] best = {startX + 1, startY};
        int maxD = 0;
        for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) {
            if (grid[y][x] != null && dist[y][x] > maxD && isLeaf(x, y)
                    && !(x == startX && y == startY)) {
                maxD = dist[y][x];
                best = new int[]{x, y};
            }
        }
        return best;
    }

    private List<int[]> getLeaves() {
        List<int[]> list = new ArrayList<>();
        for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++)
            if (grid[y][x] != null && isLeaf(x, y) && !(x == startX && y == startY))
                list.add(new int[]{x, y});
        return list;
    }

    private boolean isLeaf(int x, int y) {
        int count = 0;
        for (int[] c : connections)
            if ((c[0] == x && c[1] == y) || (c[2] == x && c[3] == y)) count++;
        return count <= 1;
    }

    /** Set hasCorridor flags on each room from the connection list. */
    private void applyCorridorFlags() {
        for (int[] c : connections) {
            int x1 = c[0], y1 = c[1], x2 = c[2], y2 = c[3];
            if      (x2 > x1) { grid[y1][x1].setHasCorridor(2, true); grid[y2][x2].setHasCorridor(3, true); }
            else if (x2 < x1) { grid[y1][x1].setHasCorridor(3, true); grid[y2][x2].setHasCorridor(2, true); }
            else if (y2 > y1) { grid[y1][x1].setHasCorridor(1, true); grid[y2][x2].setHasCorridor(0, true); }
            else               { grid[y1][x1].setHasCorridor(0, true); grid[y2][x2].setHasCorridor(1, true); }
        }
    }

    /** Build Corridor objects and assemble the DungeonFloor. */
    private DungeonFloor assembleDungeonFloor() {
        List<Room> rooms = new ArrayList<>();
        for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++)
            if (grid[y][x] != null) rooms.add(grid[y][x]);

        List<Corridor> corridors = new ArrayList<>();
        for (int[] c : connections) corridors.add(makeCorridor(c[0], c[1], c[2], c[3]));

        return new DungeonFloor(rooms, corridors, grid, grid[startY][startX]);
    }

    private Corridor makeCorridor(int x1, int y1, int x2, int y2) {
        double rw = DungeonFloor.ROOM_W, rh = DungeonFloor.ROOM_H;
        double ct = DungeonFloor.CORRIDOR_THICK;

        if (y1 == y2) { // horizontal
            int lx = Math.min(x1, x2);
            double wx = DungeonFloor.roomWorldX(lx) + rw;
            double wy = DungeonFloor.roomWorldY(y1)  + rh / 2 - ct / 2;
            double ww = DungeonFloor.CELL_W - rw;
            return new Corridor(wx, wy, ww, ct);
        } else {         // vertical
            int ty = Math.min(y1, y2);
            double wx = DungeonFloor.roomWorldX(x1) + rw / 2 - ct / 2;
            double wy = DungeonFloor.roomWorldY(ty)  + rh;
            double wh = DungeonFloor.CELL_H - rh;
            return new Corridor(wx, wy, ct, wh);
        }
    }

    // ── Tiny utilities ────────────────────────────────────────────────────
    private RewardType randomRewardType() {
        return switch (rand.nextInt(10)) {
            case 0, 1, 2, 3 -> RewardType.POTION;
            case 4, 5, 6    -> RewardType.CHEST;
            default         -> RewardType.SHOP;   // 7,8,9 → shop
        };
    }

    private static boolean inBounds(int x, int y) { return x >= 0 && x < GRID && y >= 0 && y < GRID; }
    private static int opposite(int dir) { return switch (dir) { case 0->1; case 1->0; case 2->3; default->2; }; }

    private List<Integer> shuffledDirs() {
        List<Integer> dirs = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(dirs, rand);
        return dirs;
    }
}
