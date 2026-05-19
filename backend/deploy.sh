#!/bin/bash
set -e

SERVER="root@124.222.183.199"
REMOTE_DIR="/opt/wrong-note"
SSH_KEY="/Users/wangjuxing/.ssh/id_rsa"

echo "=== 1. Maven 打包 ==="
mvn clean package -DskipTests -q
echo "打包完成: target/wrong-note-1.0.0.jar"

echo "=== 2. 创建远程目录 ==="
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $SERVER "mkdir -p $REMOTE_DIR"

echo "=== 3. 上传文件 ==="
scp -i $SSH_KEY -o StrictHostKeyChecking=no \
  target/wrong-note-1.0.0.jar \
  Dockerfile \
  docker-compose.yml \
  $SERVER:$REMOTE_DIR/

echo "=== 4. 停止旧服务 ==="
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $SERVER "cd $REMOTE_DIR && docker compose down --timeout 30 2>/dev/null || true"
echo "等待容器完全停止..."
sleep 3

echo "=== 5. 启动新服务 ==="
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $SERVER "cd $REMOTE_DIR && docker compose up -d --build"

echo "=== 6. 等待服务启动 ==="
sleep 5

echo "=== 7. 检查服务状态 ==="
ssh -i $SSH_KEY $SERVER "docker ps | grep wrong-note-backend"

echo "=== 部署完成 ==="
