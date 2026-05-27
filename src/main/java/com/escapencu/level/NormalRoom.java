package com.escapencu.level;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.enemy.Goose;
import com.escapencu.entity.enemy.Squirrel;
import com.escapencu.entity.enemy.Termite;
import com.escapencu.util.FloorTileRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Standard room used for START / NORMAL / REWARD / EXIT types.
 */
public class NormalRoom extends Room {
    private final int stage;
    private final int floorNum;

    // ── Obstacle constants ─────────────────────────────────────────────────
    private static final double BLOCK       = 32.0;   // matches FloorTileRenderer.TILE_DRAW_SIZE
    private static final double WALL_MARGIN = 32.0;
    private static final double CORR_DEPTH  = 120.0;
    private static final double CORR_HALF   = DungeonFloor.CORRIDOR_THICK / 2.0 + 36.0;
    private static final double OBS_GAP     = 32.0;
    private static final int    MAX_SHAPES  = 4;

    /**
     * Obstacle shape templates as (col, row) grid offsets.
     * All shapes are open (no closed loops) to prevent enclosure.
     */
    private static final int[][][] SHAPES = {
        // ── Single ──────────────────────────────────────────────────────
        {{0,0}},
        // ── I-horizontal ────────────────────────────────────────────────
        {{0,0},{1,0}},
        {{0,0},{1,0},{2,0}},
        // ── I-vertical ──────────────────────────────────────────────────
        {{0,0},{0,1}},
        {{0,0},{0,1},{0,2}},
        // ── L (4 orientations) ──────────────────────────────────────────
        {{0,0},{0,1},{0,2},{1,2}},
        {{1,0},{1,1},{0,2},{1,2}},
        {{0,0},{1,0},{0,1},{0,2}},
        {{0,0},{1,0},{1,1},{1,2}},
        // ── T (4 orientations) ──────────────────────────────────────────
        {{0,0},{1,0},{2,0},{1,1}},
        {{0,0},{0,1},{1,1},{0,2}},
        {{0,1},{1,0},{1,1},{2,1}},
        {{1,0},{0,1},{1,1},{1,2}},
        // ── C / U (4 openings) ──────────────────────────────────────────
        {{0,0},{1,0},{0,1},{0,2},{1,2}},
        {{0,0},{1,0},{1,1},{0,2},{1,2}},
        {{0,0},{2,0},{0,1},{1,1},{2,1}},
        {{0,0},{1,0},{2,0},{0,1},{2,1}},
    };

    /** Flat list of placed obstacle blocks: each double[] is {worldX, worldY, w, h}. */
    private final List<double[]> obstacleBlocks = new ArrayList<>();

    /** Stable tile-index map generated once at construction (seeded by grid pos). */
    private final int[][] floorTilemap;

    public NormalRoom(int gridX, int gridY, Type type, int stage, int floorNum) {
        super(gridX, gridY, type);
        this.stage        = stage;
        this.floorNum     = floorNum;
        this.floorTilemap = FloorTileRenderer.generateTilemap(gridX, gridY, worldW - WALL * 2, worldH - WALL * 2, stage);
    }

    // ── Obstacle init (called by MapGenerator after corridors are known) ───

    /**
     * Pre-generates obstacle layout for this room.
     * Must be called after corridor flags are set.
     * No-op for START / EXIT / REWARD rooms.
     */
    public void initObstacles() {
        if (type != Type.NORMAL) return;
        spawnObstacles();
    }

    // ── Enemy spawning ─────────────────────────────────────────────────────

    @Override
    protected void spawnEnemies() {
        if (type == Type.START || type == Type.EXIT || type == Type.REWARD) return;

        int count = 2 + floorNum; // floor1→3, floor2→4, floor3→5
        Random spawnRng = new Random(stage * 9973L + floorNum * 1031L);

        for (int i = 0; i < count; i++) {
            double ex = worldX + WALL + 60 + (i * 230) % (worldW - WALL * 2 - 80);
            double ey = worldY + WALL + 60 + (i * 170) % (worldH - WALL * 2 - 80);
            final double CHECK_SIZE = 50.0;

            for (int att = 0; att < 40 && blockedByObstacle(ex, ey, CHECK_SIZE, CHECK_SIZE); att++) {
                ex = worldX + WALL + 30 + spawnRng.nextDouble() * (worldW - WALL * 2 - 60);
                ey = worldY + WALL + 30 + spawnRng.nextDouble() * (worldH - WALL * 2 - 60);
            }

            Enemy e = switch (stage) {
                case 1  -> new Squirrel(ex, ey, stage);
                case 2  -> new Goose(ex, ey, stage);
                default -> new Termite(ex, ey, stage);
            };
            e.setRoomBounds(worldX + WALL, worldY + WALL,
                            worldX + worldW - WALL, worldY + worldH - WALL);
            e.setObstacleChecker((px, py, pw, ph) -> !blockedByObstacle(px, py, pw, ph));
            enemies.add(e);
        }
    }

    // ── Obstacle placement ─────────────────────────────────────────────────

    private void spawnObstacles() {
        Random rng = new Random();
        int shapeCount = 2 + rng.nextInt(MAX_SHAPES - 1); // 2-4 shapes

        double minX = worldX + WALL + WALL_MARGIN;
        double maxX = worldX + worldW - WALL - WALL_MARGIN;
        double minY = worldY + WALL + WALL_MARGIN;
        double maxY = worldY + worldH - WALL - WALL_MARGIN;

        for (int s = 0; s < shapeCount; s++) {
            tryPlaceShape(rng, minX, maxX, minY, maxY);
        }
    }

    private void tryPlaceShape(Random rng, double minX, double maxX,
                                double minY, double maxY) {
        int[][] shape = SHAPES[rng.nextInt(SHAPES.length)];

        int maxCol = 0, maxRow = 0;
        for (int[] cell : shape) {
            maxCol = Math.max(maxCol, cell[0]);
            maxRow = Math.max(maxRow, cell[1]);
        }
        double shapeW = (maxCol + 1) * BLOCK;
        double shapeH = (maxRow + 1) * BLOCK;

        double rangeX = maxX - minX - shapeW;
        double rangeY = maxY - minY - shapeH;
        if (rangeX <= 0 || rangeY <= 0) return;

        for (int attempt = 0; attempt < 40; attempt++) {
            double ox = minX + rng.nextDouble() * rangeX;
            double oy = minY + rng.nextDouble() * rangeY;

            List<double[]> newBlocks = new ArrayList<>();
            boolean valid = true;
            for (int[] cell : shape) {
                double bx = ox + cell[0] * BLOCK;
                double by = oy + cell[1] * BLOCK;

                if (bx < minX || bx + BLOCK > maxX + BLOCK
                 || by < minY || by + BLOCK > maxY + BLOCK) {
                    valid = false; break;
                }
                if (inCorridorClearZone(bx, by)) { valid = false; break; }
                if (tooCloseToExisting(bx, by))  { valid = false; break; }

                newBlocks.add(new double[]{bx, by, BLOCK, BLOCK});
            }
            if (valid) {
                obstacleBlocks.addAll(newBlocks);
                return;
            }
        }
    }

    private boolean inCorridorClearZone(double bx, double by) {
        // North
        if (getHasCorridor(0)) {
            double cx = worldX + worldW / 2.0;
            if (bx < cx + CORR_HALF && bx + BLOCK > cx - CORR_HALF
                    && by < worldY + WALL + CORR_DEPTH) return true;
        }
        // South
        if (getHasCorridor(1)) {
            double cx = worldX + worldW / 2.0;
            if (bx < cx + CORR_HALF && bx + BLOCK > cx - CORR_HALF
                    && by + BLOCK > worldY + worldH - WALL - CORR_DEPTH) return true;
        }
        // East
        if (getHasCorridor(2)) {
            double cy = worldY + worldH / 2.0;
            if (by < cy + CORR_HALF && by + BLOCK > cy - CORR_HALF
                    && bx + BLOCK > worldX + worldW - WALL - CORR_DEPTH) return true;
        }
        // West
        if (getHasCorridor(3)) {
            double cy = worldY + worldH / 2.0;
            if (by < cy + CORR_HALF && by + BLOCK > cy - CORR_HALF
                    && bx < worldX + WALL + CORR_DEPTH) return true;
        }
        return false;
    }

    private boolean tooCloseToExisting(double bx, double by) {
        double pad = OBS_GAP;
        for (double[] e : obstacleBlocks) {
            if (bx - pad < e[0] + e[2] && bx + BLOCK + pad > e[0]
             && by - pad < e[1] + e[3] && by + BLOCK + pad > e[1]) return true;
        }
        return false;
    }

    /**
     * Returns true when the given rectangle overlaps any obstacle block.
     * Used by the enemy obstacle checker and area movement validation.
     */
    public boolean blockedByObstacle(double px, double py, double pw, double ph) {
        for (double[] b : obstacleBlocks) {
            if (px < b[0] + b[2] && px + pw > b[0]
             && py < b[1] + b[3] && py + ph > b[1]) return true;
        }
        return false;
    }

    // ── Draw ───────────────────────────────────────────────────────────────

    @Override
    protected void drawFloor(GraphicsContext gc) {
        // ── Wall area: solid color matched to the stage tile palette ──────────────
        gc.setFill(wallColor());
        gc.fillRect(worldX, worldY, worldW, worldH);

        // ── Tiled floor texture (inner walkable area only) ────────────────────────
        FloorTileRenderer.draw(gc, floorTilemap, stage, worldX + WALL, worldY + WALL);

        // ── Stage 2: dampen the bright blue tiles ─────────────────────────────────
        if (stage == 2) {
            gc.setFill(Color.color(0.0, 0.02, 0.15, 0.32));
            gc.fillRect(worldX + WALL, worldY + WALL, worldW - WALL * 2, worldH - WALL * 2);
        }

        // ── Inner edge: 3-px dark strip where wall meets floor ───────────────────
        gc.setFill(wallEdgeColor());
        gc.fillRect(worldX + WALL,                    worldY + WALL,                    worldW - WALL * 2, 3);
        gc.fillRect(worldX + WALL,                    worldY + worldH - WALL - 3,       worldW - WALL * 2, 3);
        gc.fillRect(worldX + WALL,                    worldY + WALL,                    3, worldH - WALL * 2);
        gc.fillRect(worldX + worldW - WALL - 3,       worldY + WALL,                    3, worldH - WALL * 2);

        drawObstacles(gc);

        if (type == Type.EXIT)   drawPortal(gc);
        if (type == Type.REWARD) drawRewardLabel(gc);
    }

    /** Solid wall colour — delegates to shared helper in Room. */
    private Color wallColor()     { return stageWallColor(stage); }

    /** Dark edge strip colour — delegates to shared helper in Room. */
    private Color wallEdgeColor() { return stageWallEdgeColor(stage); }

    private void drawObstacles(GraphicsContext gc) {
        if (obstacleBlocks.isEmpty()) return;

        Color main      = obstacleMainColor();
        Color highlight = obstacleHighlightColor();
        Color shadow    = obstacleShadowColor();
        double edge = 3.0;

        for (double[] b : obstacleBlocks) {
            double bx = b[0], by = b[1], bw = b[2], bh = b[3];

            gc.setFill(main);
            gc.fillRect(bx, by, bw, bh);

            gc.setFill(highlight);
            gc.fillRect(bx, by, bw, edge);
            gc.fillRect(bx, by, edge, bh);

            gc.setFill(shadow);
            gc.fillRect(bx, by + bh - edge, bw, edge);
            gc.fillRect(bx + bw - edge, by, edge, bh);

            gc.setStroke(shadow);
            gc.setLineWidth(0.8);
            gc.strokeLine(bx + bw * 0.35, by + edge + 2, bx + bw * 0.45, by + bh * 0.55);
            gc.strokeLine(bx + bw * 0.6,  by + bh * 0.4, bx + bw * 0.75, by + bh - edge - 2);
        }
    }

    private Color obstacleMainColor() {
        return switch (stage) {
            case 1  -> Color.rgb(72,  72,  90);
            case 2  -> Color.rgb(60,  75,  55);
            default -> Color.rgb(80,  60,  55);
        };
    }
    private Color obstacleHighlightColor() {
        return switch (stage) {
            case 1  -> Color.rgb(105, 105, 128);
            case 2  -> Color.rgb(88,  108, 80);
            default -> Color.rgb(115, 88,  80);
        };
    }
    private Color obstacleShadowColor() {
        return switch (stage) {
            case 1  -> Color.rgb(42, 42, 58);
            case 2  -> Color.rgb(36, 48, 33);
            default -> Color.rgb(50, 36, 33);
        };
    }

    private void drawPortal(GraphicsContext gc) {
        double cx = worldX + worldW / 2, cy = worldY + worldH / 2;
        gc.setFill(Color.color(0.4, 0.8, 1.0, 0.5));
        gc.fillOval(cx - 50, cy - 50, 100, 100);
        gc.setFill(Color.color(0.7, 0.95, 1.0));
        gc.fillOval(cx - 28, cy - 28, 56, 56);
        gc.setFill(Color.WHITE);
        gc.fillText("下一層 ▶", cx - 26, cy + 5);
    }

    private void drawRewardLabel(GraphicsContext gc) {
        double cx = worldX + worldW / 2, cy = worldY + worldH / 2;
        gc.setFill(Color.GOLD);
        gc.fillText("★ 獎勵房間 (TODO)", cx - 55, cy);
    }

}
