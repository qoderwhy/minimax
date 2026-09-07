# 03 · 目录结构与分层规范

> 智能体生成的任何代码都必须落在本规范指定的目录与包内。

## 1. 仓库顶层结构

```
qkit/
├── pom.xml                          # 根 POM，dependencyManagement
├── qkit-admin/                   # 启动模块：ScaffoldApplication + application*.yml
├── qkit-common/                  # 通用层：R/异常/枚举/常量/工具
├── qkit-framework/               # 框架层：MyBatis-Plus/Sa-Token/Redis/Web/AOP
├── qkit-system/                  # 系统管理业务：用户/角色/部门/岗位/菜单/字典/日志
├── frontend/                        # 前端工程（pnpm workspace 顶层）
├── deploy/
│   ├── docker-compose.yml           # MySQL + Redis 一键起
│   ├── mysql/init/                  # 首次启动 SQL（仅 docker-entrypoint-initdb.d 场景）
│   └── nginx/                       # 反代配置（部署时用）
├── sql/                             # 旧路径，兼容；新 SQL 放 qkit-admin/src/main/resources/db/migration/
├── docs/                            # 本文档集
└── scripts/                         # 一次性脚本
```

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
│   ├── admin/                       # /admin-api/system/user
│   └── app/                         # /app-api/...  （本期无）
├── service/                         # 接口 + impl
│   ├── UserService.java
│   └── impl/UserServiceImpl.java
├── manager/                         # 复杂业务编排（可选）
│   └── UserManager.java
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
│   └── DataScopeEnum.java
└── annotation/                      # 模块内自定义注解（业务注解放这里；框架注解放 qkit-framework/log/annotation/）
    └── DataScope.java（数据权限注解见 06 §4.1）
```

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

## 3. 公共模块包结构（`qkit-common`）

```
com.qkit.common
├── api/                             # 统一响应 R（R 自身带分页字段 total/pageNum/pageSize，见 05 §1，无需独立 PageResult）
│   └── R.java
├── enums/                           # 通用枚举（ErrorCode、CommonStatusEnum 等）
│   └── ErrorCode.java
├── exception/                       # BusinessException / GlobalExceptionHandler
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── entity/                          # 公共实体基类
│   └── BaseEntity.java              # 5 个公共字段 + 主键 id，所有 Entity 继承
├── validation/                      # 校验分组
│   └── group/                       # @Validated(SaveGroup.class) 分组注解
│       ├── SaveGroup.java
│       ├── UpdateGroup.java
│       └── DefaultGroup.java（兜底，不分组时匹配）
├── constant/                        # 常量
│   ├── CacheConstants.java
│   └── SecurityConstants.java
├── util/                            # 工具类（基于 Hutool 二次封装）
└── annotation/                      # 通用注解（@Dict 字典翻译等）
```

> **包位置铁律**：
> - `enums/ErrorCode` ← 错误码枚举（被 Service/Manager throw new BusinessException(ErrorCode.XXX) 引用）
> - `entity/BaseEntity` ← Entity 继承的公共基类
> - `validation/group/` ← SaveGroup/UpdateGroup/DefaultGroup 三档分组
> - 业务模块自定义注解放 `com.qkit.<module>.annotation/`；框架级注解放 `com.qkit.framework.<sub>.annotation/`，**不在** common 下重复声明

## 4. 框架模块包结构（`qkit-framework`）

```
com.qkit.framework
├── mybatis/                         # MyBatis-Plus 配置 + MetaObjectHandler
│   ├── MybatisPlusConfig.java
│   └── AutoFillHandler.java
├── security/                        # 安全相关：Sa-Token + 数据权限注解
│   ├── SaTokenConfig.java
│   ├── StpInterfaceImpl.java        # 权限/角色获取
│   ├── SaTokenExceptionHandler.java
│   └── annotation/
│       └── DataScope.java           # @DataScope 注解（详见 06 §4.1.1）
├── redis/                           # Redis 配置 + 工具
│   ├── RedisConfig.java
│   └── RedisUtil.java（基于 Hutool RedisTemplate 封装）
├── web/                             # Web 配置
│   ├── WebConfig.java               # CORS / 拦截器
│   └── GlobalExceptionHandler.java（如果 common 没放）
├── log/                             # 日志
│   ├── annotation/OperLog.java        # @OperLog 注解（module + name 双参）
│   ├── OperLogAspect.java             # @OperLog 切面
│   └── LogUtil.java
├── ratelimit/                       # 登录限流
│   └── LoginRateLimiter.java
└── captcha/                         # 图形验证码
    └── CaptchaUtil.java
```

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
│   │   ├── login-log.ts
│   │   └── oper-log.ts
│   └── auth/
│       └── index.ts
├── assets/                          # 静态资源（图片、logo）
├── components/                      # 全局通用组件（业务无关）
│   ├── SvgIcon.vue
│   ├── PageHeader.vue
│   └── DictSelect.vue               # 字典下拉（基于 @Dict 注解）
├── composables/                     # 组合式函数
│   ├── useCrud.ts                   # 通用 CRUD 列表页
│   ├── usePagination.ts
│   └── useDict.ts
├── layout/                          # 后台布局
│   ├── index.vue                    # 顶栏 + 侧边栏 + Tags + 主区
│   ├── components/
│   │   ├── Sidebar.vue
│   │   ├── Navbar.vue
│   │   └── TagsView.vue
├── router/
│   ├── index.ts
│   ├── routes.ts                    # 静态路由（登录/404）
│   └── dynamic.ts                   # 动态路由（菜单驱动）
├── stores/                          # Pinia
│   ├── user.ts                      # token / userInfo
│   ├── permission.ts                # 动态路由 / 按钮权限
│   └── dict.ts                      # 全局字典缓存
├── types/                           # TS 类型
│   ├── api.d.ts
│   ├── system/
│   │   ├── user.d.ts
│   │   └── ...
│   └── global.d.ts
├── utils/
│   ├── request.ts                   # Axios 封装
│   ├── auth.ts                      # token 存取
│   ├── validate.ts
│   └── dict.ts                      # 字典 key→label 映射
├── views/                           # 业务页面，按模块分目录
│   ├── login/
│   │   └── index.vue
│   ├── dashboard/
│   │   └── index.vue
│   └── system/
│       ├── user/
│       │   ├── index.vue            # 列表
│       │   └── components/
│       │       ├── UserFormDialog.vue
│       │       └── AssignRoleDialog.vue
│       ├── role/
│       │   ├── index.vue
│       │   └── components/
│       │       └── AssignMenuDialog.vue
│       ├── menu/
│       ├── dept/
│       ├── post/
│       ├── dict/
│       │   ├── index.vue            # 字典分类列表
│       │   └── items.vue            # 字典项列表（路由参数 ?typeId=）
│       ├── oper-log/
│       └── login-log/
├── styles/
│   ├── variables.scss
│   ├── element-overrides.scss
│   └── index.scss
├── App.vue
├── main.ts
└── env.d.ts
```

### 5.1 前端铁律

| 规则 | 说明 |
|---|---|
| **F1 API 封装** | 所有 HTTP 调用走 `src/api/<module>/<name>.ts`；视图不直接 axios |
| **F2 TS 类型** | 类型放 `types/`；与后端 VO 同名对应（如 `SysUserVO`） |
| **F3 useCrud** | 列表页统一封装为 `useCrud` 组合式函数 |
| **F4 字典** | 用 `<DictSelect dict-type="sys_user_sex" />` 或 `useDict` hook |
| **F5 角色权限** | 按钮级权限用 `v-permission="['system:user:create']"` 指令 |
| **F6 路由懒加载** | `component: () => import('@/views/...')` |
| **F7 动态路由** | 登录后从 `/system/menu/route` 拉取，过滤生成路由 |
| **F8 token** | Pinia `useUserStore().token`；`request.ts` 拦截器自动加 header |

## 6. 命名规范速查

| 对象 | 规范 | 示例 |
|---|---|---|
| 根项目 | 全小写 | `qkit` |
| Maven 模块 | 全小写，连字符 | `qkit-system` |
| 数据库表 | 小写下划线，模块前缀 | `sys_user`、`sys_role` |
| 实体类 | 表名转大驼峰，无后缀 | `SysUser` |
| Mapper | `XxxMapper` | `SysUserMapper` |
| Service | `XxxService` / `XxxServiceImpl` | `UserService` |
| Controller | `XxxController` | `UserController` |
| 路由 | `/admin-api/<module>/<verb>` | `/admin-api/system/user/page` |
| Controller 方法 | 标准动词 | `page / list / detail / create / update / delete / export` |
| DTO / VO | `XxxSaveDTO` / `XxxQueryDTO` / `XxxVO` | `UserSaveDTO` |
| 前端 api 文件 | 与 Controller 同名小写 | `api/system/user.ts` |
| 前端组件 | 大驼峰 .vue | `UserFormDialog.vue` |
| 前端 store | `useXxxStore` | `useUserStore` |
| 权限标识 | `<module>:<resource>:<action>` | `system:user:create` |
| 字典类型 | `sys_<module>_<name>` | `sys_user_sex` |

## 7. 跨域与跨模块

1. **业务模块间禁止直接依赖实现类**：跨域查询走对方 Service 接口（Spring 注入）。
2. **跨域写操作**走 Spring `ApplicationEvent` 领域事件（本期可有可无）。
3. **CORS**：`framework/web/WebConfig.java` 统一配置 `CorsConfig`，开发环境放开所有来源，生产环境限定域名。
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
| `role_ids`（JSON） | `List<Long> roleIds` | `List<Long> roleIds` | `string[]` | 关联数组（注意：TS 用 `string` 防 JS 精度丢失） |

> **铁律**：**DDL ↔ Java 字段映射必须显式开启驼峰策略**（见 A.2）；**Java ↔ TS 类型**默认一致，TypeScript `strict: true` 自动校验；TS 中所有 `Long` 字段显式标为 `string`。

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
    UserDetailVO toDetailVO(User entity);
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
| 字段含连字符（`user-name`） | `` `user-name` ``（必须反引号） | `userName` + `@Column("user-name")` | ⚠️ MapStruct 默认会映射失败，需 `@Mapping(source = "user-name", target = "userName")` 显式标注 |
| 字段含 SQL 关键字（`order`） | `` `order` `` | `order`（不能叫 order，编译失败） | ❌ **必须改名**为 `sort` 或加前缀（`display_order`） |
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
