#!/bin/bash
# DiskAnalyzer v3.0 Linux 安装脚本
# Liquid Glass Edition

set -e

echo "========================================"
echo "  DiskAnalyzer v3.0 安装程序 (Linux)"
echo "  Liquid Glass Edition"
echo "========================================"
echo ""

# 检查是否为root
if [ "$EUID" -ne 0 ]; then
    echo "[×] 请使用sudo运行此脚本"
    echo "    例如: sudo $0"
    exit 1
fi

# 检查Java环境
HAS_JAVA=0
if command -v java &> /dev/null; then
    echo "[✓] 已检测到Java环境"
    java -version 2>&1 | head -1
    HAS_JAVA=1
else
    echo "[×] 未检测到Java环境"
fi
echo ""

# 默认安装路径
DEFAULT_INSTALL="/opt/DiskAnalyzer"
echo -n "请输入安装路径 (默认: $DEFAULT_INSTALL): "
read INSTALL_PATH
INSTALL_PATH=${INSTALL_PATH:-$DEFAULT_INSTALL}

echo ""
echo "安装路径: $INSTALL_PATH"
echo ""

# 创建桌面快捷方式
echo -n "是否在桌面创建快捷方式? (Y/N, 默认: Y): "
read CREATE_SHORTCUT
CREATE_SHORTCUT=${CREATE_SHORTCUT:-Y}

# 创建应用菜单
echo -n "是否添加到应用菜单? (Y/N, 默认: Y): "
read CREATE_MENU
CREATE_MENU=${CREATE_MENU:-Y}

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

# 创建桌面快捷方式
if [[ "$CREATE_SHORTCUT" =~ ^[Yy]$ ]]; then
    echo "[→] 创建桌面快捷方式..."
    
    # 获取当前用户的桌面路径
    for USER_HOME in /home/*; do
        if [ -d "$USER_HOME/Desktop" ]; then
            cat > "$USER_HOME/Desktop/DiskAnalyzer.desktop" << EOF
[Desktop Entry]
Name=DiskAnalyzer
Comment=磁盘空间分析工具 v3.0
Exec=$INSTALL_PATH/run.sh
Icon=$INSTALL_PATH/icon.png
Terminal=false
Type=Application
Categories=Utility;System;
EOF
            chmod +x "$USER_HOME/Desktop/DiskAnalyzer.desktop"
            chown $(basename $USER_HOME):$(basename $USER_HOME) "$USER_HOME/Desktop/DiskAnalyzer.desktop"
            echo "[✓] 桌面快捷方式已创建: $USER_HOME/Desktop/DiskAnalyzer.desktop"
        fi
    done
fi

# 创建应用菜单
if [[ "$CREATE_MENU" =~ ^[Yy]$ ]]; then
    echo "[→] 添加到应用菜单..."
    
    cat > /usr/share/applications/DiskAnalyzer.desktop << EOF
[Desktop Entry]
Name=DiskAnalyzer
Comment=磁盘空间分析工具 v3.0
Exec=$INSTALL_PATH/run.sh
Icon=$INSTALL_PATH/icon.png
Terminal=false
Type=Application
Categories=Utility;System;FileTools;
EOF
    echo "[✓] 已添加到应用菜单"
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

# 删除桌面快捷方式
for USER_HOME in /home/*; do
    rm -f "$USER_HOME/Desktop/DiskAnalyzer.desktop"
done

# 删除应用菜单
rm -f /usr/share/applications/DiskAnalyzer.desktop

# 删除程序文件
rm -rf /opt/DiskAnalyzer

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
echo "你可以从以下位置启动程序:"
if [[ "$CREATE_SHORTCUT" =~ ^[Yy]$ ]]; then
    echo "  - 桌面快捷方式"
fi
if [[ "$CREATE_MENU" =~ ^[Yy]$ ]]; then
    echo "  - 应用菜单"
fi
echo "  - 终端运行: $INSTALL_PATH/run.sh"
echo ""
