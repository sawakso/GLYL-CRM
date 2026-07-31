package cn.cordys.crm.marketingform.dto.response;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 公开表单配置响应 (意向客户扫码后 GET 获取, 用于渲染填写表单)。
 * 不暴露目标池、映射规则等内部信息。
 */
@Data
public class MarketingFormPublicResponse {

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "字段集合及其属性")
    private List<BaseField> fields;

    @Schema(description = "表单属性")
    private FormProp formProp;
}
