-- ==========================================================
-- V1.0.2__sys_config.sql  系统参数配置表 + 菜单 + 种子数据
-- ==========================================================

-- ---------- 系统参数配置表 ----------
CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL COMMENT '主键',
  `config_name`  VARCHAR(100) NOT NULL DEFAULT '' COMMENT '参数名称',
  `config_key`   VARCHAR(100) NOT NULL DEFAULT '' COMMENT '参数键名（程序取值的Key）',
  `config_value` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '参数键值',
  `config_type`  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否系统内置: Y是 N否（内置不可删）',
  `remark`       VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  `create_by`    BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人',
  `create_time`  DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by`    BIGINT       NOT NULL DEFAULT 0 COMMENT '更新人',
  `update_time`  DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数配置表';

-- ---------- 系统管理 → 参数设置 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(180, '参数设置', 'C', 1, '/system/config', 'system/config/index', 'system:config:page', 'Operation', 9, 0, 1, 1, NOW()),
(181, '配置查询', 'F', 180, NULL, NULL, 'system:config:page',   NULL, 1, 0, 1, 1, NOW()),
(182, '配置列表', 'F', 180, NULL, NULL, 'system:config:list',   NULL, 2, 0, 1, 1, NOW()),
(183, '新增配置', 'F', 180, NULL, NULL, 'system:config:create', NULL, 3, 0, 1, 1, NOW()),
(184, '编辑配置', 'F', 180, NULL, NULL, 'system:config:update', NULL, 4, 0, 1, 1, NOW()),
(185, '删除配置', 'F', 180, NULL, NULL, 'system:config:delete', NULL, 5, 0, 1, 1, NOW());

-- admin 角色获得参数设置菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 180 AND 185 AND del_flag = 0;

-- ---------- 参数配置种子数据 ----------
INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, remark, create_by, create_time) VALUES
(1, '用户初始密码',   'sys.user.initPassword',   '123456',     'Y', '新用户默认初始密码',             1, NOW()),
(2, '登录验证码开关', 'sys.login.captchaEnabled','true',       'Y', '登录时是否显示图形验证码',       1, NOW()),
(3, 'IP登录失败锁定次数', 'sys.login.ipRetryLimit', '5',      'N', '同一 IP 密码连续输错锁定次数',   1, NOW()),
(4, '上传文件大小上限',   'sys.upload.maxSize',  '10',         'N', '上传文件大小上限(MB)',          1, NOW()),
(5, '系统首页皮肤',       'sys.index.skinName',  'skin-blue',  'N', '系统首页皮肤',                   1, NOW());