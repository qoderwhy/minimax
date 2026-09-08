# 02 · 技术选型与决策清单

> ⚠️ 版本号全部**锁死**。智能体禁止自行升级/降级。
> 所有【待确认】的决策点在第 4 节统一收敛，改一处全文生效。

## 1. 后端

| 类别 | 选型 | 锁定版本 | 备注 |
|---|---|---|---|
| JDK | OpenJDK (Temurin) | **17** | LTS；record / switch 表达式 / Text Blocks 可用 |
| 框架 | Spring Boot | **3.2.12** | Jakarta EE 命名空间，禁用 `javax.*` |
| Web | spring-boot-starter-web | 随 Boot | 不用 WebFlux |
| 持久层 | MyBatis-Plus | **3.5.7** | 含分页/乐观锁/防全表更新插件 |
| 数据库连接池 | HikariCP | 随 Boot | 默认即可 |
| 数据库 | MySQL | **8.0+** | utf8mb4 / utf8mb4_unicode_ci；驱动 `mysql-connector-j` |
| 缓存 | Redis + spring-data-redis (Lettuce) | Redis **7.x** | 存 Sa-Token、图形验证码、字典缓存、限流计数 |
| 认证 | Sa-Token | **1.39.0** | JWT 模式，禁用其它模式 |
| 校验 | spring-boot-starter-validation | 随 Boot | `jakarta.validation` |
| 工具库 | Hutool | **5.8.32** | 禁 commons-lang3 混用 |
| 对象映射 | MapStruct | **1.5.5.Final** | **唯一映射方案**，禁 `BeanUtils.copyProperties` |
| API 文档 | springdoc-openapi + knife4j | springdoc **2.5.0** / knife4j **4.5.0** | 禁 Swagger 2 / Springfox |
| 工具 | Lombok | **1.18.34** | DTO/VO **不**用 Lombok（直接用 record）；Entity / Domain 禁 `@Data` |
| 密码加密 | Sa-Token 内置 BCrypt | — | 不自实现 |
| 数据库版本化 | Flyway | **9.22.3**（避开 10.x 二次开源 License 变更；与 Boot 3.2 默认管理版本解耦） | `db/migration/V*.sql` |
| 健康检查 | spring-boot-starter-actuator | 随 Boot | 提供 `/actuator/health`（10 §6.1）；**不**暴露全部端点，需显式 `management.endpoints.web.exposure.include` |
| 监控指标（可选） | micrometer-registry-prometheus | 随 Boot | 暴露 `/actuator/prometheus`（10 §6.3）；本期不强制接 Prometheus |
| 测试 | JUnit 5 + Mockito | 5.10+ / 5.x | 不引 Testcontainers（DB 用本地 docker） |

### 1.1 后端明确禁用

- ❌ Springfox / springfox-swagger（与 Boot 3 不兼容）
- ❌ `javax.*` 包（必须 `jakarta.*`）
- ❌ 物理删除接口（统一逻辑删除 `@TableLogic`）
- ❌ `BeanUtils.copyProperties`（必须 MapStruct）
- ❌ 字段注入 `@Autowired/@Resource`（必须构造器注入 `@RequiredArgsConstructor`）
- ❌ 业务代码手写原生 XML SQL / 字符串拼接（复杂查询用 `LambdaQueryWrapper`）
- ❌ `Thread.sleep` / 硬编码线程休眠
- ❌ 在 Controller 里写业务逻辑

## 2. 前端

| 类别 | 选型 | 锁定版本 | 备注 |
|---|---|---|---|
| 框架 | Vue | **3.4.x** | Composition API + `<script setup lang="ts">`，禁用 Options API |
| 构建 | Vite | **5.4.x** | 禁 Webpack |
| 语言 | TypeScript | **5.4.x** | `strict: true`；业务文件全 TS，禁纯 .js |
| 组件库 | Element Plus | **2.8.x** | 中文文档最全 |
| 自动导入 | unplugin-vue-components + unplugin-auto-import | latest | 主题色用 CSS Vars 覆盖 |
| 状态管理 | Pinia | **2.1.x** | 禁 Vuex |
| 路由 | Vue Router | **4.3.x** | 动态路由（菜单驱动） |
| HTTP | Axios | **1.7.x** | 封装于 `src/utils/request.ts` |
| 图表 | ECharts | **5.5.x** | 按需引入，本期只在仪表盘占位 |
| 包管理 | pnpm | **9.x** | 禁 npm / yarn 混用 |
| CSS | SCSS | — | 全局变量在 `src/styles/variables.scss` |
| 工具 | dayjs | **1.11.x** | 替代 moment |
| 工具 | nprogress | latest | 路由切换进度条 |
| Node.js | Node | **20 LTS** | — |

### 2.1 前端明确禁用

- ❌ jQuery 及 jQuery 插件
- ❌ `var` 声明、`any` 满天飞（tsconfig 开启 strict，确需用 `unknown` + 收窄）
- ❌ 直接操作 DOM（业务页面）
- ❌ 组件内写死后端地址（必须走 vite proxy + env）
- ❌ 组件内直接 `axios.get('/xxx')`（必须走 `src/api/` 封装层）
- ❌ Options API 写法

## 3. 依赖坐标速查

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring-boot.version>3.2.12</spring-boot.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <sa-token.version>1.39.0</sa-token.version>
    <hutool.version>5.8.32</hutool.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <springdoc.version>2.5.0</springdoc.version>
    <knife4j.version>4.5.0</knife4j.version>
    <lombok.version>1.18.34</lombok.version>
    <mysql.version>8.3.0</mysql.version>
    <flyway.version>9.22.3</flyway.version>
</properties>
```

```jsonc
// frontend/package.json 关键依赖（截选）
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "element-plus": "^2.8.0",
    "@element-plus/icons-vue": "^2.3.1",
    "axios": "^1.7.0",
    "dayjs": "^1.11.0",
    "echarts": "^5.5.0",
    "nprogress": "^0.2.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.4.0",
    "typescript": "^5.4.0",
    "vue-tsc": "^2.0.0",
    "unplugin-vue-components": "^0.27.0",
    "unplugin-auto-import": "^0.17.0",
    "sass": "^1.77.0",
    "eslint": "^8.57.0",
    "prettier": "^3.3.0"
  }
}
```

## 4. ⭐ 决策清单（qkit 专属）

> 继承自 `scaffold-docs/02` 的 D1~D15，本项目只列出**与兄弟项目不同**或**新增**的决策点。

| # | 决策点 | 决策 | 理由 |
|---|---|---|---|
| D-mini-1 | 包名 | `com.qkit` | 与模块名 qkit-* 对齐 |
| D-mini-2 | 模块前缀 | `qkit-` | 而不是 lowcode- / scaffold-；与兄弟项目区分 |
| D-mini-3 | 认证框架 | **Sa-Token 1.39.0** | 不用 Spring Security；JWT 模式 |
| D-mini-4 | 主键策略 | 数据库自增 `BIGINT AUTO_INCREMENT` + 实体 `@TableId(type = IdType.AUTO)`，Long 直接序列化为数字 | 自增值小，无 JS 精度问题 |
| D-mini-5 | 公共字段 | `create_by / create_time / update_by / update_time / del_flag(0/1)` | 口径与 yudao-cloud 一致 |
| D-mini-6 | 返回码 | `code=200 成功`；失败 `code != 200` + `msg` | ErrorCode 枚举统一 |
| D-mini-7 | 分页 | `pageNum / pageSize`（入参）；`R.ok(data, total, pageNum, pageSize)` | 与 05 一致 |
| D-mini-8 | URL 前缀 | 无 `/api/v1` 前缀；统一 `/admin-api/<module>/<verb>` | Sa-Token 路由拦截更简洁 |
| D-mini-9 | 模块拆分 | 4 个底座 + 预留：qkit-admin/common/framework/system | 不含 designer / generator / flow |
| D-mini-10 | 多租户 | **默认不启用**（`qkit.tenant.enabled=false`）；启用时由 04 §1.9 注入 `tenant_id` 字段 + 拦截器 | MVP 不需要；开关控制避免二期返工全表 |
| D-mini-11 | 分布式锁 | **不引入 Redisson** | 单体部署足够；后续按需 |
| D-mini-12 | UI 库 | **Element Plus 2.8.x** | 与兄弟项目对齐 |
| D-mini-13 | Service 分层 | Controller → Service → Manager(可选) → Mapper | 单表 CRUD 不引入 Manager；跨表/跨服务编排才用 Manager 封装，Service 暴露业务 API |
| D-mini-14 | 数据权限 | **5 级**：全部 / 本部门及下级 / 本部门 / 仅本人 / 自定义 | 默认值 1（全部） |
| D-mini-15 | 操作日志 | `@OperLog(module="用户管理", name="新增用户")` 注解 + AOP 自动记录 | 零侵入；注解参数必须**双参**，对应 `sys_oper_log.module / name` |
| D-mini-16 | 登录安全 | BCrypt + 5 次失败锁定 10 分钟 + 图形验证码 | Sa-Token 限流 + 自定义 |
| D-mini-17 | 静态资源 | 前端 `pnpm build` 产物由 Spring Boot 静态托管 `classpath:/static/` | 单 jar 部署 |

## 5. 投喂前自检清单

- [ ] 后端所有依赖版本与第 1 节一致
- [ ] 前端所有依赖版本与第 2 节一致
- [ ] D-mini-1 ~ D-mini-17 全部已决
- [ ] `mvn dependency:tree` 无冲突
- [ ] `pnpm i` 无 peer dependency 警告
