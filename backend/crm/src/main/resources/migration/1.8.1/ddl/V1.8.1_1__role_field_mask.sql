-- 角色字段脱敏配置表
-- 按 角色×模块×字段 维度配置需要脱敏显示的字段
CREATE TABLE IF NOT EXISTS `sys_role_field_mask` (
    `id`              VARCHAR(32)  NOT NULL,
    `role_id`         VARCHAR(32)  NOT NULL COMMENT '角色ID',
    `module_key`      VARCHAR(50)  NOT NULL COMMENT '模块key(customer/opportunity/order/contact)',
    `field_id`        VARCHAR(32)  DEFAULT NULL COMMENT '自定义字段ID(对应sys_module_field.id)',
    `field_key`       VARCHAR(100) DEFAULT NULL COMMENT '内置字段key(如phone,仅contact固定列用)',
    `field_type`      VARCHAR(20)  NOT NULL COMMENT '字段类型(PHONE/INPUT/DATA_SOURCE等,决定打码算法)',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `pos`             BIGINT       NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time`     BIGINT       NOT NULL,
    `update_time`     BIGINT       NOT NULL,
    `create_user`     VARCHAR(32)  NOT NULL,
    `update_user`     VARCHAR(32)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_role_module` (`role_id`, `module_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色字段脱敏配置';
