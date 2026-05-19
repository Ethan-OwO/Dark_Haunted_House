package com.escapencu.entity;

import com.escapencu.core.GameState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class FirePatch {
    public final double x, y, radius;
    private double lifetime  = 10.0;
    private double tickTimer = 0;
    private boolean alive    = true;

    private static final double TICK = 0.5;
    private static final int    DMG  = 3;

    public FirePatch(double x, double y, double radius) {
        this.x      = x;
        this.y      = y;
        this.radius = radius;
    }

    public void update(double dt, Player player, List<Entity> enemies) {
        lifetime -= dt;
        if (lifetime <= 0) { alive = false; return; }

        tickTimer -= dt;
        if (tickTimer <= 0) {
            tickTimer = TICK;
            double pd = Math.hypot(player.getCenterX() - x, player.getCenterY() - y);
            if (pd < radius && !GameState.opMode) player.takeDamage(DMG);

            for (Entity e : enemies) {
                if (!e.isAlive()) continue;
                double ed = Math.hypot(e.getCenterX() - x, e.getCenterY() - y);
                if (ed < radius) e.takeDamage(DMG);
            }
        }
    }

    public void draw(GraphicsContext gc) {
        // 最後 2 秒開始淡出
        double alpha = Math.min(1.0, lifetime / 2.0) * 0.55;
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.ORANGERED);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setFill(Color.YELLOW);
        double ir = radius * 0.4;
        gc.fillOval(x - ir, y - ir, ir * 2, ir * 2);
        gc.setGlobalAlpha(1.0);
    }

    public boolean isAlive() { return alive; }
}
