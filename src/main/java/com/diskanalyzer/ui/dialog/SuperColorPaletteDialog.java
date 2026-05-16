package com.diskanalyzer.ui.dialog;

import com.diskanalyzer.service.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class SuperColorPaletteDialog extends GlassDialog {

    private static final int MAX_HISTORY = 28;

    // Preview area
    private final Rectangle colorPreview = new Rectangle(150, 60);

    // RGB sliders
    private final Slider redSlider = createSlider(0, 255, 33);
    private final Slider greenSlider = createSlider(0, 255, 150);
    private final Slider blueSlider = createSlider(0, 255, 243);
    private final Slider alphaSlider = createSlider(0, 100, 100);

    // HSL sliders
    private final Slider hueSlider = createSlider(0, 360, 0);
    private final Slider saturationSlider = createSlider(0, 100, 80);
    private final Slider lightnessSlider = createSlider(0, 100, 50);

    // Value labels
    private final Label redLabel = new Label("33");
    private final Label greenLabel = new Label("150");
    private final Label blueLabel = new Label("243");
    private final Label alphaLabel = new Label("100%");
    private final Label hueLabel = new Label("0");
    private final Label saturationLabel = new Label("80%");
    private final Label lightnessLabel = new Label("50%");

    // Text fields
    private final TextField hexField = createTextField();
    private final TextField rgbField = createTextField();
    private final TextField hslField = createTextField();

    // Current color
    private Color currentColor = Color.rgb(33, 150, 243);

    // History
    private final List<Color> colorHistory = new ArrayList<>();
    private final GridPane historyGrid = new GridPane();

    // Gradient support
    private final GridPane presetGrid = new GridPane();
    private final GridPane gradientGrid = new GridPane();

    private Color gradientEndColor = Color.rgb(15, 23, 42);
    private final Rectangle gradientPreview = new Rectangle(150, 40);
    private final TextField gradientHexField = createTextField();
    private final Button pickEndColorBtn = new Button("选择结束色");

    // Owner reference for background modification
    private Stage ownerStage;
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private boolean isUpdatingFromHSL = false;

    public SuperColorPaletteDialog(Stage owner) {
        super(owner, "超级调色板");
        this.ownerStage = owner;
        setupUI();
        loadPresetColors();
        loadGradientPresets();
        loadHistory();
        updateColorDisplay();
    }

    private void setupUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setMinWidth(700);
        root.setMaxWidth(700);
        root.getStyleClass().add("glass-dialog-root");

        root.getChildren().add(createTitleBar());

        TabPane tabPane = new TabPane();

        // Tab 1: Single color picker
        Tab singleTab = new Tab("单色选择");
        singleTab.setClosable(false);
        singleTab.setContent(createSingleColorPanel());
        tabPane.getTabs().add(singleTab);

        // Tab 2: Gradient picker
        Tab gradientTab = new Tab("渐变调色");
        gradientTab.setClosable(false);
        gradientTab.setContent(createGradientPanel());
        tabPane.getTabs().add(gradientTab);

        // Tab 3: Background modifier
        Tab bgTab = new Tab("程序背景");
        bgTab.setClosable(false);
        bgTab.setContent(createBackgroundPanel());
        tabPane.getTabs().add(bgTab);

        root.getChildren().add(tabPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/glass-styles.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        sizeToScene();
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 0, 8, 0));
        titleBar.getStyleClass().add("glass-title-bar");

        Label title = new Label("超级调色板");
        title.getStyleClass().add("glass-label-title");
        title.setFont(Font.font("Microsoft YaHei UI", FontWeight.BOLD, 15));

        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleBar.getChildren().addAll(title, spacer, closeButton);
        return titleBar;
    }

    private VBox createSingleColorPanel() {
        HBox mainContent = new HBox(15);
        mainContent.setAlignment(Pos.TOP_CENTER);
        mainContent.setPadding(new Insets(5, 0, 0, 0));

        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(450);

        // Color preview
        VBox previewBox = new VBox(6);
        previewBox.setAlignment(Pos.CENTER);
        colorPreview.setArcWidth(12);
        colorPreview.setArcHeight(12);
        colorPreview.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.3, 0, 3);");

        previewBox.getChildren().add(colorPreview);
        leftPanel.getChildren().add(previewBox);

        leftPanel.getChildren().add(createSliderGroup("RGB 模式",
            createSliderRow("R", redSlider, redLabel, this::onRGBChange),
            createSliderRow("G", greenSlider, greenLabel, this::onRGBChange),
            createSliderRow("B", blueSlider, blueLabel, this::onRGBChange),
            createSliderRow("A", alphaSlider, alphaLabel, this::onRGBChange)
        ));

        leftPanel.getChildren().add(createSliderGroup("HSL 模式",
            createSliderRow("H", hueSlider, hueLabel, this::onHSLChange),
            createSliderRow("S", saturationSlider, saturationLabel, this::onHSLChange),
            createSliderRow("L", lightnessSlider, lightnessLabel, this::onHSLChange)
        ));

        VBox valueBox = new VBox(6);
        valueBox.getStyleClass().add("glass-panel");
        valueBox.setPadding(new Insets(10));
        Label titleLabel = new Label("颜色值");
        titleLabel.getStyleClass().add("glass-label-title");

        hexField.setEditable(false);
        rgbField.setEditable(false);
        hslField.setEditable(false);

        valueBox.getChildren().addAll(titleLabel, hexField, rgbField, hslField);
        leftPanel.getChildren().add(valueBox);

        // Right: presets and history
        VBox rightPanel = new VBox(12);
        rightPanel.setPrefWidth(210);

        rightPanel.getChildren().add(createSection("预设颜色", presetGrid));
        rightPanel.getChildren().add(createSection("最近使用", historyGrid));

        // Action buttons
        VBox actionBox = new VBox(6);
        String[] actions = {"复制HEX", "复制RGB", "复制HSL"};
        String[] formats = {"hex", "rgb", "hsl"};
        for (int i = 0; i < 3; i++) {
            Button btn = new Button(actions[i]);
            btn.getStyleClass().add("glass-button");
            btn.setPrefWidth(210);
            int idx = i;
            btn.setOnAction(e -> copyToClipboard(formats[idx]));
            actionBox.getChildren().add(btn);
        }

        Button saveBtn = new Button("保存到历史");
        saveBtn.getStyleClass().add("glass-button-primary");
        saveBtn.setPrefWidth(210);
        saveBtn.setOnAction(e -> addToHistory());
        actionBox.getChildren().add(saveBtn);

        rightPanel.getChildren().add(actionBox);
        mainContent.getChildren().addAll(leftPanel, rightPanel);

        VBox wrapper = new VBox(mainContent);
        wrapper.setPadding(new Insets(5, 0, 0, 0));
        return wrapper;
    }

    private VBox createGradientPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10));

        // Gradient preview
        gradientPreview.setArcWidth(10);
        gradientPreview.setArcHeight(10);
        gradientPreview.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0.3, 0, 3);");
        panel.getChildren().add(gradientPreview);

        // Start color (current)
        HBox startRow = new HBox(10);
        startRow.setAlignment(Pos.CENTER_LEFT);
        Label startLbl = new Label("起始色:");
        startLbl.getStyleClass().add("glass-label");
        TextField startHex = createTextField();
        startHex.setEditable(false);

        // End color picker
        HBox endRow = new HBox(10);
        endRow.setAlignment(Pos.CENTER_LEFT);
        Label endLbl = new Label("结束色:");
        endLbl.getStyleClass().add("glass-label");
        TextField endHex = createTextField();
        endHex.setEditable(false);
        endHex.setText(colorToHex(gradientEndColor));

        pickEndColorBtn.getStyleClass().add("glass-button");
        pickEndColorBtn.setOnAction(e -> pickGradientEndColor());
        endRow.getChildren().addAll(endLbl, endHex, pickEndColorBtn);

        // Angle control
        HBox angleRow = new HBox(10);
        angleRow.setAlignment(Pos.CENTER_LEFT);
        Label angleLbl = new Label("角度:");
        angleLbl.getStyleClass().add("glass-label");
        Slider angleSlider = createSlider(0, 360, 180);
        Label angleVal = new Label("180");
        angleVal.getStyleClass().add("glass-label-highlight");
        angleSlider.valueProperty().addListener((o, old, newVal) -> {
            angleVal.setText(String.valueOf(newVal.intValue()));
            updateGradientPreview(angleSlider.getValue());
        });
        angleRow.getChildren().addAll(angleLbl, angleSlider, angleVal);

        // Gradient value output
        VBox outBox = new VBox(6);
        outBox.getStyleClass().add("glass-panel");
        outBox.setPadding(new Insets(10));
        Label outTitle = new Label("渐变CSS代码");
        outTitle.getStyleClass().add("glass-label-title");
        TextField cssField = createTextField();
        cssField.setEditable(false);
        Button copyCssBtn = new Button("复制CSS");
        copyCssBtn.getStyleClass().add("glass-button");
        copyCssBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(cssField.getText());
            clipboard.setContent(content);
        });
        outBox.getChildren().addAll(outTitle, cssField, copyCssBtn);

        // Update gradient when start color changes
        Runnable updateGradient = () -> {
            startHex.setText(colorToHex(currentColor));
            updateGradientPreview(angleSlider.getValue());
            String css = generateGradientCSS(angleSlider.getValue());
            cssField.setText(css);
        };

        redSlider.valueProperty().addListener((o, a, b) -> updateGradient.run());
        greenSlider.valueProperty().addListener((o, a, b) -> updateGradient.run());
        blueSlider.valueProperty().addListener((o, a, b) -> updateGradient.run());

        updateGradient.run();

        panel.getChildren().addAll(startRow, endRow, angleRow, outBox);
        panel.getChildren().add(createSection("预设渐变", gradientGrid));

        return panel;
    }

    private VBox createBackgroundPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10));

        Label desc = new Label("选择颜色作为程序主界面背景色，支持纯色和渐变");
        desc.getStyleClass().add("glass-label");
        desc.setWrapText(true);
        panel.getChildren().add(desc);

        // Current background info
        HBox infoRow = new HBox(10);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        Label infoLbl = new Label("当前背景色:");
        infoLbl.getStyleClass().add("glass-label");
        String savedColor = prefs.get("bg_color", "#0a1628");
        TextField currentBgHex = createTextField();
        currentBgHex.setEditable(false);
        currentBgHex.setText(savedColor);
        infoRow.getChildren().addAll(infoLbl, currentBgHex);
        panel.getChildren().add(infoRow);

        // Apply current color as background
        Button applyColorBtn = new Button("应用当前颜色为背景");
        applyColorBtn.getStyleClass().add("glass-button-primary");
        applyColorBtn.setOnAction(e -> {
            String hex = colorToHex(currentColor);
            prefs.put("bg_color", hex);
            currentBgHex.setText(hex);
            applyBackgroundToApp(hex, null);
        });
        panel.getChildren().add(applyColorBtn);

        // Apply gradient as background
        Button applyGradientBtn = new Button("应用渐变（需要先在渐变调色页设置结束色）");
        applyGradientBtn.getStyleClass().add("glass-button");
        applyGradientBtn.setOnAction(e -> {
            String startHex = colorToHex(currentColor);
            String endHex = colorToHex(gradientEndColor);
            prefs.put("bg_color", startHex);
            prefs.put("bg_color_end", endHex);
            currentBgHex.setText(startHex + " -> " + endHex);
            applyBackgroundToApp(startHex, endHex);
        });
        panel.getChildren().add(applyGradientBtn);

        // Reset to default
        Button resetBtn = new Button("恢复默认背景");
        resetBtn.getStyleClass().add("glass-button-danger");
        resetBtn.setOnAction(e -> {
            prefs.put("bg_color", "#0a1628");
            prefs.remove("bg_color_end");
            currentBgHex.setText("#0a1628");
            applyBackgroundToApp("#0a1628", null);
        });
        panel.getChildren().add(resetBtn);

        // Quick apply from presets
        panel.getChildren().add(createSection("快速选择背景色", presetGrid));

        return panel;
    }

    private VBox createSection(String title, GridPane grid) {
        VBox section = new VBox(6);
        Label sectionTitle = new Label(title);
        sectionTitle.getStyleClass().add("glass-label-title");
        section.getChildren().addAll(sectionTitle, grid);
        return section;
    }

    private VBox createSliderGroup(String title, HBox... sliders) {
        VBox group = new VBox(6);
        group.getStyleClass().add("glass-panel");
        group.setPadding(new Insets(10));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("glass-label-title");
        group.getChildren().add(titleLabel);
        for (HBox slider : sliders) {
            group.getChildren().add(slider);
        }
        return group;
    }

    private HBox createSliderRow(String label, Slider slider, Label valueLabel, Runnable onChanged) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("glass-label");
        lbl.setMinWidth(18);

        slider.setPrefWidth(230);
        slider.valueProperty().addListener((obs, old, newVal) -> {
            valueLabel.setText(formatSliderValue(label, newVal.doubleValue()));
            onChanged.run();
        });

        valueLabel.getStyleClass().add("glass-label-highlight");
        valueLabel.setMinWidth(35);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(lbl, slider, valueLabel);
        return row;
    }

    private void onRGBChange() {
        if (isUpdatingFromHSL) return;

        int r = (int) Math.round(redSlider.getValue());
        int g = (int) Math.round(greenSlider.getValue());
        int b = (int) Math.round(blueSlider.getValue());
        double a = alphaSlider.getValue() / 100.0;

        currentColor = Color.rgb(r, g, b, a);
        updateColorDisplay();
        updateHSLFromRGB();
    }

    private void onHSLChange() {
        isUpdatingFromHSL = true;

        double h = hueSlider.getValue();
        double s = saturationSlider.getValue() / 100.0;
        double l = lightnessSlider.getValue() / 100.0;

        currentColor = Color.hsb(h, s, l, alphaSlider.getValue() / 100.0);
        updateColorDisplay();
        updateRGBFromHSL();

        isUpdatingFromHSL = false;
    }

    private void updateRGBFromHSL() {
        redSlider.setValue(Math.round(currentColor.getRed() * 255));
        greenSlider.setValue(Math.round(currentColor.getGreen() * 255));
        blueSlider.setValue(Math.round(currentColor.getBlue() * 255));
    }

    private void updateHSLFromRGB() {
        hueSlider.setValue(currentColor.getHue());
        saturationSlider.setValue(currentColor.getSaturation() * 100);
        lightnessSlider.setValue(currentColor.getBrightness() * 100);
    }

    private void updateColorDisplay() {
        int r = (int) Math.round(currentColor.getRed() * 255);
        int g = (int) Math.round(currentColor.getGreen() * 255);
        int b = (int) Math.round(currentColor.getBlue() * 255);

        colorPreview.setFill(currentColor);

        redLabel.setText(String.valueOf(r));
        greenLabel.setText(String.valueOf(g));
        blueLabel.setText(String.valueOf(b));
        alphaLabel.setText(String.valueOf((int) (currentColor.getOpacity() * 100)) + "%");

        hueLabel.setText(String.valueOf((int) Math.round(currentColor.getHue())));
        saturationLabel.setText(String.valueOf((int) Math.round(currentColor.getSaturation() * 100)) + "%");
        lightnessLabel.setText(String.valueOf((int) Math.round(currentColor.getBrightness() * 100)) + "%");

        hexField.setText(String.format("#%02X%02X%02X", r, g, b));
        rgbField.setText(String.format("rgb(%d, %d, %d)", r, g, b));
        hslField.setText(String.format("hsl(%d, %d%%, %d%%)",
            (int) currentColor.getHue(),
            (int) (currentColor.getSaturation() * 100),
            (int) (currentColor.getBrightness() * 100)));
    }

    private void updateGradientPreview(double angle) {
        Color startColor = currentColor;
        Color endColor = gradientEndColor;

        double startAngle = angle - 180;
        if (startAngle < 0) startAngle += 360;

        double endX = 0.5 + 0.5 * Math.cos(Math.toRadians(startAngle));
        double endY = 0.5 + 0.5 * Math.sin(Math.toRadians(startAngle));

        LinearGradient gradient = new LinearGradient(
            1 - endX, 1 - endY, endX, endY, true, CycleMethod.NO_CYCLE,
            new Stop(0, startColor),
            new Stop(1, endColor)
        );

        gradientPreview.setFill(gradient);
    }

    private String generateGradientCSS(double angle) {
        String startHex = colorToHex(currentColor);
        String endHex = colorToHex(gradientEndColor);
        return String.format("linear-gradient(%ddeg, %s 0%%, %s 100%%)", (int) angle, startHex, endHex);
    }

    private void pickGradientEndColor() {
        ColorPicker colorPicker = new ColorPicker(gradientEndColor);
        colorPicker.setOnAction(e -> {
            gradientEndColor = colorPicker.getValue();
            updateColorDisplay();
        });

        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle("选择结束色");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/glass-styles.css").toExternalForm());
        dialog.getDialogPane().setContent(colorPicker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? colorPicker.getValue() : null);
        dialog.showAndWait().ifPresent(c -> {
            gradientEndColor = c;
            updateColorDisplay();
        });
    }

    private void applyBackgroundToApp(String startHex, String endHex) {
        if (ownerStage == null || ownerStage.getScene() == null) return;

        Scene scene = ownerStage.getScene();
        StackPane root = (StackPane) scene.getRoot();

        if (endHex != null && !endHex.isEmpty()) {
            LinearGradient gradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(startHex)),
                new Stop(1, Color.web(endHex))
            );
            root.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-image: none;", startHex
            ));
            // Use inline style for gradient
            Pane bgPane = new Pane();
            bgPane.setStyle(String.format(
                "-fx-background-color: linear-gradient(to bottom, %s 0%%, %s 100%%);", startHex, endHex
            ));
            // We need to rebuild - simplest approach is to set via CSS
            String customCss = ".gradient-bg {\n" +
                "    -fx-background-color: linear-gradient(to bottom, " + startHex + " 0%, " + endHex + " 100%);\n" +
                "}";
            try {
                java.nio.file.Path cssPath = java.nio.file.Files.createTempFile("diskanalyzer-bg-", ".css");
                java.nio.file.Files.writeString(cssPath, customCss);
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/glass-styles.css").toExternalForm());
                scene.getStylesheets().add(cssPath.toUri().toString());
                root.getStyleClass().add("gradient-bg");
            } catch (Exception ignored) {}
        } else {
            // Remove gradient-bg class
            StackPane root2 = (StackPane) scene.getRoot();
            root2.getStyleClass().remove("gradient-bg");
            // Reset to glass default
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/glass-styles.css").toExternalForm());
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "背景已应用！");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/glass-styles.css").toExternalForm());
        ((Stage) alert.getDialogPane().getScene().getWindow()).initStyle(StageStyle.TRANSPARENT);
        alert.showAndWait();
    }

    private void loadPresetColors() {
        Color[] presetColors = {
            Color.web("#0a1628"), Color.web("#1a2332"), Color.web("#1a1a2e"), Color.web("#16213e"),
            Color.web("#0f3460"), Color.web("#1b1b2f"), Color.web("#162447"), Color.web("#1f4068"),
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.PURPLE,
            Color.web("#FF6B6B"), Color.web("#FFA07A"), Color.web("#FFD93D"), Color.web("#6BCB77"),
            Color.web("#4D96FF"), Color.web("#9B59B6"),
            Color.web("#2C3E50"), Color.web("#34495E"), Color.web("#7F8C8D"), Color.web("#95A5A6"),
            Color.web("#ECF0F1"), Color.web("#BDC3C7"), Color.web("#FFFFFF"), Color.web("#000000")
        };

        int col = 0, row = 0;
        for (Color color : presetColors) {
            presetGrid.add(createColorRect(color), col, row);
            col++;
            if (col >= 7) { col = 0; row++; }
        }

        for (int i = 0; i < MAX_HISTORY; i++) {
            Rectangle emptyRect = new Rectangle(22, 22);
            emptyRect.setFill(Color.rgb(40, 40, 40));
            emptyRect.setArcWidth(4);
            emptyRect.setArcHeight(4);
            emptyRect.setCursor(Cursor.DEFAULT);
            historyGrid.add(emptyRect, i % 7, i / 7);
        }
    }

    private void loadGradientPresets() {
        String[][] gradients = {
            {"#0a1628", "#1a365d"}, {"#1a1a2e", "#16213e"}, {"#0f0c29", "#302b63"},
            {"#141e30", "#243b55"}, {"#0f2027", "#2c5364"}, {"#200122", "#6f0000"},
            {"#1e3c72", "#2a5298"}, {"#0f0c29", "#24243e"}, {"#000428", "#004e92"},
            {"#1a2a6c", "#b21f1f"}, {"#0b486b", "#f56217"}, {"#360033", "#0b8793"},
            {"#4a00e0", "#8e2de2"}, {"#00b4db", "#0083b0"}, {"#c33764", "#1d2671"},
            {"#2c3e50", "#3498db"}, {"#232526", "#414345"}, {"#1f1c2c", "#928dab"}
        };

        int col = 0, row = 0;
        for (String[] g : gradients) {
            Color start = Color.web(g[0]);
            Color end = Color.web(g[1]);

            LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, start), new Stop(1, end));

            Rectangle rect = new Rectangle(60, 30);
            rect.setFill(gradient);
            rect.setArcWidth(6);
            rect.setArcHeight(6);
            rect.setCursor(Cursor.HAND);
            rect.setOnMouseClicked(e -> {
                currentColor = start;
                gradientEndColor = end;
                updateColorDisplay();
                updateRGBFromHSL();
                updateHSLFromRGB();
            });

            gradientGrid.add(rect, col, row);
            col++;
            if (col >= 3) { col = 0; row++; }
        }
    }

    private void loadHistory() {
        for (int i = 0; i < MAX_HISTORY; i++) {
            String hex = prefs.get("color_hist_" + i, null);
            if (hex != null) {
                try {
                    colorHistory.add(Color.web(hex));
                } catch (Exception ignored) {}
            }
        }
        updateHistoryGrid();
    }

    private Rectangle createColorRect(Color color) {
        Rectangle rect = new Rectangle(22, 22);
        rect.setFill(color);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        rect.setCursor(Cursor.HAND);
        rect.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0.2, 0, 1);");
        rect.setOnMouseClicked(e -> {
            currentColor = color;
            updateColorDisplay();
            updateRGBFromHSL();
            updateHSLFromRGB();
        });
        return rect;
    }

    private void addToHistory() {
        if (colorHistory.isEmpty() || !colorHistory.get(0).equals(currentColor)) {
            colorHistory.add(0, currentColor);
            if (colorHistory.size() > MAX_HISTORY) {
                colorHistory.remove(colorHistory.size() - 1);
            }
            // Persist
            for (int i = 0; i < colorHistory.size(); i++) {
                prefs.put("color_hist_" + i, colorToHex(colorHistory.get(i)));
            }
            updateHistoryGrid();
        }
    }

    private void updateHistoryGrid() {
        historyGrid.getChildren().clear();
        for (int i = 0; i < MAX_HISTORY; i++) {
            Rectangle rect;
            if (i < colorHistory.size()) {
                rect = createColorRect(colorHistory.get(i));
            } else {
                rect = new Rectangle(22, 22);
                rect.setFill(Color.rgb(40, 40, 40));
                rect.setArcWidth(4);
                rect.setArcHeight(4);
                rect.setCursor(Cursor.DEFAULT);
            }
            historyGrid.add(rect, i % 7, i / 7);
        }
    }

    private void copyToClipboard(String format) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        switch (format) {
            case "hex": content.putString(hexField.getText()); break;
            case "rgb": content.putString(rgbField.getText()); break;
            case "hsl": content.putString(hslField.getText()); break;
        }
        clipboard.setContent(content);
    }

    private String colorToHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        return slider;
    }

    private static TextField createTextField() {
        TextField field = new TextField();
        field.getStyleClass().add("glass-text-field");
        field.setEditable(false);
        return field;
    }

    private String formatSliderValue(String label, double value) {
        if (label.equals("H")) return String.valueOf((int) value);
        if (label.equals("A")) return String.valueOf((int) value) + "%";
        return String.valueOf((int) value);
    }
}
