package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.constants.BizStatusEnum;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.constants.LeadsStageEnum;
import cn.cordys.crm.clue.constants.LifeStatusEnum;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.ClueStatusTransitionRequest;
import cn.cordys.crm.clue.dto.response.ClueStatusTransitionResponse;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 线索状态流转服务
 * 实现多维度状态机的核心流转逻辑：
 * - stage (飞致云原有阶段)：NEW → FOLLOWING → INTERESTED/MQL → CONVERTED/CLOSED
 * - leadsStage (线索阶段)：NEW → CONTACTED → CONVERTED/INVALID
 * - bizStatus (业务状态)：与 stage 联动
 * - lifeStatus (生命状态)：ACTIVE/DORMANT/INVALID
 */
@Service
public class ClueStatusTransitionService {

    @Resource
    private BaseMapper<Clue> clueMapper;

    /**
     * 阶段 → 建议的线索阶段 映射
     */
    private static final Map<ClueStatus, LeadsStageEnum> STAGE_TO_LEADS_STAGE = Map.of(
            ClueStatus.NEW, LeadsStageEnum.NEW,
            ClueStatus.FOLLOWING, LeadsStageEnum.CONTACTED,
            ClueStatus.INTERESTED, LeadsStageEnum.CONTACTED,
            ClueStatus.MQL, LeadsStageEnum.CONTACTED,
            ClueStatus.CONVERTED, LeadsStageEnum.CONVERTED,
            ClueStatus.CLOSED, LeadsStageEnum.INVALID
    );

    /**
     * 阶段 → 建议的业务状态 映射
     */
    private static final Map<ClueStatus, BizStatusEnum> STAGE_TO_BIZ_STATUS = Map.of(
            ClueStatus.NEW, BizStatusEnum.NEW,
            ClueStatus.FOLLOWING, BizStatusEnum.TRYING,
            ClueStatus.INTERESTED, BizStatusEnum.INTERESTED,
            ClueStatus.MQL, BizStatusEnum.INTERESTED,
            ClueStatus.CONVERTED, BizStatusEnum.CONVERTED,
            ClueStatus.CLOSED, BizStatusEnum.NOT_INTERESTED
    );

    /**
     * 获取当前线索可流转的下一个阶段列表
     */
    public ClueStatusTransitionResponse getAvailableTransitions(String clueId) {
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null) {
            throw new GenericException(Translator.get("clue_not_found"));
        }

        ClueStatus currentStage = ClueStatus.ofKey(clue.getStage());
        if (currentStage == null) {
            currentStage = ClueStatus.NEW;
        }

        Set<ClueStatus> allowedStages = currentStage.allowedTransitions();
        List<ClueStatusTransitionResponse.TransitionOption> options = new ArrayList<>();
        for (ClueStatus target : allowedStages) {
            options.add(new ClueStatusTransitionResponse.TransitionOption(
                    target.getKey(),
                    target.getName(),
                    STAGE_TO_LEADS_STAGE.getOrDefault(target, LeadsStageEnum.NEW).getKey(),
                    STAGE_TO_BIZ_STATUS.getOrDefault(target, BizStatusEnum.NEW).getKey()
            ));
        }

        return new ClueStatusTransitionResponse(
                currentStage.getKey(),
                currentStage.getName(),
                clue.getLeadsStage(),
                clue.getBizStatus(),
                clue.getLifeStatus(),
                options
        );
    }

    /**
     * 执行状态流转
     * 包含：验证流转合法性 → 更新多维状态 → 记录操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void transition(ClueStatusTransitionRequest request, String userId, String orgId) {
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        if (originClue == null) {
            throw new GenericException(Translator.get("clue_not_found"));
        }

        // 1. 解析目标阶段
        ClueStatus targetStage = ClueStatus.ofKey(request.getStage());
        if (targetStage == null) {
            throw new GenericException(Translator.get("invalid_status") + ": " + request.getStage());
        }

        // 2. 解析当前阶段
        ClueStatus currentStage = ClueStatus.ofKey(originClue.getStage());
        if (currentStage == null) {
            currentStage = ClueStatus.NEW;
        }

        // 3. 验证流转合法性
        if (!currentStage.canTransitionTo(targetStage)) {
            throw new GenericException(Translator.get("status_transition_not_allowed") +
                    ": " + currentStage.getName() + " -> " + targetStage.getName());
        }

        // 4. 构建更新对象
        Clue updateClue = new Clue();
        updateClue.setId(request.getId());
        updateClue.setStage(targetStage.getKey());
        updateClue.setLastStage(originClue.getStage());
        updateClue.setUpdateTime(System.currentTimeMillis());
        updateClue.setUpdateUser(userId);

        // 5. 同步多维状态（优先使用请求中的值，否则用默认映射）
        updateClue.setLeadsStage(
                StringUtils.isNotBlank(request.getLeadsStage())
                        ? request.getLeadsStage()
                        : STAGE_TO_LEADS_STAGE.getOrDefault(targetStage, LeadsStageEnum.NEW).getKey()
        );
        updateClue.setBizStatus(
                StringUtils.isNotBlank(request.getBizStatus())
                        ? request.getBizStatus()
                        : STAGE_TO_BIZ_STATUS.getOrDefault(targetStage, BizStatusEnum.NEW).getKey()
        );
        if (StringUtils.isNotBlank(request.getLifeStatus())) {
            updateClue.setLifeStatus(request.getLifeStatus());
        }
        // 仅失败/无效关闭类终态自动设置生命状态为作废；
        // 成功/已转化(CONVERTED/SUCCESS)为有效终态，保持活跃(ACTIVE)
        if (StringUtils.isBlank(request.getLifeStatus()) &&
                (targetStage == ClueStatus.FAIL || targetStage == ClueStatus.CLOSED)) {
            updateClue.setLifeStatus(LifeStatusEnum.INVALID.getKey());
        } else if (StringUtils.isBlank(request.getLifeStatus()) &&
                (targetStage == ClueStatus.SUCCESS || targetStage == ClueStatus.CONVERTED)) {
            updateClue.setLifeStatus(LifeStatusEnum.ACTIVE.getKey());
        }

        // 6. 执行更新
        clueMapper.update(updateClue);

        // 7. 记录日志
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(originClue.getName())
                        .originalValue(originClue)
                        .modifiedValue(clueMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }
}
