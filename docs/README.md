# qkit 文档集

> 本目录是 qkit 脚手架的**完整规范集**，同时也是"喂给 AI 智能体"的输入材料。
> 任何开发 / 维护 / 智能体生成代码前**必须**先读完 01 + 02 + 03 + 07。

## 文档清单

| # | 文档 | 优先级 | 何时读 |
|---|---|---|---|
| [01-charter.md](./01-charter.md) | 项目章程 | P0 | 第一次接手 |
| [02-tech-stack.md](./02-tech-stack.md) | 技术选型与决策清单 | P0 | 写代码前 |
| [03-structure.md](./03-structure.md) | 目录结构与分层规范 | P0 | 写代码前；§A 字段命名映射在写 Convert/Entity 前 |
| [04-database.md](./04-database.md) | 数据库设计 | P0 | 新建表前 |
| [05-api.md](./05-api.md) | 接口规范 | P0 | 写 Controller 前 |
| [06-permission.md](./06-permission.md) | 权限与安全 | P0 | 涉及权限/安全前 |
| [07-conventions.md](./07-conventions.md) | 编码规范 | P0 | Code Review 必查；§6 测试模板在写单测/组件测试前 |
| [08-golden-examples.md](./08-golden-examples.md) | 黄金示例代码 | P0 | 新模块开工前；§2 树形示例在涉及 parent_id 自引用模块前 |
| [09-module-onboarding.md](./09-module-onboarding.md) | 新模块接入指引 | P0 | 加新业务模块前 |
| [10-environment.md](./10-environment.md) | 环境与部署 | P1 | 部署/排错时 |

## 投喂智能体的标准 prompt 模板

```
【角色】你是资深 Java + Vue 架构师，按 qkit/docs/ 文档集生成代码，禁止自行变更架构/版本/命名。

【阶段】当前只做：<具体任务，引用文档编号>。禁止超出本步范围做其他模块。

【红线】先复述 03/07 的分层与编码红线，再开始。

【风格】所有代码与 08 黄金示例保持一致：
  - record DTO、构造器注入、MapStruct
  - 标准动词 Controller（page/list/detail/create/update/delete/assign/reset）
  - R、@SaCheckPermission、@OperLog
  - <script setup lang="ts">、api 封装、useCrud
  - **SaveGroup/UpdateGroup** 分组校验（@Validated(SaveGroup.class)）+ record DTO

【完成定义】产出文件清单 + 如何验证（mvn / 启动 / 接口）+ 未完成项。

【禁止】
  - 不要自动创建 git 提交
  - 不要覆盖已存在文件（先提示冲突）
  - 不要编造未在文档中的版本号
  - 不要新增 02 决策清单外的技术栈
```

## 三条铁律

1. **版本号全部锁死**：只使用 02 文档中已确认的版本，禁智能体自行升级/降级。
2. **黄金示例 > 文字规范**：要求智能体生成的每一个模块，风格必须与 08 文档中的示例一致。
3. **DDL 不可即兴修改**：04 文档中的核心表结构是地基，字段增删必须人工确认。

## 红线速查（每批投喂时复述）

- 禁止跨层调用：Controller 不直连 Mapper
- 禁止 `BeanUtils.copyProperties`（必须 MapStruct）
- 禁止字段注入（必须构造器注入）
- 禁止物理删除（必须逻辑删除）
- 禁止敏感信息硬编码（密码/密钥走 env）
- 禁止前端 Options API（必须 `<script setup lang="ts">`）
- 禁止明文密码 / 弱加密
- 禁止 SQL 字符串拼接
- 禁止前端 `v-html` 直接渲染用户输入（XSS）
- 禁止 MyBatis XML `${}` 拼接外部入参（SQL 注入）
- 禁止 SaveDTO 只用 `@Valid` 不带分组（必须 `@Validated(SaveGroup.class)` 配合 `groups` 注解，否则分组校验不生效）

详见 [07-conventions.md](./07-conventions.md) 与 [06-permission.md](./06-permission.md)。

## 按钮权限速查（新增按钮前必看）

**一个权限码字符串，在 3 处必须完全一致**：

| 位置 | 形态 | 见 |
|---|---|---|
| 后端 Controller 方法 | `@SaCheckPermission("xxx:xxx:xxx")` | 08 §1.6 |
| 前端按钮元素 | `v-permission="['xxx:xxx:xxx']"`（06 §5.3 支持数组多码：`['a','b']` 任一命中即显示） | 08 §1.11 |
| 数据库 | `sys_menu.type='F'` 且 `sys_menu.perm='xxx:xxx:xxx'` | 06 §1、04 §3 |

**5 步走**（跳步 = 漏洞）：

1. 06 §10 权限码字典登记（或确认已在字典内）
2. 后端 Controller 方法加 `@SaCheckPermission("<code>")`
3. 前端按钮元素加 `v-permission="['<code>']"`
4. `sys_menu` 插入 `type=F` 记录，`perm='<code>'`
5. 「角色管理 → 分配菜单」勾选该按钮 → 重登录 → 验证显隐 + 接口调用

**一票否决**：

- 后端有注解但前端无 `v-permission` → 权限绕过
- 菜单 `type=F` 但 `perm` 为空 → 典型漏洞

详见 [06 §10](./06-permission.md) / [08 §1.6 §1.11](./08-golden-examples.md) / [09 §Step 10](./09-module-onboarding.md)。

## 投喂顺序建议

1. **第一批**：01 + 02 + 03 + 04 + 05 + 08 + 09
   → 生成：项目骨架 + 系统管理模块 + 数据库
   - **树形/自引用模块**额外读 08 §2（部门管理示例）
   - **字段命名/Convert** 额外读 03 §A
2. **第二批**：06 + 07
   → 补充：权限/安全/编码规范细节
   - **写测试**额外读 07 §6（Service 单测 + MockMvc + Vue 组件测试模板）
3. **第三批**：10
   → 交付：环境配置 / 部署文档
