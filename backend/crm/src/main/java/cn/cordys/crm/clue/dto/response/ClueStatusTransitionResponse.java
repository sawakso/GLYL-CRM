package cn.cordys.crm.clue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 线索状态流转响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClueStatusTransitionResponse {

    @Schema(description = "当前阶段")
    private String currentStage;

    @Schema(description = "当前阶段名称")
    private String currentStageName;

    @Schema(description = "当前线索阶段")
    private String currentLeadsStage;

    @Schema(description = "当前业务状态")
    private String currentBizStatus;

    @Schema(description = "当前生命状态")
    private String currentLifeStatus;

    @Schema(description = "可流转的下一个阶段列表")
    private List<TransitionOption> availableTransitions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionOption {
        @Schema(description = "目标阶段 key")
        private String stage;
        @Schema(description = "目标阶段名称")
        private String stageName;
        @Schema(description = "建议的线索阶段")
        private String suggestedLeadsStage;
        @Schema(description = "建议的业务状态")
        private String suggestedBizStatus;
    }
}
