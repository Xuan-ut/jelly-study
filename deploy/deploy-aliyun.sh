#!/bin/bash

echo "========================================="
echo "  JellyStudy 阿里云 ECS 部署脚本"
echo "========================================="

if [ -z "$1" ]; then
    echo "Usage: ./deploy-aliyun.sh <server-ip> [ssh-user]"
    echo "Example: ./deploy-aliyun.sh 47.100.xx.xx root"
    exit 1
fi

SERVER_IP=$1
SSH_USER=${2:-root}
PROJECT_DIR="/opt/jellystudy"

echo "==> 1. 在本地打包项目..."
cd "$(dirname "$0")/.."
mvn clean package -DskipTests -q
echo "    打包完成"

echo "==> 2. 上传项目到服务器..."
ssh ${SSH_USER}@${SERVER_IP} "mkdir -p ${PROJECT_DIR}/deploy/sql"
scp docker-compose-aliyun.yml ${SSH_USER}@${SERVER_IP}:${PROJECT_DIR}/docker-compose.yml
scp -r jellystudy-knowledge/target/*.jar ${SSH_USER}@${SERVER_IP}:${PROJECT_DIR}/jellystudy-knowledge/target/
scp -r jellystudy-knowledge/Dockerfile ${SSH_USER}@${SERVER_IP}:${PROJECT_DIR}/jellystudy-knowledge/
scp -r jellystudy-studyplan/target/*.jar ${SSH_USER}@${SERVER_IP}:${PROJECT_DIR}/jellystudy-studyplan/target/
scp -r jellystudy-studyplan/Dockerfile ${SSH_USER}@${SERVER_IP}:${PROJECT_DIR}/jellystudy-studyplan/
scp deploy/sql/init.sql ${SSH_USER}@${SERVER_IP}:${PROJECT_DIR}/deploy/sql/
echo "    上传完成"

echo "==> 3. 在服务器上安装 Docker（如果未安装）..."
ssh ${SSH_USER}@${SERVER_IP} << 'REMOTE_SCRIPT'
if ! command -v docker &> /dev/null; then
    echo "安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl start docker
    systemctl enable docker
    echo "Docker 安装完成"
else
    echo "Docker 已安装"
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose 安装完成"
else
    echo "Docker Compose 已安装"
fi

echo "开放防火墙端口..."
firewall-cmd --permanent --add-port=8081/tcp 2>/dev/null || true
firewall-cmd --permanent --add-port=8084/tcp 2>/dev/null || true
firewall-cmd --permanent --add-port=8091/tcp 2>/dev/null || true
firewall-cmd --permanent --add-port=8094/tcp 2>/dev/null || true
firewall-cmd --permanent --add-port=8848/tcp 2>/dev/null || true
firewall-cmd --permanent --add-port=15672/tcp 2>/dev/null || true
firewall-cmd --reload 2>/dev/null || true
REMOTE_SCRIPT

echo "==> 4. 启动服务..."
ssh ${SSH_USER}@${SERVER_IP} "cd ${PROJECT_DIR} && docker-compose up -d --build"

echo "==> 5. 等待服务启动..."
sleep 30

echo "==> 6. 初始化 Nacos 配置..."
ssh ${SSH_USER}@${SERVER_IP} << 'REMOTE_SCRIPT'
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=jellystudy-studyplan.properties&group=DEFAULT_GROUP&content=jellystudy.studyplan.knowledge-points-per-stage=5
jellystudy.studyplan.default-study-duration=45
jellystudy.studyplan.max-active-plans=10
jellystudy.studyplan.achievement-enabled=true
jellystudy.studyplan.welcome-message=Welcome to JellyStudy!"

curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=jellystudy-knowledge.properties&group=DEFAULT_GROUP&content=jellystudy.knowledge.cache-ttl=3600
jellystudy.knowledge.max-depth=5
jellystudy.knowledge.welcome-message=Hello from Nacos!"
REMOTE_SCRIPT

echo ""
echo "========================================="
echo "  部署完成！"
echo "========================================="
echo ""
echo "服务访问地址:"
echo "  知识点服务1:  http://${SERVER_IP}:8081"
echo "  知识点服务2:  http://${SERVER_IP}:8091"
echo "  学习计划服务1: http://${SERVER_IP}:8084"
echo "  学习计划服务2: http://${SERVER_IP}:8094"
echo "  Nacos控制台:  http://${SERVER_IP}:8848/nacos (nacos/nacos)"
echo "  RabbitMQ管理: http://${SERVER_IP}:15672 (guest/guest)"
echo ""
echo "常用命令:"
echo "  查看服务状态: ssh ${SSH_USER}@${SERVER_IP} 'cd ${PROJECT_DIR} && docker-compose ps'"
echo "  查看日志:     ssh ${SSH_USER}@${SERVER_IP} 'cd ${PROJECT_DIR} && docker-compose logs -f'"
echo "  重启服务:     ssh ${SSH_USER}@${SERVER_IP} 'cd ${PROJECT_DIR} && docker-compose restart'"
echo "  停止服务:     ssh ${SSH_USER}@${SERVER_IP} 'cd ${PROJECT_DIR} && docker-compose down'"
