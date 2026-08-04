-- 给线索「来源」单选字段补充「市场表单」选项:
-- 1.11.0 扩展字段建库时 options 为空, 导致表单回流线索的 source 值显示"选项不存在"。
-- 仅当 options 为空时初始化, 避免覆盖用户在后台已配置的选项。
UPDATE sys_module_field_blob b
JOIN sys_module_field f ON b.id = f.id
JOIN sys_module_form m ON m.id = f.form_id
SET b.prop = JSON_SET(
    b.prop,
    '$.options',
    JSON_ARRAY(JSON_OBJECT('label', '市场表单', 'value', 'MARKETING_FORM'))
)
WHERE m.form_key = 'CLUE'
  AND f.internal_key = 'source'
  AND (JSON_EXTRACT(b.prop, '$.options') IS NULL
       OR JSON_LENGTH(JSON_EXTRACT(b.prop, '$.options')) = 0);
