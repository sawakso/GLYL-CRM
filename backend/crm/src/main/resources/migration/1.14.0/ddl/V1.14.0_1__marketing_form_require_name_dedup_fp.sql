-- 市场表单填报防呆增强 (1.14.0):
-- 1) marketing_form 增加 require_name: 开关"姓名必填才能提交" (前端设置tab可配置)
-- 2) clue 增加 dedup_fingerprint + 唯一索引: 并发安全去重兜底
--    (免登录场景同身份并发提交时, 数据库唯一约束保证同表单同身份只建一条线索)

-- 1. 市场表单: 是否强制姓名必填 (仅当映射到线索 name 的字段存在时才生效)
ALTER TABLE marketing_form
    ADD require_name TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '是否强制姓名必填才能提交: 0/1' AFTER dedup_key;

-- 2. 线索: 去重指纹(并发安全兜底)
--    指纹 = formId:identityKey:identityValue, 仅当身份可识别(手机号/设备/IP非空)时写入;
--    唯一索引保证同表单同身份并发下只建一条线索
ALTER TABLE clue
    ADD dedup_fingerprint VARCHAR(300) NULL
        COMMENT '表单回流去重指纹 formId:key:value(并发安全去重兜底)' AFTER duplicate_clue_id;

CREATE UNIQUE INDEX uk_dedup_fingerprint
    ON clue (dedup_fingerprint ASC);
