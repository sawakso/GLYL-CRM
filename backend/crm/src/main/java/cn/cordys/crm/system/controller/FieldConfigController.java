package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.SystemService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.List;

/**
 * 对象字段配置管理: 列出 / 新增 / 停用启用 / 删除字段。
 */
@RestController
@RequestMapping("/field-config")
@Tag(name = "字段配置管理")
public class FieldConfigController {

    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private SystemService systemService;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;

    @GetMapping("/{formKey}/list")
    @Operation(summary = "获取对象全部字段")
    public List<Map<String, Object>> list(@PathVariable String formKey) {
        List<BaseField> fields = moduleFormService.getAllFields(formKey, OrganizationContext.getOrganizationId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseField f : fields) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId() == null ? "" : f.getId());
            m.put("name", f.getName() == null ? "" : f.getName());
            m.put("type", f.getType() == null ? "" : f.getType());
            m.put("internalKey", f.getInternalKey() == null ? "" : f.getInternalKey());
            m.put("readable", f.getReadable() != null && f.getReadable());
            m.put("editable", f.getEditable() != null && f.getEditable());
            m.put("pos", f.getPos() == null ? 0L : f.getPos());
            m.put("businessKey", f.getBusinessKey() == null ? "" : f.getBusinessKey());
            result.add(m);
        }
        return result;
    }

    @PostMapping("/{formKey}/toggle/{fieldId}")
    @Operation(summary = "切换字段停用/启用 (readable)")
    public String toggle(@PathVariable String formKey, @PathVariable String fieldId) {
        List<BaseField> fields = moduleFormService.getAllFields(formKey, OrganizationContext.getOrganizationId());
        for (BaseField f : fields) {
            if (fieldId.equals(f.getId())) {
                boolean enabled = f.getReadable() == null || f.getReadable();
                f.setReadable(!enabled);
                ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(fieldId);
                if (blob != null) {
                    blob.setProp(JSON.toJSONString(f));
                    moduleFieldBlobMapper.updateById(blob);
                }
                // 停用/启用后刷新表单缓存, 避免市场表单等仍读到旧的字段配置
                systemService.clearFormCache();
                return enabled ? "DISABLED" : "ENABLED";
            }
        }
        throw new RuntimeException("字段不存在: " + fieldId);
    }

    @PostMapping("/{formKey}/add")
    @Operation(summary = "新增字段")
    public Map<String, String> add(@PathVariable String formKey, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String type = (String) body.get("type");
        String internalKey = (String) body.get("internalKey");
        if (StringUtils.isBlank(name) || StringUtils.isBlank(type)) {
            throw new RuntimeException("字段名称和类型不能为空");
        }
        // 查 form
        LambdaQueryWrapper<ModuleForm> fw = new LambdaQueryWrapper<>();
        fw.eq(ModuleForm::getFormKey, formKey);
        fw.eq(ModuleForm::getOrganizationId, OrganizationContext.getOrganizationId());
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(fw);
        if (forms == null || forms.isEmpty()) {
            throw new RuntimeException("对象不存在: " + formKey);
        }
        // 生成字段
        String fieldId = IDGenerator.nextStr();
        String idKey = StringUtils.defaultIfBlank(internalKey, fieldId);
        // ModuleField
        long now = System.currentTimeMillis();
        String operatorId = SessionUtils.getUserId();
        ModuleField mf = new ModuleField();
        mf.setId(fieldId);
        mf.setFormId(forms.get(0).getId());
        mf.setName(name);
        mf.setType(type);
        mf.setInternalKey(idKey);
        mf.setPos(System.currentTimeMillis() % 100000);
        mf.setMobile(true);
        mf.setCreateUser(operatorId);
        mf.setUpdateUser(operatorId);
        mf.setCreateTime(now);
        mf.setUpdateTime(now);
        moduleFieldMapper.insert(mf);
        // blob prop
        ModuleFieldBlob blob = new ModuleFieldBlob();
        blob.setId(fieldId);
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("id", fieldId);
        prop.put("name", name);
        prop.put("internalKey", idKey);
        prop.put("type", type);
        prop.put("readable", true);
        prop.put("editable", true);
        prop.put("showLabel", true);
        prop.put("mobile", true);
        prop.put("fieldWidth", 1.0);
        prop.put("rules", new ArrayList<>());
        blob.setProp(JSON.toJSONString(prop));
        moduleFieldBlobMapper.insert(blob);
        // 新增字段后刷新表单缓存, 使市场表单等能立即引用新字段
        systemService.clearFormCache();
        return Map.of("id", fieldId);
    }

    @PostMapping("/{formKey}/update")
    @Operation(summary = "更新字段名称与key")
    public String update(@PathVariable String formKey, @RequestBody Map<String, Object> body) {
        String fieldId = (String) body.get("id");
        String name = (String) body.get("name");
        String internalKey = (String) body.get("internalKey");
        String businessKey = (String) body.get("businessKey");
        if (StringUtils.isBlank(fieldId)) {
            throw new RuntimeException("字段ID不能为空");
        }
        ModuleField mf = moduleFieldMapper.selectByPrimaryKey(fieldId);
        if (mf == null) {
            throw new RuntimeException("字段不存在: " + fieldId);
        }
        ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(fieldId);
        if (blob == null) {
            throw new RuntimeException("字段配置不存在: " + fieldId);
        }
        BaseField field = JSON.parseObject(blob.getProp(), BaseField.class);
        if (field == null) {
            throw new RuntimeException("字段配置解析失败: " + fieldId);
        }
        // 更新名称(标题) / internalKey / businessKey
        if (StringUtils.isNotBlank(name)) {
            mf.setName(name);
            field.setName(name);
        }
        if (StringUtils.isNotBlank(internalKey)) {
            mf.setInternalKey(internalKey);
            field.setInternalKey(internalKey);
        }
        if (businessKey != null) {
            field.setBusinessKey(StringUtils.isBlank(businessKey) ? null : businessKey);
        }
        mf.setUpdateUser(SessionUtils.getUserId());
        mf.setUpdateTime(System.currentTimeMillis());
        moduleFieldMapper.updateById(mf);
        blob.setProp(JSON.toJSONString(field));
        moduleFieldBlobMapper.updateById(blob);
        // 刷新表单缓存, 使改动生效
        systemService.clearFormCache();
        return "OK";
    }

    @PostMapping("/{formKey}/{fieldId}")
    @Operation(summary = "删除字段")
    public String delete(@PathVariable String formKey, @PathVariable String fieldId) {
        // 必须先将字段停用(readable=false)才能删除
        ModuleFieldBlob blob = moduleFieldBlobMapper.selectByPrimaryKey(fieldId);
        if (blob != null && StringUtils.isNotBlank(blob.getProp())) {
            BaseField field = JSON.parseObject(blob.getProp(), BaseField.class);
            // 姓名字段(映射name)必填且必须存在, 非管理员不允许删除
            if (field != null && "name".equals(field.getBusinessKey()) && !InternalUser.ADMIN.getValue().equals(SessionUtils.getUserId())) {
                throw new RuntimeException("姓名是必填字段，不允许删除");
            }
            if (field != null && Boolean.TRUE.equals(field.getReadable())) {
                throw new RuntimeException("请先停用该字段再删除");
            }
        }
        moduleFieldMapper.deleteByPrimaryKey(fieldId);
        moduleFieldBlobMapper.deleteByPrimaryKey(fieldId);
        // 删除字段后刷新表单缓存, 避免市场表单等仍显示已删除的字段
        systemService.clearFormCache();
        return "OK";
    }
}
