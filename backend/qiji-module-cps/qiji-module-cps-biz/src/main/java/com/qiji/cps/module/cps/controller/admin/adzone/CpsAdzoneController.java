package com.qiji.cps.module.cps.controller.admin.adzone;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneBatchCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneBatchCreateRespVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzonePageReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneRespVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdChannelRelationReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPidReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPositionCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsJdPositionQueryReqVO;
import com.qiji.cps.module.cps.client.official.jd.JdOfficialManagementClient;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.adzone.CpsJdRemoteAdzoneService;
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

@Tag(name = "管理后台 - CPS推广位")
@RestController
@RequestMapping("/cps/adzone")
@Validated
public class CpsAdzoneController {

    @Resource
    private CpsAdzoneService adzoneService;
    @Resource
    private CpsJdRemoteAdzoneService jdRemoteAdzoneService;

    @PostMapping("/create")
    @Operation(summary = "创建推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:create')")
    public CommonResult<Long> createAdzone(@Valid @RequestBody CpsAdzoneSaveReqVO createReqVO) {
        return success(adzoneService.createAdzone(createReqVO));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:create')")
    public CommonResult<CpsAdzoneBatchCreateRespVO> batchCreateAdzones(
            @Valid @RequestBody CpsAdzoneBatchCreateReqVO reqVO) {
        return success(adzoneService.batchCreateAdzones(reqVO.getItems()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:update')")
    public CommonResult<Boolean> updateAdzone(@Valid @RequestBody CpsAdzoneSaveReqVO updateReqVO) {
        adzoneService.updateAdzone(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除推广位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:adzone:delete')")
    public CommonResult<Boolean> deleteAdzone(@RequestParam("id") Long id) {
        adzoneService.deleteAdzone(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取推广位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<CpsAdzoneRespVO> getAdzone(@RequestParam("id") Long id) {
        CpsAdzoneDO adzone = adzoneService.getAdzone(id);
        return success(BeanUtils.toBean(adzone, CpsAdzoneRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取推广位分页")
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<PageResult<CpsAdzoneRespVO>> getAdzonePage(@Valid CpsAdzonePageReqVO pageReqVO) {
        PageResult<CpsAdzoneDO> pageResult = adzoneService.getAdzonePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsAdzoneRespVO.class));
    }

    @GetMapping("/list-by-platform")
    @Operation(summary = "获取平台下的推广位列表")
    @Parameter(name = "platformCode", description = "平台编码", required = true)
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<List<CpsAdzoneRespVO>> getAdzoneListByPlatformCode(@RequestParam("platformCode") String platformCode) {
        List<CpsAdzoneDO> list = adzoneService.getAdzoneListByPlatformCode(platformCode);
        return success(BeanUtils.toBean(list, CpsAdzoneRespVO.class));
    }

    @PostMapping("/jd/remote-create")
    @Operation(summary = "创建并同步京东远端推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:create')")
    public CommonResult<List<JdOfficialManagementClient.Position>> createJdRemotePositions(
            @Valid @RequestBody CpsJdPositionCreateReqVO request) {
        return success(jdRemoteAdzoneService.createAndSyncPositions(request));
    }

    @PostMapping("/jd/remote-sync")
    @Operation(summary = "查询并同步京东远端推广位")
    @PreAuthorize("@ss.hasPermission('cps:adzone:query')")
    public CommonResult<JdOfficialManagementClient.PositionPage> syncJdRemotePositions(
            @Valid @RequestBody CpsJdPositionQueryReqVO request) {
        return success(jdRemoteAdzoneService.queryAndSyncPositions(request));
    }

    @PostMapping("/jd/pid")
    @Operation(summary = "获取并同步京东 PID")
    @PreAuthorize("@ss.hasPermission('cps:adzone:create')")
    public CommonResult<String> getJdPid(@Valid @RequestBody CpsJdPidReqVO request) {
        return success(jdRemoteAdzoneService.getAndSyncPid(request));
    }

    @PostMapping("/jd/channel-relation")
    @Operation(summary = "生成京东渠道关系 ID")
    @PreAuthorize("@ss.hasPermission('cps:adzone:create')")
    public CommonResult<Long> createJdChannelRelation(
            @Valid @RequestBody CpsJdChannelRelationReqVO request) {
        return success(jdRemoteAdzoneService.createChannelRelation(request));
    }

}
