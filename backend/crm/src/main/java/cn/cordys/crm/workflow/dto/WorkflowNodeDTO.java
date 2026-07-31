package cn.cordys.crm.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工作流节点DTO")
public class WorkflowNodeDTO {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点唯一标识")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点配置(JSON字符串)")
    private String config;

    @Schema(description = "X坐标")
    private Integer posX;

    @Schema(description = "Y坐标")
    private Integer posY;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
