package cn.cordys.crm.workflow.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "sys_workflow_edge")
public class WorkflowEdge extends BaseModel {

    @Schema(description = "工作流定义ID")
    private String workflowId;

    @Schema(description = "源节点ID")
    private String sourceNodeId;

    @Schema(description = "目标节点ID")
    private String targetNodeId;

    @Schema(description = "连线条件(JSON, 用于分支)")
    private String conditionExpr;

    @Schema(description = "连线类型: DEFAULT/CONDITIONAL")
    private String edgeType;
}
