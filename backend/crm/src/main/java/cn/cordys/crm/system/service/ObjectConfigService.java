package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.system.domain.ObjectConfig;
import cn.cordys.crm.system.dto.request.ObjectConfigRenameRequest;
import cn.cordys.crm.system.dto.response.ObjectConfigListResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对象配置管理 - 统一管理预设对象和自定义对象
 */
@Service
public class ObjectConfigService {

    /**
     * 预设对象的默认名称(中文)
     */
    private static final Map<String, String> PRESET_DEFAULT_NAMES = new LinkedHashMap<>();

    static {
        PRESET_DEFAULT_NAMES.put(FormKey.CLUE.getKey(), "线索");
        PRESET_DEFAULT_NAMES.put(FormKey.CUSTOMER.getKey(), "客户");
        PRESET_DEFAULT_NAMES.put(FormKey.CONTACT.getKey(), "联系人");
        PRESET_DEFAULT_NAMES.put(FormKey.FOLLOW_RECORD.getKey(), "跟进记录");
        PRESET_DEFAULT_NAMES.put(FormKey.FOLLOW_PLAN.getKey(), "跟进计划");
        PRESET_DEFAULT_NAMES.put(FormKey.OPPORTUNITY.getKey(), "商机");
        PRESET_DEFAULT_NAMES.put(FormKey.PRODUCT.getKey(), "产品");
        PRESET_DEFAULT_NAMES.put(FormKey.PRICE.getKey(), "价格");
        PRESET_DEFAULT_NAMES.put(FormKey.QUOTATION.getKey(), "报价单");
        PRESET_DEFAULT_NAMES.put(FormKey.CONTRACT.getKey(), "合同");
        PRESET_DEFAULT_NAMES.put(FormKey.INVOICE.getKey(), "发票");
        PRESET_DEFAULT_NAMES.put(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "回款计划");
        PRESET_DEFAULT_NAMES.put(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), "回款记录");
        PRESET_DEFAULT_NAMES.put(FormKey.ORDER.getKey(), "订单");
    }

    @Resource
    private BaseMapper<ObjectConfig> objectConfigMapper;

    @Resource
    private BaseMapper<CustomForm> customFormMapper;

    /**
     * 获取统一对象列表(预设 + 自定义)
     */
    public List<ObjectConfigListResponse> list(String orgId) {
        // 1. 查询当前组织的对象配置(自定义名称)
        List<ObjectConfig> objectConfigs = getObjectConfigs(orgId);
        Map<String, ObjectConfig> configMap = objectConfigs.stream()
                .collect(Collectors.toMap(ObjectConfig::getFormKey, c -> c, (a, b) -> a));

        List<ObjectConfigListResponse> result = new ArrayList<>();

        // 2. 预设对象
        for (FormKey formKey : FormKey.values()) {
            String key = formKey.getKey();
            String defaultName = PRESET_DEFAULT_NAMES.getOrDefault(key, key);
            ObjectConfig config = configMap.get(key);

            ObjectConfigListResponse resp = new ObjectConfigListResponse();
            resp.setId(config != null ? config.getId() : null);
            resp.setKey(key);
            resp.setName(config != null && StringUtils.isNotBlank(config.getCustomName())
                    ? config.getCustomName() : defaultName);
            resp.setDefaultName(defaultName);
            resp.setType("PRESET");
            resp.setEnable(config != null ? config.getEnable() : true);
            resp.setDeletable(false);
            resp.setFormId(null);
            result.add(resp);
        }

        // 3. 自定义对象
        List<CustomForm> customForms = getCustomForms();
        for (CustomForm form : customForms) {
            ObjectConfig config = configMap.get(form.getId());

            ObjectConfigListResponse resp = new ObjectConfigListResponse();
            resp.setId(config != null ? config.getId() : null);
            resp.setKey(form.getId());
            resp.setName(config != null && StringUtils.isNotBlank(config.getCustomName())
                    ? config.getCustomName() : form.getName());
            resp.setDefaultName(form.getName());
            resp.setType("CUSTOM");
            resp.setEnable(form.getEnable());
            resp.setDeletable(true);
            resp.setFormId(form.getId());
            result.add(resp);
        }

        return result;
    }

    /**
     * 重命名对象(仅修改自定义名称)
     */
    @Transactional(rollbackFor = Exception.class)
    public void rename(ObjectConfigRenameRequest request, String userId, String orgId) {
        String key = request.getKey();
        String name = request.getName();

        ObjectConfig existing = getConfigByKey(key, orgId);

        if (existing != null) {
            ObjectConfig update = new ObjectConfig();
            update.setId(existing.getId());
            update.setCustomName(name);
            update.setUpdateTime(System.currentTimeMillis());
            update.setUpdateUser(userId);
            objectConfigMapper.update(update);
        } else {
            ObjectConfig config = new ObjectConfig();
            config.setId(IDGenerator.nextStr());
            config.setFormKey(key);
            config.setCustomName(name);
            config.setObjectType(isPresetKey(key) ? "PRESET" : "CUSTOM");
            config.setEnable(true);
            config.setOrganizationId(orgId);
            config.setCreateTime(System.currentTimeMillis());
            config.setUpdateTime(System.currentTimeMillis());
            config.setCreateUser(userId);
            config.setUpdateUser(userId);
            objectConfigMapper.insert(config);
        }
    }

    /**
     * 启用/禁用对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void switchEnable(String key, String userId, String orgId) {
        ObjectConfig existing = getConfigByKey(key, orgId);

        if (existing != null) {
            ObjectConfig update = new ObjectConfig();
            update.setId(existing.getId());
            update.setEnable(!existing.getEnable());
            update.setUpdateTime(System.currentTimeMillis());
            update.setUpdateUser(userId);
            objectConfigMapper.update(update);
        } else {
            ObjectConfig config = new ObjectConfig();
            config.setId(IDGenerator.nextStr());
            config.setFormKey(key);
            config.setObjectType(isPresetKey(key) ? "PRESET" : "CUSTOM");
            config.setEnable(false);
            config.setOrganizationId(orgId);
            config.setCreateTime(System.currentTimeMillis());
            config.setUpdateTime(System.currentTimeMillis());
            config.setCreateUser(userId);
            config.setUpdateUser(userId);
            objectConfigMapper.insert(config);
        }
    }

    /**
     * 获取对象的自定义名称(供其他模块使用)
     */
    public String getObjectName(String key, String orgId) {
        ObjectConfig config = getConfigByKey(key, orgId);
        if (config != null && StringUtils.isNotBlank(config.getCustomName())) {
            return config.getCustomName();
        }
        return PRESET_DEFAULT_NAMES.getOrDefault(key, key);
    }

    /**
     * 批量获取所有对象名称(供前端缓存使用)
     * @return Map<formKey, displayName>
     */
    public Map<String, String> getObjectNameMap(String orgId) {
        List<ObjectConfig> configs = getObjectConfigs(orgId);
        Map<String, String> nameMap = new HashMap<>();
        
        // 预设对象
        for (FormKey formKey : FormKey.values()) {
            String key = formKey.getKey();
            String defaultName = PRESET_DEFAULT_NAMES.getOrDefault(key, key);
            nameMap.put(key, defaultName);
        }
        
        // 自定义对象
        List<CustomForm> customForms = getCustomForms();
        for (CustomForm form : customForms) {
            nameMap.put(form.getId(), form.getName());
        }
        
        // 覆盖自定义名称
        for (ObjectConfig config : configs) {
            if (StringUtils.isNotBlank(config.getCustomName())) {
                nameMap.put(config.getFormKey(), config.getCustomName());
            }
        }
        
        return nameMap;
    }

    private ObjectConfig getConfigByKey(String key, String orgId) {
        LambdaQueryWrapper<ObjectConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectConfig::getFormKey, key)
                .eq(ObjectConfig::getOrganizationId, orgId);
        List<ObjectConfig> configs = objectConfigMapper.selectListByLambda(wrapper);
        return configs.isEmpty() ? null : configs.getFirst();
    }

    private List<ObjectConfig> getObjectConfigs(String orgId) {
        LambdaQueryWrapper<ObjectConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectConfig::getOrganizationId, orgId);
        return objectConfigMapper.selectListByLambda(wrapper);
    }

    private List<CustomForm> getCustomForms() {
        return customFormMapper.selectAll(null);
    }

    private boolean isPresetKey(String key) {
        return FormKey.ofKey(key) != null;
    }
}
