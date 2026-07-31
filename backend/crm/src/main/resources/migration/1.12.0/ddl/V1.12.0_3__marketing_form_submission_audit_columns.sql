-- 补齐 marketing_form_submission 审计字段
-- 实体 MarketingFormSubmission 继承 BaseModel (含 createUser/updateUser/createTime/updateTime),
-- 初版建表遗漏了这四列, 导致列表查询 (按 formId 统计提交数) 报 Unknown column 'create_user'。
ALTER TABLE marketing_form_submission
    ADD COLUMN `create_time` BIGINT NULL COMMENT '创建时间' AFTER `organization_id`,
    ADD COLUMN `update_time` BIGINT NULL COMMENT '更新时间' AFTER `create_time`,
    ADD COLUMN `create_user` VARCHAR(32) NULL COMMENT '创建人' AFTER `update_time`,
    ADD COLUMN `update_user` VARCHAR(32) NULL COMMENT '更新人' AFTER `create_user`;
