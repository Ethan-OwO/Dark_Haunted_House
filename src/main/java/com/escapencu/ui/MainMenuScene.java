package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.SceneManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class MainMenuScene {

    // 載入支援中文的像素字型（Windows 也適用，只是換了字型外觀）
    private static Font pf(double size) {
        Font f = Font.loadFont(MainMenuScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), size);
        return f != null ? f : Font.font(size);
    }

    public Scene build() {
        // === 1. 主選單介面 ===
        Text title = new Text("逃離中央大學");
        title.setFont(pf(52));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("Escape from NCU");
        subtitle.setFont(pf(20));
        subtitle.setFill(Color.LIGHTGRAY);

        Button startBtn = new Button("開始遊戲");
        startBtn.setFont(pf(24));
        startBtn.setPrefWidth(220);
        startBtn.setOnAction(e -> SceneManager.showTalentSelect());

        // 新增：操作說明按鈕
        Button controlsBtn = new Button("操作說明");
        controlsBtn.setFont(pf(24));
        controlsBtn.setPrefWidth(220);

        Button exitBtn = new Button("離開");
        exitBtn.setFont(pf(18));
        exitBtn.setPrefWidth(220);
        exitBtn.setOnAction(e -> System.exit(0));

        VBox mainMenu = new VBox(22, title, subtitle, startBtn, controlsBtn, exitBtn);
        mainMenu.setAlignment(Pos.CENTER);

        // === 2. 操作說明介面 (基礎版) ===
        Text controlsTitle = new Text("操作說明");
        controlsTitle.setFont(pf(52));
        controlsTitle.setFill(Color.WHITE);

        Text controlsText = new Text(
                "W / A / S / D : 移動\n\n" +
                        "滑鼠左鍵 : 射擊\n\n" +
                        "Space (空白鍵) : 衝刺 / 翻滾\n\n" +
                        "P : 暫停遊戲"
        );
        controlsText.setFont(pf(24));
        controlsText.setFill(Color.WHITE);
        controlsText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button backBtn = new Button("返回");
        backBtn.setFont(pf(24));
        backBtn.setPrefWidth(220);

        VBox controlsMenu = new VBox(40, controlsTitle, controlsText, backBtn);
        controlsMenu.setAlignment(Pos.CENTER);
        controlsMenu.setStyle("-fx-background-color: transparent;");
        controlsMenu.setVisible(false); // 預設隱藏

        // === 3. 按鈕切換邏輯 ===
        controlsBtn.setOnAction(e -> {
            mainMenu.setVisible(false);
            controlsMenu.setVisible(true);
        });

        backBtn.setOnAction(e -> {
            controlsMenu.setVisible(false);
            mainMenu.setVisible(true);
        });

        // === 4. 背景圖 Canvas ===
        Canvas bgCanvas = new Canvas(GameApp.WIDTH, GameApp.HEIGHT);
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();

        Image bg = new Image(MainMenuScene.class.getResourceAsStream(
                "/images/menu_bg.jpg"));
        gc.drawImage(bg, 0, 0, GameApp.WIDTH, GameApp.HEIGHT);

        // 半透明深色遮罩，讓標題與按鈕更清楚
        gc.setFill(Color.color(0, 0, 0, 0.25));
        gc.fillRect(0, 0, GameApp.WIDTH, GameApp.HEIGHT);

        // === 5. 使用 StackPane 疊加並回傳 ===
        StackPane rootPane = new StackPane(bgCanvas, mainMenu, controlsMenu);

        return new Scene(rootPane, GameApp.WIDTH, GameApp.HEIGHT);
    }
}
