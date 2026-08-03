package cn.cordys.crm.clue.dto;

import cn.cordys.crm.clue.domain.CluePoolAssignRule;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 线索池分配规则回显 DTO
 * <p>
 * 继承 {@link CluePoolAssignRule} 全部字段,并增加目标人员名称用于前端回显。
 * <p>
 * 注意:仅保留 {@link NoArgsConstructor},避免 Lombok 的 {@code AllArgsConstructor} 被
 * Spring Boot 默认注册的 {@code ParameterNamesModule} 识别,导致 Jackson 优先使用全参构造器
 * 而忽略父类的 {@code assignType}/{@code assignTargetType} 等字段(反序列化为 null)。
 */
@Data
@NoArgsConstructor
public class CluePoolAssignRuleDTO extends CluePoolAssignRule {

    @Schema(description = "目标人员名称集合(回显用)")
    private List<ScopeNameDTO> targetUserNames;

    @Schema(description = "匹配条件集合(解析后的 conditions)")
    private List<AssignRuleConditionDTO> conditionList;
}
