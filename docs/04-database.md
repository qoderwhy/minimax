# 04 · 数据库设计

> 所有表必须遵循本文档的命名、字段、公共列规范。智能体生成 DDL 前**必须**先读完本文。

## 1. 设计原则

1. **公共列**：每张业务表必须含 `create_by / create_time / update_by / update_time / del_flag` 5 个公共字段，外加主键 `id`（自增 `BIGINT AUTO_INCREMENT`）。
2. **逻辑删除**：`del_flag TINYINT DEFAULT 0`（0=未删，1=已删），统一由 MyBatis-Plus `@TableLogic` 处理。
3. **主键策略**：数据库自增 `BIGINT AUTO_INCREMENT`，实体主键 `@TableId(type = IdType.AUTO)`，`Long` 直接序列化为数字。
4. **字符集**：库 `utf8mb4`、表 `utf8mb4_unicode_ci`、排序规则默认。
5. **时区**：`+08:00`（东八区），MySQL 服务端和连接都设。
6. **金额**：本期无金额表，**预声明**：所有金额字段必须 `DECIMAL(18,2)` 或 `BIGINT`（分），**禁止** `DOUBLE/FLOAT`。
7. **状态**：统一 `status TINYINT DEFAULT 0`（0=停用，1=正常）。
8. **注释**：每张表、每个字段必须写 `COMMENT`。
9. **租户字段（决策点）**：本期**不追加** `tenant_id` 公共列。初始化框架时由 `application.yml` 的 `qkit.tenant.enabled` 开关决定是否生成租户相关字段和逻辑（详见 §1.9）。

### 1.9 租户开关约定（本期不实现，二期预留）

为避免"二期接入多租户时返工全表"，初始化框架阶段提供**可选项**，由 `application.yml` 控制：

```yaml
qkit:
  tenant:
    enabled: false   # 默认 false：单租户，公共列不增加 tenant_id
                     # 二期设为 true：自动注入 tenant_id 字段 + 拦截器 + 前端 store
```

| 开关值 | 含义 | 影响 |
|---|---|---|
| `false`（默认） | 单租户 | 公共列保持 5 字段不变（外加主键 id）；02 §4 决策清单**不引入** tenant 依赖 |
| `true` | 多租户预留 | 业务表自动加 `tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID'` + `idx_tenant_id` 索引；MyBatis-Plus 注入 `TenantLineInnerInterceptor`；前端 `useUserStore` 增加 `tenantId` 字段 |

**红线**：
- 本期（`enabled: false`）严禁在任何 DDL / Entity / SQL 中**手动**加 `tenant_id` 字段
- 二期开启时，必须用 Flyway 脚本统一加字段（04 §7），不允许逐表手改
- 开关切换需走 PR 评审，由架构师确认

> 与 01 §2.2 "多租户（字段级隔离，预留 `tenant_id` 接口）"对齐 — 本期只预留**接入点**，不落地。

## 2. ER 概览

```
sys_user (用户)
  ├─ N:N sys_role          通过 sys_user_role
  ├─ N:1 sys_dept          (dept_id)
  ├─ N:1 sys_post          (post_id)
  └─ 1:N sys_oper_log      (user_id 操作人)

sys_role (角色)
  ├─ N:N sys_menu          通过 sys_role_menu (按 menu_id 数组 OR 关联表)
  └─ N:N sys_dept          通过 sys_role_dept (数据权限-自定义部门)

sys_dept (部门)            树形：parent_id 自引用
sys_post (岗位)            N:1 sys_dept
sys_menu (菜单)            树形：parent_id 自引用；type ∈ {目录M, 菜单C, 按钮F}

sys_dict (字典分类)        1:N sys_dict_item
sys_dict_item (字典项)     N:1 sys_dict

sys_oper_log (操作日志)    N:1 sys_user
sys_login_log (登录日志)   N:1 sys_user (user_id 可空，登录失败时)
sys_config (系统参数)      独立，无表间关系
```

## 3. 表清单

| 表名 | 用途 | 复杂度 |
|---|---|---|
| `sys_user` | 用户 | B |
| `sys_role` | 角色 | B |
| `sys_user_role` | 用户-角色关联 | B |
| `sys_dept` | 部门（树） | B |
| `sys_post` | 岗位 | B |
| `sys_user_post` | 用户-岗位关联 | B |
| `sys_menu` | 菜单（树） | B |
| `sys_role_menu` | 角色-菜单关联 | B |
| `sys_role_dept` | 角色-数据权限（自定义） | B |
| `sys_dict` | 字典分类 | B |
| `sys_dict_item` | 字典项 | B |
| `sys_oper_log` | 操作日志 | A |
| `sys_login_log` | 登录日志 | A |
| `sys_config` | 系统参数配置 | B |

> 表前缀 `sys_` 表示"系统管理"，业务模块建议 `b_`（business）或 `<domain>_`，本期无。

## 4. 表结构详述

### 4.1 sys_user

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | ✓ | 自增 | 主键 |
| username | VARCHAR(30) | ✓ | — | 登录名，唯一 |
| password | VARCHAR(100) | ✓ | — | BCrypt 密文 |
| nickname | VARCHAR(30) | | | 昵称 |
| real_name | VARCHAR(30) | | | 真实姓名 |
| avatar | VARCHAR(255) | | | 头像 URL |
| email | VARCHAR(50) | | | 邮箱 |
| phone | VARCHAR(20) | | | 手机号 |
| sex | TINYINT | | 0 | 0=未知 1=男 2=女，关联字典 `sys_user_sex` |
| dept_id | BIGINT | | | 所属部门 |
| post_id | BIGINT | | | 岗位 |
| status | TINYINT | | 0 | 0=停用 1=正常 |
| login_ip | VARCHAR(50) | | | 最后登录 IP |
| login_date | DATETIME | | | 最后登录时间 |
| remark | VARCHAR(500) | | | 备注 |
| create_by | BIGINT | | | 创建人 |
| create_time | DATETIME | | | 创建时间 |
| update_by | BIGINT | | | 更新人 |
| update_time | DATETIME | | | 更新时间 |
| del_flag | TINYINT | | 0 | 逻辑删除 |

**索引**：`uk_username (username, del_flag)`、`idx_dept_id (dept_id)`

### 4.2 sys_role

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| name | VARCHAR(30) | 角色名称（唯一） |
| code | VARCHAR(30) | 角色编码（唯一，如 `admin` / `common`） |
| data_scope | TINYINT | 数据权限：1=全部 2=本部门及下级 3=本部门 4=仅本人 5=自定义（=5 时配合 sys_role_dept 取部门集合） |
| status | TINYINT | 0=停用 1=正常 |
| sort | INT | 排序 |
| remark | VARCHAR(500) | 备注 |
| +公共列 | | |

**索引**：`uk_code (code, del_flag)`

### 4.3 sys_user_role

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT | 用户 ID |
| role_id | BIGINT | 角色 ID |

**主键**：`pk (user_id, role_id)`

### 4.4 sys_dept（树形）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| name | VARCHAR(30) | 部门名称 |
| parent_id | BIGINT | 上级部门 ID，根节点 0 |
| sort | INT | 排序 |
| leader | VARCHAR(30) | 负责人 |
| phone | VARCHAR(20) | 联系电话 |
| email | VARCHAR(50) | 邮箱 |
| status | TINYINT | 0=停用 1=正常 |
| +公共列 | | |

**索引**：`idx_parent_id (parent_id)`

### 4.5 sys_post

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| code | VARCHAR(30) | 岗位编码（唯一） |
| name | VARCHAR(30) | 岗位名称 |
| dept_id | BIGINT | 所属部门 |
| sort | INT | 排序 |
| status | TINYINT | 0=停用 1=正常 |
| remark | VARCHAR(500) | 备注 |
| +公共列 | | |

### 4.6 sys_user_post

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT | |
| post_id | BIGINT | |

**主键**：`pk (user_id, post_id)`

### 4.7 sys_menu（树形，三种类型）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| name | VARCHAR(50) | 菜单名称 |
| type | CHAR(1) | **M=目录 / C=菜单 / F=按钮** |
| parent_id | BIGINT | 上级菜单，根 0 |
| path | VARCHAR(200) | 路由路径（仅 C） |
| component | VARCHAR(200) | 前端组件路径（仅 C） |
| perm | VARCHAR(100) | 权限标识（F 必填；C 可选，用于表示"查看权限"；M 留空） |
| icon | VARCHAR(50) | 图标 |
| sort | INT | 排序 |
| visible | TINYINT | 是否显示 0=隐藏 1=显示 |
| keep_alive | TINYINT | 是否缓存 0=不缓存 1=缓存 |
| status | TINYINT | 0=禁用 1=启用 |
| +公共列 | | |

**索引**：`idx_parent_id (parent_id)`、`idx_perm (perm)`

### 4.8 sys_role_menu

| 字段 | 类型 | 说明 |
|---|---|---|
| role_id | BIGINT | |
| menu_id | BIGINT | |

**主键**：`pk (role_id, menu_id)`

### 4.9 sys_role_dept（数据权限"自定义"用）

| 字段 | 类型 | 说明 |
|---|---|---|
| role_id | BIGINT | |
| dept_id | BIGINT | |

**主键**：`pk (role_id, dept_id)`

### 4.10 sys_dict

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| name | VARCHAR(50) | 字典名称 |
| type | VARCHAR(50) | 字典类型（唯一，如 `sys_user_sex`） |
| status | TINYINT | 0=停用 1=正常 |
| remark | VARCHAR(500) | 备注 |
| +公共列 | | |

### 4.11 sys_dict_item

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| dict_type | VARCHAR(50) | 字典类型（逻辑外键 → sys_dict.type） |
| label | VARCHAR(50) | 字典项显示值 |
| value | VARCHAR(50) | 字典项存储值 |
| sort | INT | 排序 |
| status | TINYINT | 0=停用 1=正常 |
| css_class | VARCHAR(50) | Element Tag 类型（primary/success/warning/danger/info） |
| remark | VARCHAR(500) | 备注 |
| +公共列 | | |

**索引**：`idx_dict_type (dict_type)`

### 4.12 sys_oper_log

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| module | VARCHAR(50) | 模块名（@OperLog 第一个参数） |
| name | VARCHAR(50) | 操作名（@OperLog 第二个参数） |
| user_id | BIGINT | 操作人 ID |
| username | VARCHAR(30) | 操作人账号（冗余，便于展示） |
| ip | VARCHAR(50) | IP |
| user_agent | VARCHAR(500) | UA |
| method | VARCHAR(200) | 方法签名（如 `UserController.create`） |
| request_url | VARCHAR(255) | URL（含 query） |
| request_method | VARCHAR(10) | HTTP method |
| request_param | TEXT | 入参 JSON |
| response_result | TEXT | 返回结果摘要（截断 2KB） |
| status | TINYINT | 0=失败 1=成功 |
| error_msg | TEXT | 异常堆栈（截断） |
| cost_ms | BIGINT | 耗时（毫秒） |
| oper_time | DATETIME | 操作时间 |
| +create_by / create_time | | 公共列只取这两个 |

**索引**：`idx_user_id (user_id)`、`idx_oper_time (oper_time)`、`idx_module (module)`

### 4.13 sys_login_log

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID（失败可空） |
| username | VARCHAR(30) | 用户名（必填） |
| ip | VARCHAR(50) | IP |
| user_agent | VARCHAR(500) | UA |
| status | TINYINT | 0=失败 1=成功 |
| message | VARCHAR(255) | 提示信息（如"密码错误"、"用户不存在"） |
| login_time | DATETIME | 登录时间 |

**索引**：`idx_username (username)`、`idx_login_time (login_time)`、`idx_status (status)`

### 4.14 sys_config（系统参数配置）

> 与字典表分工：`sys_config` 存系统运行参数，程序读取、影响行为；`sys_dict` 存业务枚举值，给人看/下拉选择。配置修改走权限校验 + 操作日志。

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| id | BIGINT | ✓ | 自增 | 主键 |
| config_name | VARCHAR(100) | ✓ | — | 参数名称 |
| config_key | VARCHAR(100) | ✓ | — | 参数键名（程序取值的 Key，唯一） |
| config_value | VARCHAR(500) | | '' | 参数键值 |
| config_type | CHAR(1) | ✓ | 'N' | 是否系统内置：Y=内置（不可删、不可改键名）N=自定义 |
| remark | VARCHAR(500) | | '' | 备注 |
| +公共列 | | | | create_by / create_time / update_by / update_time（**无 del_flag**，配置表物理删除） |

**索引**：`uk_config_key (config_key)`

**约定**：
1. 取值走 `SysConfigService` 类型安全接口：`getValue(key[, default])` / `getInt(key, default)` / `getBoolean(key, default)`
2. 缓存 + 热更新：应用启动时全量加载到 Redis（`sys_config:{key}` → 键值），管理后台增删改后刷新 Redis 缓存，不重启生效；多实例部署天然一致
3. `config_type='Y'` 的内置参数：不可删除、不可修改键名；敏感值（密钥类）存库前加密、接口脱敏

## 5. 初始化数据（种子）

### 5.1 必要字典

- `sys_user_sex`：未知 / 男 / 女
- `sys_common_status`：正常 / 停用
- `sys_oper_status`：成功 / 失败
- `sys_menu_type`：目录 / 菜单 / 按钮
- `sys_data_scope`：全部 / 本部门及下级 / 本部门 / 仅本人 / 自定义

### 5.2 必要角色

- `admin`（超级管理员）：data_scope=1（全部）
- `common`（普通用户）：data_scope=4（仅本人）

### 5.3 必要菜单

菜单树按"目录（M）→ 菜单（C）→ 按钮（F）"三级组织。权限码 `perm` 与 06 第 10.1 节字典表**逐字一致**（以 06 字典表为权威事实源）。

| 一级目录 | 二级菜单 | 按钮（type=F, perm=权限码） |
|---|---|---|
| 系统管理 | 用户管理 (`system:user:page` + `system:user:list` + `system:user:detail`) | `system:user:create` / `system:user:update` / `system:user:delete` / `system:user:assign-role` / `system:user:reset-password` / `system:user:export` |
| 系统管理 | 角色管理 (`system:role:page` + `system:role:list` + `system:role:detail`) | `system:role:create` / `system:role:update` / `system:role:delete` / `system:role:assign-menu` |
| 系统管理 | 菜单管理 (`system:menu:tree`) | `system:menu:create` / `system:menu:update` / `system:menu:delete` |
| 系统管理 | 部门管理 (`system:dept:tree`) | `system:dept:simple-list` / `system:dept:create` / `system:dept:update` / `system:dept:delete` |
| 系统管理 | 岗位管理 (`system:post:page` + `system:post:list`) | `system:post:create` / `system:post:update` / `system:post:delete` |
| 系统管理 | 字典管理 (`system:dict:page` + `system:dict:list`) | `system:dict:create` / `system:dict:update` / `system:dict:delete` |
| 系统管理 | 操作日志 (`system:oper-log:page`) | — |
| 系统管理 | 登录日志 (`system:login-log:page`) | — |
| 系统监控 | 预留（`type=M` 占位，暂不放子菜单） | — |
| 工具 | 预留（`type=M` 占位，暂不放子菜单） | — |

> 上表为**速查清单**。任何新增 / 修改权限码，**必须**同时更新 `06 §10.1` 字典表（唯一权威事实源），并通过增量 Flyway 迁移（`V1.0.2__*.sql` 起）落地，**不得**改动已应用的 `V1.0.0__init.sql` 或 `V1.0.1__seed.sql`。

### 5.4 完整种子 SQL 模板（`V1.0.1__seed.sql`）

> 以下 SQL 按字典表权限码 **逐字** 写。智能体**禁止**自创菜单 id 之外的字段命名。

```sql
-- ==========================================================
-- V1.0.1__seed.sql  种子数据
-- 菜单 id 区间：1~99 一级目录；100~199 系统管理；200~299 系统监控；300~399 工具
-- ==========================================================

-- ---------- 1. 一级目录 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, icon, sort, visible, status, create_by, create_time) VALUES
(1,  '系统管理', 'M', 0, '/system',   'Layout',          'Setting',   1, 1, 1, 1, NOW()),
(2,  '系统监控', 'M', 0, '/monitor',  'Layout',          'Monitor',   2, 1, 1, 1, NOW()),
(3,  '工具',     'M', 0, '/tool',     'Layout',          'Tools',     3, 1, 1, 1, NOW());

-- ---------- 2. 系统管理 → 用户管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(100, '用户管理', 'C', 1, '/system/user', 'system/user/index', 'system:user:page', 'User', 1, 1, 1, 1, NOW()),
(101, '用户查询', 'F', 100, NULL, NULL, 'system:user:page',           NULL, 1, 1, 1, 1, NOW()),
(102, '用户列表', 'F', 100, NULL, NULL, 'system:user:list',           NULL, 2, 1, 1, 1, NOW()),
(103, '用户详情', 'F', 100, NULL, NULL, 'system:user:detail',         NULL, 3, 1, 1, 1, NOW()),
(104, '新增用户', 'F', 100, NULL, NULL, 'system:user:create',         NULL, 4, 1, 1, 1, NOW()),
(105, '编辑用户', 'F', 100, NULL, NULL, 'system:user:update',         NULL, 5, 1, 1, 1, NOW()),
(106, '删除用户', 'F', 100, NULL, NULL, 'system:user:delete',         NULL, 6, 1, 1, 1, NOW()),
(107, '分配角色', 'F', 100, NULL, NULL, 'system:user:assign-role',    NULL, 7, 1, 1, 1, NOW()),
(108, '重置密码', 'F', 100, NULL, NULL, 'system:user:reset-password', NULL, 8, 1, 1, 1, NOW()),
(109, '导出用户', 'F', 100, NULL, NULL, 'system:user:export',         NULL, 9, 1, 1, 1, NOW());

-- ---------- 3. 系统管理 → 角色管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(110, '角色管理', 'C', 1, '/system/role', 'system/role/index', 'system:role:page', 'UserFilled', 2, 1, 1, 1, NOW()),
(111, '角色分页', 'F', 110, NULL, NULL, 'system:role:page',         NULL, 1, 1, 1, 1, NOW()),
(112, '角色列表', 'F', 110, NULL, NULL, 'system:role:list',         NULL, 2, 1, 1, 1, NOW()),
(113, '新增角色', 'F', 110, NULL, NULL, 'system:role:create',       NULL, 3, 1, 1, 1, NOW()),
(114, '编辑角色', 'F', 110, NULL, NULL, 'system:role:update',       NULL, 4, 1, 1, 1, NOW()),
(115, '删除角色', 'F', 110, NULL, NULL, 'system:role:delete',       NULL, 5, 1, 1, 1, NOW()),
(116, '分配菜单', 'F', 110, NULL, NULL, 'system:role:assign-menu',  NULL, 6, 1, 1, 1, NOW());

-- ---------- 4. 系统管理 → 菜单管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(120, '菜单管理', 'C', 1, '/system/menu', 'system/menu/index', 'system:menu:tree', 'Menu', 3, 1, 1, 1, NOW()),
(121, '菜单查询', 'F', 120, NULL, NULL, 'system:menu:tree',   NULL, 1, 1, 1, 1, NOW()),
(122, '新增菜单', 'F', 120, NULL, NULL, 'system:menu:create', NULL, 2, 1, 1, 1, NOW()),
(123, '编辑菜单', 'F', 120, NULL, NULL, 'system:menu:update', NULL, 3, 1, 1, 1, NOW()),
(124, '删除菜单', 'F', 120, NULL, NULL, 'system:menu:delete', NULL, 4, 1, 1, 1, NOW());

-- ---------- 5. 系统管理 → 部门管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(130, '部门管理', 'C', 1, '/system/dept', 'system/dept/index', 'system:dept:tree', 'OfficeBuilding', 4, 1, 1, 1, NOW()),
(131, '部门查询', 'F', 130, NULL, NULL, 'system:dept:tree',        NULL, 1, 1, 1, 1, NOW()),
(132, '部门下拉', 'F', 130, NULL, NULL, 'system:dept:simple-list', NULL, 2, 1, 1, 1, NOW()),
(133, '新增部门', 'F', 130, NULL, NULL, 'system:dept:create',      NULL, 3, 1, 1, 1, NOW()),
(134, '编辑部门', 'F', 130, NULL, NULL, 'system:dept:update',      NULL, 4, 1, 1, 1, NOW()),
(135, '删除部门', 'F', 130, NULL, NULL, 'system:dept:delete',      NULL, 5, 1, 1, 1, NOW());

-- ---------- 6. 系统管理 → 岗位管理 ----------
-- 字典表登记：page / list / create / update / delete（与 06 第 10.1 节一致）
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(140, '岗位管理', 'C', 1, '/system/post', 'system/post/index', 'system:post:page', 'Postcard', 5, 1, 1, 1, NOW()),
(141, '岗位查询', 'F', 140, NULL, NULL, 'system:post:page',   NULL, 1, 1, 1, 1, NOW()),
(142, '岗位下拉', 'F', 140, NULL, NULL, 'system:post:list',   NULL, 2, 1, 1, 1, NOW()),
(143, '新增岗位', 'F', 140, NULL, NULL, 'system:post:create', NULL, 3, 1, 1, 1, NOW()),
(144, '编辑岗位', 'F', 140, NULL, NULL, 'system:post:update', NULL, 4, 1, 1, 1, NOW()),
(145, '删除岗位', 'F', 140, NULL, NULL, 'system:post:delete', NULL, 5, 1, 1, 1, NOW());

-- ---------- 7. 系统管理 → 字典管理 ----------
-- 字典表登记：page / list / create / update / delete（与 06 第 10.1 节一致）
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(150, '字典管理', 'C', 1, '/system/dict', 'system/dict/index', 'system:dict:page', 'Collection', 6, 1, 1, 1, NOW()),
(151, '字典查询', 'F', 150, NULL, NULL, 'system:dict:page',   NULL, 1, 1, 1, 1, NOW()),
(152, '字典下拉', 'F', 150, NULL, NULL, 'system:dict:list',   NULL, 2, 1, 1, 1, NOW()),
(153, '新增字典', 'F', 150, NULL, NULL, 'system:dict:create', NULL, 3, 1, 1, 1, NOW()),
(154, '编辑字典', 'F', 150, NULL, NULL, 'system:dict:update', NULL, 4, 1, 1, 1, NOW()),
(155, '删除字典', 'F', 150, NULL, NULL, 'system:dict:delete', NULL, 5, 1, 1, 1, NOW());

-- ---------- 8. 系统管理 → 操作日志 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(160, '操作日志', 'C', 1, '/system/oper-log', 'system/oper-log/index', 'system:oper-log:page', 'Document', 7, 1, 1, 1, NOW()),
(161, '日志查询', 'F', 160, NULL, NULL, 'system:oper-log:page', NULL, 1, 1, 1, 1, NOW());

-- ---------- 9. 系统管理 → 登录日志 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(170, '登录日志', 'C', 1, '/system/login-log', 'system/login-log/index', 'system:login-log:page', 'Lock', 8, 1, 1, 1, NOW()),
(171, '日志查询', 'F', 170, NULL, NULL, 'system:login-log:page', NULL, 1, 1, 1, 1, NOW());

-- ---------- 10. 角色 ----------
-- admin 拥有 data_scope=1（全部）；common 拥有 data_scope=4（仅本人）
INSERT INTO sys_role (id, name, code, data_scope, sort, status, create_by, create_time) VALUES
(1, '超级管理员', 'admin',  1, 1, 1, 1, NOW()),
(2, '普通用户',   'common', 4, 2, 1, 1, NOW());

-- admin 拥有全部菜单权限（1~185）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE del_flag = 0;

-- common 仅有：首页 / 操作日志 / 登录日志 查询权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 160), (2, 161), (2, 170), (2, 171);

-- ---------- 11. 部门 / 岗位 ----------
INSERT INTO sys_dept (id, name, parent_id, sort, status, create_by, create_time) VALUES
(1, '总公司', 0, 1, 1, 1, NOW()),
(2, '研发部', 1, 1, 1, 1, NOW()),
(3, '产品部', 1, 2, 1, 1, NOW());

INSERT INTO sys_post (id, code, name, dept_id, sort, status, create_by, create_time) VALUES
(1, 'ceo',     '超级管理员', 1, 1, 1, 1, NOW()),
(2, 'rd',      '研发工程师', 2, 1, 1, 1, NOW()),
(3, 'product', '产品经理',   3, 1, 1, 1, NOW());

-- ---------- 12. admin 用户 ----------
-- password = admin1234 的 BCrypt 哈希（cost=10）
-- 实际部署时由 `make seed` 或启动器重新哈希；此处 hash 由 `BCrypt.hashpw('admin1234', BCrypt.gensalt(10))` 生成。
INSERT INTO sys_user (id, username, password, nickname, real_name, status, dept_id, post_id, create_by, create_time) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超管', '超级管理员', 1, 1, 1, 1, NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_post (user_id, post_id) VALUES (1, 1);

-- ---------- 13. 字典 ----------
INSERT INTO sys_dict (id, name, type, status, remark, create_by, create_time) VALUES
(1, '用户性别',   'sys_user_sex',      1, '用户性别列表',       1, NOW()),
(2, '系统状态',   'sys_common_status', 1, '正常/停用',         1, NOW()),
(3, '操作状态',   'sys_oper_status',   1, '成功/失败',         1, NOW()),
(4, '菜单类型',   'sys_menu_type',     1, '目录/菜单/按钮',     1, NOW()),
(5, '数据权限',   'sys_data_scope',    1, '5 级数据权限范围',   1, NOW()),
(6, '系统是否',   'sys_yes_no',        1, '是/否',             1, NOW());

INSERT INTO sys_dict_item (dict_type, label, value, sort, status, create_by, create_time) VALUES
-- sys_user_sex
('sys_user_sex',      '未知', '0', 1, 1, 1, NOW()),
('sys_user_sex',      '男',   '1', 2, 1, 1, NOW()),
('sys_user_sex',      '女',   '2', 3, 1, 1, NOW()),
-- sys_common_status
('sys_common_status', '正常', '1', 1, 1, 1, NOW()),
('sys_common_status', '停用', '0', 2, 1, 1, NOW()),
-- sys_oper_status
('sys_oper_status',   '成功', '1', 1, 1, 1, NOW()),
('sys_oper_status',   '失败', '0', 2, 1, 1, NOW()),
-- sys_menu_type
('sys_menu_type',     '目录', 'M', 1, 1, 1, NOW()),
('sys_menu_type',     '菜单', 'C', 2, 1, 1, NOW()),
('sys_menu_type',     '按钮', 'F', 3, 1, 1, NOW()),
-- sys_data_scope
('sys_data_scope',    '全部',         '1', 1, 1, 1, NOW()),
('sys_data_scope',    '本部门及下级', '2', 2, 1, 1, NOW()),
('sys_data_scope',    '本部门',       '3', 3, 1, 1, NOW()),
('sys_data_scope',    '仅本人',       '4', 4, 1, 1, NOW()),
('sys_data_scope',    '自定义',       '5', 5, 1, 1, NOW()),
-- sys_yes_no
('sys_yes_no',        '是', 'Y', 1, 1, 1, NOW()),
('sys_yes_no',        '否', 'N', 2, 1, 1, NOW());

-- ---------- 14. 系统参数配置 ----------
INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, remark, create_by, create_time) VALUES
(1, '用户初始密码',       'sys.user.initPassword',    '123456',     'Y', '新用户默认初始密码',                1, NOW()),
(2, '登录验证码开关',     'sys.login.captchaEnabled', 'true',       'Y', '登录时是否显示图形验证码',          1, NOW()),
(3, '登录失败锁定次数',   'sys.login.retryLimit',     '5',          'N', '同一用户名密码连续输错锁定次数',    1, NOW()),
(4, '上传文件大小上限',   'sys.upload.maxSize',       '10',         'N', '上传文件大小上限(MB)',              1, NOW()),
(5, '系统首页皮肤',       'sys.index.skinName',       'skin-blue',  'N', '系统首页皮肤',                      1, NOW());
```

> 完整 DDL 在 `qkit-admin/src/main/resources/db/migration/V1.0.0__init.sql`，种子数据在 `V1.0.1__seed.sql`。
> admin 密码哈希为示例 hash，**生产部署必须**用 `BCrypt.hashpw('新密码', 10)` 重新生成后替换。

## 6. JSON vs 关系型权衡

| 数据 | 选择 | 理由 |
|---|---|---|
| 表结构扩展字段 | JSON 字段 | 灵活，但**本期不用**（KISS） |
| 菜单扩展配置（如 keepAlive） | JSON 字段 | 预留 `meta JSON` |
| 操作日志入参 | TEXT（存 JSON 字符串） | 全文索引不是核心诉求 |
| 字典扩展配置（css_class） | 独立列 | 简单字段不上 JSON |
| 角色多部门 | 关联表 | 关系型强，需独立查询 |

## 7. 后续表添加规范

新增业务表时：

1. 必须含主键 `id` + 公共 5 列（`create_by / create_time / update_by / update_time / del_flag`）
2. 表名 `<prefix>_<module>_<entity>`，如 `b_trade_order`
3. 写 Flyway `V<yyyy>.<mm>.<dd>.<seq>__<comment>.sql`
4. 公共列在 DDL 中显式写，**不**用自动生成
5. 索引在 DDL 末尾追加，命名 `idx_xxx` / `uk_xxx`
6. 注释必填
