package com.qiji.cps.module.cps.controller.app.membergoods;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordPageReqVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordRespVO;
import com.qiji.cps.module.cps.controller.app.membergoods.vo.AppCpsMemberGoodsRecordSaveReqVO;
import com.qiji.cps.module.cps.service.membergoods.CpsMemberGoodsRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 APP - CPS 浏览足迹")
@RestController
@RequestMapping("/cps/browse-history")
@Validated
public class AppCpsBrowseHistoryController {

    @Resource
    private CpsMemberGoodsRecordService memberGoodsRecordService;

    @PostMapping("/record")
    @Operation(summary = "记录商品浏览足迹")
    public CommonResult<Boolean> record(@Valid @RequestBody AppCpsMemberGoodsRecordSaveReqVO reqVO) {
        memberGoodsRecordService.recordBrowse(SecurityFrameworkUtils.getLoginUserId(), reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取我的浏览足迹")
    public CommonResult<PageResult<AppCpsMemberGoodsRecordRespVO>> getPage(
            @Valid AppCpsMemberGoodsRecordPageReqVO reqVO) {
        return success(memberGoodsRecordService.getBrowsePage(SecurityFrameworkUtils.getLoginUserId(), reqVO));
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清空我的浏览足迹")
    public CommonResult<Boolean> clean() {
        memberGoodsRecordService.cleanBrowseHistory(SecurityFrameworkUtils.getLoginUserId());
        return success(true);
    }
}
