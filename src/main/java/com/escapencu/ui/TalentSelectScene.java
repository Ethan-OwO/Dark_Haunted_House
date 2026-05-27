package com.escapencu.ui;

import com.escapencu.application.GameApp;
import com.escapencu.core.GameState;
import com.escapencu.core.SceneManager;
import com.escapencu.lebron.LeBronPreview;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class TalentSelectScene {

    // 載入支援中文的像素字型（Windows 也適用，只是換了字型外觀）
    private static Font pf(double size) {
        Font f = Font.loadFont(TalentSelectScene.class.getResourceAsStream("/fonts/Cubic_11.ttf"), size);
        return f != null ? f : Font.font(size);
    }

    private GameState.Talent chosen = GameState.Talent.NONE;

    private LeBronPreview lebronPreview = null;

    public Scene build() {
        Text title = new Text("選擇天賦");
        title.setFont(pf(44));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("選擇一個天賦，在遊戲開始前生效");
        subtitle.setFont(pf(18));
        subtitle.setFill(Color.LIGHTGRAY);

        Text selectedLabel = new Text("已選擇：無天賦");
        selectedLabel.setFont(pf(16));
        selectedLabel.setFill(Color.web("#FDB927"));

        StackPane lebronCard = buildLeBronCard();
        StackPane noneCard   = buildNoneCard();

        lebronCard.setOnMouseClicked(e -> {
            chosen = GameState.Talent.LEBRON;
            selectedLabel.setText("已選擇：LeBron 夥伴");
            applyBorder(lebronCard, true);
            applyBorder(noneCard, false);
        });
        noneCard.setOnMouseClicked(e -> {
            chosen = GameState.Talent.NONE;
            selectedLabel.setText("已選擇：無天賦");
            applyBorder(lebronCard, false);
            applyBorder(noneCard, true);
        });

        // 預設選中「無天賦」
        applyBorder(noneCard, true);
        applyBorder(lebronCard, false);

        HBox cardsBox = new HBox(40, lebronCard, noneCard);
        cardsBox.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("確認開始");
        confirmBtn.setFont(pf(24));
        confirmBtn.setPrefWidth(220);
        confirmBtn.setOnAction(e -> {
            // 先停掉預覽動畫，避免 AnimationTimer 在場景切換後繼續跑
            if (lebronPreview != null) {
                lebronPreview.stop();
                lebronPreview = null;
            }
            GameState.selectedTalent      = chosen;
            GameState.talentUsedThisStage = false;
            SceneManager.showGame();
        });

        VBox root = new VBox(20, title, subtitle, cardsBox, selectedLabel, confirmBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e;");

        return new Scene(root, GameApp.WIDTH, GameApp.HEIGHT);
    }

    private StackPane buildLeBronCard() {
        lebronPreview = new LeBronPreview(120, 180);
        lebronPreview.start();
        LeBronPreview preview = lebronPreview;

        Text name = new Text("LeBron 夥伴");
        name.setFont(pf(15));
        name.setFill(Color.web("#FDB927"));

        Text desc = new Text("按 Q 召喚 LeBron 砸地板\n對全場敵人造成傷害並留下\n燃燒地板 10 秒\n（每 Stage 限用 1 次）");
        desc.setFont(pf(12));
        desc.setFill(Color.LIGHTGRAY);
        desc.setWrappingWidth(130);

        VBox info = new VBox(4, name, desc);
        info.setAlignment(Pos.CENTER);
        info.setPadding(new Insets(6, 8, 8, 8));

        VBox content = new VBox(0, preview, info);
        content.setAlignment(Pos.CENTER);

        StackPane card = new StackPane(content);
        card.setPadding(new Insets(12));
        card.setStyle(cardStyle(false));
        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    private StackPane buildNoneCard() {
        Canvas preview = new Canvas(120, 180);
        drawNonePreview(preview.getGraphicsContext2D());

        Text name = new Text("無天賦");
        name.setFont(pf(15));
        name.setFill(Color.LIGHTGRAY);

        Text desc = new Text("不選擇任何天賦\n純粹依靠自身實力");
        desc.setFont(pf(12));
        desc.setFill(Color.GRAY);
        desc.setWrappingWidth(130);

        VBox info = new VBox(4, name, desc);
        info.setAlignment(Pos.CENTER);
        info.setPadding(new Insets(6, 8, 8, 8));

        VBox content = new VBox(0, preview, info);
        content.setAlignment(Pos.CENTER);

        StackPane card = new StackPane(content);
        card.setPadding(new Insets(12));
        card.setStyle(cardStyle(false));
        card.setCursor(javafx.scene.Cursor.HAND);
        return card;
    }

    private String cardStyle(boolean selected) {
        String border = selected ? "#FDB927" : "#555555";
        return "-fx-background-color: #2a1050;" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 3;" +
               "-fx-border-radius: 10;" +
               "-fx-background-radius: 10;";
    }

    private void applyBorder(StackPane card, boolean selected) {
        card.setStyle(cardStyle(selected));
    }

    private void drawNonePreview(GraphicsContext gc) {
        // 深色背景
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRoundRect(0, 0, 120, 180, 10, 10);

        // 大問號（英文符號，不需要中文字型）
        gc.setFill(Color.web("#555555"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 72));
        gc.fillText("?", 32, 120);

        // 底部小字
        gc.setFill(Color.web("#444444"));
        gc.setFont(pf(12));
        gc.fillText("無天賦", 35, 155);
    }
}
