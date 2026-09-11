-- ==========================================================
-- V1.0.2__monitor_menu.sql  服务监控菜单
-- 菜单 id 区间：200~299 系统监控
-- ==========================================================

-- ---------- 系统监控 → 服务监控 ----------
INSERT INTO sys_menu (id, name, type, parent_id, path, component, perm, icon, sort, visible, status, create_by, create_time) VALUES
(200, '服务监控', 'C', 2, '/monitor/server', 'monitor/server/index', 'monitor:server:page', 'Monitor', 1, 1, 1, 1, NOW());