package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "sys_object_config")
public class ObjectConfig extends BaseModel {

    @Schema(description = "对象标识")
    private String formKey;

    @Schema(description = "自定义名称")
    private String customName;

    @Schema(description = "对象类型: PRESET/CUSTOM")
    private String objectType;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "组织ID")
    private String organizationId;
}
