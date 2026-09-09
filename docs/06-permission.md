# 06 · 权限与安全

> 权限是脚手架的核心。RBAC 菜单权限 + 按钮权限 + 数据权限 5 级必须实现。
> 安全红线（密码、登录失败锁定、敏感日志）必须满足。

## 1. RBAC 模型

### 1.1 三层资源

| 资源 | 表 | 标识 |
|---|---|---|
| 菜单 | `sys_menu` | `path` 路由 |
| 按钮 | `sys_menu.type=F` | `perm` 权限字符串 |
| 数据 | 业务表 | `data_scope` 数据权限范围 |

### 1.2 菜单类型

| type | 含义 | path | component | perm | 用途 |
|---|---|---|---|---|---|
| M | 目录 | `/system` | `Layout` | — | 侧边栏分组 |
| C | 菜单 | `/system/user` | `system/user/index` | 可选（C 类型表示"查看权限"时填，如 `system:user:page`） | 真实页面 |
| F | 按钮 | — | — | **必填**（如 `system:user:create`） | 按钮级权限 |

> **设计取舍**：C 类菜单的 `perm` 表示"是否可访问此菜单页"，对应"查看权限"。04 §4.7 DDL 允许 C 类 perm 留空（仅做菜单分类不做访问控制），但**推荐**填与该菜单下首个 F 按钮一致的 perm（如 `system:user:page`），便于 Sa-Token 路由拦截。

## 2. 权限标识规范

格式：`<module>:<resource>:<action>`

- `module`：与菜单一级目录一致（小写），如 `system`
- `resource`：资源名（小写），如 `user`
- `action`：动词（小写），与 05 第 5 节 Controller 标准动词**一一对应**（11 个）：`page` / `list` / `detail` / `create` / `update` / `delete` / `export` / `simple-list` / `assign-role` / `reset-password` / `assign-menu`

示例：
- `system:user:page`（用户分页列表）
- `system:user:list`（用户简单列表）
- `system:user:detail`（用户详情）
- `system:user:create`（新增用户）
- `system:user:update`（编辑用户）
- `system:user:delete`（删除用户）
- `system:user:export`（导出用户）
- `system:user:assign-role`（分配角色）
- `system:user:reset-password`（重置密码）

> 智能体生成按钮时必须遵循此命名。

## 3. Sa-Token 集成

### 3.1 配置

```yaml
sa-token:
  token-name: satoken          # 前端 Header 名（见 05 第 6 节）
  timeout: 86400              # 24h 过期
  active-timeout: 1800        # 30min 无操作失效
  is-concurrent: true         # 允许多端登录
  is-share: false
  token-style: uuid
  is-read-cookie: false
  is-read-header: true
```

### 3.2 StpInterfaceImpl（权限/角色获取）

```java
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    /** 权限列表 */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getUserPermissions(Long.parseLong(loginId.toString()));
    }

    /** 角色列表 */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionService.getUserRoleCodes(Long.parseLong(loginId.toString()));
    }

    /** 数据权限范围（5 级） */
    public DataScope getDataScope(Long userId) {
        return permissionService.getDataScope(userId);
    }
}
```

### 3.3 注解用法

```java
// 推荐做法：不在类级别加 @SaCheckPermission，而是在每个方法上加 @SaCheckPermission("xxx:xxx:<action>")。
// 原因：权限码字典表的粒度是 module:resource:action 三段，类级别只能表达 module:resource 两段，无法表达 action 维度（page/list/create/update/delete ...），会与字典表粒度不匹配。
@RestController
@RequestMapping("/admin-api/system/user")
public class UserController {

    // 方法级别
    @GetMapping("/page")
    @SaCheckPermission("system:user:page")
    public R<List<UserVO>> page(UserQueryDTO query) { ... }

    @PostMapping("/create")
    @SaCheckPermission("system:user:create")
    @OperLog(module = "用户管理", name = "新增用户")
    public R<Long> create(@RequestBody @Valid UserSaveDTO dto) { ... }
}
```

## 4. 数据权限（5 级）

| 值 | 范围 | SQL 拦截 |
|---|---|---|
| 1 | 全部 | 不过滤 |
| 2 | 本部门及下级 | `dept_id IN (子部门列表)` |
| 3 | 本部门 | `dept_id = 当前用户部门` |
| 4 | 仅本人 | `create_by = 当前用户 ID` |
| 5 | 自定义 | `dept_id IN (sys_role_dept 中该角色绑定的部门)` |

### 4.1 实现方案

> 本期**MVP 不强制**所有业务表都接入数据权限，**只**为 `sys_user` 演示完整链路。

#### 4.1.1 DataScope 注解（位于 `qkit-framework/security/annotation/DataScope.java`）

```java
package com.qkit.framework.security.annotation;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /** 表别名，SQL 中 AS 的别名 */
    String alias() default "";
    /** 部门字段名，默认 dept_id */
    String deptColumn() default "dept_id";
    /** 用户字段名，默认 create_by */
    String userColumn() default "create_by";
}
```

#### 4.1.2 DataScopeAspect（基于 MyBatis-Plus 拦截器改写 SQL）

```java
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        // 1. 取当前用户
        Long userId = StpUtil.getLoginAsLongId();
        DataScopeEnum scope = getDataScope(userId);
        // 2. 注入 MyBatis-Plus DataScopeInterceptor
        //    简化版：直接在执行前用 ThreadLocal 传上下文
        DataScopeContext.set(new DataScopeContext.Scope(
            scope, dataScope.alias(), dataScope.deptColumn(), dataScope.userColumn()
        ));
        try {
            return pjp.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
```

#### 4.1.3 使用

```java
@DataScope(alias = "u", deptColumn = "dept_id", userColumn = "create_by")
public Page<UserVO> page(UserQueryDTO query) {
    return userMapper.selectUserPage(query, page);
}
```

#### 4.1.4 SQL 拼接示例（5 级对应改写）

> `DataScopeContext` 拿到 scope 后，在 MyBatis-Plus `DataScopeInterceptor`（或自定义 SQL 改写器）里按以下规则改写：

| scope | 改写片段（追加到 WHERE） | 假设上下文 |
|---|---|---|
| 1 全部 | 不过滤 | 当前用户 dataScope=1 |
| 2 本部门及下级 | `AND {alias}.dept_id IN (子部门列表)` | 当前用户 dept_id=10，递归子部门=[10,11,12] |
| 3 本部门 | `AND {alias}.dept_id = 10` | 当前用户 dept_id=10 |
| 4 仅本人 | `AND {alias}.create_by = 100` | 当前用户 id=100 |
| 5 自定义 | `AND {alias}.dept_id IN (sys_role_dept 中 role_id=当前用户角色绑定的部门)` | 查 `sys_role_dept` 取部门集合 |

**别名规则**：`alias` 为注解传入（例 `u`），最终 SQL 形如：

```sql
-- 原始
SELECT u.* FROM sys_user u WHERE u.del_flag = 0
-- scope=2 改写后
SELECT u.* FROM sys_user u WHERE u.del_flag = 0 AND u.dept_id IN (10, 11, 12)
-- scope=4 改写后
SELECT u.* FROM sys_user u WHERE u.del_flag = 0 AND u.create_by = 100
```

**子部门递归**（scope=2）实现要点：

```java
// qkit-system/.../utils/DeptHelper.java
public static List<Long> getChildDeptIds(Long rootDeptId) {
    // 1. 一次性查出所有部门（缓存 5 分钟）
    // 2. parent_id 自下而上反查 rootDeptId
    // 3. 返回 root + 所有子部门 id 集合
    // 注意：禁止在 SQL 改写器里再发 SQL（递归调用会触发拦截器）
}
```

**反例（禁止）**：

- 在 `Mapper.xml` 里硬编码 `AND create_by = #{currentUserId}` → 绕过 scope 配置，无法切换
- 子部门用 SQL 递归查询（`WITH RECURSIVE`）→ 改写器里发 SQL 会触发自身拦截器死循环
- 跨表 JOIN 时多个 `@DataScope` 注解未指定不同 alias → WHERE 条件冲突

## 5. 动态路由与按钮权限

### 5.1 后端：菜单路由接口

```java
@GetMapping("/route")
public R<List<RouteVO>> getRoute() {
    return R.ok(menuService.getCurrentUserRoute());
}
```

`RouteVO`：
```java
{
  "id": 1,
  "name": "SystemUser",
  "path": "/system/user",
  "component": "system/user/index",
  "meta": {
    "title": "用户管理",
    "icon": "User",
    "hidden": false,
    "keepAlive": true
  },
  "children": [...]
}
```

### 5.2 前端：动态路由生成

```ts
// stores/permission.ts
const dynamicRoutes = transformMenuToRoutes(menus)
router.addRoute(dynamicRoutes)
```

### 5.3 按钮级指令

```ts
// directives/permission.ts
app.directive('permission', {
  mounted(el, binding) {
    applyPermission(el, binding)
  },
  updated(el, binding) {
    // 处理 v-if / el-tab-pane 切换时 store 尚未就绪的场景
    applyPermission(el, binding)
  }
})

function applyPermission(el: HTMLElement, binding: DirectiveBinding) {
  const value = binding.value
  if (!value) return
  const perms = usePermissionStore().buttons
  const allow = Array.isArray(value)
    ? value.some((v) => perms.includes(v))
    : perms.includes(value)
  if (!allow) el.parentNode?.removeChild(el)
}
```

使用：
```html
<el-button v-permission="['system:user:create']" @click="onCreate">新增</el-button>
```

## 6. 密码与登录安全

### 6.1 密码加密

- 算法：BCrypt（Sa-Token 自带 `cn.dev33.satoken.secure.BCrypt`，`hashpw` / `checkpw`）
- 禁止明文、禁止 MD5、禁止自定义算法
- 密码字段在 DTO 中 `String password`，**禁止**在 VO 中返回

### 6.2 登录失败锁定

- 5 次失败 → 锁定 10 分钟
- 计数存 Redis：key = `login:fail:{username}`，TTL 10 分钟
- 第 6 次请求即使密码正确也拒绝

```java
public void validateLoginFailCount(String username) {
    String key = "login:fail:" + username;
    Integer count = redis.get(key);
    if (count != null && count >= 5) {
        throw new BusinessException(ErrorCode.USER_LOCKED);
    }
}

public void onLoginFail(String username) {
    String key = "login:fail:" + username;
    Long count = redis.incr(key, 1);
    if (count == 1) redis.expire(key, 10 * 60);
}

public void onLoginSuccess(String username) {
    redis.delete("login:fail:" + username);
}
```

### 6.3 图形验证码

- 4 位字符（数字 + 字母），3 分钟过期
- 存 Redis：key = `captcha:{uuid}`
- 登录时随请求传 `captchaId` + `captchaCode`

### 6.4 Token 安全

- Sa-Token JWT 模式 + 24h 过期
- 用户主动登出后 token 失效（黑名单）
- 改密码后强制下线所有设备（`StpUtil.logout(userId)`）

## 7. 安全红线（违反即事故）

| # | 红线 |
|---|---|
| S1 | **禁止**明文存密码 |
| S2 | **禁止**前端写死后端地址 / API Key / 密钥 |
| S3 | **禁止**日志输出密码、token、身份证、银行卡 |
| S4 | **禁止**前端 `localStorage` 存敏感数据（token 可用 sessionStorage 替代） |
| S5 | **禁止**接口返回 Entity/DTO 给前端（必须 VO 脱敏） |
| S6 | **禁止**前端按钮只靠隐藏，必须配后端 `@SaCheckPermission` |
| S7 | **禁止** SQL 拼接，所有查询走 MyBatis-Plus |
| S8 | **禁止**未登录就返回数据（除登录/验证码） |
| S9 | **禁止**`@CrossOrigin` 在 Controller 上散落（统一 WebConfig） |
| S10 | **禁止**硬编码密钥 / 加密 salt / JWT secret（走 env） |
| S11 | **禁止**前端用 `v-html` 渲染用户输入（XSS）；如必须，用 `DOMPurify` 清洗后再插入 |
| S12 | **禁止**接口接受未校验来源的"写"请求（CSRF）；Sa-Token 模式下需在 `WebConfig` 加 `SameSite=Lax` Cookie 或校验 `Origin` 头 |
| S13 | **禁止**MyBatis XML 用 `${}` 占位外部入参（SQL 注入）；外部入参一律 `#{}`；LambdaQueryWrapper 用方法调用 |

## 8. 敏感字段过滤

- 用户列表接口 `password` 字段**绝对不能**返回
- 操作日志中 `password` 字段值替换为 `******`
- 前端若误存密码到 store，**立即**清除

## 9. 操作日志红线

- `password` 字段值在 `OperLogAspect` 中**必须**脱敏
- `Authorization / satoken / Cookie` Header **不记录**
- 响应内容超过 2KB 截断
- 异常堆栈超过 4KB 截断

## 10. 权限码字典（前后端单一事实源）

> **硬约束**：前端 `v-permission="['system:user:create']"` 的字符串，**必须**与后端 `@SaCheckPermission("system:user:create")` 的字符串**完全一致**（含大小写、冒号、拼写）。
> 智能体生成新模块时，**先**在权限码字典表登记 → **再**写后端注解 → **再**写前端指令。**禁止**两端各自拼写。

### 10.1 字典表（MVP 完整清单）

> **唯一事实源**：本表与 04 种子 SQL 的 `sys_menu.perm`、05 第 5 节动词表、本节第 2 节 action 清单四方**逐字一致**。

| 权限码 | 资源 | 动作 | 后端位置 | 前端位置 |
|---|---|---|---|---|
| `system:user:page` | 用户 | 分页查询 | `UserController.page` | 用户列表 |
| `system:user:list` | 用户 | 简单列表 | `UserController.list` | 分配角色弹窗下拉 |
| `system:user:detail` | 用户 | 详情 | `UserController.detail` | 「查看详情」按钮 |
| `system:user:create` | 用户 | 新增 | `UserController.create` | 「新增用户」按钮 |
| `system:user:update` | 用户 | 更新 | `UserController.update` | 「编辑用户」按钮 |
| `system:user:delete` | 用户 | 删除 | `UserController.delete` | 「删除用户」按钮 |
| `system:user:assign-role` | 用户 | 分配角色 | `UserController.assignRole` | 「分配角色」按钮 |
| `system:user:reset-password` | 用户 | 重置密码 | `UserController.resetPassword` | 「重置密码」按钮 |
| `system:user:export` | 用户 | 导出 | `UserController.export` | 「导出」按钮 |
| `system:role:page` | 角色 | 分页查询 | `RoleController.page` | 角色列表 |
| `system:role:list` | 角色 | 简单列表 | `RoleController.list` | 角色下拉 |
| `system:role:detail` | 角色 | 详情 | `RoleController.detail` | 「查看详情」按钮 |
| `system:role:create` | 角色 | 新增 | `RoleController.create` | 「新增角色」按钮 |
| `system:role:update` | 角色 | 更新 | `RoleController.update` | 「编辑角色」按钮 |
| `system:role:delete` | 角色 | 删除 | `RoleController.delete` | 「删除角色」按钮 |
| `system:role:assign-menu` | 角色 | 分配菜单 | `RoleController.assignMenu` | 「分配菜单」按钮 |
| `system:menu:tree` | 菜单 | 树形查询 | `MenuController.tree` | 菜单树 |
| `system:menu:create` | 菜单 | 新增 | `MenuController.create` | 「新增菜单」按钮 |
| `system:menu:update` | 菜单 | 更新 | `MenuController.update` | 「编辑菜单」按钮 |
| `system:menu:delete` | 菜单 | 删除 | `MenuController.delete` | 「删除菜单」按钮 |
| `system:dept:tree` | 部门 | 树形查询 | `DeptController.tree` | 部门树 |
| `system:dept:simple-list` | 部门 | 下拉列表 | `DeptController.simpleList` | 用户/岗位表单部门下拉 |
| `system:dept:create` | 部门 | 新增 | `DeptController.create` | 「新增部门」按钮 |
| `system:dept:update` | 部门 | 更新 | `DeptController.update` | 「编辑部门」按钮 |
| `system:dept:delete` | 部门 | 删除 | `DeptController.delete` | 「删除部门」按钮 |
| `system:post:page` | 岗位 | 分页查询 | `PostController.page` | 岗位列表 |
| `system:post:list` | 岗位 | 简单列表 | `PostController.list` | 岗位下拉 |
| `system:post:create` | 岗位 | 新增 | `PostController.create` | 「新增岗位」按钮 |
| `system:post:update` | 岗位 | 更新 | `PostController.update` | 「编辑岗位」按钮 |
| `system:post:delete` | 岗位 | 删除 | `PostController.delete` | 「删除岗位」按钮 |
| `system:dict:page` | 字典 | 分页查询 | `DictController.page` | 字典列表 |
| `system:dict:list` | 字典 | 简单列表 | `DictController.list` | 字典下拉 |
| `system:dict:create` | 字典 | 新增 | `DictController.create` | 「新增字典」按钮 |
| `system:dict:update` | 字典 | 更新 | `DictController.update` | 「编辑字典」按钮 |
| `system:dict:delete` | 字典 | 删除 | `DictController.delete` | 「删除字典」按钮 |
| `system:oper-log:page` | 操作日志 | 查询 | `OperLogController.page` | 操作日志列表 |
| `system:login-log:page` | 登录日志 | 查询 | `LoginLogController.page` | 登录日志列表 |
| `system:config:page` | 参数配置 | 分页查询 | `SysConfigController.page` | 参数配置列表 |
| `system:config:list` | 参数配置 | 简单列表 | `SysConfigController.list` | 参数配置下拉 |
| `system:config:create` | 参数配置 | 新增 | `SysConfigController.create` | 「新增参数」按钮 |
| `system:config:update` | 参数配置 | 更新 | `SysConfigController.update` | 「编辑参数」按钮 |
| `system:config:delete` | 参数配置 | 删除 | `SysConfigController.delete` | 「删除参数」按钮 |

### 10.2 新增权限码流程

1. 在本表追加一行（模块名:资源名:动作）
2. 后端 Controller 方法加 `@SaCheckPermission("新:增:码")`
3. 前端按钮加 `v-permission="['新:增:码']"`
4. 同步在 `sys_menu` 表插入对应按钮菜单（`type=F`，`perm=新:增:码`）

> 任何**不一致**（后端写了注解但前端无 `v-permission`、或前端有按钮但后端无注解）都属于权限漏洞，**必须**在 Code Review 阶段拦截。

## 11. 权限缓存策略

- 用户登录后，Sa-Token Session 存：`loginId` + `userId`
- 权限/角色列表由 `StpInterfaceImpl.getPermissionList()` **懒加载**（每次请求调用）
- **Redis 缓存**：`perm:{userId}` → `List<String>` 权限码集合，TTL 30 分钟
- 变更触发清缓存：用户角色变更 / 角色权限变更 / 菜单变更 → `redis.delete("perm:" + userId)`
- 前端权限（按钮可见性）从 `usePermissionStore.buttons` 拉（启动时调 `/admin-api/auth/perms`）
