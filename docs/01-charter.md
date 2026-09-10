# 01 · 项目章程 Charter

> 任何开发（包括智能体）必须先读本文档，搞清楚"做什么 / 不做什么 / 怎么算做完"再动手。

## 1. 一句话定位

**qkit 是一套「低代码风格」的前后台基础架构脚手架**，用于支撑公司内/团队内后续业务系统（OA、CRM、ERP、B 端 SaaS 后台等）快速搭建。

> "低代码风格" 指的不是真正的低代码平台（不含拖拽式设计器、不含运行时渲染器、不含代码生成器），而是**高度规范化、模块化、约定优于配置**的脚手架 — 拿到手就能跑通登录 / 用户 / 角色 / 菜单 / 字典 / 日志，**新增一个业务模块只需 30 分钟**（参照 `08-golden-examples.md` 的 A/B 级模板）。

## 2. 范围

### 2.1 MVP（一期必交付）

| 模块 | 关键能力 | 复杂度 |
|---|---|---|
| 登录认证 | 账号密码 + 图形验证码 + 失败锁定 + JWT（Sa-Token） | A |
| 用户管理 | 增删改查、分配角色、分配岗位、修改密码、重置密码 | B |
| 角色管理 | 增删改查、分配菜单权限、分配数据权限范围、关联用户 | B |
| 部门管理 | 树形 CRUD、启用/停用 | B |
| 岗位（职位）管理 | 增删改查、按部门筛选 | B |
| 菜单管理 | 树形 CRUD、目录 / 菜单 / 按钮 三种类型、关联权限标识 | B |
| 字典管理 | 字典分类 + 字典项两级 CRUD | B |
| 操作日志 | 方法级自动记录（谁、什么时候、什么 IP、请求了哪个接口、耗时、参数、返回值摘要） | A |
| 登录日志 | 登录成功 / 失败 / 登出 + IP + UA + 失败原因 | A |
| 参数配置 | 系统参数增删改查、内置参数保护、本地缓存热更新 | B |

### 2.2 二期/远期（不在本期，但预留接口与菜单挂载点）

- 文件中心（本地存储 + MinIO 适配）
- 数据源管理（连接外部 MySQL/PostgreSQL/HTTP 数据源）
- 表单/页面设计器与运行时渲染器
- 审批流（Flowable 预留模块名 `qkit-flow`）
- 代码生成器（根据表结构生成 CRUD）
- 多租户（字段级隔离，默认不启用；启用细节见 04 §1.9 + 02 D-mini-10）

### 2.3 明确不做

- ❌ 工作流引擎、流程画图（不在二期也不在远期；如需走单独产品）
- ❌ 拖拽式页面 / 表单设计器
- ❌ 代码生成器
- ❌ 移动端 / 小程序
- ❌ 公开 SaaS、租户开通后台
- ❌ AI 代码生成、AI 助手
- ❌ 国际化 i18n（默认中文，预留文案抽取结构，不做多语种）

## 3. 技术栈

> 详细版本锁定见 `02-tech-stack.md`。此处只列选型。

**后端**：Spring Boot 3.2.x + MyBatis-Plus 3.5.x + Sa-Token 1.39.0 + MySQL 8 + Redis 7 + MapStruct + Knife4j
**前端**：Vue 3.4 + TypeScript 5 + Vite 5 + Element Plus 2.8 + Pinia 2 + Vue Router 4 + Axios
**构建**：Maven 3.8+（多模块）+ npm（随 Node 20 LTS）
**JDK / Node**：JDK 17 / Node 20 LTS
**工具**：Docker Compose（MySQL + Redis 一键起）、Hutool 工具库、Flyway 数据库版本化

## 4. 仓库与模块结构

```
qkit/
├── pom.xml                 # 根 POM，dependencyManagement 锁版本
├── qkit-admin/          # 启动模块（Controller 聚合、application.yml、db/migration、main 入口）
├── qkit-common/         # 通用层：R/异常/错误码/常量/工具/校验分组/日志 SPI
├── qkit-framework/      # 框架层：MyBatis-Plus/Sa-Token/Redis/Jackson/Web/AOP/审计 配置
├── qkit-system/         # 系统管理：用户/角色/部门/岗位/菜单/字典/参数/日志
├── frontend/               # 前端工程（npm）
├── deploy/                 # docker-compose / docker(Dockerfile) / nginx
├── docs/                   # 本文档集
└── scripts/                # 本地启动 / 部署脚本（dev.sh、dev.bat、deploy.sh）
```

> 详细分层、包结构、命名规范见 `03-structure.md`。

## 5. 参考产品

- **RuoYi-Vue-Pro（yudao-cloud）** — 仓库结构、模块拆分、菜单权限模型
- **eladmin** — DTO/VO 分层、Manager 层设计
- **vue-vben-admin** — 前端 layout、动态路由、useCrud 思路
- **pig** — Sa-Token 集成方案（如有差异以 yudao-cloud 为准）

本脚手架不是任一项目的复刻 — 是**综合**。

## 6. 成功标准（MVP 验收）

以下**全部**满足才算 MVP 完成：

### 6.1 工程层面

- [ ] `mvn -B clean package` 在 JDK 17 下零警告通过
- [ ] `npm install && npm run build` 通过（`frontend/`）
- [ ] `cd deploy && docker compose up -d` 一次编排 MySQL 8 + Redis 7 + backend + frontend
- [ ] 启动脚本（`./scripts/dev.sh` 或 IDE）能拉起后端，监听 8080
- [ ] 前端 `npm run dev` 起 dev server 监听 5173
- [ ] 前端 `npm run build` 产物可由 Spring Boot 静态托管或 Nginx 部署

### 6.2 功能层面（冒烟用例）

- [ ] 打开 `http://localhost:5173` → 跳转登录页
- [ ] 输入 admin / admin123 + 图形验证码 → 登录成功
- [ ] 默认错误密码 5 次锁定 10 分钟
- [ ] 进入「系统管理 → 用户管理」→ 列表展示 admin
- [ ] 新建一个用户 test01 → 在「操作日志」中能看到"新增用户 test01"的记录
- [ ] 给 test01 分配「普通用户」角色 + 部门 + 岗位
- [ ] 退出 admin → 用 test01 登录 → 看到菜单只剩自己被授权的部分
- [ ] 「角色管理」给普通用户角色分配「字典管理」的查看权限 → test01 登录可见
- [ ] 「字典管理」新增一个字典"业务状态"=启用/禁用 → 任何页面下拉都可通过字典 key 引用
- [ ] 「菜单管理」新增一个按钮类菜单"导出用户" → 角色勾选后用户列表出现导出按钮
- [ ] 「操作日志」列表能按用户 / 模块 / 时间区间筛选
- [ ] 「登录日志」列表能看到上述所有登录成功 / 失败记录

### 6.3 质量层面

- [ ] 后端 Controller 全部带 `@Operation`（Knife4j 文档可访问 `/doc.html`）
- [ ] 全部 Service 方法带 `@Transactional` 边界（只读除外）
- [ ] 全局异常统一封装为 `R.fail(code, msg)`，前端按 code 弹 ElMessage
- [ ] 跨域配置正确，前端 dev 跨域 8080 成功
- [ ] 至少 1 个后端单测（service 层）+ 1 个前端组件测试（可选）
- [ ] README 完整，环境搭建命令一键可跑

## 7. 投喂智能体的标准 prompt 模板

```
【角色】你是资深 Java + Vue 架构师，按 qkit/docs/ 文档集生成代码，禁止自行变更架构/版本/命名。
【阶段】当前只做：<具体任务，引用文档编号>。禁止超出。
【红线】先复述 03/07 的分层与编码红线，再开始。
【风格】所有代码与 08 黄金示例保持一致。
【完成定义】产出文件清单 + 如何验证 + 未完成项。
【禁止】不要自动 git 提交；不要覆盖已存在文件；不要编造未在文档中的版本号。
```

## 8. 与 sibling `springboot/`、`scaffold-docs/` 的关系

`E:\usr\mk\scaffold\` 下存在兄弟项目：

- `scaffold-docs/` — **通用规范池**，是事实标准
- `springboot/` — **参考实现**（DOL 实践版）

本项目（qkit）：

- **复用** `scaffold-docs/` 的所有规范（投喂指南、决策清单、命名规范、黄金示例）
- **差异化** 在：包名 `com.qkit.*`、模块名 `qkit-*`、**不含低代码引擎**（简化）、Sa-Token 取代 Spring Security
- **不复用** `springboot/` 的代码 — qkit 是独立的可运行项目

> 智能体生成代码时，应**优先以本目录 docs/ 为准**；`scaffold-docs/` 仅作为"通用规范参考"补充，不直接复制其代码。
