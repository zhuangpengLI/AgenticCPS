package com.qiji.cps.module.cps.controller.admin.onboarding;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.*;
import com.qiji.cps.module.cps.service.onboarding.CpsPlatformOnboardingLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS平台配置中心")
@RestController
@RequestMapping("/cps/platform-onboarding")
@Validated
public class CpsPlatformOnboardingController {

    @Resource
    private CpsPlatformOnboardingLifecycleService lifecycleService;

    @GetMapping("/page")
    @Operation(summary = "获取平台配置中心聚合分页")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:query')")
    public CommonResult<PageResult<CpsPlatformOnboardingPageRespVO>> getPage(
            @Valid CpsPlatformOnboardingPageReqVO request) {
        return success(lifecycleService.getPage(request));
    }

    @GetMapping("/get")
    @Operation(summary = "获取平台运行配置和草稿")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:query')")
    public CommonResult<CpsPlatformOnboardingDetailRespVO> get(
            @RequestParam("platformCode") String platformCode) {
        return success(lifecycleService.getDetail(platformCode));
    }

    @PostMapping("/draft")
    @Operation(summary = "创建或更新平台接入草稿")
    @PreAuthorize("@ss.hasPermission(#request.draftVersion == null ? "
            + "'cps:platform-onboarding:create' : 'cps:platform-onboarding:update')")
    public CommonResult<CpsPlatformOnboardingDetailRespVO> saveDraft(
            @Valid @RequestBody CpsPlatformOnboardingDraftSaveReqVO request) {
        return success(lifecycleService.saveDraft(request));
    }

    @DeleteMapping("/draft")
    @Operation(summary = "删除平台接入草稿")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:delete')")
    public CommonResult<Boolean> deleteDraft(
            @RequestParam(value = "platformCode", required = false) String platformCode,
            @RequestParam(value = "draftVersion", required = false) Long draftVersion,
            @Valid @RequestBody(required = false) CpsPlatformOnboardingDraftDeleteReqVO body) {
        CpsPlatformOnboardingDraftDeleteReqVO request =
                body == null ? new CpsPlatformOnboardingDraftDeleteReqVO() : body;
        if (platformCode != null) {
            request.setPlatformCode(platformCode);
        }
        if (draftVersion != null) {
            request.setDraftVersion(draftVersion);
        }
        lifecycleService.deleteDraft(request);
        return success(true);
    }

    @PostMapping("/validate")
    @Operation(summary = "校验平台接入草稿")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:update')")
    public CommonResult<CpsPlatformOnboardingCheckRespVO> validate(
            @Valid @RequestBody CpsPlatformOnboardingValidateReqVO request) {
        return success(lifecycleService.validate(request));
    }

    @PostMapping("/test")
    @Operation(summary = "测试平台接入连接和能力")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:test')")
    public CommonResult<CpsPlatformOnboardingCheckRespVO> test(
            @Valid @RequestBody CpsPlatformOnboardingTestReqVO request) {
        return success(lifecycleService.test(request));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布平台接入草稿")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:publish')")
    public CommonResult<CpsPlatformOnboardingDetailRespVO> publish(
            @Valid @RequestBody CpsPlatformOnboardingPublishReqVO request) {
        return success(lifecycleService.publish(request));
    }

    @PutMapping("/enable")
    @Operation(summary = "启用已发布平台")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:publish')")
    public CommonResult<Boolean> enable(
            @Valid @RequestBody CpsPlatformOnboardingLifecycleReqVO request) {
        lifecycleService.enablePlatform(request.getPlatformCode());
        return success(true);
    }

    @PutMapping("/disable")
    @Operation(summary = "停用平台")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:update')")
    public CommonResult<Boolean> disable(
            @Valid @RequestBody CpsPlatformOnboardingLifecycleReqVO request) {
        lifecycleService.disablePlatform(request.getPlatformCode());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除已停用平台配置包")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:delete')")
    public CommonResult<Boolean> delete(@RequestParam("platformCode") String platformCode) {
        lifecycleService.deletePlatformBundle(platformCode);
        return success(true);
    }

    @GetMapping("/platform-capabilities")
    @Operation(summary = "获取已注册平台能力")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:query')")
    public CommonResult<List<CpsPlatformCapabilityRespVO>> platformCapabilities(
            @RequestParam(value = "platformCode", required = false) String platformCode) {
        return success(lifecycleService.getPlatformCapabilities(platformCode));
    }

    @GetMapping("/vendor-descriptors")
    @Operation(summary = "获取供应商能力描述符")
    @PreAuthorize("@ss.hasPermission('cps:platform-onboarding:query')")
    public CommonResult<List<CpsVendorDescriptorRespVO>> vendorDescriptors(
            @RequestParam(value = "platformCode", required = false) String platformCode) {
        return success(lifecycleService.getVendorDescriptors(platformCode));
    }
}
