package cn.cordys.crm.workflow.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "sys_workflow_definition")
public class WorkflowDefinition extends BaseModel {

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "流程类型: DATA_AUTOMATION/NOTIFICATION/STATUS_FLOW")
    private String workflowType;

    @Schema(description = "关联对象标识(FormKey)")
    private String formKey;

    @Schema(description = "触发类型: CREATE/UPDATE/DELETE/SCHEDULE/MANUAL")
    private String triggerType;

    @Schema(description = "触发条件配置(JSON)")
    private String triggerConfig;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "组织ID")
    private String organizationId;
}
