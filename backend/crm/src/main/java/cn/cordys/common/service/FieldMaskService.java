package cn.cordys.common.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.RolePermissionDTO;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.utils.FieldMaskUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.mapper.ExtRoleFieldMaskMapper;
import cn.cordys.crm.system.domain.RoleFieldMask;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字段级脱敏执行引擎
 * <p>
 * 在业务 Service 层填充 moduleFields 之后调用，对配置了脱敏的字段值进行打码。
 * admin 用户及拥有 ALL 数据权限的角色豁免(看明文)。
 * </p>
 */
@Service
public class FieldMaskService {

    @Resource
    private ExtRoleFieldMaskMapper extRoleFieldMaskMapper;

    @Resource
    private PermissionCache permissionCache;

    /**
     * 对自定义字段列表(moduleFields)执行脱敏
     *
     * @param moduleFields 字段值列表
     * @param moduleKey    模块key(customer/opportunity/order/contact)
     */
    public void maskModuleFields(List<BaseModuleFieldValue> moduleFields, String moduleKey) {
        if (CollectionUtils.isEmpty(moduleFields)) {
            return;
        }
        if (shouldMask()) {
            return;
        }
        // 获取当前用户所有角色在该模块需脱敏的 fieldId → fieldType 映射
        Set<String> roleIds = getCurrentRoleIds();
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<RoleFieldMask> masks = extRoleFieldMaskMapper.selectByRoleIdsAndModule(
                List.copyOf(roleIds), moduleKey);
        if (CollectionUtils.isEmpty(masks)) {
            return;
        }
        // fieldId → fieldType 映射(多角色取并集)
        java.util.Map<String, String> fieldIdToType = masks.stream()
                .filter(m -> m.getFieldId() != null)
                .collect(Collectors.toMap(
                        RoleFieldMask::getFieldId,
                        RoleFieldMask::getFieldType,
                        (a, b) -> a));
        // 遍历字段值进行脱敏
        for (BaseModuleFieldValue fv : moduleFields) {
            String fieldType = fieldIdToType.get(fv.getFieldId());
            if (fieldType != null && fv.getFieldValue() != null) {
                fv.setFieldValue(maskObject(fieldType, fv.getFieldValue()));
            }
        }
    }

    /**
     * 对内置字段(如联系人 phone)执行脱敏
     *
     * @param value     原始值
     * @param fieldKey  字段key(如 "phone")
     * @param moduleKey 模块key(如 "contact")
     * @return 脱敏后的值
     */
    public String maskBuiltinField(String value, String fieldKey, String moduleKey) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (shouldMask()) {
            return value;
        }
        Set<String> roleIds = getCurrentRoleIds();
        if (CollectionUtils.isEmpty(roleIds)) {
            return value;
        }
        List<RoleFieldMask> masks = extRoleFieldMaskMapper.selectByRoleIdsAndModule(
                List.copyOf(roleIds), moduleKey);
        if (CollectionUtils.isEmpty(masks)) {
            return value;
        }
        // 查找匹配 fieldKey 的配置
        for (RoleFieldMask mask : masks) {
            if (Strings.CI.equals(mask.getFieldKey(), fieldKey)) {
                return (String) FieldMaskUtils.maskValue(mask.getFieldType(), value);
            }
        }
        return value;
    }

    /**
     * 判断当前用户是否需要脱敏
     * <p>
     * admin 及拥有 ALL 数据权限的角色返回 true(豁免,即 shouldMask=true 表示"应该跳过脱敏")。
     * 注意：方法名 shouldSkipMask 更准确，但为保持调用处可读性用 shouldMask。
     * 返回 true = 跳过脱敏(看明文)；false = 需要脱敏。
     * </p>
     */
    private boolean shouldMask() {
        String userId = SessionUtils.getUserId();
        // admin 豁免
        if (Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
            return true;
        }
        String orgId = OrganizationContext.getOrganizationId();
        List<RolePermissionDTO> roles = permissionCache.getRolePermissions(userId, orgId);
        if (CollectionUtils.isEmpty(roles)) {
            return false;
        }
        // 任一角色 dataScope=ALL 则豁免
        for (RolePermissionDTO role : roles) {
            if (role.getDataScope() != null && Strings.CI.equals(role.getDataScope(), RoleDataScope.ALL.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前用户的角色ID集合
     */
    private Set<String> getCurrentRoleIds() {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        List<RolePermissionDTO> roles = permissionCache.getRolePermissions(userId, orgId);
        if (CollectionUtils.isEmpty(roles)) {
            return new HashSet<>();
        }
        return roles.stream()
                .map(RolePermissionDTO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 对字段值执行脱敏(处理 String 和 List 两种情况)
     */
    @SuppressWarnings("unchecked")
    private Object maskObject(String fieldType, Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> item != null ? FieldMaskUtils.maskValue(fieldType, item.toString()) : null)
                    .collect(Collectors.toList());
        }
        return FieldMaskUtils.maskValue(fieldType, value);
    }
}
