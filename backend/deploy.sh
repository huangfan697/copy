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

echo "=== 4. 远程部署 ==="
ssh -i $SSH_KEY -o StrictHostKeyChecking=no $SERVER "cd $REMOTE_DIR && docker compose down 2>/dev/null; docker compose up -d --build"

echo "=== 部署完成 ==="
ssh -i $SSH_KEY $SERVER "docker ps | grep wrong-note-backend"
