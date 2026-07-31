package cn.cordys.crm.marketingform.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.clue.service.CluePoolAssignRuleService;
import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.marketingform.domain.MarketingFormSubmission;
import cn.cordys.crm.marketingform.dto.request.MarketingFormSubmitRequest;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单→线索桥接服务。意向客户通过二维码公开提交表单后, 本服务把表单字段值映射成 Clue,
 * 按 PoolClueService.realImport 的 ADD 模式直接插入线索池 (inSharedPool=true, poolId=目标池)。
 * 无需登录上下文: orgId 从 marketing_form 记录解析, userId 用 InternalUser.ADMIN。
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class MarketingLeadBridgeService {

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
    private CluePoolAssignRuleService cluePoolAssignRuleService;
    @Resource
    private LogService logService;

    /**
     * 处理公开提交: 表单值 → 创建线索进池 → (可选)触发自动分配。
     *
     * @param token   二维码令牌
     * @param request 提交请求 (含 moduleFields)
     * @param httpRequest HTTP 请求 (取 IP)
     * @return 创建的线索 ID
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

            // 4. 解析字段映射 + 提取表单值
            Map<String, String> fieldMapping = parseFieldMapping(form.getFieldMapping());
            Map<String, Object> formValues = extractFormValues(request.getModuleFields());

            // 5. 构建 Clue (镜像 PoolClueService.realImport ADD 分支)
            Clue clue = buildClue(form, fieldMapping, formValues, orgId, operatorId);
            clueMapper.insert(clue);

            // 6. 保存自定义字段值 (走 clue_field EAV)
            if (CollectionUtils.isNotEmpty(request.getModuleFields())) {
                clueFieldService.saveModuleField(clue, orgId, operatorId, request.getModuleFields(), false);
            }

            // 7. 记日志
            logService.add(new LogDTO(orgId, clue.getId(), operatorId, LogType.ADD, LogModule.CLUE_INDEX, clue.getName()));

            // 8. 记提交留痕
            recordSubmission(form.getId(), clue.getId(), orgId, httpRequest);

            // 9. 触发线索池→销售自动分配 (失败不阻断, 线索留在池里)
            try {
                cluePoolAssignRuleService.matchAndAssign(clue.getId(), pool.getId(), orgId, operatorId);
            } catch (Exception e) {
                log.warn("线索池自动分配失败, 线索 {} 留在池 {} 中: {}", clue.getId(), pool.getId(), e.getMessage());
            }

            return clue.getId();
        } finally {
            OrganizationContext.clear();
        }
    }

    private CluePool validateTargetPool(String poolId, String orgId) {
        if (StringUtils.isBlank(poolId)) {
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
    private Clue buildClue(MarketingForm form, Map<String, String> fieldMapping,
                           Map<String, Object> formValues, String orgId, String operatorId) {
        Clue clue = new Clue();
        clue.setId(IDGenerator.nextStr());
        clue.setOrganizationId(orgId);
        clue.setStage(ClueStatus.NEW.name());
        clue.setInSharedPool(true);
        clue.setPoolId(form.getTargetPoolId());
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(System.currentTimeMillis());
        clue.setCreateUser(operatorId);
        clue.setUpdateUser(operatorId);
        // 关联来源市场活动 (我已加的字段)
        clue.setMarketingEventId(form.getId());
        // 来源标记
        clue.setSource("MARKETING_FORM");

        // 按映射规则把表单值填到 Clue 固定列字段
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

        // name 是必填, 若映射未覆盖则用活动名 + 时间戳兜底
        if (StringUtils.isBlank(clue.getName())) {
            clue.setName(form.getName() + "-" + System.currentTimeMillis());
        }

        return clue;
    }

    /**
     * 按 camelCase 字段名反射设置 Clue 属性。只支持常用字符串/数值字段。
     */
    private void applyClueField(Clue clue, String fieldName, Object value) {
        String strValue = value.toString();
        try {
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
                case "leadsStage" -> clue.setLeadsStage(strValue);
                case "bizStatus" -> clue.setBizStatus(strValue);
                case "lifeStatus" -> clue.setLifeStatus(strValue);
                case "remark" -> clue.setRemark(strValue);
                default -> log.debug("字段映射 {} 未在 applyClueField 白名单中, 跳过 (可走 EAV)", fieldName);
            }
        } catch (Exception e) {
            log.warn("设置 Clue 字段 {} 失败: {}", fieldName, e.getMessage());
        }
    }

    private void recordSubmission(String formId, String clueId, String orgId, HttpServletRequest httpRequest) {
        MarketingFormSubmission submission = new MarketingFormSubmission();
        submission.setId(IDGenerator.nextStr());
        submission.setMarketingFormId(formId);
        submission.setClueId(clueId);
        submission.setSubmitTime(System.currentTimeMillis());
        submission.setSubmitIp(getClientIp(httpRequest));
        submission.setOrganizationId(orgId);
        marketingFormSubmissionMapper.insert(submission);
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
