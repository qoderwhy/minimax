# 05 · 接口规范

> 所有 Controller 必须遵循本文档的路径、方法命名、入参、出参、错误码规范。
> 前端 axios 封装也以本文档的响应结构为基础。

## 1. 统一响应：R

```java
@Data
public class R<T> {
    private Integer code;       // 200=成功，其他=失败
    private String message;     // 提示信息（成功固定为 "ok"）
    private T data;             // 业务数据
    private Long total;         // 分页总数（仅分页接口）
    private Long pageNum;       // 当前页
    private Long pageSize;      // 每页条数
    private String traceId;     // 链路 ID，便于排查
}
```

### 1.1 静态方法

```java
R.ok();                              // 成功无数据
R.ok(data);                          // 成功 + 单对象
R.ok(data, total, pageNum, pageSize); // 分页
R.fail(code, message);               // 失败
R.fail(ErrorCode.XXX);               // 失败（枚举重载）
```

### 1.2 分页响应

```json
{
  "code": 200,
  "message": "ok",
  "data": [ /* VO 列表 */ ],
  "total": 100,
  "pageNum": 1,
  "pageSize": 10
}
```

> 字段名遵循前端常用命名（`total` / `pageNum` / `pageSize`），不用 MP 的 `records / current / size`。

## 2. 错误码（ErrorCode 枚举）

```java
public enum ErrorCode {
    SUCCESS(200, "成功"),

    // 通用（HTTP 语义）
    BAD_REQUEST(400, "请求参数错误"),
    TIME_FORMAT_INVALID(400, "时间格式不正确"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    VALIDATION_FAILED(422, "参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    REPEAT_SUBMIT(429, "请勿重复提交，请稍后再试"),
    INTERNAL_ERROR(500, "系统异常，请联系管理员"),

    // 用户 100xx
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PASSWORD_ERROR(10002, "用户名或密码错误"),
    USER_DISABLED(10003, "用户已被停用"),
    USER_LOCKED(10004, "账号已锁定，请稍后再试"),
    USER_EXISTS(10005, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(10006, "用户名或密码错误"),
    USER_CANNOT_DELETE_SELF(10007, "不能删除当前登录用户"),
    USER_CANNOT_DISABLE_SELF(10008, "不能停用当前登录用户"),
    USER_PROTECTED(10009, "系统内置管理员账号，不允许操作"),

    // 验证码 101xx
    CAPTCHA_INVALID(10101, "验证码错误或已过期"),
    CAPTCHA_REQUIRED(10102, "请输入验证码"),

    // 角色 110xx
    ROLE_NOT_FOUND(11001, "角色不存在"),
    ROLE_IN_USE(11002, "角色已分配用户，无法删除"),
    ROLE_SYSTEM_PROTECTED(11003, "系统内置角色，不允许操作"),
    ROLE_EXISTS(11004, "角色编码已存在"),

    // 菜单 / 部门
    MENU_HAS_CHILDREN(12001, "存在子菜单，无法删除"),
    MENU_PARENT_INVALID(12002, "上级菜单不能是自身或其下级"),
    DEPT_HAS_CHILDREN(13001, "存在子部门，无法删除"),
    DEPT_HAS_USER(13002, "部门下存在用户，无法删除"),
    DEPT_PARENT_INVALID(13003, "上级部门不能是自身或其下级"),

    // 字典 / 岗位 / 导出 / 参数
    DICT_HAS_ITEMS(14001, "字典下存在字典项，无法删除"),
    POST_IN_USE(15001, "岗位已分配用户，无法删除"),
    EXPORT_ERROR(16001, "导出失败，请稍后重试"),
    EXPORT_LIMIT_EXCEEDED(16002, "导出数据量超过上限，请缩小查询范围后重试"),
    CONFIG_KEY_EXISTS(17001, "参数键名已存在"),
    CONFIG_BUILTIN(17002, "系统内置参数，不允许删除或修改键名");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

> 新增错误码必须追加在对应分组，**禁止**散乱编号；服务于同一语义时允许复用码值（如 `TOO_MANY_REQUESTS` / `REPEAT_SUBMIT` 均为 429，`BAD_REQUEST` / `TIME_FORMAT_INVALID` 均为 400）。

## 3. 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + " " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler(NotPermissionException.class) // Sa-Token
    public R<Void> handleNoPerm(NotPermissionException e) {
        return R.fail(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNoLogin(NotLoginException e) {
        return R.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(SystemException.class)
    public R<Void> handleSystem(SystemException e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + " " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleAny(Exception e) {
        log.error("系统异常", e);
        return R.fail(ErrorCode.INTERNAL_ERROR);
    }
}
```

## 4. URL 路径规范

### 4.1 前缀

- **管理后台**：`/admin-api/<module>/<resource>`
- **统一前缀**：`/admin-api`

> 本期**不**用 `/api/v1` 前缀；后续若破坏性变更用 `v2` 子路径。

### 4.2 模块名（与 sys_* 表对应）

| 模块名 | 说明 |
|---|---|
| `auth` | 登录、登出、验证码、用户信息 |
| `system/user` | 用户管理 |
| `system/role` | 角色管理 |
| `system/menu` | 菜单管理 |
| `system/dept` | 部门管理 |
| `system/post` | 岗位管理 |
| `system/dict` | 字典管理 |
| `system/config` | 系统参数配置 |
| `system/oper-log` | 操作日志 |
| `system/login-log` | 登录日志 |

## 5. Controller 标准动词

| 动词 | HTTP | 路径示例 | 入参 | 出参 | 说明 |
|---|---|---|---|---|---|
| `page` | GET | `/admin-api/system/user/page` | `@RequestParam pageNum, pageSize, ...Query` | `R<List<UserVO>>(total, pageNum, pageSize)` | 分页列表（带条件） |
| `list` | GET | `/admin-api/system/role/list` | `@RequestParam ...Query` | `R<List<RoleVO>>` | 简单列表（无分页） |
| `detail` | GET | `/admin-api/system/user/detail/{id}` | `@PathVariable id` | `R<UserVO>` | 详情 |
| `create` | POST | `/admin-api/system/user/create` | `@RequestBody @Valid UserSaveDTO` | `R<Long>` 返回 ID | 新增 |
| `update` | PUT | `/admin-api/system/user/update` | `@RequestBody @Valid UserSaveDTO` | `R<Boolean>` | 更新 |
| `delete` | DELETE | `/admin-api/system/user/delete` | `@RequestBody List<Long> ids` | `R<Boolean>` | 批量删除 |
| `export` | GET | `/admin-api/system/user/export` | `@RequestParam ...Query` | 文件流 | 导出 Excel |
| `simple-list` | GET | `/admin-api/system/dept/simple-list` | — | `R<List<DeptSimpleVO>>` | 下拉专用简化 VO |
| `assign-role` | PUT | `/admin-api/system/user/assign-role` | `@RequestParam userId, @RequestBody List<Long> roleIds` | `R<Boolean>` | 分配角色（仅限 user 资源） |
| `reset-password` | PUT | `/admin-api/system/user/reset-password` | `@RequestBody @Valid UserResetPasswordDTO{userId,newPassword}` | `R<Boolean>` | 重置密码（仅限 user 资源） |
| `assign-menu` | PUT | `/admin-api/system/role/assign-menu` | `@RequestParam roleId, @RequestBody List<Long> menuIds` | `R<Boolean>` | 分配菜单（仅限 role 资源） |
| `assign-dept` | PUT | `/admin-api/system/role/assign-dept` | `@RequestParam roleId, @RequestBody List<Long> deptIds` | `R<Boolean>` | 分配数据权限部门（仅限 role 资源） |
| `clean` | DELETE | `/admin-api/system/oper-log/clean` | — | `R<Boolean>` | 清空日志（oper-log / login-log） |

> 增删改查及业务动作的核心动词为：`page` / `list` / `detail` / `create` / `update` / `delete` / `export` / `simple-list` / `assign-role` / `reset-password` / `assign-menu`（另有资源专属动作 `assign-dept`、`clean` 等，见上表）。**实际动作以各 Controller 与 06 第 10.1 节权限码字典表为准，二者必须一致**。`POST /create` 与 `PUT /update` 是 Spring 风格（`@PostMapping` / `@PutMapping`），不混用 `@GetMapping` 改资源。

### 5.1 ⛔ URL 命名禁忌 6 条（违反即不通过 Code Review）

| 禁词 | 理由 | 应改为 |
|---|---|---|
| `get` / `query` / `find` / `fetch` / `search` | 模糊（不知道返回什么）；与 HTTP GET 语义重叠 | `detail` / `page` / `list` / `simple-list` |
| `add` / `insert` / `save` | 模糊（"保存"既可新增又可更新） | `create`（新增） / `update`（更新） |
| `edit` / `modify` | 模糊（编辑 ≠ 修改） | `update` |
| `del` / `remove` | 缩写不一致 | `delete` |
| `info` / `data` / `listAll` | 含义不明 | `detail` / `page` / `list` |
| `enable` / `disable` | "改状态"不专一 | `update` + body 带 `status` 字段 |

> **铁律**：Controller 方法名 = 路径最后一段 = 表第 1 列动词。三者完全一致。

## 6. 请求头

| Header | 用途 | 示例 |
|---|---|---|
| `satoken` | Sa-Token token 原文 | `<token>`（前端 `request.ts` 拦截器自动加，**不加** `Bearer` 前缀） |
| `Content-Type` | 请求类型 | `application/json;charset=UTF-8` |
| `X-Trace-Id` | 链路 ID（可选） | UUID |
| `Authorization` | **不用**（Sa-Token 用自定义头） | — |

### 6.1 前端 Axios 拦截器

```ts
request.interceptors.request.use((config) => {
  const token = useUserStore().token
  if (token) config.headers['satoken'] = token      // 直接透传 token 原文，无 Bearer 前缀
  return config
})

request.interceptors.response.use(
  (resp) => {
    const r = resp.data as R
    if (r === null || typeof r !== 'object' || !('code' in r)) return resp  // 文件流等原样返回
    if (r.code === 200) return resp                 // 成功返回完整 axios response
    if (r.code === 401) { /* 跳登录一次 */ }
    ElMessage.error(r.message || '请求失败')
    return Promise.reject(r)
  },
  (err) => {
    ElMessage.error(err?.message || '网络异常')
    return Promise.reject(err)
  }
)
```

> 成功时拦截器返回的是**完整 axios response**，业务层再通过 `requestData()` 取 `resp.data.data`；`http.page()` 取 `resp.data.data` + `resp.data.total`。视图层不直接消费 `R`。

## 7. 分页约定

### 7.1 入参

- `pageNum`：从 1 开始
- `pageSize`：默认 10，最大 200
- 其它 Query 字段平铺（不用 wrapper 对象）

### 7.2 出参

```json
{
  "code": 200,
  "message": "ok",
  "data": [ ... ],
  "total": 100,
  "pageNum": 1,
  "pageSize": 10
}
```

### 7.3 Service 实现

```java
Page<UserVO> result = userMapper.selectUserPage(query, Page.of(query.getPageNum(), query.getPageSize()));
return R.ok(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
```

## 8. 跨域（CORS）

`qkit-framework` 的 `WebConfig`：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOriginPatterns)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("satoken")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

> 默认**仅放行本机来源**；生产通过 `app.cors.allowed-origin-patterns` 指定域名，**禁止**用 `*`（与 `allowCredentials(true)` 冲突）。

## 9. 幂等性

- 创建/更新接口天然幂等（基于 ID 判重）
- 涉及"扣减"等场景再加幂等键 `Idempotency-Key` header（本期不涉及）

## 10. 限流

- 登录接口：5 次失败锁 10 分钟（自定义 `LoginRateLimiter`，基于 Redis 计数）
- 其它接口：本期不限流，预留 Redisson 接入点

## 11. 接口文档

- 全部 Controller 加 `@Tag(name = "用户管理")`
- 全部方法加 `@Operation(summary = "分页查询用户")`
- 启用 springdoc-openapi + knife4j
- 访问：`http://localhost:8080/doc.html`（Knife4j UI）

## 12. OpenAPI 导出

`qkit-admin` 提供 `/v3/api-docs` 端点，前端 codegen 工具可基于此自动生成 TS 类型。
