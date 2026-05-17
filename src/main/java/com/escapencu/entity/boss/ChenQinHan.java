package com.escapencu.entity.boss;

import com.escapencu.entity.EffectBullet;
import com.escapencu.entity.Player;
import com.escapencu.level.Room;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Stage 2 Boss — 沉沁汗
 * Attack 1: Burrows underground, appears under the player with a warning circle.
 * Attack 2: Shoots logic-gate styled bullets (AND/OR/XOR).
 */
public class ChenQinHan extends Boss {

    private final Random rng = new Random();

    // Burrow state
    private boolean burrowed    = false;
    private double  burrowCD    = 3.0;
    private double  burrowTimer = 0;
    private double  warningX, warningY;

    // Bullet attack cycle
    private double bulletTimer  = 1.5;
    private int    attackCycle  = 0;

    public ChenQinHan(double cx, double cy, int stage) {
        super(cx - 26, cy - 26, 52, 52, 300 * stage, 60, 14 * stage);
        bulletDamage  = 10 * stage;
        shootCooldown = 999; // managed manually
    }

    @Override
    protected void doAttack(Player player, double deltaTime) {
        // ── Burrow ────────────────────────────────────────────────────────
        if (burrowed) {
            burrowTimer -= deltaTime;
            if (burrowTimer <= 0) {
                // Emerge at warning position
                x = warningX - width  / 2;
                y = warningY - height / 2;
                invincible = false;
                burrowed   = false;
                burrowCD   = phase == 2 ? 2.5 : 4.0;
                // Stun player if close
                if (Math.hypot(getCenterX() - player.getCenterX(),
                               getCenterY() - player.getCenterY()) < 50) {
                    player.takeDamage(20);
                    player.applyStun(0.3);
                }
            }
            return; // no other action while underground
        }

        burrowCD -= deltaTime;
        if (burrowCD <= 0) {
            // Pick random floor position inside the room
            warningX  = roomX + Room.WALL + 80 + rng.nextDouble() * (roomW - Room.WALL * 2 - 160);
            warningY  = roomY + Room.WALL + 80 + rng.nextDouble() * (roomH - Room.WALL * 2 - 160);
            burrowed   = true;
            invincible = true;
            burrowTimer = 1.0;
            return;
        }

        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);

        // ── Logic-gate bullet attacks ─────────────────────────────────────
        bulletTimer -= deltaTime;
        if (bulletTimer <= 0) {
            fireLogicAttack(player);
            attackCycle = (attackCycle + 1) % 3;
            bulletTimer = phase == 2 ? 1.2 : 2.0;
        }
    }

    private void fireLogicAttack(Player player) {
        double dx   = player.getCenterX() - getCenterX();
        double dy   = player.getCenterY() - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        double nx = dx / dist, ny = dy / dist;

        switch (attackCycle) {
            case 0 -> { // AND — fast, straight, high damage
                double spd = 280;
                bullets.add(new EffectBullet(getCenterX(), getCenterY(),
                    nx * spd, ny * spd, 15, Color.LIMEGREEN, "AND", null));
            }
            case 1 -> { // OR — 3-way spread
                double spd = 200;
                for (int a = -1; a <= 1; a++) {
                    double angle = Math.atan2(ny, nx) + a * Math.toRadians(15);
                    bullets.add(new EffectBullet(getCenterX(), getCenterY(),
                        Math.cos(angle) * spd, Math.sin(angle) * spd,
                        10, Color.ORANGE, "OR", null));
                }
            }
            case 2 -> { // XOR — fan spread, unpredictable
                double spd = 220;
                double base = Math.atan2(ny, nx);
                for (int i = -2; i <= 2; i++) {
                    double angle = base + i * Math.toRadians(15);
                    bullets.add(new EffectBullet(getCenterX(), getCenterY(),
                        Math.cos(angle) * spd, Math.sin(angle) * spd,
                        8, Color.MEDIUMPURPLE, "XOR", null));
                }
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw warning circle when burrowed
        if (burrowed) {
            gc.setFill(Color.color(1.0, 0.9, 0.0, 0.35));
            gc.fillOval(warningX - 40, warningY - 40, 80, 80);
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.strokeOval(warningX - 40, warningY - 40, 80, 80);
            return; // boss body is underground
        }

        gc.setFill(phase == 2 ? Color.rgb(30, 100, 160) : Color.rgb(40, 130, 190));
        gc.fillRect(x, y, width, height);
        gc.setFill(Color.WHITE);
        gc.fillText("沉沁汗", x + 6, y + height / 2 + 5);

        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 10, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 10, width * (double) hp / maxHp, 5);
        }

        for (var b : bullets) b.draw(gc);
    }
}
