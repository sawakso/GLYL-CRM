package cn.cordys.crm.marketingform.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "marketing_form_submission")
public class MarketingFormSubmission extends BaseModel {

    @Schema(description = "市场表单ID")
    private String marketingFormId;

    @Schema(description = "回流生成的线索ID")
    private String clueId;

    @Schema(description = "提交时间")
    private Long submitTime;

    @Schema(description = "提交者IP")
    private String submitIp;

    @Schema(description = "组织ID")
    private String organizationId;
}
