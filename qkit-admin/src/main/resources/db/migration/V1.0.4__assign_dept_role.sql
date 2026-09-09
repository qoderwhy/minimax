-- ==========================================================
-- V1.0.4__assign_dept_role.sql  将「分配部门」权限授予超级管理员
-- ==========================================================

-- V1.03 已新增菜单 117（system:role:assign-dept），但未写入 sys_role_menu。
-- 此处将其授予超级管理员角色（role_id=1），避免权限按钮因无权限码而隐藏。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 117
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 117);