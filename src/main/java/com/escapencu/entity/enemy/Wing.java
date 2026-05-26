package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Fallen wings left on the floor by a Termite.
 *
 * Visual  : fallen_wings.png — stays on the floor permanently (until room is left).
 * Poison  : a green aura surrounds the wings for POISON_DURATION seconds.
 *           Standing inside applies poison; the aura fades and vanishes after that.
 * Combat  : contact damage = 0, cannot be shot down.
 */
public class Wing extends Enemy {

    private static final Image  IMG             = ResourceLoader.getImage("/images/enemy/termite/fallen_wings.png");
    private static final double RADIUS          = 40.0;
    private static final double DRAW_SIZE       = RADIUS * 2 * 1.3;
    private static final double POISON_DURATION = 3.0; // seconds the aura is active

    private double poisonTimer; // counts down from POISON_DURATION to 0

    public Wing(double cx, double cy) {
        super(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2, 999, 0, 0);
        shootCooldown = 999;
        poisonTimer   = POISON_DURATION;

        spawnTimer    = 0;
    }

    // ── Update ─────────────────────────────────────────────────────────────

    @Override
    public void update(double deltaTime) {
        if (poisonTimer > 0) poisonTimer -= deltaTime;
        // alive stays true forever — wings remain on the floor
    }

    @Override
    public void update(double deltaTime, Player player) {
        update(deltaTime);
        if (poisonTimer <= 0) return;           // aura expired, no more poison
        double dist = Math.hypot(player.getCenterX() - getCenterX(),
                player.getCenterY() - getCenterY());
        if (dist < RADIUS) player.applyPoison(1.0); // refresh each tick while inside
    }

    @Override
    public void takeDamage(int damage) { /* wings cannot be destroyed */ }

    /** Wings are permanent floor objects — they must not block room-clear. */
    @Override
    public boolean countsForRoomClear() { return false; }

    // ── Draw ───────────────────────────────────────────────────────────────

    @Override
    public void draw(GraphicsContext gc) {
        double cx   = getCenterX();
        double cy   = getCenterY();
        double half = DRAW_SIZE / 2.0;

        // ── Poison aura (only while active) ───────────────────────────────
        if (poisonTimer > 0) {
            // Outer aura fades out in the last 1 second
            double auraAlpha = Math.min(1.0, poisonTimer) * 0.38;
            gc.setFill(Color.color(0.35, 0.85, 0.25, auraAlpha));
            gc.fillOval(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);

            // Boundary ring
            double ringAlpha = Math.min(1.0, poisonTimer) * 0.75;
            gc.setStroke(Color.color(0.45, 0.95, 0.3, ringAlpha));
            gc.setLineWidth(2.0);
            gc.strokeOval(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);
        }

        // ── Wings sprite (permanent) ───────────────────────────────────────
        if (IMG != null) {
            gc.drawImage(IMG, cx - half, cy - half, DRAW_SIZE, DRAW_SIZE);
        } else {
            // Fallback
            gc.setFill(Color.color(0.6, 0.2, 0.8, 0.5));
            gc.fillOval(x, y, width, height);
        }
    }
}
