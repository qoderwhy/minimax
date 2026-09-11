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
│   ├── mapper/UserMapper.java
│   ├── domain/entity/User.java
│   ├── domain/dto/UserSaveDTO.java
│   ├── domain/dto/UserQueryDTO.java
│   ├── domain/dto/UserResetPasswordDTO.java
│   ├── domain/vo/UserVO.java
│   └── convert/UserConvert.java
└── src/main/resources/mapper/                # 本例无 XML（简单查询走 LambdaQueryWrapper）

qkit-admin/src/main/resources/db/migration/
└── V1.0.0__init.sql                          # 全库 DDL（含 sys_user）

frontend/src/
├── api/system/user.ts                        # 接口封装 + TS 类型（同文件）
└── views/system/user/index.vue               # 列表页（表单/授权弹窗内联）
```

### 1.2 DDL

```sql
-- V1.0.0__init.sql（sys_user 节选）
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

import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
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
    @NotBlank(message = "密码不能为空", groups = SaveGroup.class)
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
 * 用户查询条件 record。分页字段可为空，由 Controller 用 {@code withPageDefaults()}
 * 补齐，保留其余查询条件。
 */
public record UserQueryDTO(
    String username,
    String nickname,
    String phone,
    Integer status,
    Long deptId,
    Long pageNum,
    Long pageSize
) implements Serializable {

    /** 仅补齐分页默认值，保留其余查询条件 */
    public UserQueryDTO withPageDefaults() {
        return new UserQueryDTO(username, nickname, phone, status, deptId,
                pageNum == null ? 1L : pageNum,
                pageSize == null ? 10L : pageSize);
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
 * <p>字典翻译字段（{@code sexLabel / statusLabel}）由 Service 层填充；{@code roleIds}
 * 仅在 detail 接口填充，列表接口为 {@code null}。</p>
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
    String remark,
    List<Long> roleIds
) {
}
```

### 1.7 Convert

```java
package com.qkit.system.convert;

import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.LoginUserVO;
import com.qkit.system.domain.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserVO toVO(User entity);

    List<UserVO> toVOList(List<User> list);

    User toEntity(UserSaveDTO dto);

    /** 更新用转换器：忽略 password 字段，避免前端未传时覆盖原密码 */
    @Mapping(target = "password", ignore = true)
    User toUpdateEntity(UserSaveDTO dto);

    LoginUserVO toLoginUserVO(User user);
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
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserProfileUpdateDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface UserService {

    R<List<UserVO>> page(UserQueryDTO query);

    /** 按查询条件导出用户列表（EasyExcel 写入响应流） */
    void export(UserQueryDTO query, HttpServletResponse response);

    R<UserVO> detail(Long id);

    /** 管理端详情：受数据权限约束，超出可见范围按「不存在」处理 */
    R<UserVO> detailInScope(Long id);

    R<Long> create(UserSaveDTO dto);

    R<Boolean> update(UserSaveDTO dto);

    R<Boolean> delete(List<Long> ids);

    R<Boolean> resetPassword(Long userId, String newPassword);

    R<Boolean> assignRole(Long userId, List<Long> roleIds);

    R<Boolean> changePassword(Long userId, PasswordDTO dto);

    /** 当前登录用户完整资料（含部门、岗位、角色） */
    R<UserVO> profile(Long userId);

    /** 更新当前登录用户资料 */
    R<Boolean> updateProfile(Long userId, UserProfileUpdateDTO dto);

    /** 根据用户名查询（登录用） */
    User getByUsername(String username);

    /** 根据主键查询 */
    User getById(Long id);

    /** 更新登录信息 */
    void updateLoginInfo(Long userId, String ip);
}
```

### 1.10 ServiceImpl

> 以下为对齐**真实签名**的精简示例；完整实现（导出 `export`、字段富化 `enrich`、自锁保护 `guardProtectedAccount`、数据权限等）见 `qkit-system/.../service/impl/UserServiceImpl.java`。

```java
package com.qkit.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qkit.common.api.R;
import com.qkit.common.api.ErrorCode;
import com.qkit.common.exception.BusinessException;
import com.qkit.framework.security.annotation.DataScope;
import com.qkit.system.convert.UserConvert;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.entity.User;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.mapper.UserMapper;
import com.qkit.system.service.UserRoleService;
import com.qkit.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final UserConvert userConvert;

    @Override
    @DataScope(table = "sys_user", deptColumn = "dept_id", userColumn = "create_by")
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
    public R<UserVO> detail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        UserVO vo = userConvert.toVO(user);
        // record 不可变：用 roleIds 重建 VO（列表接口 roleIds 为 null，详情接口填充）
        vo = new UserVO(vo.id(), vo.username(), vo.nickname(), vo.realName(),
            vo.email(), vo.phone(), vo.avatar(), vo.sex(), vo.sexLabel(),
            vo.deptId(), vo.deptName(), vo.postId(), vo.postName(),
            vo.status(), vo.statusLabel(), vo.loginIp(), vo.loginDate(),
            vo.createTime(), vo.remark(), userRoleService.getRoleIdsByUserId(id));
        return R.ok(vo);
    }

    @Override
    public R<UserVO> detailInScope(Long id) {
        // 管理端详情受数据权限约束：超出可见范围按「不存在」处理（完整实现见源码）
        return detail(id);
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
        if (user.getStatus() == null) user.setStatus(1);
        userMapper.insert(user);
        return R.ok(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(UserSaveDTO dto) {
        User exist = userMapper.selectById(dto.id());
        if (exist == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        // 自锁/内置账号保护：禁止停用或重命名 admin、禁止停用自己（完整实现见 guardProtectedAccount）

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
        if (ids.contains(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ErrorCode.USER_CANNOT_DELETE_SELF);
        }
        // 内置管理员不可删；级联清理 user_role/user_post、强制下线、清权限缓存（完整实现见源码）
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
import cn.dev33.satoken.stp.StpUtil;
import com.qkit.common.api.R;
import com.qkit.common.validation.group.SaveGroup;
import com.qkit.common.validation.group.UpdateGroup;
import com.qkit.framework.log.annotation.OperLog;
import com.qkit.framework.repeat.annotation.RepeatSubmit;
import com.qkit.system.domain.dto.PasswordDTO;
import com.qkit.system.domain.dto.UserProfileUpdateDTO;
import com.qkit.system.domain.dto.UserQueryDTO;
import com.qkit.system.domain.dto.UserResetPasswordDTO;
import com.qkit.system.domain.dto.UserSaveDTO;
import com.qkit.system.domain.vo.UserVO;
import com.qkit.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")   // 统一前缀 /admin-api 由 server.servlet.context-path 提供，此处不重复
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @SaCheckPermission("system:user:page")
    public R<List<UserVO>> page(UserQueryDTO query) {
        query = query.withPageDefaults();
        return userService.page(query);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/detail/{id}")
    @SaCheckPermission("system:user:detail")
    public R<UserVO> detail(@PathVariable Long id) {
        // 走受数据权限约束的查询，避免越权读取其他数据范围内的用户
        return userService.detailInScope(id);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/create")
    @SaCheckPermission("system:user:create")
    @OperLog(module = "用户管理", name = "新增用户")
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) UserSaveDTO dto) {
        return userService.create(dto);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    @SaCheckPermission("system:user:update")
    @OperLog(module = "用户管理", name = "更新用户")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) UserSaveDTO dto) {
        return userService.update(dto);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:user:delete")
    @OperLog(module = "用户管理", name = "删除用户")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return userService.delete(ids);
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password")
    @SaCheckPermission("system:user:reset-password")
    @OperLog(module = "用户管理", name = "重置密码")
    @RepeatSubmit
    public R<Boolean> resetPassword(@RequestBody @Valid UserResetPasswordDTO dto) {
        return userService.resetPassword(dto.userId(), dto.newPassword());
    }

    @Operation(summary = "分配角色")
    @PutMapping("/assign-role")
    @SaCheckPermission("system:user:assign-role")
    @OperLog(module = "用户管理", name = "分配角色")
    @RepeatSubmit
    public R<Boolean> assignRole(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        return userService.assignRole(userId, roleIds);
    }

    @Operation(summary = "导出用户")
    @GetMapping("/export")
    @SaCheckPermission("system:user:export")
    @OperLog(module = "用户管理", name = "导出用户")
    public void export(UserQueryDTO query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("用户列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        userService.export(query, response);
    }

    // ==================== 个人中心（本人） ====================

    @Operation(summary = "个人资料详情")
    @GetMapping("/profile")
    public R<UserVO> profile() {
        return userService.profile(StpUtil.getLoginIdAsLong());
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    @OperLog(module = "个人中心", name = "更新个人资料")
    @RepeatSubmit
    public R<Boolean> updateProfile(@RequestBody @Valid UserProfileUpdateDTO dto) {
        return userService.updateProfile(StpUtil.getLoginIdAsLong(), dto);
    }

    @Operation(summary = "修改密码（本人）")
    @PutMapping("/profile/password")
    @OperLog(module = "个人中心", name = "修改密码")
    @RepeatSubmit
    public R<Boolean> changePassword(@RequestBody @Valid PasswordDTO dto) {
        return userService.changePassword(StpUtil.getLoginIdAsLong(), dto);
    }
}
```

### 1.12 前端 types（就近定义在 api 文件）

```ts
// api/system/user.ts（类型就近定义；项目没有 types/system/user.ts）
export interface UserItem {
  id: number
  username: string
  nickname: string
  phone?: string
  email?: string
  status: number
  deptId?: number
  deptName?: string
  postId?: number
  postName?: string
  createTime?: string
  /** 仅 detail 接口返回，用于回显已分配角色 */
  roleIds?: number[]
}

export interface UserSave {
  id?: number
  username: string
  nickname: string
  password?: string
  phone?: string
  email?: string
  status: number
  deptId?: number
  postId?: number
  roleIds: number[]
}

export interface UserQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  nickname?: string
  phone?: string
  status?: number
  deptId?: number
}
```

### 1.13 前端 api

```ts
// api/system/user.ts
import request from '@/utils/request'

export function pageUser(params: UserQuery) {
  return request.page<UserItem>({ url: '/admin-api/system/user/page', params })  // { list, total }
}

export function getUser(id: number) {
  return request.get<UserItem>({ url: `/admin-api/system/user/detail/${id}` })
}

export function saveUser(data: UserSave): Promise<number | boolean> {
  return data.id
    ? request.put<boolean>({ url: '/admin-api/system/user/update', data })
    : request.post<number>({ url: '/admin-api/system/user/create', data })
}

export function assignRole(userId: number, roleIds: number[]) {
  return request.put<boolean>({
    url: '/admin-api/system/user/assign-role',
    params: { userId },
    data: roleIds
  })
}

export function deleteUser(id: number) {
  return request.delete<void>({ url: '/admin-api/system/user/delete', data: [id] })
}

export function resetUserPassword(id: number, password: string) {
  return request.put<void>({
    url: '/admin-api/system/user/reset-password',
    data: { userId: id, newPassword: password }
  })
}

export function exportUser(params: UserQuery) {
  return request.download<Blob>({ url: '/admin-api/system/user/export', params })
}
```

### 1.14 前端列表页（精简版）

```vue
<!-- views/system/user/index.vue（精简示意；真实页面表单/授权弹窗内联在本文件） -->
<script setup lang="ts">
import { pageUser, saveUser, deleteUser } from '@/api/system/user'
import type { UserItem, UserSave, UserQuery } from '@/api/system/user'
import { useCrud } from '@/composables/useCrud'

const {
  query, list, total, loading, fetch, onSearch, onReset,
  dialogVisible, dialogMode, form, formRef, onAdd, onEdit, onSave, onDelete
} = useCrud<UserItem, UserQuery, UserSave>({
  page: pageUser,                 // (query) => Promise<{ list, total }>
  save: saveUser,                 // (form) => Promise
  remove: deleteUser,             // (id) => Promise
  defaultForm: () => ({ username: '', nickname: '', status: 1, roleIds: [] })
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button v-permission="'system:user:create'" type="primary" @click="onAdd">新增用户</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="username" label="登录名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="deptName" label="部门" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="120" />
      <el-table-column label="状态" min-width="80">
        <template #default="{ row }">
          <!-- DictTag 通过默认插槽接收字典值，无 :value prop -->
          <DictTag dict-type="sys_common_status">{{ row.status }}</DictTag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:user:update'" type="primary" link @click="onEdit(row)">编辑</el-button>
          <el-button v-permission="'system:user:delete'" type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="fetch"
      @size-change="onSearch"
    />

    <!-- 表单弹窗（v-model="dialogVisible" / :model="form"）内联在本文件 -->
  </div>
</template>
```

## 2. 端到端示例：部门管理（B 级 · 树形）

> 与 §1 用户管理并列。**重点演示**：① `parent_id` 自引用树形查询（避免 Mapper XML 写 WITH RECURSIVE，详见 06 §4.1.4）；② 父级联删除校验（子部门 / 部门下用户）；③ 前端 `<el-table :tree-props>` 树形展示。代码复用 §1 样板处只展示**树形特异部分**。

### 2.1 涉及文件清单

```
qkit-system/
├── src/main/java/com/qkit/system/
│   ├── controller/admin/DeptController.java
│   ├── service/DeptService.java
│   ├── service/impl/DeptServiceImpl.java
│   ├── domain/entity/Dept.java
│   ├── domain/dto/DeptSaveDTO.java
│   ├── domain/vo/DeptTreeVO.java
│   ├── domain/vo/DeptSimpleVO.java
│   └── convert/DeptConvert.java
└── src/main/resources/mapper/                # 无 XML（树在 Service 内构建）

frontend/src/
├── api/system/dept.ts                        # 接口 + 类型（同文件）
└── views/system/dept/index.vue               # el-table 树形（tree-props）
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

import com.qkit.system.domain.entity.Dept;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 树节点：label=name，value=id 字符串；同时携带可编辑字段用于回显。 */
@Schema(description = "部门树节点")
public record DeptTreeVO(
    Long id,
    Long parentId,
    String label,
    String value,
    Integer sort,
    String leader,
    String phone,
    String email,
    Integer status,
    List<DeptTreeVO> children
) {
    public static DeptTreeVO from(Dept dept, List<DeptTreeVO> children) {
        return new DeptTreeVO(dept.getId(), dept.getParentId(), dept.getName(),
            String.valueOf(dept.getId()), dept.getSort(), dept.getLeader(),
            dept.getPhone(), dept.getEmail(), dept.getStatus(), children);
    }

    /** 替换子节点并保留其余字段（数据权限过滤后重建树时使用） */
    public DeptTreeVO withChildren(List<DeptTreeVO> children) {
        return new DeptTreeVO(id, parentId, label, value, sort, leader, phone, email, status, children);
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
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeptConvert {
    DeptConvert INSTANCE = Mappers.getMapper(DeptConvert.class);
    List<DeptSimpleVO> toSimpleVOList(List<Dept> list);
    DeptSimpleVO toSimpleVO(Dept entity);
}
```

> `DeptTreeVO` **不**经 MapStruct，由 Service 手工调用 `DeptTreeVO.from(dept, children)` 构建（树形递归填充 `children`）。

### 2.7 Mapper

```java
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
    // 仅 BaseMapper；树形在 Service 内构建，无自定义方法、无 XML
}
```

### 2.8 Service（接口）

```java
public interface DeptService {
    /** 部门树（name 为空查全部；支持关键字过滤，保留命中节点及其祖先） */
    R<List<DeptTreeVO>> tree(String name);

    /** 下拉用简单列表（受数据权限约束） */
    R<List<DeptSimpleVO>> simpleList();

    R<Long> create(DeptSaveDTO dto);
    R<Boolean> update(DeptSaveDTO dto);
    R<Boolean> delete(List<Long> ids);
}
```

### 2.9 ServiceImpl（**树形核心**）

```java
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;           // 检查部门下用户
    private final DeptConvert deptConvert;
    private final DataScopeHelper dataScopeHelper; // 可见部门集合 + 缓存失效

    @Override
    @Transactional(readOnly = true)
    public R<List<DeptTreeVO>> tree(String name) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Dept> all = deptMapper.selectList(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSort));
        List<Dept> nodes = (name == null || name.isBlank()) ? all : withAncestors(all, name);
        Map<Long, List<Dept>> byParent = nodes.stream()
            .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        List<DeptTreeVO> fullTree = buildTree(0L, byParent);
        // 数据权限过滤必须在树构建之后：先裁剪会让子节点因父节点缺失而一起丢失
        List<Long> visibleIds = dataScopeHelper.visibleDeptIds(userId);
        Set<Long> visible = visibleIds != null ? new HashSet<>(visibleIds) : null;
        return R.ok(visible != null ? filterTree(fullTree, visible) : fullTree);
    }

    @Override
    @Transactional(readOnly = true)
    @DataScope(table = "sys_dept", deptColumn = "id")
    public R<List<DeptSimpleVO>> simpleList() {
        List<Dept> all = deptMapper.selectList(new LambdaQueryWrapper<Dept>()
            .eq(Dept::getStatus, 1).orderByAsc(Dept::getSort));
        return R.ok(deptConvert.toSimpleVOList(all));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) throw new BusinessException(ErrorCode.BAD_REQUEST);
        // 父子映射只构建一次，避免在循环内重复全表加载
        List<Dept> all = deptMapper.selectList(null);
        Map<Long, List<Long>> parentToChildren = all.stream().collect(Collectors.groupingBy(
            Dept::getParentId, Collectors.mapping(Dept::getId, Collectors.toList())));
        for (Long id : ids) {
            if (countChildren(id, parentToChildren) > 0) throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
            Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, id));
            if (userCount > 0) throw new BusinessException(ErrorCode.DEPT_HAS_USER);
        }
        deptMapper.deleteBatchIds(ids);
        // 事务提交后再失效缓存，避免提交前被并发回填脏数据
        TransactionUtils.afterCommit(dataScopeHelper::invalidateDeptCache);
        return R.ok(true);
    }

    /** 校验上级部门合法：沿目标父部门向上回溯，命中自身说明目标父部门位于自己的子树中 */
    private void validateParent(Long id, Long parentId) {
        // update 中调用；命中抛 ErrorCode.DEPT_PARENT_INVALID（完整实现见源码）
    }

    private int countChildren(Long rootId, Map<Long, List<Long>> parentToChildren) {
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);
        int count = 0;
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            count++;
            parentToChildren.getOrDefault(id, new ArrayList<>()).forEach(stack::push);
        }
        return count - 1;   // 排除自身
    }

    private List<DeptTreeVO> filterTree(List<DeptTreeVO> nodes, Set<Long> visible) {
        return nodes.stream()
            .filter(n -> visible.contains(n.id()))
            .map(n -> n.withChildren(filterTree(n.children(), visible)))
            .collect(Collectors.toList());
    }

    private List<DeptTreeVO> buildTree(Long parentId, Map<Long, List<Dept>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
            .map(d -> DeptTreeVO.from(d, buildTree(d.getId(), byParent)))
            .toList();
    }

    // withAncestors（关键字过滤保留祖先）、create / update / simpleList 完整实现见源码
}
```

> **关键设计**：
> - **避免在 Mapper XML 写 `WITH RECURSIVE`**：06 §4.1.4 反例明确禁止（会触发 MyBatis-Plus 拦截器死循环）。子树在 Service 层 Java 内存里递归，安全。
> - **数据权限**：先构建完整树，再按 `DataScopeHelper.visibleDeptIds(userId)` 用 `filterTree` 裁剪；先裁剪会因父节点缺失而丢失子树。可见部门集合由 `DataScopeHelper` 统一缓存，写操作后 `invalidateDeptCache`。
> - **缓存失效时机**：用 `TransactionUtils.afterCommit(...)` 在事务提交后失效，避免提交前被并发回填脏数据。
> - **级联删除**：先查子部门 → 抛 `DEPT_HAS_CHILDREN`；再查用户 → 抛 `DEPT_HAS_USER`。**不**做物理级联（保留用户可重新分配部门）。

### 2.10 Controller

```java
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")   // 统一前缀 /admin-api 由 server.servlet.context-path 提供，此处不重复
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    @SaCheckPermission("system:dept:tree")
    public R<List<DeptTreeVO>> tree(@RequestParam(required = false) String name) {
        return deptService.tree(name);
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
    @RepeatSubmit
    public R<Long> create(@RequestBody @Validated(SaveGroup.class) DeptSaveDTO dto) {
        return deptService.create(dto);
    }

    @Operation(summary = "更新部门")
    @PutMapping("/update")
    @SaCheckPermission("system:dept:update")
    @OperLog(module = "部门管理", name = "更新部门")
    @RepeatSubmit
    public R<Boolean> update(@RequestBody @Validated(UpdateGroup.class) DeptSaveDTO dto) {
        return deptService.update(dto);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/delete")
    @SaCheckPermission("system:dept:delete")
    @OperLog(module = "部门管理", name = "删除部门")
    @RepeatSubmit
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return deptService.delete(ids);
    }
}
```

### 2.11 前端 types

```ts
// api/system/dept.ts（类型就近定义）
/** 部门树节点（对应后端 DeptTreeVO，label 为部门名称） */
export interface DeptTreeItem {
  id: number
  parentId: number
  label: string
  sort?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
  children?: DeptTreeItem[]
}

/** 部门下拉项（对应后端 DeptSimpleVO，name 为部门名称） */
export interface DeptSimpleItem {
  id: number
  name: string
  parentId: number
}

export interface DeptSave {
  id?: number
  name: string
  parentId: number
  sort: number
  leader?: string
  phone?: string
  email?: string
  status: number
}
```

### 2.12 前端 api

```ts
// api/system/dept.ts
import request from '@/utils/request'

export function treeDept() {
  return request.get<DeptTreeItem[]>({ url: '/admin-api/system/dept/tree' })
}

export function listDept() {
  return request.get<DeptSimpleItem[]>({ url: '/admin-api/system/dept/simple-list' })
}

export function saveDept(data: DeptSave) {
  return data.id
    ? request.put<void>({ url: '/admin-api/system/dept/update', data })
    : request.post<void>({ url: '/admin-api/system/dept/create', data })
}

export function deleteDept(id: number) {
  return request.delete<void>({ url: '/admin-api/system/dept/delete', data: [id] })
}
```

### 2.13 前端树形页（el-tree + 工具栏）

```vue
<!-- views/system/dept/index.vue（精简示意；真实页面用 el-table 的 tree-props 展示树） -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { treeDept, deleteDept } from '@/api/system/dept'
import type { DeptTreeItem } from '@/api/system/dept'

const treeData = ref<DeptTreeItem[]>([])
const loading = ref(false)

const fetchTree = async () => {
  loading.value = true
  try {
    treeData.value = await treeDept()     // 直接返回列表（已解包）
  } finally { loading.value = false }
}

const handleDelete = async (node: DeptTreeItem) => {
  await ElMessageBox.confirm(`确认删除部门「${node.label}」？`, '提示', { type: 'warning' })
  await deleteDept(node.id)
  ElMessage.success('删除成功')
  fetchTree()
}

onMounted(fetchTree)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button v-permission="'system:dept:create'" type="primary" @click="/* 打开表单弹窗，parentId=0 */">
        新增顶级部门
      </el-button>
    </div>

    <!-- 用 el-table 的 tree-props 展示树形，row-key 用 id -->
    <el-table
      v-loading="loading"
      :data="treeData"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="label" label="部门名称" min-width="200" />
      <el-table-column prop="leader" label="负责人" min-width="120" />
      <el-table-column prop="phone" label="联系电话" min-width="140" />
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:dept:create'" type="primary" link @click="/* 新增下级 */">新增下级</el-button>
          <el-button v-permission="'system:dept:update'" type="primary" link @click="/* 编辑 */">编辑</el-button>
          <el-button v-permission="'system:dept:delete'" type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

> **注意**：部门页用 **`<el-table :tree-props="{ children: 'children' }" row-key="id">`** 展示树（**不是** `<el-tree>`）；`DeptTreeItem` **无** `value` 字段。

### 2.14 复制此示例的 checklist

- [ ] DDL 含 `parent_id` 自引用 + `idx_parent_id` 索引
- [ ] Convert 带 `nullValuePropertyMappingStrategy = IGNORE` + `unmappedTargetPolicy = IGNORE`（见 03 §A.2）
- [ ] 树在 Service 内**内存构建**（`DeptTreeVO.from`），禁止在 Mapper XML 写 `WITH RECURSIVE`
- [ ] 数据权限：树**先构建后裁剪**（`filterTree`），否则父节点缺失会丢整棵子树
- [ ] `delete` 校验子部门 + 部门下用户，抛 `DEPT_HAS_CHILDREN` / `DEPT_HAS_USER`；`update` 校验 `DEPT_PARENT_INVALID`
- [ ] Controller 5 个方法全部 `@SaCheckPermission`，写接口加 `@RepeatSubmit`
- [ ] 前端用 `<el-table :tree-props="{ children: 'children' }" row-key="id">` 展示树

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
