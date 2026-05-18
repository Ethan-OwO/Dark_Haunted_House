package com.escapencu.lebron;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;

import java.util.Random;

/**
 * LeBron James 角色選擇預覽。
 *   ┌────────────────────────────┐
 *   │  紫金漸層背景               │
 *   │       LeBron (運球, 2幀)    │
 *   │       ░░ 動態火焰 ░░        │
 *   │       LEBRON JAMES          │
 *   │       THE KING · #23        │
 *   └────────────────────────────┘
 *
 * 用法 (Scene 內):
 *   LeBronPreview preview = new LeBronPreview(520, 720);
 *   root.getChildren().add(preview);
 *   preview.start();  // 啟動動畫
 *   // 結束時呼叫 preview.stop()
 */
public class LeBronPreview extends StackPane {

    private final double w, h;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // Sprite 兩幀（要從 /images/player/ 載入）
    private static final Image FRAME_A = loadImage("/images/player/lbj_idle_s.png");
    private static final Image FRAME_B = loadImage("/images/player/lbj_walk1_s.png");

    private static final double DRIBBLE_FRAME_DUR = 0.28; // 每幀 0.28 秒
    private static final double SPRITE_SIZE = 320;        // sprite 顯示尺寸 (px)

    private long lastNanos = -1;
    private double frameTimer = 0;
    private int currentFrame = 0;
    private double globalTime = 0;
    private AnimationTimer timer;

    private final Random rng = new Random(7);

    public LeBronPreview(double width, double height) {
        this.w = width;
        this.h = height;
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setStyle("-fx-background-color: #0c0a09;");

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false); // 保持像素清晰

        // 名牌
        Text name = new Text("LEBRON JAMES");
        name.setFill(Color.web("#FDB927"));
        name.setFont(Font.font("Courier New", FontWeight.BLACK, 28));
        DropShadow nameGlow = new DropShadow(20, Color.web("#FDB927", 0.8));
        nameGlow.setInput(new DropShadow(0, 3, 3, Color.web("#552583")));
        name.setEffect(nameGlow);

        Text sub = new Text("THE KING · #23");
        sub.setFill(Color.color(1, 1, 1, 0.7));
        sub.setFont(Font.font("Courier New", FontWeight.BOLD, 13));

        javafx.scene.layout.VBox nameplate = new javafx.scene.layout.VBox(6, name, sub);
        nameplate.setAlignment(javafx.geometry.Pos.CENTER);
        nameplate.setTranslateY(height / 2 - 60);

        getChildren().addAll(canvas, nameplate);
    }

    public void start() {
        if (timer != null) return;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanos < 0) { lastNanos = now; return; }
                double dt = (now - lastNanos) / 1e9;
                lastNanos = now;
                update(dt);
                draw();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; lastNanos = -1; }
    }

    private void update(double dt) {
        globalTime += dt;
        frameTimer += dt;
        if (frameTimer >= DRIBBLE_FRAME_DUR) {
            frameTimer = 0;
            currentFrame = 1 - currentFrame;
        }
    }

    private void draw() {
        // 1) 漸層背景
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#0c0a09"));
        gc.fillRect(0, 0, w, h);
        // 紫光（上）
        gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.3, w * 0.7, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#552583", 0.35)),
                new Stop(1, Color.web("#552583", 0))));
        gc.fillRect(0, 0, w, h);
        // 火光（下）
        gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.8, w * 0.6, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff5000", 0.30)),
                new Stop(1, Color.web("#ff5000", 0))));
        gc.fillRect(0, 0, w, h);

        // 2) 地板影子
        double feetY = h * 0.62 + SPRITE_SIZE / 2.0 - 14;
        gc.setFill(Color.color(0, 0, 0, 0.45));
        gc.fillOval(w / 2 - 90, feetY - 12, 180, 24);

        // 3) 腳下火焰 (3 層 + 火星)
        drawFire(w / 2, feetY + 6, 1.0);

        // 4) Sprite
        Image frame = (currentFrame == 0) ? FRAME_A : FRAME_B;
        double dy = (currentFrame == 0) ? 0 : -4; // 第二幀稍微 bob
        if (frame != null) {
            gc.drawImage(frame,
                    w / 2 - SPRITE_SIZE / 2,
                    h * 0.62 - SPRITE_SIZE / 2 + dy,
                    SPRITE_SIZE, SPRITE_SIZE);
        }
    }

    /** 在 (cx, cy) 畫一團三層像素風火焰 + 火星，使用 globalTime 做閃爍. */
    private void drawFire(double cx, double cy, double scale) {
        double flicker = Math.sin(globalTime * 14) * 0.06 + Math.sin(globalTime * 9) * 0.04;

        // 外層 — 暗紅
        drawFireLayer(cx, cy, 130 * scale, 70 * scale * (1 + flicker),
                Color.web("#7A1500"), Color.web("#DB2A1F"));
        // 中層 — 橘紅
        drawFireLayer(cx, cy - 6, 95 * scale, 55 * scale * (1 - flicker * 0.5),
                Color.web("#DB2A1F"), Color.web("#FF8C1A"));
        // 內層 — 黃
        drawFireLayer(cx, cy - 10, 60 * scale, 40 * scale * (1 + flicker),
                Color.web("#FF8C1A"), Color.web("#FFE03D"));

        // 火星（embers）
        for (int i = 0; i < 10; i++) {
            double phase = (globalTime * 0.8 + i * 0.31) % 1.0;
            double ex = cx + Math.sin(i * 2.3) * 70 * scale + Math.sin(globalTime * 2 + i) * 8;
            double ey = cy - phase * 120 * scale;
            double size = (1 - phase) * 6 + 2;
            double alpha = 1 - phase;
            gc.setFill(Color.color(1, 0.7 + i % 2 * 0.2, 0.2, alpha));
            gc.fillRect(ex - size / 2, ey - size / 2, size, size);
        }
    }

    /** 三角形/水滴狀的火焰層，由 cellCount 個方塊堆出像素感. */
    private void drawFireLayer(double cx, double bottomY, double width, double height,
                               Color edgeColor, Color coreColor) {
        int rows = 14;
        int cols = 26;
        double cellW = width / cols;
        double cellH = height / rows;
        for (int y = 0; y < rows; y++) {
            double yFrac = y / (double) (rows - 1);
            double radius = (1 - yFrac * yFrac) * (cols * 0.45)
                    + Math.sin(y * 0.9 + globalTime * 8) * 1.2
                    + (rng.nextDouble() - 0.5) * 0.6;
            for (int x = 0; x < cols; x++) {
                double dx = Math.abs(x - (cols - 1) / 2.0);
                if (dx <= radius) {
                    boolean edge = dx > radius - 1.1;
                    gc.setFill(edge ? edgeColor : coreColor);
                    gc.fillRect(
                            cx - width / 2 + x * cellW,
                            bottomY - (y + 1) * cellH,
                            cellW + 0.5, cellH + 0.5);
                }
            }
        }
    }

    private static Image loadImage(String path) {
        var stream = LeBronPreview.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("[LeBronPreview] Image not found: " + path);
            return null;
        }
        return new Image(stream);
    }
}
