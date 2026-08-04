-- 线索填报防呆 / 去重能力:
-- 1) 线索池层配置去重「默认方案」(dedup_strategy / dedup_window / dedup_key)
-- 2) 市场表单层可继承(INHERIT)或覆盖池默认
-- 3) clue 增加 duplicate_clue_id 关联被判定为重复的原线索(配合已有 is_duplicated)
-- 4) marketing_form_submission 记录设备指纹与身份键留痕, 供窗口内去重检索

-- 1. 线索池: 去重默认方案
ALTER TABLE clue_pool
    ADD dedup_strategy VARCHAR(30) NOT NULL DEFAULT 'NONE'
        COMMENT '去重策略: NONE(不去重)/UPDATE(覆盖更新原线索)/SKIP(丢弃重复提交)/MARK(新建并标记疑似重复)' AFTER auto_assign_cron,
    ADD dedup_window INT NOT NULL DEFAULT 5
        COMMENT '去重时间窗(分钟), 0 表示不限时间窗(全量去重)' AFTER dedup_strategy,
    ADD dedup_key VARCHAR(20) NOT NULL DEFAULT 'AUTO'
        COMMENT '身份判定键: AUTO(手机>设备>IP 逐级降级)/PHONE(仅手机号)/DEVICE(仅设备指纹)/IP(仅IP)' AFTER dedup_window;

-- 2. 市场表单: 可覆盖池默认(INHERIT 表示跟随线索池)
ALTER TABLE marketing_form
    ADD dedup_window INT NULL
        COMMENT '去重时间窗(分钟), NULL 表示跟随线索池' AFTER dedup_strategy,
    ADD dedup_key VARCHAR(20) NULL
        COMMENT '身份判定键: AUTO/PHONE/DEVICE/IP, NULL 表示跟随线索池' AFTER dedup_window;

ALTER TABLE marketing_form
    MODIFY COLUMN dedup_strategy VARCHAR(30) NULL DEFAULT 'INHERIT'
        COMMENT '去重策略: INHERIT(跟随线索池)/NONE/UPDATE/SKIP/MARK';

-- 历史数据: 去重能力此前从未生效, 统一改为跟随线索池默认(池默认 NONE, 行为不变)
UPDATE marketing_form
SET dedup_strategy = 'INHERIT'
WHERE dedup_strategy IS NULL
   OR dedup_strategy = ''
   OR dedup_strategy = 'NONE';

-- 3. 线索: 关联重复来源线索
ALTER TABLE clue
    ADD duplicate_clue_id VARCHAR(32) NULL
        COMMENT '疑似重复时关联的原线索ID(MARK 策略写入)' AFTER is_duplicated;

-- 4. 提交留痕: 设备指纹 + 身份键 + 处理结果
ALTER TABLE marketing_form_submission
    ADD submit_device VARCHAR(64) NULL COMMENT '提交者设备指纹(前端 localStorage 生成)' AFTER submit_ip,
    ADD identity_key VARCHAR(20) NULL COMMENT '本次去重实际使用的身份键: PHONE/DEVICE/IP' AFTER submit_device,
    ADD identity_value VARCHAR(191) NULL COMMENT '身份键取值(手机号/设备指纹/IP)' AFTER identity_key,
    ADD dedup_action VARCHAR(20) NULL COMMENT '去重处理结果: CREATE/UPDATE/SKIP/MARK' AFTER identity_value;

CREATE INDEX idx_form_identity_time
    ON marketing_form_submission (marketing_form_id ASC, identity_value ASC, submit_time ASC);
