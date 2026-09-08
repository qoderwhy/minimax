-- ==========================================================
-- V1.0.0__init.sql  建表脚本
-- ==========================================================

-- ---------- 用户 ----------
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL COMMENT '主键',
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
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=正常 1=停用',
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

-- ---------- 角色 ----------
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `name` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '角色编码',
  `data_scope` TINYINT NOT NULL DEFAULT 1 COMMENT '数据权限:1=全部 2=本部门及下级 3=本部门 4=仅本人 5=自定义',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=正常 1=停用',
  `remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ---------- 用户角色关联 ----------
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ---------- 部门 ----------
CREATE TABLE `sys_dept` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `name` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '部门名称',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '上级部门ID',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `leader` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '负责人',
  `phone` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '联系电话',
  `email` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '邮箱',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=正常 1=停用',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ---------- 岗位 ----------
CREATE TABLE `sys_post` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `code` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '岗位编码',
  `name` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '岗位名称',
  `dept_id` BIGINT NOT NULL DEFAULT 0 COMMENT '部门ID',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=正常 1=停用',
  `remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`, `del_flag`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ---------- 用户岗位关联 ----------
CREATE TABLE `sys_user_post` (
  `user_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户岗位关联表';

-- ---------- 菜单 ----------
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '菜单名称',
  `type` CHAR(1) NOT NULL DEFAULT '' COMMENT '类型:M=目录 C=菜单 F=按钮',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '上级菜单',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
  `perm` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '权限标识',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` TINYINT NOT NULL DEFAULT 0 COMMENT '是否显示:0=显示 1=隐藏',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=禁用 1=启用',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_perm` (`perm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- ---------- 角色菜单关联 ----------
CREATE TABLE `sys_role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ---------- 角色部门（数据权限自定义） ----------
CREATE TABLE `sys_role_dept` (
  `role_id` BIGINT NOT NULL,
  `dept_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色部门关联表';

-- ---------- 字典分类 ----------
CREATE TABLE `sys_dict` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '字典名称',
  `type` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '字典类型',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
  `remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典分类表';

-- ---------- 字典项 ----------
CREATE TABLE `sys_dict_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dict_type` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '字典类型',
  `label` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '显示值',
  `value` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '存储值',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
  `css_class` VARCHAR(50) NOT NULL DEFAULT '' COMMENT 'CSS 类',
  `remark` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_by` BIGINT NOT NULL DEFAULT 0,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项表';

-- ---------- 操作日志 ----------
CREATE TABLE `sys_oper_log` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `module` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '模块名',
  `name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '操作名',
  `user_id` BIGINT NOT NULL DEFAULT 0 COMMENT '操作人ID',
  `username` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '操作人账号',
  `ip` VARCHAR(50) NOT NULL DEFAULT '' COMMENT 'IP',
  `user_agent` VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'UA',
  `method` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '方法签名',
  `request_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'URL',
  `request_method` VARCHAR(10) NOT NULL DEFAULT '' COMMENT 'HTTP method',
  `request_params` TEXT COMMENT '入参JSON',
  `response_result` TEXT COMMENT '返回结果摘要',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=成功 1=失败',
  `error_msg` TEXT COMMENT '异常堆栈',
  `cost_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时(毫秒)',
  `oper_time` DATETIME DEFAULT NULL COMMENT '操作时间',
  `create_by` BIGINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ---------- 登录日志 ----------
CREATE TABLE `sys_login_log` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '用户名',
  `ip` VARCHAR(50) NOT NULL DEFAULT '' COMMENT 'IP',
  `user_agent` VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'UA',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=成功 1=失败',
  `message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '提示信息',
  `login_time` DATETIME DEFAULT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';
