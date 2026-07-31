-- 线索池模块: 注册菜单模块开关 (与 marketingForm 平级)
-- 线索池本身是线索子模块, 此处仅注册 sys_module 使 /system/module 卡片的启用开关可用
-- 路由 CLUE_MANAGEMENT_POOL 仍为线索子路由, 不单独映射到 featureRouteMap
-- 使用 INSERT IGNORE 保证幂等 (避免重复执行报错)

-- 注册模块菜单 (organization_id=100001 为默认组织, pos=12 排在 marketingForm 之后)
INSERT IGNORE INTO sys_module (id, organization_id, module_key, enable, pos, create_user, create_time, update_user, update_time)
VALUES ('102011276770672910', '100001', 'cluePool', b'1', 12, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);
