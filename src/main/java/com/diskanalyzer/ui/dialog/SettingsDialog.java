package com.diskanalyzer.ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SettingsDialog extends Stage {
    
    private TextField scanThreadsField;
    private TextField maxFileSizeField;
    private CheckBox scanHiddenFilesBox;
    private CheckBox skipSystemFilesBox;
    private ComboBox<String> themeCombo;
    private TextField excludePathsField;
    
    public SettingsDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("设置");
        
        VBox mainLayout = new VBox(15);
        mainLayout.getStyleClass().addAll("glass-container");
        mainLayout.setPadding(new Insets(20));
        mainLayout.setPrefWidth(550);
        mainLayout.setPrefHeight(450);
        
        HBox titleBar = createTitleBar();
        VBox contentPanel = createContentPanel();
        HBox bottomBar = createBottomBar();
        
        mainLayout.getChildren().addAll(titleBar, contentPanel, bottomBar);
        
        Scene scene = new Scene(mainLayout);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        String cssPath = getClass().getResource("/glass-styles.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        
        setScene(scene);
        show();
        mainLayout.applyCss();
        hide();
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("glass-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("\u2699\uFE0F 设置");
        title.getStyleClass().add("glass-label-title");
        
        Button closeButton = new Button("\u2715");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        titleBar.getChildren().addAll(title, spacer, closeButton);
        return titleBar;
    }
    
    private VBox createContentPanel() {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("glass-panel");
        panel.setPadding(new Insets(15));
        
        VBox generalSettings = createGeneralSettings();
        VBox scanSettings = createScanSettings();
        VBox themeSettings = createThemeSettings();
        
        panel.getChildren().addAll(
            createSectionTitle("\uD83D\uDD27 常规设置"),
            generalSettings,
            createSectionTitle("\uD83D\uDCC2 扫描设置"),
            scanSettings,
            createSectionTitle("\uD83C\uDFA8 主题设置"),
            themeSettings
        );
        
        return panel;
    }
    
    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("glass-label");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }
    
    private VBox createGeneralSettings() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        
        Label excludeLabel = new Label("排除路径:");
        excludeLabel.getStyleClass().add("glass-label");
        
        excludePathsField = new TextField("C:\\Windows,C:\\Program Files");
        excludePathsField.getStyleClass().add("glass-text-field");
        excludePathsField.setPromptText("用逗号分隔要排除的路径");
        
        grid.add(excludeLabel, 0, 0);
        grid.add(excludePathsField, 1, 0);
        GridPane.setColumnSpan(excludePathsField, 2);
        
        box.getChildren().add(grid);
        return box;
    }
    
    private VBox createScanSettings() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        
        Label threadsLabel = new Label("扫描线程数:");
        threadsLabel.getStyleClass().add("glass-label");
        
        scanThreadsField = new TextField("4");
        scanThreadsField.getStyleClass().add("glass-text-field");
        scanThreadsField.setPrefWidth(100);
        
        Label maxFileLabel = new Label("最大文件大小(MB):");
        maxFileLabel.getStyleClass().add("glass-label");
        
        maxFileSizeField = new TextField("1024");
        maxFileSizeField.getStyleClass().add("glass-text-field");
        maxFileSizeField.setPrefWidth(100);
        
        scanHiddenFilesBox = new CheckBox("扫描隐藏文件");
        scanHiddenFilesBox.getStyleClass().add("glass-check-box");
        scanHiddenFilesBox.setSelected(false);
        
        skipSystemFilesBox = new CheckBox("跳过系统文件");
        skipSystemFilesBox.getStyleClass().add("glass-check-box");
        skipSystemFilesBox.setSelected(true);
        
        grid.add(threadsLabel, 0, 0);
        grid.add(scanThreadsField, 1, 0);
        grid.add(maxFileLabel, 2, 0);
        grid.add(maxFileSizeField, 3, 0);
        grid.add(scanHiddenFilesBox, 0, 1);
        grid.add(skipSystemFilesBox, 2, 1);
        
        box.getChildren().add(grid);
        return box;
    }
    
    private VBox createThemeSettings() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        
        Label themeLabel = new Label("主题:");
        themeLabel.getStyleClass().add("glass-label");
        
        themeCombo = new ComboBox<>();
        themeCombo.getStyleClass().add("glass-combo-box");
        themeCombo.getItems().addAll("液态玻璃", "深色模式", "浅色模式");
        themeCombo.setValue("液态玻璃");
        themeCombo.setPrefWidth(150);
        
        grid.add(themeLabel, 0, 0);
        grid.add(themeCombo, 1, 0);
        
        box.getChildren().add(grid);
        return box;
    }
    
    private HBox createBottomBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_RIGHT);
        
        Button resetButton = new Button("\u6062\u590D\u9ED8\u8BA4");
        resetButton.getStyleClass().add("glass-button");
        resetButton.setOnAction(e -> resetToDefaults());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button saveButton = new Button("\u4FDD\u5B58");
        saveButton.getStyleClass().add("glass-button-primary");
        saveButton.setOnAction(e -> saveSettings());
        
        Button cancelButton = new Button("\u53D6\u6D88");
        cancelButton.getStyleClass().add("glass-button");
        cancelButton.setOnAction(e -> close());
        
        bar.getChildren().addAll(resetButton, spacer, saveButton, cancelButton);
        return bar;
    }
    
    private void resetToDefaults() {
        scanThreadsField.setText("4");
        maxFileSizeField.setText("1024");
        scanHiddenFilesBox.setSelected(false);
        skipSystemFilesBox.setSelected(true);
        excludePathsField.setText("C:\\Windows,C:\\Program Files");
        themeCombo.setValue("液态玻璃");
    }
    
    private void saveSettings() {
        try {
            int threads = Integer.parseInt(scanThreadsField.getText());
            if (threads < 1 || threads > 16) {
                showAlert("线程数必须在1-16之间");
                return;
            }
            
            int maxSize = Integer.parseInt(maxFileSizeField.getText());
            if (maxSize < 1) {
                showAlert("文件大小必须大于0");
                return;
            }
            
            showAlert("设置已保存");
            close();
        } catch (NumberFormatException e) {
            showAlert("请输入有效的数字");
        }
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("设置");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        alert.getDialogPane().getStylesheets().clear();
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/glass-styles.css").toExternalForm());
        
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        
        alert.showAndWait();
    }
}
