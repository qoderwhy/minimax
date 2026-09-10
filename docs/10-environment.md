# 10 · 环境与部署

## 1. 开发环境

| 工具 | 版本 | 安装 | 验证命令 |
|---|---|---|---|
| JDK | 17 (Temurin) | https://adoptium.net/ | `java -version` |
| Maven | 3.8+ | https://maven.apache.org/ | `mvn -v` |
| Node.js | 20 LTS | https://nodejs.org/ | `node -v` |
| npm | 随 Node 20 | （随 Node 安装） | `npm -v` |
| Docker Desktop | 4.x | https://www.docker.com/ | `docker -v` |
| MySQL 客户端 | 8.0+ | （可选，用 docker 起服务即可） | `mysql -V` |
| Redis 客户端 | 7+ | （同上） | `redis-cli -v` |
| IDEA | 2023.2+ | https://www.jetbrains.com/idea/ | — |
| VSCode | 1.85+ | https://code.visualstudio.com/ | — |

## 2. 一键启动

### 2.1 启动基础服务（MySQL + Redis，可选含前后端）

> `deploy/docker-compose.yml` 实际包含 4 个服务：`mysql`、`redis`、`backend`、`frontend`。
> 只想起中间件时，可仅启动 `mysql redis`。

```bash
cd deploy
docker compose up -d              # 全量（含 backend / frontend 容器）
# 或仅中间件：
docker compose up -d mysql redis
```

等待 MySQL 就绪（约 30s）：

```bash
docker compose logs -f mysql
```

看到 `ready for connections` 即可。

### 2.2 初始化数据库

数据库 DDL / 种子 SQL 位于 `qkit-admin/src/main/resources/db/migration/`（`V1.0.0__init.sql`、`V1.0.1__seed.sql`），由 Flyway 管理：

```bash
# 方式 A：Flyway（推荐）
#   注意：application-dev.yml 中 spring.flyway.enabled=false，dev 启动【不会】自动执行；
#   需把该项改为 true，或使用 prod profile（application-prod.yml 已开启）。
#   开启后启动后端即自动执行 db/migration 下的迁移脚本。

# 方式 B：手动导入（dev 常用）
docker compose exec -T mysql mysql -uroot -proot123 qkit \
  < qkit-admin/src/main/resources/db/migration/V1.0.0__init.sql
docker compose exec -T mysql mysql -uroot -proot123 qkit \
  < qkit-admin/src/main/resources/db/migration/V1.0.1__seed.sql
```

### 2.3 启动后端

```bash
cd qkit-admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

监听端口：**8080**。

### 2.4 启动前端

```bash
cd frontend
npm install
npm run dev
```

监听端口：**5173**。

打开 http://localhost:5173 即可。

## 3. 环境隔离（profile）

`qkit-admin/src/main/resources/`：

```
application.yml          # 公共配置
application-dev.yml      # 开发（默认 profile）
application-prod.yml     # 生产
logback-spring.xml       # 日志配置
# application-local.yml  # 规划：个人本地覆盖（当前未创建；如需启用请自行新建并加入 .gitignore）
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
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:
  flyway:
    enabled: false

sa-token:
  token-name: satoken
  timeout: 86400
  active-timeout: 1800
  is-concurrent: true
  is-share: false
  token-style: uuid
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
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${SPRING_REDIS_HOST}
      port: ${SPRING_REDIS_PORT:6379}
      database: ${SPRING_REDIS_DB:0}
      password: ${SPRING_REDIS_PASSWORD:}
  flyway:
    enabled: true
    locations: classpath:db/migration

sa-token:
  token-name: satoken
  timeout: ${SA_TOKEN_TIMEOUT:86400}
  jwt-secret-key: ${SA_JWT_SECRET}

app:
  security:
    trusted-proxies: ${TRUSTED_PROXIES:172.16.0.0/12,127.0.0.1,::1}

logging:
  level:
    com.qkit: info
```

> 日志文件路径由 `logback-spring.xml` 控制（prod 为 `/var/log/qkit/app.log` 及 `app-error.log`），**不在** `application-prod.yml` 里配。
> 生产环境接口文档已关闭（`springdoc`/`knife4j` 均 `enable=false`），actuator 仅暴露 `health` 且不展示细节。

> 生产配置**全部**用环境变量，不写死。

## 4. 数据库版本化（Flyway）

### 4.1 配置

> Flyway 版本已由 `pom.xml` 锁为 **9.22.3**（见 02 §3 `<properties>`），与 MySQL 8 + Spring Boot 3.2 兼容。**禁止**升级到 10.x（License 变更且与 Boot 3.2 默认管理版本错位）。

```yaml
# 仅 application-prod.yml 开启；application-dev.yml 为 enabled: false
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

> 当前**未配置** `baseline-on-migrate` 与 `table`；历史表用 Flyway 默认名 `flyway_schema_history`。

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
# 后端打包（多模块需在仓库根执行）
mvn -B clean package -DskipTests

# 上传 jar 到服务器（qkit-admin/pom.xml 已设 finalName=qkit-admin）
scp qkit-admin/target/qkit-admin.jar user@server:/opt/qkit/

# 服务器上启动
java -jar -Dspring.profiles.active=prod \
  -DSPRING_DATASOURCE_URL=jdbc:mysql://10.0.0.1:3306/qkit \
  -DSPRING_DATASOURCE_USERNAME=qkit -DSPRING_DATASOURCE_PASSWORD=xxx \
  -DSPRING_REDIS_HOST=10.0.0.2 -DSPRING_REDIS_PASSWORD=xxx \
  -DSA_JWT_SECRET=xxx \
  /opt/qkit/qkit-admin.jar
```

> **无** Maven `prod` profile，生产由 `--spring.profiles.active=prod` 激活；后端 jar 默认**不**内嵌前端静态资源（前端由 Nginx 托管，见 5.3）。

### 5.2 Docker Compose 部署

`deploy/docker-compose.yml`（示意，实际以文件为准）：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: qkit
    ports: ["3306:3306"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  backend:
    build: { context: .., dockerfile: deploy/docker/Dockerfile.backend }
    container_name: qkit-backend
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/qkit?...
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123
      SPRING_REDIS_HOST: redis
      SA_JWT_SECRET: please-change-this-jwt-secret-32-chars-minimum
    ports: ["8080:8080"]
  frontend:
    build: { context: .., dockerfile: deploy/docker/Dockerfile.frontend }
    container_name: qkit-frontend
    ports: ["80:80"]
```

> 服务名是 `backend` / `frontend`（**不是** `qkit`）；环境变量统一 `SPRING_*` 前缀；仓库仅一个 compose 文件，**无** `docker-compose.prod.yml`。

### 5.3 Nginx 反代（前后端分离部署）

`deploy/nginx/nginx.conf`（容器内路径，静态资源 root 为 `/usr/share/nginx/html`）：

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # 前端 SPA：所有未知路径都返回 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API（compose 内服务名为 backend）
    location /admin-api/ {
        proxy_pass http://backend:8080/admin-api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> 配置**文件名**为 `deploy/nginx/nginx.conf`（**无** `qkit.conf`）。

HTTPS：用 certbot 申请 Let's Encrypt 证书，配置 443。

## 6. 监控与日志

### 6.1 健康检查

```bash
curl http://localhost:8080/actuator/health
```

> `application.yml` 仅暴露 `health,info,prometheus`；**默认不返回** `components` 细节（prod 显式 `show-details: never`）。
> 需要看到 db/redis 明细时，临时设置 `management.endpoint.health.show-details: always`。

```json
{"status":"UP"}
```

### 6.2 日志

- 开发：控制台（彩色）+ 文件 `logs/qkit.log`（ERROR 另存 `logs/qkit-error.log`，按天 + 大小滚动）
- 生产：控制台 + 文件 `/var/log/qkit/app.log`（ERROR 另存 `app-error.log`），配置见 `logback-spring.xml`
- 当前为**纯文本**格式（非 JSON）；如需接 ELK / Loki 的 JSON 日志，需自行引入 encoder

### 6.3 指标

`application.yml` 已把 `prometheus` 列入 `management.endpoints.web.exposure.include`，但**当前未引入** `micrometer-registry-prometheus` 依赖，端点实际不存在；本期**不强制**接 Prometheus，接入时补依赖即可。

## 7. CI/CD（建议）

> **规划**：当前仓库**未**包含 `.github/`，以下为建议模板。

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
      - run: mvn -B verify                      # 多模块聚合，须在仓库根执行
      - run: npm install && npm run build
        working-directory: frontend
```

## 8. 常见环境问题

### 8.1 MySQL 8 字符集

项目统一使用 `utf8mb4_unicode_ci`（DDL `V1.0.0__init.sql` 与 `deploy/docker-compose.yml` 的 `--collation-server` 一致），以保持与既有表结构口径统一，**不**改用 `utf8mb4_0900_ai_ci`。

> 如遇 MySQL 8 对 `utf8mb4_unicode_ci` 的 deprecated 警告，属提示级，可忽略；确需切换排序规则须全库迁移并同步 02 §1 口径。

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
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | `jdbc:mysql://...` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 数据库账密 | — |
| `SPRING_REDIS_HOST` / `SPRING_REDIS_PORT` / `SPRING_REDIS_DB` / `SPRING_REDIS_PASSWORD` | Redis | — |
| `SA_JWT_SECRET` | Sa-Token JWT 密钥 | 32 位随机字符串 |
| `SA_TOKEN_TIMEOUT` | token 超时秒数（可选，默认 86400） | `86400` |
| `TRUSTED_PROXIES` | 可信反向代理地址（可选） | `172.16.0.0/12,127.0.0.1,::1` |
| `SERVER_PORT` | 后端端口（Spring 标准绑定，可选） | `8080` |
| `LOGGING_LEVEL_COM_QKIT` | 日志级别 | `info` |

> 密钥类用 secret manager（Vault / AWS Secrets Manager / 阿里云 KMS），不直接 .env。

## 10. 升级路径

版本升级 checklist：

- [ ] 备份数据库
- [ ] 备份旧 jar
- [ ] 检查 Flyway 迁移文件是否兼容
- [ ] 启动新版本
- [ ] 验证关键接口：登录、列表查询、字典拉取
- [ ] 回滚预案：保留旧 jar 一周
