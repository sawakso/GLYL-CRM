-- 线索池默认池标记：市场表单未配置目标池时，线索默认进组织默认池
SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE clue_pool
    ADD COLUMN is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认线索池(市场表单未配置目标池时进此池)';

-- 兼容历史数据：把「新进客户线索池」设为本组织默认池
UPDATE clue_pool SET is_default = 1 WHERE name LIKE '%新进%' AND enable = 1;
