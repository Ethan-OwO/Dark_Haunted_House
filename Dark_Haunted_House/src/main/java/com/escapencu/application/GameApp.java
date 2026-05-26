package com.escapencu.application;

import com.escapencu.core.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class GameApp extends Application {
    public static final int WIDTH  = 960;
    public static final int HEIGHT = 640;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("逃離中央大學");
        primaryStage.setResizable(false);
        SceneManager.init(primaryStage);
        SceneManager.showMainMenu();
        primaryStage.show();
    }
}
