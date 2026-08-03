package cn.cordys.crm.clue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配规则匹配条件
 * <p>
 * 用于 {@link cn.cordys.crm.clue.domain.CluePoolAssignRule#getConditions()} 的 JSON 序列化。
 * 一个规则可包含多个条件,条件之间为 AND 关系。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRuleConditionDTO {

    @Schema(description = "条件类型: FIELD(内容字段)/TIME(时间判断)")
    private String conditionType;

    @Schema(description = "自定义字段ID(对应 sys_module_field.id);时间条件时此字段为时间字段ID或 CLUE_CREATE_TIME(系统创建时间)")
    @NotBlank
    private String fieldId;

    @Schema(description = "操作符: 内容字段用 EQUALS/NOT_EQUALS/CONTAINS;时间条件用 BEFORE/AFTER/BETWEEN")
    @NotBlank
    private String operator;

    @Schema(description = "匹配值;时间条件时为时间戳(毫秒)")
    @NotBlank
    private String value;

    @Schema(description = "匹配值2;仅 BETWEEN(时间区间)时使用,表示结束时间戳(毫秒)")
    private String value2;
}
