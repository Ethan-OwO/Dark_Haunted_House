package com.escapencu.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Bullet extends Entity {
    private final double  vx, vy;
    private final int     damage;
    private final boolean fromPlayer;
    private double age = 0;
    private static final double MAX_AGE = 3.0;
    private Image   image            = null;  // optional custom sprite
    private boolean rotateToVelocity = false; // if true, rotate image to face travel dir
    private final double angleDeg;            // pre-computed travel angle (degrees)

    private boolean ignoreWalls = false;
    public boolean isIgnoreWalls()                      { return ignoreWalls; }
    public void    setIgnoreWalls(boolean ignoreWalls)  { this.ignoreWalls = ignoreWalls; }

    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer) {
        this(cx, cy, vx, vy, damage, fromPlayer, 10);
    }

    /** Constructor with custom size (used by bosses for larger projectiles). */
    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer, double size) {
        super(cx - size / 2, cy - size / 2, size, size, 1);
        this.vx         = vx;
        this.vy         = vy;
        this.damage     = damage;
        this.fromPlayer = fromPlayer;
        this.angleDeg   = Math.toDegrees(Math.atan2(vy, vx));
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
        if (image != null) {
            if (rotateToVelocity) {
                // Rotate image so its right edge faces the travel direction
                gc.save();
                gc.translate(getCenterX(), getCenterY());
                gc.rotate(angleDeg);
                gc.drawImage(image, -width / 2, -height / 2, width, height);
                gc.restore();
            } else {
                gc.drawImage(image, x, y, width, height);
            }
        } else {
            gc.setFill(fromPlayer ? Color.YELLOW : Color.ORANGERED);
            gc.fillOval(x, y, width, height);
        }
    }

    public void setImage(Image img)                   { this.image = img; }
    public void setRotateToVelocity(boolean rotate)   { this.rotateToVelocity = rotate; }
    public void hit()                                 { alive = false; }
    public int  getDamage()                           { return damage; }
    public boolean isFromPlayer()                     { return fromPlayer; }
}
