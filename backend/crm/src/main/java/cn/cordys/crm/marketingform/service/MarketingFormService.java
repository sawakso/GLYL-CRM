package cn.cordys.crm.marketingform.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.marketingform.domain.MarketingFormSubmission;
import cn.cordys.crm.marketingform.dto.request.MarketingFormAddRequest;
import cn.cordys.crm.marketingform.dto.request.MarketingFormUpdateRequest;
import cn.cordys.crm.marketingform.dto.response.MarketingFormGetResponse;
import cn.cordys.crm.marketingform.dto.response.MarketingFormListResponse;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigLogDTO;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 市场活动表单服务。复用共享表单引擎 (ModuleFormService 存字段定义, ModuleFormCacheService 存配置),
 * 本服务只管市场层元数据 (活动名/目标池/映射/dedup/qr_token/status)。
 * 镜像 CustomFormService 写法, 但去掉角色/管理员体系 (改用 @CsPermission 粗粒度权限)。
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class MarketingFormService {

    @Resource
    private BaseMapper<MarketingForm> marketingFormMapper;
    @Resource
    private BaseMapper<MarketingFormSubmission> marketingFormSubmissionMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleFormBlob> moduleFormBlobMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private ModuleFormService moduleFormService;

    @Value("classpath:form/form.json")
    private org.springframework.core.io.Resource formResource;

    @OperationLog(module = LogModule.MARKETING_FORM, type = LogType.ADD, resourceName = "{#request.name}")
    public MarketingForm create(MarketingFormAddRequest request, String userId, String orgId) {
        String formId = IDGenerator.nextStr();

        // 1. 保存 marketing_form 元数据
        MarketingForm form = new MarketingForm();
        form.setId(formId);
        form.setName(request.getName());
        form.setDescription(request.getDescription());
        form.setTargetPoolId(request.getTargetPoolId());
        form.setFieldMapping(request.getFieldMapping());
        form.setDedupStrategy(StringUtils.defaultIfBlank(request.getDedupStrategy(), "INHERIT"));
        form.setDedupWindow(request.getDedupWindow());
        form.setDedupKey(request.getDedupKey());
        form.setRequireName(Boolean.TRUE.equals(request.getRequireName()));
        form.setQrToken(generateQrToken());
        form.setStatus("DRAFT");
        form.setOrganizationId(orgId);
        form.setCreateTime(System.currentTimeMillis());
        form.setUpdateTime(System.currentTimeMillis());
        form.setCreateUser(userId);
        form.setUpdateUser(userId);
        marketingFormMapper.insert(form);

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(formId)
                        .modifiedValue(form)
                        .build()
        );

        // 2. 保存 sys_module_form (使用相同 ID, formKey 也用 formId, 与 CustomForm 一致)
        ModuleForm moduleForm = new ModuleForm();
        moduleForm.setId(formId);
        moduleForm.setFormKey(formId);
        moduleForm.setOrganizationId(orgId);
        moduleForm.setCreateTime(System.currentTimeMillis());
        moduleForm.setUpdateTime(System.currentTimeMillis());
        moduleForm.setCreateUser(userId);
        moduleForm.setUpdateUser(userId);
        moduleFormMapper.insert(moduleForm);

        // 3. 保存表单配置 (FormProp)
        ModuleFormBlob formBlob = new ModuleFormBlob();
        formBlob.setId(formId);
        FormProp formProp = getFormPropForCreate(request.getFormProp());
        formBlob.setProp(JSON.toJSONString(formProp));
        moduleFormBlobMapper.insert(formBlob);

        // 4. 校验并保存字段定义到共享 sys_module_field
        // 市场活动表单无固定的"名称+负责人"业务字段(字段通过 field_mapping 映射到线索),
        // 不能走 preCheckForFieldSave (其 custom-form 分支会强制要求 customFormDataName/customFormDataNOwner),
        // 故只做重名/重复选项校验。
        moduleFormService.preCheckForFieldSaveSkipBusiness(request.getFields());
        moduleFormService.saveFields(request.getFields(), formId, userId);

        return form;
    }

    public List<MarketingFormListResponse> list(String orgId) {
        LambdaQueryWrapper<MarketingForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingForm::getOrganizationId, orgId);
        wrapper.orderByDesc(MarketingForm::getCreateTime);
        List<MarketingForm> forms = marketingFormMapper.selectListByLambda(wrapper);

        return forms.stream().map(form -> {
            MarketingFormListResponse resp = BeanUtils.copyBean(new MarketingFormListResponse(), form);
            // 目标池名称
            if (StringUtils.isNotBlank(form.getTargetPoolId())) {
                CluePool pool = cluePoolMapper.selectByPrimaryKey(form.getTargetPoolId());
                if (pool != null) {
                    resp.setTargetPoolName(pool.getName());
                }
            }
            // 提交数
            LambdaQueryWrapper<MarketingFormSubmission> subWrapper = new LambdaQueryWrapper<>();
            subWrapper.eq(MarketingFormSubmission::getMarketingFormId, form.getId());
            Long count = (long) marketingFormSubmissionMapper.selectListByLambda(subWrapper).size();
            resp.setSubmissionCount(count);
            return resp;
        }).toList();
    }

    public MarketingFormGetResponse get(String id, String orgId) {
        MarketingForm form = marketingFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(Translator.get("marketing.form.not.exist"));
        }

        MarketingFormGetResponse resp = BeanUtils.copyBean(new MarketingFormGetResponse(), form);

        ModuleForm moduleForm = moduleFormMapper.selectByPrimaryKey(id);
        if (moduleForm != null) {
            ModuleFormConfigDTO businessFormConfig = moduleFormService.getBusinessFormConfig(moduleForm.getFormKey(), orgId);
            resp.setFields(businessFormConfig.getFields());
            resp.setFormProp(businessFormConfig.getFormProp());
        }

        if (StringUtils.isNotBlank(form.getTargetPoolId())) {
            CluePool pool = cluePoolMapper.selectByPrimaryKey(form.getTargetPoolId());
            if (pool != null) {
                resp.setTargetPoolName(pool.getName());
            }
        }
        return resp;
    }

    @OperationLog(module = LogModule.MARKETING_FORM, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void update(MarketingFormUpdateRequest request, String userId, String orgId) {
        MarketingForm originForm = marketingFormMapper.selectByPrimaryKey(request.getId());
        ModuleForm originModuleForm = moduleFormMapper.selectByPrimaryKey(request.getId());
        if (originForm == null || originModuleForm == null) {
            throw new GenericException(Translator.get("marketing.form.not.exist"));
        }

        // 更新市场元数据
        MarketingForm updateForm = new MarketingForm();
        updateForm.setId(request.getId());
        updateForm.setName(request.getName());
        updateForm.setDescription(request.getDescription());
        updateForm.setTargetPoolId(request.getTargetPoolId());
        updateForm.setFieldMapping(request.getFieldMapping());
        updateForm.setDedupStrategy(StringUtils.defaultIfBlank(request.getDedupStrategy(), "INHERIT"));
        updateForm.setDedupWindow(request.getDedupWindow());
        updateForm.setDedupKey(request.getDedupKey());
        updateForm.setRequireName(Boolean.TRUE.equals(request.getRequireName()));
        updateForm.setUpdateTime(System.currentTimeMillis());
        updateForm.setUpdateUser(userId);
        marketingFormMapper.update(updateForm);

        // 更新表单字段定义 (走共享 ModuleFormCacheService)
        ModuleFormSaveRequest moduleFormRequest = new ModuleFormSaveRequest();
        moduleFormRequest.setFormKey(originModuleForm.getFormKey());
        moduleFormRequest.setFormProp(request.getFormProp());
        moduleFormRequest.setFields(request.getFields());

        LogContextInfo formChangeLogContext = moduleFormService.getModuleFormChangeLogContext(originModuleForm.getFormKey(), orgId, moduleFormRequest);
        formChangeLogContext.setResourceName(request.getName());
        if (formChangeLogContext.getOriginalValue() instanceof ModuleFormConfigLogDTO originalLog) {
            originalLog.setName(originForm.getName());
        }
        if (formChangeLogContext.getModifiedValue() instanceof ModuleFormConfigLogDTO modifiedLog) {
            modifiedLog.setName(request.getName());
        }

        moduleFormService.saveWithoutLogSkipBusiness(moduleFormRequest, userId, orgId);
        OperationLogContext.setContext(formChangeLogContext);
    }

    @OperationLog(module = LogModule.MARKETING_FORM, type = LogType.UPDATE, resourceId = "{#id}")
    public void updateStatus(String id, String status, String userId) {
        MarketingForm form = marketingFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(Translator.get("marketing.form.not.exist"));
        }
        MarketingForm updateForm = new MarketingForm();
        updateForm.setId(id);
        updateForm.setStatus(status);
        updateForm.setUpdateTime(System.currentTimeMillis());
        updateForm.setUpdateUser(userId);
        marketingFormMapper.update(updateForm);
        updateForm.setName(form.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(form.getName())
                        .originalValue(form)
                        .modifiedValue(updateForm)
                        .build()
        );
    }

    @OperationLog(module = LogModule.MARKETING_FORM, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        MarketingForm form = marketingFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(Translator.get("marketing.form.not.exist"));
        }
        OperationLogContext.setResourceName(form.getName());

        // 删除提交记录及字段值
        LambdaQueryWrapper<MarketingFormSubmission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(MarketingFormSubmission::getMarketingFormId, id);
        marketingFormSubmissionMapper.deleteByLambda(subWrapper);

        marketingFormMapper.deleteByIds(List.of(id));
    }

    /**
     * 按 qr_token 查询市场表单 (公开端点用, 不需登录上下文)。
     */
    public MarketingForm getByToken(String token) {
        LambdaQueryWrapper<MarketingForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingForm::getQrToken, token);
        List<MarketingForm> forms = marketingFormMapper.selectListByLambda(wrapper);
        return forms.isEmpty() ? null : forms.get(0);
    }

    private String generateQrToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成公开页去重提示文案 (表单可继承线索池默认)。
     * 未启用去重 (NONE) 或配置缺失时返回 null。
     */
    public String buildDedupTip(MarketingForm form) {
        if (form == null) {
            return null;
        }
        String strategy = StringUtils.defaultIfBlank(form.getDedupStrategy(), "INHERIT");
        Integer window = form.getDedupWindow();
        String key = form.getDedupKey();
        if ("INHERIT".equalsIgnoreCase(strategy)) {
            CluePool pool = cluePoolMapper.selectByPrimaryKey(form.getTargetPoolId());
            if (pool != null) {
                strategy = StringUtils.defaultIfBlank(pool.getDedupStrategy(), "NONE");
                if (window == null) {
                    window = pool.getDedupWindow();
                }
                if (StringUtils.isBlank(key)) {
                    key = pool.getDedupKey();
                }
            } else {
                strategy = "NONE";
            }
        }
        if ("NONE".equalsIgnoreCase(strategy)) {
            return null;
        }
        int windowMinutes = window != null && window > 0 ? window : 5;
        String windowDesc = window != null && window > 0 ? windowMinutes + " 分钟内" : "";
        return switch (strategy.toUpperCase()) {
            case "UPDATE" -> "同一位客户" + windowDesc + "再次提交，将自动更新之前填写的线索";
            case "SKIP" -> "同一位客户" + windowDesc + "只能提交一次，重复提交将被忽略";
            case "MARK" -> "同一位客户" + windowDesc + "重复提交，将标记为疑似重复线索";
            default -> null;
        };
    }

    private FormProp getFormPropForCreate(FormProp createFormProp) {
        if (createFormProp == null) {
            try {
                return JSON.parseObject(formResource.getInputStream(), FormProp.class);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return new FormProp();
            }
        }
        return createFormProp;
    }
}
