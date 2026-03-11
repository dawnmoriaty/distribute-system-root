@echo off
echo ============================================
echo   DISTRIBUTED SYSTEM - BRANCH: client-1
echo   VAI TRO: JavaFX Client 1
echo ============================================
echo.
echo [client-1] Khoi dong JavaFX Client 1...
echo [client-1] Client se ket noi den Load Balancer de gui request.
echo [client-1] Dam bao Load Balancer (nhanh load-balancer) da chay truoc.
echo.
call gradlew.bat :javafx-client:run

