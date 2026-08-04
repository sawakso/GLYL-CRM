package cn.cordys.crm.marketingform.dto.request;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MarketingFormAddRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "活动名称")
    private String name;

    @Size(max = 1000)
    @Schema(description = "说明")
    private String description;

    @Schema(description = "目标线索池ID")
    private String targetPoolId;

    @Schema(description = "字段映射规则 JSON: {表单字段internalKey: clue字段名}")
    private String fieldMapping;

    @Schema(description = "去重策略: INHERIT(跟随线索池)/NONE/UPDATE/SKIP/MARK (默认 INHERIT)")
    private String dedupStrategy;

    @Schema(description = "去重时间窗(分钟), null 表示跟随线索池")
    private Integer dedupWindow;

    @Schema(description = "身份判定键: AUTO/PHONE/DEVICE/IP, null 表示跟随线索池")
    private String dedupKey;

    @Schema(description = "是否强制姓名必填才能提交")
    private Boolean requireName;

    @Schema(description = "保存字段集合")
    private List<BaseField> fields;

    @Schema(description = "表单属性")
    private FormProp formProp;
}
