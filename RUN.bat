@echo off
echo ============================================
echo   DISTRIBUTED SYSTEM - SERVER (Worker %1)
echo   Nhanh: main
echo ============================================
echo.

if "%1"=="" (
    echo Usage: RUN.bat [port]
    echo   RUN.bat 9001    - Chay Worker Server 1 voi Admin Dashboard
    echo   RUN.bat 9002    - Chay Worker Server 2 voi Admin Dashboard
    echo.
    echo Dang chay Worker Server 1 (port 9001) mac dinh...
    set PORT=9001
) else (
    set PORT=%1
)

echo Dang khoi dong Server tren port %PORT% voi Admin Dashboard...
echo.
call gradlew.bat :server-node:run --args="%PORT%"

