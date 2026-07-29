package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色字段脱敏配置
 * <p>
 * 按 角色×模块×字段 维度记录需要脱敏显示的字段。
 * field_id 用于自定义字段(对应 sys_module_field.id)；
 * field_key 用于实体固定列(如联系人 phone)，两者互斥。
 * field_type 决定打码算法(PHONE→后6位打*，INPUT→保留首字符)。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_role_field_mask")
public class RoleFieldMask extends BaseModel {

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "模块key(customer/opportunity/order/contact)")
    private String moduleKey;

    @Schema(description = "自定义字段ID(对应sys_module_field.id)")
    private String fieldId;

    @Schema(description = "内置字段key(如phone,仅固定列用)")
    private String fieldKey;

    @Schema(description = "字段类型(PHONE/INPUT/DATA_SOURCE等)")
    private String fieldType;

    @Schema(description = "组织ID")
    private String organizationId;

    @Schema(description = "排序")
    private Long pos;
}
