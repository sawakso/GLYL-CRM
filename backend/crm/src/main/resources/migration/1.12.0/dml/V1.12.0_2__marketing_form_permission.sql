-- 市场活动表单模块: 注册菜单模块 + 角色权限
-- marketingForm 模块注册到 sys_module (与 clue/customForm 平级顶级菜单)
-- 权限: org_admin 全部权限, sales_manager/sales_staff 只读
-- 公开提交端点 /pub/** 已在 ShiroFilter 放行为 anon, 不需要权限
-- 使用 INSERT IGNORE 保证幂等 (避免重复执行报错)

-- 注册模块菜单 (organization_id=100001 为默认组织, pos=11 排在最后)
INSERT IGNORE INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
VALUES ('102011276770672901', '100001', 'marketingForm', b'1', 11, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- org_admin: 全部权限 (市场部可由管理员分配)
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES ('102011276770672902', 'org_admin', 'MARKETING_FORM:READ');
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES ('102011276770672903', 'org_admin', 'MARKETING_FORM:ADD');
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES ('102011276770672904', 'org_admin', 'MARKETING_FORM:UPDATE');
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES ('102011276770672905', 'org_admin', 'MARKETING_FORM:DELETE');

-- sales_manager: 只读
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES ('102011276770672906', 'sales_manager', 'MARKETING_FORM:READ');

-- sales_staff: 只读
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id) VALUES ('102011276770672907', 'sales_staff', 'MARKETING_FORM:READ');
