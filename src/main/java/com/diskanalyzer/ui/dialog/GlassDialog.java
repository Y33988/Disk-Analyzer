package com.diskanalyzer.ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;

public class GlassDialog extends Stage {
    
    public GlassDialog(Stage owner, String title) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle(title);
    }
    
    protected VBox createGlassContent() {
        VBox content = new VBox(15);
        content.getStyleClass().add("glass-dialog-content");
        content.setPadding(new Insets(25));
        return content;
    }
    
    protected HBox createButtonBar(Button... buttons) {
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        buttonBar.getChildren().addAll(buttons);
        return buttonBar;
    }
    
    protected Label createTitleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("glass-label-title");
        return label;
    }
    
    protected Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("glass-label");
        return label;
    }
    
    protected TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("glass-text-field");
        return field;
    }
    
    public void showGlass() {
        Scene scene = getScene();
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
        }
        show();
    }
    
    protected void applyGlassStyle(Pane pane) {
        pane.getStyleClass().addAll("glass-panel");
    }
}
