package cn.cordys.crm.workflow.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "sys_workflow_node")
public class WorkflowNode extends BaseModel {

    @Schema(description = "工作流定义ID")
    private String workflowId;

    @Schema(description = "节点类型: TRIGGER/ACTION/CONDITION/DELAY/END")
    private String nodeType;

    @Schema(description = "节点唯一标识(流程内)")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点配置(JSON)")
    private String config;

    @Schema(description = "X坐标(可视化编辑器)")
    private Integer posX;

    @Schema(description = "Y坐标(可视化编辑器)")
    private Integer posY;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
