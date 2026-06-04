package com.escapencu.entity.boss;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Stationary floor trap placed by WuXiaoGuang. Explodes when player steps near. */
public class Mine extends Enemy {

    private static final Image IMG_IDLE  = ResourceLoader.getImage("/images/boss/wxg/mine/mine_idle.png",  false);
    private static final Image IMG_FUSED = ResourceLoader.getImage("/images/boss/wxg/mine/mine_fused.png", false);

    private static final double PROXIMITY     = 35.0;
    private static final int    EXPLOSION_DMG = 20;

    private boolean fuseStarted = false;
    private double  fuseTimer   = 0;
    private double  blinkTimer  = 0;
    private boolean blinkOn     = true;

    public Mine(double cx, double cy) {
        super(cx - 21, cy - 21, 42, 42, 1, 0, 0);
        shootCooldown = 999;
        this.spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime, Player player) {
        blinkTimer += deltaTime;
        if (blinkTimer >= 0.3) { blinkTimer = 0; blinkOn = !blinkOn; }
    }

    /** Returns true immediately when player steps within proximity — instant explosion. */
    public boolean checkProximity(Player player) {
        if (!isAlive()) return false;
        double dist = Math.hypot(player.getCenterX() - getCenterX(),
                player.getCenterY() - getCenterY());
        return dist < PROXIMITY;
    }

    public int getExplosionDamage() { return EXPLOSION_DMG; }

    @Override
    public void draw(GraphicsContext gc) {
        if (!blinkOn) return;

        Image img = fuseStarted ? IMG_FUSED : IMG_IDLE;
        if (img != null) {
            gc.drawImage(img, x, y, width, height);
        } else {
            // Fallback
            gc.setFill(fuseStarted ? Color.rgb(255, 80, 0) : Color.rgb(220, 200, 30));
            gc.fillOval(x, y, width, height);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            gc.strokeOval(x, y, width, height);
            gc.setFill(Color.BLACK);
            gc.fillText("!", getCenterX() - 3, getCenterY() + 4);
        }
    }
}
