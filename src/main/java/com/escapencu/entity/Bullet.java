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

    private javafx.scene.image.Image bulletImage = null;

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

    public Bullet(double cx, double cy, double vx, double vy, int damage, boolean fromPlayer, javafx.scene.image.Image img) {
        this(cx, cy, vx, vy, damage, fromPlayer); // 呼叫上方的基礎建構子
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
        if (bulletImage != null) {
            double angleRad = Math.atan2(vy, vx); // 算出子彈目前飛行的速度弧度
            double angleDeg = Math.toDegrees(angleRad); // 轉換為角度

            gc.save();
            // 將畫筆移動到子彈實體的中心點（因為 x, y 是左上角）
            gc.translate(x + width / 2, y + height / 2);
            gc.rotate(angleDeg); // 旋轉子彈圖片使其尖端朝前

            // 你的黃金子彈是長方形，這裡可微調它在畫面上的視覺大小 (寬16, 高8)
            double bulletW = 16;
            double bulletH = 8;
            gc.drawImage(bulletImage, -bulletW / 2, -bulletH / 2, bulletW, bulletH);

            gc.restore();
        } else {
            // 沒有圖片的普通子彈（或敵人子彈）
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
