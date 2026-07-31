package cn.cordys.crm.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作流连线DTO")
public class WorkflowEdgeDTO {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "源节点ID")
    private String sourceNodeId;

    @Schema(description = "目标节点ID")
    private String targetNodeId;

    @Schema(description = "连线条件(JSON字符串)")
    private String conditionExpr;

    @Schema(description = "连线类型")
    private String edgeType;
}
