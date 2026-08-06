package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.approval.constants.ApprovalResourceUpdateType;
import cn.cordys.crm.approval.dto.ResourceApprovalFieldUpdateParam;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.handler.ApprovalResourceHandler;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.ClueField;
import cn.cordys.crm.clue.domain.ClueFieldBlob;
import cn.cordys.crm.clue.dto.request.ClueUpdateRequest;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 线索审批资源处理器
 * <p>
 * 使线索（CLUE）提审后的审批通过/驳回能够真正写回线索：
 * 审批状态更新、编辑前快照捕获（驳回/撤回回退）、后置字段应用、删除审批。
 * <p>
 * 注：FormKey.CLUE 的 hasSnapshot() 为 false，故 updateSnapshotApprovalStatus 不会被审批框架自动调用，
 * 线索审批状态由 ApprovalResourceService.updateResourceApprovalStatus 直接写入 clue.approval_status 列。
 * 此处仍实现该方法以便后续若开启快照能力时无缝衔接。
 */
@Slf4j
@Service
public class ClueApprovalResourceHandler implements ApprovalResourceHandler {

    @Resource
    private BaseMapper<Clue> clueMapper;

    @Resource
    private ClueService clueService;

    @Resource
    private ClueFieldService clueFieldService;

    @Resource
    private ModuleFormService moduleFormService;

    @Override
    public FormKey getFormKey() {
        return FormKey.CLUE;
    }

    @Override
    public void delete(String resourceId, String userId, String organizationId) {
        clueService.delete(resourceId, userId, organizationId);
    }

    @Override
    public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
        if (param == null || StringUtils.isBlank(param.getResourceId())) {
            return;
        }
        Clue clue = clueMapper.selectByPrimaryKey(param.getResourceId());
        if (clue == null) {
            return;
        }
        clue.setApprovalStatus(param.getApprovalStatus());
        clueMapper.update(clue);
    }

    @Override
    public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
        if (postFieldParam == null || CollectionUtils.isEmpty(postFieldParam.getFields())) {
            return;
        }
        Clue clue = clueMapper.selectByPrimaryKey(postFieldParam.getResourceId());
        if (clue == null) {
            return;
        }
        // 审批后置字段配置存的是字段 id，兼容老配置的 businessKey
        ModuleFormConfigDTO formConfig = clueService.getFormConfig(clue.getOrganizationId());
        Map<String, BaseField> fieldConfigMap = formConfig.getFields().stream()
                .collect(Collectors.toMap(BaseField::getId, f -> f, (a, b) -> a));
        Map<String, BaseField> businessKeyMap = formConfig.getFields().stream()
                .filter(f -> StringUtils.isNotBlank(f.getBusinessKey()))
                .collect(Collectors.toMap(BaseField::getBusinessKey, f -> f, (a, b) -> a));

        List<ClueField> fieldValues = new ArrayList<>();
        List<ClueFieldBlob> blobValues = new ArrayList<>();
        boolean changed = false;
        for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
            if (fieldUpdateParam == null || fieldUpdateParam.getFieldValue() == null) {
                continue;
            }
            String fieldId = fieldUpdateParam.getFieldId();
            BaseField fieldConfig = fieldConfigMap.get(fieldId);
            if (fieldConfig == null) {
                fieldConfig = businessKeyMap.get(fieldId);
            }
            if (fieldConfig == null) {
                log.debug("线索审批后置字段[{}]未匹配到表单配置，已跳过", fieldId);
                continue;
            }
            if (fieldConfig.hasBusinessKey()) {
                // 业务主表列字段：写入 Clue 实体，最后统一 updateById 落库
                clueFieldService.setResourceFieldValue(clue, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
                changed = true;
            } else {
                // 扩展字段（EAV）：clue_field / clue_field_blob 删除重建
                @SuppressWarnings("rawtypes")
                AbstractModuleFieldResolver resolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                String strValue = resolver == null ? String.valueOf(fieldUpdateParam.getFieldValue())
                        : resolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue());
                if (fieldConfig.isBlob()) {
                    clueFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<ClueFieldBlob>()
                            .eq(ClueFieldBlob::getFieldId, fieldId)
                            .eq(ClueFieldBlob::getResourceId, postFieldParam.getResourceId()));
                    ClueFieldBlob field = new ClueFieldBlob();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldId);
                    field.setFieldValue(strValue);
                    blobValues.add(field);
                } else {
                    clueFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<ClueField>()
                            .eq(ClueField::getFieldId, fieldId)
                            .eq(ClueField::getResourceId, postFieldParam.getResourceId()));
                    ClueField field = new ClueField();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldId);
                    field.setFieldValue(strValue);
                    fieldValues.add(field);
                }
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        clueMapper.updateById(clue);
        if (CollectionUtils.isNotEmpty(fieldValues)) {
            clueFieldService.getResourceFieldMapper().batchInsert(fieldValues);
        }
        if (CollectionUtils.isNotEmpty(blobValues)) {
            clueFieldService.getResourceFieldBlobMapper().batchInsert(blobValues);
        }
    }

    @Override
    public String getPreUpdateSnapshotData(String resourceId, String userId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(resourceId);
        if (clue == null) {
            return null;
        }
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(resourceId);
        ClueUpdateRequest snapshotReq = BeanUtils.copyBean(new ClueUpdateRequest(), clue);
        snapshotReq.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());
        ModuleFormConfigDTO formConfig = clueService.getFormConfig(clue.getOrganizationId());
        moduleFormService.processBusinessFieldValues(snapshotReq, clueFields, formConfig);
        return JSON.toJSONString(snapshotReq);
    }

    @Override
    public void revertToSnapshot(String resourceId, String userId, String orgId, String snapshotData) {
        try {
            ClueUpdateRequest request = JSON.parseObject(snapshotData, ClueUpdateRequest.class);
            if (request == null) {
                return;
            }
            // updateType=approval → HitApproval 切面放行，避免回退时再次触发审批
            request.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());
            clueService.update(request, userId, orgId);
        } catch (Exception e) {
            log.error("线索审批回退还原业务数据失败, resourceId:{}", resourceId, e);
        }
    }
}
