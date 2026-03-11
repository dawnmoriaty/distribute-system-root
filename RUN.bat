@echo off
echo ============================================
echo   DISTRIBUTED SYSTEM - BRANCH: main
echo   VAI TRO: Worker Server + Admin Dashboard
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

echo [main] Khoi dong Worker Server tren port %PORT% voi Admin Dashboard...
echo [main] Server se lang nghe ket noi TCP tu Load Balancer hoac Client truc tiep.
echo [main] Admin Dashboard UI cho phep quan ly ket noi TCP (ACL, Whitelist, Blacklist).
echo.
call gradlew.bat :server-node:run --args="%PORT%"

