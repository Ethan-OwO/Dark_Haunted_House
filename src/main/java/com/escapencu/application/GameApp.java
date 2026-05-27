package com.escapencu.application;

import com.escapencu.core.SceneManager;
import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class GameApp extends Application {
    public static final int WIDTH  = 960;
    public static final int HEIGHT = 640;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Escape from NCU");
        primaryStage.setResizable(true);
        // F11 切換全螢幕：掛在 Stage 層，換場景也不失效
        primaryStage.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });

        SceneManager.init(primaryStage);
        SceneManager.showMainMenu();
        primaryStage.show();
    }
}
