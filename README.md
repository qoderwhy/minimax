# qkit

> 低代码风格 OA/CRM/ERP 后台脚手架，基于 Spring Boot 3.2 + Vue 3 + Sa-Token + MyBatis-Plus。

## 一、技术栈

### 后端
- Spring Boot 3.2.12（Java 17）
- MyBatis-Plus 3.5.7（逻辑删除 + 分页 + 乐观锁）
- Sa-Token 1.39.0（JWT 模式 + RBAC）
- Spring Data Redis（Lettuce + Jackson）
- Flyway 9.22.3（数据库版本管理）
- MapStruct 1.5.5.Final（DTO/VO 转换）
- knife4j 4.5.0 + springdoc-openapi 2.5.0（接口文档）
- Hutool 5.8.32（工具集）
- easy-captcha 1.6.2（图形验证码）
- BCrypt（密码加密）

### 前端
- Vue 3.4（Composition API + `<script setup lang="ts">`）
- Vite 5.4 + TypeScript 5.4（strict）
- Element Plus 2.8（自动按需引入 + zhCn）
- Pinia 2.1（用户/权限/字典）
- Vue Router 4.3（动态路由 + Hash 模式）
- Axios（统一拦截 + token 注入）
- nprogress（加载进度条）

## 二、模块结构

```
qkit/
├── pom.xml                       根 POM（依赖管理）
├── qkit-common/                  公共模块（实体基类、异常、工具）
│   ├── api/                      R、ErrorCode
│   ├── exception/                业务异常 + 全局处理
│   ├── entity/                   BaseEntity
│   └── util/                     IdGenerator、AssertUtil
├── qkit-framework/               框架模块（中间件、配置）
│   ├── mybatis/                  MyBatis-Plus 配置
│   ├── redis/                    Redis 配置
│   ├── web/                      CORS、WebMvc
│   ├── security/                 Sa-Token 配置
│   ├── log/                      @OperLog + AOP
│   ├── captcha/                  验证码工具
│   └── ratelimit/                登录限流
├── qkit-system/                  业务模块（9 个）
│   ├── auth/                     登录、注销、Me、权限
│   ├── user/                     用户 CRUD + 重置密码
│   ├── role/                     角色 CRUD + 菜单授权
│   ├── dept/                     部门树 CRUD
│   ├── post/                     岗位 CRUD
│   ├── menu/                     菜单树 + 路由
│   ├── dict/                     字典类型 + 字典项 + Redis 缓存
│   ├── operlog/                  操作日志
│   └── loginlog/                 登录日志
├── qkit-admin/                   启动模块
│   ├── ScaffoldApplication.java
│   ├── application.yml
│   └── db/migration/             Flyway DDL/Seed
├── frontend/                     前端
│   ├── src/api/                  接口封装
│   ├── src/views/                业务页面
│   ├── src/router/               路由
│   ├── src/stores/               Pinia
│   ├── src/layout/               布局
│   └── src/components/           公共组件
├── deploy/
│   ├── docker-compose.yml
│   ├── docker/                   Dockerfile
│   └── nginx/                    Nginx 配置
└── scripts/
    ├── dev.sh / dev.bat          本地启动
    └── deploy.sh                 一键部署
```

## 三、快速开始

### 1. 环境要求
- JDK 17+（推荐 Temurin）
- Maven 3.9+
- Node.js 20+
- MySQL 8.0+
- Redis 7.x+

### 2. 初始化数据库

```sql
CREATE DATABASE qkit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `qkit-admin/src/main/resources/application-dev.yml` 中的 `spring.datasource` 和 `spring.redis` 配置。

### 3. 启动后端

```bash
# 根目录
mvn clean install -DskipTests
cd qkit-admin
mvn spring-boot:run
```

启动后访问 `http://localhost:8080/doc.html` 查看 API 文档。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`，默认账号 `admin / admin1234`。

### 5. 一键启动（推荐）

```bash
# Linux/macOS/Git Bash
./scripts/dev.sh

# Windows CMD
scripts\dev.bat
```

## 四、API 规范

### 统一响应
```json
{
  "code": 200,
  "message": "ok",
  "data": { ... },
  "total": 0,
  "pageNum": 1,
  "pageSize": 10,
  "traceId": "abc123"
}
```

### 错误码
- `200` 成功；`400/401/403/404/405/422/429/500` 为 HTTP 语义通用码
- `10001-10006` 用户相关（10001 用户不存在、10002 密码错误、10003 用户被禁用、10004 账号锁定、10005 用户名已存在）
- `10101-10102` 验证码
- `11001-11004` 角色相关
- `12001` 菜单相关
- `13001-13002` 部门相关
- `14001` 字典相关
- `15001` 岗位相关
- `16001` 导出
- `17001-17002` 参数配置

> 完整错误码见 `com.qkit.common.api.ErrorCode` 枚举。

### 权限标识
`<module>:<resource>:<action>` 三段式，例如 `system:user:save`、`system:role:delete`。

## 五、安全设计

1. **密码**：BCrypt（cost=10）单向加密，登录失败 5 次/10 分钟锁用户名。
2. **认证**：Sa-Token JWT 模式，token 放在 `satoken` 请求头。
3. **接口鉴权**：`@SaCheckPermission("system:user:save")`。
4. **按钮鉴权**：前端 `v-permission="'system:user:save'"` 指令。
5. **数据权限**：5 级（全部/本部门及下级/本部门/本人/自定义），通过 `@DataScope` 注解。
6. **传输**：生产环境必须 HTTPS。
7. **SQL 注入**：MyBatis-Plus 使用预编译参数化查询。
8. **XSS**：前端 Element Plus 自动转义。
9. **CSRF**：Sa-Token 前后端分离模式天然免疫。

## 六、部署

### Docker Compose（推荐）

```bash
cd deploy
docker compose up -d
```

启动后：
- 前端：`http://localhost`
- 后端：`http://localhost:8080/admin-api`
- MySQL：`localhost:3306`（root/root123）
- Redis：`localhost:6379`

### 手动部署

1. 后端：`mvn package` → 上传 `qkit-admin/target/*.jar` → `java -jar`
2. 前端：`npm run build` → 将 `dist/` 部署到 Nginx
3. Nginx 配置参考 `deploy/nginx/nginx.conf`

## 七、开发指南

### 新增业务模块

1. **后端**：在 `qkit-system` 下按 `domain/convert/mapper/service/controller` 五层结构添加
2. **前端**：在 `frontend/src/views/` 对应目录下添加页面
3. **菜单**：登录后台，进入「系统管理 → 菜单管理」添加菜单
4. **权限**：进入「角色管理」给角色分配新菜单的权限

### 代码风格
- 后端：阿里巴巴 Java 规范、`record` DTO/VO、`@TableName`/`@TableField` 显式映射
- 前端：Vue3 Composition API + `<script setup lang="ts">`、统一从 `@/` 引入

## 八、版本

当前版本：**v0.1.0**（脚手架 MVP）

## 九、License

MIT
