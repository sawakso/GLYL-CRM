-- 线索池自动分配策略增强:
-- 1) 分配规则目标支持「按部门/区域动态解析成员」(含子部门可配)
-- 2) 条件支持「时间判断」(早于/晚于/介于,可基于线索任意时间字段或系统创建时间)
-- 3) 线索池支持「每池独立 cron 定时自动分配」

-- 1. clue_pool_assign_rule 表新增目标类型与部门解析字段
ALTER TABLE clue_pool_assign_rule
    ADD assign_target_type VARCHAR(20) NOT NULL DEFAULT 'USER'
        COMMENT '目标类型: USER(指定人员)/DEPT(部门成员)' AFTER target_user_ids,
    ADD target_dept_ids TEXT COMMENT '目标部门ID JSON数组(DEPT 模式动态解析成员)' AFTER assign_target_type,
    ADD include_child_dept BIT(1) NOT NULL DEFAULT 0 COMMENT '目标部门是否包含子部门(DEPT 模式生效)' AFTER target_dept_ids;

-- 2. clue_pool 表新增定时自动分配配置
ALTER TABLE clue_pool
    ADD auto_assign_enabled BIT(1) NOT NULL DEFAULT 0 COMMENT '是否启用定时自动分配' AFTER allow_view_pool_log,
    ADD auto_assign_cron VARCHAR(100) COMMENT '定时自动分配cron表达式' AFTER auto_assign_enabled;
