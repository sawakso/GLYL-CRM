package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ObjectConfigListResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "对象标识(formKey)")
    private String key;

    @Schema(description = "显示名称")
    private String name;

    @Schema(description = "默认名称(不可修改)")
    private String defaultName;

    @Schema(description = "对象类型: PRESET/CUSTOM")
    private String type;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "是否可删除(预设对象不可删除)")
    private Boolean deletable;

    @Schema(description = "关联的表单ID(自定义对象)")
    private String formId;
}
