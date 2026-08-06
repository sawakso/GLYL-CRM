-- 线索审批闭环：审批状态列 + 审批资源快照表
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE clue
    ADD COLUMN approval_status VARCHAR(50) DEFAULT NULL COMMENT '审批状态(NONE/APPROVING/APPROVED/UNAPPROVED/AUTO_APPROVED/AUTO_UNAPPROVED)',
    ADD COLUMN approved TINYINT(1) DEFAULT 0 COMMENT '是否已审批通过';

-- 审批资源快照表（审批驳回/撤回时回退用）。原 @Table 已声明但运行库缺失，此处补齐。
CREATE TABLE IF NOT EXISTS approval_resource_snapshot
(
    `id`            VARCHAR(32) NOT NULL COMMENT 'id',
    `form_key`      VARCHAR(32) COMMENT '表单类型',
    `resource_id`   VARCHAR(32) COMMENT '资源ID',
    `snapshot_data` LONGTEXT    COMMENT '审批前资源快照(JSON)',
    `create_user`   VARCHAR(32) COMMENT '创建人',
    `update_user`   VARCHAR(32) COMMENT '更新人',
    `create_time`   BIGINT      COMMENT '创建时间',
    `update_time`   BIGINT      COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT = '审批资源快照'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_ars_resource_id ON approval_resource_snapshot (resource_id ASC);
