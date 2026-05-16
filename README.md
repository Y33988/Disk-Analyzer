# 磁盘空间分析器 v3.0 - Liquid Glass Edition

现代液态玻璃风格的高性能磁盘空间分析工具，支持实时扫描、智能清理、超级调色板等功能。

## 快速开始

### 方式1：直接运行
```bash
双击 run.bat
```

### 方式2：手动编译运行
```bash
javac -d target\classes --module-path "path\to\javafx\lib" --add-modules javafx.controls,javafx.fxml src\main\java\com\diskanalyzer\model\*.java src\main\java\com\diskanalyzer\service\*.java src\main\java\com\diskanalyzer\ui\dialog\*.java src\main\java\com\diskanalyzer\ui\component\GlassEffectPane.java src\main\java\com\diskanalyzer\ui\effect\*.java src\main\java\com\diskanalyzer\controller\GlassMainController.java src\main\java\com\diskanalyzer\GlassDiskAnalyzerApp.java

java --module-path "path\to\javafx\lib" --add-modules javafx.controls,javafx.fxml -cp target\classes com.diskanalyzer.GlassDiskAnalyzerApp
```

## 系统要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Windows 10+ |
| Java | JDK 25+ |
| JavaFX | JavaFX SDK 25.0.3+ |

## 核心功能

### 高性能扫描
- **NIO并行扫描**：基于 `Files.walkFileTree` 的多线程目录遍历
- **智能线程分配**：根据CPU核心数自动分配扫描线程
- **实时进度显示**：显示扫描速度、文件数和用时

### 文件管理
- **目录树导航**：双击文件夹进入，一键返回上级
- **文件详情表格**：显示名称、大小、类型、修改时间、占用比例
- **安全删除**：支持直接删除或移到回收站
- **文件属性查看**：详细文件信息展示

### 智能清理
- **临时文件清理**：系统临时目录、浏览器缓存
- **系统缓存清理**：Windows更新缓存、缩略图缓存
- **日志文件清理**：预读文件、错误报告
- **实时进度反馈**：清理进度和日志显示

### 超级调色板（灵感源自QQ）
- **单色选择**：RGB和HSL双模式调节
- **渐变调色**：支持起始色和结束色设置，实时预览渐变效果
- **预设颜色库**：30+精心设计的预设色和18种预设渐变
- **历史记录**：自动保存最近28个使用过的颜色
- **一键复制**：支持复制HEX、RGB、HSL格式
- **程序背景修改**：可直接修改程序主界面背景颜色

### 主题系统
- **液态玻璃主题**：深蓝背景配合半透明玻璃效果
- **深色模式**：纯黑色背景，蓝色高亮
- **浅色模式**：灰白色背景，明亮界面
- **自动记忆**：主题选择自动保存，下次启动自动应用

### 数据导出
- **支持格式**：TXT、CSV
- **完整信息**：包含所有文件详情和统计信息

## 项目结构

```
├── src/main/java/com/diskanalyzer/
│   ├── GlassDiskAnalyzerApp.java          # JavaFX应用入口
│   ├── controller/
│   │   └── GlassMainController.java       # 主控制器
│   ├── model/
│   │   └── EnhancedFileNode.java          # 文件节点模型
│   ├── service/
│   │   ├── EnhancedScanService.java       # 高性能扫描服务
│   │   ├── EnhancedFileManager.java       # 文件管理服务
│   │   ├── SystemCleaner.java             # 系统清理服务
│   │   └── ThemeManager.java             # 主题管理服务
│   ├── ui/
│   │   ├── dialog/
│   │   │   ├── GlassDialog.java           # 基础玻璃对话框
│   │   │   ├── SettingsDialog.java        # 设置面板
│   │   │   ├── SuperColorPaletteDialog.java # 超级调色板
│   │   │   └── SystemCleanDialog.java     # 系统清理对话框
│   │   ├── component/
│   │   │   └── GlassEffectPane.java       # 玻璃效果面板
│   │   └── effect/
│   │       └── BackdropBlurEffect.java    # 背景模糊效果
│   └── visualization/
│       └── VisualizationEngine.java       # 可视化引擎
├── src/main/resources/
│   ├── glass-main-view.fxml               # 主界面布局
│   ├── glass-styles.css                   # 液态玻璃主题
│   ├── dark-styles.css                    # 深色主题
│   └── light-styles.css                   # 浅色主题
└── run.bat                                # 一键运行脚本
```

## 更新日志

详细更新日志请查看 [CHANGELOG.md](CHANGELOG.md)

## 许可证

Copyright (c) 2026 Disk Analyzer. All rights reserved.
