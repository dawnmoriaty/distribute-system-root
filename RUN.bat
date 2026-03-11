@echo off
echo ============================================
echo   DISTRIBUTED SYSTEM - BRANCH: load-balancer
echo   VAI TRO: Load Balancer + Admin Dashboard
echo ============================================
echo.
echo [load-balancer] Khoi dong Load Balancer + Admin Dashboard UI...
echo [load-balancer] LB se lang nghe ket noi TCP tu Client, route den Worker Servers.
echo [load-balancer] Admin Dashboard UI: quan ly ket noi TCP, Worker Health, ACL.
echo.
call gradlew.bat :load-balancer:run

