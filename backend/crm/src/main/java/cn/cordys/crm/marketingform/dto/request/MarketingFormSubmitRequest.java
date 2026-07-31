package cn.cordys.crm.marketingform.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 意向客户通过二维码公开提交表单的请求体。
 * moduleFields 为表单字段值列表 (EAV 格式: {fieldId, fieldValue})。
 * 桥接服务按 marketing_form.field_mapping 把这些值映射成 clue 字段。
 */
@Data
public class MarketingFormSubmitRequest {

    @Schema(description = "表单字段值列表 (EAV)")
    private List<BaseModuleFieldValue> moduleFields;
}
