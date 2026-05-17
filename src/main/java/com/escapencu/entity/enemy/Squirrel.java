package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Stage 1 normal enemy — fast, low HP, shoots frequently. */
public class Squirrel extends Enemy {

    public Squirrel(double x, double y, int stage) {
        super(x, y, 28, 28, 15 * stage, 130, 3 * stage);
        shootCooldown = 0.9;
        bulletDamage  = 4 * stage;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
        shootAt(player.getCenterX(), player.getCenterY(), 240);
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setFill(Color.rgb(100, 65, 30));
        gc.fillRect(x, y, width, height);
        // Tiny ear bumps
        gc.setFill(Color.rgb(130, 85, 45));
        gc.fillOval(x + 3,          y - 5, 8, 8);
        gc.fillOval(x + width - 11, y - 5, 8, 8);
        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
        for (var b : bullets) b.draw(gc);
    }
}
