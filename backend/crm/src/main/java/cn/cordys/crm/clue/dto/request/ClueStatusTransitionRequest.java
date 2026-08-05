package cn.cordys.crm.clue.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.clue.constants.BizStatusEnum;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.constants.LeadsStageEnum;
import cn.cordys.crm.clue.constants.LifeStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线索状态流转请求
 * 支持多维度状态同步更新
 */
@Data
public class ClueStatusTransitionRequest {

    @NotBlank
    @Size(max = 32)
    @Schema(description = "线索ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @EnumValue(enumClass = ClueStatus.class)
    @Schema(description = "线索阶段 (stage)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stage;

    @EnumValue(enumClass = LeadsStageEnum.class)
    @Schema(description = "线索阶段 (leadsStage)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String leadsStage;

    @EnumValue(enumClass = BizStatusEnum.class)
    @Schema(description = "业务状态 (bizStatus)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String bizStatus;

    @EnumValue(enumClass = LifeStatusEnum.class)
    @Schema(description = "生命状态 (lifeStatus)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String lifeStatus;

    @Size(max = 500)
    @Schema(description = "流转备注")
    private String remark;
}
