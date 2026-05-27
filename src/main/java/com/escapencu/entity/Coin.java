package com.escapencu.entity;

import com.escapencu.core.GameState;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * A dropped coin in the world.
 * When the player comes within MAGNET_RADIUS, the coin accelerates toward the
 * player like a magnet. On contact (COLLECT_RADIUS) it is absorbed and adds
 * its value to GameState.score.
 *
 * Animated using the 6-frame Coin.png spritesheet.
 */
public class Coin {

    // ── Spritesheet ────────────────────────────────────────────────────────
    private static final Image  SHEET       = ResourceLoader.getImage("/images/2D Chests & Coins/Coin.png", false);
    private static final int    FRAME_COUNT = 6;
    private static final double FRAME_DUR   = 0.10;   // seconds per frame
    private static final double DRAW_SIZE   = 48.0;   // display size (upscaled)

    // ── Pickup physics ─────────────────────────────────────────────────────
    private static final double MAGNET_RADIUS  = 150.0;
    private static final double COLLECT_RADIUS =  18.0;
    private static final double MAGNET_FORCE   = 1400.0; // px/s² at center
    private static final double MAX_SPEED      = 700.0;
    private static final double FRICTION_EXP   = 0.80;   // velocity × this^(dt*60) when idle

    // ── State ──────────────────────────────────────────────────────────────
    private double  x, y;
    private double  vx, vy;
    private final int value;
    private boolean collected = false;

    // ── Animation ──────────────────────────────────────────────────────────
    private double animTimer = 0;
    private int    animFrame;   // stagger start frame so coins don't all look identical

    public Coin(double x, double y, int value) {
        this.x      = x;
        this.y      = y;
        this.value  = value;
        // Random stagger so sibling coins are out of phase
        this.animFrame = (int)(Math.random() * FRAME_COUNT);
        // Scatter on spawn
        double angle = Math.random() * Math.PI * 2;
        double speed = 45 + Math.random() * 75;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
    }

    // ── Update ─────────────────────────────────────────────────────────────
    public void update(double dt, Player player) {
        if (collected) return;

        double pcx  = player.getCenterX();
        double pcy  = player.getCenterY();
        double dx   = pcx - x;
        double dy   = pcy - y;
        double dist = Math.hypot(dx, dy);

        // Collect
        if (dist < COLLECT_RADIUS) {
            collected = true;
            GameState.score += value;
            return;
        }

        // Magnetic pull (quadratic — stronger near center)
        if (dist < MAGNET_RADIUS && dist > 0) {
            double ratio = 1.0 - dist / MAGNET_RADIUS;
            double force = MAGNET_FORCE * ratio * ratio;
            vx += (dx / dist) * force * dt;
            vy += (dy / dist) * force * dt;
        } else {
            // Idle friction — scatter coins slow down naturally
            double friction = Math.pow(FRICTION_EXP, dt * 60);
            vx *= friction;
            vy *= friction;
        }

        // Speed cap
        double spd = Math.hypot(vx, vy);
        if (spd > MAX_SPEED) { vx = vx / spd * MAX_SPEED; vy = vy / spd * MAX_SPEED; }

        x += vx * dt;
        y += vy * dt;

        // Animation
        animTimer += dt;
        if (animTimer >= FRAME_DUR) {
            animTimer -= FRAME_DUR;
            animFrame = (animFrame + 1) % FRAME_COUNT;
        }
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    public void draw(GraphicsContext gc) {
        if (collected) return;
        if (SHEET != null) {
            double fw = SHEET.getWidth() / FRAME_COUNT;
            double fh = SHEET.getHeight();
            gc.drawImage(SHEET,
                    animFrame * fw, 0, fw, fh,
                    x - DRAW_SIZE / 2, y - DRAW_SIZE / 2, DRAW_SIZE, DRAW_SIZE);
        } else {
            // Fallback circle
            gc.setFill(Color.GOLD);
            gc.fillOval(x - 7, y - 7, 14, 14);
        }
    }

    public boolean isCollected() { return collected; }

    /**
     * Clamps the coin to stay inside the given rectangle (inner room bounds).
     * Called by Room.update() every frame after position is updated.
     */
    public void clampToBounds(double minX, double minY, double maxX, double maxY) {
        if (collected) return;
        double hw = DRAW_SIZE / 2;
        if (x - hw < minX) { x = minX + hw; if (vx < 0) vx = 0; }
        if (x + hw > maxX) { x = maxX - hw; if (vx > 0) vx = 0; }
        if (y - hw < minY) { y = minY + hw; if (vy < 0) vy = 0; }
        if (y + hw > maxY) { y = maxY - hw; if (vy > 0) vy = 0; }
    }
}
