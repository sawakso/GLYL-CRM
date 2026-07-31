package cn.cordys.crm.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工作流保存请求")
public class WorkflowSaveRequest {

    @Schema(description = "ID(更新时传)")
    private String id;

    @NotBlank(message = "流程名称不能为空")
    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程描述")
    private String description;

    @NotBlank(message = "流程类型不能为空")
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
}
