# 09 · 新模块接入指引

> 新增业务模块（如 "订单管理"、"客户管理"）的**完整步骤**。
> 全部按本文档 checklist 走，缺一步不通过。

## 1. 模块命名规范

- 模块前缀：`b_`（business，业务域）
- 表名：`b_<domain>_<entity>`，例：`b_trade_order`
- Maven 模块（如有独立模块需求）：`qkit-b-trade`
- 默认**不**为业务模块建独立 Maven 子模块，复用 `qkit-system` 包结构（`com.qkit.system.<subpackage>`）即可

> 与兄弟项目 `glm-<大域>-<子域>` 命名不同，本项目**简化**：业务模块统一收在 `qkit-system` 一个 Maven 模块下，按子包分业务域。

## 2. A 级 vs B 级判定

| 维度 | B 级（默认） | A 级 |
|---|---|---|
| 业务规则 | 简单 CRUD | 有状态机 / 不变量 / 复杂校验 |
| Entity 形态 | 贫血 DO（继承 BaseEntity） | 充血 Entity（业务方法） |
| Manager 层 | 不需要 | 跨表/跨服务编排 |
| Convert | 直接 Entity ↔ VO | 引入 DO ↔ Entity ↔ VO |
| 适用场景 | 系统管理、字典类业务 | 订单、库存、审批流 |

**默认 B 级**。只有出现以下情况之一才升级 A 级：

- 实体有状态机（如订单状态：待支付 → 已支付 → 已发货 → 已收货 → 已退款）
- 有"业务规则"必须放在实体内部（避免散在 service 各处）
- 跨 ≥ 3 张表的事务一致性

## 3. 新增业务模块 checklist（B 级，10 步）

### Step 1：写 DDL

`qkit-admin/src/main/resources/db/migration/V<yyyy.MM.dd>__<b_trade_order>.sql`

```sql
CREATE TABLE `b_trade_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '订单号',
  `customer_id` BIGINT NOT NULL DEFAULT 0 COMMENT '客户ID',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
  `remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  -- 公共 5 列（必填；主键 id 见上）
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
```

### Step 2：写 Entity

路径：`qkit-system/src/main/java/com/qkit/system/domain/entity/trade/Order.java`

参照 08 黄金示例 1.3。注意：

- `@TableName("b_trade_order")`
- 主键 `@TableId(type = IdType.AUTO)`（配合 DDL `AUTO_INCREMENT`）
- 金额字段 `BigDecimal`，**禁** `double / float`
- 逻辑删除 `@TableLogic` 加在 `delFlag` 字段

### Step 3：写 DTO

路径：`qkit-system/src/main/java/com/qkit/system/domain/dto/trade/OrderSaveDTO.java` 与 `OrderQueryDTO.java`

- `SaveDTO`：**record**，component 上加 validation 注解；分组用 `groups = SaveGroup/UpdateGroup.class`
- `QueryDTO`：**record**，分页字段 `pageNum / pageSize`（无默认值，由 Controller 调用前注入）

### Step 4：写 VO

路径：`qkit-system/src/main/java/com/qkit/system/domain/vo/trade/OrderVO.java`

- 字段冗余展示值（`customerName` 等）用 Service 填充
- 字典翻译字段（`statusLabel`）由前端 `<DictTag>` 渲染
- **禁**返回 Entity / DTO

### Step 5：写 Convert

路径：`qkit-system/src/main/java/com/qkit/system/convert/trade/OrderConvert.java`

- `@Mapper(componentModel = "spring")`
- 方法：`toVO / toVOList / toEntity / toDetailVO`

### Step 6：写 Mapper

路径：`qkit-system/src/main/java/com/qkit/system/mapper/trade/OrderMapper.java`

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 复杂查询放这里（XML 在 resources/mapper/trade/OrderMapper.xml）
}
```

### Step 7：写 Service / Impl

路径：
- 接口：`service/trade/OrderService.java`
- 实现：`service/impl/trade/OrderServiceImpl.java`

按 08 黄金示例 1.9 / 1.10 抄。

### Step 8：写 Controller

路径：`controller/admin/trade/OrderController.java`

按 08 黄金示例 1.11 抄。**每个方法加 `@SaCheckPermission` 与 `@OperLog`**。

### Step 9：写前端

- `frontend/src/types/trade/order.ts` — TS 类型
- `frontend/src/api/trade/order.ts` — HTTP 封装
- `frontend/src/views/trade/order/index.vue` — 列表页
- `frontend/src/views/trade/order/components/OrderFormDialog.vue` — 表单对话框

### Step 10：菜单注册

在 `sys_menu` 表中插入菜单树（用 SQL 脚本或管理后台手填）：

```sql
-- 一级菜单：业务管理
INSERT INTO sys_menu (id, name, type, parent_id, path, component, icon, sort, visible, status, create_time)
VALUES (100, '业务管理', 'M', 0, '/biz', 'Layout', 'Box', 10, 0, 0, NOW());

-- 二级菜单：订单管理（perm 用 biz:order:page 表示查看权限，与 05 §5.1 禁用动词一致）
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_time)
VALUES (101, '订单管理', 'C', 100, '/biz/order', 'biz/order/index', 'biz:order:page', 'List', 1, 0, 0, NOW());

-- 按钮权限
INSERT INTO sys_menu (id, name, type, parent_id, perm, sort, status, create_time) VALUES
  (102, '订单查询', 'F', 101, 'biz:order:page', 1, 0, NOW()),
  (103, '新增订单', 'F', 101, 'biz:order:create', 2, 0, NOW()),
  (104, '更新订单', 'F', 101, 'biz:order:update', 3, 0, NOW()),
  (105, '删除订单', 'F', 101, 'biz:order:delete', 4, 0, NOW());
```

## 4. 跨模块调用规则

**业务模块间禁止直接依赖实现类**。场景：

| 场景 | 处理 |
|---|---|
| 订单模块要查用户信息 | 注入 `UserService`（**接口**，不是 Impl） |
| 订单完成后发通知 | 发 `ApplicationEvent`，由通知模块订阅 |
| 跨服务写操作 | 走事件 / 接口 |
| 跨服务只读 | 走接口（**注接口不注实现**） |

```java
// ✅ 正确
private final UserService userService;

// ❌ 错误
private final UserServiceImpl userServiceImpl;
```

## 5. 新增字典步骤

### 5.1 后端

在 `sys_dict` 与 `sys_dict_item` 表插入记录：

```sql
INSERT INTO sys_dict (id, name, type, status, create_time)
VALUES (50, '业务状态', 'biz_order_status', 0, NOW());

INSERT INTO sys_dict_item (dict_type, label, value, sort, status, create_time) VALUES
  ('biz_order_status', '待支付', '0', 1, 0, NOW()),
  ('biz_order_status', '已支付', '1', 2, 0, NOW()),
  ('biz_order_status', '已发货', '2', 3, 0, NOW()),
  ('biz_order_status', '已收货', '3', 4, 0, NOW());
```

### 5.2 前端

字典存储在 Pinia `useDictStore`（启动时从后端拉取缓存到 Redis）：

```vue
<DictSelect v-model="form.status" dict-type="biz_order_status" />
<DictTag dict-type="biz_order_status" :value="row.status" />
```

## 6. 字典存储策略

- 启动时 `CommandLineRunner` 把所有启用字典从 MySQL 拉入 Redis
- Redis key：`sys_dict:{type}` → JSON 列表
- TTL 永不过期；写操作（增删改字典项）发事件清缓存
- 前端启动时一次性拉所有字典到 store，**不**每次请求后端

## 7. 简化模式（无业务规则的纯字典表）

某些表（如配置表）极简，可以省去 DTO/VO 转换：

```java
@GetMapping("/list")
public R<List<Config>> list() {
    return R.ok(configService.list());
}
```

> 但**强烈不建议**这么写 — VO 永远存在更安全。本项目**统一**走完整链路。

## 8. 验收 checklist

新模块完成后：

- [ ] `mvn -B compile` 通过
- [ ] 启动后端，登录 Knife4j 看到新接口
- [ ] 前端 `pnpm dev` 启动，菜单可见
- [ ] 列表能展示、新增/编辑/删除/分页/搜索 全可用
- [ ] 操作日志能看到新增/编辑/删除记录
- [ ] 不存在的按钮（如不勾权限）不显示
- [ ] 跨表查询正确（如订单查用户名）

### 8.1 权限码一致性自检（必查）

> 详见 `06-permission.md` 第 10 节"权限码字典"。

- [ ] **先**在 06 文档第 10.1 节字典表追加新模块的权限码
- [ ] 后端 Controller 方法全部加 `@SaCheckPermission("<module>:<resource>:<action>")`
- [ ] 前端按钮全部加 `v-permission="['<module>:<resource>:<action>']"`
- [ ] **逐字比对**前后端字符串（大小写、冒号、拼写）
- [ ] 在 `sys_menu` 表插入对应按钮（`type=F`，`perm=权限码`）
- [ ] 角色分配后 → 重登录 → 按钮按权限显隐正确

> 任何不一致（后端有注解但前端无 v-permission，或反之）= 权限漏洞，必须在 PR 阶段拦截。

## 9. 提交规范

- 单模块 1 个 commit
- 标题：`feat(system): 新增订单管理模块`
- 内容：列 DDL 文件、Service 类、前端文件路径
- 引文：关联需求 / issue 编号
