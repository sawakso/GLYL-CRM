-- 线索表补齐纷享销客核心字段 (ext_ver 1.11.0)
-- 这些字段已在 form/field.json 中配置(ext_ver=1.11.0), 由 DataInitService 加载进 module_field 注册表,
-- 但 clue 表缺少对应列, 导致前端显示但读不到值。本脚本给 clue 主表补齐 DDL 列。
-- 说明: 12 个 __c 自定义字段走 clue_field EAV 机制, 不在此加列。
--       审计字段(created_by/last_modified_by/last_modified_time)复用已有 create_user/update_user/update_time, 不新增列。
--       leads_pool_id 复用已有 pool_id, assigned_time 复用已有 collection_time, 不新增列。
-- 类型约定: 时间 BIGINT(毫秒戳), 布尔 BIT(1) DEFAULT 0, ID类 VARCHAR(32), 短文本 VARCHAR(255), 枚举 VARCHAR(30)。

ALTER TABLE clue
    -- 联系/企业信息
    ADD tel VARCHAR(30) COMMENT '电话' AFTER phone,
    ADD mobile VARCHAR(30) COMMENT '手机' AFTER tel,
    ADD email VARCHAR(100) COMMENT '邮件' AFTER mobile,
    ADD address VARCHAR(255) COMMENT '地址' AFTER email,
    ADD url VARCHAR(255) COMMENT '网址' AFTER address,
    ADD company VARCHAR(255) COMMENT '企业名称' AFTER name,
    ADD department VARCHAR(255) COMMENT '部门' AFTER company,
    ADD job_title VARCHAR(255) COMMENT '职务' AFTER department,
    ADD picture_path VARCHAR(1000) COMMENT '名片' AFTER job_title,

    -- 状态/管理 (leads_stage 与已有 stage 区分: 纷享多维度状态, stage 保留为飞致云原有阶段)
    ADD source VARCHAR(30) COMMENT '来源' AFTER stage,
    ADD leads_stage VARCHAR(30) COMMENT '线索阶段' AFTER source,
    ADD biz_status VARCHAR(30) COMMENT '业务状态' AFTER leads_stage,
    ADD life_status VARCHAR(30) COMMENT '生命状态' AFTER biz_status,
    ADD lock_status VARCHAR(30) NOT NULL DEFAULT 'UNLOCKED' COMMENT '锁定状态' AFTER life_status,
    ADD record_type VARCHAR(30) COMMENT '业务类型' AFTER lock_status,

    -- 跟进/负责人
    ADD next_followed_time BIGINT COMMENT '下次跟进时间' AFTER record_type,
    ADD next_followed_remark VARCHAR(1000) COMMENT '下次跟进要点' AFTER next_followed_time,
    ADD last_follow_time BIGINT COMMENT '最后跟进时间' AFTER next_followed_remark,
    ADD last_follower VARCHAR(32) COMMENT '最后跟进人' AFTER last_follow_time,
    ADD remark VARCHAR(2000) COMMENT '客户咨询详情' AFTER last_follower,
    ADD out_owner VARCHAR(32) COMMENT '外部负责人' AFTER remark,
    ADD assigner_id VARCHAR(32) COMMENT '分配管理员' AFTER out_owner,
    ADD data_own_department VARCHAR(32) COMMENT '归属部门' AFTER assigner_id,
    ADD owner_department VARCHAR(255) COMMENT '负责人主属部门' AFTER data_own_department,

    -- 时间/转换 (assigned_time 复用 collection_time, last_modified_time 复用 update_time, 不新增)
    ADD transform_time BIGINT COMMENT '转换时间' AFTER owner_department,
    ADD owner_change_time BIGINT COMMENT '负责人变更时间' AFTER transform_time,
    ADD returned_time BIGINT COMMENT '退回/收回时间' AFTER owner_change_time,
    ADD changed_to_mql_time BIGINT COMMENT '转MQL时间' AFTER returned_time,
    ADD expire_time BIGINT COMMENT '预计收回时间' AFTER changed_to_mql_time,

    -- 转换/布尔
    ADD conversion_probability DECIMAL(5,2) COMMENT '转换概率(百分数)' AFTER expire_time,
    ADD remaining_time BIGINT COMMENT '剩余保有时间(毫秒)' AFTER conversion_probability,
    ADD is_overtime BIT(1) NOT NULL DEFAULT 0 COMMENT '是否超时' AFTER remaining_time,
    ADD is_duplicated BIT(1) NOT NULL DEFAULT 0 COMMENT '是否存在重复数据' AFTER is_overtime,
    ADD biz_reg_name BIT(1) NOT NULL DEFAULT 0 COMMENT '工商注册' AFTER is_duplicated,

    -- 原因/结果
    ADD back_reason VARCHAR(32) COMMENT '销售人员退回原因' AFTER biz_reg_name,
    ADD close_reason VARCHAR(32) COMMENT '线索无效原因' AFTER back_reason,
    ADD completed_result VARCHAR(2000) COMMENT '处理结果' AFTER close_reason,

    -- 延期
    ADD extend_days INT COMMENT '申请延期天数' AFTER completed_result,
    ADD extend_reason VARCHAR(1000) COMMENT '延期原因' AFTER extend_days,

    -- 关联
    ADD marketing_event_id VARCHAR(32) COMMENT '市场活动名称' AFTER extend_reason,
    ADD collected_to VARCHAR(32) COMMENT '归集到的线索' AFTER marketing_event_id,

    -- 其他
    ADD phone_number_attribution VARCHAR(255) COMMENT '手机归属地' AFTER collected_to,
    ADD enterprise_wechat_user_id VARCHAR(100) COMMENT '企业微信UserId' AFTER phone_number_attribution;

-- 索引: 常用于筛选/分配规则匹配的字段
CREATE INDEX idx_clue_mobile ON clue (mobile ASC);
CREATE INDEX idx_clue_company ON clue (company ASC);
CREATE INDEX idx_clue_leads_stage ON clue (leads_stage ASC);
CREATE INDEX idx_clue_biz_status ON clue (biz_status ASC);
CREATE INDEX idx_clue_life_status ON clue (life_status ASC);
CREATE INDEX idx_clue_source ON clue (source ASC);
CREATE INDEX idx_clue_marketing_event_id ON clue (marketing_event_id ASC);
