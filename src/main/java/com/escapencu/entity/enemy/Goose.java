package com.escapencu.entity.enemy;

import com.escapencu.entity.Enemy;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Stage 2 normal enemy — slow tank that periodically charges at the player.
 *
 * Normal state  : walks slowly toward player, charges every 2.5-4 s.
 * Berserk state : triggered when HP ≤ 50%.
 *                 Charge speed ×1.5, cooldown halved, re-locks onto player
 *                 immediately after each charge ends.
 */
public class Goose extends Enemy {

    // ── Sprites — walk ─────────────────────────────────────────────────────
    private static final Image IMG_IDLE_N  = ResourceLoader.getImage("/images/enemy/goose/idle_n.png");
    private static final Image IMG_IDLE_S  = ResourceLoader.getImage("/images/enemy/goose/idle_s.png");
    private static final Image IMG_IDLE_E  = ResourceLoader.getImage("/images/enemy/goose/idle_e.png");
    private static final Image IMG_WALK1_N = ResourceLoader.getImage("/images/enemy/goose/walk1_n.png");
    private static final Image IMG_WALK1_S = ResourceLoader.getImage("/images/enemy/goose/walk1_s.png");
    private static final Image IMG_WALK1_E = ResourceLoader.getImage("/images/enemy/goose/walk1_e.png");

    // ── Sprites — charge (2-frame animation) ──────────────────────────────
    private static final Image IMG_DASH1_N = ResourceLoader.getImage("/images/enemy/goose/dash1_n.png");
    private static final Image IMG_DASH1_S = ResourceLoader.getImage("/images/enemy/goose/dash1_s.png");
    private static final Image IMG_DASH1_E = ResourceLoader.getImage("/images/enemy/goose/dash1_e.png");
    private static final Image IMG_DASH2_N = ResourceLoader.getImage("/images/enemy/goose/dash2_n.png");
    private static final Image IMG_DASH2_S = ResourceLoader.getImage("/images/enemy/goose/dash2_s.png");
    private static final Image IMG_DASH2_E = ResourceLoader.getImage("/images/enemy/goose/dash2_e.png");

    private static final double DRAW_SIZE      = 112.0;
    private static final double BOB_SPEED      = 5.0;
    private static final double BOB_AMP        = 2.0;
    private static final double DASH_FRAME_DUR = 0.10; // seconds per dash frame

    // ── Direction & animation ──────────────────────────────────────────────
    private enum Dir { N, S, E, W }
    private Dir    currentDir   = Dir.S;
    private double bobTime      = 0;
    private double bobOffset    = 0;
    private double dashFrameTimer = 0;
    private int    dashFrame    = 0; // 0 = dash1, 1 = dash2

    // ── Charge mechanic ────────────────────────────────────────────────────
    private static final double BASE_CHARGE_SPEED = 220.0;
    private static final double BASE_CHARGE_TIME  = 1.5;
    private static final double BASE_CD_MIN       = 3.0;
    private static final double BERSERK_SPEED_MUL = 1.5;
    private static final double BERSERK_CD_MUL    = 0.5;

    private boolean charging    = false;
    private double  chargeTimer = 0;
    private double  chargeCD;
    private double  chargeDX, chargeDY;

    // ── Berserk state ──────────────────────────────────────────────────────
    private boolean berserk         = false;
    private double  berserKFlashTimer = 0; // red tint flash on berserk trigger

    public Goose(double x, double y, int stage) {
        super(x, y, 50, 50, 70 * stage, 35, 12 * stage);
        shootCooldown = 999; // melee only
        chargeCD      = 2.5 + Math.random() * 1.5;
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);

        // ── Berserk trigger ───────────────────────────────────────────────
        if (!berserk && hp <= maxHp / 2) {
            berserk          = true;
            berserKFlashTimer = 0.6;
        }
        if (berserKFlashTimer > 0) berserKFlashTimer -= deltaTime;

        double chargeSpeed = BASE_CHARGE_SPEED * (berserk ? BERSERK_SPEED_MUL : 1.0);
        double cdBase      = BASE_CD_MIN       * (berserk ? BERSERK_CD_MUL    : 1.0);

        // ── Charge / walk logic ───────────────────────────────────────────
        boolean moving;
        if (charging) {
            x += chargeDX * chargeSpeed * deltaTime;
            y += chargeDY * chargeSpeed * deltaTime;
            moving = true;

            currentDir = degToDir(Math.toDegrees(Math.atan2(chargeDY, chargeDX)));

            // Dash frame animation
            dashFrameTimer -= deltaTime;
            if (dashFrameTimer <= 0) {
                dashFrame      = 1 - dashFrame; // toggle 0↔1
                dashFrameTimer = DASH_FRAME_DUR;
            }

            chargeTimer -= deltaTime;
            if (chargeTimer <= 0 || clampToRoom()) {
                charging = false;
                // Berserk: re-lock immediately with a very short cooldown
                chargeCD = berserk
                    ? 0.4 + Math.random() * 0.3
                    : cdBase + Math.random();
            }
        } else {
            chargeCD -= deltaTime;
            updateDir(player.getCenterX(), player.getCenterY());
            double prevX = x, prevY = y;
            moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
            moving = (Math.abs(x - prevX) > 0.001 || Math.abs(y - prevY) > 0.001);

            if (chargeCD <= 0) {
                double dx   = player.getCenterX() - getCenterX();
                double dy   = player.getCenterY() - getCenterY();
                double dist = Math.hypot(dx, dy);
                if (dist > 1) {
                    chargeDX      = dx / dist;
                    chargeDY      = dy / dist;
                    charging      = true;
                    chargeTimer   = BASE_CHARGE_TIME * (berserk ? 0.8 : 1.0);
                    dashFrame     = 0;
                    dashFrameTimer = DASH_FRAME_DUR;
                }
            }
        }

        if (moving) { bobTime += deltaTime; bobOffset = Math.sin(bobTime * BOB_SPEED) * BOB_AMP; }
        else        { bobTime = 0; bobOffset = 0; }
    }

    private void updateDir(double pcx, double pcy) {
        currentDir = degToDir(Math.toDegrees(Math.atan2(pcy - getCenterY(), pcx - getCenterX())));
    }

    private Dir degToDir(double deg) {
        if      (deg >= -45  && deg <  45)  return Dir.E;
        else if (deg >=  45  && deg < 135)  return Dir.S;
        else if (deg >= -135 && deg < -45)  return Dir.N;
        else                                return Dir.W;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Berserk flash: brief red tint on trigger
        if (berserKFlashTimer > 0) {
            double alpha = (berserKFlashTimer / 0.6) * 0.5;
            gc.setFill(Color.color(1, 0, 0, alpha));
            gc.fillRect(x - 4, y - 4, width + 8, height + 8);
        }

        Image   frame = pickFrame();
        boolean flip  = (currentDir == Dir.W);
        double  drawX = getCenterX() - DRAW_SIZE / 2.0;
        double  drawY = getCenterY() - DRAW_SIZE / 2.0 + bobOffset;

        // Berserk: slight red tint on sprite
        if (berserk && frame != null) gc.setGlobalAlpha(0.85);

        if (frame != null) {
            if (flip) {
                gc.save();
                gc.translate(getCenterX(), 0);
                gc.scale(-1, 1);
                gc.drawImage(frame, -DRAW_SIZE / 2.0, drawY, DRAW_SIZE, DRAW_SIZE);
                gc.restore();
            } else {
                gc.drawImage(frame, drawX, drawY, DRAW_SIZE, DRAW_SIZE);
            }
            gc.setGlobalAlpha(1.0);
        } else {
            gc.setFill(charging ? Color.ORANGERED : (berserk ? Color.RED : Color.rgb(230, 230, 220)));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.ORANGE);
            gc.fillRect(x + width - 4, y + height / 2 - 4, 8, 8);
        }

        // Berserk indicator above head
        if (berserk) {
            gc.setFill(Color.RED);
            gc.fillText("!", getCenterX() - 3, y - 12);
        }

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(berserk ? Color.ORANGERED : Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
    }

    public boolean isCharging() { return charging; }
    public boolean isBerserk()  { return berserk;  }

    private Image pickFrame() {
        Dir dir = (currentDir == Dir.W) ? Dir.E : currentDir;

        if (charging) {
            // Alternate between dash1 and dash2
            if (dashFrame == 0) return switch (dir) {
                case N -> IMG_DASH1_N;
                case S -> IMG_DASH1_S;
                default -> IMG_DASH1_E;
            };
            return switch (dir) {
                case N -> IMG_DASH2_N;
                case S -> IMG_DASH2_S;
                default -> IMG_DASH2_E;
            };
        }
        if (bobOffset != 0) return switch (dir) {
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
