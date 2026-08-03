package cn.cordys.crm.clue.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 线索池分配规则
 * <p>
 * 按条件匹配线索并自动分配给目标人员,支持仅分配给某人(SINGLE)或循环分配(ROUND_ROBIN)。
 *
 * @see cn.cordys.crm.clue.service.CluePoolAssignRuleService
 */
@Data
@NoArgsConstructor
@Table(name = "clue_pool_assign_rule")
public class CluePoolAssignRule extends BaseModel {

    @Schema(description = "线索池ID")
    private String poolId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "匹配条件JSON: [{fieldId, operator, value}]")
    private String conditions;

    @Schema(description = "分配方式: SINGLE(仅分配给某人)/ROUND_ROBIN(循环分配)")
    private String assignType;

    @Schema(description = "目标类型: USER(指定人员)/DEPT(部门成员)")
    private String assignTargetType;

    @Schema(description = "目标用户ID JSON数组(assignTargetType=USER 时使用)")
    private String targetUserIds;

    @Schema(description = "目标部门ID JSON数组(assignTargetType=DEPT 时按部门动态解析成员)")
    private String targetDeptIds;

    @Schema(description = "目标部门是否包含子部门(assignTargetType=DEPT 时生效)")
    private Boolean includeChildDept;

    @Schema(description = "循环分配当前指针")
    private Integer currentIndex;

    @Schema(description = "排序(优先级)")
    private Integer pos;

    @Schema(description = "启用/禁用")
    private Boolean enable;

    @Schema(description = "组织ID")
    private String organizationId;
}
