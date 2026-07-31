package cn.cordys.crm.marketingform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "marketing_form")
public class MarketingForm extends BaseModel {

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "目标线索池ID")
    private String targetPoolId;

    @Schema(description = "字段映射规则 JSON: {表单字段internalKey: clue字段名}")
    private String fieldMapping;

    @Schema(description = "去重策略: NONE/UPDATE/SKIP/MARK")
    private String dedupStrategy;

    @Schema(description = "公开二维码令牌")
    private String qrToken;

    @Schema(description = "状态: DRAFT/ACTIVE/CLOSED")
    private String status;

    @Schema(description = "组织ID")
    private String organizationId;
}
