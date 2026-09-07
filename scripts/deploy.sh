#!/usr/bin/env bash
# 一键部署（生产环境）
set -e

cd "$(dirname "$0")/.."

echo "============================================"
echo "  qkit 生产部署"
echo "============================================"

# 1. 构建后端
echo "[1/3] 构建后端..."
mvn -B -DskipTests clean package

# 2. 构建前端
echo "[2/3] 构建前端..."
(cd frontend && npm install && npm run build)

# 3. docker compose up
echo "[3/3] 启动容器..."
cd deploy
docker compose down || true
docker compose build
docker compose up -d

echo "============================================"
echo "  部署完成"
echo "  - 前端: http://localhost"
echo "  - 后端: http://localhost:8080/admin-api"
echo "============================================"
