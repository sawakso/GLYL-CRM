package cn.cordys.crm.marketingform.dto.response;

import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MarketingFormGetResponse extends MarketingForm {

    @Schema(description = "字段集合及其属性")
    private List<BaseField> fields;

    @Schema(description = "表单属性")
    private FormProp formProp;

    @Schema(description = "目标线索池名称")
    private String targetPoolName;
}
