package com.diskanalyzer.ui.component;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class GlassEffectPane extends Pane {
    
    private final ImageView glassBackground = new ImageView();
    private final Pane contentLayer = new Pane();
    private final BoxBlur blurEffect = new BoxBlur(15, 15, 3);
    
    private Node targetNode;
    private Timeline refreshTimeline;
    private double blurIntensity = 15;
    private double tintOpacity = 0.15;
    private Color tintColor = Color.WHITE;
    
    public GlassEffectPane() {
        this(15, 0.15);
    }
    
    public GlassEffectPane(double blurIntensity, double tintOpacity) {
        super();
        this.blurIntensity = blurIntensity;
        this.tintOpacity = tintOpacity;
        setupLayers();
        setupAutoRefresh();
    }
    
    private void setupLayers() {
        getChildren().addAll(glassBackground, contentLayer);
        
        glassBackground.setEffect(blurEffect);
        glassBackground.setMouseTransparent(true);
        glassBackground.setSmooth(true);
        
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        setClip(clip);
        
        contentLayer.setMouseTransparent(false);
        
        getStyleClass().add("glass-effect-pane");
    }
    
    private void setupAutoRefresh() {
        refreshTimeline = new Timeline();
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(500), e -> {
                if (isVisible() && targetNode != null) {
                    captureBackground();
                }
            })
        );
        refreshTimeline.play();
    }
    
    public void setTargetNode(Node node) {
        this.targetNode = node;
        captureBackground();
    }
    
    public void captureBackground() {
        if (targetNode == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                params.setTransform(
                    javafx.scene.transform.Transform.translate(
                        -localToScene(0, 0).getX() + targetNode.localToScene(0, 0).getX(),
                        -localToScene(0, 0).getY() + targetNode.localToScene(0, 0).getY()
                    )
                );
                
                javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(
                    (int) Math.ceil(getWidth()),
                    (int) Math.ceil(getHeight())
                );
                
                targetNode.snapshot(params, image);
                
                glassBackground.setImage(image);
                glassBackground.setFitWidth(getWidth());
                glassBackground.setFitHeight(getHeight());
                glassBackground.setPreserveRatio(false);
                
            } catch (Exception e) {
                System.err.println("Glass effect capture failed: " + e.getMessage());
            }
        });
    }
    
    public void addContent(Node... children) {
        contentLayer.getChildren().addAll(children);
    }
    
    public Pane getContentLayer() {
        return contentLayer;
    }
    
    public void setBlurIntensity(double intensity) {
        this.blurIntensity = intensity;
        blurEffect.setWidth(intensity);
        blurEffect.setHeight(intensity);
        captureBackground();
    }
    
    public void setTintOpacity(double opacity) {
        this.tintOpacity = opacity;
        captureBackground();
    }
    
    public void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
    
    public void startRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.play();
        }
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        glassBackground.setLayoutX(0);
        glassBackground.setLayoutY(0);
        glassBackground.setFitWidth(getWidth());
        glassBackground.setFitHeight(getHeight());
        
        contentLayer.setLayoutX(0);
        contentLayer.setLayoutY(0);
        contentLayer.resizeRelocate(0, 0, getWidth(), getHeight());
    }
    
    @Override
    public String getUserAgentStylesheet() {
        return getClass().getResource("/glass-styles.css").toExternalForm();
    }
}
