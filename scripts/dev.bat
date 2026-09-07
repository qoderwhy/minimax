@echo off
REM Windows 开发环境启动脚本
setlocal

cd /d "%~dp0\.."

echo ============================================
echo   qkit 开发环境启动 (Windows)
echo ============================================

REM 启动后端
echo [1/3] 启动后端 Spring Boot...
start "qkit-backend" cmd /k "cd qkit-admin && mvn spring-boot:run"

REM 启动前端
echo [2/3] 启动前端 Vite...
start "qkit-frontend" cmd /k "cd frontend && npm run dev"

echo [3/3] 等待服务就绪...
timeout /t 8 /nobreak > nul
echo   -^> 后端: http://localhost:8080/admin-api
echo   -^> 前端: http://localhost:5173
echo   -^> 文档: http://localhost:8080/doc.html
echo.
echo 关闭对应 cmd 窗口即可停止服务

endlocal
