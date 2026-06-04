package com.escapencu.guanzhang;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;

import java.util.Random;

/**
 * 館長陳之漢 選技能預覽卡 (120×180)
 *
 * 風格：黑色調 + 暗紅光暈 + 刺青紋路 + 肌肉像素人
 * 動態：金屬光澤閃爍 + 刺青能量脈衝
 */
public class GuanZhangPreview extends StackPane {

    private final double w, h;
    private final Canvas canvas;
    private final GraphicsContext gc;

    private long lastNanos = -1;
    private double globalTime = 0;
    private int frameIndex = 0;
    private double frameTimer = 0;
    private static final double FRAME_DUR = 0.35;

    private AnimationTimer timer;
    private final Random rng = new Random(42);

    // ── 館長像素肌肉人 (32×44) ──────────────────────────────────────────
    private static final String[] FRAME_A = {
        "................................",
        "............KKKKKK..............",
        "...........KDDDDDDDK............",
        "...........KDSSSSSDDK...........",
        "...........KDSRRSSDDK...........",
        "...........KDSRRSSDDK...........",
        "...........KDDSSSSDK............",
        "............KKKKKK..............",
        "........KKKKKKKKKKKKKK..........",
        ".......KMMMMMMMMMMMMMMK.........",
        ".......KMmMMMMMMMMMmMK..........",
        ".......KMMmMMMMMMMmMMK..........",
        ".......KMMMMmMMMmMMMMK..........",
        "........KMMMMMMMMMMK............",
        "........KMMKKKKKKMK.............",
        ".......KMKK.KTTTTK.KKM..........",
        ".......KMK..KTtTTK..KM..........",
        ".......KMK..KTTTTK..KM..........",
        ".......KKK..KKKKKK..KKK.........",
        ".........KMMKK..KKMM............",
        ".........KMMmK..KmMMK...........",
        ".........KMMMKKKKMMMk...........",
        ".........KKKKKKKKKKK............",
        "...........KMMK.KMMK............",
        "...........KMMk.kMMK............",
        "...........KKKK.KKKK............",
        "................................",
        "................................",
    };

    private static final String[] FRAME_B = {
        "................................",
        "............KKKKKK..............",
        "...........KDDDDDDDK............",
        "...........KDSSSSSDDK...........",
        "...........KDSRRSSDDK...........",
        "...........KDSRRSSDDK...........",
        "...........KDDSSSSDK............",
        "............KKKKKK..............",
        "........KKKKKKKKKKKKKK..........",
        ".......KMMMMMMMMMMMMMMK.........",
        "......KKMmMMMMMMMMMmMKK.........",
        "......KMMmMMMMMMMmMMMMK.........",
        "......KMMMMmMMMmMMMMMK..........",
        ".......KKMMMMMMMMMMkK...........",
        ".......KMMMKKKKKKMMKk...........",
        "......KMKK..KTTTTK..KKM.........",
        "......KMK...KTtTTK...KM.........",
        "......KMK...KTTTTK...KM.........",
        "......KKK...KKKKKK...KKK........",
        "..........KMMKK..KKMM...........",
        "..........KMMmK..KmMMK..........",
        "..........KMMMKKKKMMMk..........",
        "..........KKKKKKKKKKK...........",
        "............KMMK.KMMK...........",
        "............KMMk.kMMK...........",
        "............KKKK.KKKK...........",
        "................................",
        "................................",
    };

    private static final java.util.Map<Character, Color> COLORS = java.util.Map.ofEntries(
        java.util.Map.entry('K', Color.web("#000000")),
        java.util.Map.entry('D', Color.web("#1a0a0a")),
        java.util.Map.entry('S', Color.web("#3a1a1a")),
        java.util.Map.entry('R', Color.web("#8B0000")),
        java.util.Map.entry('M', Color.web("#5a3020")),
        java.util.Map.entry('m', Color.web("#7a4030")),
        java.util.Map.entry('T', Color.web("#c0392b")),
        java.util.Map.entry('t', Color.web("#e74c3c")),
        java.util.Map.entry('k', Color.web("#2c1010"))
    );

    public GuanZhangPreview(double width, double height) {
        this.w = width;
        this.h = height;
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setStyle("-fx-background-color: #080808;");

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);

        Text name = new Text("館長陳之漢");
        name.setFill(Color.web("#cc0000"));
        name.setFont(Font.loadFont(
            GuanZhangPreview.class.getResourceAsStream("/fonts/Cubic_11.ttf"), 14));
        DropShadow glow = new DropShadow(12, Color.web("#cc0000", 0.9));
        name.setEffect(glow);

        Text sub = new Text("THE IRON GUARDIAN");
        sub.setFill(Color.color(1, 1, 1, 0.5));
        sub.setFont(Font.font("Courier New", FontWeight.BOLD, 9));

        javafx.scene.layout.VBox nameplate = new javafx.scene.layout.VBox(3, name, sub);
        nameplate.setAlignment(javafx.geometry.Pos.CENTER);
        nameplate.setTranslateY(height / 2 - 28);

        getChildren().addAll(canvas, nameplate);
    }

    public void start() {
        if (timer != null) return;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanos < 0) { lastNanos = now; return; }
                double dt = (now - lastNanos) / 1e9;
                lastNanos = now;
                globalTime += dt;
                frameTimer += dt;
                if (frameTimer >= FRAME_DUR) { frameTimer = 0; frameIndex = 1 - frameIndex; }
                draw();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; lastNanos = -1; }
    }

    private void draw() {
        gc.clearRect(0, 0, w, h);

        // 黑底
        gc.setFill(Color.web("#080808"));
        gc.fillRect(0, 0, w, h);

        // 暗紅光暈（上）— 脈衝
        double pulse = 0.18 + 0.07 * Math.sin(globalTime * 3.2);
        gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.25, w * 0.65, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8B0000", pulse)),
                new Stop(1, Color.web("#8B0000", 0))));
        gc.fillRect(0, 0, w, h);

        // 暗灰底光（下）
        gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.82, w * 0.5, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#333333", 0.25)),
                new Stop(1, Color.web("#333333", 0))));
        gc.fillRect(0, 0, w, h);

        // 刺青紋路（靜態裝飾線）
        drawTattooLines();

        // 腳影
        double feetY = h * 0.60 + 56;
        gc.setFill(Color.color(0, 0, 0, 0.5));
        gc.fillOval(w / 2 - 40, feetY - 8, 80, 14);

        // 像素人
        String[] grid = (frameIndex == 0) ? FRAME_A : FRAME_B;
        double dy = (frameIndex == 0) ? 0 : -3;
        int cell = 3;
        double originX = w / 2 - (32 * cell) / 2.0;
        double originY = h * 0.58 - 28 * cell + dy;
        for (int row = 0; row < grid.length; row++) {
            String line = grid[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                Color c = COLORS.get(ch);
                if (c == null) continue;
                gc.setFill(c);
                gc.fillRect(originX + col * cell, originY + row * cell, cell, cell);
            }
        }

        // 能量氣場粒子（暗紅火星）
        for (int i = 0; i < 8; i++) {
            double phase = (globalTime * 0.6 + i * 0.4) % 1.0;
            double ex = w / 2 + Math.sin(i * 1.9 + globalTime) * 35;
            double ey = feetY - phase * 90;
            double sz = (1 - phase) * 4 + 1;
            gc.setFill(Color.color(0.7, 0.05, 0.05, 1 - phase));
            gc.fillRect(ex - sz / 2, ey - sz / 2, sz, sz);
        }
    }

    private void drawTattooLines() {
        gc.setStroke(Color.web("#3a0000", 0.35));
        gc.setLineWidth(1);
        // 左臂紋
        gc.strokeLine(12, 70, 20, 95);
        gc.strokeLine(10, 80, 18, 75);
        gc.strokeLine(8, 90, 17, 85);
        // 右臂紋
        gc.strokeLine(w - 12, 70, w - 20, 95);
        gc.strokeLine(w - 10, 80, w - 18, 75);
        gc.strokeLine(w - 8, 90, w - 17, 85);
        // 胸口符文
        gc.strokeLine(w / 2 - 8, 100, w / 2 + 8, 100);
        gc.strokeLine(w / 2, 95, w / 2, 108);
    }
}
