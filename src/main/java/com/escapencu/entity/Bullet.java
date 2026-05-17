package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bullet extends Entity {
    private final double  vx, vy;
    private final int     damage;
    private final boolean fromPlayer;
    private double age = 0;
    private static final double MAX_AGE = 3.0; // seconds before auto-despawn

    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer) {
        super(cx - 5, cy - 5, 10, 10, 1);
        this.vx         = vx;
        this.vy         = vy;
        this.damage     = damage;
        this.fromPlayer = fromPlayer;
    }

    @Override
    public void update(double deltaTime) {
        x   += vx * deltaTime;
        y   += vy * deltaTime;
        age += deltaTime;
        if (age > MAX_AGE) alive = false;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(fromPlayer ? Color.YELLOW : Color.ORANGERED);
        gc.fillOval(x, y, width, height);
    }

    public void hit()             { alive = false; }
    public int  getDamage()       { return damage; }
    public boolean isFromPlayer() { return fromPlayer; }
}
