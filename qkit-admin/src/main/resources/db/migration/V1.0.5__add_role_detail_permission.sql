-- ==========================================================
-- V1.0.5__add_role_detail_permission.sql  角色详情权限点
-- ==========================================================

-- 角色管理 → 角色详情（F 按钮，父级=角色管理 110，sort=8）
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time)
VALUES (118, '角色详情', 'F', 110, NULL, NULL, 'system:role:detail', NULL, 8, 1, 1, 1, NOW());

-- 授予超级管理员角色（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 118
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 118);