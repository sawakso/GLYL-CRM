package cn.cordys.crm.system.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.RoleFieldMask;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtRoleFieldMaskMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色字段脱敏配置服务
 * <p>
 * 管理按 角色×模块×字段 维度的脱敏配置，提供配置的增删改查与缓存。
 * 配置保存时先删后插(参考 SearchFieldMaskConfigService 的模式)。
 * </p>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FieldMaskConfigService {

    @Resource
    private ExtRoleFieldMaskMapper extRoleFieldMaskMapper;

    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    /**
     * 保存角色的字段脱敏配置(先删后插)
     *
     * @param roleId  角色ID
     * @param masks   脱敏配置列表
     * @param userId  操作人
     */
    @CacheEvict(value = "field_mask_cache", allEntries = true)
    public void saveMasks(String roleId, List<RoleFieldMask> masks, String userId) {
        extRoleFieldMaskMapper.deleteByRoleId(roleId);
        if (CollectionUtils.isEmpty(masks)) {
            return;
        }
        long now = System.currentTimeMillis();
        List<RoleFieldMask> toInsert = new ArrayList<>();
        for (int i = 0; i < masks.size(); i++) {
            RoleFieldMask mask = masks.get(i);
            mask.setId(IDGenerator.nextStr());
            mask.setRoleId(roleId);
            mask.setCreateUser(userId);
            mask.setUpdateUser(userId);
            mask.setCreateTime(now);
            mask.setUpdateTime(now);
            mask.setPos((long) i);
            toInsert.add(mask);
        }
        batchInsert(toInsert);
    }

    /**
     * 查询角色的全部脱敏配置(回显用)
     */
    public List<RoleFieldMask> getMasksByRole(String roleId) {
        return extRoleFieldMaskMapper.selectByRoleId(roleId);
    }

    /**
     * 查询角色在某模块需脱敏的自定义字段 fieldId 集合(带缓存)
     *
     * @param roleId    角色ID
     * @param moduleKey 模块key(customer/opportunity/order/contact)
     * @return 需脱敏的 fieldId 集合，空集表示无需脱敏
     */
    @Cacheable(value = "field_mask_cache", key = "#roleId + ':' + #moduleKey")
    public Set<String> getMaskFieldIds(String roleId, String moduleKey) {
        List<RoleFieldMask> masks = extRoleFieldMaskMapper.selectByRoleIdAndModule(roleId, moduleKey);
        if (CollectionUtils.isEmpty(masks)) {
            return Collections.emptySet();
        }
        return masks.stream()
                .filter(m -> m.getFieldId() != null)
                .map(RoleFieldMask::getFieldId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 查询角色在某模块需脱敏的内置字段 key 集合(如联系人 phone)
     */
    @Cacheable(value = "field_mask_cache", key = "#roleId + ':' + #moduleKey + ':builtin'")
    public Set<String> getMaskFieldKeys(String roleId, String moduleKey) {
        List<RoleFieldMask> masks = extRoleFieldMaskMapper.selectByRoleIdAndModule(roleId, moduleKey);
        if (CollectionUtils.isEmpty(masks)) {
            return Collections.emptySet();
        }
        return masks.stream()
                .filter(m -> m.getFieldKey() != null)
                .map(RoleFieldMask::getFieldKey)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 获取某模块可配置脱敏的字段列表(从表单配置获取)
     *
     * @param formKey      表单key(customer/opportunity/order/contact)
     * @param organizationId 组织ID
     * @return 可脱敏字段列表(仅包含 PHONE/INPUT/DATA_SOURCE/SERIAL_NUMBER 类型)
     */
    public List<BaseField> getMaskableFields(String formKey, String organizationId) {
        ModuleFormConfigDTO config = moduleFormCacheService.getBusinessFormConfig(formKey, organizationId);
        if (config == null || CollectionUtils.isEmpty(config.getFields())) {
            return Collections.emptyList();
        }
        // 只返回可脱敏的字段类型
        Set<String> maskableTypes = Set.of("PHONE", "INPUT", "DATA_SOURCE", "SERIAL_NUMBER", "INPUT_MULTIPLE");
        return config.getFields().stream()
                .filter(f -> f.getType() != null && maskableTypes.contains(f.getType().toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * 批量插入(使用 BaseMapper)
     */
    private void batchInsert(List<RoleFieldMask> masks) {
        // ExtRoleFieldMaskMapper 没有批量插入方法，逐条插入
        // 数据量不大(每角色每模块通常几十条)，逐条插入可接受
        for (RoleFieldMask mask : masks) {
            insertOne(mask);
        }
    }

    @Resource
    private cn.cordys.mybatis.BaseMapper<RoleFieldMask> roleFieldMaskMapper;

    private void insertOne(RoleFieldMask mask) {
        roleFieldMaskMapper.insert(mask);
    }
}
