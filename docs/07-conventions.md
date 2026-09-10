# 07 · 编码规范

> 智能体生成的任何代码必须遵循本文。Code Review 阶段会逐条检查。

## 1. 后端 Java 规范

### 1.1 基础

- JDK 17，必须开启 `-Xlint:all`
- 缩进 4 空格，禁止 Tab
- 行宽 ≤ 120 字符
- 文件编码 UTF-8
- import 不使用通配符（`import java.util.*` 禁）
- 同包 import 按字母序，分组：java.* / jakarta.* / 第三方 / 自有
- 空格：运算符两侧、逗号后、关键字后；行尾不留空格

### 1.2 命名

| 类型 | 规范 | 示例 |
|---|---|---|
| 类 | 大驼峰 | `UserServiceImpl` |
| 方法 | 小驼峰 | `getUserById` |
| 变量 | 小驼峰 | `userId` |
| 常量 | 全大写下划线 | `MAX_RETRY_COUNT` |
| 包 | 全小写 | `com.qkit.system` |
| 枚举 | 大驼峰；成员全大写 | `ErrorCode.USER_NOT_FOUND` |
| 泛型 | 单字母大写 | `T` `K` `V` |

### 1.3 类与构造器

- **必须**使用构造器注入（`@RequiredArgsConstructor` + `final` 字段）
- **禁止**字段注入 `@Autowired / @Resource`
- 公共类必须有 Javadoc 类注释
- 工具类构造器私有（`private XxxUtil() {}`）

### 1.4 Bean 与数据对象

| 类型 | 形态 | 注解 | 字段访问 |
|---|---|---|---|
| Entity | class | `@Getter` + 必要 setter + 构造器；**禁** `@Data` | 严格控制 |
| DTO（入参） | **record** | record component 上加 `jakarta.validation` 注解；分组用 `groups` | `obj.field()` |
| VO（出参） | **record** | Long 字段直接序列化为数字（主键自增，值域小，无精度丢失） | `obj.field()` |
| Convert | interface | `@Mapper(componentModel = "spring")`；Update 用 `@Mapping(target = "password", ignore = true)` | — |

> - **DTO/VO 一律 record**：不可变、线程安全、自动生成 `equals/hashCode/toString`；与 Java 17 特性对齐。
> - **Entity 禁 `@Data`** 是为了避免无脑 setter 破坏不变量；service 层可控修改字段时单独暴露 setter。
> - record 的 component 上不能加 Lombok 注解（`@Data` 等），但 Jackson / Validation / MapStruct 全部支持 record。
> - **导出时间格式**：EasyExcel 导出不经过 Jackson，导出 VO 的时间字段必须显式标注 `@DateTimeFormat("yyyy-MM-dd HH:mm:ss")`（`com.alibaba.excel.annotation.format.DateTimeFormat`），与接口全局格式保持一致。
> - **例外**：`com.qkit.common.api.R<T>` 通用响应包装属于框架 API 类，不属于 DTO/VO，可使用 Lombok `@Data`；详见 05 §1。

### 1.5 对象映射

- **必须** MapStruct
- **禁止** `BeanUtils.copyProperties / Spring BeanUtils / Hutool BeanUtil.copy`
- Convert 类放 `domain/convert/`，命名 `XxxConvert`

```java
@Mapper(componentModel = "spring")
public interface UserConvert {
    UserVO toVO(User entity);
    List<UserVO> toVOList(List<User> list);
    User toEntity(UserSaveDTO dto);
}
```

### 1.6 校验

- Controller 入参加 `@Valid`
- DTO 字段加 `jakarta.validation` 注解：`@NotBlank @NotNull @Size @Email @Pattern`
- 分组校验：`@Validated(SaveGroup.class)` + `@GroupSequence`
- 业务校验放 Service / Manager 层（`Assert.notNull(...)` 抛 `BusinessException`）

### 1.7 异常

- 业务异常：`throw new BusinessException(ErrorCode.XXX)` 或 `ErrorCode.XXX.getCode(), "附加信息"`
- 禁止 `throw new RuntimeException(...)`
- 禁止吞异常（`catch (Exception e) {}` 空块）
- catch 后必须**要么**抛、**要么**日志记录后重抛

### 1.8 日志

- 引入 SLF4J（`@Slf4j` Lombok）
- 业务日志用 `log.info / log.warn`
- 异常日志用 `log.error("...异常信息", e)`，**必须**带 throwable
- 禁止 `System.out.println / System.err.println`
- 禁止日志中输出：密码、token、身份证、银行卡、完整信用卡号

### 1.9 事务

- `@Transactional(rollbackFor = Exception.class)`（默认只回滚 RuntimeException 不够）
- 只读方法 `@Transactional(readOnly = true)`
- 事务加在 Service 方法上，**不加**在 Controller / Manager
- 跨服务调用（跨 Service）通过注入接口，**不在**事务内做远程调用（防长事务）

### 1.10 MyBatis-Plus 规范

- 简单 CRUD 用 `LambdaQueryWrapper`
- 复杂查询写自定义方法（Mapper 接口 + XML）
- 分页：`Page.of(pageNum, pageSize)`
- 批量操作：先 `deleteBatchIds`，后 `saveBatch`（不要 updateBatchById 覆盖式）
- 逻辑删除：Entity 加 `@TableLogic`，DB 列 `del_flag TINYINT`

### 1.11 集合与流

- 优先 `List.of` / `Map.of` / `Set.of`（不可变）
- 流式操作：`.stream().filter(...).toList()`（JDK 17）
- 大集合（>1万）操作考虑并行流，但要评估成本

## 2. 后端代码红线（Code Review 一票否决）

| # | 红线 | 示例反模式 |
|---|---|---|
| B1 | Controller 注入 Mapper | `@Autowired UserMapper` |
| B2 | 返回 Entity 给前端 | `return R.ok(entity)` |
| B3 | 物理删除 | `mapper.deleteById(id)` 改为 `update.delFlag=1` |
| B4 | `BeanUtils.copyProperties` | 改为 MapStruct |
| B5 | 字段注入 | `@Autowired private XxxService` 改为构造器 |
| B6 | `select *` | 必须显式列字段 |
| B7 | 业务代码字符串拼 SQL | 用 `LambdaQueryWrapper` |
| B8 | 异常吞掉 | `catch (Exception e) {}` |
| B9 | Controller 写业务 | `if (...) ... if (...) ...` 搬到 Service |
| B10 | Entity 用 `@Data` | 改 `@Getter` + 必要 setter |
| B11 | Map 类型参数返回前端 | 改 VO |
| B12 | 循环里调数据库 | `for(x : list) mapper.select(x)` |
| B13 | `@Transactional` 加在 Controller | 移到 Service |
| B14 | 密码 / token 输出日志 | 脱敏或剔除 |
| B15 | 金额用 `double / float` | 用 `BigDecimal` 或 `Long`（分） |

## 3. 前端 TypeScript / Vue 规范

### 3.1 基础

- 全量 TypeScript，`strict: true`
- 业务文件 `.ts / .vue`，**禁**纯 `.js`
- 缩进 2 空格（prettier 默认）
- 单引号、不带分号（prettier 配）
- 行宽 100 字符
- import 顺序：`vue` / 第三方 / `@/` 别名

### 3.2 命名

| 类型 | 规范 | 示例 |
|---|---|---|
| 组件 | 大驼峰 .vue | `UserFormDialog.vue` |
| 组合式函数 | `use` 前缀 | `useCrud` |
| 类型 / 接口 | 大驼峰 | `UserVO` |
| 枚举 | 大驼峰；成员大驼峰 | `UserStatus.ENABLED` |
| 常量 | 全大写下划线 | `MAX_PAGE_SIZE` |
| 变量 / 函数 | 小驼峰 | `userList` |
| CSS 类名 | kebab-case | `.user-form` |

### 3.3 组件

- 全部用 `<script setup lang="ts">`
- **禁** Options API
- props 用 `defineProps<{ userId: number }>()` 类型化
- emit 用 `defineEmits<{ (e: 'submit', data: UserVO): void }>()`
- 组件名多词：`UserForm` 不用 `Form`
- `<template>` 顶层只能有一个根元素

### 3.4 状态管理

- 全部走 Pinia
- 命名 `useXxxStore`
- 不要在 store 里写业务逻辑，只做状态存取

### 3.5 HTTP

- 所有 HTTP 走 `src/api/<module>/<name>.ts`
- 视图组件**禁止** `import axios from 'axios'`
- 类型与返回类型一致

```ts
// api/system/user.ts
import request from '@/utils/request'
import type { UserVO, UserSaveDTO, UserQueryDTO } from '@/types/system/user'

export const pageUser = (params: UserQueryDTO) =>
  request.get<R<UserVO[]>>({ url: '/admin-api/system/user/page', params })

export const createUser = (data: UserSaveDTO) =>
  request.post<R<number>>({ url: '/admin-api/system/user/create', data })

export const updateUser = (data: UserSaveDTO) =>
  request.put<R<boolean>>({ url: '/admin-api/system/user/update', data })

export const deleteUser = (ids: number[]) =>
  request.del<R<boolean>>({ url: '/admin-api/system/user/delete', data: ids })

export const getUserDetail = (id: number) =>
  request.get<R<UserVO>>({ url: `/admin-api/system/user/detail/${id}` })
```

### 3.6 列表页 useCrud

```ts
// composables/useCrud.ts
export function useCrud<T>(api: CrudApi<T>) {
  const list = ref<T[]>([])
  const total = ref(0)
  const loading = ref(false)
  const query = reactive({ pageNum: 1, pageSize: 10 })

  const fetchData = async () => {
    loading.value = true
    try {
      const res = await api.page(query)
      list.value = res.data
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  const handleQuery = () => { query.pageNum = 1; fetchData() }
  const handleReset = () => { /* reset query */; handleQuery() }

  onMounted(fetchData)
  return { list, total, loading, query, fetchData, handleQuery, handleReset }
}
```

### 3.7 字典

- 用 `useDict('sys_user_sex')` 拉字典项
- 组件 `<DictSelect dict-type="sys_user_sex" v-model="form.sex" />`
- 列表展示用 `<DictTag dict-type="sys_user_sex" :value="row.sex" />`

## 4. 前端红线

| # | 红线 | 反模式 |
|---|---|---|
| F1 | Options API | `export default { data() {} }` |
| F2 | 视图直接 axios | `this.$axios.get(...)` |
| F3 | `any` 满天飞 | `function handler(data: any)` |
| F4 | 硬编码后端地址 | `axios.get('http://localhost:8080/...')` |
| F5 | 路由同步 import | `import User from '@/views/...'` |
| F6 | 多个根元素 | `<template><div/><div/></template>` |
| F7 | `var` 声明 | `var i = 0` |
| F8 | 控制台日志残留 | 生产构建去除 `console.log` |
| F9 | 大对象 deep watch | `watch(() => huge, deep: true)` |
| F10 | 直接改 props | `props.user.name = 'x'` |

## 5. 测试规范

### 5.1 后端

- 框架：JUnit 5 + Mockito
- Service 层单测覆盖率 ≥ 60%（核心业务 ≥ 80%）
- 测试方法命名：`methodName_condition_expectedResult`
- 不引 Testcontainers（用本地 docker MySQL 即可）
- 不需要 100% 覆盖，重业务逻辑必须覆盖

### 5.2 前端（本期不强求）

- 框架：Vitest + Vue Test Utils
- 只对核心 composables 和复杂组件写测试

## 6. 测试代码模板

> §5 给的是规范条款（覆盖率/命名/范围），本节给最小可跑代码模板。新模块必须按本节照抄。

### 6.1 Service 单测（JUnit 5 + Mockito）

```java
package com.qkit.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.exception.BusinessException;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.service.UserRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRoleService userRoleService;
    @Mock private UserConvert userConvert;
    @InjectMocks private UserServiceImpl userService;

    @Test
    @DisplayName("create_用户名已存在 → 抛 USER_EXISTS 异常")
    void create_usernameExists_throwsException() {
        // given
        UserSaveDTO dto = new UserSaveDTO(null, "admin", "abc12345",
            null, null, null, null, null, null, null, null, null);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // when + then
        BusinessException ex = assertThrows(BusinessException.class,
            () -> userService.create(dto));
        assertEquals(ErrorCode.USER_EXISTS.getCode(), ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }
}
```

> **要点**：`@ExtendWith(MockitoExtension.class)` + `@Mock/@InjectMocks`；业务异常用 `assertThrows` + 校验 `ErrorCode.getCode()`；Mapper 调用次数用 `verify(...).never()`。

### 6.2 Controller MockMvc（@WebMvcTest）

```java
package com.qkit.system.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qkit.common.api.R;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void page_returnsPagedList() throws Exception {
        UserVO vo = new UserVO("1", "admin", "超管", "管理员",
            "a@b.com", "13800138000", "", 1, "男",
            "1", "总公司", "1", "CEO", 0, "正常",
            "127.0.0.1", null, null, null);
        when(userService.page(any())).thenReturn(R.ok(List.of(vo), 1L, 1L, 10L));

        mockMvc.perform(get("/admin-api/system/user/page")
                .param("pageNum", "1").param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].username").value("admin"))
            .andExpect(jsonPath("$.total").value(1));
    }
}
```

> **注意**：Sa-Token 在 `@WebMvcTest` 默认会拦截 `@SaCheckPermission`，测试时**必须**在配置类排除 `SaTokenInterceptor`，或用 `@AutoConfigureMockMvc(addFilters = false)`。

### 6.3 Vue 组件测试（Vitest + happy-dom）

```ts
// views/system/user/__tests__/UserFormDialog.spec.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserFormDialog from '../components/UserFormDialog.vue'

describe('UserFormDialog', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('opens dialog when open() is invoked', async () => {
    const wrapper = mount(UserFormDialog, { attachTo: document.body })
    expect(wrapper.find('.el-dialog').exists()).toBe(false)
    wrapper.vm.open()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.el-dialog').exists()).toBe(true)
  })

  it('emits save with form data on submit', async () => {
    const wrapper = mount(UserFormDialog)
    wrapper.vm.open()
    await wrapper.vm.$nextTick()
    // 模拟填表 + 点击确定（依赖 Element Plus，必要时 mock）
    await wrapper.find('input[name="username"]').setValue('test01')
    await wrapper.find('button.submit').trigger('click')
    expect(wrapper.emitted('saved')).toBeTruthy()
  })
})
```

> **要点**：`@vue/test-utils` 的 `mount` 必须放在 happy-dom 环境（vitest.config.ts 配 `environment: 'happy-dom'`）；Element Plus 组件在测试时如渲染失败，单独 mock `@/utils/request` 与 Element Plus 即可。

## 7. Git 与 Commit

### 7.1 分支

- `main` — 主分支，受保护
- `feature/<module>-<name>` — 功能分支
- `fix/<module>-<name>` — 修复分支

### 7.2 Commit 规范（Conventional Commits）

```
<type>(<scope>): <subject>

<body>

<footer>
```

- type: `feat / fix / docs / style / refactor / perf / test / chore / build / ci`
- scope: 模块名（`system` / `framework` / `common` / `frontend`）
- subject: 50 字符内，动词开头
- body: 72 字符换行，说明 what & why

示例：
```
feat(system): 新增用户管理 CRUD

- 新增 UserController / Service / Mapper
- 新增前端 user 列表页 + 表单对话框
- @SaCheckPermission 接入
- @OperLog 自动记录

Closes #12
```

## 8. 文档规范

- 公共 API（Controller / Service / Mapper / Convert）必须有 Javadoc 类注释
- 方法注释非强制，但**复杂业务**必须有
- 字段注释：`/** xxx */` 在 DDL 和 Entity 都写

## 9. 一键检查命令

后端：
```bash
mvn -B checkstyle:check spotbugs:check
```

前端：
```bash
pnpm lint
pnpm type-check   # vue-tsc --noEmit
```
