package cn.cordys.crm.clue.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.constants.CluePoolAssignConstants;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.domain.CluePoolAssignRule;
import cn.cordys.crm.clue.dto.AssignRuleConditionDTO;
import cn.cordys.crm.clue.dto.CluePoolAssignRuleDTO;
import cn.cordys.crm.clue.mapper.ExtCluePoolAssignRuleMapper;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 线索池分配规则服务
 * <p>
 * 核心能力:按条件匹配线索并自动分配给目标人员,支持仅分配给某人(SINGLE)或循环分配(ROUND_ROBIN)。
 * <ul>
 *   <li>{@link #saveRules} — 保存池的分配规则(先删后插)</li>
 *   <li>{@link #getRulesByPoolId} — 查池的分配规则(回显)</li>
 *   <li>{@link #getRulesByPoolIds} — 批量查(分页回显用)</li>
 *   <li>{@link #matchAndAssign} — 核心:单条线索匹配规则并自动分配</li>
 *   <li>{@link #batchMatchAndAssign} — 批量:对池中所有未分配线索执行匹配</li>
 * </ul>
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class CluePoolAssignRuleService {

    @Resource
    private ExtCluePoolAssignRuleMapper extCluePoolAssignRuleMapper;
    @Resource
    private BaseMapper<CluePoolAssignRule> assignRuleMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    @Lazy
    private PoolClueService poolClueService;
    @Resource
    private ClueFieldService clueFieldService;

    /**
     * 保存线索池的分配规则(先删后插)
     *
     * @param poolId      线索池ID
     * @param rules       分配规则集合(来自前端请求,可为空)
     * @param currentUserId 当前用户ID
     * @param currentOrgId  当前组织ID
     */
    public void saveRules(String poolId, List<CluePoolAssignRuleDTO> rules, String currentUserId, String currentOrgId) {
        checkPoolBelongsToOrganization(poolId, currentOrgId);
        if (CollectionUtils.isEmpty(rules)) {
            extCluePoolAssignRuleMapper.deleteByPoolId(poolId, currentOrgId);
            return;
        }

        long now = System.currentTimeMillis();
        List<CluePoolAssignRule> toInsert = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            CluePoolAssignRuleDTO dto = rules.get(i);
            if (dto == null) {
                throw invalidRule("assignRules");
            }
            List<String> userIds = extractTargetUserIds(dto);
            validateRule(dto, userIds);

            CluePoolAssignRule rule = new CluePoolAssignRule();
            BeanUtils.copyBean(rule, dto);
            rule.setId(IDGenerator.nextStr());
            rule.setPoolId(poolId);
            rule.setOrganizationId(currentOrgId);
            rule.setConditions(JSON.toJSONString(dto.getConditionList()));
            rule.setTargetUserIds(JSON.toJSONString(userIds));
            if (rule.getPos() == null) {
                rule.setPos(i);
            }
            if (rule.getCurrentIndex() == null || rule.getCurrentIndex() < 0
                    || rule.getCurrentIndex() >= userIds.size()) {
                rule.setCurrentIndex(0);
            }
            if (rule.getEnable() == null) {
                rule.setEnable(true);
            }
            rule.setCreateTime(now);
            rule.setUpdateTime(now);
            rule.setCreateUser(currentUserId);
            rule.setUpdateUser(currentUserId);
            toInsert.add(rule);
        }

        // 所有规则校验通过后再替换，避免非法请求清空已有配置。
        extCluePoolAssignRuleMapper.deleteByPoolId(poolId, currentOrgId);
        extCluePoolAssignRuleMapper.batchInsert(toInsert);
    }

    private List<String> extractTargetUserIds(CluePoolAssignRuleDTO dto) {
        List<String> userIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dto.getTargetUserNames())) {
            userIds.addAll(dto.getTargetUserNames().stream()
                    .filter(Objects::nonNull)
                    .map(ScopeNameDTO::getId)
                    .toList());
        } else if (StringUtils.isNotBlank(dto.getTargetUserIds())) {
            try {
                List<String> storedUserIds = JSON.parseArray(dto.getTargetUserIds(), String.class);
                if (CollectionUtils.isNotEmpty(storedUserIds)) {
                    userIds.addAll(storedUserIds);
                }
            } catch (Exception e) {
                throw invalidRule("assignRules.targetUserIds");
            }
        }
        return userIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private void validateRule(CluePoolAssignRuleDTO dto, List<String> userIds) {
        boolean single = Strings.CS.equals(dto.getAssignType(), CluePoolAssignConstants.ASSIGN_TYPE_SINGLE);
        boolean roundRobin = Strings.CS.equals(dto.getAssignType(), CluePoolAssignConstants.ASSIGN_TYPE_ROUND_ROBIN);
        if (!single && !roundRobin) {
            throw invalidRule("assignRules.assignType");
        }
        if ((single && userIds.size() != 1) || (roundRobin && userIds.isEmpty())) {
            throw invalidRule("assignRules.targetUserNames");
        }
        if (CollectionUtils.isEmpty(dto.getConditionList())) {
            return;
        }
        for (AssignRuleConditionDTO condition : dto.getConditionList()) {
            if (condition == null || StringUtils.isBlank(condition.getFieldId())) {
                throw invalidRule("assignRules.conditionList.fieldId");
            }
            if (StringUtils.isBlank(condition.getOperator()) || !isSupportedOperator(condition.getOperator())) {
                throw invalidRule("assignRules.conditionList.operator");
            }
            if (StringUtils.isBlank(condition.getValue())) {
                throw invalidRule("assignRules.conditionList.value");
            }
        }
    }

    private boolean isSupportedOperator(String operator) {
        return Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_EQUALS)
                || Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_NOT_EQUALS)
                || Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_CONTAINS);
    }

    private GenericException invalidRule(String field) {
        return new GenericException(Translator.get("invalid_parameter") + ": " + field);
    }

    /**
     * 查询线索池的分配规则(回显)
     *
     * @param poolId 线索池ID
     * @return 分配规则集合(含目标人员名称)
     */
    public List<CluePoolAssignRuleDTO> getRulesByPoolId(String poolId) {
        List<CluePoolAssignRuleDTO> rules = extCluePoolAssignRuleMapper.selectByPoolId(poolId);
        if (CollectionUtils.isEmpty(rules)) {
            return new ArrayList<>();
        }
        fillTargetUserNames(rules);
        return rules;
    }

    /**
     * 批量查询多个线索池的分配规则(分页回显用)
     *
     * @param poolIds 线索池ID集合
     * @return 分配规则集合(含目标人员名称)
     */
    public List<CluePoolAssignRuleDTO> getRulesByPoolIds(List<String> poolIds) {
        if (CollectionUtils.isEmpty(poolIds)) {
            return new ArrayList<>();
        }
        List<CluePoolAssignRuleDTO> rules = extCluePoolAssignRuleMapper.selectByPoolIds(poolIds);
        if (CollectionUtils.isEmpty(rules)) {
            return new ArrayList<>();
        }
        fillTargetUserNames(rules);
        return rules;
    }

    /**
     * 填充目标人员名称(用于前端回显)
     */
    private void fillTargetUserNames(List<CluePoolAssignRuleDTO> rules) {
        rules.forEach(rule -> {
            // 解析条件 JSON
            if (StringUtils.isNotBlank(rule.getConditions())) {
                rule.setConditionList(JSON.parseArray(rule.getConditions(), AssignRuleConditionDTO.class));
            }
            // 解析目标用户ID,查名称
            if (StringUtils.isNotBlank(rule.getTargetUserIds())) {
                List<String> userIds = JSON.parseArray(rule.getTargetUserIds(), String.class);
                if (CollectionUtils.isNotEmpty(userIds)) {
                    // 用 scope 机制查名称(用户ID直接作为 scopeId)
                    List<ScopeNameDTO> scopes = userExtendService.getScope(userIds);
                    rule.setTargetUserNames(scopes);
                }
            }
        });
    }

    /**
     * 核心:单条线索匹配分配规则并自动分配
     * <p>
     * 遍历池的启用分配规则(按优先级排序),第一个命中的规则执行分配。
     * 未命中任何规则则线索留在池中。
     *
     * @param clueId       线索ID
     * @param poolId       线索池ID
     * @param currentOrgId 当前组织ID
     * @param operatorId   操作人ID(系统自动分配时为 ADMIN)
     * @return true=已成功分配; false=未匹配到规则或分配失败
     */
    public boolean matchAndAssign(String clueId, String poolId, String currentOrgId, String operatorId) {
        checkPoolBelongsToOrganization(poolId, currentOrgId);
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null || !Strings.CS.equals(clue.getPoolId(), poolId)
                || !Strings.CS.equals(clue.getOrganizationId(), currentOrgId)
                || !Boolean.TRUE.equals(clue.getInSharedPool())) {
            return false;
        }
        return doMatchAndAssign(clueId, poolId, currentOrgId, operatorId);
    }

    private boolean doMatchAndAssign(String clueId, String poolId, String currentOrgId, String operatorId) {
        List<CluePoolAssignRule> rules = extCluePoolAssignRuleMapper.selectEnabledByPoolId(poolId, currentOrgId);
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }

        List<BaseModuleFieldValue> fieldValues = clueFieldService.getResourceFieldMap(List.of(clueId), true).get(clueId);
        if (CollectionUtils.isEmpty(fieldValues)) {
            fieldValues = new ArrayList<>();
        }
        Map<String, Object> fieldValueMap = fieldValues.stream()
                .filter(v -> v.getFieldId() != null)
                .collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue, (a, b) -> a));

        for (CluePoolAssignRule rule : rules) {
            if (matchRule(rule, fieldValueMap)) {
                String targetUserId = resolveTargetUser(rule, operatorId, currentOrgId);
                if (targetUserId != null) {
                    try {
                        poolClueService.assign(clueId, targetUserId, currentOrgId, operatorId);
                        return true;
                    } catch (Exception e) {
                        log.warn("线索池自动分配失败, clueId={}, poolId={}, targetUser={}, error={}",
                                clueId, poolId, targetUserId, e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 批量:对池中所有未分配线索执行规则匹配
     *
     * @param poolId       线索池ID
     * @param currentOrgId 当前组织ID
     * @param operatorId   操作人ID
     * @return 成功分配数量
     */
    public int batchMatchAndAssign(String poolId, String currentOrgId, String operatorId) {
        checkPoolBelongsToOrganization(poolId, currentOrgId);
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clue::getPoolId, poolId)
                .eq(Clue::getOrganizationId, currentOrgId)
                .eq(Clue::getInSharedPool, true);
        List<Clue> clues = clueMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(clues)) {
            return 0;
        }

        int successCount = 0;
        for (Clue clue : clues) {
            try {
                boolean assigned = doMatchAndAssign(clue.getId(), poolId, currentOrgId, operatorId);
                if (assigned) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("批量自动分配线索失败, clueId={}, error={}", clue.getId(), e.getMessage());
            }
        }
        return successCount;
    }

    /**
     * 匹配单条规则(所有条件 AND 关系)
     *
     * @param rule          分配规则
     * @param fieldValueMap 线索字段值 Map(fieldId -> fieldValue)
     * @return 是否命中
     */
    private boolean matchRule(CluePoolAssignRule rule, Map<String, Object> fieldValueMap) {
        if (StringUtils.isBlank(rule.getConditions())) {
            // 无条件规则 = 匹配所有线索
            return true;
        }
        List<AssignRuleConditionDTO> conditions;
        try {
            conditions = JSON.parseArray(rule.getConditions(), AssignRuleConditionDTO.class);
        } catch (Exception e) {
            log.warn("线索池分配规则条件格式错误, ruleId={}", rule.getId());
            return false;
        }
        if (CollectionUtils.isEmpty(conditions)) {
            return true;
        }
        // 所有条件都满足(AND)
        for (AssignRuleConditionDTO condition : conditions) {
            if (!matchCondition(condition, fieldValueMap)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 匹配单个条件
     */
    private boolean matchCondition(AssignRuleConditionDTO condition, Map<String, Object> fieldValueMap) {
        if (condition == null || StringUtils.isBlank(condition.getFieldId())
                || StringUtils.isBlank(condition.getOperator()) || StringUtils.isBlank(condition.getValue())
                || !isSupportedOperator(condition.getOperator())) {
            return false;
        }
        Object actualValue = fieldValueMap.get(condition.getFieldId());
        String expectedValue = condition.getValue();
        String operator = condition.getOperator();

        if (actualValue == null) {
            return false;
        }

        // fieldValue 可能是 String 或 List(多选)
        String actualStr;
        if (actualValue instanceof List<?> list) {
            actualStr = list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        } else {
            actualStr = String.valueOf(actualValue);
        }

        return switch (operator) {
            case CluePoolAssignConstants.OPERATOR_EQUALS -> Strings.CS.equals(actualStr, expectedValue);
            case CluePoolAssignConstants.OPERATOR_NOT_EQUALS -> !Strings.CS.equals(actualStr, expectedValue);
            case CluePoolAssignConstants.OPERATOR_CONTAINS -> actualStr.contains(expectedValue);
            default -> false;
        };
    }

    /**
     * 根据分配方式解析目标用户
     *
     * @param rule          分配规则
     * @param operatorId    操作人(系统分配时为 ADMIN)
     * @param currentOrgId  当前组织ID
     * @return 目标用户ID, null=无法分配
     */
    private String resolveTargetUser(CluePoolAssignRule rule, String operatorId, String currentOrgId) {
        List<String> userIds;
        try {
            userIds = JSON.parseArray(rule.getTargetUserIds(), String.class).stream()
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("线索池分配规则目标人员格式错误, ruleId={}", rule.getId());
            return null;
        }
        if (CollectionUtils.isEmpty(userIds)) {
            return null;
        }

        if (Strings.CS.equals(rule.getAssignType(), CluePoolAssignConstants.ASSIGN_TYPE_SINGLE)) {
            return userIds.size() == 1 ? userIds.getFirst() : null;
        }
        if (!Strings.CS.equals(rule.getAssignType(), CluePoolAssignConstants.ASSIGN_TYPE_ROUND_ROBIN)) {
            return null;
        }

        CluePoolAssignRule currentRule = rule;
        for (int attempt = 0; attempt < 5; attempt++) {
            Integer expectedIndex = currentRule.getCurrentIndex();
            int index = expectedIndex == null || expectedIndex < 0 || expectedIndex >= userIds.size()
                    ? 0 : expectedIndex;
            int nextIndex = (index + 1) % userIds.size();
            int updated = extCluePoolAssignRuleMapper.compareAndSetCurrentIndex(
                    currentRule.getId(), currentOrgId, expectedIndex, nextIndex,
                    operatorId, System.currentTimeMillis());
            if (updated == 1) {
                return userIds.get(index);
            }

            CluePoolAssignRule criteria = new CluePoolAssignRule();
            criteria.setId(currentRule.getId());
            criteria.setOrganizationId(currentOrgId);
            currentRule = assignRuleMapper.selectOne(criteria);
            if (currentRule == null || !Boolean.TRUE.equals(currentRule.getEnable())) {
                return null;
            }
        }
        log.warn("线索池循环分配指针更新冲突, ruleId={}", rule.getId());
        return null;
    }

    /**
     * 删除线索池的所有分配规则(删除池时调用)
     *
     * @param poolId 线索池ID
     */
    public void deleteByPoolId(String poolId, String currentOrgId) {
        extCluePoolAssignRuleMapper.deleteByPoolId(poolId, currentOrgId);
    }

    private void checkPoolBelongsToOrganization(String poolId, String currentOrgId) {
        CluePool pool = cluePoolMapper.selectByPrimaryKey(poolId);
        if (pool == null || !Strings.CS.equals(pool.getOrganizationId(), currentOrgId)) {
            throw new GenericException(Translator.get("clue_pool_not_exist"));
        }
    }
}
