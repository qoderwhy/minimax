-- ==========================================================
-- V1.0.1__seed.sql  全部种子数据（菜单/角色/用户/部门/岗位/字典/参数）
-- ==========================================================
-- 注意：本文件属于「已执行过的迁移」时不要直接修改其内容。
-- Flyway 会校验已执行迁移的 checksum，已部署的库会因此启动失败。
-- 若需修正已部署库的数据，请执行 flyway repair（或修正 flyway_schema_history 中的 checksum），
-- 并手工执行等价的数据修正 SQL，例如：
--   UPDATE sys_menu SET visible = 1 WHERE id BETWEEN 180 AND 185;
--   UPDATE sys_config SET config_value = 'Qkit@123' WHERE config_key = 'sys.user.initPassword';
--   INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark)
--     VALUES ('同一IP失败锁定次数', 'sys.login.ipRetryLimit', '20', 'N', '同一来源 IP 连续登录失败锁定次数');
-- 全新环境直接重建数据库即可。
-- ==========================================================

-- ---------- 一级目录 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, icon, sort, visible, status, create_by, create_time) VALUES
(1,  '系统管理', 'M', 0, '/system',   'Layout',          'Setting',   1, 1, 1, 1, NOW()),
(2,  '系统监控', 'M', 0, '/monitor',  'Layout',          'Monitor',   2, 1, 1, 1, NOW()),
(3,  '工具',     'M', 0, '/tool',     'Layout',          'Tools',     3, 1, 1, 1, NOW());

-- ---------- 系统管理 → 用户管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(100, '用户管理', 'C', 1, '/system/user', 'system/user/index', 'system:user:page', 'User', 1, 1, 1, 1, NOW()),
(101, '用户查询', 'F', 100, NULL, NULL, 'system:user:page',           NULL, 1, 1, 1, 1, NOW()),
(102, '用户列表', 'F', 100, NULL, NULL, 'system:user:list',           NULL, 2, 1, 1, 1, NOW()),
(103, '用户详情', 'F', 100, NULL, NULL, 'system:user:detail',         NULL, 3, 1, 1, 1, NOW()),
(104, '新增用户', 'F', 100, NULL, NULL, 'system:user:create',         NULL, 4, 1, 1, 1, NOW()),
(105, '编辑用户', 'F', 100, NULL, NULL, 'system:user:update',         NULL, 5, 1, 1, 1, NOW()),
(106, '删除用户', 'F', 100, NULL, NULL, 'system:user:delete',         NULL, 6, 1, 1, 1, NOW()),
(107, '分配角色', 'F', 100, NULL, NULL, 'system:user:assign-role',    NULL, 7, 1, 1, 1, NOW()),
(108, '重置密码', 'F', 100, NULL, NULL, 'system:user:reset-password', NULL, 8, 1, 1, 1, NOW()),
(109, '导出用户', 'F', 100, NULL, NULL, 'system:user:export',         NULL, 9, 1, 1, 1, NOW());

-- ---------- 系统管理 → 角色管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(110, '角色管理', 'C', 1, '/system/role', 'system/role/index', 'system:role:page', 'UserFilled', 2, 1, 1, 1, NOW()),
(111, '角色分页', 'F', 110, NULL, NULL, 'system:role:page',         NULL, 1, 1, 1, 1, NOW()),
(112, '角色列表', 'F', 110, NULL, NULL, 'system:role:list',         NULL, 2, 1, 1, 1, NOW()),
(113, '新增角色', 'F', 110, NULL, NULL, 'system:role:create',       NULL, 3, 1, 1, 1, NOW()),
(114, '编辑角色', 'F', 110, NULL, NULL, 'system:role:update',       NULL, 4, 1, 1, 1, NOW()),
(115, '删除角色', 'F', 110, NULL, NULL, 'system:role:delete',       NULL, 5, 1, 1, 1, NOW()),
(116, '分配菜单', 'F', 110, NULL, NULL, 'system:role:assign-menu',  NULL, 6, 1, 1, 1, NOW()),
(117, '分配部门', 'F', 110, NULL, NULL, 'system:role:assign-dept',  NULL, 7, 1, 1, 1, NOW()),
(118, '角色详情', 'F', 110, NULL, NULL, 'system:role:detail',       NULL, 8, 1, 1, 1, NOW());

-- ---------- 系统管理 → 菜单管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(120, '菜单管理', 'C', 1, '/system/menu', 'system/menu/index', 'system:menu:tree', 'Menu', 3, 1, 1, 1, NOW()),
(121, '菜单查询', 'F', 120, NULL, NULL, 'system:menu:tree',   NULL, 1, 1, 1, 1, NOW()),
(122, '新增菜单', 'F', 120, NULL, NULL, 'system:menu:create', NULL, 2, 1, 1, 1, NOW()),
(123, '编辑菜单', 'F', 120, NULL, NULL, 'system:menu:update', NULL, 3, 1, 1, 1, NOW()),
(124, '删除菜单', 'F', 120, NULL, NULL, 'system:menu:delete', NULL, 4, 1, 1, 1, NOW());

-- ---------- 系统管理 → 部门管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(130, '部门管理', 'C', 1, '/system/dept', 'system/dept/index', 'system:dept:tree', 'OfficeBuilding', 4, 1, 1, 1, NOW()),
(131, '部门查询', 'F', 130, NULL, NULL, 'system:dept:tree',        NULL, 1, 1, 1, 1, NOW()),
(132, '部门下拉', 'F', 130, NULL, NULL, 'system:dept:simple-list', NULL, 2, 1, 1, 1, NOW()),
(133, '新增部门', 'F', 130, NULL, NULL, 'system:dept:create',      NULL, 3, 1, 1, 1, NOW()),
(134, '编辑部门', 'F', 130, NULL, NULL, 'system:dept:update',      NULL, 4, 1, 1, 1, NOW()),
(135, '删除部门', 'F', 130, NULL, NULL, 'system:dept:delete',      NULL, 5, 1, 1, 1, NOW());

-- ---------- 系统管理 → 岗位管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(140, '岗位管理', 'C', 1, '/system/post', 'system/post/index', 'system:post:page', 'Postcard', 5, 1, 1, 1, NOW()),
(141, '岗位查询', 'F', 140, NULL, NULL, 'system:post:page',   NULL, 1, 1, 1, 1, NOW()),
(142, '岗位下拉', 'F', 140, NULL, NULL, 'system:post:list',   NULL, 2, 1, 1, 1, NOW()),
(143, '新增岗位', 'F', 140, NULL, NULL, 'system:post:create', NULL, 3, 1, 1, 1, NOW()),
(144, '编辑岗位', 'F', 140, NULL, NULL, 'system:post:update', NULL, 4, 1, 1, 1, NOW()),
(145, '删除岗位', 'F', 140, NULL, NULL, 'system:post:delete', NULL, 5, 1, 1, 1, NOW());

-- ---------- 系统管理 → 字典管理 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(150, '字典管理', 'C', 1, '/system/dict', 'system/dict/index', 'system:dict:page', 'Collection', 6, 1, 1, 1, NOW()),
(151, '字典查询', 'F', 150, NULL, NULL, 'system:dict:page',   NULL, 1, 1, 1, 1, NOW()),
(152, '字典下拉', 'F', 150, NULL, NULL, 'system:dict:list',   NULL, 2, 1, 1, 1, NOW()),
(153, '新增字典', 'F', 150, NULL, NULL, 'system:dict:create', NULL, 3, 1, 1, 1, NOW()),
(154, '编辑字典', 'F', 150, NULL, NULL, 'system:dict:update', NULL, 4, 1, 1, 1, NOW()),
(155, '删除字典', 'F', 150, NULL, NULL, 'system:dict:delete', NULL, 5, 1, 1, 1, NOW());

-- ---------- 系统管理 → 操作日志 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(160, '操作日志', 'C', 1, '/system/oper-log', 'system/oper-log/index', 'system:oper-log:page', 'Document', 7, 1, 1, 1, NOW()),
(161, '日志查询', 'F', 160, NULL, NULL, 'system:oper-log:page',   NULL, 1, 1, 1, 1, NOW()),
(162, '删除日志', 'F', 160, NULL, NULL, 'system:oper-log:delete', NULL, 2, 1, 1, 1, NOW()),
(163, '清空日志', 'F', 160, NULL, NULL, 'system:oper-log:clean',  NULL, 3, 1, 1, 1, NOW());

-- ---------- 系统管理 → 登录日志 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(170, '登录日志', 'C', 1, '/system/login-log', 'system/login-log/index', 'system:login-log:page', 'Lock', 8, 1, 1, 1, NOW()),
(171, '日志查询', 'F', 170, NULL, NULL, 'system:login-log:page',   NULL, 1, 1, 1, 1, NOW()),
(172, '删除日志', 'F', 170, NULL, NULL, 'system:login-log:delete', NULL, 2, 1, 1, 1, NOW()),
(173, '清空日志', 'F', 170, NULL, NULL, 'system:login-log:clean',  NULL, 3, 1, 1, 1, NOW());

-- ---------- 系统管理 → 参数设置 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(180, '参数设置', 'C', 1, '/system/config', 'system/config/index', 'system:config:page', 'Operation', 9, 1, 1, 1, NOW()),
(181, '配置查询', 'F', 180, NULL, NULL, 'system:config:page',   NULL, 1, 1, 1, 1, NOW()),
(182, '配置列表', 'F', 180, NULL, NULL, 'system:config:list',   NULL, 2, 1, 1, 1, NOW()),
(183, '新增配置', 'F', 180, NULL, NULL, 'system:config:create', NULL, 3, 1, 1, 1, NOW()),
(184, '编辑配置', 'F', 180, NULL, NULL, 'system:config:update', NULL, 4, 1, 1, 1, NOW()),
(185, '删除配置', 'F', 180, NULL, NULL, 'system:config:delete', NULL, 5, 1, 1, 1, NOW());

-- ---------- 角色 ----------
INSERT INTO sys_role (id, name, code, data_scope, sort, status, create_by, create_time) VALUES
(1, '超级管理员', 'admin',  1, 1, 1, 1, NOW()),
(2, '普通用户',   'common', 4, 2, 1, 1, NOW());

-- admin 角色获得全部菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE del_flag = 0;

-- 普通用户角色仅授予日志查看
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 160), (2, 161), (2, 170), (2, 171);

-- ---------- 部门 / 岗位 ----------
INSERT INTO sys_dept (id, name, parent_id, sort, status, create_by, create_time) VALUES
(1, '总公司', 0, 1, 1, 1, NOW()),
(2, '研发部', 1, 1, 1, 1, NOW()),
(3, '产品部', 1, 2, 1, 1, NOW());

INSERT INTO sys_post (id, code, name, dept_id, sort, status, create_by, create_time) VALUES
(1, 'ceo',     '超级管理员', 1, 1, 1, 1, NOW()),
(2, 'rd',      '研发工程师', 2, 1, 1, 1, NOW()),
(3, 'product', '产品经理',   3, 1, 1, 1, NOW());

-- ---------- admin 用户 ----------
-- 默认口令 admin123（BCrypt cost=10），首次登录后请立即修改
INSERT INTO sys_user (id, username, password, nickname, real_name, status, dept_id, post_id, create_by, create_time) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超管', '超级管理员', 1, 1, 1, 1, NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_post (user_id, post_id) VALUES (1, 1);

-- ---------- 字典 ----------
INSERT INTO sys_dict (id, name, type, status, remark, create_by, create_time) VALUES
(1, '用户性别',   'sys_user_sex',      1, '用户性别列表', 1, NOW()),
(2, '系统状态',   'sys_common_status', 1, '正常/停用',   1, NOW()),
(3, '操作状态',   'sys_oper_status',   1, '成功/失败',   1, NOW()),
(4, '菜单类型',   'sys_menu_type',     1, '目录/菜单/按钮', 1, NOW()),
(5, '数据权限',   'sys_data_scope',    1, '5 级数据权限范围', 1, NOW()),
(6, '系统是否',   'sys_yes_no',        1, '是/否',       1, NOW());

INSERT INTO sys_dict_item (dict_type, label, value, sort, status, create_by, create_time) VALUES
('sys_user_sex',      '未知', '0', 1, 1, 1, NOW()),
('sys_user_sex',      '男',   '1', 2, 1, 1, NOW()),
('sys_user_sex',      '女',   '2', 3, 1, 1, NOW()),
('sys_common_status', '正常', '1', 1, 1, 1, NOW()),
('sys_common_status', '停用', '0', 2, 1, 1, NOW()),
('sys_oper_status',   '成功', '1', 1, 1, 1, NOW()),
('sys_oper_status',   '失败', '0', 2, 1, 1, NOW()),
('sys_menu_type',     '目录', 'M', 1, 1, 1, NOW()),
('sys_menu_type',     '菜单', 'C', 2, 1, 1, NOW()),
('sys_menu_type',     '按钮', 'F', 3, 1, 1, NOW()),
('sys_data_scope',    '全部',         '1', 1, 1, 1, NOW()),
('sys_data_scope',    '本部门及下级', '2', 2, 1, 1, NOW()),
('sys_data_scope',    '本部门',       '3', 3, 1, 1, NOW()),
('sys_data_scope',    '仅本人',       '4', 4, 1, 1, NOW()),
('sys_data_scope',    '自定义',       '5', 5, 1, 1, NOW()),
('sys_yes_no',        '是', 'Y', 1, 1, 1, NOW()),
('sys_yes_no',        '否', 'N', 2, 1, 1, NOW());

-- ---------- 系统参数配置 ----------
INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, remark, create_by, create_time) VALUES
(1, '用户初始密码',       'sys.user.initPassword',    'Qkit@123',   'Y', '新用户默认初始密码（需满足 8-32 位）', 1, NOW()),
(2, '登录验证码开关',     'sys.login.captchaEnabled', 'true',       'Y', '登录时是否显示图形验证码',          1, NOW()),
(3, '登录失败锁定次数',   'sys.login.retryLimit',     '5',          'N', '同一用户名密码连续输错锁定次数',    1, NOW()),
(4, '上传文件大小上限',   'sys.upload.maxSize',       '10',         'N', '上传文件大小上限(MB)',              1, NOW()),
(5, '系统首页皮肤',       'sys.index.skinName',       'skin-blue',  'N', '系统首页皮肤',                      1, NOW()),
(6, '同一IP失败锁定次数', 'sys.login.ipRetryLimit',   '20',         'N', '同一来源 IP 连续登录失败锁定次数',  1, NOW());
