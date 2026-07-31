-- 市场活动表单模块 (ext_ver 1.12.0)
-- 与「线索」平级的顶级模块。市场部新建表单 → 生成二维码 → 客户免登录填写 → 自动回流成线索进池。
-- 字段定义复用共享 sys_module_form / sys_module_field (ModuleFormService 管理),
-- 字段值复用 BaseResourceFieldService EAV 引擎, 本表只存市场层元数据 + 提交留痕 + EAV 值表。

CREATE TABLE marketing_form(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `name` VARCHAR(255) NOT NULL   COMMENT '活动名称' ,
    `description` VARCHAR(1000)    COMMENT '说明' ,
    `target_pool_id` VARCHAR(32)    COMMENT '目标线索池ID' ,
    `field_mapping` TEXT    COMMENT 'JSON: {表单字段internalKey: clue字段名} 映射规则' ,
    `dedup_strategy` VARCHAR(30) NULL DEFAULT 'NONE'   COMMENT '去重策略(预留: NONE/UPDATE/SKIP/MARK)' ,
    `qr_token` VARCHAR(64) NOT NULL   COMMENT '公开二维码令牌' ,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT'   COMMENT '状态: DRAFT/ACTIVE/CLOSED' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
    `create_time` BIGINT NOT NULL   COMMENT '创建时间' ,
    `update_time` BIGINT NOT NULL   COMMENT '更新时间' ,
    `create_user` VARCHAR(32) NOT NULL   COMMENT '创建人' ,
    `update_user` VARCHAR(32) NOT NULL   COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '市场活动表单'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE UNIQUE INDEX uk_qr_token ON marketing_form(qr_token ASC);
CREATE INDEX idx_org ON marketing_form(organization_id ASC);

-- 提交记录留痕 (每次客户提交一条, 关联回流生成的线索)
-- 注意: 实体 MarketingFormSubmission 继承 BaseModel, 必须包含审计字段列
CREATE TABLE marketing_form_submission(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `marketing_form_id` VARCHAR(32) NOT NULL   COMMENT '市场表单ID' ,
    `clue_id` VARCHAR(32)    COMMENT '回流生成的线索ID' ,
    `submit_time` BIGINT NOT NULL   COMMENT '提交时间' ,
    `submit_ip` VARCHAR(64)    COMMENT '提交者IP' ,
    `organization_id` VARCHAR(32) NOT NULL   COMMENT '组织id' ,
    `create_time` BIGINT    COMMENT '创建时间' ,
    `update_time` BIGINT    COMMENT '更新时间' ,
    `create_user` VARCHAR(32)    COMMENT '创建人' ,
    `update_user` VARCHAR(32)    COMMENT '更新人' ,
    PRIMARY KEY (id)
)  COMMENT = '市场表单提交记录'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_form ON marketing_form_submission(marketing_form_id ASC);

-- EAV 字段值表 (复用 BaseResourceFieldService, 结构镜像 custom_form_data_field)
CREATE TABLE marketing_form_field(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '提交记录id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` VARCHAR(255) NOT NULL   COMMENT '自定义属性值' ,
    PRIMARY KEY (id)
)  COMMENT = '市场表单字段值'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON marketing_form_field(resource_id ASC);

CREATE TABLE marketing_form_field_blob(
    `id` VARCHAR(32) NOT NULL   COMMENT 'id' ,
    `resource_id` VARCHAR(32) NOT NULL   COMMENT '提交记录id' ,
    `field_id` VARCHAR(32) NOT NULL   COMMENT '自定义属性id' ,
    `field_value` TEXT NOT NULL   COMMENT '自定义属性值(大文本)' ,
    PRIMARY KEY (id)
)  COMMENT = '市场表单字段值大文本'
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_resource_id ON marketing_form_field_blob(resource_id ASC);
