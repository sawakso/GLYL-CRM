-- 修正 1.11.0.1 中 4 个 NOT NULL 列导致的保存失败 (ext_ver 1.11.0)
-- 背景: BaseMapper 反射式 INSERT 会把实体所有字段显式写入 SQL (含未填的 null),
-- MySQL STRICT_TRANS_TABLES 模式下显式插 NULL 进 NOT NULL 列直接报错, DEFAULT 子句
-- 在列被显式 INSERT 时不生效。这 4 个都是表单可选字段 (未填即 null), 改为允许 NULL
-- 最稳健: 既不依赖 BeanUtils 的 null 跳过行为, 语义上"未设置"=NULL 也正确。
-- lock_status 保留 DEFAULT 'UNLOCKED' 供未来 OMIT 式插入或 app 显式设值使用。

ALTER TABLE clue
    MODIFY COLUMN lock_status VARCHAR(30) NULL DEFAULT 'UNLOCKED' COMMENT '锁定状态',
    MODIFY COLUMN is_overtime BIT(1) NULL DEFAULT 0 COMMENT '是否超时',
    MODIFY COLUMN is_duplicated BIT(1) NULL DEFAULT 0 COMMENT '是否存在重复数据',
    MODIFY COLUMN biz_reg_name BIT(1) NULL DEFAULT 0 COMMENT '工商注册';
