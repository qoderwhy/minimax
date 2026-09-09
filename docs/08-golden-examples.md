# 08 · 黄金示例代码

> 新增任何业务模块前，**必须**先读完本文档的"用户管理"端到端示例，照着抄。
> 这是 03/05/06/07 规范的具体落地。代码风格不达此标准 = 不通过。

## 1. 端到端示例：用户管理（B 级）

### 1.1 涉及文件清单

```
qkit-system/
├── src/main/java/com/qkit/system/
│   ├── controller/admin/UserController.java
│   ├── service/UserService.java
│   ├── service/impl/UserServiceImpl.java
│   ├── manager/UserManager.java              # 本例无（纯 CRUD 不需要）
│   ├── mapper/UserMapper.java
│   ├── domain/entity/User.java
│   ├── domain/dto/UserSaveDTO.java
│   ├── domain/dto/UserQueryDTO.java
│   ├── domain/vo/UserVO.java
│   ├── domain/vo/UserDetailVO.java
│   └── convert/UserConvert.java
└── src/main/resources/mapper/
    └── UserMapper.xml                        # 本例无（简单查询走 LambdaQueryWrapper）

qkit-admin/src/main/resources/db/migration/
└── V1.0.0__sys_user.sql

frontend/src/
├── api/system/user.ts
├── types/system/user.ts
├── views/system/user/
│   ├── index.vue
│   └── components/
│       ├── UserFormDialog.vue
│       └── AssignRoleDialog.vue
```

### 1.2 DDL

```sql
-- V1.0.0__sys_user.sql
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '登录名',
  `password` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '密码(BCrypt)',
  `nickname` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '昵称',
  `real_name` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '真实姓名',
  `email` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '邮箱',
  `phone` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '手机号',
  `avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
  `sex` TINYINT NOT NULL DEFAULT 0 COMMENT '性别:0=未知 1=男 2=女',
  `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
  `post_id` BIGINT NOT NULL DEFAULT 0 COMMENT '岗位ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=停用 1=正常',
  `login_ip` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`, `del_flag`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### 1.3 Entity

```java
package com.qkit.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qkit.common.entity.BaseEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@TableName("sys_user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer sex;
    private Long deptId;
    private Long postId;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginDate;
    private String remark;

    @TableLogic
    private Integer delFlag;
}
```

### 1.4 SaveDTO

```java
package com.qkit.system.domain.dto;

import com.qkit.common.group.SaveGroup;
import com.qkit.common.group.UpdateGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 新增/更新用户入参 record。
 * <p>record component 上的 validation 注解在 {@code @RequestBody @Valid} 链路下生效；
 * 分组校验通过 {@code groups} 指定（{@link SaveGroup} / {@link UpdateGroup}）。</p>
 */
public record UserSaveDTO(
    @NotNull(message = "ID不能为空", groups = UpdateGroup.class) Long id,
    @NotBlank(message = "登录名不能为空", groups = SaveGroup.class)
    @Size(max = 30, message = "登录名长度不能超过30") String username,
    @Size(min = 8, max = 32, message = "密码长度必须在8-32位之间", groups = SaveGroup.class) String password,
    String nickname,
    String realName,
    @Email(message = "邮箱格式不正确") String email,
    String phone,
    Integer sex,
    Long deptId,
    Long postId,
    Integer status,
    String remark
) implements Serializable {
}
```

### 1.5 QueryDTO

```java
package com.qkit.system.domain.dto;

import java.io.Serializable;

/**
 * 用户查询条件 record。分页字段必填，默认值由 Controller 在调用前用 {@code withDefaults()} 注入。
 * <p>{@link #of(Long, Long)} 工厂用于 Controller 在绑定分页参数后构造完整 Query，
 * 避免在 service 内反复 null-check 分页字段。</p>
 */
public record UserQueryDTO(
    String username,
    String phone,
    Integer status,
    Long deptId,
    Long pageNum,
    Long pageSize
) implements Serializable {

    public static UserQueryDTO of(Long pageNum, Long pageSize) {
        return new UserQueryDTO(null, null, null, null, pageNum, pageSize);
    }
}
```

### 1.6 VO

```java
package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 VO record。主键自增，Long 直接序列化为数字，无 JS 精度问题。
 * <p>字典翻译字段（{@code sexLabel / statusLabel}）由 Service 层填充；详情专用字段
 * （{@code roleIds}）由 detail 接口专用 VO {@link UserDetailVO} 提供。</p>
 */
@Schema(description = "用户 VO")
public record UserVO(
    Long id,
    String username,
    String nickname,
    String realName,
    String email,
    String phone,
    String avatar,
    Integer sex,
    String sexLabel,
    Long deptId,
    String deptName,
    Long postId,
    String postName,
    Integer status,
    String statusLabel,
    String loginIp,
    LocalDateTime loginDate,
    LocalDateTime createTime,
    List<Long> roleIds
) {
}
```

### 1.7 Convert

```java
package com.qkit.system.convert;

import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.UserDetailVO;
import com.qkit.system.domain.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserVO toVO(User entity);

    List<UserVO> toVOList(List<User> list);

    UserDetailVO toDetailVO(User entity);

    User toEntity(UserSaveDTO dto);

    /** 更新用转换器：忽略 password 字段，避免前端未传时覆盖原密码 */
    @Mapping(target = "password", ignore = true)
    User toUpdateEntity(UserSaveDTO dto);
}
```

### 1.8 Mapper

```java
package com.qkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qkit.system.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 1.9 Service

```java
package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.vo.UserDetailVO;
import com.qkit.system.domain.vo.UserVO;

import java.util.List;

public interface UserService {

    R<List<UserVO>> page(UserQueryDTO query);

    R<UserDetailVO> detail(Long id);

    R<Long> create(UserSaveDTO dto);

    R<Boolean> update(UserSaveDTO dto);

    R<Boolean> delete(List<Long> ids);

    R<Boolean> resetPassword(Long userId, String newPassword);

    R<Boolean> assignRole(Long userId, List<Long> roleIds);
}
```

### 1.10 ServiceImpl

```java
package com.qkit.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.R;
import com.qkit.common.enums.ErrorCode;
import com.qkit.common.exception.BusinessException;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.UserDetailVO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.service.UserRoleService;
import com.qkit.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final UserConvert userConvert;

    @Override
    public R<List<UserVO>> page(UserQueryDTO query) {
        Page<User> page = Page.of(query.pageNum(), query.pageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .like(StrUtil.isNotBlank(query.username()), User::getUsername, query.username())
            .like(StrUtil.isNotBlank(query.phone()), User::getPhone, query.phone())
            .eq(query.status() != null, User::getStatus, query.status())
            .eq(query.deptId() != null, User::getDeptId, query.deptId())
            .orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(page, wrapper);
        List<UserVO> voList = userConvert.toVOList(result.getRecords());
        return R.ok(voList, result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Override
    public R<UserDetailVO> detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        // record 不可变，通过构造器注入 roleIds
        UserVO base = userConvert.toVO(user);
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(id);
        UserDetailVO vo = new UserDetailVO(
            base.id(), base.username(), base.nickname(), base.realName(),
            base.email(), base.phone(), base.avatar(), base.sex(), base.sexLabel(),
            base.deptId(), base.deptName(), base.postId(), base.postName(),
            base.status(), base.statusLabel(), base.loginIp(), base.loginDate(),
            base.createTime(), roleIds
        );
        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(UserSaveDTO dto) {
        // 1. 唯一性校验
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.username()));
        if (count > 0) throw new BusinessException(ErrorCode.USER_EXISTS);

        // 2. 保存
        User user = userConvert.toEntity(dto);
        user.setPassword(BCrypt.hashpw(dto.password()));
        userMapper.insert(user);
        return R.ok(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(UserSaveDTO dto) {
        User exist = userMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        // 用户名变更检查
        if (!exist.getUsername().equals(dto.username())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.username())
                .ne(User::getId, dto.id()));
            if (count > 0) throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        User update = userConvert.toUpdateEntity(dto);  // MapStruct 显式忽略 password 字段
        userMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        userMapper.deleteBatchIds(ids);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> resetPassword(Long userId, String newPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(user);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> assignRole(Long userId, List<Long> roleIds) {
        userRoleService.saveByUserId(userId, roleIds);
        return R.ok(true);
    }
}
```

### 1.11 Controller

```java
package com.qkit.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.qkit.common.api.R;
import com.qkit.common.group.SaveGroup;
import com.qkit.common.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.vo.UserDetailVO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/admin-api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @SaCheckPermission("system:user:page")
    public R<List<UserVO>> page(UserQueryDTO query) {
        // 分页字段兜底（GET 参数可能未传）
        if (query.pageNum() == null || query.pageSize() == null) {
            query = UserQueryDTO.of(
                query.pageNum() == null ? 1L : query.pageNum(),
                query.pageSize() == null ? 10L : query.pageSize()
            );
        }
        return userService.page(query);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/detail/{id}")
    @SaCheckPermission("system:user:detail")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return userService.detail(id);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/create")
    @SaCheckPermission("system:user:create")
    @OperLog(module = "用户管理", name = "新增用户")
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) UserSaveDTO dto) {
        return userService.create(dto);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    @SaCheckPermission("system:user:update")
    @OperLog(module = "用户管理", name = "更新用户")
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) UserSaveDTO dto) {
        return userService.update(dto);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:user:delete")
    @OperLog(module = "用户管理", name = "删除用户")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return userService.delete(ids);
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password")
    @SaCheckPermission("system:user:reset-password")
    @OperLog(module = "用户管理", name = "重置密码")
    public R<Boolean> resetPassword(@RequestParam Long userId, @RequestParam String newPassword) {
        return userService.resetPassword(userId, newPassword);
    }

    @Operation(summary = "分配角色")
    @PutMapping("/assign-role")
    @SaCheckPermission("system:user:assign-role")
    @OperLog(module = "用户管理", name = "分配角色")
    public R<Boolean> assignRole(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        return userService.assignRole(userId, roleIds);
    }
}
```

### 1.12 前端 types

```ts
// types/system/user.ts
export interface UserVO {
  id: string
  username: string
  nickname: string
  realName: string
  email: string
  phone: string
  avatar: string
  sex: number
  sexLabel?: string
  deptId: string
  deptName?: string
  postId: string
  postName?: string
  status: number
  statusLabel?: string
  loginIp: string
  loginDate: string
  createTime: string
}

export interface UserDetailVO extends UserVO {
  roleIds: string[]
}

export interface UserSaveDTO {
  id?: string
  username: string
  password?: string
  nickname?: string
  realName?: string
  email?: string
  phone?: string
  sex?: number
  deptId?: string
  postId?: string
  status?: number
  remark?: string
}

export interface UserQueryDTO {
  username?: string
  phone?: string
  status?: number
  deptId?: string
  pageNum: number
  pageSize: number
}
```

### 1.13 前端 api

```ts
// api/system/user.ts
import request from '@/utils/request'
import type { UserVO, UserDetailVO, UserSaveDTO, UserQueryDTO } from '@/types/system/user'

export const pageUser = (params: UserQueryDTO) =>
  request.get<R<UserVO[]>>({ url: '/admin-api/system/user/page', params })

export const getUserDetail = (id: string) =>
  request.get<R<UserDetailVO>>({ url: `/admin-api/system/user/detail/${id}` })

export const createUser = (data: UserSaveDTO) =>
  request.post<R<string>>({ url: '/admin-api/system/user/create', data })

export const updateUser = (data: UserSaveDTO) =>
  request.put<R<boolean>>({ url: '/admin-api/system/user/update', data })

export const deleteUser = (ids: string[]) =>
  request.del<R<boolean>>({ url: '/admin-api/system/user/delete', data: ids })

export const resetPassword = (userId: string, newPassword: string) =>
  request.put<R<boolean>>({
    url: '/admin-api/system/user/reset-password',
    params: { userId, newPassword }
  })

export const assignUserRole = (userId: string, roleIds: string[]) =>
  request.put<R<boolean>>({
    url: '/admin-api/system/user/assign-role',
    params: { userId },
    data: roleIds
  })
```

### 1.14 前端列表页（精简版）

```vue
<!-- views/system/user/index.vue -->
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUser, deleteUser, createUser, updateUser } from '@/api/system/user'
import type { UserVO, UserQueryDTO, UserSaveDTO } from '@/types/system/user'
import UserFormDialog from './components/UserFormDialog.vue'

const list = ref<UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive<UserQueryDTO>({ pageNum: 1, pageSize: 10 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await pageUser(query)
    list.value = res.data
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => { query.pageNum = 1; fetchData() }
const handleReset = () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, username: '', phone: '', status: undefined, deptId: undefined })
  fetchData()
}

const formDialogRef = ref()
const handleCreate = () => formDialogRef.value.open()
const handleUpdate = (row: UserVO) => formDialogRef.value.open(row)
const handleDelete = async (row: UserVO) => {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser([row.id])
  ElMessage.success('删除成功')
  fetchData()
}

const onSaved = () => { fetchData() }
fetchData()
</script>

<template>
  <div class="page">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" class="search-bar">
      <el-form-item label="登录名">
        <el-input v-model="query.username" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="query.status" dict-type="sys_common_status" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button v-permission="['system:user:create']" type="primary" @click="handleCreate">
        新增用户
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="username" label="登录名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="deptName" label="部门" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="120" />
      <el-table-column prop="statusLabel" label="状态" min-width="80">
        <template #default="{ row }">
          <DictTag dict-type="sys_common_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="['system:user:update']" type="primary" link @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button v-permission="['system:user:delete']" type="danger" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="fetchData"
      @size-change="handleQuery"
    />

    <!-- 表单对话框 -->
    <UserFormDialog ref="formDialogRef" @saved="onSaved" />
  </div>
</template>
```

## 2. 端到端示例：部门管理（B 级 · 树形）

> 与 §1 用户管理并列。**重点演示**：① `parent_id` 自引用树形查询（避免 Mapper XML 写 WITH RECURSIVE，详见 06 §4.1.4）；② 父级联删除校验（子部门 / 部门下用户）；③ 前端 `<el-tree>` + 懒加载。代码复用 §1 样板处只展示**树形特异部分**。

### 2.1 涉及文件清单

```
qkit-system/
├── src/main/java/com/qkit/system/
│   ├── controller/admin/DeptController.java
│   ├── service/DeptService.java
│   ├── service/impl/DeptServiceImpl.java
│   ├── domain/entity/Dept.java
│   ├── domain/vo/DeptTreeVO.java
│   ├── domain/vo/DeptSimpleVO.java
│   └── convert/DeptConvert.java
└── src/main/resources/mapper/
    └── DeptMapper.xml（仅本例需要，复杂子树过滤走 XML）

frontend/src/
├── api/system/dept.ts
├── types/system/dept.ts
└── views/system/dept/
    └── index.vue                              # el-tree + 工具栏
```

### 2.2 DDL

复用 04 §4.4 sys_dept 定义。关键字段：`parent_id BIGINT`（根 0）、`name`、`leader`、`status`。

### 2.3 Entity

```java
package com.qkit.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qkit.common.entity.BaseEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@TableName("sys_dept")
public class Dept extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Long parentId;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;

    @TableLogic
    private Integer delFlag;
}
```

### 2.4 TreeVO（带 label-value 适配 el-tree）

```java
package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** el-tree 数据源：label=name，value=id 字符串，children=子节点。 */
@Schema(description = "部门树节点")
public record DeptTreeVO(
    Long id,
    Long parentId,
    String label,       // el-tree 节点显示（= name）
    String value,       // el-tree 节点值（= id 字符串）
    List<DeptTreeVO> children
) {
    public static DeptTreeVO from(Long id, Long parentId, String name, List<DeptTreeVO> children) {
        return new DeptTreeVO(id, parentId, name, String.valueOf(id), children);
    }
}
```

### 2.5 SimpleVO（下拉专用，无 children）

```java
public record DeptSimpleVO(
    Long id,
    String name,
    Long parentId
) {}
```

### 2.6 Convert

```java
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DeptConvert {
    DeptConvert INSTANCE = Mappers.getMapper(DeptConvert.class);
    DeptSimpleVO toSimpleVO(Dept entity);
    List<DeptSimpleVO> toSimpleVOList(List<Dept> list);
    DeptTreeVO toTreeVO(Dept entity);
}
```

### 2.7 Mapper

```java
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
    // 简单 CRUD 用 LambdaQueryWrapper；子树批量查询走 DeptMapper.xml
    List<Dept> selectByNameLike(@Param("name") String name);
}
```

### 2.8 Service（接口）

```java
public interface DeptService {
    /** 获取完整部门树（不带子节点懒加载） */
    R<List<DeptTreeVO>> tree(DeptQueryDTO query);

    /** 下拉用简单列表 */
    R<List<DeptSimpleVO>> simpleList();

    R<Long> create(DeptSaveDTO dto);
    R<Boolean> update(DeptSaveDTO dto);
    R<Boolean> delete(List<Long> ids);

    /** 关键：递归取子部门 id 集合（含自身），缓存 5 分钟。详见 06 §4.1.4 */
    List<Long> getChildDeptIds(Long rootDeptId);
}
```

### 2.9 ServiceImpl（**树形核心**）

```java
@Service
@Validated
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;          // 检查部门下用户
    private final DeptConvert deptConvert;
    private final RedisTemplate<String, List<Long>> redisTemplate;

    private static final String DEPT_CHILD_KEY = "dept:child:";

    @Override
    @Transactional(readOnly = true)
    public R<List<DeptTreeVO>> tree(DeptQueryDTO query) {
        // 1. 全表（数据量小可接受；>5000 行建议缓存或按需懒加载）
        List<Dept> all = deptMapper.selectList(null);
        // 2. parent_id 分组成 Map<parentId, List<Dept>>
        Map<Long, List<Dept>> byParent = all.stream()
            .collect(Collectors.groupingBy(Dept::getParentId));
        // 3. 递归构建树（从根节点 parent_id=0 开始）
        List<DeptTreeVO> roots = byParent.getOrDefault(0L, List.of()).stream()
            .map(d -> buildTree(d, byParent))
            .toList();
        return R.ok(roots);
    }

    private DeptTreeVO buildTree(Dept dept, Map<Long, List<Dept>> byParent) {
        List<DeptTreeVO> children = byParent.getOrDefault(dept.getId(), List.of()).stream()
            .map(child -> buildTree(child, byParent))
            .toList();
        return DeptTreeVO.from(dept.getId(), dept.getParentId(), dept.getName(), children);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        for (Long id : ids) {
            // 1. 检测子部门
            List<Long> children = getChildDeptIds(id);
            if (children.size() > 1) throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
            // 2. 检测部门下用户
            Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeptId, id));
            if (userCount > 0) throw new BusinessException(ErrorCode.DEPT_HAS_USER);
        }
        deptMapper.deleteBatchIds(ids);
        // 3. 清缓存
        ids.forEach(id -> redisTemplate.delete(DEPT_CHILD_KEY + id));
        return R.ok(true);
    }

    @Override
    public List<Long> getChildDeptIds(Long rootDeptId) {
        String key = DEPT_CHILD_KEY + rootDeptId;
        List<Long> cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        List<Dept> all = deptMapper.selectList(null);
        Map<Long, List<Long>> parentToChildren = all.stream()
            .collect(Collectors.groupingBy(
                Dept::getParentId,
                Collectors.mapping(Dept::getId, Collectors.toList())));
        // 自下而上反查
        List<Long> result = new ArrayList<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootDeptId);
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            result.add(id);
            List<Long> kids = parentToChildren.getOrDefault(id, List.of());
            kids.forEach(stack::push);
        }
        redisTemplate.opsForValue().set(key, result, Duration.ofMinutes(5));
        return result;
    }

    // create / update / simpleList 略，按 §1.10 模式套
}
```

> **关键设计**：
> - **避免在 Mapper XML 写 `WITH RECURSIVE`**：06 §4.1.4 反例明确禁止（会触发 MyBatis-Plus 拦截器死循环）。子树在 Service 层 Java 内存里递归，安全。
> - **缓存粒度**：每个部门 id 单独缓存其子树，避免全表缓存爆炸。
> - **级联删除**：先查子部门 → 抛 DEPT_HAS_CHILDREN；再查用户 → 抛 DEPT_HAS_USER。**不**做物理级联（保留用户可重新分配部门）。

### 2.10 Controller

```java
@Tag(name = "部门管理")
@RestController
@RequestMapping("/admin-api/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "查询部门树")
    @GetMapping("/tree")
    @SaCheckPermission("system:dept:tree")
    public R<List<DeptTreeVO>> tree(DeptQueryDTO query) {
        return deptService.tree(query);
    }

    @Operation(summary = "部门下拉")
    @GetMapping("/simple-list")
    @SaCheckPermission("system:dept:simple-list")
    public R<List<DeptSimpleVO>> simpleList() {
        return deptService.simpleList();
    }

    @Operation(summary = "新增部门")
    @PostMapping("/create")
    @SaCheckPermission("system:dept:create")
    @OperLog(module = "部门管理", name = "新增部门")
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) DeptSaveDTO dto) {
        return deptService.create(dto);
    }

    @Operation(summary = "更新部门")
    @PutMapping("/update")
    @SaCheckPermission("system:dept:update")
    @OperLog(module = "部门管理", name = "更新部门")
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) DeptSaveDTO dto) {
        return deptService.update(dto);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:dept:delete")
    @OperLog(module = "部门管理", name = "删除部门")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return deptService.delete(ids);
    }
}
```

### 2.11 前端 types

```ts
// types/system/dept.ts
export interface DeptTreeVO {
  id: string
  parentId: string
  label: string
  value: string
  children?: DeptTreeVO[]
}

export interface DeptSimpleVO {
  id: string
  name: string
  parentId: string
}

export interface DeptSaveDTO {
  id?: string
  name: string
  parentId?: string
  sort?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
}

export interface DeptQueryDTO {
  name?: string
}
```

### 2.12 前端 api

```ts
// api/system/dept.ts
import request from '@/utils/request'
import type { DeptTreeVO, DeptSimpleVO, DeptSaveDTO, DeptQueryDTO } from '@/types/system/dept'

export const treeDept = (q: DeptQueryDTO) =>
  request.get<R<DeptTreeVO[]>>({ url: '/admin-api/system/dept/tree', params: q })

export const simpleDeptList = () =>
  request.get<R<DeptSimpleVO[]>>({ url: '/admin-api/system/dept/simple-list' })

export const createDept = (data: DeptSaveDTO) =>
  request.post<R<string>>({ url: '/admin-api/system/dept/create', data })

export const updateDept = (data: DeptSaveDTO) =>
  request.put<R<boolean>>({ url: '/admin-api/system/dept/update', data })

export const deleteDept = (ids: string[]) =>
  request.del<R<boolean>>({ url: '/admin-api/system/dept/delete', data: ids })
```

### 2.13 前端树形页（el-tree + 工具栏）

```vue
<!-- views/system/dept/index.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { treeDept, simpleDeptList, createDept, updateDept, deleteDept } from '@/api/system/dept'
import type { DeptTreeVO, DeptSaveDTO } from '@/types/system/dept'

const treeData = ref<DeptTreeVO[]>([])
const expandedKeys = ref<number[]>([])
const loading = ref(false)

const fetchTree = async () => {
  loading.value = true
  try {
    const res = await treeDept({})
    treeData.value = res.data
  } finally { loading.value = false }
}

const handleAppend = (parentNode: DeptTreeVO) => {
  // 打开表单弹窗，parentId=parentNode.id
}

const handleDelete = async (node: DeptTreeVO) => {
  await ElMessageBox.confirm(`确认删除部门「${node.label}」？`, '提示', { type: 'warning' })
  await deleteDept([node.id])
  ElMessage.success('删除成功')
  fetchTree()
}

onMounted(() => {
  fetchTree()
  // 展开第一层
  expandedKeys.value = treeData.value.map(d => Number(d.id))
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button v-permission="['system:dept:create']" type="primary" @click="handleAppend(null)">
        新增顶级部门
      </el-button>
    </div>
    <el-tree
      v-loading="loading"
      :data="treeData"
      :props="{ label: 'label', children: 'children' }"
      node-key="value"
      :default-expanded-keys="expandedKeys.map(String)"
      :expand-on-click-node="false"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span>{{ node.label }}</span>
          <span class="actions">
            <el-button v-permission="['system:dept:create']" type="primary" link
              @click="handleAppend(data)">新增下级</el-button>
            <el-button v-permission="['system:dept:update']" type="primary" link
              @click="handleUpdate(data)">编辑</el-button>
            <el-button v-permission="['system:dept:delete']" type="danger" link
              @click="handleDelete(data)">删除</el-button>
          </span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<style scoped>
.tree-node { flex: 1; display: flex; justify-content: space-between; align-items: center; }
.actions { opacity: 0; }
.tree-node:hover .actions { opacity: 1; }
</style>
```

> **注意**：`node-key` 用 `value`（string），`:default-expanded-keys` 必须转 `String[]`，否则 Element Plus 会因类型不匹配报错。

### 2.14 复制此示例的 checklist

- [ ] DDL 含 `parent_id` 自引用 + `idx_parent_id` 索引
- [ ] Convert 补 `nullValuePropertyMappingStrategy = IGNORE`（见 03 §A.2）
- [ ] `getChildDeptIds` 加缓存，避免每次递归扫全表
- [ ] `delete` 校验子部门 + 部门下用户，按顺序抛 `DEPT_HAS_CHILDREN` / `DEPT_HAS_USER`
- [ ] Controller 5 个方法全部 `@SaCheckPermission`，按 06 §10.1 字典表 perm 字符串
- [ ] 前端 `:default-expanded-keys` 显式 `String[]`

## 3. A 级示例指引（仅占位）

涉及业务规则/状态机/不变量时按 A 级做：

- Entity 放充血逻辑（如 `Order.cancel()` 内部检查状态）
- 引入 `Manager` 层编排放 service
- 详见 `03-structure.md` 第 2.1 节
- **本期所有系统管理模块均为 B 级**，不强制 A 级

## 4. 复制此示例的 checklist

新模块开工前：

- [ ] 阅读本文 1.1 ~ 1.14 全部代码
- [ ] 按 1.2 写 DDL（含主键 id + 公共 5 列）
- [ ] 按 1.3 ~ 1.6 写 Entity/DTO/VO
- [ ] 按 1.7 写 Convert
- [ ] 按 1.8 写 Mapper
- [ ] 按 1.9 ~ 1.10 写 Service/Impl
- [ ] 按 1.11 写 Controller
- [ ] 按 1.12 ~ 1.13 写前端 types/api
- [ ] 按 1.14 写前端列表页
- [ ] 跑通：列表能展示 / 新增 / 编辑 / 删除 / 校验
