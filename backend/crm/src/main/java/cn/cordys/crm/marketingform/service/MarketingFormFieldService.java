package cn.cordys.crm.marketingform.service;

import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.marketingform.domain.MarketingFormField;
import cn.cordys.crm.marketingform.domain.MarketingFormFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 市场表单字段值 EAV 服务。复用 BaseResourceFieldService 全套引擎 (校验/解析/序列化/读写),
 * 只需提供 formKey (ThreadLocal) + 两个 mapper。镜像 CustomFormDataFieldService 写法。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MarketingFormFieldService extends BaseResourceFieldService<MarketingFormField, MarketingFormFieldBlob> {

    private static final ThreadLocal<String> FORM_KEY_HOLDER = new ThreadLocal<>();

    @Resource
    private BaseMapper<MarketingFormField> marketingFormFieldMapper;
    @Resource
    private BaseMapper<MarketingFormFieldBlob> marketingFormFieldBlobMapper;

    public static void setFormKey(String formKey) {
        FORM_KEY_HOLDER.set(formKey);
    }

    public static void clearFormKey() {
        FORM_KEY_HOLDER.remove();
    }

    @Override
    protected String getFormKey() {
        String formKey = FORM_KEY_HOLDER.get();
        if (formKey == null) {
            throw new IllegalStateException("formKey 没有设置，请先调用 setFormKey 方法设置 formKey");
        }
        return formKey;
    }

    @Override
    protected BaseMapper<MarketingFormField> getResourceFieldMapper() {
        return marketingFormFieldMapper;
    }

    @Override
    protected BaseMapper<MarketingFormFieldBlob> getResourceFieldBlobMapper() {
        return marketingFormFieldBlobMapper;
    }
}
