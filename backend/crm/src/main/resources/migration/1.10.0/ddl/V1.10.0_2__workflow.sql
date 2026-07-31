-- 工作流定义: 通用业务流程定义(非审批流)
CREATE TABLE sys_workflow_definition
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'ID',
    `name`            VARCHAR(255) NOT NULL COMMENT '流程名称',
    `description`     VARCHAR(500)          COMMENT '流程描述',
    `workflow_type`   VARCHAR(50)  NOT NULL DEFAULT 'DATA_AUTOMATION' COMMENT '流程类型: DATA_AUTOMATION/NOTIFICATION/STATUS_FLOW',
    `form_key`        VARCHAR(64)           COMMENT '关联对象标识(FormKey)',
    `trigger_type`    VARCHAR(50)           COMMENT '触发类型: CREATE/UPDATE/DELETE/SCHEDULE/MANUAL',
    `trigger_config`  JSON                  COMMENT '触发条件配置(JSON)',
    `enable`          BIT(1)       NOT NULL DEFAULT 1 COMMENT '是否启用',
    `organization_id` VARCHAR(32)  NOT NULL COMMENT '组织ID',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_org_type` (`organization_id`, `workflow_type`),
    KEY `idx_form_key` (`form_key`)
) COMMENT = '工作流定义'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 工作流节点
CREATE TABLE sys_workflow_node
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'ID',
    `workflow_id`     VARCHAR(32)  NOT NULL COMMENT '工作流定义ID',
    `node_type`       VARCHAR(50)  NOT NULL COMMENT '节点类型: TRIGGER/ACTION/CONDITION/DELAY/END',
    `node_key`        VARCHAR(64)           COMMENT '节点唯一标识(流程内)',
    `name`            VARCHAR(255)          COMMENT '节点名称',
    `config`          JSON                  COMMENT '节点配置(JSON)',
    `pos_x`           INT          NOT NULL DEFAULT 0 COMMENT 'X坐标(可视化编辑器)',
    `pos_y`           INT          NOT NULL DEFAULT 0 COMMENT 'Y坐标(可视化编辑器)',
    `sort_order`      INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`)
) COMMENT = '工作流节点'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 工作流连线(节点之间的连接)
CREATE TABLE sys_workflow_edge
(
    `id`              VARCHAR(32)  NOT NULL COMMENT 'ID',
    `workflow_id`     VARCHAR(32)  NOT NULL COMMENT '工作流定义ID',
    `source_node_id`  VARCHAR(32)  NOT NULL COMMENT '源节点ID',
    `target_node_id`  VARCHAR(32)  NOT NULL COMMENT '目标节点ID',
    `condition_expr`  JSON                  COMMENT '连线条件(JSON, 用于分支)',
    `edge_type`       VARCHAR(20)  NOT NULL DEFAULT 'DEFAULT' COMMENT '连线类型: DEFAULT/CONDITIONAL',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间',
    `update_time`     BIGINT       NOT NULL COMMENT '更新时间',
    `create_user`     VARCHAR(32)  NOT NULL COMMENT '创建人',
    `update_user`     VARCHAR(32)  NOT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`)
) COMMENT = '工作流连线'
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
