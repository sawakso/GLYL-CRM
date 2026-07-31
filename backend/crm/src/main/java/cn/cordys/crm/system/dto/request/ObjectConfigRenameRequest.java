package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ObjectConfigRenameRequest {

    @NotBlank(message = "对象标识不能为空")
    @Schema(description = "对象标识(formKey)")
    private String key;

    @Schema(description = "自定义名称")
    private String name;
}
