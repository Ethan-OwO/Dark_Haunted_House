package com.escapencu.entity.boss;

import com.escapencu.entity.Bullet;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.level.Room;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Stage 1 Boss — 無小光
 *
 * Passive: periodically turns fully invisible and splits into 5 Decoys.
 *          Only reappears once all 5 Decoys are destroyed (15-second safety timeout).
 *          While invisible: does NOT move, shoot, or place mines.
 * Active:  fires 3-way spread bullets; places Hanoi-Tower floor mines.
 */
public class WuXiaoGuang extends Boss {

    // ── Sprites ────────────────────────────────────────────────────────────
    private static final String BASE = "/images/boss/wxg/wxg/";
    static final Image IMG_S = ResourceLoader.getImage(BASE + "wuxiaoguang_idle_s.png", false);
    static final Image IMG_N = ResourceLoader.getImage(BASE + "wuxiaoguang_idle_n.png", false);
    static final Image IMG_E = ResourceLoader.getImage(BASE + "wuxiaoguang_idle_e.png", false);
    static final Image IMG_W = ResourceLoader.getImage(BASE + "wuxiaoguang_idle_w.png", false);
    static final Image IMG_BULLET =
            ResourceLoader.getImage("/images/boss/wxg/bullet/wxg_bullet.png", false);

    // ── Facing direction ───────────────────────────────────────────────────
    private enum Dir { S, N, E, W }
    private Dir facing = Dir.S;

    private final int    stage;
    private final Random rng = new Random();

    // ── Invisibility / decoy phase ─────────────────────────────────────────
    private double  invisCD    = 5.0;   // time until next invis trigger
    private double  invisTimer = 0;     // safety timeout (15 s)
    private boolean invisible  = false;
    private final List<Decoy> activeDecoys = new ArrayList<>();

    // ── Mine placement ─────────────────────────────────────────────────────
    private double mineTimer = 6.0;
    private int    mineCount = 0;
    private static final int MAX_MINES = 5;

    private final List<Entity> pendingRoom = new ArrayList<>();

    public WuXiaoGuang(double cx, double cy, int stage) {
        super(cx - 40, cy - 40, 80, 80, 250 * stage, 55, 15 * stage);
        this.stage    = stage;
        bulletDamage  = 8 * stage;
        shootCooldown = 2.2;
    }

    // ── Fires 3-way spread, each bullet rotated to face its travel direction ──
    @Override
    public void shootAt(double tx, double ty, double bulletSpeed) {
        if (shootTimer > 0) return;
        double dx   = tx - getCenterX();
        double dy   = ty - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;

        double base   = Math.atan2(dy, dx);
        double spread = Math.PI / 6; // 30°
        double[] angles = { base, base - spread, base + spread };
        double spd = bulletSpeed * (phase == 2 ? 1.35 : 1.0); // Phase 2 faster

        for (double angle : angles) {
            Bullet b = new Bullet(
                    getCenterX(), getCenterY(),
                    Math.cos(angle) * spd,
                    Math.sin(angle) * spd,
                    bulletDamage, false, 56);
            if (IMG_BULLET != null) {
                b.setImage(IMG_BULLET);
                b.setRotateToVelocity(true);
            }
            bullets.add(b);
        }
        shootTimer = shootCooldown;
    }

    /** Fires {@code count} bullets evenly spread in all directions (reappear burst). */
    private void fireBurst(int count) {
        double spd = (220 + stage * 20) * (phase == 2 ? 1.35 : 1.0);
        for (int i = 0; i < count; i++) {
            double angle = i * (2 * Math.PI / count);
            Bullet b = new Bullet(getCenterX(), getCenterY(),
                    Math.cos(angle) * spd, Math.sin(angle) * spd,
                    bulletDamage, false, 56);
            if (IMG_BULLET != null) { b.setImage(IMG_BULLET); b.setRotateToVelocity(true); }
            bullets.add(b);
        }
    }

    private void updateFacing(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) facing = dx > 0 ? Dir.E : Dir.W;
        else                             facing = dy > 0 ? Dir.S : Dir.N;
    }

    @Override
    protected void doAttack(Player player, double deltaTime) {

        // ── Invisibility / decoy phase ─────────────────────────────────────
        if (invisible) {
            invisTimer -= deltaTime;
            // Reveal when all decoys are dead OR safety timeout expires
            boolean allDead = !activeDecoys.isEmpty()
                    && activeDecoys.stream().noneMatch(Entity::isAlive);
            if (allDead || invisTimer <= 0) {
                invisible  = false;
                invincible = false;
                activeDecoys.clear();
                if (phase == 2) fireBurst(12); // Phase 2: radial burst on reappear
                else            fireBurst(8);
            }
            return; // no movement, no shooting, no mines while invisible
        }

        // ── Trigger invisibility ───────────────────────────────────────────
        invisCD -= deltaTime;
        if (invisCD <= 0) {
            invisible  = true;
            invincible = true;
            invisTimer = 15.0; // safety timeout
            invisCD    = 5.0 + rng.nextDouble() * 2;

            // Spawn 5 decoys spread around WuXiaoGuang's current centre
            double cx     = getCenterX();
            double cy     = getCenterY();
            double spread = 70;
            for (int i = 0; i < 5; i++) {
                double angle  = i * (2 * Math.PI / 5);
                double spawnX = cx + Math.cos(angle) * spread - 40;
                double spawnY = cy + Math.sin(angle) * spread - 40;
                // Clamp inside room inner bounds
                spawnX = Math.max(roomX + Room.WALL + 10,
                         Math.min(roomX + roomW - Room.WALL - 90, spawnX));
                spawnY = Math.max(roomY + Room.WALL + 10,
                         Math.min(roomY + roomH - Room.WALL - 90, spawnY));
                Decoy d = new Decoy(spawnX, spawnY, stage);
                pendingRoom.add(d);
                activeDecoys.add(d);
            }
            return; // skip movement/attack this tick
        }

        // ── Normal movement + shooting (visible phase) ─────────────────────
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        updateFacing(dx, dy);
        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);
        shootAt(player.getCenterX(), player.getCenterY(), 200 + stage * 20);

        // ── Mine placement ─────────────────────────────────────────────────
        mineTimer -= deltaTime;
        if (mineTimer <= 0 && mineCount < MAX_MINES) {
            double mx = roomX + Room.WALL + 50 + rng.nextDouble() * (roomW - Room.WALL * 2 - 100);
            double my = roomY + Room.WALL + 50 + rng.nextDouble() * (roomH - Room.WALL * 2 - 100);
            pendingRoom.add(new Mine(mx, my));
            mineCount++;
            mineTimer = 5.0 + rng.nextDouble() * 2;
        }
    }

    @Override
    public List<Entity> getPendingSpawns() {
        if (pendingRoom.isEmpty()) return Collections.emptyList();
        List<Entity> copy = new ArrayList<>(pendingRoom);
        pendingRoom.clear();
        return copy;
    }

    @Override
    protected void updatePhase() {
        if (phase == 1 && hp <= maxHp / 2) {
            phase         = 2;
            invisCD       = Math.min(invisCD, 2.0);
            mineTimer     = Math.min(mineTimer, 3.0);
            shootCooldown = 1.5; // Phase 2: shoot more frequently
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Fully invisible — skip drawing body entirely
        if (invisible) {
            for (var b : bullets) b.draw(gc); // existing bullets still fly
            return;
        }

        Image img = switch (facing) {
            case N -> IMG_N;
            case E -> IMG_E;
            case W -> IMG_W;
            default -> IMG_S;
        };

        if (img != null) {
            if (phase == 2) {
                ColorAdjust p2 = new ColorAdjust();
                p2.setHue(0.1);
                p2.setBrightness(-0.2);
                p2.setSaturation(0.5);
                gc.setEffect(p2);
            }
            gc.drawImage(img, x, y, width, height);
            gc.setEffect(null);
        } else {
            gc.setFill(phase == 2 ? Color.rgb(140, 20, 160) : Color.rgb(100, 30, 120));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.WHITE);
            gc.fillText("無小光", x + 6, y + height / 2 + 5);
        }

        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 10, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 10, width * (double) hp / maxHp, 5);
        }

        for (var b : bullets) b.draw(gc);
    }
}
