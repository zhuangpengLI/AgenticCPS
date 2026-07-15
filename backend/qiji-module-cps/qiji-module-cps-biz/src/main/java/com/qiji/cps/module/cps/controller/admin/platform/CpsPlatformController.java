package com.qiji.cps.module.cps.controller.admin.platform;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformConnectionTestRespVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformPageReqVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformRespVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS平台配置")
@RestController
@RequestMapping("/cps/platform")
@Validated
public class CpsPlatformController {

    @Resource
    private CpsPlatformService platformService;
    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @PostMapping("/create")
    @Operation(summary = "创建CPS平台配置")
    @PreAuthorize("@ss.hasPermission('cps:platform:create')")
    public CommonResult<Long> createPlatform(@Valid @RequestBody CpsPlatformSaveReqVO createReqVO) {
        return success(platformService.createPlatform(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新CPS平台配置")
    @PreAuthorize("@ss.hasPermission('cps:platform:update')")
    public CommonResult<Boolean> updatePlatform(@Valid @RequestBody CpsPlatformSaveReqVO updateReqVO) {
        platformService.updatePlatform(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除CPS平台配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:platform:delete')")
    public CommonResult<Boolean> deletePlatform(@RequestParam("id") Long id) {
        platformService.deletePlatform(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取CPS平台配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:platform:query')")
    public CommonResult<CpsPlatformRespVO> getPlatform(@RequestParam("id") Long id) {
        CpsPlatformDO platform = platformService.getPlatform(id);
        return success(BeanUtils.toBean(platform, CpsPlatformRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取CPS平台配置分页")
    @PreAuthorize("@ss.hasPermission('cps:platform:query')")
    public CommonResult<PageResult<CpsPlatformRespVO>> getPlatformPage(@Valid CpsPlatformPageReqVO pageReqVO) {
        PageResult<CpsPlatformDO> pageResult = platformService.getPlatformPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsPlatformRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获取已启用的平台列表")
    @PreAuthorize("@ss.hasPermission('cps:platform:query')")
    public CommonResult<List<CpsPlatformRespVO>> getEnabledPlatformList() {
        List<CpsPlatformDO> list = platformService.getEnabledPlatformList();
        return success(BeanUtils.toBean(list, CpsPlatformRespVO.class));
    }

    @GetMapping("/test-connection")
    @Operation(summary = "测试 CPS 平台连接")
    @Parameter(name = "platformCode", description = "平台编码", required = true)
    @PreAuthorize("@ss.hasPermission('cps:platform:query')")
    public CommonResult<CpsPlatformConnectionTestRespVO> testConnection(@RequestParam("platformCode") String platformCode) {
        CpsPlatformClient client = platformClientFactory.getClient(platformCode);
        if (client == null) {
            return success(CpsPlatformConnectionTestRespVO.builder()
                    .platformCode(platformCode)
                    .supported(false)
                    .success(false)
                    .failureReason("未找到平台适配器")
                    .build());
        }
        try {
            boolean success = client.testConnection();
            return success(CpsPlatformConnectionTestRespVO.builder()
                    .platformCode(platformCode)
                    .supported(true)
                    .success(success)
                    .failureReason(success ? null : "平台适配器返回连接失败")
                    .build());
        } catch (Exception ex) {
            return success(CpsPlatformConnectionTestRespVO.builder()
                    .platformCode(platformCode)
                    .supported(true)
                    .success(false)
                    .failureReason(ex.getMessage())
                    .build());
        }
    }

}
