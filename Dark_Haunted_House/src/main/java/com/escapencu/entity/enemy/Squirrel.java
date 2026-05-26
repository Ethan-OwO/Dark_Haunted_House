package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Stage 1 normal enemy — fast melee attacker.
 *
 * Movement: A* pathfinding on a 48-px grid so the squirrel navigates
 *           around obstacles instead of getting stuck.
 * Combat  : Lunge when within LUNGE_RANGE (no projectiles).
 */
public class Squirrel extends Enemy {

    // ── Sprites ────────────────────────────────────────────────────────────
    private static final Image IMG_IDLE_N  = ResourceLoader.getImage("/images/enemy/squirrel/idle_n.png");
    private static final Image IMG_IDLE_S  = ResourceLoader.getImage("/images/enemy/squirrel/idle_s.png");
    private static final Image IMG_IDLE_E  = ResourceLoader.getImage("/images/enemy/squirrel/idle_e.png");
    private static final Image IMG_WALK1_N = ResourceLoader.getImage("/images/enemy/squirrel/walk1_n.png");
    private static final Image IMG_WALK1_S = ResourceLoader.getImage("/images/enemy/squirrel/walk1_s.png");
    private static final Image IMG_WALK1_E = ResourceLoader.getImage("/images/enemy/squirrel/walk1_e.png");

    private static final double DRAW_SIZE = 80.0;
    private static final double BOB_SPEED = 8.0;
    private static final double BOB_AMP   = 1.5;

    // ── Lunge mechanic ─────────────────────────────────────────────────────
    private static final double LUNGE_RANGE    = 110.0;
    private static final double LUNGE_SPEED    = 320.0;
    private static final double LUNGE_DURATION = 0.28;
    private static final double LUNGE_CD_MIN   = 1.2;

    private boolean lunging       = false;
    private double  lungeTimeLeft = 0;
    private double  lungeCD       = 0;
    private double  lungeDX, lungeDY;

    // ── A* pathfinding ─────────────────────────────────────────────────────
    /** Grid cell size — matches the obstacle BLOCK constant in NormalRoom. */
    private static final double CELL          = 48.0;
    /** Recalculate path at most this often (seconds). */
    private static final double PATH_INTERVAL = 0.4;
    /** Distance to a waypoint (px) before advancing to the next one. */
    private static final double WP_REACH      = CELL * 0.55;

    private List<double[]> path      = new ArrayList<>();
    private int            pathIdx   = 0;
    private double         pathTimer = 0.0;
    private double         lastGoalX = Double.NaN;
    private double         lastGoalY = Double.NaN;

    // ── Direction & animation ──────────────────────────────────────────────
    private enum Dir { N, S, E, W }
    private Dir    currentDir = Dir.S;
    private double bobTime    = 0;
    private double bobOffset  = 0;

    public Squirrel(double x, double y, int stage) {
        super(x, y, 28, 28, 15 * stage, 130, 3 * stage);
        shootCooldown = 999; // melee only
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);

        if (lungeCD > 0) lungeCD -= deltaTime;

        boolean moving;

        if (lunging) {
            // ── Lunge: fast dash in locked direction ───────────────────────
            moveBy(lungeDX * LUNGE_SPEED * deltaTime,
                   lungeDY * LUNGE_SPEED * deltaTime);
            clampToRoom();
            moving        = true;
            lungeTimeLeft -= deltaTime;
            if (lungeTimeLeft <= 0) {
                lunging = false;
                lungeCD = LUNGE_CD_MIN + Math.random() * 0.6;
            }

        } else {
            // ── A* path following ──────────────────────────────────────────
            double pcx = player.getCenterX();
            double pcy = player.getCenterY();
            double[] ct    = chaseTarget(pcx, pcy);
            double goalX   = ct[0], goalY = ct[1];

            // Recalculate when timer expires or goal moved more than one cell
            if (pathTimer <= 0
                    || Double.isNaN(lastGoalX)
                    || Math.hypot(goalX - lastGoalX, goalY - lastGoalY) > CELL) {
                path      = computePath(goalX, goalY);
                pathIdx   = 0;
                pathTimer = PATH_INTERVAL;
                lastGoalX = goalX;
                lastGoalY = goalY;
            }
            pathTimer -= deltaTime;

            // Current waypoint target
            double targetX = goalX, targetY = goalY;
            while (pathIdx < path.size()) {
                double[] wp   = path.get(pathIdx);
                double   dist = Math.hypot(wp[0] - getCenterX(), wp[1] - getCenterY());
                if (dist < WP_REACH) { pathIdx++; continue; }
                targetX = wp[0];
                targetY = wp[1];
                break;
            }

            updateDir(targetX, targetY);
            double prevX = x, prevY = y;
            moveToward(targetX, targetY, deltaTime);
            clampToRoom();
            moving = (Math.abs(x - prevX) > 0.001 || Math.abs(y - prevY) > 0.001);

            // Lunge trigger
            double dx   = pcx - getCenterX();
            double dy   = pcy - getCenterY();
            double dist = Math.hypot(dx, dy);
            if (lungeCD <= 0 && dist < LUNGE_RANGE && dist > 1) {
                lungeDX       = dx / dist;
                lungeDY       = dy / dist;
                lunging       = true;
                lungeTimeLeft = LUNGE_DURATION;
            }
        }

        if (moving) { bobTime += deltaTime; bobOffset = Math.sin(bobTime * BOB_SPEED) * BOB_AMP; }
        else        { bobTime  = 0;         bobOffset = 0; }
    }

    // ── A* implementation ──────────────────────────────────────────────────

    /**
     * Computes a list of world-coordinate waypoints from the squirrel's
     * current position to (goalX, goalY), navigating around obstacles.
     * Falls back to a direct line if no obstacle checker is set.
     */
    private List<double[]> computePath(double goalX, double goalY) {
        List<double[]> direct = new ArrayList<>();
        direct.add(new double[]{goalX, goalY});

        if (obstacleChecker == null) return direct;

        // Grid origin = inner room top-left
        double ox   = roomMinX;
        double oy   = roomMinY;
        int    cols = Math.max(1, (int) ((roomMaxX - roomMinX) / CELL));
        int    rows = Math.max(1, (int) ((roomMaxY - roomMinY) / CELL));

        // Mark cells blocked by obstacles (test with a slightly shrunk box
        // so the squirrel can still squeeze through narrow gaps)
        boolean[][] blocked = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                blocked[r][c] = !obstacleChecker.canMoveTo(
                        ox + c * CELL + 4, oy + r * CELL + 4,
                        CELL - 8, CELL - 8);

        // Convert world positions to grid cells
        int sc = cellOf(getCenterX() - ox, cols);
        int sr = cellOf(getCenterY() - oy, rows);
        int gc = cellOf(goalX - ox, cols);
        int gr = cellOf(goalY - oy, rows);

        if (sc == gc && sr == gr) return direct;

        // ── A* ────────────────────────────────────────────────────────────
        double[][] g = new double[rows][cols];
        for (double[] row : g) Arrays.fill(row, Double.MAX_VALUE);
        int[][] parentC = new int[rows][cols];
        int[][] parentR = new int[rows][cols];
        for (int[] row : parentC) Arrays.fill(row, -1);
        g[sr][sc] = 0;

        // Priority queue sorted by f = g + h
        PriorityQueue<int[]> open = new PriorityQueue<>(
                Comparator.comparingDouble(n ->
                        g[n[1]][n[0]] + octile(n[0], n[1], gc, gr)));
        open.add(new int[]{sc, sr});
        boolean[][] closed = new boolean[rows][cols];

        // 8-directional movement
        int[]    dc   = { 0,  1, 1, 1,  0, -1, -1, -1};
        int[]    dr   = {-1, -1, 0, 1,  1,  1,  0, -1};
        double[] cost = { 1, 1.414, 1, 1.414, 1, 1.414, 1, 1.414};

        boolean found = false;
        while (!open.isEmpty()) {
            int[] cur = open.poll();
            int cc = cur[0], cr = cur[1];
            if (closed[cr][cc]) continue;
            closed[cr][cc] = true;
            if (cc == gc && cr == gr) { found = true; break; }

            for (int d = 0; d < 8; d++) {
                int nc = cc + dc[d], nr = cr + dr[d];
                if (nc < 0 || nc >= cols || nr < 0 || nr >= rows) continue;
                if (closed[nr][nc] || blocked[nr][nc]) continue;
                // Diagonal: both axis-aligned neighbours must be free
                if (cost[d] > 1 && blocked[cr][cc + dc[d]] && blocked[cr + dr[d]][cc]) continue;

                double ng = g[cr][cc] + cost[d];
                if (ng < g[nr][nc]) {
                    g[nr][nc]       = ng;
                    parentC[nr][nc] = cc;
                    parentR[nr][nc] = cr;
                    open.add(new int[]{nc, nr});
                }
            }
        }

        if (!found) return direct; // no path found, head straight

        // Reconstruct path (cell centres) + exact goal at end
        List<double[]> waypoints = new ArrayList<>();
        int cc = gc, cr = gr;
        while (!(cc == sc && cr == sr)) {
            waypoints.add(0, new double[]{ox + (cc + 0.5) * CELL,
                                          oy + (cr + 0.5) * CELL});
            int pc = parentC[cr][cc], pr = parentR[cr][cc];
            cc = pc; cr = pr;
        }
        waypoints.add(new double[]{goalX, goalY});
        return waypoints;
    }

    /** Octile distance heuristic (admissible for 8-directional grids). */
    private static double octile(int c, int r, int gc, int gr) {
        int dx = Math.abs(c - gc), dy = Math.abs(r - gr);
        return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
    }

    /** Convert a world offset to a clamped grid index. */
    private static int cellOf(double offset, int max) {
        return Math.max(0, Math.min(max - 1, (int) (offset / CELL)));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void updateDir(double tx, double ty) {
        double deg = Math.toDegrees(Math.atan2(ty - getCenterY(), tx - getCenterX()));
        if      (deg >= -45  && deg <  45)  currentDir = Dir.E;
        else if (deg >=  45  && deg < 135)  currentDir = Dir.S;
        else if (deg >= -135 && deg < -45)  currentDir = Dir.N;
        else                                currentDir = Dir.W;
    }

    public boolean isLunging() { return lunging; }

    // ── Draw ───────────────────────────────────────────────────────────────

    @Override
    public void draw(GraphicsContext gc) {
        Image   frame  = pickFrame();
        boolean flip   = (currentDir == Dir.W);
        double  drawX  = getCenterX() - DRAW_SIZE / 2.0;
        double  scaleY = lunging ? 1.15 : 1.0;
        double  drawY  = getCenterY() - (DRAW_SIZE * scaleY) / 2.0 + bobOffset;

        if (frame != null) {
            if (flip) {
                gc.save();
                gc.translate(getCenterX(), 0);
                gc.scale(-1, 1);
                gc.drawImage(frame, -DRAW_SIZE / 2.0, drawY, DRAW_SIZE, DRAW_SIZE * scaleY);
                gc.restore();
            } else {
                gc.drawImage(frame, drawX, drawY, DRAW_SIZE, DRAW_SIZE * scaleY);
            }
        } else {
            gc.setFill(lunging ? Color.rgb(160, 100, 40) : Color.rgb(100, 65, 30));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.rgb(130, 85, 45));
            gc.fillOval(x + 3,           y - 5, 8, 8);
            gc.fillOval(x + width - 11,  y - 5, 8, 8);
        }

        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
        drawStatusEffects(gc);
    }

    private Image pickFrame() {
        Dir dir = (currentDir == Dir.W) ? Dir.E : currentDir;
        if (lunging || bobOffset != 0) return switch (dir) {
            case N -> IMG_WALK1_N;
            case S -> IMG_WALK1_S;
            default -> IMG_WALK1_E;
        };
        return switch (dir) {
            case N -> IMG_IDLE_N;
            case S -> IMG_IDLE_S;
            default -> IMG_IDLE_E;
        };
    }
}
