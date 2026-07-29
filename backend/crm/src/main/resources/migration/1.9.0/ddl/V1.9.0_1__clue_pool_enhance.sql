-- 线索池管理功能增强: 新增协同管理员、领取模式、提醒及权限配置字段 + 分配规则表

-- 1. clue_pool 表新增配置字段
ALTER TABLE clue_pool
    ADD collaborator_id TEXT COMMENT '协同管理员ID JSON数组' AFTER owner_id,
    ADD description VARCHAR(1000) COMMENT '线索池描述' AFTER name,
    ADD pick_mode VARCHAR(20) NOT NULL DEFAULT 'VISIBLE_PICKABLE'
        COMMENT '领取模式: VISIBLE_PICKABLE(成员可见可领取)/ADMIN_ASSIGN_ONLY(成员不可见,管理员可分配)' AFTER auto,
    ADD new_lead_remind BIT(1) NOT NULL DEFAULT 0 COMMENT '新线索提醒:为管理员推送待办' AFTER pick_mode,
    ADD unassigned_reminder_minutes INT NOT NULL DEFAULT 1440 COMMENT '未分配超时提醒分钟数' AFTER new_lead_remind,
    ADD unfollowed_reminder_minutes INT NOT NULL DEFAULT 2880 COMMENT '未跟进超时提醒分钟数' AFTER unassigned_reminder_minutes,
    ADD notify_pool_admin_on_unfollowed_timeout BIT(1) NOT NULL DEFAULT 0 COMMENT '未跟进超时时通知线索池管理员' AFTER unfollowed_reminder_minutes,
    ADD allow_transfer_after_pick BIT(1) NOT NULL DEFAULT 0 COMMENT '领取后允许转移' AFTER notify_pool_admin_on_unfollowed_timeout,
    ADD restrict_transfer_in_to_members BIT(1) NOT NULL DEFAULT 0 COMMENT '仅允许转入线索池成员' AFTER allow_transfer_after_pick,
    ADD restrict_return_to_members BIT(1) NOT NULL DEFAULT 0 COMMENT '仅允许退回线索池成员' AFTER restrict_transfer_in_to_members,
    ADD clear_team_on_owner_change BIT(1) NOT NULL DEFAULT 0 COMMENT '负责人变化时清空团队' AFTER restrict_return_to_members,
    ADD clear_external_owner_on_owner_empty BIT(1) NOT NULL DEFAULT 0 COMMENT '负责人为空时清空外部负责人' AFTER clear_team_on_owner_change,
    ADD clear_external_team_on_external_owner_empty BIT(1) NOT NULL DEFAULT 0 COMMENT '外部负责人为空时清空外部团队' AFTER clear_external_owner_on_owner_empty,
    ADD clear_owner_on_pool_transfer BIT(1) NOT NULL DEFAULT 0 COMMENT '转移线索池时清空负责人' AFTER clear_external_team_on_external_owner_empty,
    ADD clear_external_owner_on_pool_transfer BIT(1) NOT NULL DEFAULT 0 COMMENT '转移线索池时清空外部负责人' AFTER clear_owner_on_pool_transfer,
    ADD allow_view_change_log_before_pick BIT(1) NOT NULL DEFAULT 0 COMMENT '领取前可查看变更记录' AFTER clear_external_owner_on_pool_transfer,
    ADD allow_edit_team_before_pick BIT(1) NOT NULL DEFAULT 0 COMMENT '领取前可编辑团队' AFTER allow_view_change_log_before_pick,
    ADD allow_send_sales_record_before_pick BIT(1) NOT NULL DEFAULT 0 COMMENT '领取前可发送销售记录' AFTER allow_edit_team_before_pick,
    ADD allow_view_sales_record_before_pick BIT(1) NOT NULL DEFAULT 0 COMMENT '领取前可查看销售记录' AFTER allow_send_sales_record_before_pick,
    ADD allow_view_pool_log BIT(1) NOT NULL DEFAULT 0 COMMENT '可查看线索池日志' AFTER allow_view_sales_record_before_pick;

-- 2. 线索表新增线索池来源和提醒状态
ALTER TABLE clue
    ADD source_pool_id VARCHAR(32) COMMENT '来源线索池ID',
    ADD pool_entry_time BIGINT COMMENT '进入线索池时间',
    ADD unassigned_reminder_sent_at BIGINT COMMENT '未分配提醒发送时间',
    ADD unfollowed_reminder_sent_at BIGINT COMMENT '未跟进提醒发送时间';

CREATE INDEX idx_clue_source_pool_id ON clue (source_pool_id);

-- 3. 线索池分配规则表
CREATE TABLE clue_pool_assign_rule
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `pool_id`         VARCHAR(32)  NOT NULL COMMENT '线索池ID',
    `rule_name`       VARCHAR(100) COMMENT '规则名称',
    `conditions`      TEXT         COMMENT '匹配条件JSON: [{fieldId, operator, value}]',
    `assign_type`     VARCHAR(20)  NOT NULL COMMENT '分配方式: SINGLE(仅分配给某人)/ROUND_ROBIN(循环分配)',
    `target_user_ids` TEXT         NOT NULL COMMENT '目标用户ID JSON数组',
    `current_index`   INT          DEFAULT 0 COMMENT '循环分配当前指针',
    `pos`             INT          DEFAULT 0 COMMENT '排序(优先级)',
    `enable`          BIT(1)       DEFAULT 1 COMMENT '启用/禁用',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_pool_id` (`pool_id`)
) COMMENT = '线索池分配规则'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
