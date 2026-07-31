package cn.cordys.crm.marketingform.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.marketingform.dto.request.MarketingFormAddRequest;
import cn.cordys.crm.marketingform.dto.request.MarketingFormUpdateRequest;
import cn.cordys.crm.marketingform.dto.response.MarketingFormGetResponse;
import cn.cordys.crm.marketingform.dto.response.MarketingFormListResponse;
import cn.cordys.crm.marketingform.service.MarketingFormService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "市场活动表单")
@RestController
@RequestMapping("/marketing-form")
public class MarketingFormController {

    @Resource
    private MarketingFormService marketingFormService;

    @GetMapping("/list")
    @Operation(summary = "市场表单列表")
    @CsPermission(PermissionConstants.MARKETING_FORM_READ)
    public List<MarketingFormListResponse> list() {
        return marketingFormService.list(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "市场表单详情")
    @CsPermission(PermissionConstants.MARKETING_FORM_READ)
    public MarketingFormGetResponse get(@PathVariable String id) {
        return marketingFormService.get(id, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "新建市场表单")
    @CsPermission(PermissionConstants.MARKETING_FORM_ADD)
    public MarketingForm add(@Validated @RequestBody MarketingFormAddRequest request) {
        return marketingFormService.create(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "更新市场表单")
    @CsPermission(PermissionConstants.MARKETING_FORM_UPDATE)
    public void update(@Validated @RequestBody MarketingFormUpdateRequest request) {
        marketingFormService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/status/{id}/{status}")
    @Operation(summary = "更新状态 (DRAFT/ACTIVE/CLOSED)")
    @CsPermission(PermissionConstants.MARKETING_FORM_UPDATE)
    public void updateStatus(@PathVariable String id, @PathVariable String status) {
        marketingFormService.updateStatus(id, status, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除市场表单")
    @CsPermission(PermissionConstants.MARKETING_FORM_DELETE)
    public void delete(@PathVariable String id) {
        marketingFormService.delete(id);
    }
}
