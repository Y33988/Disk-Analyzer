# DiskAnalyzer v3.0 安装指南

## 快速安装

### Windows
```
1. 下载 DiskAnalyzer-v3.0-Windows-x64.zip
2. 解压到任意位置
3. 双击 install.bat 运行安装程序
4. 按提示设置安装路径和快捷方式
```

### Linux
```bash
# 下载并解压
unzip DiskAnalyzer-v3.0-Linux-x64.zip

# 运行安装脚本（需要sudo）
sudo bash install.sh
```

### macOS
```bash
# 下载DMG文件并打开
# 或者运行安装脚本
bash install_macos.sh
```

## 安装选项

### Windows安装程序功能
- [x] 自定义安装路径
- [x] 桌面快捷方式
- [x] 开始菜单快捷方式
- [x] 一键卸载

### Linux安装程序功能
- [x] 自定义安装路径（默认 /opt/DiskAnalyzer）
- [x] 桌面快捷方式
- [x] 应用菜单集成
- [x] 一键卸载

### macOS安装程序功能
- [x] 自定义安装路径（默认 /Applications/DiskAnalyzer）
- [x] .app应用包
- [x] 一键卸载

## 手动安装

如果安装脚本不工作，可以手动安装：

1. 解压下载的文件到目标位置
2. 确保Java已安装（`java -version`）
3. Windows: 双击 `run.bat`
4. Linux/macOS: 运行 `chmod +x run.sh && ./run.sh`

## 卸载

### Windows
```
方法1: 运行安装目录下的 uninstall.bat
方法2: 删除安装目录和快捷方式
```

### Linux
```bash
sudo bash /opt/DiskAnalyzer/uninstall.sh
```

### macOS
```bash
rm -rf /Applications/DiskAnalyzer.app
rm -rf /Applications/DiskAnalyzer
```
