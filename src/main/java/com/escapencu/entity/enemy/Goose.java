package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Stage 2 normal enemy — slow tank that periodically charges at the player. */
public class Goose extends Enemy {

    private boolean charging    = false;
    private double  chargeTimer = 0;
    private double  chargeCD;
    private double  chargeDX, chargeDY;

    public Goose(double x, double y, int stage) {
        super(x, y, 50, 50, 70 * stage, 35, 12 * stage);
        shootCooldown = 999; // melee only
        chargeCD      = 2.5 + Math.random() * 1.5; // first charge in 2.5-4s
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);

        if (charging) {
            x += chargeDX * 220 * deltaTime;
            y += chargeDY * 220 * deltaTime;
            chargeTimer -= deltaTime;
            if (chargeTimer <= 0) {
                charging = false;
                chargeCD = 3.0 + Math.random();
            }
        } else {
            chargeCD -= deltaTime;
            moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
            if (chargeCD <= 0) {
                double dx   = player.getCenterX() - getCenterX();
                double dy   = player.getCenterY() - getCenterY();
                double dist = Math.hypot(dx, dy);
                if (dist > 1) {
                    chargeDX    = dx / dist;
                    chargeDY    = dy / dist;
                    charging    = true;
                    chargeTimer = 1.5;
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(charging ? Color.ORANGERED : Color.rgb(230, 230, 220));
        gc.fillRect(x, y, width, height);
        // Simple beak
        gc.setFill(Color.ORANGE);
        gc.fillRect(x + width - 4, y + height / 2 - 4, 8, 8);
        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
    }
}
