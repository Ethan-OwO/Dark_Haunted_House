package com.escapencu.entity.enemy;

import com.escapencu.entity.EffectBullet;
import com.escapencu.entity.Enemy;
import com.escapencu.entity.Entity;
import com.escapencu.entity.Player;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 3 normal enemy — starts as a winged alate, drops wings after a few
 * seconds and becomes a faster worker.
 *
 * Phase 1 (alate)  : moves at normal speed, shoots at player.
 * Transition       : fallen_wings sprite flashes briefly, Wing poison zone spawned.
 * Phase 2 (worker) : 40% faster, stops shooting, more aggressive melee charge.
 */
public class Termite extends Enemy {

    // ── Sprites — alate (winged) phase ────────────────────────────────────
    private static final Image IMG_ALATE_IDLE_N  = ResourceLoader.getImage("/images/enemy/termite/alate_idle_n.png");
    private static final Image IMG_ALATE_IDLE_S  = ResourceLoader.getImage("/images/enemy/termite/alate_idle_s.png");
    private static final Image IMG_ALATE_IDLE_E  = ResourceLoader.getImage("/images/enemy/termite/alate_idle_e.png");
    private static final Image IMG_ALATE_WALK1_N = ResourceLoader.getImage("/images/enemy/termite/alate_walk1_n.png");
    private static final Image IMG_ALATE_WALK1_S = ResourceLoader.getImage("/images/enemy/termite/alate_walk1_s.png");
    private static final Image IMG_ALATE_WALK1_E = ResourceLoader.getImage("/images/enemy/termite/alate_walk1_e.png");

    // ── Sprites — worker (wingless) phase ─────────────────────────────────
    private static final Image IMG_WORKER_IDLE_N  = ResourceLoader.getImage("/images/enemy/termite/worker_idle_n.png");
    private static final Image IMG_WORKER_IDLE_S  = ResourceLoader.getImage("/images/enemy/termite/worker_idle_s.png");
    private static final Image IMG_WORKER_IDLE_E  = ResourceLoader.getImage("/images/enemy/termite/worker_idle_e.png");
    private static final Image IMG_WORKER_WALK1_N = ResourceLoader.getImage("/images/enemy/termite/worker_walk1_n.png");
    private static final Image IMG_WORKER_WALK1_S = ResourceLoader.getImage("/images/enemy/termite/worker_walk1_s.png");
    private static final Image IMG_WORKER_WALK1_E = ResourceLoader.getImage("/images/enemy/termite/worker_walk1_e.png");

    // ── Transition sprite ──────────────────────────────────────────────────
    private static final Image IMG_FALLEN_WINGS = ResourceLoader.getImage("/images/enemy/termite/fallen_wings.png");

    private static final double DRAW_SIZE          = 90.0;
    private static final double BOB_SPEED          = 8.0;
    private static final double BOB_AMP            = 1.5;
    private static final double WING_DROP_TIME      = 6.0;  // seconds until wings fall
    private static final double TRANSITION_DURATION = 0.45; // seconds to show fallen_wings

    // ── Direction & animation ──────────────────────────────────────────────
    private enum Dir { N, S, E, W }
    private Dir    currentDir = Dir.S;
    private double bobTime    = 0;
    private double bobOffset  = 0;

    // ── Phase state ────────────────────────────────────────────────────────
    private boolean hasWings        = true;
    private double  wingTimer       = WING_DROP_TIME;
    private double  transitionTimer = 0;
    private boolean wingSpawned     = false;  // poison zone when wings drop
    private boolean deathSpawned    = false;  // poison zone on death (alate only)

    // ── Kiting (alate phase: keep player in spit range, avoid melee) ───────
    private static final double KITE_MIN  = 120.0; // back away when closer than this
    private static final double KITE_MAX  = 240.0; // approach when farther than this
    // Strafe: move sideways while in sweet-spot to make it harder to dodge-back
    private double strafeDir   = (Math.random() > 0.5) ? 1.0 : -1.0;
    private double strafeTimer = 0.0;
    private static final double STRAFE_INTERVAL = 1.8; // seconds before reversing strafe

    // ── Saliva spit (alate phase only) ────────────────────────────────────
    private static final double SPIT_COOLDOWN = 2.2; // seconds between spits
    private static final double SPIT_SPEED    = 160.0;
    // Spit color: mucus yellow-green
    private static final Color  SPIT_COLOR    = Color.color(0.65, 0.88, 0.25, 0.92);

    public Termite(double x, double y, int stage) {
        super(x, y, 32, 32, 20 * stage, 75, 4 * stage);
        shootCooldown  = SPIT_COOLDOWN;
        bulletDamage   = 2 * stage; // spit damage (lower than old shoot)
        ignoreObstacles = true;     // alate phase: hover over furniture
    }

    @Override
    public void update(double deltaTime, Player player) {
        super.update(deltaTime);

        // ── Phase transition countdown ────────────────────────────────────
        if (hasWings) {
            wingTimer -= deltaTime;
            if (wingTimer <= 0) {
                hasWings        = false;
                transitionTimer = TRANSITION_DURATION;
                speed          *= 2.0;    // worker sprints at 2× alate speed
                shootCooldown   = 999;    // worker stops shooting
                ignoreObstacles = false;  // worker can no longer fly over obstacles
            }
        }
        if (transitionTimer > 0) transitionTimer -= deltaTime;

        // ── Movement & animation ──────────────────────────────────────────
        double pcx  = player.getCenterX();
        double pcy  = player.getCenterY();
        double dxP  = pcx - getCenterX();
        double dyP  = pcy - getCenterY();
        double dist = Math.hypot(dxP, dyP);

        double targetX, targetY;
        if (hasWings && dist > 0.1) {
            // ── Alate kiting: maintain spit range, strafe sideways ─────────
            strafeTimer -= deltaTime;
            if (strafeTimer <= 0) {
                strafeDir   = (Math.random() > 0.5) ? 1.0 : -1.0;
                strafeTimer = STRAFE_INTERVAL + Math.random() * 0.8;
            }
            double perpX = -dyP / dist; // unit vector perpendicular to player direction
            double perpY =  dxP / dist;

            if (dist < KITE_MIN) {
                // Too close: back directly away from player
                targetX = getCenterX() - (dxP / dist) * KITE_MAX;
                targetY = getCenterY() - (dyP / dist) * KITE_MAX;
            } else if (dist > KITE_MAX) {
                // Too far: close in
                targetX = pcx;
                targetY = pcy;
            } else {
                // Sweet spot: strafe perpendicular to keep a moving target
                targetX = getCenterX() + perpX * strafeDir * 60;
                targetY = getCenterY() + perpY * strafeDir * 60;
            }
        } else {
            // ── Worker phase: charge straight at player ────────────────────
            double[] ct = chaseTarget(pcx, pcy);
            targetX = ct[0];
            targetY = ct[1];
        }

        updateDir(targetX, targetY);
        double prevX = x, prevY = y;
        moveToward(targetX, targetY, deltaTime);
        clampToRoom();
        boolean moving = (Math.abs(x - prevX) > 0.001 || Math.abs(y - prevY) > 0.001);
        if (moving) { bobTime += deltaTime; bobOffset = Math.sin(bobTime * BOB_SPEED) * BOB_AMP; }
        else        { bobTime = 0; bobOffset = 0; }

        // ── Shooting (alate phase only) ───────────────────────────────────
        if (hasWings) spitAt(player.getCenterX(), player.getCenterY());
    }

    /**
     * Spit a saliva projectile at the target.
     * On hit: applies slow (1.8 s) to the player.
     * Only fires during the alate (winged) phase.
     */
    private void spitAt(double tx, double ty) {
        if (shootTimer > 0) return;
        double dx   = tx - getCenterX();
        double dy   = ty - getCenterY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1) return;
        int dmg = bulletDamage;
        bullets.add(new EffectBullet(
                getCenterX(), getCenterY(),
                (dx / dist) * SPIT_SPEED,
                (dy / dist) * SPIT_SPEED,
                dmg,
                SPIT_COLOR,
                "",                         // no label — sprite will replace this later
                p -> p.applySlow(1.8)
        ));
        shootTimer = shootCooldown;
    }

    private void updateDir(double pcx, double pcy) {
        double deg = Math.toDegrees(Math.atan2(pcy - getCenterY(), pcx - getCenterX()));
        if      (deg >= -45  && deg <  45)  currentDir = Dir.E;
        else if (deg >=  45  && deg < 135)  currentDir = Dir.S;
        else if (deg >= -135 && deg < -45)  currentDir = Dir.N;
        else                                currentDir = Dir.W;
    }

    // ── Spawn Wing poison zones ────────────────────────────────────────────
    @Override
    public List<Entity> getPendingSpawns() {
        List<Entity> list = new ArrayList<>();

        // Wings drop → poison zone appears at drop location
        if (!hasWings && !wingSpawned) {
            wingSpawned = true;
            list.add(new Wing(getCenterX(), getCenterY()));
        }

        // Alate dies before timer runs out → wings still drop at death
        if (!isAlive() && !deathSpawned && hasWings) {
            deathSpawned = true;
            list.add(new Wing(getCenterX(), getCenterY()));
        }

        return list.isEmpty() ? Collections.emptyList() : list;
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    @Override
    public void draw(GraphicsContext gc) {
        // Transition flash: fallen_wings overlay, fades out
        if (transitionTimer > 0 && IMG_FALLEN_WINGS != null) {
            double alpha = transitionTimer / TRANSITION_DURATION;
            gc.setGlobalAlpha(alpha * 0.9);
            gc.drawImage(IMG_FALLEN_WINGS,
                    getCenterX() - DRAW_SIZE / 2.0,
                    getCenterY() - DRAW_SIZE / 2.0,
                    DRAW_SIZE, DRAW_SIZE);
            gc.setGlobalAlpha(1.0);
        }

        // Character sprite
        Image   frame = pickFrame();
        boolean flip  = (currentDir == Dir.W);
        double  drawX = getCenterX() - DRAW_SIZE / 2.0;
        double  drawY = getCenterY() - DRAW_SIZE / 2.0 + bobOffset;

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
        } else {
            // Fallback: colour block
            gc.setFill(hasWings ? Color.rgb(110, 75, 30) : Color.rgb(160, 120, 60));
            gc.fillRect(x, y, width, height);
            if (hasWings) {
                gc.setStroke(Color.rgb(80, 50, 20));
                gc.setLineWidth(1);
                gc.strokeLine(getCenterX() - 4, y, getCenterX() - 10, y - 8);
                gc.strokeLine(getCenterX() + 4, y, getCenterX() + 10, y - 8);
            }
        }

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 8, width, 4);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 8, width * (double) hp / maxHp, 4);
        }
        drawStatusEffects(gc);
        for (var b : bullets) b.draw(gc);
    }

    private Image pickFrame() {
        Dir     dir    = (currentDir == Dir.W) ? Dir.E : currentDir;
        boolean moving = (bobOffset != 0);
        if (hasWings) {
            if (moving) return switch (dir) {
                case N -> IMG_ALATE_WALK1_N;
                case S -> IMG_ALATE_WALK1_S;
                default -> IMG_ALATE_WALK1_E;
            };
            return switch (dir) {
                case N -> IMG_ALATE_IDLE_N;
                case S -> IMG_ALATE_IDLE_S;
                default -> IMG_ALATE_IDLE_E;
            };
        } else {
            if (moving) return switch (dir) {
                case N -> IMG_WORKER_WALK1_N;
                case S -> IMG_WORKER_WALK1_S;
                default -> IMG_WORKER_WALK1_E;
            };
            return switch (dir) {
                case N -> IMG_WORKER_IDLE_N;
                case S -> IMG_WORKER_IDLE_S;
                default -> IMG_WORKER_IDLE_E;
            };
        }
    }
}
