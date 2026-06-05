package com.escapencu.entity.boss;

import com.escapencu.entity.EffectBullet;
import com.escapencu.entity.Player;
import com.escapencu.level.Room;
import com.escapencu.util.ResourceLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Stage 2 Boss — 沉沁汗 (Truth Table)
 * Attack 1: Burrows underground, appears under the player with a warning circle.
 * Attack 2: Shoots logic-gate styled bullets (AND / OR / XOR).
 */
public class ChenQinHan extends Boss {

    // ── Sprites ────────────────────────────────────────────────────────────
    private static final String BASE = "/images/boss/chenqinhan/";
    private static final Image IMG_S = ResourceLoader.getImage(BASE + "truthtable_idle_s.png", false);
    private static final Image IMG_N = ResourceLoader.getImage(BASE + "truthtable_idle_n.png", false);
    private static final Image IMG_E = ResourceLoader.getImage(BASE + "truthtable_idle_e.png", false);
    private static final Image IMG_W = ResourceLoader.getImage(BASE + "truthtable_idle_w.png", false);

    private static final Image IMG_WARN   = ResourceLoader.getImage(BASE + "burrow_warn/crack_floor.png",        false);
    private static final Image IMG_EMERGE = ResourceLoader.getImage(BASE + "burrow_emerge/explosion_floor.png",  false);

    private static final Image IMG_AND = ResourceLoader.getImage(BASE + "bullet/AND.png", false);
    private static final Image IMG_OR  = ResourceLoader.getImage(BASE + "bullet/OR.png",  false);
    private static final Image IMG_XOR = ResourceLoader.getImage(BASE + "bullet/XOR.png", false);

    // ── Facing direction ───────────────────────────────────────────────────
    private int facing = 0; // 0=S 1=N 2=E 3=W

    private final Random rng = new Random();

    // ── Burrow state ───────────────────────────────────────────────────────
    private boolean burrowed     = false;
    private double  burrowCD     = 3.0;
    private double  burrowTimer  = 0;
    private double  warningX, warningY;
    private double  emergeFlash  = 0; // brief emerge-effect display timer

    // ── Bullet attack ──────────────────────────────────────────────────────
    private double bulletTimer = 1.5;

    public ChenQinHan(double cx, double cy, int stage) {
        super(cx - 57, cy - 57, 114, 114, 500 * stage, 120, 20 * stage);
        bulletDamage  = 15 * stage;
        shootCooldown = 999; // managed manually
    }

    @Override
    protected void doAttack(Player player, double deltaTime) {
        // Tick emerge flash
        if (emergeFlash > 0) emergeFlash -= deltaTime;

        // ── Burrow ────────────────────────────────────────────────────────
        if (burrowed) {
            burrowTimer -= deltaTime;
            if (burrowTimer <= 0) {
                x = warningX - width  / 2;
                y = warningY - height / 2;
                invincible  = false;
                burrowed    = false;
                emergeFlash = 0.4; // show emerge effect for 0.4s
                burrowCD    = phase == 2 ? 2.5 : 4.0;
                if (Math.hypot(getCenterX() - player.getCenterX(),
                               getCenterY() - player.getCenterY()) < 50) {
                    player.takeDamage(20);
                    player.applyStun(0.3);
                }
            }
            return;
        }

        burrowCD -= deltaTime;
        if (burrowCD <= 0) {
            // Emerge directly under the player's current position
            warningX    = player.getCenterX();
            warningY    = player.getCenterY();
            burrowed    = true;
            invincible  = true;
            burrowTimer = 1.0;
            return;
        }

        // ── Track facing direction ─────────────────────────────────────────
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        if (Math.abs(dx) > Math.abs(dy)) facing = dx > 0 ? 2 : 3;
        else                             facing  = dy > 0 ? 0 : 1;

        moveToward(player.getCenterX(), player.getCenterY(), deltaTime);

        // ── Logic-gate bullet attacks ─────────────────────────────────────
        bulletTimer -= deltaTime;
        if (bulletTimer <= 0) {
            fireLogicAttack();
            bulletTimer = phase == 2 ? 1.2 : 2.0;
        }
    }

    /** Fires 8 bullets in all directions; each bullet's type (AND/OR/XOR) is random. */
    private void fireLogicAttack() {
        for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4); // 0°, 45°, 90°, ... 315°
            int type = rng.nextInt(3);         // 0=AND  1=OR  2=XOR

            double spd = switch (type) {
                case 0  -> phase == 2 ? 360 : 280;
                case 1  -> phase == 2 ? 260 : 200;
                default -> phase == 2 ? 280 : 220;
            };
            Color  col  = switch (type) { case 0 -> Color.LIMEGREEN; case 1 -> Color.ORANGE; default -> Color.MEDIUMPURPLE; };
            String lbl  = switch (type) { case 0 -> "AND"; case 1 -> "OR"; default -> "XOR"; };
            Image  img  = switch (type) { case 0 -> IMG_AND; case 1 -> IMG_OR; default -> IMG_XOR; };
            int    dmg  = switch (type) { case 0 -> 15; case 1 -> 10; default -> 8; };
            int    sz   = switch (type) { case 0 -> 36; case 1 -> 30; default -> 33; };

            EffectBullet b = new EffectBullet(getCenterX(), getCenterY(),
                    Math.cos(angle) * spd, Math.sin(angle) * spd,
                    dmg, col, lbl, null, sz);
            if (img != null) { b.setImage(img); b.setRotateToVelocity(true); }
            bullets.add(b);
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // ── Underground: show warn circle only ────────────────────────────
        if (burrowed) {
            if (IMG_WARN != null) {
                double sz = 144;
                gc.drawImage(IMG_WARN, warningX - sz / 2, warningY - sz / 2, sz, sz);
            } else {
                gc.setFill(Color.color(1.0, 0.9, 0.0, 0.35));
                gc.fillOval(warningX - 40, warningY - 40, 80, 80);
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                gc.strokeOval(warningX - 40, warningY - 40, 80, 80);
            }
            for (var b : bullets) b.draw(gc);
            return;
        }

        // ── Emerge flash effect ────────────────────────────────────────────
        if (emergeFlash > 0 && IMG_EMERGE != null) {
            double sz = 165;
            gc.drawImage(IMG_EMERGE, getCenterX() - sz / 2, getCenterY() - sz / 2, sz, sz);
        }

        // ── Boss body ──────────────────────────────────────────────────────
        Image img = switch (facing) {
            case 1 -> IMG_N;
            case 2 -> IMG_E;
            case 3 -> IMG_W;
            default -> IMG_S;
        };

        if (img != null) {
            if (phase == 2) {
                // ColorAdjust only tints pixels that exist in the sprite (transparency safe)
                ColorAdjust p2 = new ColorAdjust();
                p2.setHue(-0.15);
                p2.setBrightness(-0.25);
                p2.setSaturation(0.4);
                gc.setEffect(p2);
            }
            gc.drawImage(img, x, y, width, height);
            gc.setEffect(null);
        } else {
            gc.setFill(phase == 2 ? Color.rgb(30, 100, 160) : Color.rgb(40, 130, 190));
            gc.fillRect(x, y, width, height);
            gc.setFill(Color.WHITE);
            gc.fillText("沉沁汗", x + 6, y + height / 2 + 5);
        }

        // HP bar
        if (hp < maxHp) {
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y - 10, width, 5);
            gc.setFill(Color.LIMEGREEN);
            gc.fillRect(x, y - 10, width * (double) hp / maxHp, 5);
        }

        for (var b : bullets) b.draw(gc);
    }
}
