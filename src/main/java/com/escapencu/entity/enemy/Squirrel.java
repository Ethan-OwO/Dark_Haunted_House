package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Stage 1 normal enemy — fast melee attacker.
 *
 * Behaviour:
 *   - Walks toward the player at moderate speed.
 *   - When within LUNGE_RANGE, performs a short burst-lunge directly at the
 *     player (speed ×2.5 for LUNGE_DURATION seconds), then enters cooldown.
 *   - Damage is dealt by GameScene's contact overlap check (contactDamage).
 *   - No projectiles.
 */
public class Squirrel extends Enemy {

    // ── Sprites ────────────────────────────────────────────────────────────
    private static final Image IMG_IDLE_N  = ResourceLoader.getImage("/images/enemy/squirrel/idle_n.png");
    private static final Image IMG_IDLE_S  = ResourceLoader.getImage("/images/enemy/squirrel/idle_s.png");
    private static final Image IMG_IDLE_E  = ResourceLoader.getImage("/images/enemy/squirrel/idle_e.png");
    private static final Image IMG_WALK1_N = ResourceLoader.getImage("/images/enemy/squirrel/walk1_n.png");
    private static final Image IMG_WALK1_S = ResourceLoader.getImage("/images/enemy/squirrel/walk1_s.png");
    private static final Image IMG_WALK1_E = ResourceLoader.getImage("/images/enemy/squirrel/walk1_e.png");

    private static final double DRAW_SIZE = 80.0;
    private static final double BOB_SPEED = 8.0;
    private static final double BOB_AMP   = 1.5;

    // ── Lunge mechanic ─────────────────────────────────────────────────────
    private static final double LUNGE_RANGE    = 110.0; // pixel distance to trigger
    private static final double LUNGE_SPEED    = 320.0; // pixels/s during lunge
    private static final double LUNGE_DURATION = 0.28;  // seconds of lunge
    private static final double LUNGE_CD_MIN   = 1.2;   // min cooldown after lunge

    private boolean lunging      = false;
    private double  lungeTimeLeft = 0;
    private double  lungeCD      = 0;
    private double  lungeDX, lungeDY; // stored direction at lunge start

    // ── Direction & animation ──────────────────────────────────────────────
    private enum Dir { N, S, E, W }
    private Dir    currentDir = Dir.S;
    private double bobTime    = 0;
    private double bobOffset  = 0;

    public Squirrel(double x, double y, int stage) {
        super(x, y, 28, 28, 15 * stage, 130, 3 * stage);
        shootCooldown = 999; // melee only — no projectiles
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime); // ticks shoot/bullet timers (no-op here)

        if (lungeCD > 0) lungeCD -= deltaTime;

        boolean moving;
        if (lunging) {
            x += lungeDX * LUNGE_SPEED * deltaTime;
            y += lungeDY * LUNGE_SPEED * deltaTime;
            clampToRoom();
            moving       = true;
            lungeTimeLeft -= deltaTime;
            if (lungeTimeLeft <= 0) {
                lunging = false;
                lungeCD = LUNGE_CD_MIN + Math.random() * 0.6;
            }
        } else {
            updateDir(player.getCenterX(), player.getCenterY());
            double prevX = x, prevY = y;
            moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
            clampToRoom();
            moving = (Math.abs(x - prevX) > 0.001 || Math.abs(y - prevY) > 0.001);

            // Trigger lunge when close enough and cooldown ready
            if (lungeCD <= 0) {
                double dx   = player.getCenterX() - getCenterX();
                double dy   = player.getCenterY() - getCenterY();
                double dist = Math.hypot(dx, dy);
                if (dist < LUNGE_RANGE && dist > 1) {
                    lungeDX      = dx / dist;
                    lungeDY      = dy / dist;
                    lunging      = true;
                    lungeTimeLeft = LUNGE_DURATION;
                }
            }
        }

        if (moving) { bobTime += deltaTime; bobOffset = Math.sin(bobTime * BOB_SPEED) * BOB_AMP; }
        else        { bobTime = 0; bobOffset = 0; }
    }

    private void updateDir(double pcx, double pcy) {
        double deg = Math.toDegrees(Math.atan2(pcy - getCenterY(), pcx - getCenterX()));
        if      (deg >= -45  && deg <  45)  currentDir = Dir.E;
        else if (deg >=  45  && deg < 135)  currentDir = Dir.S;
        else if (deg >= -135 && deg < -45)  currentDir = Dir.N;
        else                                currentDir = Dir.W;
    }

    /** Whether the squirrel is currently mid-lunge (used externally if needed). */
    public boolean isLunging() { return lunging; }

    @Override
    public void draw(GraphicsContext gc) {
        Image   frame = pickFrame();
        boolean flip  = (currentDir == Dir.W);
        double  drawX = getCenterX() - DRAW_SIZE / 2.0;
        // Squish effect during lunge: slightly taller/narrower sprite
        double  scaleY = lunging ? 1.15 : 1.0;
        double  drawY  = getCenterY() - (DRAW_SIZE * scaleY) / 2.0 + bobOffset;

        if (frame != null) {
            if (flip) {
                gc.save();
                gc.translate(getCenterX(), 0);
                gc.scale(-1, 1);
                gc.drawImage(frame, -DRAW_SIZE / 2.0, drawY, DRAW_SIZE, DRAW_SIZE * scaleY);
                gc.restore();
            } else {
                gc.drawImage(frame, drawX, drawY, DRAW_SIZE, DRAW_SIZE * scaleY);
            }
        } else {
            // Fallback colour block
            gc.setFill(lunging ? Color.rgb(160, 100, 40) : Color.rgb(100, 65, 30));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.rgb(130, 85, 45));
            gc.fillOval(x + 3,          y - 5, 8, 8);
            gc.fillOval(x + width - 11, y - 5, 8, 8);
        }

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
        // No bullets to draw
    }

    private Image pickFrame() {
        Dir dir = (currentDir == Dir.W) ? Dir.E : currentDir;
        // Use walk frame while moving or lunging
        if (lunging || bobOffset != 0) return switch (dir) {
            case N -> IMG_WALK1_N;
            case S -> IMG_WALK1_S;
            default -> IMG_WALK1_E;
        };
        return switch (dir) {
            case N -> IMG_IDLE_N;
            case S -> IMG_IDLE_S;
            default -> IMG_IDLE_E;
        };
    }
}
