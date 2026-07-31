package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.request.ObjectConfigRenameRequest;
import cn.cordys.crm.system.dto.response.ObjectConfigListResponse;
import cn.cordys.crm.system.service.ObjectConfigService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "对象管理")
@RestController
@RequestMapping("/object-config")
public class ObjectConfigController {

    @Resource
    private ObjectConfigService objectConfigService;

    @GetMapping("/list")
    @Operation(summary = "获取统一对象列表(预设+自定义)")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_READ)
    public List<ObjectConfigListResponse> list() {
        return objectConfigService.list(OrganizationContext.getOrganizationId());
    }

    @PostMapping("/rename")
    @Operation(summary = "重命名对象")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
    public void rename(@Validated @RequestBody ObjectConfigRenameRequest request) {
        objectConfigService.rename(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/switch/{key}")
    @Operation(summary = "启用/禁用对象")
    @RequiresPermissions(PermissionConstants.MODULE_SETTING_UPDATE)
    public void switchEnable(@PathVariable String key) {
        objectConfigService.switchEnable(key, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/name/{key}")
    @Operation(summary = "获取对象名称")
    public String getObjectName(@PathVariable String key) {
        return objectConfigService.getObjectName(key, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/name-map")
    @Operation(summary = "批量获取所有对象名称(供前端缓存)")
    public Map<String, String> getObjectNameMap() {
        return objectConfigService.getObjectNameMap(OrganizationContext.getOrganizationId());
    }
}
