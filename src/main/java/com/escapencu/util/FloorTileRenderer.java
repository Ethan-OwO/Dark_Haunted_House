package com.escapencu.util;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.Random;

/**
 * Draws stage-appropriate floor tile textures across a room.
 *
 * Source tiles are 16 × 16 px; they are scaled to TILE_DRAW_SIZE × TILE_DRAW_SIZE
 * in world space for a chunky pixel-art look.
 *
 * Call generateTilemap() once in the room constructor (seeded by grid position
 * so the layout is stable across frames), then draw() every frame.
 *
 * Stage → tile set mapping:
 *   Stage 1 (工程五館)  — level1 (1-5).png  green grass
 *   Stage 2 (男13宿舍)  — level2 (1-5).png  blue tile
 *   Stage 3 (圖書館)    — level3 (1-4).png  brown brick
 */
public class FloorTileRenderer {

    /** How many world-space pixels each tile occupies (source is 16 px → 2× scale). */
    public static final int TILE_DRAW_SIZE = 32;

    // ── Tile images loaded once at class-init ─────────────────────────────
    private static final Image[] TILES_L1 = loadLevel(1, 5);
    private static final Image[] TILES_L2 = loadLevel(2, 5);
    private static final Image[] TILES_L3 = loadLevel(3, 4);

    private static Image[] loadLevel(int level, int count) {
        Image[] arr = new Image[count];
        for (int i = 0; i < count; i++) {
            // e.g. "/images/texture/level1 (1).png"
            String path = "/images/texture/level" + level + " (" + (i + 1) + ").png";
            arr[i] = ResourceLoader.getImage(path);
        }
        return arr;
    }

    private static Image[] tilesForStage(int stage) {
        return switch (stage) {
            case 1  -> TILES_L1;
            case 2  -> TILES_L2;
            default -> TILES_L3;
        };
    }

    // ── Tilemap generation ────────────────────────────────────────────────

    /**
     * Generates a stable 2-D index array for the given room.
     * Dimensions cover (roomW × roomH) world-space pixels at TILE_DRAW_SIZE per cell.
     * Seeded by grid position + stage so every room is unique but never re-shuffled.
     *
     * @param gridX  room grid column
     * @param gridY  room grid row
     * @param roomW  room world width  (e.g. DungeonFloor.ROOM_W)
     * @param roomH  room world height (e.g. DungeonFloor.ROOM_H)
     * @param stage  current stage (1-3), selects the tile set
     * @return       [rows][cols] array of tile indices
     */
    public static int[][] generateTilemap(int gridX, int gridY,
                                          double roomW, double roomH, int stage) {
        int cols     = (int) Math.ceil(roomW / TILE_DRAW_SIZE);
        int rows     = (int) Math.ceil(roomH / TILE_DRAW_SIZE);
        int numTiles = tilesForStage(stage).length;
        int[][] map  = new int[rows][cols];
        Random  rng  = new Random(gridX * 73856093L ^ gridY * 19349663L ^ stage * 1000003L);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                map[r][c] = rng.nextInt(numTiles);
        return map;
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    /**
     * Tiles the floor texture starting at (originX, originY).
     * Tiles extend beyond the room boundary; walls drawn afterwards cover the excess.
     *
     * @param gc       GraphicsContext (world coordinates, camera already applied)
     * @param tilemap  index array from generateTilemap()
     * @param stage    1-3, selects which tile set to use
     * @param originX  world X of the room's top-left corner
     * @param originY  world Y of the room's top-left corner
     */
    public static void draw(GraphicsContext gc, int[][] tilemap, int stage,
                             double originX, double originY) {
        Image[] tiles = tilesForStage(stage);
        int T = TILE_DRAW_SIZE;
        for (int r = 0; r < tilemap.length; r++) {
            for (int c = 0; c < tilemap[r].length; c++) {
                Image img = tiles[tilemap[r][c] % tiles.length];
                if (img != null)
                    gc.drawImage(img, originX + c * T, originY + r * T, T, T);
            }
        }
    }
}
