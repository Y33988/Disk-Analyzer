package com.diskanalyzer.ui.effect;

import javafx.scene.SnapshotParameters;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * 毛玻璃效果工具类 - 通过捕获背景节点并模糊来实现真正的液态玻璃效果
 */
public class BackdropBlurEffect {
    
    /**
     * 为指定节点应用毛玻璃背景效果
     * @param target 要应用效果的节点
     * @param backgroundNode 要捕获的背景节点
     */
    public static void applyToPane(Pane target, Node backgroundNode) {
        applyToPane(target, backgroundNode, 15, 0.85);
    }
    
    /**
     * 为指定节点应用毛玻璃背景效果
     * @param target 要应用效果的节点
     * @param backgroundNode 要捕获的背景节点  
     * @param blurRadius 模糊半径
     * @param opacity 不透明度 (0-1)
     */
    public static void applyToPane(Pane target, Node backgroundNode, double blurRadius, double opacity) {
        try {
            WritableImage snapshot = captureNodeBackground(target, backgroundNode);
            if (snapshot != null) {
                ImageView blurredView = createBlurredView(snapshot, blurRadius);
                
                Rectangle clip = new Rectangle();
                clip.widthProperty().bind(target.widthProperty());
                clip.heightProperty().bind(target.heightProperty());
                clip.setArcWidth(16);
                clip.setArcHeight(16);
                
                blurredView.setClip(clip);
                
                Pane glassLayer = new Pane(blurredView);
                glassLayer.setMouseTransparent(true);
                glassLayer.getStyleClass().add("glass-backdrop-layer");
                
                target.getChildren().add(0, glassLayer);
            }
        } catch (Exception e) {
            System.err.println("毛玻璃效果应用失败: " + e.getMessage());
        }
    }
    
    /**
     * 捕获节点背景区域的快照
     */
    private static WritableImage captureNodeBackground(Node target, Node backgroundNode) {
        Scene scene = target.getScene();
        if (scene == null) return null;
        
        double x = target.localToScene(0, 0).getX();
        double y = target.localToScene(0, 0).getY();
        double width = target.getBoundsInLocal().getWidth();
        double height = target.getBoundsInLocal().getHeight();
        
        if (width <= 0 || height <= 0) return null;
        
        SnapshotParameters params = new SnapshotParameters();
        params.setViewport(new javafx.geometry.Rectangle2D(x, y, width, height));
        params.setFill(Color.TRANSPARENT);
        
        try {
            return backgroundNode.snapshot(params, null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 创建模糊视图
     */
    private static ImageView createBlurredView(WritableImage image, double blurRadius) {
        ImageView view = new ImageView(image);
        BoxBlur blur = new BoxBlur(blurRadius, blurRadius, 3);
        view.setEffect(blur);
        return view;
    }
    
    /**
     * 创建纯色半透明背景（作为备选方案）
     */
    public static Background createTranslucentBackground(Color baseColor, double opacity) {
        return Background.fill(
            new Color(
                baseColor.getRed(),
                baseColor.getGreen(), 
                baseColor.getBlue(),
                opacity
            )
        );
    }
}
