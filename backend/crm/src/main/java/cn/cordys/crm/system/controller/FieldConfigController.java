package cn.cordys.crm.system.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
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
        ModuleField mf = new ModuleField();
        mf.setId(fieldId);
        mf.setFormId(forms.get(0).getId());
        mf.setName(name);
        mf.setType(type);
        mf.setInternalKey(idKey);
        mf.setPos(System.currentTimeMillis() % 100000);
        mf.setMobile(true);
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
        return Map.of("id", fieldId);
    }

    @DeleteMapping("/{formKey}/{fieldId}")
    @Operation(summary = "删除字段")
    public String delete(@PathVariable String formKey, @PathVariable String fieldId) {
        moduleFieldMapper.deleteByPrimaryKey(fieldId);
        moduleFieldBlobMapper.deleteByPrimaryKey(fieldId);
        return "OK";
    }
}
