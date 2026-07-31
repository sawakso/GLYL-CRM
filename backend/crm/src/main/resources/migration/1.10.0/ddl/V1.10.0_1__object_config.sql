-- 对象管理: 存储预设对象和自定义对象的自定义名称及配置
CREATE TABLE sys_object_config
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `form_key`        VARCHAR(64)  NOT NULL COMMENT '对象标识(预设对象为FormKey枚举值, 自定义对象为customFormId)',
    `custom_name`     VARCHAR(255)          COMMENT '自定义名称(为空时使用默认名称)',
    `object_type`     VARCHAR(20)  NOT NULL DEFAULT 'PRESET' COMMENT '对象类型: PRESET(预设对象)/CUSTOM(自定义对象)',
    `enable`          BIT(1)       NOT NULL DEFAULT 1 COMMENT '是否启用',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_form_key_org` (`form_key`, `organization_id`)
) COMMENT = '对象配置'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
