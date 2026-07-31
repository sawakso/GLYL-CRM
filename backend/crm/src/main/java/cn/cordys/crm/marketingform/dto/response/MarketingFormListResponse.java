package cn.cordys.crm.marketingform.dto.response;

import cn.cordys.crm.marketingform.domain.MarketingForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MarketingFormListResponse extends MarketingForm {

    @Schema(description = "目标线索池名称")
    private String targetPoolName;

    @Schema(description = "提交数")
    private Long submissionCount;
}
