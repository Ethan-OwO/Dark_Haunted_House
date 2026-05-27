package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base enemy class.
 * Subclasses (Squirrel, Goose, Termite, bosses) override update() for custom AI.
 * Call super.update(deltaTime) to handle bullet movement and shoot timer.
 */
public class Enemy extends Entity {
    protected double speed;
    protected int    contactDamage;
    protected double shootCooldown;
    protected double shootTimer = 0;
    protected int    bulletDamage;
    // ▼▼▼ 修改點 1：新增生成緩衝相關變數與方法 ▼▼▼
    protected double spawnTimer = 1.0; // 預設 1 秒的生成緩衝

    public boolean isSpawning() { return spawnTimer > 0; }

    public void decrementSpawnTimer(double dt) { spawnTimer -= dt; }

    public void drawSpawnWarning(GraphicsContext gc) {
        double cx = getCenterX();
        double cy = getCenterY();
        // 讓光圈稍微比怪物本身大一點
        double r = Math.max(width, height) / 2.0 * 1.3;
        // 利用 sin 函數製作閃爍效果
        double alpha = 0.2 + 0.6 * Math.abs(Math.sin(spawnTimer * 15));

        gc.setFill(Color.color(1.0, 0.2, 0.2, alpha));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        gc.setStroke(Color.color(1.0, 0.0, 0.0, 0.8));
        gc.setLineWidth(2.0);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }
    // ▲▲▲ 修改結束 ▲▲▲

    // ── Coin drop ──────────────────────────────────────────────────────────
    private boolean coinDropped = false;

    /** Whether coins have already been spawned for this enemy's death. */
    public boolean hasCoinDropped() { return coinDropped; }
    public void    markCoinDropped() { coinDropped = true; }

    /**
     * Number of coins (each worth 1) dropped when this enemy dies.
     * Scales with maxHp so tougher enemies drop more.
     * Override in Boss for a larger reward.
     */
    public int getCoinValue() { return 2 + maxHp / 20; }

    protected final List<Bullet> bullets = new ArrayList<>();

    // ── Status effect timers (applied by book effects) ─────────────────────
    private static final double ENEMY_SLOW_FACTOR  = 0.45;
    private static final double ENEMY_BURN_TICK    = 0.4;   // seconds between burn ticks
    private static final int    ENEMY_BURN_DAMAGE  = 3;

    private double enemySlowTimer  = 0;
    private double enemyBurnTimer  = 0;
    private double enemyBurnTick   = 0;

    /** Slow the enemy's movement for the given duration (stacks by taking the max). */
    public void applySlow(double duration) {
        enemySlowTimer = Math.max(enemySlowTimer, duration);
    }

    /** Set the enemy on fire for the given duration (stacks by taking the max). */
    public void applyBurn(double duration) {
        enemyBurnTimer = Math.max(enemyBurnTimer, duration);
        if (enemyBurnTimer > 0 && enemyBurnTick <= 0) enemyBurnTick = ENEMY_BURN_TICK;
    }

    public boolean isEnemySlowed()  { return enemySlowTimer > 0; }
    public boolean isEnemyBurning() { return enemyBurnTimer > 0; }

    // ── Room bounds (set by Room when spawning) ────────────────────────────
    protected double roomMinX = Double.NEGATIVE_INFINITY;
    protected double roomMinY = Double.NEGATIVE_INFINITY;
    protected double roomMaxX = Double.POSITIVE_INFINITY;
    protected double roomMaxY = Double.POSITIVE_INFINITY;

    /** Called by the room that owns this enemy so it won't walk through walls. */
    public void setRoomBounds(double minX, double minY, double maxX, double maxY) {
        roomMinX = minX;
        roomMinY = minY;
        roomMaxX = maxX;
        roomMaxY = maxY;
    }

    // ── Obstacle checker (set by NormalRoom when spawning) ─────────────────
    protected AreaChecker obstacleChecker = null;

    /**
     * When true, this enemy ignores room obstacles entirely (e.g. Termite alate
     * phase hovering over furniture).  Room walls (roomBounds) still apply.
     */
    protected boolean ignoreObstacles = false;

    /**
     * Supplies a checker so the enemy cannot walk through room obstacles.
     * canMoveTo returns true when the given rectangle is free of obstacles.
     */
    public void setObstacleChecker(AreaChecker checker) {
        this.obstacleChecker = checker;
    }

    // ── Steering: separation from other enemies ────────────────────────────
    private List<Enemy> steeringNeighbors = Collections.emptyList();

    // Separation: push away from enemies within this radius
    private static final double SEP_RADIUS   = 70.0;
    private static final double SEP_WEIGHT   = 0.55;
    // Obstacle probe: look this far ahead to steer around obstacles
    private static final double PROBE_DIST   = 52.0;
    private static final double AVOID_WEIGHT = 1.4;

    /**
     * Called by Room.update() each frame before this enemy updates.
     * Provides the neighbor list for separation steering.
     */
    public void setSteeringNeighbors(List<Enemy> neighbors) {
        this.steeringNeighbors = neighbors;
    }

    // ── Encirclement: each enemy approaches from a personal angle ──────────
    /** Randomised once at spawn — determines which side of the player this enemy approaches from. */
    private final double approachAngle  = Math.random() * Math.PI * 2;
    private static final double ENCIRCLE_RADIUS = 85.0; // px offset from player centre

    /**
     * Returns the effective movement target for chasing the player.
     * When far, returns a point beside the player at this enemy's personal
     * approach angle so groups of enemies encircle rather than stack.
     * When already close, returns the player's exact position.
     */
    protected double[] chaseTarget(double playerX, double playerY) {
        double dist = Math.hypot(playerX - getCenterX(), playerY - getCenterY());
        if (dist < ENCIRCLE_RADIUS * 1.5) return new double[]{playerX, playerY};
        double tx = playerX + Math.cos(approachAngle) * ENCIRCLE_RADIUS;
        double ty = playerY + Math.sin(approachAngle) * ENCIRCLE_RADIUS;
        // Keep target inside room bounds
        tx = Math.max(roomMinX, Math.min(roomMaxX, tx));
        ty = Math.max(roomMinY, Math.min(roomMaxY, ty));
        return new double[]{tx, ty};
    }

    /**
     * Clamps x/y to the walkable room area.
     * @return true if the position was actually clamped (hit a wall).
     */
    protected boolean clampToRoom() {
        double cx = Math.max(roomMinX, Math.min(x, roomMaxX - width));
        double cy = Math.max(roomMinY, Math.min(y, roomMaxY - height));
        boolean hit = (cx != x || cy != y);
        x = cx;
        y = cy;
        return hit;
    }

    public Enemy(double x, double y, double width, double height,
                 int hp, double speed, int contactDamage) {
        super(x, y, width, height, hp);
        this.speed         = speed;
        this.contactDamage = contactDamage;
        this.bulletDamage  = contactDamage;
        this.shootCooldown = 2.0;
    }

    @Override
    public void update(double deltaTime) {
        if (shootTimer > 0) shootTimer -= deltaTime;
        bullets.removeIf(b -> !b.isAlive());
        for (Bullet b : bullets) b.update(deltaTime);

        // ── Status effect ticks ────────────────────────────────────────────
        if (enemySlowTimer > 0) enemySlowTimer -= deltaTime;
        if (enemyBurnTimer > 0) {
            enemyBurnTimer -= deltaTime;
            enemyBurnTick  -= deltaTime;
            if (enemyBurnTick <= 0) {
                takeDamage(ENEMY_BURN_DAMAGE);
                enemyBurnTick = ENEMY_BURN_TICK;
            }
        }
    }

    // ── AI helpers (call from subclass or TestRoom) ───────────────────────

    /**
     * Move toward (tx, ty) using combined steering:
     *   Seek       — head toward the target
     *   Separation — push away from nearby enemies so they don't pile up
     *   Obstacle probe — steer around obstacles detected ahead
     *
     * Subclasses can call this with any target (player, flank point, etc.)
     * and the steering handles the rest.
     */
    public void moveToward(double tx, double ty, double deltaTime) {
        double dx   = tx - getCenterX();
        double dy   = ty - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;

        // ── Seek (normalised direction toward target) ──────────────────────
        double seekX = dx / dist;
        double seekY = dy / dist;

        // ── Separation: push away from nearby live enemies ─────────────────
        double sepX = 0, sepY = 0;
        for (Enemy n : steeringNeighbors) {
            if (n == this || !n.isAlive()) continue;
            double sdx   = getCenterX() - n.getCenterX();
            double sdy   = getCenterY() - n.getCenterY();
            double sdist = Math.hypot(sdx, sdy);
            if (sdist > 0.1 && sdist < SEP_RADIUS) {
                double str = (SEP_RADIUS - sdist) / SEP_RADIUS; // 0→1, stronger when closer
                sepX += (sdx / sdist) * str;
                sepY += (sdy / sdist) * str;
            }
        }

        // ── Obstacle probe: three feelers ahead ───────────────────────────
        double avoidX = 0, avoidY = 0;
        if (obstacleChecker != null && !ignoreObstacles) {
            double perpX = -seekY, perpY = seekX; // unit vector perpendicular (left)
            // Centre probe, right-diagonal probe, left-diagonal probe
            double[][] offs = {
                    { seekX * PROBE_DIST,
                            seekY * PROBE_DIST },
                    { seekX * PROBE_DIST * 0.7 + perpX * PROBE_DIST * 0.7,
                            seekY * PROBE_DIST * 0.7 + perpY * PROBE_DIST * 0.7 },
                    { seekX * PROBE_DIST * 0.7 - perpX * PROBE_DIST * 0.7,
                            seekY * PROBE_DIST * 0.7 - perpY * PROBE_DIST * 0.7 }
            };
            double[][] push = {
                    { -seekX, -seekY },   // centre blocked  → push back
                    { -perpX, -perpY },   // right blocked   → push left
                    {  perpX,  perpY }    // left  blocked   → push right
            };
            for (int i = 0; i < 3; i++) {
                double px = getCenterX() + offs[i][0] - width  / 2;
                double py = getCenterY() + offs[i][1] - height / 2;
                if (!obstacleChecker.canMoveTo(px, py, width, height)) {
                    avoidX += push[i][0];
                    avoidY += push[i][1];
                }
            }
        }

        // ── Combine all forces into one normalised direction ───────────────
        double finalX = seekX + sepX * SEP_WEIGHT + avoidX * AVOID_WEIGHT;
        double finalY = seekY + sepY * SEP_WEIGHT + avoidY * AVOID_WEIGHT;
        double mag    = Math.hypot(finalX, finalY);
        if (mag > 0.001) { finalX /= mag; finalY /= mag; }
        else             { finalX = seekX; finalY = seekY; } // fallback: seek only

        double effectiveSpeed = speed * (enemySlowTimer > 0 ? ENEMY_SLOW_FACTOR : 1.0);
        moveBy(finalX * effectiveSpeed * deltaTime, finalY * effectiveSpeed * deltaTime);
    }

    /**
     * Move by (dx, dy), sliding along obstacles when the full vector is blocked.
     * Tries the full move first; if blocked, tries X-only then Y-only (wall-slide).
     *
     * @return true if any movement occurred, false if completely stuck.
     */
    protected boolean moveBy(double dx, double dy) {
        if (obstacleChecker == null || ignoreObstacles) {
            x += dx;
            y += dy;
            return true;
        }
        // Full move free?
        if (obstacleChecker.canMoveTo(x + dx, y + dy, width, height)) {
            x += dx;
            y += dy;
            return true;
        }
        // Try sliding along each axis independently
        boolean movedX = false, movedY = false;
        if (Math.abs(dx) > 0.001 && obstacleChecker.canMoveTo(x + dx, y, width, height)) {
            x += dx;
            movedX = true;
        }
        if (Math.abs(dy) > 0.001 && obstacleChecker.canMoveTo(x, y + dy, width, height)) {
            y += dy;
            movedY = true;
        }
        return movedX || movedY;
    }

    /** Fire a bullet toward a world position (respects cooldown). */
    public void shootAt(double tx, double ty, double bulletSpeed) {
        if (shootTimer > 0) return;
        double dx   = tx - getCenterX();
        double dy   = ty - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        bullets.add(new Bullet(getCenterX(), getCenterY(),
                (dx / dist) * bulletSpeed,
                (dy / dist) * bulletSpeed,
                bulletDamage, false));
        shootTimer = shootCooldown;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.SALMON);
        gc.fillRect(x, y, width, height);

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 5);
        }

        drawStatusEffects(gc);
        for (Bullet b : bullets) b.draw(gc);
    }

    /**
     * Draw small status-effect badges above the enemy.
     * Call this at the end of any subclass draw() that wants to show these.
     */
    protected void drawStatusEffects(GraphicsContext gc) {
        double bx = x;
        double by = y - 18;   // sit just above the HP bar row
        if (enemyBurnTimer > 0) {
            gc.setFill(Color.ORANGE);
            gc.fillText("🔥", bx, by);
            bx += 18;
        }
        if (enemySlowTimer > 0) {
            gc.setFill(Color.LIGHTBLUE);
            gc.fillText("❄", bx, by);
        }
    }

    /** Player-aware update overload — subclasses override for AI logic. */
    public void update(double deltaTime, Player player) { update(deltaTime); }

    /** Returns entities to spawn this tick (e.g., Termite minions on death). */
    public List<Entity> getPendingSpawns() { return Collections.emptyList(); }

    public List<Bullet> getBullets()       { return bullets; }
    public int          getContactDamage() { return contactDamage; }
}
