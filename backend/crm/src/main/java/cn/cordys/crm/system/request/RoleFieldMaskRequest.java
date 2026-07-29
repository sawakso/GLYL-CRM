package cn.cordys.crm.system.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色字段脱敏配置保存请求
 */
@Data
public class RoleFieldMaskRequest {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{role_id_is_null}")
    private String roleId;

    @Schema(description = "脱敏配置列表")
    private List<MaskItem> masks;

    /**
     * 单条脱敏配置
     */
    @Data
    public static class MaskItem {
        @Schema(description = "模块key(customer/opportunity/order/contact)")
        @NotBlank
        private String moduleKey;

        @Schema(description = "自定义字段ID(对应sys_module_field.id)")
        private String fieldId;

        @Schema(description = "内置字段key(如phone,仅固定列用)")
        private String fieldKey;

        @Schema(description = "字段类型(PHONE/INPUT/DATA_SOURCE等)")
        @NotBlank
        private String fieldType;
    }
}
