package com.escapencu.entity.boss;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Teaching assistant spawned when a Decoy is destroyed. Melee only, low HP. */
public class TAEnemy extends Enemy {

    // ── Sprites: [0]=S  [1]=N  [2]=E  [3]=W ──────────────────────────────
    private static final Image[] WALK   = loadDir("ta_walk");
    private static final Image[] ATTACK = loadDir("ta_attack");

    private static Image[] loadDir(String prefix) {
        String base = "/images/boss/wxg/ta/";
        return new Image[]{
            ResourceLoader.getImage(base + prefix + "_s.png", false),
            ResourceLoader.getImage(base + prefix + "_n.png", false),
            ResourceLoader.getImage(base + prefix + "_e.png", false),
            ResourceLoader.getImage(base + prefix + "_w.png", false)
        };
    }

    // ── State ──────────────────────────────────────────────────────────────
    private static final double ATTACK_DIST = 40.0;
    private int     facing     = 0;   // 0=S 1=N 2=E 3=W
    private boolean isAttacking = false;

    public TAEnemy(double x, double y, int stage) {
        super(x, y, 48, 48, 30 * stage, 110, 6 * stage);
        shootCooldown = 999;
        this.spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);

        double dx   = player.getCenterX() - getCenterX();
        double dy   = player.getCenterY() - getCenterY();
        double dist = Math.hypot(dx, dy);

        // Update facing direction
        if (Math.abs(dx) > Math.abs(dy)) facing = dx > 0 ? 2 : 3;
        else                             facing = dy > 0 ? 0 : 1;

        isAttacking = dist < ATTACK_DIST;
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
    }

    @Override
    public void draw(GraphicsContext gc) {
        Image[] frames = isAttacking ? ATTACK : WALK;
        Image img = frames[facing];

        if (img != null) {
            gc.drawImage(img, x, y, width, height);
        } else {
            // Fallback
            gc.setFill(Color.rgb(240, 220, 80));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.BLACK);
            gc.fillText(isAttacking ? "!!" : "TA", x + 3, y + 14);
        }

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 7, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 7, width * (double) hp / maxHp, 4);
        }
    }
}
