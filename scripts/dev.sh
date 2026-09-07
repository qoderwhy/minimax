#!/usr/bin/env bash
# 开发环境启动脚本（Linux/macOS/Git Bash）
set -e

cd "$(dirname "$0")/.."

echo "============================================"
echo "  qkit 开发环境启动"
echo "============================================"

# 1. 启动后端（依赖：MySQL、Redis 已就绪）
echo "[1/3] 启动后端 Spring Boot..."
if [[ -d "qkit-admin" ]]; then
  (cd qkit-admin && mvn spring-boot:run) &
  BACKEND_PID=$!
  echo "  -> backend PID: $BACKEND_PID"
fi

# 2. 启动前端
echo "[2/3] 启动前端 Vite..."
if [[ -d "frontend" ]]; then
  (cd frontend && npm run dev) &
  FRONTEND_PID=$!
  echo "  -> frontend PID: $FRONTEND_PID"
fi

# 3. 等待
echo "[3/3] 等待服务就绪..."
sleep 8
echo "  -> 后端: http://localhost:8080/admin-api"
echo "  -> 前端: http://localhost:5173"
echo "  -> 文档: http://localhost:8080/doc.html"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 优雅退出
trap "echo 'Stopping...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM
wait
