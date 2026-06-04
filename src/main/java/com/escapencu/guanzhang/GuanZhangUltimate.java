package com.escapencu.guanzhang;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

/**
 * 館長山羌 被動觸發過場：IRON GUARDIAN · LAST STAND
 *
 * 4.0 秒 timeline:
 *   0.0 – 0.5 s  ─ 暗紅閃光爆開（死亡瞬間）
 *   0.5 – 1.5 s  ─ 館長被槍照片飛入 + 震動
 *   1.5 – 2.5 s  ─ 照片定格；金色盾牌光環收縮包圍
 *   2.5 – 2.8 s  ─ 白光爆閃
 *   2.8 – 3.5 s  ─ "IRON GUARDIAN" 大字 + 金色粒子
 *   3.5 – 4.0 s  ─ 全部淡出
 */
public class GuanZhangUltimate extends Pane {

    private final double w, h;
    private final Canvas canvas;
    private final GraphicsContext gc;

    private static final Image GZ_IMG = loadImage("/images/player/gzgotshot.png");

    public static final double DURATION = 4.0;

    private AnimationTimer timer;
    private long startNanos = -1;
    private Runnable onFinish;
    private final Random rng = new Random(77);

    public GuanZhangUltimate(double width, double height) {
        this.w = width;
        this.h = height;
        setPrefSize(width, height);
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        getChildren().add(canvas);
        setMouseTransparent(true);
    }

    public void play(Runnable onDone) {
        this.onFinish = onDone;
        if (timer != null) timer.stop();
        startNanos = -1;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (startNanos < 0) startNanos = now;
                double t = (now - startNanos) / 1e9;
                if (t >= DURATION) {
                    this.stop();
                    if (onFinish != null) onFinish.run();
                    return;
                }
                draw(t);
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; }
    }

    // ── helpers ───────────────────────────────────────────────────────────
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double window(double t, double a, double b) {
        if (t <= a) return 0;
        if (t >= b) return 1;
        return (t - a) / (b - a);
    }
    private static double easeOut(double t) { return 1 - Math.pow(1 - t, 3); }

    // ── main draw ─────────────────────────────────────────────────────────
    private void draw(double t) {
        gc.clearRect(0, 0, w, h);

        // 1) 黑底
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        // 2) 暗紅血光（死亡爆開：0.0–1.0）
        double bloodT   = window(t, 0.0, 0.3);
        double bloodFade = 1 - window(t, 0.8, 1.5);
        double bloodAlpha = 0.85 * easeOut(bloodT) * bloodFade;
        if (bloodAlpha > 0.01) {
            gc.setFill(new RadialGradient(0, 0, w / 2, h / 2, w * 0.75, false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#CC0000", bloodAlpha)),
                    new Stop(0.5, Color.web("#660000", bloodAlpha * 0.5)),
                    new Stop(1, Color.color(0, 0, 0, 0))));
            gc.fillRect(0, 0, w, h);
        }

        // 3) 金色背景光（1.5 之後）
        double goldBg = window(t, 1.5, 2.5) * (1 - window(t, 3.5, DURATION));
        if (goldBg > 0.01) {
            gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.55, w * 0.7, false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#B8860B", goldBg * 0.4)),
                    new Stop(1, Color.color(0, 0, 0, 0))));
            gc.fillRect(0, 0, w, h);
        }

        // 4) 震動（0.5–1.0）
        double shakeT = window(t, 0.5, 1.0);
        double shakeMag = (1 - shakeT) * 18;
        double sx = (rng.nextDouble() - 0.5) * shakeMag;
        double sy = (rng.nextDouble() - 0.5) * shakeMag;
        gc.save();
        gc.translate(sx, sy);

        // 5) 照片飛入（0.5–1.5）
        if (t >= 0.45 && GZ_IMG != null) {
            double flyT   = window(t, 0.5, 1.5);
            double fadeFin = 1 - window(t, 3.5, DURATION);

            double imgW   = lerp(80, 420, easeOut(flyT));
            double imgH   = imgW * (GZ_IMG.getHeight() / GZ_IMG.getWidth());
            double startX = w + 300;
            double targetX = w / 2.0;
            double startY = -300;
            double targetY = h * 0.48;
            double cx = lerp(startX, targetX, easeOut(flyT));
            double cy = lerp(startY, targetY, easeOut(flyT));

            // 動態模糊（飛入中）
            int blur = (flyT < 1.0) ? 4 : 1;
            for (int i = blur - 1; i >= 0; i--) {
                double alpha = (i == 0) ? fadeFin : 0.15 * fadeFin;
                gc.setGlobalAlpha(alpha);
                gc.drawImage(GZ_IMG,
                        cx - imgW / 2 + i * 14,
                        cy - imgH / 2 + i * 10,
                        imgW, imgH);
            }
            gc.setGlobalAlpha(1.0);
        }

        // 6) 金色盾牌光環（1.5–3.0）
        double shieldT  = window(t, 1.5, 2.5);
        double shieldFade = 1 - window(t, 3.0, 3.6);
        if (shieldT > 0) {
            drawShields(t, shieldT * shieldFade);
        }

        // 7) 白色爆閃（2.5–2.8）
        double flashT = window(t, 2.5, 2.8);
        if (flashT > 0 && flashT < 1) {
            gc.setFill(Color.color(1, 1, 1, (1 - flashT) * 0.9));
            gc.fillRect(-sx, -sy, w, h); // 不受震動偏移影響
        }

        // 8) 衝擊環（2.5–3.0）
        double ringT = window(t, 2.5, 3.0);
        if (ringT > 0 && ringT < 1) {
            double radius = ringT * Math.max(w, h) * 0.6;
            gc.setStroke(Color.web("#FFD700", 1 - ringT));
            gc.setLineWidth(12 * (1 - ringT) + 2);
            gc.strokeOval(w / 2 - radius, h * 0.65 - radius * 0.5, radius * 2, radius);
            gc.setStroke(Color.web("#CC0000", (1 - ringT) * 0.7));
            gc.setLineWidth(5);
            gc.strokeOval(w / 2 - radius * 0.65, h * 0.65 - radius * 0.33, radius * 1.3, radius * 0.65);
        }

        gc.restore();

        // 9) 金色粒子（2.5–4.0）
        double particleFade = window(t, 2.5, 2.9) * (1 - window(t, 3.5, DURATION));
        if (particleFade > 0) drawGoldParticles(t, particleFade);

        // 10) "IRON GUARDIAN" 大字（2.8–4.0）
        double textT = window(t, 2.8, 3.2);
        if (textT > 0) {
            double textFade = 1 - window(t, 3.5, DURATION);
            drawGuardianText(w / 2, h * 0.18 + (1 - textT) * 45, 0.55 + textT * 0.5, textFade);
        }
    }

    private void drawShields(double t, double alpha) {
        if (alpha <= 0) return;
        int count = 6;
        double cx = w / 2.0, cy = h * 0.48;
        // 盾牌從遠端收縮進中心
        double shrinkT  = window(t, 1.5, 2.3);
        double orbitR   = lerp(w * 0.55, 120, easeOut(shrinkT));
        double angleOff = t * 1.8; // 旋轉

        for (int i = 0; i < count; i++) {
            double angle = angleOff + i * (Math.PI * 2 / count);
            double sx2 = cx + Math.cos(angle) * orbitR;
            double sy2 = cy + Math.sin(angle) * orbitR * 0.55;
            drawShieldIcon(sx2, sy2, alpha);
        }

        // 內層更快旋轉的小護盾
        double innerR = orbitR * 0.55;
        double angleOff2 = -t * 2.6;
        for (int i = 0; i < 3; i++) {
            double angle = angleOff2 + i * (Math.PI * 2 / 3);
            double sx2 = cx + Math.cos(angle) * innerR;
            double sy2 = cy + Math.sin(angle) * innerR * 0.55;
            drawShieldIcon(sx2, sy2, alpha * 0.75);
        }
    }

    private void drawShieldIcon(double cx, double cy, double alpha) {
        // 金色六邊形護盾（像素風）
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.web("#FFD700"));
        // 盾牌形：上方尖頂四邊形
        double[] px = {cx, cx - 9, cx - 9, cx, cx + 9, cx + 9};
        double[] py = {cy - 13, cy - 6, cy + 4, cy + 13, cy + 4, cy - 6};
        gc.fillPolygon(px, py, 6);
        gc.setFill(Color.web("#B8860B"));
        // 盾牌內紋
        gc.fillRect(cx - 3, cy - 4, 6, 2);
        gc.fillRect(cx - 2, cy - 1, 4, 5);
        gc.setGlobalAlpha(1.0);
    }

    private void drawGoldParticles(double t, double alpha) {
        int count = 28;
        for (int i = 0; i < count; i++) {
            double phase = (t * 0.55 + i * 0.19) % 1.0;
            double angle = i * (Math.PI * 2 / count) + t * 0.9;
            double dist  = 60 + phase * 220;
            double px    = w / 2 + Math.cos(angle) * dist;
            double py    = h * 0.48 + Math.sin(angle) * dist * 0.5 - phase * 60;
            double sz    = (1 - phase) * 7 + 2;
            double a     = (1 - phase) * alpha;
            gc.setFill(Color.color(1.0, 0.84 + (i % 3) * 0.05, 0.1, a));
            gc.fillRect(px - sz / 2, py - sz / 2, sz, sz);
        }
    }

    private void drawGuardianText(double cx, double cy, double scale, double alpha) {
        gc.save();
        gc.translate(cx, cy);
        gc.scale(scale, scale);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        gc.setFont(javafx.scene.text.Font.font("Courier New",
                javafx.scene.text.FontWeight.BLACK, 110));
        // 紅色陰影
        gc.setFill(Color.web("#8B0000", alpha));
        gc.fillText("IRON GUARDIAN", 5, 5);
        // 黑框
        gc.setStroke(Color.color(0, 0, 0, alpha));
        gc.setLineWidth(6);
        gc.strokeText("IRON GUARDIAN", 0, 0);
        // 金色主體
        gc.setFill(Color.web("#FFD700", alpha));
        gc.fillText("IRON GUARDIAN", 0, 0);

        gc.setFont(javafx.scene.text.Font.font("Courier New",
                javafx.scene.text.FontWeight.BOLD, 22));
        gc.setFill(Color.color(1, 1, 1, alpha * 0.8));
        gc.fillText("— PASSIVE · LAST STAND —", 0, 75);

        gc.restore();
    }

    private static Image loadImage(String path) {
        var stream = GuanZhangUltimate.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("[GuanZhangUltimate] Image not found: " + path);
            return null;
        }
        return new Image(stream);
    }
}
