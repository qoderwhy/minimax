-- ==========================================================
-- V1.0.3__data_scope.sql  数据权限扩展
-- ==========================================================

-- 角色→部门关系表已在 V1.0.0 创建（sys_role_dept），本迁移仅补充权限点。

-- 角色管理 → 分配部门（F 按钮，父级=角色管理 110，sort=7）
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time)
VALUES (117, '分配部门', 'F', 110, NULL, NULL, 'system:role:assign-dept', NULL, 7, 1, 1, 1, NOW());

-- 注意：sys_role_menu 的授权见 V1.0.4（本迁移已执行过，sys_role_menu 在此版本未写入）。