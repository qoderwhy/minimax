# 10 · 环境与部署

## 1. 开发环境

| 工具 | 版本 | 安装 | 验证命令 |
|---|---|---|---|
| JDK | 17 (Temurin) | https://adoptium.net/ | `java -version` |
| Maven | 3.8+ | https://maven.apache.org/ | `mvn -v` |
| Node.js | 20 LTS | https://nodejs.org/ | `node -v` |
| pnpm | 9+ | `npm i -g pnpm` | `pnpm -v` |
| Docker Desktop | 4.x | https://www.docker.com/ | `docker -v` |
| MySQL 客户端 | 8.0+ | （可选，用 docker 起服务即可） | `mysql -V` |
| Redis 客户端 | 7+ | （同上） | `redis-cli -v` |
| IDEA | 2023.2+ | https://www.jetbrains.com/idea/ | — |
| VSCode | 1.85+ | https://code.visualstudio.com/ | — |

## 2. 一键启动

### 2.1 启动基础服务（MySQL + Redis）

```bash
docker compose up -d
```

等待 MySQL 就绪（约 30s）：

```bash
docker compose logs -f mysql
```

看到 `ready for connections` 即可。

### 2.2 初始化数据库

首次启动会自动执行 `deploy/mysql/init/*.sql`（如果有）。**但 Flyway 才是项目级的版本化管理**，新环境建议用：

```bash
# 方式 A：项目级 Flyway（推荐，qkit-admin 启动时自动跑）
# 启动后端时自动执行

# 方式 B：手动导入（不推荐）
docker compose exec -T mysql mysql -uroot -proot123 qkit \
  < qkit-admin/src/main/resources/db/migration/V1.0.0__init.sql
```

### 2.3 启动后端

```bash
cd qkit-admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或用 Makefile：

```bash
make backend-run
```

监听端口：**8080**。

### 2.4 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

监听端口：**5173**。

打开 http://localhost:5173 即可。

## 3. 环境隔离（profile）

`qkit-admin/src/main/resources/`：

```
application.yml          # 公共配置
application-dev.yml      # 开发（默认）
application-prod.yml     # 生产
application-local.yml    # 个人（gitignore，已被 .gitignore 排除）
```

### 3.1 application.yml 公共部分

```yaml
spring:
  application:
    name: qkit
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null

server:
  port: 8080
  servlet:
    context-path: /
```

### 3.2 application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qkit?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: qkit
    password: qkit123
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:

sa-token:
  token-name: satoken
  timeout: 86400
  active-timeout: 1800
  is-concurrent: true
  is-read-cookie: false
  is-read-header: true

logging:
  level:
    com.qkit: debug
    org.springframework: warn
```

### 3.3 application-prod.yml

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PWD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DB:0}
      password: ${REDIS_PWD}

sa-token:
  token-name: satoken
  timeout: ${SA_TOKEN_TIMEOUT:86400}
  jwt-secret-key: ${SA_JWT_SECRET}

logging:
  level:
    com.qkit: info
  file:
    name: /var/log/qkit/app.log
```

> 生产配置**全部**用环境变量，不写死。

## 4. 数据库版本化（Flyway）

### 4.1 配置

> Flyway 版本已由 `pom.xml` 锁为 **9.22.3**（见 02 §3 `<properties>`），与 MySQL 8 + Spring Boot 3.2 兼容。**禁止**升级到 10.x（License 变更且与 Boot 3.2 默认管理版本错位）。

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    table: flyway_schema_history
```

### 4.2 迁移文件命名

`qkit-admin/src/main/resources/db/migration/` 下：

- `V1.0.0__init.sql` — 初始建表
- `V1.0.1__seed.sql` — 种子数据
- `V1.1.0__add_user_avatar.sql` — 后续变更

格式：`V<版本号>__<描述>.sql`，描述可中文，**双下划线**。

### 4.3 演进原则

- 已部署环境的迁移文件**禁止修改**
- 新增变更用新版本号
- 大表加字段注意 `pt-osc` / `gh-ost` 工具（本期数据量小，可直 ALTER）

## 5. 生产部署

### 5.1 单体 jar 部署

```bash
# 后端打包（包含前端静态资源）
cd qkit-admin
mvn -B clean package -Pprod

# 上传 jar 到服务器
scp target/qkit-admin-1.0.0.jar user@server:/opt/qkit/

# 服务器上启动
java -jar -Dspring.profiles.active=prod \
  -DDB_URL=jdbc:mysql://10.0.0.1:3306/qkit \
  -DDB_USER=qkit -DDB_PWD=xxx \
  -DREDIS_HOST=10.0.0.2 -DREDIS_PWD=xxx \
  -DSA_JWT_SECRET=xxx \
  /opt/qkit/qkit-admin-1.0.0.jar
```

### 5.2 Docker Compose 部署

`deploy/docker-compose.prod.yml`：

```yaml
version: "3.8"
services:
  qkit:
    image: qkit-admin:1.0.0
    container_name: qkit
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/qkit
      DB_USER: qkit
      DB_PWD: ${DB_PWD}
      REDIS_HOST: redis
      REDIS_PWD: ${REDIS_PWD}
      SA_JWT_SECRET: ${SA_JWT_SECRET}
    depends_on:
      - mysql
      - redis
  mysql:
    image: mysql:8.0
    # ...
  redis:
    image: redis:7-alpine
    # ...
```

### 5.3 Nginx 反代（前后端分离部署）

`deploy/nginx/qkit.conf`：

```nginx
server {
    listen 80;
    server_name qkit.example.com;

    # 前端静态资源
    location / {
        root /var/www/qkit/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /admin-api/ {
        proxy_pass http://127.0.0.1:8080/admin-api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket（如未来引入，预留）
    # location /ws { proxy_pass http://127.0.0.1:8080/ws; ... }
}
```

HTTPS：用 certbot 申请 Let's Encrypt 证书，配置 443。

## 6. 监控与日志

### 6.1 健康检查

```bash
curl http://localhost:8080/actuator/health
```

响应：

```json
{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

### 6.2 日志

- 开发：控制台 + 文件 `logs/qkit.log`
- 生产：JSON 格式，输出到 stdout（容器友好） + 文件 `/var/log/qkit/app.log`
- 推荐接 ELK / Loki 收集

### 6.3 指标

`/actuator/prometheus` 暴露指标（需引 `micrometer-registry-prometheus`），本期**不强制**接 Prometheus。

## 7. CI/CD（建议）

`.github/workflows/ci.yml`：

```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 17 }
      - uses: actions/setup-node@v4
        with: { node-version: 20 }
      - uses: pnpm/action-setup@v3
        with: { version: 9 }
      - run: mvn -B verify
        working-directory: qkit-admin
      - run: pnpm install && pnpm build
        working-directory: frontend
```

## 8. 常见环境问题

### 8.1 MySQL 8 字符集警告

```
[Warning] utf8mb4_unicode_ci is deprecated
```

改用 `utf8mb4_0900_ai_ci`（MySQL 8.0 推荐）：

```sql
CREATE TABLE ... DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
```

### 8.2 启动报"Public Key Retrieval is not allowed"

JDBC URL 加 `allowPublicKeyRetrieval=true`。

### 8.3 Redis 6+ ACL 报错

Redis 7 默认有 ACL，连接需 `username:password@host`。本期用 `requirepass` 模式：

```conf
requirepass yourpassword
```

### 8.4 前端跨域 401 后无限重定向

`request.ts` 拦截器在收到 401 时**只**清 token + 跳登录一次，不要再次重定向导致死循环。

## 9. 环境变量清单（生产）

| 变量 | 说明 | 示例 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | profile | `prod` |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://...` |
| `DB_USER` / `DB_PWD` | 数据库账密 | — |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PWD` | Redis | — |
| `SA_JWT_SECRET` | Sa-Token JWT 密钥 | 32 位随机字符串 |
| `SERVER_PORT` | 后端端口（可选） | 8080 |
| `LOGGING_LEVEL_COM_QKIT` | 日志级别 | info |

> 密钥类用 secret manager（Vault / AWS Secrets Manager / 阿里云 KMS），不直接 .env。

## 10. 升级路径

版本升级 checklist：

- [ ] 备份数据库
- [ ] 备份旧 jar
- [ ] 检查 Flyway 迁移文件是否兼容
- [ ] 启动新版本
- [ ] 验证关键接口：登录、列表查询、字典拉取
- [ ] 回滚预案：保留旧 jar 一周
