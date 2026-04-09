@echo off
REM ====================================================================
REM LatentSpace Explorer - Build and Run Script
REM
REM Stage C: Updated to include JavaFX module path for 3D visualization
REM and the command package for Undo/Redo support.
REM
REM JavaFX SDK location: C:\Users\amirk\javafx\javafx-sdk-23.0.2\lib
REM Required modules: javafx.controls, javafx.swing (JFXPanel bridge)
REM ====================================================================

set JAVAFX_PATH=C:\Users\amirk\javafx\javafx-sdk-23.0.2\lib

echo Compiling source code (with JavaFX)...
if not exist "bin" mkdir bin

javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.swing -d bin src/model/*.java src/math/*.java src/command/*.java src/ui/*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Running LatentSpace Explorer...
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.swing -cp bin ui.MainFrame
pause
