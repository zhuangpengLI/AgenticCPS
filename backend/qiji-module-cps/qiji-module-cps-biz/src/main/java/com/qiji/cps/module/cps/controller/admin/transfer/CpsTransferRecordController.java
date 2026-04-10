package com.qiji.cps.module.cps.controller.admin.transfer;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordRespVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.service.transfer.CpsTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS转链记录 Controller
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS转链记录")
@RestController
@RequestMapping("/cps/transfer-record")
@Validated
public class CpsTransferRecordController {

    @Resource
    private CpsTransferService transferService;

    @GetMapping("/page")
    @Operation(summary = "转链记录分页查询")
    @PreAuthorize("@ss.hasPermission('cps:transfer-record:query')")
    public CommonResult<PageResult<CpsTransferRecordRespVO>> getTransferPage(
            @Valid CpsTransferRecordPageReqVO reqVO) {
        PageResult<CpsTransferRecordDO> page = transferService.getTransferPage(reqVO);
        return success(BeanUtils.toBean(page, CpsTransferRecordRespVO.class));
    }

}
