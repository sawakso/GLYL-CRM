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
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
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
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

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
        // DEPT 目标:用户名单由部门动态解析,只校验目标部门;USER 目标:校验指定人员
        boolean deptTarget = Strings.CS.equals(dto.getAssignTargetType(), CluePoolAssignConstants.ASSIGN_TARGET_TYPE_DEPT);
        if (deptTarget) {
            if (StringUtils.isBlank(dto.getTargetDeptIds())) {
                throw invalidRule("assignRules.targetDeptIds");
            }
        } else {
            if ((single && userIds.size() != 1) || (roundRobin && userIds.isEmpty())) {
                throw invalidRule("assignRules.targetUserNames");
            }
        }
        if (CollectionUtils.isEmpty(dto.getConditionList())) {
            return;
        }
        for (AssignRuleConditionDTO condition : dto.getConditionList()) {
            if (condition == null || StringUtils.isBlank(condition.getFieldId())) {
                throw invalidRule("assignRules.conditionList.fieldId");
            }
            boolean timeType = Strings.CS.equals(condition.getConditionType(), CluePoolAssignConstants.CONDITION_TYPE_TIME);
            if (StringUtils.isBlank(condition.getOperator())
                    || (timeType ? !isSupportedTimeOperator(condition.getOperator()) : !isSupportedOperator(condition.getOperator()))) {
                throw invalidRule("assignRules.conditionList.operator");
            }
            if (StringUtils.isBlank(condition.getValue())) {
                throw invalidRule("assignRules.conditionList.value");
            }
            if (timeType && Strings.CS.equals(condition.getOperator(), CluePoolAssignConstants.OPERATOR_BETWEEN)
                    && StringUtils.isBlank(condition.getValue2())) {
                throw invalidRule("assignRules.conditionList.value2");
            }
        }
    }

    private boolean isSupportedOperator(String operator) {
        return Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_EQUALS)
                || Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_NOT_EQUALS)
                || Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_CONTAINS);
    }

    private boolean isSupportedTimeOperator(String operator) {
        return Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_BEFORE)
                || Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_AFTER)
                || Strings.CS.equals(operator, CluePoolAssignConstants.OPERATOR_BETWEEN);
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

    /**
     * 核心:匹配线索池分配规则,仅为线索设置负责人(不动用线索池状态)。
     * <p>
     * 适用于「市场表单回流」等场景:线索本身就是普通线索(in_shared_pool=false, 不放入池),
     * 线索池仅作为「分配规则」使用 —— 命中规则后直接把匹配到的目标人员设为线索负责人,
     * 不修改 pool_id / in_shared_pool / stage 等归属状态, 线索始终显示在「线索」列表中。
     * <p>
     * 匹配逻辑与 {@link #matchAndAssign} 一致(按字段值命中规则, 支持 SINGLE / ROUND_ROBIN / DEPT 目标)。
     * 未命中任何规则时, 线索保持无负责人(owner 为空), 但依旧是普通线索、对管理员/角色可见。
     *
     * @param clueId       线索ID
     * @param poolId       线索池ID(仅用于读取分配规则)
     * @param currentOrgId 当前组织ID
     * @param operatorId   操作人ID(系统分配时为 ADMIN)
     * @return true=已命中规则并设置负责人; false=未匹配到规则或目标用户不可用
     */
    public boolean matchAndAssignOwner(String clueId, String poolId, String currentOrgId, String operatorId) {
        checkPoolBelongsToOrganization(poolId, currentOrgId);
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null || !Strings.CS.equals(clue.getOrganizationId(), currentOrgId)) {
            return false;
        }
        return doMatchAndAssignOwner(clueId, poolId, currentOrgId, operatorId);
    }

    /**
     * 仅设置负责人版的匹配分配: 复用规则匹配, 命中后调用 {@link #assignOwner} 只改 owner。
     */
    private boolean doMatchAndAssignOwner(String clueId, String poolId, String currentOrgId, String operatorId) {
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

        // 系统创建时间注入,供时间条件(CLUE_CREATE_TIME)使用
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue != null && clue.getCreateTime() != null) {
            fieldValueMap.put(CluePoolAssignConstants.TIME_FIELD_CLUE_CREATE_TIME, clue.getCreateTime());
        }

        for (CluePoolAssignRule rule : rules) {
            if (matchRule(rule, fieldValueMap)) {
                String targetUserId = resolveTargetUser(rule, operatorId, currentOrgId);
                if (targetUserId != null) {
                    try {
                        return assignOwner(clueId, targetUserId, currentOrgId, operatorId);
                    } catch (Exception e) {
                        log.warn("线索负责人自动分配失败, clueId={}, poolId={}, targetUser={}, error={}",
                                clueId, poolId, targetUserId, e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 仅为线索设置负责人, 不改变线索池归属状态(in_shared_pool / pool_id / stage 保持不变)。
     */
    private boolean assignOwner(String clueId, String targetUserId, String currentOrgId, String operatorId) {
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null || !Strings.CS.equals(clue.getOrganizationId(), currentOrgId)) {
            return false;
        }
        clue.setOwner(targetUserId);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(operatorId);
        clueMapper.update(clue);

        // 记录负责人变更历史
        try {
            clueOwnerHistoryService.add(clue, operatorId, false);
        } catch (Exception e) {
            log.warn("记录线索负责人历史失败, clueId={}, error={}", clueId, e.getMessage());
        }

        // 分配通知
        try {
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                    NotificationConstants.Event.CLUE_DISTRIBUTED, clue.getName(), operatorId,
                    currentOrgId, List.of(targetUserId), true);
        } catch (Exception e) {
            log.warn("线索分配通知失败, clueId={}, error={}", clueId, e.getMessage());
        }
        return true;
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

        // 系统创建时间注入,供时间条件(CLUE_CREATE_TIME)使用
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue != null && clue.getCreateTime() != null) {
            fieldValueMap.put(CluePoolAssignConstants.TIME_FIELD_CLUE_CREATE_TIME, clue.getCreateTime());
        }

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
                || StringUtils.isBlank(condition.getOperator())) {
            return false;
        }
        // 时间判断优先于内容字段判断
        if (Strings.CS.equals(condition.getConditionType(), CluePoolAssignConstants.CONDITION_TYPE_TIME)) {
            return matchTimeCondition(condition, fieldValueMap);
        }
        // 内容字段判断(默认)
        if (StringUtils.isBlank(condition.getValue()) || !isSupportedOperator(condition.getOperator())) {
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
            case CluePoolAssignConstants.OPERATOR_EQUALS -> matchesMultiValue(actualStr, expectedValue, false);
            case CluePoolAssignConstants.OPERATOR_NOT_EQUALS -> !matchesMultiValue(actualStr, expectedValue, false);
            case CluePoolAssignConstants.OPERATOR_CONTAINS -> matchesMultiValue(actualStr, expectedValue, true);
            default -> false;
        };
    }

    /**
     * 多值匹配: 条件值支持以「中文逗号、英文逗号、分号、竖线」分隔的多个值, 任一命中即满足。
     * <p>用于「负责区域 等于 北京市,天津市,河北省」这类"或"关系配置。</p>
     *
     * @param actualStr  线索字段实际值(字符串)
     * @param rawValue   条件配置值(可能包含多个用分隔符隔开的值)
     * @param contains   是否包含匹配(CONTAINS 时按包含判断, 否则精确相等)
     * @return 任一匹配返回 true
     */
    private boolean matchesMultiValue(String actualStr, String rawValue, boolean contains) {
        if (StringUtils.isBlank(rawValue) || StringUtils.isBlank(actualStr)) {
            return false;
        }
        // 兼容单值场景
        if (contains) {
            return actualStr.contains(rawValue);
        }
        // 拆分为多值后, 任一精确相等即命中
        List<String> values = Arrays.stream(rawValue.split("[，,;；|]"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (values.size() <= 1) {
            return Strings.CS.equals(actualStr, rawValue.trim());
        }
        for (String value : values) {
            if (Strings.CS.equals(actualStr, value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 时间条件匹配:基于线索任意日期/时间字段或系统创建时间(CLUE_CREATE_TIME)
     * <p>
     * 值统一为毫秒时间戳。BEFORE=早于; AFTER=晚于; BETWEEN=介于[value, value2]。
     */
    private boolean matchTimeCondition(AssignRuleConditionDTO condition, Map<String, Object> fieldValueMap) {
        if (!isSupportedTimeOperator(condition.getOperator()) || StringUtils.isBlank(condition.getValue())) {
            return false;
        }
        Long actual = parseTimeMillis(fieldValueMap.get(condition.getFieldId()));
        if (actual == null) {
            return false;
        }
        Long expected = parseTimeMillis(condition.getValue());
        if (expected == null) {
            return false;
        }
        return switch (condition.getOperator()) {
            case CluePoolAssignConstants.OPERATOR_BEFORE -> actual < expected;
            case CluePoolAssignConstants.OPERATOR_AFTER -> actual > expected;
            case CluePoolAssignConstants.OPERATOR_BETWEEN -> {
                Long end = parseTimeMillis(condition.getValue2());
                yield end != null && actual >= expected && actual <= end;
            }
            default -> false;
        };
    }

    private Long parseTimeMillis(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof java.util.Date date) {
            return date.getTime();
        }
        String str = String.valueOf(value).trim();
        if (StringUtils.isBlank(str)) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
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
        boolean deptTarget = Strings.CS.equals(rule.getAssignTargetType(), CluePoolAssignConstants.ASSIGN_TARGET_TYPE_DEPT);
        if (deptTarget) {
            // 按部门/区域动态解析成员(支持含子部门),人员变动无需改规则
            userIds = resolveDeptMembers(rule, currentOrgId);
        } else {
            try {
                userIds = JSON.parseArray(rule.getTargetUserIds(), String.class).stream()
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .toList();
            } catch (Exception e) {
                log.warn("线索池分配规则目标人员格式错误, ruleId={}", rule.getId());
                return null;
            }
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
     * 按部门/区域动态解析目标成员
     * <p>
     * 根据 {@code targetDeptIds} 调用部门 Mapper 解析成员;当 {@code includeChildDept=true} 时递归包含子部门。
     *
     * @param rule         分配规则
     * @param currentOrgId 当前组织ID
     * @return 成员用户ID集合(已去重),为空表示无法解析
     */
    private List<String> resolveDeptMembers(CluePoolAssignRule rule, String currentOrgId) {
        List<String> deptIds;
        try {
            deptIds = JSON.parseArray(rule.getTargetDeptIds(), String.class);
        } catch (Exception e) {
            log.warn("线索池分配规则目标部门格式错误, ruleId={}", rule.getId());
            return new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(deptIds)) {
            return new ArrayList<>();
        }
        List<String> allDeptIds = deptIds;
        if (Boolean.TRUE.equals(rule.getIncludeChildDept())) {
            // selectChildrenByIds 递归含孙级,且结果已包含传入部门自身
            List<String> withChildren = extDepartmentMapper.selectChildrenByIds(deptIds);
            if (CollectionUtils.isNotEmpty(withChildren)) {
                allDeptIds = withChildren;
            }
        }
        List<String> memberIds = extDepartmentMapper.getUserIdsByDeptIds(allDeptIds);
        if (CollectionUtils.isEmpty(memberIds)) {
            return new ArrayList<>();
        }
        return memberIds.stream().filter(StringUtils::isNotBlank).distinct().toList();
    }

    /**
     * 删除线索池的所有分配规则(删除池时调用)
     *
     * @param poolId 线索池ID
     */
    public void deleteByPoolId(String poolId, String currentOrgId) {
        extCluePoolAssignRuleMapper.deleteByPoolId(poolId, currentOrgId);
    }

    /**
     * 检查线索池是否有启用的分配规则。
     *
     * @param poolId       线索池ID
     * @param currentOrgId 当前组织ID
     * @return true=至少有一条启用规则
     */
    public boolean hasEnabledRules(String poolId, String currentOrgId) {
        List<CluePoolAssignRule> rules = extCluePoolAssignRuleMapper.selectEnabledByPoolId(poolId, currentOrgId);
        return CollectionUtils.isNotEmpty(rules);
    }

    private void checkPoolBelongsToOrganization(String poolId, String currentOrgId) {
        CluePool pool = cluePoolMapper.selectByPrimaryKey(poolId);
        if (pool == null || !Strings.CS.equals(pool.getOrganizationId(), currentOrgId)) {
            throw new GenericException(Translator.get("clue_pool_not_exist"));
        }
    }
}
