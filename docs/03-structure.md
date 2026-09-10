# 03 · 目录结构与分层规范

> 智能体生成的任何代码都必须落在本规范指定的目录与包内。

## 1. 仓库顶层结构

```
qkit/
├── pom.xml                          # 根 POM，dependencyManagement
├── qkit-admin/                   # 启动模块：ScaffoldApplication + application*.yml + db/migration（Flyway）
├── qkit-common/                  # 通用层：R/异常/枚举/常量/工具
├── qkit-framework/               # 框架层：MyBatis-Plus/Sa-Token/Redis/Web/AOP
├── qkit-system/                  # 系统管理业务：用户/角色/部门/岗位/菜单/字典/参数/日志
├── frontend/                        # 前端工程（npm，锁文件为 package-lock.json）
├── deploy/
│   ├── docker-compose.yml           # MySQL + Redis + backend + frontend 一键编排
│   ├── docker/                      # Dockerfile.backend / Dockerfile.frontend
│   └── nginx/                       # nginx.conf 反代配置（部署时用）
├── docs/                            # 本文档集
└── scripts/                         # 本地启动 / 部署脚本（dev.sh、dev.bat、deploy.sh）
```

> 数据库 DDL/种子 SQL 位于 `qkit-admin/src/main/resources/db/migration/`（Flyway），根目录**不再**有 `sql/` 目录。

### 1.1 Maven 模块依赖图

```
qkit-admin
    ├── depends on → qkit-system
    │                   ├── depends on → qkit-framework
    │                   │                   ├── depends on → qkit-common
    │                   │                   └── depends on → qkit-common
    │                   └── depends on → qkit-common
    ├── depends on → qkit-framework
    ├── depends on → qkit-common
    └── depends on → spring-boot-starter-web / validation / actuator
```

> 依赖方向严格向下，**禁止反向依赖**（system 不能依赖 admin）。

## 2. 后端包结构（以 `qkit-system` 为例）

```
com.qkit.system
├── controller/                      # 接收请求，参数校验，调 service
│   └── admin/                       # /admin-api/system/user（App 端本期无）
├── service/                         # 接口 + impl
│   ├── UserService.java
│   └── impl/UserServiceImpl.java
├── manager/                         # 复杂业务编排（可选；当前系统模块未引入）
├── mapper/                          # MyBatis-Plus Mapper
│   └── UserMapper.java
├── domain/
│   ├── entity/                      # 数据实体（继承 BaseEntity）
│   │   └── User.java
│   ├── dto/                         # 入参：UserSaveDTO / UserQueryDTO
│   │   ├── UserSaveDTO.java
│   │   └── UserQueryDTO.java
│   └── vo/                          # 出参：UserVO（**唯一返回类型**）
│       └── UserVO.java
├── convert/                         # MapStruct 转换器
│   └── UserConvert.java
├── enums/                           # 模块内枚举
│   ├── DataScopeEnum.java
│   └── MenuTypeEnum.java
├── security/                        # Sa-Token 权限/角色实现 + 数据权限
│   ├── StpInterfaceImpl.java        # 权限/角色获取（详见 06 §3.2）
│   └── datascope/                   # 数据范围 Handler（详见 06 §4.1）
└── log/                             # 登录/操作日志落库实现（LoginLogRecorder、OperLogSinkImpl）
```

> `@DataScope` 注解本身定义在 `qkit-framework`（`security/annotation/DataScope.java`），业务模块只引用，**不在**本模块另建 `annotation/` 包。

### 2.1 分层铁律

| 规则 | 说明 |
|---|---|
| **R1 单向依赖** | Controller → Service → Manager → Mapper，禁止反向 |
| **R2 Controller 不连 Mapper** | Controller 只注 Service；不直接 `@Autowired UserMapper` |
| **R3 实体不外泄** | Service 不返回 Entity 给 Controller；Controller 不返回 Entity/DTO 给前端 |
| **R4 DTO 仅入参** | Service 方法**入参用 DTO**，**返回用 VO** |
| **R5 Manager 层定位** | 跨表/跨服务编排放 Manager；单表 CRUD 不用 Manager |
| **R6 事务边界** | `@Transactional` 加在 Service 方法上（只读加 `readOnly = true`） |
| **R7 校验放 Controller** | `@Valid` 加在 Controller 入参；DTO 字段用 `jakarta.validation` 注解 |
| **R8 异常统一抛出** | Service 抛 `BusinessException(ErrorCode.XXX)`，由全局拦截器统一封装 |

### 2.2 Entity vs DTO vs VO 区别

| 类型 | 后缀 | 用途 | 注解 |
|---|---|---|---|
| Entity | 无（`User`） | 数据库映射，继承 BaseEntity | `@TableName("sys_user")` `@Getter`（**禁** `@Data`） |
| DTO | `SaveDTO` / `QueryDTO` | 接收前端入参 | **record** + validation 注解 |
| VO | `VO` | 返回给前端 | **record** + `@Schema` |
| Convert | `XxxConvert` | MapStruct | `@Mapper` |

> Entity 用 `@Getter` + 构造器 + 必要的 setter；**不**用 `@Data`，避免无脑 setter 破坏不变量。
>
> **例外**：EasyExcel 导出 VO（如 `UserExportVO`）因需要无参构造/构建器，可用 `@Data` class 而非 record。

## 3. 公共模块包结构（`qkit-common`）

```
com.qkit.common
├── api/                             # 统一响应 R + 错误码（R 自身带分页字段 total/pageNum/pageSize，见 05 §1，无需独立 PageResult）
│   ├── R.java
│   └── ErrorCode.java               # 错误码集中维护
├── cache/                           # 缓存抽象
│   └── CacheService.java
├── constant/                        # 常量
│   ├── CacheConstants.java
│   └── SecurityConstants.java
├── entity/                          # 公共实体基类
│   └── BaseEntity.java              # 5 个公共字段 + 主键 id，所有 Entity 继承
├── exception/                       # BusinessException / SystemException / GlobalExceptionHandler
│   ├── BusinessException.java
│   ├── SystemException.java
│   └── GlobalExceptionHandler.java
├── log/                             # 操作日志 SPI
│   └── spi/
│       ├── OperLogRecord.java
│       └── OperLogSink.java
├── transaction/                     # 事务工具
│   └── TransactionUtils.java
├── util/                            # 工具类（基于 Hutool 二次封装）
└── validation/                      # 校验分组
    └── group/                       # @Validated(SaveGroup.class) 分组注解
        ├── SaveGroup.java
        ├── UpdateGroup.java
        └── DefaultGroup.java（兜底，不分组时匹配）
```

> **包位置铁律**：
> - `api/ErrorCode` ← 错误码枚举（被 Service/Manager throw new BusinessException(ErrorCode.XXX) 引用）
> - `entity/BaseEntity` ← Entity 继承的公共基类
> - `validation/group/` ← SaveGroup/UpdateGroup/DefaultGroup 三档分组
> - 业务模块自定义注解放 `com.qkit.<module>.annotation/`；框架级注解放 `com.qkit.framework.<sub>.annotation/`，**不在** common 下重复声明

## 4. 框架模块包结构（`qkit-framework`）

```
com.qkit.framework
├── config/                          # OpenApiConfig（Knife4j / springdoc）
├── mybatis/                         # MyBatis-Plus 配置（含 MetaObjectHandler 自动填充）
│   └── MybatisPlusConfig.java
├── security/                        # 安全相关：Sa-Token + 数据权限注解
│   ├── SaTokenConfig.java
│   └── annotation/
│       └── DataScope.java           # @DataScope 注解（详见 06 §4.1.1）
├── redis/                           # Redis 配置
│   └── RedisConfig.java
├── jackson/                         # Jackson 全局时间序列化
│   └── JacksonConfig.java
├── web/                             # Web 配置
│   ├── WebConfig.java               # CORS / 拦截器
│   ├── TraceIdFilter.java           # 链路 ID 注入
│   └── TraceIdResponseAdvice.java   # 链路 ID 回写
├── log/                             # 操作日志
│   ├── annotation/OperLog.java        # @OperLog 注解（module + name 双参）
│   └── OperLogAspect.java             # @OperLog 切面
├── repeat/                          # 防重复提交
│   ├── annotation/RepeatSubmit.java
│   └── RepeatSubmitAspect.java
├── ratelimit/                       # 登录限流
│   └── LoginRateLimiter.java
└── captcha/                         # 图形验证码
    └── CaptchaUtil.java
```

> `GlobalExceptionHandler` 只放在 `qkit-common/exception/`，框架层不再重复定义。

## 5. 前端目录约定（`frontend/src`）

```
src/
├── api/                             # 与后端 Controller 一一对应
│   ├── system/
│   │   ├── user.ts
│   │   ├── role.ts
│   │   ├── menu.ts
│   │   ├── dept.ts
│   │   ├── post.ts
│   │   ├── dict.ts
│   │   ├── config.ts
│   │   ├── loginLog.ts
│   │   └── operLog.ts
│   ├── auth.ts
│   └── profile.ts
├── components/                      # 全局通用组件（业务无关）
│   ├── DictSelect.vue               # 字典下拉
│   ├── DictTag.vue                  # 字典标签
│   ├── IconSelect.vue
│   └── ThemePanel.vue
├── composables/                     # 组合式函数
│   ├── useCrud.ts                   # 通用 CRUD 列表页
│   └── usePagination.ts
├── directives/
│   └── permission.ts                # v-permission 指令
├── layout/                          # 后台布局
│   ├── index.vue                    # 顶栏 + 侧边栏 + Tags + 主区
│   └── components/
│       ├── Sidebar.vue
│       ├── Navbar.vue
│       └── TagsView.vue
├── router/
│   ├── index.ts
│   ├── routes.ts                    # 静态路由（登录/404）
│   └── dynamic.ts                   # 动态路由（菜单驱动）
├── stores/                          # Pinia
│   ├── user.ts                      # token / userInfo
│   ├── permission.ts                # 动态路由 / 按钮权限
│   ├── dict.ts                      # 全局字典缓存
│   ├── app.ts                       # 应用级状态
│   └── theme.ts                     # 主题
├── types/                           # TS 类型
│   └── api.d.ts                     # 通用类型；业务类型就近定义在 src/api/*
├── utils/
│   ├── request.ts                   # Axios 封装
│   ├── auth.ts                      # token 存取
│   ├── validate.ts
│   └── icons.ts
├── views/                           # 业务页面，按模块分目录
│   ├── login/
│   │   └── index.vue
│   ├── dashboard/
│   │   └── index.vue
│   ├── error/                       # 403/404 等
│   ├── profile/                     # 个人中心
│   └── system/                      # 每模块单文件 index.vue（弹窗内联）
│       ├── user/index.vue
│       ├── role/index.vue
│       ├── menu/index.vue
│       ├── dept/index.vue
│       ├── post/index.vue
│       ├── dict/index.vue
│       ├── config/index.vue
│       ├── oper-log/index.vue
│       └── login-log/index.vue
├── styles/
│   ├── index.scss                   # 全局样式 + CSS 变量
│   └── themes.scss
├── App.vue
├── main.ts
├── env.d.ts
├── auto-imports.d.ts                # unplugin-auto-import 生成
└── components.d.ts                  # unplugin-vue-components 生成
```

> 说明：`frontend/index.html` 位于工程根（不在 `src/`）；`src/assets/`、`types/system/*.d.ts`、各页面 `components/` 子目录**当前未采用**，如需再按需新增。

### 5.1 前端铁律

| 规则 | 说明 |
|---|---|
| **F1 API 封装** | 所有 HTTP 调用走 `src/api/<module>/<name>.ts`；视图不直接 axios |
| **F2 TS 类型** | 业务类型就近定义在 `src/api/<module>.ts`（如 `UserItem`）；`types/api.d.ts` 只放通用类型 |
| **F3 useCrud** | 列表页统一封装为 `useCrud` 组合式函数（options 对象入参，见 `composables/useCrud.ts`） |
| **F4 字典** | 用 `<DictSelect dict-type="sys_user_sex" />` 组件，数据取自 `useDictStore().loadDict(type)` |
| **F5 角色权限** | 按钮级权限用 `v-permission="'system:user:create'"` 指令（指令同时兼容数组形式） |
| **F6 路由懒加载** | `component: () => import('@/views/...')` |
| **F7 动态路由** | 登录后从 `/admin-api/system/menu/route` 拉取，过滤生成路由 |
| **F8 token** | Pinia `useUserStore().token`；`request.ts` 拦截器自动加 header |

## 6. 命名规范速查

| 对象 | 规范 | 示例 |
|---|---|---|
| 根项目 | 全小写 | `qkit` |
| Maven 模块 | 全小写，连字符 | `qkit-system` |
| 数据库表 | 小写下划线，模块前缀 | `sys_user`、`sys_role` |
| 实体类 | 表名转大驼峰，无后缀（去掉 `sys_` 前缀） | `User`（表 `sys_user`） |
| Mapper | `XxxMapper` | `UserMapper`（表 `sys_user`） |
| Service | `XxxService` / `XxxServiceImpl` | `UserService` |
| Controller | `XxxController` | `UserController` |
| 路由 | `/admin-api/<module>/<verb>` | `/admin-api/system/user/page` |
| Controller 方法 | 标准动词 | `page / list / detail / create / update / delete / export` |
| DTO / VO | `XxxSaveDTO` / `XxxQueryDTO` / `XxxVO` | `UserSaveDTO` |
| 前端 api 文件 | 与 Controller 同名小写 | `api/system/user.ts` |
| 前端组件 | 大驼峰 .vue | `DictTag.vue` |
| 前端 store | `useXxxStore` | `useUserStore` |
| 权限标识 | `<module>:<resource>:<action>` | `system:user:create` |
| 字典类型 | `sys_<module>_<name>` | `sys_user_sex` |

### 6.1 核心原则：表前缀归数据库，包结构归 Java

对于 `sys_user` 表，实体类**推荐命名为 `User`（去掉 `sys_` 前缀）**，其余各层跟随实体名：

```
✅ 推荐：User, UserMapper, UserService, UserController
❌ 不推荐：SysUser, SysUserMapper, SysUserService
```

| 层面 | 组织方式 | 示例 |
|---|---|---|
| 数据库 | 表前缀 `sys_` / `b_` / `sys_log_` | `sys_user`、`b_trade_order` |
| Java 代码 | **包结构** | `com.qkit.system.domain.entity.User` |

> 包结构已经承担了模块划分的职责，再在类名上加前缀属于**信息冗余**，且会把数据库设计细节泄漏到领域模型里。

### 6.2 各层命名速查表（以 `sys_user` 为例）

| 层 | 命名规则 | 本项目示例 | 说明 |
|---|---|---|---|
| **Entity** | `{名词}`（去表前缀） | `User` | 大驼峰、无后缀；继承 `BaseEntity` |
| **Mapper** | `{Entity}Mapper` | `UserMapper` | 不用 `UserDao` |
| **Service 接口** | `{Entity}Service` | `UserService` | **不加** `I` 前缀 |
| **Service 实现** | `{Entity}ServiceImpl` | `UserServiceImpl` | 位于 `service/impl/` |
| **Controller** | `{Entity}Controller` | `UserController` | 位于 `controller/admin/` |
| **DTO（入参）** | `{Entity}{动作}DTO` | `UserSaveDTO`、`UserQueryDTO` | record |
| **VO（出参）** | `{Entity}{场景}VO` | `UserVO`、`UserExportVO` | record（导出例外见 6.3） |
| **Convert** | `{Entity}Convert` | `UserConvert` | MapStruct 接口 |

### 6.3 DTO / VO 细分命名

```java
// ===== DTO：前端 → 后端（入参）=====
UserSaveDTO              // 新增 + 更新共用，用 SaveGroup / UpdateGroup 分组校验区分
UserQueryDTO             // 查询条件（含 pageNum / pageSize）
UserResetPasswordDTO     // 动作专属：重置密码
UserProfileUpdateDTO     // 动作专属：更新个人资料
PasswordDTO              // 动作专属：修改密码（本人）

// ===== VO：后端 → 前端（出参）=====
UserVO                   // 主 VO：列表 + 详情共用（详情额外填充 roleIds）
UserExportVO             // 导出专用（EasyExcel，见 6.4）
LoginUserVO              // 登录返回专用
DeptTreeVO / DeptSimpleVO// 场景 VO：树形 / 下拉专用
```

**约定**：

- 新增与更新**共用** `SaveDTO`，靠 `SaveGroup` / `UpdateGroup` 分组校验区分；**不**拆成 `UserCreateDTO` / `UserUpdateDTO`（避免字段重复维护）。
- 只有"语义完全不同且字段差异大"的动作才建专属 DTO（如重置密码、个人资料）。
- **不引入 BO（Business Object）层**：跨 Service 传参直接用 Service 接口返回的 VO / Entity。

### 6.4 导出 VO 的特殊性

`UserExportVO` 是**唯一允许不用 record** 的 VO：EasyExcel 需要无参构造与构建器，因此用 `@Data` class，且时间字段必须显式标注：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportVO {
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")   // com.alibaba.excel.annotation.format.DateTimeFormat
    private LocalDateTime createTime;
}
```

> 与 07 §1.4 一致：普通 VO 一律 record，仅导出 VO 例外。

### 6.5 同名实体冲突处理

```java
// 场景：sys_user（系统用户）与 b_user（业务/商户用户）同名

// 方案 A（推荐）：用包区分，类名仍不加前缀
com.qkit.system.domain.entity.User     // sys_user
com.qkit.business.domain.entity.User   // b_user

// 方案 B：用业务含义区分
com.qkit.domain.entity.AdminUser       // sys_user（管理员）
com.qkit.domain.entity.MerchantUser    // b_user（商户用户）
```

> ⚠️ **禁止** `SysUser` / `BizUser` / `SysConfig` 这类"表前缀类名"——暴露数据库设计细节、语义差。

### 6.6 字段与方法命名

```java
// ✅ 实体字段：DB 下划线 → Java 小驼峰
// DB: user_name, create_time, del_flag
private String userName;
private LocalDateTime createTime;
private Integer delFlag;      // 不是 isDeleted

// ✅ 布尔字段：不加 is 前缀（避免 Jackson / Lombok 生成 isXxx 造成序列化歧义）
private Boolean enabled;      // ✅
private Boolean isEnabled;    // ❌

// ✅ 外键字段：关联对象 + Id
private Long deptId;
private Long postId;
```

**方法命名（与 05 §5 一致，非 REST 风格）**：

```java
// Service / Controller 方法名 = URL 最后一段 = 标准动词
public R<List<UserVO>>  page(UserQueryDTO query);
public R<UserVO>        detail(Long id);
public R<Long>          create(UserSaveDTO dto);
public R<Boolean>       update(UserSaveDTO dto);
public R<Boolean>       delete(List<Long> ids);
public void             export(UserQueryDTO query, HttpServletResponse response);
```

| 层 | 命名 | 说明 |
|---|---|---|
| Service / Controller 方法 | `page / list / detail / create / update / delete / export / simple-list / assign-role / reset-password / assign-menu / assign-dept / clean` | 方法名、URL 最后一段、权限码 action 三段一致（见 05 §5.1、06 §10.1） |
| 前端 api 函数 | 动词后置 | `pageUser / getUser / saveUser / deleteUser / assignRole / resetUserPassword / exportUser` |

> **注意**：本项目**不**采用 REST 风格（`getUserById` / `createUser` 等），Controller 方法名必须与路径动词一致，否则违背 05 §5.1 铁律。

## 7. 跨域与跨模块

1. **业务模块间禁止直接依赖实现类**：跨域查询走对方 Service 接口（Spring 注入）。
2. **跨域写操作**走 Spring `ApplicationEvent` 领域事件（本期可有可无）。
3. **CORS**：`qkit-framework/web/WebConfig.java` 从配置项 `app.cors.allowed-origin-patterns` 读取（默认仅 `http://localhost:*`、`http://127.0.0.1:*`）；生产环境**禁止**用 `*`（与 `allowCredentials(true)` 冲突）。
4. **客户端 SDK**（`qkit-client`）：本期**不引入**；业务模块 ≥ 3 个时再考虑。

## A. 附录：字段命名映射 + MapStruct 配置

> **背景**：DDL 用 snake_case（MySQL 字段），Java/TS 用 camelCase。MapStruct 默认**不开**自动转换，跨字段映射会无声失败（target 全 null）。本附录给出三层映射表 + 必备配置。

### A.1 三层字段命名映射表

| DDL（snake_case） | Java Entity（camelCase） | Java DTO/VO（camelCase） | TS 类型（camelCase） | 说明 |
|---|---|---|---|---|
| `real_name` | `realName` | `realName` | `realName` | 经典双词 |
| `login_date` | `loginDate` | `loginDate` | `loginDate` | 时间类 |
| `avatar_url` | `avatarUrl` | `avatarUrl` | `avatarUrl` | URL 类 |
| `user_id` | `userId` | `userId` | `userId` | 外键 |
| `del_flag` | `delFlag` | —（不出参）| —（不出参） | 逻辑删除字段不进 VO |
| `create_by` | `createBy` |（来自 BaseEntity）| —（不出参） | 公共字段 |
| `create_time` | `createTime` |（来自 BaseEntity）| `createTime` | 创建时间会展示 |
| `role_ids`（JSON） | `List<Long> roleIds` | `List<Long> roleIds` | `number[]` | 关联数组（主键为自增 Long，直接序列化为数字，见 02 D-mini-4） |

> **铁律**：**DDL ↔ Java 字段映射必须显式开启驼峰策略**（见 A.2）；**Java ↔ TS 类型**默认一致，TypeScript `strict: true` 自动校验；`Long` 主键为数据库自增（见 02 D-mini-4），序列化为数字，TS 侧直接标 `number`。

### A.2 MapStruct + MyBatis-Plus 必启用开关

```java
@Mapper(
    componentModel = "spring",                              // 注入 Spring 容器
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,  // DTO null 不覆盖 Entity
    unmappedTargetPolicy = ReportingPolicy.IGNORE            // 未映射字段不报错
)
public interface UserConvert {
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);
    UserVO toVO(User entity);
    List<UserVO> toVOList(List<User> list);
    User toEntity(UserSaveDTO dto);
    @Mapping(target = "password", ignore = true)
    User toUpdateEntity(UserSaveDTO dto);
}
```

**application.yml 全局配置**（关键）：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true    # DDL snake_case ↔ Java camelCase（运行期）
```

> **不开启的后果**：`real_name` DDL 字段映射不到 `realName` Java 字段 → Service 收到的 `user.realName` 永远是 null，但**编译不报错**，运行期静默错。
>
> **关于 MapStruct 命名策略**：1.5.x 须在 `pom.xml` 的 `maven-compiler-plugin` `<compilerArgs>` 配 `-Amapstruct.defaultComponentModel=spring`（或通过注解 `componentModel="spring"`，本附录采用注解方式，无需 pom 配置）。**不在** `application.yml` 配。

### A.3 高频命名陷阱

| 场景 | DDL 写法 | Java 写法 | MapStruct 表现 |
|---|---|---|---|
| 字段含数字（`user2_name`） | `user2_name`（反引号可选） | `user2Name` | ✅ MyBatis-Plus 自动转换；MapStruct 需开启 A.2 |
| 字段含连字符（`user-name`） | `` `user-name` ``（必须反引号） | `userName` + `@TableField("user-name")` | ⚠️ MapStruct 默认会映射失败，需 `@Mapping(source = "user-name", target = "userName")` 显式标注 |
| 字段含 SQL 关键字（`order`） | `` `order` ``（必须反引号） | `order`（Java 合法，但建议改名 `sort` / `display_order`） | ⚠️ 不改名则 DDL 必须反引号，且 `@TableField("order")` 显式标注 |
| 字段含前缀缩写（`api_key`） | `api_key` | `apiKey` | ✅ MapStruct 第一个下划线后词视为整词；OK |
| 字段全大写（`URI`） | `URI` | `uri` | ⚠️ MyBatis-Plus 默认转小写，需 `@TableField("URI")` 显式标注 |
| Boolean 字段（`is_enabled`） | `is_enabled` | `enabled`（去掉 is） | ⚠️ Lombok `@Data` 会生成 `isEnabled()` 但字段是 `enabled`，MapStruct 可能映射成 `isEnabled` 字段名；建议显式 `@Mapping(target = "enabled", source = "isEnabled")` |

### A.4 反例（禁止）

```java
// ❌ 错误：MapStruct 默认不开驼峰，realName 永远是 null
@Mapper
public interface UserConvert {
    UserVO toVO(User entity);  // real_name 字段映射不到 realName
}

// ✅ 正确：@Mapper 三开关 + mybatis-plus.configuration.map-underscore-to-camel-case
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = IGNORE)
public interface UserConvert { ... }
```

> **经验法则**：任何 Convert 接口都**必须**带 3 个开关（`componentModel / nullValuePropertyMappingStrategy / unmappedTargetPolicy`），且 `application.yml` 开启 `mybatis-plus.configuration.map-underscore-to-camel-case: true`。Code Review 阶段一票否决。
