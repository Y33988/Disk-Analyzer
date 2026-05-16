@echo off
title Disk Analyzer v3.0 - Starting...
color 0A
cls

echo.
echo ===============================================
echo     Disk Analyzer v3.0 Liquid Glass Edition
echo ===============================================
echo.

echo [1/3] Checking Java environment...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not detected!
    pause
    exit /b 1
)
echo SUCCESS: Java environment detected

echo.
echo [2/3] Setting JavaFX path...
set JAVAFX_PATH=C:\Usersdata\javafx-sdk-25.0.3
set JAVA_OPTS=-Xmx2g -Xms512m
set CLASSPATH=target\classes

if not exist "%JAVAFX_PATH%\lib" (
    echo ERROR: JavaFX not found at %JAVAFX_PATH%
    pause
    exit /b 1
)
echo SUCCESS: JavaFX path configured

echo.
echo [3/3] Compiling and launching...
echo.

mkdir target\classes 2>nul
mkdir target\classes\com 2>nul

xcopy /E /I /Q /Y src\main\resources target\classes >nul

javac -d target\classes --module-path "%JAVAFX_PATH%\lib" --add-modules javafx.controls,javafx.fxml src\main\java\com\diskanalyzer\model\*.java src\main\java\com\diskanalyzer\service\*.java src\main\java\com\diskanalyzer\ui\dialog\*.java src\main\java\com\diskanalyzer\ui\component\GlassEffectPane.java src\main\java\com\diskanalyzer\ui\effect\*.java src\main\java\com\diskanalyzer\controller\GlassMainController.java src\main\java\com\diskanalyzer\GlassDiskAnalyzerApp.java 2>compile_errors.txt

if errorlevel 1 (
    echo ERROR: Compilation failed!
    echo.
    echo Compilation errors:
    type compile_errors.txt
    echo.
    pause
    exit /b 1
)

echo Launching Disk Analyzer v3.0...
echo.

java %JAVA_OPTS% --module-path "%JAVAFX_PATH%\lib" --add-modules javafx.controls,javafx.fxml -cp "%CLASSPATH%" com.diskanalyzer.GlassDiskAnalyzerApp

echo.
echo Program closed.
pause
