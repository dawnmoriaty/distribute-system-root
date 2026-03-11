@echo off
echo ============================================
echo   DISTRIBUTED SYSTEM - BRANCH: client-2
echo   VAI TRO: JavaFX Client 2
echo ============================================
echo.
echo [client-2] Khoi dong JavaFX Client 2...
echo [client-2] Client se ket noi den Load Balancer de gui request.
echo [client-2] Dam bao Load Balancer (nhanh load-balancer) da chay truoc.
echo.
call gradlew.bat :javafx-client:run

