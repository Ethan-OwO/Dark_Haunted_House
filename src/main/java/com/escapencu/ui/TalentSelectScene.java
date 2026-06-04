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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    private static final Image GZ_THUMB = loadImage("/images/player/gz.jpg");

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

        StackPane lebronCard    = buildLeBronCard();
        StackPane guanZhangCard = buildGuanZhangCard();
        StackPane noneCard      = buildNoneCard();

        lebronCard.setOnMouseClicked(e -> {
            chosen = GameState.Talent.LEBRON;
            selectedLabel.setText("已選擇：LeBron 夥伴");
            applyBorder(lebronCard, true);
            applyBorder(guanZhangCard, false);
            applyBorder(noneCard, false);
        });
        guanZhangCard.setOnMouseClicked(e -> {
            chosen = GameState.Talent.GUAN_ZHANG;
            selectedLabel.setText("已選擇：館長山羌");
            applyBorder(lebronCard, false);
            applyBorder(guanZhangCard, true);
            applyBorder(noneCard, false);
        });
        noneCard.setOnMouseClicked(e -> {
            chosen = GameState.Talent.NONE;
            selectedLabel.setText("已選擇：無天賦");
            applyBorder(lebronCard, false);
            applyBorder(guanZhangCard, false);
            applyBorder(noneCard, true);
        });

        // 預設選中「無天賦」
        applyBorder(noneCard, true);
        applyBorder(lebronCard, false);
        applyBorder(guanZhangCard, false);

        HBox cardsBox = new HBox(30, lebronCard, guanZhangCard, noneCard);
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

    private StackPane buildGuanZhangCard() {
        // 縮圖用 gz.jpg
        Canvas preview = new Canvas(120, 180);
        GraphicsContext pgc = preview.getGraphicsContext2D();
        pgc.setFill(Color.web("#0a0a0a"));
        pgc.fillRect(0, 0, 120, 180);
        if (GZ_THUMB != null) {
            // 置中裁切顯示
            double iw = GZ_THUMB.getWidth(), ih = GZ_THUMB.getHeight();
            double scale = Math.max(120.0 / iw, 180.0 / ih);
            double dw = iw * scale, dh = ih * scale;
            pgc.drawImage(GZ_THUMB, (120 - dw) / 2, (180 - dh) / 2, dw, dh);
        } else {
            pgc.setFill(Color.web("#cc0000"));
            pgc.setFont(Font.font("System", FontWeight.BOLD, 48));
            pgc.fillText("館", 30, 110);
        }
        // 底部半透明遮罩讓文字可讀
        pgc.setFill(Color.color(0, 0, 0, 0.45));
        pgc.fillRect(0, 130, 120, 50);

        Text name = new Text("館長山羌");
        name.setFont(pf(15));
        name.setFill(Color.web("#FFD700"));

        Text desc = new Text("被動：血量歸零後\n無敵 5 秒，恢復 50 HP\n（每局限一次）");
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

    private static Image loadImage(String path) {
        var stream = TalentSelectScene.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("[TalentSelectScene] Image not found: " + path);
            return null;
        }
        return new Image(stream);
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
