package cn.cordys.crm.workflow.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.workflow.dto.WorkflowDefinitionDTO;
import cn.cordys.crm.workflow.dto.WorkflowSaveRequest;
import cn.cordys.crm.workflow.service.WorkflowService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工作流管理")
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @GetMapping("/list")
    @Operation(summary = "获取工作流列表")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_READ)
    public List<WorkflowDefinitionDTO> list(@RequestParam(required = false) String workflowType) {
        return workflowService.list(OrganizationContext.getOrganizationId(), workflowType);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取工作流详情(含节点和连线)")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_READ)
    public WorkflowDefinitionDTO getDetail(@PathVariable String id) {
        return workflowService.getDetail(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/save")
    @Operation(summary = "保存工作流(新增或更新)")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_UPDATE)
    public String save(@Validated @RequestBody WorkflowSaveRequest request) {
        return workflowService.save(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除工作流")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_UPDATE)
    public void delete(@PathVariable String id) {
        workflowService.delete(id, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/switch/{id}")
    @Operation(summary = "启用/禁用工作流")
    @RequiresPermissions(PermissionConstants.PROCESS_SETTING_UPDATE)
    public void switchEnable(@PathVariable String id) {
        workflowService.switchEnable(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
