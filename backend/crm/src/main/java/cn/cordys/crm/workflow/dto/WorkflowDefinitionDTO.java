package cn.cordys.crm.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工作流定义详情(含节点和连线)")
public class WorkflowDefinitionDTO {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "流程类型")
    private String workflowType;

    @Schema(description = "关联对象标识")
    private String formKey;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发条件配置(JSON字符串)")
    private String triggerConfig;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "节点列表")
    private List<WorkflowNodeDTO> nodes;

    @Schema(description = "连线列表")
    private List<WorkflowEdgeDTO> edges;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;

    @Schema(description = "创建人")
    private String createUser;

    @Schema(description = "更新人")
    private String updateUser;
}
