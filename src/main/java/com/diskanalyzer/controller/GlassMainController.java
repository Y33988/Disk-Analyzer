package com.diskanalyzer.controller;

import com.diskanalyzer.model.EnhancedFileNode;
import com.diskanalyzer.service.EnhancedFileManager;
import com.diskanalyzer.service.EnhancedScanService;
import com.diskanalyzer.service.ThemeManager;
import com.diskanalyzer.ui.dialog.SuperColorPaletteDialog;
import com.diskanalyzer.ui.dialog.SystemCleanDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GlassMainController {
    
    @FXML private VBox mainContainer;
    @FXML private BorderPane glassBorderPane;
    @FXML private HBox topTitleBar;
    
    @FXML private Button scanButton;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;
    @FXML private Button recycleButton;
    @FXML private Button exportButton;
    @FXML private Button searchButton;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private Button settingsButton;
    @FXML private Button advancedFeaturesButton;
    @FXML private Button themeButton;
    
    @FXML private TextField pathField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private CheckBox showHiddenCheckBox;
    @FXML private ComboBox<String> themeComboBox;
    
    @FXML private Label statusLabel;
    @FXML private Label totalSizeLabel;
    @FXML private ProgressBar progressBar;
    
    @FXML private TreeView<EnhancedFileNode> fileTreeView;
    @FXML private TableView<FileRow> fileTableView;
    @FXML private Pane visualizationPane;
    
    @FXML private SplitPane mainSplitPane;
    
    private EnhancedScanService scanService;
    private EnhancedFileManager fileManager;
    private ThemeManager themeManager;
    
    private EnhancedFileNode currentRoot;
    private EnhancedFileNode originalRoot;
    
    private ObservableList<FileRow> fileTableData = FXCollections.observableArrayList();
    
    private final DecimalFormat sizeFormat = new DecimalFormat("#.##");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private List<File> scanHistory = new ArrayList<>();
    
    @FXML
    public void initialize() {
        scanService = new EnhancedScanService();
        fileManager = new EnhancedFileManager();
        themeManager = new ThemeManager();
        
        applyCurrentTheme();
        setupFonts();
        setupThemeSelector();
        setupTableColumns();
        setupEventHandlers();
        setupDragSupport();
    }
    
    private void setupFonts() {
        // Font is set via CSS stylesheets
    }
    
    private void setupThemeSelector() {
        if (themeComboBox != null) {
            themeComboBox.getItems().clear();
            for (ThemeManager.Theme theme : themeManager.getAllThemes()) {
                themeComboBox.getItems().add(theme.getDisplayName());
            }
            
            themeComboBox.setValue(themeManager.getCurrentTheme().getDisplayName());
            
            themeComboBox.setOnAction(e -> {
                String selected = themeComboBox.getValue();
                for (ThemeManager.Theme theme : themeManager.getAllThemes()) {
                    if (theme.getDisplayName().equals(selected)) {
                        themeManager.setTheme(theme);
                        applyCurrentTheme();
                        break;
                    }
                }
            });
        }
    }
    
    private void applyCurrentTheme() {
        Scene scene = mainContainer != null ? mainContainer.getScene() : null;
        if (scene == null) return;
        
        scene.getStylesheets().clear();
        String cssPath = getClass().getResource(themeManager.getCurrentTheme().getCssPath()).toExternalForm();
        scene.getStylesheets().add(cssPath);
    }
    
    public void applyThemeAfterShow() {
        Scene scene = mainContainer != null ? mainContainer.getScene() : null;
        if (scene == null) return;
        
        scene.getStylesheets().clear();
        String cssPath = getClass().getResource(themeManager.getCurrentTheme().getCssPath()).toExternalForm();
        scene.getStylesheets().add(cssPath);
        
        // Force CSS refresh on all visible nodes
        Platform.runLater(() -> {
            applyStylesRecursively(mainContainer);
        });
    }
    
    private void applyStylesRecursively(javafx.scene.Node node) {
        if (node == null) return;
        node.applyCss();
        if (node instanceof Parent) {
            for (javafx.scene.Node child : ((Parent) node).getChildrenUnmodifiable()) {
                applyStylesRecursively(child);
            }
        }
        // Apply to context menu if any
        if (node instanceof Control) {
            ContextMenu menu = ((Control) node).getContextMenu();
            if (menu != null) {
                for (javafx.scene.control.MenuItem item : menu.getItems()) {
                    node.applyCss();
                }
            }
        }
    }
    
    private void setupTableColumns() {
        TableColumn<FileRow, String> nameCol = new TableColumn<>("名称");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(300);
        
        TableColumn<FileRow, String> sizeCol = new TableColumn<>("大小");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        sizeCol.setPrefWidth(100);
        
        TableColumn<FileRow, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);
        
        TableColumn<FileRow, String> modifiedCol = new TableColumn<>("修改时间");
        modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modifiedTime"));
        modifiedCol.setPrefWidth(120);
        
        TableColumn<FileRow, String> percentCol = new TableColumn<>("占用比例");
        percentCol.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        percentCol.setPrefWidth(100);
        
        fileTableView.getColumns().setAll(nameCol, sizeCol, typeCol, modifiedCol, percentCol);
        fileTableView.setItems(fileTableData);
    }
    
    private void setupEventHandlers() {
        scanButton.setOnAction(e -> selectAndScanDirectory());
        cancelButton.setOnAction(e -> cancelScan());
        deleteButton.setOnAction(e -> deleteSelectedFiles(false));
        recycleButton.setOnAction(e -> deleteSelectedFiles(true));
        exportButton.setOnAction(e -> exportReport());
        searchButton.setOnAction(e -> performSearch());
        
        if (settingsButton != null) {
            settingsButton.setOnAction(e -> showSettingsDialog());
        }
        
        if (advancedFeaturesButton != null) {
            advancedFeaturesButton.setOnAction(e -> showAdvancedFeaturesDialog());
        }
        
        pathField.setOnAction(e -> scanFromPath());
        searchField.setOnAction(e -> performSearch());
        
        sortComboBox.setOnAction(e -> sortTable());
        showHiddenCheckBox.setOnAction(e -> rescanCurrentDirectory());
        
        fileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleTreeSelection(newVal.getValue());
            }
        });
        
        fileTableView.setRowFactory(tv -> {
            TableRow<FileRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    FileRow rowData = row.getItem();
                    if ("文件夹".equals(rowData.getType())) {
                        navigateToSubDirectory(rowData.getName());
                    } else {
                        openSelectedFile();
                    }
                }
            });
            return row;
        });
        
        fileTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                statusLabel.setText("已选择: " + newVal.getName() + " (" + newVal.getType() + ", " + newVal.getFormattedSize() + ")");
                updateButtonStates();
            }
        });
        
        ContextMenu contextMenu = createContextMenu();
        fileTableView.setContextMenu(contextMenu);
        
        fileTreeView.setContextMenu(createTreeContextMenu());
        
        minimizeButton.setOnAction(e -> {
            Stage stage = (Stage) minimizeButton.getScene().getWindow();
            stage.setIconified(true);
        });
        
        closeButton.setOnAction(e -> {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            cleanup();
            stage.close();
        });
        
        scanService.setProgressListener(new EnhancedScanService.ScanProgressListener() {
            @Override
            public void onProgressUpdate(String message) {
                Platform.runLater(() -> {
                    statusLabel.setText(message);
                    progressBar.setProgress(-1);
                });
            }
            
            @Override
            public void onScanComplete(EnhancedFileNode rootNode) {
                Platform.runLater(() -> handleScanComplete(rootNode));
            }
            
            @Override
            public void onScanFailed(String error) {
                Platform.runLater(() -> handleScanFailed(error));
            }
        });
        
        setupDragSupport();
    }
    
    private void setupDragSupport() {
        double[] xOffset = new double[1];
        double[] yOffset = new double[1];
        
        topTitleBar.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        
        topTitleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });
    }
    
    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("glass-context-menu");
        
        MenuItem openItem = new MenuItem("打开文件");
        openItem.setOnAction(e -> openSelectedFile());
        
        MenuItem deleteItem = new MenuItem("删除文件");
        deleteItem.setOnAction(e -> deleteSelectedFiles(false));
        
        MenuItem recycleItem = new MenuItem("移到回收站");
        recycleItem.setOnAction(e -> deleteSelectedFiles(true));
        
        MenuItem propertiesItem = new MenuItem("属性");
        propertiesItem.setOnAction(e -> showFileProperties());
        
        menu.getItems().addAll(openItem, new SeparatorMenuItem(), deleteItem, recycleItem, new SeparatorMenuItem(), propertiesItem);
        return menu;
    }
    
    private ContextMenu createTreeContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("glass-context-menu");
        
        MenuItem expandAll = new MenuItem("展开全部");
        expandAll.setOnAction(e -> expandAllTreeNodes(fileTreeView.getRoot(), true));
        
        MenuItem collapseAll = new MenuItem("收起全部");
        collapseAll.setOnAction(e -> expandAllTreeNodes(fileTreeView.getRoot(), false));
        
        menu.getItems().addAll(expandAll, collapseAll);
        return menu;
    }
    
    private void expandAllTreeNodes(javafx.scene.control.TreeItem<EnhancedFileNode> item, boolean expand) {
        if (item == null) return;
        item.setExpanded(expand);
        for (javafx.scene.control.TreeItem<EnhancedFileNode> child : item.getChildren()) {
            expandAllTreeNodes(child, expand);
        }
    }
    
    private void selectAndScanDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择要分析的目录");
        
        String currentPath = pathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists()) {
                chooser.setInitialDirectory(currentDir);
            }
        }
        
        File selectedDir = chooser.showDialog(mainContainer.getScene().getWindow());
        if (selectedDir != null) {
            pathField.setText(selectedDir.getAbsolutePath());
            addToScanHistory(selectedDir);
            startScan(selectedDir);
        }
    }
    
    private void addToScanHistory(File directory) {
        if (!scanHistory.contains(directory)) {
            scanHistory.add(directory);
            if (scanHistory.size() > 10) {
                scanHistory.remove(0);
            }
        }
    }
    
    private void startScan(File directory) {
        scanButton.setDisable(true);
        cancelButton.setDisable(false);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        
        if (currentRoot != null) {
            currentRoot = null;
            updateTableView(null);
            visualizationPane.getChildren().clear();
        }
        
        CompletableFuture.runAsync(() -> scanService.startScan(directory));
    }
    
    private void cancelScan() {
        scanService.cancelScan();
        resetScanUI();
        statusLabel.setText("扫描已取消");
    }
    
    private void handleScanComplete(EnhancedFileNode rootNode) {
        currentRoot = rootNode;
        originalRoot = currentRoot;
        
        updateTreeView(currentRoot);
        updateTableView(currentRoot);
        updateVisualization();
        updateTotalSize();
        resetScanUI();
        
        statusLabel.setText("扫描完成 - 共扫描 " + scanService.getScannedFilesCount() + " 个文件");
    }
    
    private void handleScanFailed(String error) {
        resetScanUI();
        statusLabel.setText("扫描失败: " + error);
        showAlert(Alert.AlertType.ERROR, "扫描失败", error);
    }
    
    private void resetScanUI() {
        scanButton.setDisable(false);
        cancelButton.setDisable(true);
        progressBar.setVisible(false);
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean hasData = currentRoot != null;
        deleteButton.setDisable(!hasData);
        recycleButton.setDisable(!hasData);
    }
    
    private void updateTotalSize() {
        if (currentRoot != null) {
            totalSizeLabel.setText("总大小: " + formatSize(currentRoot.getTotalSize()));
        } else {
            totalSizeLabel.setText("总大小: 0 B");
        }
    }
    
    private void updateTreeView(EnhancedFileNode rootNode) {
        if (rootNode == null) {
            fileTreeView.setRoot(null);
            return;
        }
        
        javafx.scene.control.TreeItem<EnhancedFileNode> rootItem = createTreeItem(rootNode);
        fileTreeView.setRoot(rootItem);
        rootItem.setExpanded(true);
    }
    
    private javafx.scene.control.TreeItem<EnhancedFileNode> createTreeItem(EnhancedFileNode node) {
        javafx.scene.control.TreeItem<EnhancedFileNode> item = new javafx.scene.control.TreeItem<>(node);
        
        for (EnhancedFileNode child : node.getChildren()) {
            if (child.isDirectory()) {
                item.getChildren().add(createTreeItem(child));
            }
        }
        
        return item;
    }
    
    private void updateTableView(EnhancedFileNode parentNode) {
        fileTableData.clear();
        
        if (parentNode == null) {
            updateButtonStates();
            return;
        }
        
        long parentSize = parentNode.getTotalSize();
        
        for (EnhancedFileNode child : parentNode.getChildren()) {
            long childSize = child.getTotalSize();
            double percentage = 0;
            
            if (parentSize > 0 && childSize > 0) {
                percentage = (double) childSize / parentSize * 100;
                percentage = Math.min(percentage, 100.0);
            }
            
            fileTableData.add(new FileRow(
                child.getName(),
                child.getFormattedSize(),
                child.getFileType().getDisplayName(),
                dateFormat.format(new Date(child.getLastModified())),
                String.format("%.1f%%", percentage),
                child
            ));
        }
        
        updateButtonStates();
    }
    
    private void updateVisualization() {
        visualizationPane.getChildren().clear();
        
        if (currentRoot == null) {
            Label emptyLabel = new Label("请选择目录进行扫描");
            emptyLabel.getStyleClass().add("glass-label");
            visualizationPane.getChildren().add(emptyLabel);
            StackPane.setAlignment(emptyLabel, javafx.geometry.Pos.CENTER);
            return;
        }
        
        drawVisualization();
    }
    
    private void drawVisualization() {
        if (currentRoot == null || currentRoot.getChildren().isEmpty()) {
            return;
        }
        
        double width = visualizationPane.getWidth();
        double height = visualizationPane.getHeight();
        
        if (width <= 0 || height <= 0) {
            return;
        }
        
        visualizationPane.getChildren().clear();
        
        List<EnhancedFileNode> sortedChildren = new ArrayList<>(currentRoot.getChildren());
        sortedChildren.sort((a, b) -> Long.compare(b.getTotalSize(), a.getTotalSize()));
        
        long totalSize = currentRoot.getTotalSize();
        double currentX = 10;
        double maxWidth = width - 20;
        double barHeight = 25;
        double spacing = 3;
        
        int maxBars = (int) ((height - 20) / (barHeight + spacing));
        int displayCount = Math.min(sortedChildren.size(), maxBars);
        
        for (int i = 0; i < displayCount; i++) {
            EnhancedFileNode child = sortedChildren.get(i);
            double childSize = child.getTotalSize();
            double ratio = totalSize > 0 ? (double) childSize / totalSize : 0;
            double barWidth = Math.max(20, maxWidth * ratio);
            
            Rectangle bar = new Rectangle(currentX, 10 + i * (barHeight + spacing), barWidth, barHeight);
            
            Color color = getNodeColor(i);
            bar.setFill(color);
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            
            Tooltip tooltip = new Tooltip(child.getName() + "\n" + child.getFormattedSize() + " (" + String.format("%.1f%%", ratio * 100) + ")");
            tooltip.getStyleClass().add("glass-tooltip");
            Tooltip.install(bar, tooltip);
            
            final EnhancedFileNode nodeRef = child;
            bar.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && nodeRef.isDirectory()) {
                    navigateToDirectory(nodeRef);
                }
            });
            
            if (barWidth > 50) {
                Label label = new Label(child.getName());
                label.getStyleClass().add("glass-label");
                label.setStyle("-fx-font-size: 11px;");
                label.setPrefWidth(barWidth - 10);
                label.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                label.setLayoutX(bar.getX() + 5);
                label.setLayoutY(bar.getY() + 5);
                
                visualizationPane.getChildren().add(label);
            }
            
            visualizationPane.getChildren().add(bar);
        }
    }
    
    private Color getNodeColor(int index) {
        Color[] colors = {
            Color.web("#2196F3"),
            Color.web("#4CAF50"),
            Color.web("#FF9800"),
            Color.web("#E91E63"),
            Color.web("#9C27B0"),
            Color.web("#00BCD4"),
            Color.web("#FF5722"),
            Color.web("#607D8B"),
            Color.web("#795548"),
            Color.web("#8BC34A")
        };
        return colors[index % colors.length];
    }
    
    private void handleTreeSelection(EnhancedFileNode fileNode) {
        if (fileNode != null) {
            currentRoot = fileNode;
            updateTableView(fileNode);
            pathField.setText(fileNode.getAbsolutePath());
            updateVisualization();
            updateTotalSize();
        }
    }
    
    private void navigateToSubDirectory(String dirName) {
        if (currentRoot == null) return;
        
        for (EnhancedFileNode child : currentRoot.getChildren()) {
            if (child.isDirectory() && child.getName().equals(dirName)) {
                navigateToDirectory(child);
                break;
            }
        }
    }
    
    private void navigateToDirectory(EnhancedFileNode directory) {
        currentRoot = directory;
        updateTableView(directory);
        pathField.setText(directory.getAbsolutePath());
        updateVisualization();
        updateTotalSize();
    }
    
    private void performSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty() || originalRoot == null) {
            if (originalRoot != null) {
                currentRoot = originalRoot;
                updateTableView(currentRoot);
            }
            return;
        }
        
        EnhancedFileNode searchResult = searchInNode(originalRoot, searchText);
        if (searchResult != null && !searchResult.getChildren().isEmpty()) {
            currentRoot = searchResult;
            updateTableView(currentRoot);
            statusLabel.setText("搜索完成，找到 " + searchResult.getChildren().size() + " 个结果");
        } else {
            statusLabel.setText("未找到匹配的文件");
            currentRoot = new EnhancedFileNode(new File("搜索结果"));
            updateTableView(currentRoot);
        }
    }
    
    private EnhancedFileNode searchInNode(EnhancedFileNode node, String searchText) {
        EnhancedFileNode result = new EnhancedFileNode(new File("搜索结果"));
        
        for (EnhancedFileNode child : node.getChildren()) {
            if (child.getName().toLowerCase().contains(searchText)) {
                result.addChild(child);
            }
            
            if (child.isDirectory()) {
                EnhancedFileNode subResult = searchInNode(child, searchText);
                if (subResult != null && !subResult.getChildren().isEmpty()) {
                    for (EnhancedFileNode subChild : subResult.getChildren()) {
                        result.addChild(subChild);
                    }
                }
            }
        }
        
        return result.getChildren().isEmpty() ? null : result;
    }
    
    private void sortTable() {
        if (currentRoot == null) return;
        
        String sortOption = sortComboBox.getValue();
        switch (sortOption) {
            case "按大小排序":
                currentRoot.sortChildrenBySize();
                break;
            case "按名称排序":
                currentRoot.sortChildrenByName();
                break;
            case "按类型排序":
                currentRoot.sortChildrenByType();
                break;
            case "按修改时间排序":
                currentRoot.sortChildrenByModifiedTime();
                break;
        }
        
        updateTableView(currentRoot);
        updateVisualization();
    }
    
    private void scanFromPath() {
        String path = pathField.getText().trim();
        if (!path.isEmpty()) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                startScan(dir);
            } else {
                showAlert(Alert.AlertType.ERROR, "错误", "路径不存在或不是目录");
            }
        }
    }
    
    private void rescanCurrentDirectory() {
        if (currentRoot != null) {
            File dir = new File(currentRoot.getAbsolutePath());
            if (dir.exists()) {
                startScan(dir);
            }
        }
    }
    
    private void deleteSelectedFiles(boolean toRecycleBin) {
        FileRow selectedRow = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "请先选择要删除的文件");
            return;
        }
        
        String action = toRecycleBin ? "移到回收站" : "删除";
        Alert confirm = createGlassAlert(Alert.AlertType.CONFIRMATION, "确认" + action, 
            "确定要将文件 " + selectedRow.getName() + " " + action + "吗？");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (currentRoot != null) {
                for (EnhancedFileNode child : currentRoot.getChildren()) {
                    if (child.getName().equals(selectedRow.getName())) {
                        File file = new File(child.getAbsolutePath());
                        fileManager.setOperationListener(new EnhancedFileManager.FileOperationListener() {
                            @Override
                            public void onFileDeleted(File file) {}
                            @Override
                            public void onFileMovedToRecycleBin(File file, EnhancedFileManager.RecycleBinEntry entry) {}
                            @Override
                            public void onFileRestored(File file, EnhancedFileManager.RecycleBinEntry entry) {}
                            @Override
                            public void onFileOpened(File file) {}
                            @Override
                            public void onFileMoved(File source, File target) {}
                            @Override
                            public void onDirectoryCreated(File directory) {}
                            @Override
                            public void onOperationFailed(String message) {
                                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "操作失败", message));
                            }
                            @Override
                            public void onBatchOperationFailed(String message, List<String> failedFiles) {}
                        });
                        
                        boolean success = toRecycleBin ? fileManager.moveFilesToRecycleBin(Collections.singletonList(file)) 
                                                       : fileManager.deleteFiles(Collections.singletonList(file));
                        
                        if (success) {
                            statusLabel.setText("文件" + action + "完成");
                            rescanCurrentDirectory();
                        }
                        
                        fileManager.setOperationListener(null);
                        break;
                    }
                }
            }
        }
    }
    
    private void openSelectedFile() {
        FileRow selectedRow = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedRow != null && currentRoot != null) {
            for (EnhancedFileNode child : currentRoot.getChildren()) {
                if (child.getName().equals(selectedRow.getName())) {
                    File file = new File(child.getAbsolutePath());
                    if (fileManager.openFile(file)) {
                        statusLabel.setText("已打开文件: " + selectedRow.getName());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "打开失败", "无法打开文件: " + selectedRow.getName());
                    }
                    break;
                }
            }
        }
    }
    
    private void showFileProperties() {
        FileRow selectedRow = fileTableView.getSelectionModel().getSelectedItem();
        if (selectedRow != null && currentRoot != null) {
            for (EnhancedFileNode child : currentRoot.getChildren()) {
                if (child.getName().equals(selectedRow.getName())) {
                    EnhancedFileManager.FileInfo info = fileManager.getFileInfo(new File(child.getAbsolutePath()));
                    if (info != null) {
                        showFilePropertiesDialog(info);
                    }
                    break;
                }
            }
        }
    }
    
    private void showFilePropertiesDialog(EnhancedFileManager.FileInfo info) {
        String properties = String.format(
            "文件属性\n\n" +
            "名称: %s\n" +
            "路径: %s\n" +
            "大小: %s\n" +
            "类型: %s\n" +
            "修改时间: %s\n" +
            "隐藏: %s\n" +
            "可读: %s\n" +
            "可写: %s\n" +
            "可执行: %s",
            info.name, info.path, formatSize(info.size),
            info.isDirectory ? "文件夹" : "文件",
            dateFormat.format(new Date(info.lastModified)),
            info.isHidden ? "是" : "否",
            info.canRead ? "是" : "否",
            info.canWrite ? "是" : "否",
            info.canExecute ? "是" : "否"
        );
        
        showAlert(Alert.AlertType.INFORMATION, "文件属性", properties);
    }
    
    private void exportReport() {
        if (currentRoot == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "请先扫描一个目录");
            return;
        }
        
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出扫描报告");
        chooser.setInitialFileName("磁盘空间分析报告_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt");
        
        File file = chooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            if (exportReportToFile(file)) {
                statusLabel.setText("报告已导出到: " + file.getName());
                showAlert(Alert.AlertType.INFORMATION, "成功", "报告导出成功！");
            } else {
                showAlert(Alert.AlertType.ERROR, "错误", "报告导出失败");
            }
        }
    }
    
    private boolean exportReportToFile(File file) {
        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter(file))) {
            writer.println("磁盘空间分析报告");
            writer.println("生成时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("扫描路径: " + currentRoot.getAbsolutePath());
            writer.println("总大小: " + formatSize(currentRoot.getTotalSize()));
            writer.println("文件数量: " + scanService.getScannedFilesCount());
            
            EnhancedFileManager.DiskSpaceInfo diskInfo = fileManager.getDiskSpaceInfo(new File(currentRoot.getAbsolutePath()));
            if (diskInfo != null) {
                writer.println("磁盘总空间: " + diskInfo.getFormattedTotalSpace());
                writer.println("磁盘已用空间: " + diskInfo.getFormattedUsedSpace());
                writer.println("磁盘可用空间: " + diskInfo.getFormattedFreeSpace());
                writer.println("磁盘使用率: " + String.format("%.1f%%", diskInfo.getUsagePercentage()));
            }
            
            writer.println("=".repeat(50));
            writer.println();
            writer.println("文件详情:");
            exportNodeToReport(writer, currentRoot, 0);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void exportNodeToReport(PrintWriter writer, EnhancedFileNode node, int depth) {
        String indent = "  ".repeat(depth);
        writer.printf("%s%s [%s] %s\n", indent, node.getName(), node.getFileType().getDisplayName(), node.getFormattedSize());
        
        for (EnhancedFileNode child : node.getChildren()) {
            exportNodeToReport(writer, child, depth + 1);
        }
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return sizeFormat.format(size / 1024.0) + " KB";
        if (size < 1024 * 1024 * 1024) return sizeFormat.format(size / (1024.0 * 1024)) + " MB";
        return sizeFormat.format(size / (1024.0 * 1024 * 1024)) + " GB";
    }
    
    private Alert createGlassAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        
        String cssPath = getClass().getResource("/glass-styles.css").toExternalForm();
        alert.getDialogPane().getStylesheets().clear();
        alert.getDialogPane().getStylesheets().add(cssPath);
        
        return alert;
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = createGlassAlert(type, title, content);
        alert.showAndWait();
    }
    
    private void showSettingsDialog() {
        com.diskanalyzer.ui.dialog.SettingsDialog dialog = new com.diskanalyzer.ui.dialog.SettingsDialog((Stage) mainContainer.getScene().getWindow());
        dialog.showAndWait();
    }
    
    private void showAdvancedFeaturesDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("高级功能");
        alert.setHeaderText("选择高级功能");
        alert.setContentText("请选择要使用的功能：");
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/glass-styles.css").toExternalForm()
        );
        
        ButtonType systemClean = new ButtonType("系统清理");
        ButtonType colorPalette = new ButtonType("超级调色板");
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(systemClean, colorPalette, cancel);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == systemClean) {
                new SystemCleanDialog((Stage) mainContainer.getScene().getWindow()).showAndWait();
            } else if (result.get() == colorPalette) {
                new SuperColorPaletteDialog((Stage) mainContainer.getScene().getWindow()).show();
            }
        }
    }
    
    private void cleanup() {
        try {
            if (scanService != null) {
                scanService.shutdown();
            }
            System.out.println("UI控制器资源清理完成");
        } catch (Exception e) {
            System.err.println("资源清理失败: " + e.getMessage());
        }
    }
    
    public static class FileRow {
        private final String name;
        private final String formattedSize;
        private final String type;
        private final String modifiedTime;
        private final String percentage;
        private final EnhancedFileNode node;
        
        public FileRow(String name, String formattedSize, String type, String modifiedTime, String percentage, EnhancedFileNode node) {
            this.name = name;
            this.formattedSize = formattedSize;
            this.type = type;
            this.modifiedTime = modifiedTime;
            this.percentage = percentage;
            this.node = node;
        }
        
        public String getName() { return name; }
        public String getFormattedSize() { return formattedSize; }
        public String getType() { return type; }
        public String getModifiedTime() { return modifiedTime; }
        public String getPercentage() { return percentage; }
        public EnhancedFileNode getNode() { return node; }
    }
}
