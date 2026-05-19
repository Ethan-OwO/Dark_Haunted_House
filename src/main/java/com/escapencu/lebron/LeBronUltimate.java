package com.escapencu.lebron;

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
 * 大招動畫：KING JAMES · SLAM OF THE CHOSEN
 *
 * 4.2 秒 timeline:
 *   0.0 – 0.6 s   ─ 黑屏；中央紅光緩緩淡入
 *   0.6 – 1.4 s   ─ LBJ 從右上飛入（拖尾、模糊）
 *   1.4 – 2.2 s   ─ 到達中央，保持扣籃姿勢
 *   2.2 – 2.4 s   ─ SLAM！白屏閃光 + 衝擊環擴散 + 螢幕震動
 *   2.4 – 3.6 s   ─ 地面起火 + "KING JAMES" 大字浮現
 *   3.6 – 4.2 s   ─ 全部淡出
 *
 * 用法 (Overlay 在現有畫面上):
 *   LeBronUltimate ult = new LeBronUltimate(960, 640);
 *   root.getChildren().add(ult);                        // 加到場景頂層
 *   ult.play(() -> root.getChildren().remove(ult));     // 結束自動移除
 */
public class LeBronUltimate extends Pane {

    private final double w, h;
    private final Canvas canvas;
    private final GraphicsContext gc;

    private static final Image DUNK_IMG = loadImage("/images/player/lbj_dunk.png");

    /** 動畫總長 (秒) — 改這個來加快/拖長整段大招. */
    public static final double DURATION = 4.2;

    private AnimationTimer timer;
    private long startNanos = -1;
    private Runnable onFinish;
    private final Random rng = new Random(13);

    public LeBronUltimate(double width, double height) {
        this.w = width;
        this.h = height;
        setPrefSize(width, height);
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        getChildren().add(canvas);
        setMouseTransparent(true); // 動畫期間不擋輸入
    }

    /** 開始播放。onDone 在 4.2 秒結束時呼叫，用來把這個 overlay 從場景移掉. */
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

    // ─────────────────────────── timeline helpers ──────────────────────────
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
    /** 把 t 從 [a,b] 映射到 [0,1]，超出範圍就 clamp. */
    private static double window(double t, double a, double b) {
        if (t <= a) return 0;
        if (t >= b) return 1;
        return (t - a) / (b - a);
    }
    private static double easeOut(double t) { return 1 - Math.pow(1 - t, 3); }
    private static double easeIn (double t) { return t * t * t; }

    // ─────────────────────────── main draw ─────────────────────────────────
    private void draw(double t) {
        gc.clearRect(0, 0, w, h);

        // 1) 黑底
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        // 2) 紅光暈 (0.3–4.2)，先升後降
        double glowProgress = window(t, 0.3, 1.4);
        double glowFade     = 1 - window(t, 3.6, DURATION);
        double glowAlpha    = 0.65 * easeOut(glowProgress) * glowFade;
        if (glowAlpha > 0.01) {
            gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.6, w * 0.7, false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#DB2A1F", glowAlpha)),
                    new Stop(0.5, Color.web("#7A1500", glowAlpha * 0.5)),
                    new Stop(1, Color.color(0, 0, 0, 0))));
            gc.fillRect(0, 0, w, h);
        }

        // 3) 螢幕震動 — slam 瞬間 (2.2–2.5)
        double shakeT = window(t, 2.2, 2.5);
        double shakeMag = (1 - shakeT) * 14;
        double sx = (rng.nextDouble() - 0.5) * shakeMag;
        double sy = (rng.nextDouble() - 0.5) * shakeMag;
        gc.save();
        gc.translate(sx, sy);

        // 4) 拖尾 (0.6–1.4)
        double trailT = window(t, 0.6, 1.4);
        if (trailT > 0 && trailT < 1) {
            for (int i = 0; i < 12; i++) {
                double age = i / 12.0;
                double tx = lerp(w * 0.9 - i * 18, w / 2 - i * 8, trailT);
                double ty = lerp(h * 0.05 + i * 14, h * 0.45 + i * 10, trailT);
                double len = 70 + i * 10;
                Color c = (i < 4) ? Color.web("#FDB927") : (i < 8 ? Color.web("#DB2A1F") : Color.web("#7A1500"));
                gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(),
                        (1 - age) * (1 - trailT) * 0.8));
                gc.fillRect(tx, ty, len, 6);
            }
        }

        // 5) LeBron Sprite (0.6 之後一直顯示)
        if (t >= 0.55 && DUNK_IMG != null) {
            double flyT = window(t, 0.6, 1.4);   // 飛入
            double slamT = window(t, 2.0, 2.25); // 砸下
            double size  = lerp(120, 360, easeOut(flyT));
            // 砸下時稍微壓扁/變大
            size *= (1 + 0.15 * slamT);

            double targetX = w / 2;
            double targetY = h * 0.55;
            double startX = w + 200;
            double startY = -200;
            double cx = lerp(startX, targetX, easeOut(flyT));
            double cy = lerp(startY, targetY, easeOut(flyT));
            // slam 之後維持
            if (slamT > 0) cy = targetY + slamT * 30;

            // 飛行模糊（畫多份漸透明）
            int blurFrames = (flyT < 1) ? 5 : 1;
            for (int i = blurFrames - 1; i >= 0; i--) {
                double alpha = (i == 0) ? 1.0 : 0.18;
                double offX = (i == 0) ? 0 : (cx - lerp(startX, targetX, easeOut(Math.max(0, flyT - i * 0.03))));
                double offY = (i == 0) ? 0 : (cy - lerp(startY, targetY, easeOut(Math.max(0, flyT - i * 0.03))));
                gc.setGlobalAlpha(alpha);
                gc.drawImage(DUNK_IMG,
                        cx - size / 2 + offX,
                        cy - size / 2 + offY,
                        size, size);
            }
            gc.setGlobalAlpha(1.0);
        }

        // 6) SLAM 白閃 (2.2–2.4)
        double flashT = window(t, 2.2, 2.4);
        if (flashT > 0 && flashT < 1) {
            gc.setFill(Color.color(1, 1, 1, 1 - flashT));
            gc.fillRect(0, 0, w, h);
        }

        // 7) 衝擊環 (2.2–2.8)
        double ringT = window(t, 2.2, 2.8);
        if (ringT > 0 && ringT < 1) {
            double radius = ringT * Math.max(w, h);
            gc.setStroke(Color.web("#FDB927", 1 - ringT));
            gc.setLineWidth(14 * (1 - ringT) + 2);
            gc.strokeOval(w / 2 - radius, h * 0.7 - radius / 2, radius * 2, radius);
            gc.setStroke(Color.web("#DB2A1F", (1 - ringT) * 0.8));
            gc.setLineWidth(6);
            gc.strokeOval(w / 2 - radius * 0.7, h * 0.7 - radius * 0.35, radius * 1.4, radius * 0.7);
        }

        // 8) 地面焦痕 (2.3 之後)
        double scorchT = window(t, 2.3, 2.6);
        if (scorchT > 0) {
            double fade = 1 - window(t, 3.6, DURATION);
            gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.85, w * 0.45 * scorchT, false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0, 0, 0, 0.92 * fade)),
                    new Stop(0.5, Color.color(0.25, 0.06, 0.02, 0.7 * fade)),
                    new Stop(1, Color.color(0, 0, 0, 0))));
            gc.fillRect(0, h * 0.7, w, h * 0.3);
        }

        // 9) 地面火焰 (2.4–4.2)
        double fireT = window(t, 2.4, 2.8);
        double fireFade = 1 - window(t, 3.6, DURATION);
        if (fireT > 0 && fireFade > 0) {
            double scale = fireT * fireFade;
            drawFire(t, w / 2, h * 0.85, 1.4 * scale);
            drawFire(t * 1.3, w * 0.28, h * 0.88, 0.9 * scale);
            drawFire(t * 0.9, w * 0.72, h * 0.88, 0.9 * scale);
        }

        // 10) "KING JAMES" 大字 (2.6–4.2)
        double textT = window(t, 2.6, 3.0);
        if (textT > 0) {
            double textFade = 1 - window(t, 3.6, DURATION);
            double scale = 0.6 + textT * 0.55;
            double y = h * 0.22 + (1 - textT) * 40;
            drawKingText(w / 2, y, scale, textFade);
        }

        gc.restore();
    }

    /** 像素風火焰 (cx, cy=底部, scale=大小倍率). */
    private void drawFire(double timeSeed, double cx, double bottomY, double scale) {
        double flicker = Math.sin(timeSeed * 14) * 0.08;
        drawFireLayer(timeSeed, cx, bottomY, 180 * scale, 100 * scale * (1 + flicker),
                Color.web("#7A1500"), Color.web("#DB2A1F"));
        drawFireLayer(timeSeed + 0.1, cx, bottomY - 6, 130 * scale, 75 * scale,
                Color.web("#DB2A1F"), Color.web("#FF8C1A"));
        drawFireLayer(timeSeed + 0.2, cx, bottomY - 12, 80 * scale, 55 * scale,
                Color.web("#FF8C1A"), Color.web("#FFE03D"));
    }

    private void drawFireLayer(double timeSeed, double cx, double bottomY,
                               double width, double height, Color edge, Color core) {
        int rows = 16, cols = 30;
        double cellW = width / cols, cellH = height / rows;
        for (int y = 0; y < rows; y++) {
            double yFrac = y / (double)(rows - 1);
            double radius = (1 - yFrac * yFrac) * (cols * 0.45)
                    + Math.sin(y * 0.9 + timeSeed * 8) * 1.3;
            for (int x = 0; x < cols; x++) {
                double dx = Math.abs(x - (cols - 1) / 2.0);
                if (dx <= radius) {
                    boolean isEdge = dx > radius - 1.1;
                    gc.setFill(isEdge ? edge : core);
                    gc.fillRect(cx - width / 2 + x * cellW,
                            bottomY - (y + 1) * cellH,
                            cellW + 0.5, cellH + 0.5);
                }
            }
        }
    }

    private void drawKingText(double cx, double cy, double scale, double alpha) {
        gc.save();
        gc.translate(cx, cy);
        gc.scale(scale, scale);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // 主標題
        gc.setFont(javafx.scene.text.Font.font("Courier New",
                javafx.scene.text.FontWeight.BLACK, 120));
        // 紫色陰影
        gc.setFill(Color.web("#552583", alpha));
        gc.fillText("KING JAMES", 6, 6);
        // 黑色描邊（粗）
        gc.setStroke(Color.color(0, 0, 0, alpha));
        gc.setLineWidth(6);
        gc.strokeText("KING JAMES", 0, 0);
        // 金色主體
        gc.setFill(Color.web("#FDB927", alpha));
        gc.fillText("KING JAMES", 0, 0);

        // 副標題
        gc.setFont(javafx.scene.text.Font.font("Courier New",
                javafx.scene.text.FontWeight.BOLD, 24));
        gc.setFill(Color.web("white", alpha * 0.85));
        gc.fillText("— ULTIMATE · SLAM OF THE CHOSEN —", 0, 80);

        gc.restore();
    }

    private static Image loadImage(String path) {
        var stream = LeBronUltimate.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("[LeBronUltimate] Image not found: " + path);
            return null;
        }
        return new Image(stream);
    }
}
