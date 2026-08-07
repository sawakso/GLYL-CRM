package cn.cordys.crm.marketingform.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.ClueField;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.constants.BizStatusEnum;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.constants.LeadsStageEnum;
import cn.cordys.crm.clue.constants.LifeStatusEnum;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.clue.service.CluePoolAssignRuleService;
import cn.cordys.crm.clue.service.CluePoolService;
import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.marketingform.domain.MarketingFormSubmission;
import cn.cordys.crm.marketingform.dto.request.MarketingFormSubmitRequest;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表单→线索桥接服务。意向客户通过二维码公开提交表单后, 本服务把表单字段值映射成 Clue,
 * 按 PoolClueService.realImport 的 ADD 模式直接插入线索池 (inSharedPool=true, poolId=目标池)。
 * 无需登录上下文: orgId 从 marketing_form 记录解析, userId 用 InternalUser.ADMIN。
 *
 * <p>防呆/去重网关 (1.13.0): 提交前按「表单配置(可继承线索池默认)」解析去重策略,
 * 在时间窗内按身份键 (手机号 &gt; 设备指纹 &gt; IP) 查找历史提交, 按策略分流:
 * <ul>
 *     <li>NONE   : 不去重, 每次提交都新建线索 (原行为)</li>
 *     <li>UPDATE : 窗口内同一身份再次提交 → 覆盖更新原线索, 不新建</li>
 *     <li>SKIP   : 窗口内同一身份再次提交 → 拦截, 直接返回原线索, 不新建</li>
 *     <li>MARK   : 窗口内同一身份再次提交 → 新建线索并标记 is_duplicated + duplicate_clue_id</li>
 * </ul>
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class MarketingLeadBridgeService {

    /** 可视为手机号的 Clue 固定列 (去重身份键提取用) */
    private static final Set<String> PHONE_CLUE_FIELDS = Set.of("phone", "mobile", "tel");

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<MarketingFormSubmission> marketingFormSubmissionMapper;
    @Resource
    private MarketingFormService marketingFormService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private BaseMapper<ClueField> clueFieldMapper;
    @Resource
    private CluePoolAssignRuleService cluePoolAssignRuleService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private LogService logService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;

    /**
     * 处理公开提交: 表单值 → 去重网关 → 创建/更新线索进池 → (可选)触发自动分配。
     *
     * @param token   二维码令牌
     * @param request 提交请求 (含 moduleFields / deviceId)
     * @param httpRequest HTTP 请求 (取 IP)
     * @return 关联的线索 ID (新建或已存在的)
     */
    public String bridge(String token, MarketingFormSubmitRequest request, HttpServletRequest httpRequest) {
        // 1. 按 token 解析市场表单
        MarketingForm form = marketingFormService.getByToken(token);
        if (form == null) {
            throw new GenericException(Translator.get("marketing.form.not.exist"));
        }
        if (!"ACTIVE".equals(form.getStatus())) {
            throw new GenericException(Translator.get("marketing.form.not.active"));
        }

        String orgId = form.getOrganizationId();
        String operatorId = InternalUser.ADMIN.getValue();

        // 2. 设置组织上下文 (无 session, 用 ThreadLocal, finally 清理)
        OrganizationContext.setOrganizationId(orgId);
        try {
            // 3. 校验目标线索池
            CluePool pool = validateTargetPool(form.getTargetPoolId(), orgId);

            // 3.1 确保来源字段选项包含当前表单名 (线索来源自动关联到表单名, 无需用户填写)
            ensureSourceOption(form, orgId);

            // 4. 解析字段映射 + 提取表单值
            Map<String, String> fieldMapping = parseFieldMapping(form.getFieldMapping());
            Map<String, Object> formValues = extractFormValues(request.getModuleFields());

            // 4.1 防呆: 表单开启"姓名必填"时, 校验映射到线索 name 的字段非空才允许提交
            if (Boolean.TRUE.equals(form.getRequireName())) {
                String nameValue = resolveNameValue(fieldMapping, formValues);
                if (StringUtils.isBlank(nameValue)) {
                    throw new GenericException(Translator.get("marketing.form.name.required"));
                }
            }

            // 5. 去重网关: 解析生效配置 → 提取身份键 → 窗口内查重 → 按策略分流
            DedupConfig config = resolveDedupConfig(form, pool);
            Identity identity = resolveIdentity(config, fieldMapping, formValues, request, httpRequest);
            MarketingFormSubmission prior = findPriorSubmission(form.getId(), identity, config.windowMinutes());

            if (prior != null && StringUtils.isNotBlank(prior.getClueId())) {
                switch (config.strategy()) {
                    case "UPDATE" -> {
                        return handleUpdate(form, fieldMapping, formValues, orgId, operatorId,
                                prior, request, httpRequest, identity);
                    }
                    case "SKIP" -> {
                        log.info("表单 {} 拦截重复提交: 身份 {}={}, 复用线索 {}", form.getId(),
                                identity.keyType(), identity.value(), prior.getClueId());
                        recordSubmission(form.getId(), prior.getClueId(), orgId, httpRequest,
                                request.getDeviceId(), identity, "SKIP");
                        return prior.getClueId();
                    }
                    case "MARK" -> {
                        return handleCreate(form, pool, fieldMapping, formValues, orgId, operatorId,
                                request, httpRequest, identity, prior.getClueId());
                    }
                    default -> {
                        // NONE / 未知策略: 保持原行为新建
                    }
                }
            }

            // 6. 默认新建
            return handleCreate(form, pool, fieldMapping, formValues, orgId, operatorId,
                    request, httpRequest, identity, null);
        } finally {
            OrganizationContext.clear();
        }
    }

    // ==================== 去重网关 ====================

    /** 去重生效配置 (表单可覆盖池默认, INHERIT 表示跟随池) */
    private record DedupConfig(String strategy, int windowMinutes, String keyType) {
    }

    /** 免登录身份键: 类型 + 取值 */
    private record Identity(String keyType, String value) {
    }

    /**
     * 解析生效去重配置: 表单配置为空或 INHERIT 时跟随线索池, 再兜底默认值。
     */
    private DedupConfig resolveDedupConfig(MarketingForm form, CluePool pool) {
        String strategy = StringUtils.defaultIfBlank(form.getDedupStrategy(), "INHERIT");
        if ("INHERIT".equalsIgnoreCase(strategy)) {
            strategy = StringUtils.defaultIfBlank(pool.getDedupStrategy(), "UPDATE");
        }
        Integer formWindow = form.getDedupWindow();
        Integer poolWindow = pool.getDedupWindow();
        int window = formWindow != null ? formWindow
                : (poolWindow != null ? poolWindow : 5);
        String key = StringUtils.defaultIfBlank(form.getDedupKey(),
                StringUtils.defaultIfBlank(pool.getDedupKey(), "AUTO"));
        return new DedupConfig(strategy, window, key);
    }

    /**
     * 提取身份键值: PHONE 只认手机号, DEVICE 只认设备指纹, IP 只认 IP;
     * AUTO 按 手机号 > 设备指纹 > IP 逐级降级。全部缺失则返回 null (无法去重, 走新建)。
     */
    private Identity resolveIdentity(DedupConfig config, Map<String, String> fieldMapping,
                                     Map<String, Object> formValues, MarketingFormSubmitRequest request,
                                     HttpServletRequest httpRequest) {
        String phone = extractPhone(fieldMapping, formValues);
        String deviceId = StringUtils.trimToNull(request == null ? null : request.getDeviceId());
        String ip = StringUtils.trimToNull(getClientIp(httpRequest));
        return switch (StringUtils.defaultIfBlank(config.keyType(), "AUTO")) {
            case "PHONE" -> phone != null ? new Identity("PHONE", phone) : null;
            case "DEVICE" -> deviceId != null ? new Identity("DEVICE", deviceId) : null;
            case "IP" -> ip != null ? new Identity("IP", ip) : null;
            default -> {
                if (phone != null) {
                    yield new Identity("PHONE", phone);
                }
                if (deviceId != null) {
                    yield new Identity("DEVICE", deviceId);
                }
                yield ip != null ? new Identity("IP", ip) : null;
            }
        };
    }

    /**
     * 从表单值中提取手机号: 优先看映射到 phone/mobile/tel 的表单字段, 兜底匹配字段名。
     */
    private String extractPhone(Map<String, String> fieldMapping, Map<String, Object> formValues) {
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            if (entry.getValue() != null && PHONE_CLUE_FIELDS.contains(entry.getValue())) {
                String s = StringUtils.trimToNull(toStr(formValues.get(entry.getKey())));
                if (s != null) {
                    return s;
                }
            }
        }
        for (Map.Entry<String, Object> entry : formValues.entrySet()) {
            String k = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if (k.contains("phone") || k.contains("mobile") || k.contains("tel")) {
                String s = StringUtils.trimToNull(toStr(entry.getValue()));
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private String toStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /**
     * 把市场表单提交的字段值(EAV, fieldId=市场表单字段id)转换为线索模块字段值(fieldId=线索字段id)。
     * <p>市场表单「引用 CRM 字段」落库为独立字段(id 重新生成), 字段定义里保留了 refFieldId
     * (被引用的线索字段ID) 与 businessKey。转换优先级:
     * <ol>
     *     <li>市场表单字段定义.refFieldId —— 引用字段的精确映射 (不依赖注册表, 覆盖所有线索字段)</li>
     *     <li>fieldMapping businessKey → 线索字段businessKey → 线索字段id —— 兼容手动映射</li>
     *     <li>字段名匹配 —— 兼容早期已保存表单 (无 refFieldId/businessKey 的引用字段)</li>
     * </ol>
     * 未映射到线索字段的值被丢弃(非线索字段, 无保存目标)。
     *
     * @param form        市场表单 (用于读取字段定义)
     * @param fieldMapping 表单字段id → 线索字段businessKey (列映射用, 兼容回退)
     * @param orgId       组织ID
     * @param formFields  表单提交值列表 (fieldId=市场表单字段id)
     * @return 转换后的字段值列表
     */
    private List<BaseModuleFieldValue> toClueModuleFields(MarketingForm form, Map<String, String> fieldMapping,
                                                          String orgId, List<BaseModuleFieldValue> formFields) {
        if (CollectionUtils.isEmpty(formFields)) {
            return formFields;
        }
        // 线索字段: 字段id恒等 + businessKey→id + name→id (多键查表, 冲突取首个)
        Map<String, String> clueFieldIdByKey = new HashMap<>();
        try {
            for (BaseField f : moduleFormService.getAllFields(FormKey.CLUE.getKey(), orgId)) {
                clueFieldIdByKey.putIfAbsent(f.getId(), f.getId());
                if (StringUtils.isNotBlank(f.getBusinessKey())) {
                    clueFieldIdByKey.putIfAbsent(f.getBusinessKey(), f.getId());
                }
                if (StringUtils.isNotBlank(f.getName())) {
                    clueFieldIdByKey.putIfAbsent(f.getName(), f.getId());
                }
            }
        } catch (Exception e) {
            log.warn("加载线索字段定义失败, 跳过 EAV 落库: {}", e.getMessage());
            return new ArrayList<>();
        }
        // 市场表单字段定义: 表单字段id → 被引用的线索字段id (refFieldId) / 字段名
        Map<String, String> formFieldRefId = new HashMap<>();
        Map<String, String> formFieldName = new HashMap<>();
        try {
            for (BaseField f : moduleFormService.getAllFields(form.getId(), orgId)) {
                if (StringUtils.isNotBlank(f.getRefFieldId())) {
                    formFieldRefId.put(f.getId(), f.getRefFieldId());
                }
                if (StringUtils.isNotBlank(f.getName())) {
                    formFieldName.put(f.getId(), f.getName());
                }
            }
        } catch (Exception e) {
            log.warn("加载市场表单字段定义失败, 引用字段映射回退 fieldMapping: {}", e.getMessage());
        }
        List<BaseModuleFieldValue> result = new ArrayList<>(formFields.size());
        for (BaseModuleFieldValue fv : formFields) {
            // 1) refFieldId 精确映射
            String clueFieldId = formFieldRefId.get(fv.getFieldId());
            // 2) 回退: fieldMapping businessKey → 线索字段
            if (clueFieldId == null) {
                String bizKey = fieldMapping.get(fv.getFieldId());
                if (StringUtils.isNotBlank(bizKey)) {
                    clueFieldId = clueFieldIdByKey.get(bizKey);
                }
            }
            // 3) 回退: 表单字段名直接匹配线索字段名 (兼容早期保存的引用数据)
            if (clueFieldId == null) {
                String fieldName = formFieldName.get(fv.getFieldId());
                if (StringUtils.isNotBlank(fieldName)) {
                    clueFieldId = clueFieldIdByKey.get(fieldName);
                }
            }
            if (clueFieldId == null) {
                log.info("市场表单字段 {} (name={}) 未映射到线索字段, 跳过 EAV 保存",
                        fv.getFieldId(), formFieldName.get(fv.getFieldId()));
                continue;
            }
            BaseModuleFieldValue nv = new BaseModuleFieldValue();
            nv.setFieldId(clueFieldId);
            nv.setFieldValue(fv.getFieldValue());
            result.add(nv);
        }
        return result;
    }

    /**
     * 从表单值中提取姓名: 优先看映射到 clue 字段 name 的表单字段, 兜底匹配字段名。
     */
    private String resolveNameValue(Map<String, String> fieldMapping, Map<String, Object> formValues) {
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            if (entry.getValue() != null && "name".equalsIgnoreCase(entry.getValue())) {
                String s = StringUtils.trimToNull(toStr(formValues.get(entry.getKey())));
                if (s != null) {
                    return s;
                }
            }
        }
        for (Map.Entry<String, Object> entry : formValues.entrySet()) {
            String k = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if (k.contains("name") || k.contains("姓名") || k.contains("称呼")) {
                String s = StringUtils.trimToNull(toStr(entry.getValue()));
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * 窗口内查重: 同一表单 + 同一身份键取值 的历史提交, 取最早一条。
     * windowMinutes &lt;= 0 表示不限时间窗 (全历史去重)。
     */
    private MarketingFormSubmission findPriorSubmission(String formId, Identity identity, int windowMinutes) {
        if (identity == null) {
            return null;
        }
        LambdaQueryWrapper<MarketingFormSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingFormSubmission::getMarketingFormId, formId);
        wrapper.eq(MarketingFormSubmission::getIdentityKey, identity.keyType());
        wrapper.eq(MarketingFormSubmission::getIdentityValue, identity.value());
        if (windowMinutes > 0) {
            long cutoff = System.currentTimeMillis() - windowMinutes * 60_000L;
            wrapper.gt(MarketingFormSubmission::getSubmitTime, cutoff);
        }
        wrapper.orderByDesc(MarketingFormSubmission::getSubmitTime);
        List<MarketingFormSubmission> list = marketingFormSubmissionMapper.selectListByLambda(wrapper);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    // ==================== 分流处理 ====================

    /** 新建线索进池 (CREATE / MARK) */
    private String handleCreate(MarketingForm form, CluePool pool, Map<String, String> fieldMapping,
                                Map<String, Object> formValues, String orgId, String operatorId,
                                MarketingFormSubmitRequest request, HttpServletRequest httpRequest,
                                Identity identity, String duplicateClueId) {
        Clue clue = buildClue(form, pool.getId(), fieldMapping, formValues, orgId, operatorId);
        if (StringUtils.isNotBlank(duplicateClueId)) {
            clue.setIsDuplicated(true);
            clue.setDuplicateClueId(duplicateClueId);
        } else {
            // 并发安全去重兜底: 仅首次创建写指纹, 靠唯一索引保证同表单同身份并发只建一条线索。
            // MARK 策略(duplicateClueId 非空)故意创建多条重复线索, 不写指纹避免撞唯一索引。
            if (identity != null && StringUtils.isNotBlank(identity.value())) {
                clue.setDedupFingerprint(buildFingerprint(form.getId(), identity));
            }
        }
        clueMapper.insert(clue);

        // 保存自定义字段值 (走 clue_field EAV)
        // 注意: 跳过唯一性校验 —— 市场表单回流已由上层去重网关(dedup)兜底,
        // 且线索刚 insert, 若在此做唯一自检会撞到当前记录(self-match)导致回滚, 无法进池
        List<BaseModuleFieldValue> clueFields = toClueModuleFields(form, fieldMapping, orgId, request.getModuleFields());
        if (CollectionUtils.isNotEmpty(clueFields)) {
            clueFieldService.saveModuleField(clue, orgId, operatorId, clueFields, false, true);
        }
        log.info("市场表单 {} 提交: 原始字段 {} 个, 转换落库 {} 个, 线索 {}", form.getId(),
                request.getModuleFields() == null ? 0 : request.getModuleFields().size(),
                clueFields == null ? 0 : clueFields.size(), clue.getId());

        // 记日志
        logService.add(new LogDTO(orgId, clue.getId(), operatorId, LogType.ADD, LogModule.CLUE_INDEX, clue.getName()));

        // 记提交留痕
        recordSubmission(form.getId(), clue.getId(), orgId, httpRequest, request.getDeviceId(), identity,
                StringUtils.isNotBlank(duplicateClueId) ? "MARK" : "CREATE");

        // 按线索池分配规则仅为线索设置负责人(不放入池, 线索始终显示在「线索」列表)。
        // 未命中规则时线索保持无负责人但仍是普通线索, 对管理员/角色可见。
        try {
            cluePoolAssignRuleService.matchAndAssignOwner(clue.getId(), pool.getId(), orgId, operatorId);
        } catch (Exception e) {
            log.warn("线索负责人自动分配失败, 线索 {} 未设置负责人: {}", clue.getId(), e.getMessage());
        }

        return clue.getId();
    }

    /** UPDATE 策略: 窗口内同一身份再次提交 → 覆盖更新原线索字段 (列 + EAV), 不新建 */
    private String handleUpdate(MarketingForm form, Map<String, String> fieldMapping,
                                Map<String, Object> formValues, String orgId, String operatorId,
                                MarketingFormSubmission prior, MarketingFormSubmitRequest request,
                                HttpServletRequest httpRequest, Identity identity) {
        Clue origin = clueMapper.selectByPrimaryKey(prior.getClueId());
        if (origin == null) {
            // 原线索已被删除, 降级为新建
            log.warn("UPDATE 去重命中线索 {} 已不存在, 降级新建", prior.getClueId());
            CluePool pool = validateTargetPool(form.getTargetPoolId(), orgId);
            return handleCreate(form, pool, fieldMapping, formValues, orgId, operatorId,
                    request, httpRequest, identity, null);
        }

        // 表单值覆盖到原线索固定列 (仅非空值覆盖, 保留池归属/来源/创建信息)
        applyFormToClue(origin, form, fieldMapping, formValues);
        origin.setUpdateTime(System.currentTimeMillis());
        origin.setUpdateUser(operatorId);
        clueMapper.update(origin);

        // EAV 字段: 精确更新——只删本次提交字段的旧值, 保留其他已存 EAV
        // 全量清空 (deleteByResourceId) 会导致未提交字段数据丢失
        // 跳过唯一性校验: 市场表单回流由上层去重网关兜底
        List<BaseModuleFieldValue> clueFields = toClueModuleFields(form, fieldMapping, orgId, request.getModuleFields());
        if (CollectionUtils.isNotEmpty(clueFields)) {
            java.util.Set<String> updFieldIds = new java.util.HashSet<>();
            for (BaseModuleFieldValue fv : clueFields) {
                updFieldIds.add(fv.getFieldId());
            }
            LambdaQueryWrapper<ClueField> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(ClueField::getResourceId, origin.getId())
                       .in(ClueField::getFieldId, new java.util.ArrayList<>(updFieldIds));
            clueFieldMapper.deleteByLambda(delWrapper);
            clueFieldService.saveModuleField(origin, orgId, operatorId, clueFields, true, true);
        }

        logService.add(new LogDTO(orgId, origin.getId(), operatorId, LogType.UPDATE, LogModule.CLUE_INDEX, origin.getName()));
        recordSubmission(form.getId(), origin.getId(), orgId, httpRequest, request.getDeviceId(), identity, "UPDATE");
        return origin.getId();
    }

    // ==================== 原逻辑保留 ====================

    private CluePool validateTargetPool(String poolId, String orgId) {
        // 未配置目标池 → 默认进组织默认线索池（is_default，兼容名含「新进」的池）
        if (StringUtils.isBlank(poolId)) {
            CluePool defaultPool = cluePoolService.getDefaultPool(orgId);
            if (defaultPool != null) {
                return defaultPool;
            }
            throw new GenericException(Translator.get("marketing.form.target.pool.invalid"));
        }
        CluePool pool = cluePoolMapper.selectByPrimaryKey(poolId);
        if (pool == null || !orgId.equals(pool.getOrganizationId()) || !Boolean.TRUE.equals(pool.getEnable())) {
            throw new GenericException(Translator.get("marketing.form.target.pool.invalid"));
        }
        return pool;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseFieldMapping(String fieldMappingJson) {
        if (StringUtils.isBlank(fieldMappingJson)) {
            return new HashMap<>();
        }
        return JSON.parseObject(fieldMappingJson, Map.class);
    }

    /**
     * 把 EAV 格式的 moduleFields (List<{fieldId, fieldValue}>) 转成 fieldId→value 映射。
     */
    private Map<String, Object> extractFormValues(List<BaseModuleFieldValue> moduleFields) {
        Map<String, Object> values = new HashMap<>();
        if (CollectionUtils.isEmpty(moduleFields)) {
            return values;
        }
        for (BaseModuleFieldValue fv : moduleFields) {
            if (StringUtils.isNotBlank(fv.getFieldId())) {
                values.put(fv.getFieldId(), fv.getFieldValue());
            }
        }
        return values;
    }

    /**
     * 构建线索。按 field_mapping 把表单值映射到 Clue 字段;
     * 未映射的值不丢失 (已在 saveModuleField 存进 clue_field EAV)。
     */
    private Clue buildClue(MarketingForm form, String poolId, Map<String, String> fieldMapping,
                           Map<String, Object> formValues, String orgId, String operatorId) {
        Clue clue = new Clue();
        clue.setId(IDGenerator.nextStr());
        clue.setOrganizationId(orgId);
        clue.setStage(ClueStatus.NEW.name());
        // 市场表单回流线索为普通线索(不放入线索池), 始终显示在「线索」列表。
        // 线索池仅作为分配规则使用, 通过 matchAndAssignOwner 为其设置负责人。
        clue.setInSharedPool(false);
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(System.currentTimeMillis());
        clue.setCreateUser(operatorId);
        clue.setUpdateUser(operatorId);
        // 关联来源市场活动
        clue.setMarketingEventId(form.getId());
        // 来源: 自动关联到市场表单的名字, 无需用户填写 (ensureSourceOption 已把表单名加入来源选项, 保证正常显示)
        clue.setSource(form.getName());
        // ============ 市场表单回流默认值 (即使表单未映射也自动填入) ============
        // 注意: 必须存枚举 key (如 "NEW"/"ACTIVE"), 不能存中文 label。
        // 前端用 optionMap(id=枚举key) 匹配显示 label, 存中文会导致列表显示"选项不存在"。
        clue.setLeadsStage(LeadsStageEnum.NEW.getKey());     // 线索进度 (列表显示)
        clue.setBizStatus(BizStatusEnum.NEW.getKey());       // 线索状态
        clue.setLifeStatus(LifeStatusEnum.ACTIVE.getKey());  // 生命状态

        applyFormToClue(clue, form, fieldMapping, formValues);

        // name 是必填, 若映射未覆盖则用活动名 + 时间戳兜底
        if (StringUtils.isBlank(clue.getName())) {
            clue.setName(form.getName() + "-" + System.currentTimeMillis());
        }

        return clue;
    }

    /** 按 field_mapping 把表单值填充到线索固定列 (仅覆盖非空值) */
    private void applyFormToClue(Clue clue, MarketingForm form, Map<String, String> fieldMapping,
                                 Map<String, Object> formValues) {
        // fieldMapping: {表单字段fieldId: clue字段名(camelCase)}
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String fieldId = entry.getKey();
            String clueField = entry.getValue();
            Object value = formValues.get(fieldId);
            if (value == null) {
                continue;
            }
            applyClueField(clue, clueField, value);
        }
    }

    /**
     * 按 camelCase 字段名反射设置 Clue 属性。
     * 支持常见标量字段; products(意向产品) 为列表字段需单独处理。
     * 其余对象/未知字段跳过 —— 误配时避免把对象值塞进主表列。
     */
    private void applyClueField(Clue clue, String fieldName, Object value) {
        try {
            if ("products".equals(fieldName)) {
                clue.setProducts(coerceStringList(value));
                return;
            }
            if (value instanceof List || value instanceof Map) {
                log.debug("跳过非标量值映射到 Clue 列 {}: 值类型 {}", fieldName, value.getClass().getSimpleName());
                return;
            }
            String strValue = value.toString();
            switch (fieldName) {
                case "name" -> clue.setName(strValue);
                case "contact" -> clue.setContact(strValue);
                case "phone" -> clue.setPhone(strValue);
                case "tel" -> clue.setTel(strValue);
                case "mobile" -> clue.setMobile(strValue);
                case "email" -> clue.setEmail(strValue);
                case "company" -> clue.setCompany(strValue);
                case "department" -> clue.setDepartment(strValue);
                case "jobTitle" -> clue.setJobTitle(strValue);
                case "address" -> clue.setAddress(strValue);
                case "url" -> clue.setUrl(strValue);
                case "source" -> clue.setSource(strValue);
                case "owner" -> clue.setOwner(strValue);
                case "leadsStage" -> clue.setLeadsStage(normalizeEnumKey(strValue,
                        LeadsStageEnum::getNameByKey, LeadsStageEnum::getKeyByName, LeadsStageEnum.NEW.getKey()));
                case "bizStatus" -> clue.setBizStatus(normalizeEnumKey(strValue,
                        BizStatusEnum::getNameByKey, BizStatusEnum::getKeyByName, BizStatusEnum.NEW.getKey()));
                case "lifeStatus" -> clue.setLifeStatus(normalizeEnumKey(strValue,
                        LifeStatusEnum::getNameByKey, LifeStatusEnum::getKeyByName, LifeStatusEnum.ACTIVE.getKey()));
                case "remark" -> clue.setRemark(strValue);
                default -> log.debug("字段映射 {} 未在 applyClueField 白名单中, 跳过 (可走 EAV)", fieldName);
            }
        } catch (Exception e) {
            log.warn("设置 Clue 字段 {} 失败: {}", fieldName, e.getMessage());
        }
    }

    /**
     * 归一化枚举字段值: 兼容"已是 key"(如 NEW) 与"中文 label"(如 新线索) 两种入参, 统一返回枚举 key。
     * 表单/市场表单可能直接把中文 label 映射进来, 若不转回 key, 前端 optionMap(id=key) 匹配不上而显示"选项不存在"。
     *
     * @param value      外部传入值
     * @param keyToName  枚举 key -> name (若入参是合法 key 则返回非 null)
     * @param nameToKey  name -> key 反查
     * @param defaultKey 无法识别时的兜底 key
     */
    private static String normalizeEnumKey(String value,
                                           java.util.function.Function<String, String> keyToName,
                                           java.util.function.Function<String, String> nameToKey,
                                           String defaultKey) {
        if (StringUtils.isBlank(value)) {
            return defaultKey;
        }
        String v = value.trim();
        // 已是合法 key (如 "NEW")
        if (keyToName.apply(v) != null) {
            return v;
        }
        // 是中文 label (如 "新线索") 则反查 key
        String key = nameToKey.apply(v);
        return key != null ? key : defaultKey;
    }

    /** 把任意值转为字符串列表 (List 直取, 逗号分隔字符串拆分, 标量包装), 供 products 等列表字段使用 */
    @SuppressWarnings("unchecked")
    private List<String> coerceStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new java.util.ArrayList<>(list.size());
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        if (value instanceof String s && StringUtils.isNotBlank(s)) {
            return java.util.Arrays.stream(s.split(",")).map(String::trim)
                    .filter(StringUtils::isNotBlank).toList();
        }
        return null;
    }

    private void recordSubmission(String formId, String clueId, String orgId, HttpServletRequest httpRequest,
                                  String deviceId, Identity identity, String dedupAction) {
        MarketingFormSubmission submission = new MarketingFormSubmission();
        submission.setId(IDGenerator.nextStr());
        submission.setMarketingFormId(formId);
        submission.setClueId(clueId);
        submission.setSubmitTime(System.currentTimeMillis());
        submission.setSubmitIp(getClientIp(httpRequest));
        submission.setSubmitDevice(StringUtils.trimToNull(deviceId));
        submission.setIdentityKey(identity != null ? identity.keyType() : null);
        submission.setIdentityValue(identity != null ? identity.value() : null);
        submission.setDedupAction(dedupAction);
        submission.setOrganizationId(orgId);
        marketingFormSubmissionMapper.insert(submission);
    }

    /**
     * 构建并发去重指纹: formId:key:value。
     * 幂等唯一, 配合 clue.dedup_fingerprint 唯一索引实现并发安全去重。
     */
    private String buildFingerprint(String formId, Identity identity) {
        return formId + ":" + identity.keyType() + ":" + identity.value();
    }

    /**
     * 并发去重兜底: 按 token + 请求重新解析身份, 查已存在的线索ID (独立于创建事务)。
     * 供 controller 在 clue.dedup_fingerprint 唯一索引冲突时复用已有线索, 避免重复进池。
     */
    public String resolveExistingClueId(String token, MarketingFormSubmitRequest request,
                                        HttpServletRequest httpRequest) {
        MarketingForm form = marketingFormService.getByToken(token);
        if (form == null) {
            return null;
        }
        CluePool pool = validateTargetPool(form.getTargetPoolId(), form.getOrganizationId());
        DedupConfig config = resolveDedupConfig(form, pool);
        Map<String, String> fieldMapping = parseFieldMapping(form.getFieldMapping());
        Map<String, Object> formValues = extractFormValues(request.getModuleFields());
        Identity identity = resolveIdentity(config, fieldMapping, formValues, request, httpRequest);
        if (identity == null || StringUtils.isBlank(identity.value())) {
            return null;
        }
        String fingerprint = buildFingerprint(form.getId(), identity);
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clue::getDedupFingerprint, fingerprint);
        List<Clue> list = clueMapper.selectListByLambda(wrapper);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    /**
     * 确保线索「来源」字段的选项包含当前市场表单名。
     * 使线索来源能自动关联到表单名 (source=form.name), 前端列表/详情能正常显示 label。
     * 幂等: 选项已包含则跳过; 表单名变更/新表单会自动追加。
     */
    @SuppressWarnings("unchecked")
    private void ensureSourceOption(MarketingForm form, String orgId) {
        if (form == null || StringUtils.isBlank(form.getName())) {
            return;
        }
        String sourceValue = form.getName();
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CLUE.getKey(), orgId);
            BaseField sourceField = fields.stream()
                    .filter(f -> "source".equals(f.getInternalKey()))
                    .findFirst().orElse(null);
            if (sourceField == null) {
                return;
            }
            ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(sourceField.getId());
            if (blob == null) {
                return;
            }
            Map<String, Object> prop = JSON.parseObject(blob.getProp(), Map.class);
            List<OptionProp> options = new ArrayList<>();
            Object rawOptions = prop.get("options");
            if (rawOptions instanceof List<?> list) {
                options = new ArrayList<>(JSON.parseArray(JSON.toJSONString(list), OptionProp.class));
            }
            boolean exists = options.stream().anyMatch(o -> sourceValue.equals(String.valueOf(o.getValue())));
            if (exists) {
                return;
            }
            options.add(new OptionProp(sourceValue, sourceValue));
            prop.put("options", options);
            blob.setProp(JSON.toJSONString(prop));
            moduleFieldBlobMapper.updateById(blob);
            log.info("市场表单 {} 已把来源选项 {} 加入线索来源字段", form.getId(), sourceValue);
        } catch (Exception e) {
            log.warn("同步来源选项失败, 来源可能显示为选项不存在: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
