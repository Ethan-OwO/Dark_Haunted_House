package com.escapencu.entity.boss;

import com.escapencu.entity.Bullet;
import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;

/**
 * Fake copy of WuXiaoGuang.
 * Moves toward the player and fires a single slow bullet.
 * One hit destroys it and spawns a TAEnemy.
 */
public class Decoy extends Enemy {

    private final int     stage;
    private boolean       spawned = false;

    // ── Facing direction (mirrors WuXiaoGuang) ────────────────────────────
    private int facing = 0; // 0=S 1=N 2=E 3=W

    public Decoy(double x, double y, int stage) {
        super(x, y, 80, 80, 1, 40, 0); // speed=40, contactDamage=0 (kills via bullet)
        this.stage    = stage;
        shootCooldown = 4.5; // slower than the real boss
        bulletDamage  = 6 * stage;
        this.spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime); // bullet tick

        double dx   = player.getCenterX() - getCenterX();
        double dy   = player.getCenterY() - getCenterY();

        // Track facing direction
        if (Math.abs(dx) > Math.abs(dy)) facing = dx > 0 ? 2 : 3;
        else                             facing  = dy > 0 ? 0 : 1;

        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);

        // Fire a single bullet (uses Enemy.shootAt, no spread)
        if (shootTimer <= 0) {
            double dist = Math.hypot(dx, dy);
            if (dist > 1) {
                double speed = 180 + stage * 15;
                Bullet b = new Bullet(getCenterX(), getCenterY(),
                        (dx / dist) * speed, (dy / dist) * speed,
                        bulletDamage, false, 40);
                Image bulletImg = WuXiaoGuang.IMG_BULLET != null
                        ? WuXiaoGuang.IMG_BULLET : null;
                if (bulletImg != null) {
                    b.setImage(bulletImg);
                    b.setRotateToVelocity(true);
                }
                bullets.add(b);
                shootTimer = shootCooldown;
            }
        }
    }

    @Override
    public List<Entity> getPendingSpawns() {
        if (!isAlive() && !spawned) {
            spawned = true;
            return List.of(new TAEnemy(getCenterX() - 24, getCenterY() - 24, stage));
        }
        return Collections.emptyList();
    }

    @Override
    public void draw(GraphicsContext gc) {
        Image img = switch (facing) {
            case 1 -> WuXiaoGuang.IMG_N;
            case 2 -> WuXiaoGuang.IMG_E;
            case 3 -> WuXiaoGuang.IMG_W;
            default -> WuXiaoGuang.IMG_S;
        };

        if (img != null) {
            gc.drawImage(img, x, y, width, height);
        } else {
            gc.setFill(Color.rgb(100, 30, 120));
            gc.fillRect(x, y, width, height);
        }

        // HP bar (1 HP so this is mostly invisible unless it flickers)
        for (var b : bullets) b.draw(gc);
    }
}
