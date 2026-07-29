package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.RoleFieldMask;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.request.RoleFieldMaskRequest;
import cn.cordys.crm.system.service.FieldMaskConfigService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色字段脱敏配置接口
 */
@RestController
@RequestMapping("/field-mask")
@Tag(name = "角色字段脱敏配置")
public class FieldMaskConfigController {

    @Resource
    private FieldMaskConfigService fieldMaskConfigService;

    /**
     * 获取某模块可配置脱敏的字段列表
     *
     * @param moduleKey 模块key(customer/opportunity/order/contact)
     */
    @GetMapping("/fields/{moduleKey}")
    @Operation(summary = "获取模块可脱敏字段列表")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_ROLE_READ})
    public List<BaseField> getMaskableFields(@PathVariable("moduleKey") String moduleKey) {
        return fieldMaskConfigService.getMaskableFields(moduleKey, OrganizationContext.getOrganizationId());
    }

    /**
     * 获取角色的脱敏配置(回显)
     *
     * @param roleId 角色ID
     */
    @GetMapping("/config/{roleId}")
    @Operation(summary = "获取角色脱敏配置")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_ROLE_READ})
    public List<RoleFieldMask> getConfig(@PathVariable("roleId") String roleId) {
        return fieldMaskConfigService.getMasksByRole(roleId);
    }

    /**
     * 保存角色的脱敏配置
     */
    @PostMapping("/config")
    @Operation(summary = "保存角色脱敏配置")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_ROLE_UPDATE})
    public void saveConfig(@Validated @RequestBody RoleFieldMaskRequest request) {
        List<RoleFieldMask> masks = new ArrayList<>();
        if (request.getMasks() != null) {
            for (RoleFieldMaskRequest.MaskItem item : request.getMasks()) {
                RoleFieldMask mask = new RoleFieldMask();
                mask.setModuleKey(item.getModuleKey());
                mask.setFieldId(item.getFieldId());
                mask.setFieldKey(item.getFieldKey());
                mask.setFieldType(item.getFieldType());
                mask.setOrganizationId(OrganizationContext.getOrganizationId());
                masks.add(mask);
            }
        }
        fieldMaskConfigService.saveMasks(request.getRoleId(), masks, SessionUtils.getUserId());
    }
}
