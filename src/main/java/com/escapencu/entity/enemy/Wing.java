package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Dropped by a Termite on death.
 * Creates a poison zone on the floor for 1-2 seconds.
 * Standing inside the circle applies poison; leaving gives ~1 s residual.
 */
public class Wing extends Enemy {

    private static final double RADIUS     = 35.0;
    private static final Random RNG        = new Random();

    private double lifetime;

    public Wing(double cx, double cy) {
        super(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2, 999, 0, 0);
        shootCooldown = 999;
        lifetime      = 1.0 + RNG.nextDouble(); // 1.0 – 2.0 seconds
    }

    /** Lifetime tick — called by Room's base e.update(dt) loop. */
    @Override
    public void update(double deltaTime) {
        lifetime -= deltaTime;
        if (lifetime <= 0) alive = false;
    }

    /** Lifetime + poison check — Room calls this each frame. */
    @Override
    public void update(double deltaTime, Player player) {
        update(deltaTime); // tick lifetime
        if (!alive) return;
        double dist = Math.hypot(player.getCenterX() - getCenterX(),
                                 player.getCenterY() - getCenterY());
        // Refresh poison to 1 s each tick while inside; fades naturally on exit
        if (dist < RADIUS) player.applyPoison(1.0);
    }

    @Override
    public void takeDamage(int damage) { /* wings cannot be shot down */ }

    @Override
    public void draw(GraphicsContext gc) {
        if (!alive) return;
        double alpha = Math.min(0.55, lifetime * 0.4);
        gc.setFill(Color.color(0.6, 0.2, 0.8, alpha));
        gc.fillOval(x, y, width, height);
        gc.setStroke(Color.color(0.8, 0.4, 1.0, alpha + 0.1));
        gc.setLineWidth(1.5);
        gc.strokeOval(x, y, width, height);
        // Wing icon
        gc.setFill(Color.color(1.0, 1.0, 1.0, alpha));
        gc.fillText("翅", getCenterX() - 5, getCenterY() + 4);
    }
}
