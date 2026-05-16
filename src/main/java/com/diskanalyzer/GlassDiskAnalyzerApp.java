package com.diskanalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class GlassDiskAnalyzerApp extends Application {
    
    private static final String APP_NAME = "磁盘空间分析器";
    private static final String APP_VERSION = "3.0";
    private static final String APP_TITLE = APP_NAME + " v" + APP_VERSION;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/glass-main-view.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1400, 900);
            scene.setFill(Color.TRANSPARENT);
            
            // Load theme CSS before showing
            String cssPath = getClass().getResource("/glass-styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.getIcons().add(createAppIcon());
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            primaryStage.show();
            
            System.out.println("=== " + APP_TITLE + " 启动成功 ===");
            System.out.println("JavaFX版本: " + System.getProperty("javafx.version"));
            System.out.println("Java版本: " + System.getProperty("java.version"));
            
        } catch (IOException e) {
            System.err.println("应用程序初始化失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private Image createAppIcon() {
        WritableImage image = new WritableImage(64, 64);
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(64, 64);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.web("#2196F3"));
        gc.fillOval(0, 0, 64, 64);
        
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeOval(8, 8, 48, 48);
        
        gc.strokeArc(16, 16, 32, 32, 0, 120, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(16, 16, 32, 32, 120, 120, javafx.scene.shape.ArcType.OPEN);
        gc.strokeArc(16, 16, 32, 32, 240, 120, javafx.scene.shape.ArcType.OPEN);
        
        gc.setFill(Color.WHITE);
        gc.fillOval(30, 30, 4, 4);
        
        return canvas.snapshot(null, image);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
