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

    @Schema(description = "自定义字段ID(对应 sys_module_field.id)")
    @NotBlank
    private String fieldId;

    @Schema(description = "操作符: EQUALS(等于)/NOT_EQUALS(不等于)/CONTAINS(包含)")
    @NotBlank
    private String operator;

    @Schema(description = "匹配值")
    @NotBlank
    private String value;
}
