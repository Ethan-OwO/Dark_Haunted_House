package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.SceneManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class MainMenuScene {
    public Scene build() {
        // === 1. 主選單介面 ===
        Text title = new Text("逃離中央大學");
        title.setFont(Font.font(52));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("Escape from NCU");
        subtitle.setFont(Font.font(20));
        subtitle.setFill(Color.LIGHTGRAY);

        Button startBtn = new Button("開始遊戲");
        startBtn.setFont(Font.font(24));
        startBtn.setPrefWidth(220);
        startBtn.setOnAction(e -> SceneManager.showTalentSelect());

        // 新增：操作說明按鈕
        Button controlsBtn = new Button("操作說明");
        controlsBtn.setFont(Font.font(24));
        controlsBtn.setPrefWidth(220);

        Button exitBtn = new Button("離開");
        exitBtn.setFont(Font.font(18));
        exitBtn.setPrefWidth(220);
        exitBtn.setOnAction(e -> System.exit(0));

        VBox mainMenu = new VBox(22, title, subtitle, startBtn, controlsBtn, exitBtn);
        mainMenu.setAlignment(Pos.CENTER);

        // === 2. 操作說明介面 (基礎版) ===
        Text controlsTitle = new Text("操作說明");
        controlsTitle.setFont(Font.font(52));
        controlsTitle.setFill(Color.WHITE);

        Text controlsText = new Text(
                "W / A / S / D : 移動\n\n" +
                        "滑鼠左鍵 : 射擊\n\n" +
                        "Space (空白鍵) : 衝刺 / 翻滾\n\n" +
                        "P : 暫停遊戲"
        );
        controlsText.setFont(Font.font(24));
        controlsText.setFill(Color.WHITE);
        controlsText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button backBtn = new Button("返回");
        backBtn.setFont(Font.font(24));
        backBtn.setPrefWidth(220);

        VBox controlsMenu = new VBox(40, controlsTitle, controlsText, backBtn);
        controlsMenu.setAlignment(Pos.CENTER);
        controlsMenu.setStyle("-fx-background-color: #1a1a2e;"); // 和主畫面一樣的深色底
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

        // === 4. 使用 StackPane 疊加並回傳 ===
        StackPane rootPane = new StackPane(mainMenu, controlsMenu);
        rootPane.setStyle("-fx-background-color: #1a1a2e;");

        return new Scene(rootPane, GameApp.WIDTH, GameApp.HEIGHT);
    }
}