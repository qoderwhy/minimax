---
name: qkit-remaining-optimizations
overview: 对 qkit 脚手架剩余的 23 项安全/性能/规范/行为缺陷问题做整体治理，含种子数据修正与前端 7 个列表页的统一重构，按风险分级分批交付并可独立验证。
todos:
  - id: fix-idor-log-daterange
    content: 修复用户详情越权（新增受数据权限约束的详情方法）与日志时间范围筛选（后端条件+前端控件）
    status: completed
  - id: harden-prod-security
    content: 生产加固：关闭接口文档、收敛 actuator、可信代理、Redis 白名单、限流加 IP 维度并接入重试上限参数
    status: completed
    dependencies:
      - fix-idor-log-daterange
  - id: optimize-perf-hotpaths
    content: 性能修复：分页上限、岗位 N+1、导出分批限流、登录去事务、异步线程池
    status: completed
  - id: dict-item-server-paging
    content: 字典项改为后端分页：新增分页接口与查询 DTO，前端表格切换数据源
    status: completed
  - id: cleanup-conventions
    content: 规范清理：8 个转换接口补映射开关、删死配置与死代码、修注释漂移、统一启动预热，用 [skill:lsp-code-analysis] 确认引用清零
    status: completed
    dependencies:
      - optimize-perf-hotpaths
      - dict-item-server-paging
  - id: fix-tree-and-route
    content: 行为缺陷：树查询保留祖先链、上级节点自环与后代校验、路由名唯一化
    status: completed
    dependencies:
      - cleanup-conventions
  - id: fix-seed-data
    content: 修正 V1.0.1 种子：参数设置菜单可见性、初始密码长度、管理员口令三处一致，并附已部署库补救步骤
    status: completed
  - id: unify-list-pages
    content: 用 [subagent:code-explorer] 盘点页面差异后抽 usePagination/useCrud 统一 7 个列表页，并拆分部门类型、修正权限指令语义
    status: completed
    dependencies:
      - dict-item-server-paging
      - cleanup-conventions
  - id: add-core-tests
    content: 引入测试依赖并补数据权限、权限码计算与缓存失效、字典缓存、用户保护逻辑单元测试
    status: completed
    dependencies:
      - fix-idor-log-daterange
      - optimize-perf-hotpaths
  - id: final-verification
    content: 最终校验：后端编译与单测、前端安装依赖后类型检查与 lint、逐页回归清单核对
    status: completed
    dependencies:
      - fix-idor-log-daterange
      - harden-prod-security
      - optimize-perf-hotpaths
      - dict-item-server-paging
      - cleanup-conventions
      - fix-tree-and-route
      - fix-seed-data
      - unify-list-pages
      - add-core-tests
---

## 需求概述

在已完成的两批修复之上，继续清理项目中剩余的安全隐患、性能瓶颈、规范漂移与行为缺陷，并按用户确认的范围一次性编排完成，每批可独立验证与提交。

## 核心功能

- **安全补漏**：修复用户详情越权读取；补齐日志时间范围筛选；生产环境关闭接口文档与收敛 actuator；加固客户端 IP 解析、限流维度与 Redis 反序列化白名单。
- **性能修复**：分页上限；岗位列表消除 N+1；导出改分批并设上限；字典项改为后端分页；登录去掉不必要事务；异步日志线程池可控。
- **规范与技术债**：补全 MapStruct 映射开关；清理死配置与死代码；修正注释漂移；统一启动预热方式；修正按钮权限指令语义。
- **行为缺陷**：树查询结果保留祖先链；上级节点自环/后代校验；动态路由名唯一化。
- **测试补齐**：从零引入测试依赖，覆盖数据权限、权限码计算与缓存失效、字典缓存、用户保护逻辑。
- **种子数据**：修正被误隐藏的参数设置菜单、初始密码长度与登录口令不一致（用户已确认直接修改 V1.0.1，计划中给出已部署库的补救步骤）。
- **前端统一**：抽出分页与增删改查组合式函数，统一 7 个列表页脚本（用户/角色/岗位/字典/参数/操作日志/登录日志），对齐前后端类型，页面视觉与交互保持不变。

## 边界与约束

- 工作区已有 28 个文件的未提交修复，本次必须在其之上增量进行，不得回退。
- 不得重新引入此前判定为“扩大风险”的两处改动：权限服务的多层缓存、以自增读取方式做限流判断。
- 前端重构仅限 7 个列表页；部门与菜单为树形页，不纳入统一重构。
- 视觉设计不发生变化，仅调整代码结构与数据来源。

## 一、技术栈

沿用现有技术栈，不新增框架：后端 Spring Boot 3.2.12 + Java 17 + MyBatis-Plus 3.5.7 + Sa-Token 1.39 + Flyway + MapStruct；前端 Vue 3.4 + Vite 5 + TypeScript 5（strict）+ Element Plus + Pinia。新增依赖仅为测试用途的 `spring-boot-starter-test`（scope=test）。

## 二、实施策略

按“先安全、后性能、再规范、最后重构与测试”的七批推进，每批完成后可独立编译或校验、可独立提交。之所以这样排序：安全项改动小、收益高且不依赖其他批次；性能项会修改被后续批次引用的服务实现（岗位、字典、用户导出），必须先行落地；死代码清理与类型对齐放在重构之前，避免“先重构再删除”的重复劳动；测试放在行为稳定之后，作为最终回归保障。

关键决策与理由：

1. **安全项**

- 用户详情越权：`@DataScope` 切面通过 ThreadLocal + MyBatis-Plus 的 `DataPermissionInterceptor` 生效，直接在 `detail` 上加注解会连带影响个人中心（SELF 级别以 `create_by` 过滤，会把本人记录也过滤掉）。因此新增独立方法 `detailInScope(id)` 承载注解，内部直接调用 `detail(id)`（同类内部调用不会重复走代理，但 ThreadLocal 仍在生效，SQL 会被正确过滤）；原 `detail(id)` 保持无注解，供个人中心使用。不可见时自然查不到记录，返回“用户不存在”。
- 日志时间范围：DTO 字段名保持 `beginTime/endTime`，前端接口层把 `startTime/endTime` 改名对齐，并使用日期时间值格式，避免后端拼接字符串补时分秒。
- 生产加固：`application-prod.yml` 覆盖关闭 springdoc 与 knife4j，actuator 仅暴露 health 且不展示详情；容器编排把后端端口发布到宿主机，因此必须按 profile 关闭而不是依赖网关白名单。
- 限流与 IP：`LoginRateLimiter` 增加 IP 维度计数，读取计数沿用普通读取（不使用自增读取，避免创建无过期时间的键），写入沿用自增并在首次写入时设置过期时间；开关阈值由调用方从系统参数读取后传入，避免框架模块反向依赖业务模块。
- 客户端 IP：解析 `X-Forwarded-For` 前增加可信代理判断，非可信来源直接取 `remoteAddr`。
- Redis 白名单：把 `allowIfBaseType(Object)` 收窄为按包名前缀放行，覆盖项目缓存中实际使用的类型。

2. **性能项**

- 分页上限：给分页插件设置最大条数，与文档声明保持一致。
- 岗位 N+1：复用已修复的用户模块写法，先收集部门 ID 批量查询再组装。
- 导出：使用流式写出并按批读取，设置总行数上限，超限直接失败提示缩小范围，避免整表载入内存。
- 字典项分页：新增分页查询接口与查询 DTO（含默认值补齐方法），前端表格数据源切换到该接口；原全量接口保留给既有调用方。
- 登录事务：去掉登录方法上的事务注解，登录流程只有一次单条更新，本身即原子，去掉后不再让事务跨越密码校验与验证码读取。
- 异步日志：显式配置线程池的核心数、最大数、队列容量与线程名前缀，队列满时由切面兜底捕获并降级为告警。

3. **规范项**

- MapStruct：为 8 个转换接口统一补上“空值策略 + 未映射目标忽略”两开关（组件模型已有）。当前依赖 MyBatis-Plus 的 NOT_NULL 更新策略兜住空值，改为显式空值忽略后行为一致，属消除“靠巧合正确”。
- 预热统一：两种预热方式改为统一使用启动后回调，保证数据库访问发生在上下文就绪之后。
- 死代码：删除无 `@Scheduled` 的调度开关、无调用方的前端接口封装与重复常量，删除前用语义分析确认引用关系，避免误删被间接引用的导出项。
- 权限指令：数组语义由“全部满足”改为“任一满足”（现有调用点均为单值，语义变更对现有行为无影响，同时消除易踩的坑），并在重复更新时避免对已移除节点二次操作。

4. **行为缺陷**

- 树查询：关键字过滤改为“命中节点 + 其祖先链”，不再因父节点未命中而丢掉整棵子树；部门树仍需在构建后叠加数据权限过滤，顺序不能颠倒。
- 上级校验：新增错误码（按现有分段追加），校验上级不能是自身、也不能是自己的后代（用父子映射做一次向上/向下遍历）。
- 路由名：由“路径首字母大写拼接”改为“固定前缀 + 菜单 ID”，保证唯一，避免登出时按名称移除路由误删。

5. **前端重构**

- 新增分页组合式函数与增删改查组合式函数，后者以工厂函数形式提供（字典页需同时实例化“字典类型”与“字典项”两套），统一承载查询态、列表态、加载态、弹窗态与增删改查动作，并支持“编辑回显转换”与“保存后附加动作”两个扩展点（用于用户页的详情回显与保存后分配角色）。各页仅保留模板、列定义与校验规则，脚本行数显著下降。
- 类型对齐：把“简单列表”与“树形节点”两种形状拆成两个类型，消除同一类型被两种后端结构复用的问题。

## 三、架构与复用

不引入新的架构模式。复用既有机制：事务提交后回调用于缓存刷新；`CacheService` 作为统一缓存出口；错误码集中枚举、按分段追加；数据权限沿用既有切面与处理器；前端复用既有权限指令与权限 store。新增的组合式函数只是把现有页面中重复的脚本抽到统一位置，不改变渲染与交互。

## 四、目录结构（增量部分）

```
qkit/
├── pom.xml                                   # [MODIFY] dependencyManagement 增加测试依赖版本管理
├── qkit-common/
│   └── src/main/java/com/qkit/common/
│       ├── api/ErrorCode.java                # [MODIFY] 追加菜单/部门上级校验错误码
│       ├── constant/CacheConstants.java      # [MODIFY] 新增登录失败 IP 维度 key 前缀；修正字典缓存 TTL 注释
│       ├── constant/SecurityConstants.java   # [MODIFY] 合并重复的链路追踪常量，新增 IP 维度阈值
│       └── util/WebUtil.java                 # [MODIFY] 可信代理判断后再解析 X-Forwarded-For
├── qkit-framework/
│   └── src/main/java/com/qkit/framework/
│       ├── mybatis/MybatisPlusConfig.java    # [MODIFY] 分页插件设置最大条数
│       ├── redis/RedisConfig.java            # [MODIFY] 收窄多态类型白名单
│       └── ratelimit/LoginRateLimiter.java   # [MODIFY] 增加 IP 维度与阈值入参；保持只读式计数读取
├── qkit-system/
│   ├── pom.xml                               # [MODIFY] 引入测试依赖
│   └── src/main/java/com/qkit/system/
│       ├── convert/*.java                    # [MODIFY] 8 个转换接口统一补空值策略与未映射忽略
│       ├── controller/admin/DictController.java  # [MODIFY] 新增字典项分页接口
│       ├── domain/dto/DictItemQueryDTO.java  # [NEW] 字典项分页查询入参（含 withPageDefaults）
│       ├── domain/dto/OperLogQueryDTO.java   # [MODIFY] 时间范围字段与校验
│       ├── domain/dto/LoginLogQueryDTO.java  # [MODIFY] 时间范围字段与校验
│       ├── security/datascope/               # 复用，无需改动
│       ├── service/UserService.java          # [MODIFY] 新增受数据权限约束的详情方法
│       ├── service/DictService.java          # [MODIFY] 新增字典项分页方法
│       ├── service/impl/UserServiceImpl.java # [MODIFY] detailInScope；导出分批限流
│       ├── service/impl/DictServiceImpl.java # [MODIFY] 字典项分页；预热方式统一
│       ├── service/impl/SysConfigServiceImpl.java # [MODIFY] 预热方式统一
│       ├── service/impl/PostServiceImpl.java # [MODIFY] 部门名称批量装配，消除 N+1
│       ├── service/impl/OperLogServiceImpl.java   # [MODIFY] 时间范围条件
│       ├── service/impl/LoginLogServiceImpl.java  # [MODIFY] 时间范围条件
│       ├── service/impl/AuthServiceImpl.java # [MODIFY] 去掉登录事务；传入限流阈值与 IP
│       ├── service/impl/MenuServiceImpl.java # [MODIFY] 树保留祖先；上级校验；路由名唯一
│       └── service/impl/DeptServiceImpl.java # [MODIFY] 树保留祖先；上级校验
│   └── src/test/java/com/qkit/system/        # [NEW] 数据权限/权限服务/字典缓存/用户保护单元测试
├── qkit-admin/
│   └── src/main/
│       ├── java/com/qkit/admin/ScaffoldApplication.java  # [MODIFY] 移除无用的调度开关
│       └── resources/
│           ├── application.yml               # [MODIFY] 异步线程池参数
│           ├── application-prod.yml          # [MODIFY] 关闭接口文档、收敛 actuator
│           └── db/migration/V1.0.1__seed.sql # [MODIFY] 参数设置菜单可见性、初始密码、管理员口令
└── frontend/src/
    ├── composables/usePagination.ts          # [NEW] 分页状态与事件
    ├── composables/useCrud.ts                # [NEW] 列表页增删改查通用逻辑（工厂函数）
    ├── directives/permission.ts              # [MODIFY] 数组语义改任一满足；避免重复移除节点
    ├── stores/permission.ts                  # [MODIFY] 权限判定语义同步
    ├── api/system/dict.ts                    # [MODIFY] 字典项分页接口
    ├── api/system/dept.ts                    # [MODIFY] 拆分简单列表与树形节点类型
    ├── api/system/operLog.ts / loginLog.ts   # [MODIFY] 时间参数字段名对齐
    ├── api/system/user.ts / menu.ts / role.ts # [MODIFY] 删除无调用方的封装
    └── views/system/{user,role,post,dict,config,oper-log,login-log}/index.vue # [MODIFY] 统一改用组合式函数
```

## 五、关键代码结构

编辑器与执行者需遵循以下契约（仅接口级）：

```ts
// frontend/src/composables/useCrud.ts
export interface UseCrudOptions<Row, Query, Form> {
  page: (query: Query) => Promise<{ list: Row[]; total: number }>
  create?: (data: Form) => Promise<unknown>
  update?: (data: Form) => Promise<unknown>
  remove?: (id: number) => Promise<unknown>
  defaultQuery: () => Query
  defaultForm: () => Form
  /** 编辑回显转换，可取详情（用户页需要） */
  toForm?: (row: Row) => Promise<Form> | Form
  /** 保存成功后的附加动作，如提交角色/菜单授权 */
  afterSave?: (savedId: number | undefined, form: Form) => Promise<void>
  /** 删除确认文案，默认按行标题拼接 */
  confirmDelete?: (row: Row) => string
}

export function useCrud<Row, Query, Form>(options: UseCrudOptions<Row, Query, Form>): {
  query: Query
  list: Ref<Row[]>
  total: Ref<number>
  loading: Ref<boolean>
  fetch: () => Promise<void>
  onSearch: () => void
  onReset: () => void
  dialogVisible: Ref<boolean>
  dialogMode: Ref<'add' | 'edit'>
  form: Ref<Form>
  formRef: Ref
  onAdd: () => void
  onEdit: (row: Row) => Promise<void>
  onSave: () => Promise<void>
  onDelete: (row: Row) => Promise<void>
}
```

```java
// qkit-system/.../service/UserService.java
/** 管理端详情：受数据权限约束 */
R<UserVO> detailInScope(Long id);
```

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 在抽组合式函数、拆分部门类型、清理死代码之前，跨目录盘点 7 个列表页的脚本重复结构、各自特有的扩展点（用户页详情回显与角色提交、角色页双树授权、字典页双实例、日志页时间控件），以及各文件被引用情况，形成统一的改造清单。
- Expected outcome: 输出一份“页面共性/差异/扩展点”清单与受影响的文件全集，确保组合式函数的接口设计一次到位、重构不做二次返工。

### Skill

- **lsp-code-analysis**
- Purpose: 在删除死代码与调整公开接口前做语义级影响分析：确认 `saveUserStatus`、`listMenu`、`assignRoleDept` 确实无引用；确认把 `detail` 拆分为 `detailInScope` 后所有调用点（含个人中心）都已正确归属；确认修改组合式函数签名后 7 个页面的引用全部更新。
- Expected outcome: 每个删除项与接口变更都有“引用已清零/调用点已更新”的可验证结论，避免编译期通过但运行期行为缺失。