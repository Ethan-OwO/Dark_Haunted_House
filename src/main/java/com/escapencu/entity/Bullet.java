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
    private Image   image            = null;
    private boolean rotateToVelocity = false;
    private final double angleDeg;
    private boolean ignoreWalls = false;
    private javafx.scene.image.Image bulletImage = null;

    public boolean isIgnoreWalls()                     { return ignoreWalls; }
    public void    setIgnoreWalls(boolean ignoreWalls) { this.ignoreWalls = ignoreWalls; }

    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer) {
        this(cx, cy, vx, vy, damage, fromPlayer, 10);
    }

    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer, double size) {
        super(cx - size / 2, cy - size / 2, size, size, 1);
        this.vx         = vx;
        this.vy         = vy;
        this.damage     = damage;
        this.fromPlayer = fromPlayer;
        this.angleDeg   = Math.toDegrees(Math.atan2(vy, vx));
    }

    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer, javafx.scene.image.Image img) {
        this(cx, cy, vx, vy, damage, fromPlayer);
        this.bulletImage = img;
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
                gc.save();
                gc.translate(getCenterX(), getCenterY());
                gc.rotate(angleDeg);
                gc.drawImage(image, -width / 2, -height / 2, width, height);
                gc.restore();
            } else {
                gc.drawImage(image, x, y, width, height);
            }
        } else {
            if (bulletImage != null) {
                double angleRad = Math.atan2(vy, vx);
                double angleDeg = Math.toDegrees(angleRad);
                gc.save();
                gc.translate(x + width / 2, y + height / 2);
                gc.rotate(angleDeg);
                double bulletW = 16;
                double bulletH = 8;
                gc.drawImage(bulletImage, -bulletW / 2, -bulletH / 2, bulletW, bulletH);
                gc.restore();
            } else {
                gc.setFill(fromPlayer ? Color.YELLOW : Color.ORANGERED);
                gc.fillOval(x, y, width, height);
            }
        }  // ← 這個 } 是原本漏掉的
    }

    public void setImage(Image img)                 { this.image = img; }
    public void setRotateToVelocity(boolean rotate) { this.rotateToVelocity = rotate; }
    public void hit()                               { alive = false; }
    public int  getDamage()                         { return damage; }
    public boolean isFromPlayer()                   { return fromPlayer; }
}
