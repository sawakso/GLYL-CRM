package cn.cordys.crm.marketingform.controller;

import cn.cordys.common.util.BeanUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.marketingform.domain.MarketingForm;
import cn.cordys.crm.marketingform.dto.request.MarketingFormSubmitRequest;
import cn.cordys.crm.marketingform.dto.response.MarketingFormPublicResponse;
import cn.cordys.crm.marketingform.service.MarketingFormService;
import cn.cordys.crm.marketingform.service.MarketingLeadBridgeService;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公开表单端点 (意向客户扫码填写, 免登录)。
 * Shiro 放行 /pub/** 为 anon。orgId 从 token 解析的 marketing_form 记录获取,
 * 不依赖 SessionUtils。返回的配置不暴露目标池/映射规则等内部信息。
 */
@Slf4j
@Tag(name = "市场表单-公开")
@RestController
@RequestMapping("/pub/marketing-form")
public class MarketingFormPublicController {

    @Resource
    private MarketingFormService marketingFormService;
    @Resource
    private MarketingLeadBridgeService marketingLeadBridgeService;
    @Resource
    private ModuleFormService moduleFormService;

    @GetMapping("/{token}")
    @Operation(summary = "获取公开表单配置 (客户扫码后渲染填写表单)")
    public MarketingFormPublicResponse getForm(@PathVariable String token) {
        MarketingForm form = marketingFormService.getByToken(token);
        if (form == null) {
            return null;
        }

        MarketingFormPublicResponse resp = new MarketingFormPublicResponse();
        resp.setName(form.getName());
        resp.setDescription(form.getDescription());
        resp.setOrganizationId(form.getOrganizationId());
        resp.setDedupTip(marketingFormService.buildDedupTip(form));
        resp.setRequireName(Boolean.TRUE.equals(form.getRequireName()));

        // 设置 org 上下文以读取字段定义 (ModuleFormService.getBusinessFormConfig 需要 orgId)
        OrganizationContext.setOrganizationId(form.getOrganizationId());
        try {
            ModuleFormConfigDTO config = moduleFormService.getBusinessFormConfig(form.getId(), form.getOrganizationId());
            if (config != null) {
                resp.setFields(config.getFields());
                resp.setFormProp(config.getFormProp());
            }
        } finally {
            OrganizationContext.clear();
        }
        return resp;
    }

    @PostMapping("/{token}/submit")
    @Operation(summary = "提交表单 (自动回流成线索)")
    public String submit(@PathVariable String token, @Validated @RequestBody MarketingFormSubmitRequest request,
                         HttpServletRequest httpRequest) {
        try {
            return marketingLeadBridgeService.bridge(token, request, httpRequest);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发去重兜底: 同表单同身份并发提交时, clue.dedup_fingerprint 唯一索引冲突,
            // 复用已存在线索, 不重复创建 (整个创建事务已回滚, 无脏数据)
            log.warn("市场表单 {} 并发提交去重冲突, 尝试复用已有线索: {}", token, e.getMessage());
            String existingId = marketingLeadBridgeService.resolveExistingClueId(token, request, httpRequest);
            if (StringUtils.isNotBlank(existingId)) {
                return existingId;
            }
            throw e;
        }
    }
}
