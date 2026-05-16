@echo off
chcp 65001 >nul
title DiskAnalyzer v3.0 安装程序

echo ========================================
echo   DiskAnalyzer v3.0 安装程序
echo   Liquid Glass Edition
echo ========================================
echo.

:: 检查Java环境
set HAS_JAVA=0
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [√] 已检测到Java环境
    java -version 2>&1 | findstr "version"
    set HAS_JAVA=1
) else (
    echo [×] 未检测到Java环境
)
echo.

:: 获取安装路径
set "DEFAULT_INSTALL=%USERPROFILE%\DiskAnalyzer"
set /p INSTALL_PATH="请输入安装路径 (默认: %DEFAULT_INSTALL%): "
if "%INSTALL_PATH%"=="" set "INSTALL_PATH=%DEFAULT_INSTALL%"

echo.
echo 安装路径: %INSTALL_PATH%
echo.

:: 创建快捷方式
set /p CREATE_SHORTCUT="是否在桌面创建快捷方式? (Y/N, 默认: Y): "
if "%CREATE_SHORTCUT%"=="" set "CREATE_SHORTCUT=Y"

set /p CREATE_STARTMENU="是否在开始菜单创建快捷方式? (Y/N, 默认: Y): "
if "%CREATE_STARTMENU%"=="" set "CREATE_STARTMENU=Y"

echo.
echo ========================================
echo   开始安装...
echo ========================================
echo.

:: 创建安装目录
if not exist "%INSTALL_PATH%" (
    mkdir "%INSTALL_PATH%"
    echo [√] 创建安装目录: %INSTALL_PATH%
) else (
    echo [√] 安装目录已存在: %INSTALL_PATH%
)

:: 复制文件
echo [→] 复制程序文件...
xcopy /E /I /Y /Q "build\release\*" "%INSTALL_PATH%" >nul
if %ERRORLEVEL% EQU 0 (
    echo [√] 文件复制完成
) else (
    echo [×] 文件复制失败
    pause
    exit /b 1
)

:: 创建桌面快捷方式
if /i "%CREATE_SHORTCUT%"=="Y" (
    echo [→] 创建桌面快捷方式...
    powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('$env:USERPROFILE\Desktop\DiskAnalyzer.lnk'); $Shortcut.TargetPath = '%INSTALL_PATH%\run.bat'; $Shortcut.WorkingDirectory = '%INSTALL_PATH%'; $Shortcut.IconLocation = '%INSTALL_PATH%\icon.ico'; $Shortcut.Description = 'DiskAnalyzer v3.0 - Liquid Glass Edition'; $Shortcut.Save()"
    if %ERRORLEVEL% EQU 0 (
        echo [√] 桌面快捷方式已创建
    ) else (
        echo [×] 桌面快捷方式创建失败
    )
)

:: 创建开始菜单快捷方式
if /i "%CREATE_STARTMENU%"=="Y" (
    echo [→] 创建开始菜单快捷方式...
    set "STARTMENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs\DiskAnalyzer"
    if not exist "%STARTMENU%" mkdir "%STARTMENU%"
    powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('$env:APPDATA\Microsoft\Windows\Start Menu\Programs\DiskAnalyzer\DiskAnalyzer.lnk'); $Shortcut.TargetPath = '%INSTALL_PATH%\run.bat'; $Shortcut.WorkingDirectory = '%INSTALL_PATH%'; $Shortcut.IconLocation = '%INSTALL_PATH%\icon.ico'; $Shortcut.Description = 'DiskAnalyzer v3.0 - Liquid Glass Edition'; $Shortcut.Save()"
    if %ERRORLEVEL% EQU 0 (
        echo [√] 开始菜单快捷方式已创建
    ) else (
        echo [×] 开始菜单快捷方式创建失败
    )
)

:: 创建卸载脚本
echo @echo off > "%INSTALL_PATH%\uninstall.bat"
echo echo ======================================== >> "%INSTALL_PATH%\uninstall.bat"
echo echo   DiskAnalyzer v3.0 卸载程序 >> "%INSTALL_PATH%\uninstall.bat"
echo echo ======================================== >> "%INSTALL_PATH%\uninstall.bat"
echo echo. >> "%INSTALL_PATH%\uninstall.bat"
echo set /p CONFIRM="确定要卸载 DiskAnalyzer 吗? (Y/N): " >> "%INSTALL_PATH%\uninstall.bat"
echo if /i not "%%CONFIRM%%"=="Y" exit /b >> "%INSTALL_PATH%\uninstall.bat"
echo echo [→] 正在卸载... >> "%INSTALL_PATH%\uninstall.bat"
echo del "%%USERPROFILE%%\Desktop\DiskAnalyzer.lnk" 2^>nul >> "%INSTALL_PATH%\uninstall.bat"
echo rmdir /s /q "%%APPDATA%%\Microsoft\Windows\Start Menu\Programs\DiskAnalyzer" 2^>nul >> "%INSTALL_PATH%\uninstall.bat"
echo echo [√] 快捷方式已删除 >> "%INSTALL_PATH%\uninstall.bat"
echo echo [→] 删除程序文件... >> "%INSTALL_PATH%\uninstall.bat"
echo pushd %%~dp0 >> "%INSTALL_PATH%\uninstall.bat"
echo cd .. >> "%INSTALL_PATH%\uninstall.bat"
echo rmdir /s /q "%%~dp0" 2^>nul >> "%INSTALL_PATH%\uninstall.bat"
echo echo [√] 卸载完成 >> "%INSTALL_PATH%\uninstall.bat"
echo pause >> "%INSTALL_PATH%\uninstall.bat"

:: 完成
echo.
echo ========================================
echo   安装完成！
echo ========================================
echo.
echo 安装位置: %INSTALL_PATH%
echo.
if /i "%CREATE_SHORTCUT%"=="Y" (
    echo 你可以从桌面快捷方式启动程序
)
if /i "%CREATE_STARTMENU%"=="Y" (
    echo 你也可以从开始菜单启动程序
)
echo.
pause
