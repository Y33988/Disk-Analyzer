package com.diskanalyzer.ui.dialog;

import com.diskanalyzer.service.SystemCleaner;
import com.diskanalyzer.service.SystemCleaner.CleanCategory;
import com.diskanalyzer.service.SystemCleaner.CleanResult;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.concurrent.Task;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SystemCleanDialog extends Stage {
    
    private final SystemCleaner cleaner;
    private final Map<CleanCategory, CheckBox> categoryChecks = new EnumMap<>(CleanCategory.class);
    private final Map<CleanCategory, Label> sizeLabels = new EnumMap<>(CleanCategory.class);
    private TextArea logArea;
    private Button cleanButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    
    public SystemCleanDialog(Stage owner) {
        this.cleaner = new SystemCleaner();
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("系统清理");
        
        VBox mainLayout = new VBox(15);
        mainLayout.getStyleClass().addAll("glass-container");
        mainLayout.setPadding(new Insets(20));
        mainLayout.setPrefWidth(600);
        mainLayout.setPrefHeight(500);
        
        HBox titleBar = createTitleBar();
        VBox categoryPanel = createCategoryPanel();
        VBox logPanel = createLogPanel();
        HBox bottomBar = createBottomBar();
        
        mainLayout.getChildren().addAll(titleBar, categoryPanel, logPanel, bottomBar);
        
        Scene scene = new Scene(mainLayout);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        String cssPath = getClass().getResource("/glass-styles.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        
        setScene(scene);
    }
    
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("glass-title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("\uD83E\uDDF9 系统清理工具");
        title.getStyleClass().add("glass-label-title");
        
        Button closeButton = new Button("\u2715");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        titleBar.getChildren().addAll(title, spacer, closeButton);
        return titleBar;
    }
    
    private VBox createCategoryPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("glass-panel");
        panel.setPadding(new Insets(15));
        
        Label titleLabel = new Label("选择要清理的项目");
        titleLabel.getStyleClass().add("glass-label");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        int row = 0;
        int col = 0;
        for (CleanCategory category : CleanCategory.values()) {
            CheckBox checkBox = new CheckBox(category.getDisplayName());
            checkBox.getStyleClass().add("glass-check-box");
            checkBox.setSelected(true);
            categoryChecks.put(category, checkBox);
            
            Label sizeLabel = new Label("计算中...");
            sizeLabel.getStyleClass().add("glass-label");
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #90CAF9;");
            sizeLabels.put(category, sizeLabel);
            
            VBox itemBox = new VBox(5);
            itemBox.getChildren().addAll(checkBox, sizeLabel);
            
            grid.add(itemBox, col, row);
            
            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }
        
        Button refreshButton = new Button("\uD83D\uDD04 刷新大小");
        refreshButton.getStyleClass().add("glass-button");
        refreshButton.setOnAction(e -> calculateSizes());
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(titleLabel, refreshButton);
        
        VBox.setVgrow(grid, Priority.ALWAYS);
        panel.getChildren().addAll(topRow, grid);
        
        return panel;
    }
    
    private VBox createLogPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("glass-panel");
        panel.setPadding(new Insets(10));
        
        Label titleLabel = new Label("\uD83D\uDCCB 清理日志");
        titleLabel.getStyleClass().add("glass-label");
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.getStyleClass().add("glass-text-field");
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 12px;");
        
        statusLabel = new Label("\u51C6\u5907\u5C31\u7EEA");
        statusLabel.getStyleClass().add("glass-label");
        
        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.getStyleClass().add("glass-progress-bar");
        
        panel.getChildren().addAll(titleLabel, logArea, statusLabel, progressBar);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        return panel;
    }
    
    private HBox createBottomBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_RIGHT);
        
        Button selectAllButton = new Button("全选");
        selectAllButton.getStyleClass().add("glass-button");
        selectAllButton.setOnAction(e -> {
            categoryChecks.values().forEach(cb -> cb.setSelected(true));
        });
        
        Button deselectAllButton = new Button("取消全选");
        deselectAllButton.getStyleClass().add("glass-button");
        deselectAllButton.setOnAction(e -> {
            categoryChecks.values().forEach(cb -> cb.setSelected(false));
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        cleanButton = new Button("\uD83D\uDE80 开始清理");
        cleanButton.getStyleClass().add("glass-button-primary");
        cleanButton.setOnAction(e -> startClean());
        
        Button closeButton = new Button("\u5173\u95ED");
        closeButton.getStyleClass().add("glass-button");
        closeButton.setOnAction(e -> close());
        
        bar.getChildren().addAll(selectAllButton, deselectAllButton, spacer, cleanButton, closeButton);
        return bar;
    }
    
    private void calculateSizes() {
        logArea.appendText("正在计算可清理空间...\n");
        
        Task<Void> calcTask = new Task<>() {
            @Override
            protected Void call() {
                for (CleanCategory category : CleanCategory.values()) {
                    long size = cleaner.estimateSize(category);
                    Platform.runLater(() -> {
                        sizeLabels.get(category).setText(cleaner.formatSize(size));
                    });
                }
                return null;
            }
        };
        
        calcTask.setOnSucceeded(e -> logArea.appendText("计算完成\n"));
        calcTask.setOnFailed(e -> logArea.appendText("计算失败: " + calcTask.getException().getMessage() + "\n"));
        
        new Thread(calcTask).start();
    }
    
    private void startClean() {
        List<CleanCategory> selected = new ArrayList<>();
        for (Map.Entry<CleanCategory, CheckBox> entry : categoryChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }
        
        if (selected.isEmpty()) {
            showGlassAlert(Alert.AlertType.WARNING, "提示", "请至少选择一个清理项目");
            return;
        }
        
        cleanButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        logArea.clear();
        logArea.appendText("开始系统清理...\n");
        
        cleaner.setListener(new SystemCleaner.CleanProgressListener() {
            @Override
            public void onCategoryStart(CleanCategory category) {
                Platform.runLater(() -> {
                    statusLabel.setText("正在清理: " + category.getDisplayName());
                    logArea.appendText("\n[" + new java.util.Date() + "] 清理 " + category.getDisplayName() + "...\n");
                });
            }
            
            @Override
            public void onCategoryComplete(CleanResult result) {
                Platform.runLater(() -> {
                    logArea.appendText("  " + result.message + "\n");
                    if (result.success) {
                        logArea.appendText("  \u2705 清理成功\n");
                    } else {
                        logArea.appendText("  \u26A0\uFE0F 部分成功\n");
                    }
                });
            }
            
            @Override
            public void onAllComplete(List<CleanResult> results) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    cleanButton.setDisable(false);
                    statusLabel.setText("清理完成");
                    
                    long totalCleaned = results.stream().mapToLong(r -> r.cleanedSize).sum();
                    int totalFiles = results.stream().mapToInt(r -> r.fileCount).sum();
                    
                    logArea.appendText("\n====== 清理摘要 ======\n");
                    logArea.appendText("总计清理: " + cleaner.formatSize(totalCleaned) + "\n");
                    logArea.appendText("文件数量: " + totalFiles + "\n");
                    logArea.appendText("成功项目: " + results.stream().filter(r -> r.success).count() + "/" + results.size() + "\n");
                    logArea.appendText("===================\n");
                    
                    showGlassAlert(Alert.AlertType.INFORMATION, "清理完成", 
                        "系统清理完成！\n\n共清理: " + cleaner.formatSize(totalCleaned) + "\n文件数量: " + totalFiles);
                });
            }
            
            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    logArea.appendText("错误: " + message + "\n");
                    progressBar.setVisible(false);
                    cleanButton.setDisable(false);
                    statusLabel.setText("清理出错");
                });
            }
        });
        
        cleaner.clean(selected);
    }
    
    private void showGlassAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        String cssPath = getClass().getResource("/glass-styles.css").toExternalForm();
        alert.getDialogPane().getStylesheets().clear();
        alert.getDialogPane().getStylesheets().add(cssPath);
        
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        
        alert.showAndWait();
    }
}
