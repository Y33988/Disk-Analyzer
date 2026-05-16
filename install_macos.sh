#!/bin/bash
# DiskAnalyzer v3.0 macOS 安装脚本
# Liquid Glass Edition

set -e

echo "========================================"
echo "  DiskAnalyzer v3.0 安装程序 (macOS)"
echo "  Liquid Glass Edition"
echo "========================================"
echo ""

# 检查Java环境
HAS_JAVA=0
if command -v java &> /dev/null; then
    echo "[✓] 已检测到Java环境"
    java -version 2>&1 | head -1
    HAS_JAVA=1
else
    echo "[×] 未检测到Java环境"
    echo "    请先安装Java: https://www.java.com/download/"
fi
echo ""

# 默认安装路径
DEFAULT_INSTALL="/Applications/DiskAnalyzer"
echo -n "请输入安装路径 (默认: $DEFAULT_INSTALL): "
read INSTALL_PATH
INSTALL_PATH=${INSTALL_PATH:-$DEFAULT_INSTALL}

echo ""
echo "安装路径: $INSTALL_PATH"
echo ""

# 创建Dock图标
echo -n "是否在Dock添加快捷方式? (Y/N, 默认: Y): "
read CREATE_DOCK
CREATE_DOCK=${CREATE_DOCK:-Y}

echo ""
echo "========================================"
echo "  开始安装..."
echo "========================================"
echo ""

# 创建安装目录
if [ ! -d "$INSTALL_PATH" ]; then
    mkdir -p "$INSTALL_PATH"
    echo "[✓] 创建安装目录: $INSTALL_PATH"
else
    echo "[✓] 安装目录已存在: $INSTALL_PATH"
fi

# 复制文件
echo "[→] 复制程序文件..."
cp -r build/release/* "$INSTALL_PATH/"
if [ $? -eq 0 ]; then
    echo "[✓] 文件复制完成"
else
    echo "[×] 文件复制失败"
    exit 1
fi

# 设置执行权限
chmod +x "$INSTALL_PATH/run.sh"
echo "[✓] 已设置执行权限"

# 创建Dock快捷方式
if [[ "$CREATE_DOCK" =~ ^[Yy]$ ]]; then
    echo "[→] 添加到Dock..."
    # 创建.app包
    APP_PATH="$INSTALL_PATH/DiskAnalyzer.app"
    mkdir -p "$APP_PATH/Contents/MacOS"
    
    cat > "$APP_PATH/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>DiskAnalyzer</string>
    <key>CFBundleDisplayName</key>
    <string>DiskAnalyzer</string>
    <key>CFBundleVersion</key>
    <string>3.0</string>
    <key>CFBundleShortVersionString</key>
    <string>3.0</string>
    <key>CFBundleExecutable</key>
    <string>run.sh</string>
    <key>CFBundleIconFile</key>
    <string>icon.icns</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleSignature</key>
    <string>????</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
EOF
    
    # 复制图标
    if [ -f "$INSTALL_PATH/icon.icns" ]; then
        cp "$INSTALL_PATH/icon.icns" "$APP_PATH/Contents/Resources/"
    fi
    
    # 创建启动脚本
    cat > "$APP_PATH/Contents/MacOS/run.sh" << EOF
#!/bin/bash
cd "$(dirname "\$0")/../.."
./run.sh
EOF
    chmod +x "$APP_PATH/Contents/MacOS/run.sh"
    
    echo "[✓] 已创建.app包: $APP_PATH"
    echo "    请手动将.app拖到Dock"
fi

# 创建卸载脚本
cat > "$INSTALL_PATH/uninstall.sh" << 'UNINSTALL_EOF'
#!/bin/bash
echo "========================================"
echo "  DiskAnalyzer v3.0 卸载程序"
echo "========================================"
echo ""
echo -n "确定要卸载 DiskAnalyzer 吗? (Y/N): "
read CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    exit 0
fi

echo "[→] 正在卸载..."

# 删除.app
rm -rf /Applications/DiskAnalyzer.app
rm -rf /Applications/DiskAnalyzer

echo "[✓] 卸载完成"
UNINSTALL_EOF

chmod +x "$INSTALL_PATH/uninstall.sh"

echo ""
echo "========================================"
echo "  安装完成！"
echo "========================================"
echo ""
echo "安装位置: $INSTALL_PATH"
echo ""
echo "启动方式:"
echo "  - 双击 $INSTALL_PATH/DiskAnalyzer.app"
echo "  - 终端运行: $INSTALL_PATH/run.sh"
echo ""
