package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/**
 * A homing variant of EffectBullet.
 * - Flies straight for HOMING_DELAY seconds, then steers toward the player.
 * - Fades out during the last 0.5 seconds before disappearing.
 * - Auto-expires after MAX_AGE seconds.
 */
public class HomingEffectBullet extends EffectBullet {

    private static final double HOMING_SPEED = 230.0; // px/s homing velocity
    private static final double STEER_RATE   = 3.0;   // turn sharpness
    private static final double HOMING_DELAY = 0.5;   // seconds of straight flight before tracking
    private static final double MAX_AGE      = 1.5;   // total lifetime
    private static final double FADE_START   = 1.0;   // when transparency fade begins

    private final Player target;
    private double homVx, homVy; // mutable velocity
    private double homAge = 0;

    private final Image  icon;
    private final double sz;

    public HomingEffectBullet(double cx, double cy,
                              double vx, double vy,
                              int damage,
                              Color color, String label,
                              Consumer<Player> onHit,
                              double size,
                              Player target,
                              Image icon) {
        super(cx, cy, vx, vy, damage, color, label, onHit, size);
        this.target = target;
        this.homVx  = vx;
        this.homVy  = vy;
        this.icon   = icon;
        this.sz     = size;
    }

    @Override
    public void update(double deltaTime) {
        homAge += deltaTime;

        if (homAge > HOMING_DELAY) {
            // Start steering toward the player after the delay
            double dx   = target.getCenterX() - getCenterX();
            double dy   = target.getCenterY() - getCenterY();
            double dist = Math.hypot(dx, dy);
            if (dist > 1) {
                double tx = (dx / dist) * HOMING_SPEED;
                double ty = (dy / dist) * HOMING_SPEED;
                homVx += (tx - homVx) * STEER_RATE * deltaTime;
                homVy += (ty - homVy) * STEER_RATE * deltaTime;
            }
        }

        x += homVx * deltaTime;
        y += homVy * deltaTime;

        if (homAge > MAX_AGE) alive = false;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Fade out during the last 0.5 seconds
        double alpha = homAge > FADE_START
                ? Math.max(0, 1.0 - (homAge - FADE_START) / (MAX_AGE - FADE_START))
                : 1.0;

        if (icon != null) {
            double angle = Math.toDegrees(Math.atan2(homVy, homVx));
            gc.save();
            gc.setGlobalAlpha(alpha);
            gc.translate(getCenterX(), getCenterY());
            gc.rotate(angle);
            gc.drawImage(icon, -sz / 2, -sz / 2, sz, sz);
            gc.restore();
            gc.setGlobalAlpha(1.0);
        } else {
            gc.setGlobalAlpha(alpha);
            super.draw(gc);
            gc.setGlobalAlpha(1.0);
        }
    }
}
