# 部署指南

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0
- 阿里云 OSS 账号
- 通义千问 DashScope API Key

## 1. 数据库初始化

```bash
mysql -h <数据库地址> -P <端口> -u root -p < sql/init.sql
```

## 2. 配置 application.yml

编辑 `backend/src/main/resources/application.yml`，修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<数据库地址>:<端口>/wrong_note?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: <用户名>
    password: <密码>

aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: <你的 AccessKey ID>
    access-key-secret: <你的 AccessKey Secret>
    bucket-name: wrong-note
    url-prefix: https://wrong-note.oss-cn-hangzhou.aliyuncs.com/

dashscope:
  api-key: <你的通义千问 API Key>
  model: qwen-vl-plus
```

## 3. 后端启动

```bash
cd backend
mvn spring-boot:run
```

或使用 jar 包部署：

```bash
cd backend
mvn clean package -DskipTests
java -jar target/wrong-note-1.0.0.jar
```

服务默认运行在 `http://localhost:8080`。

## 4. 小程序部署

1. 下载 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 导入 `miniprogram` 目录
3. 修改 `miniprogram/app.js` 中的 `baseUrl` 为后端实际地址
4. 编译预览 / 上传

## 5. Docker Compose 部署（可选）

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: wrong_note
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql

  backend:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/wrong_note?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
      DASHSCOPE_API_KEY: <你的通义千问 API Key>
      OSS_ACCESS_KEY_ID: <你的 OSS AccessKey ID>
      OSS_ACCESS_KEY_SECRET: <你的 OSS AccessKey Secret>
    ports:
      - "8080:8080"
    depends_on:
      - mysql

volumes:
  mysql_data:
```

## 6. Nginx 反向代理（可选）

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 验证

```bash
# 检查后端是否启动
curl http://localhost:8080/api/notes

# 检查数据库连接
curl http://localhost:8080/api/stats/error-rate?days=7
```
