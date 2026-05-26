package com.escapencu.entity.boss;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Teaching assistant spawned when a Decoy is destroyed. Melee only, low HP. */
public class TAEnemy extends Enemy {

    public TAEnemy(double x, double y, int stage) {
        super(x, y, 22, 22, 8 * stage, 110, 6 * stage);
        shootCooldown = 999;
        this.spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.rgb(240, 220, 80));
        gc.fillRect(x, y, width, height);
        gc.setFill(Color.BLACK);
        gc.fillText("TA", x + 3, y + 14);
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 7, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 7, width * (double) hp / maxHp, 4);
        }
    }
}
