package com.qiji.cps.module.cps.controller.admin.transfer;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordRespVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.service.transfer.CpsTransferService;
import com.qiji.cps.module.member.dal.dataobject.user.MemberUserDO;
import com.qiji.cps.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.common.util.collection.CollectionUtils.convertMap;
import static com.qiji.cps.framework.common.util.collection.CollectionUtils.convertSet;

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

    @Resource
    private MemberUserService memberUserService;

    @GetMapping("/page")
    @Operation(summary = "转链记录分页查询")
    @PreAuthorize("@ss.hasPermission('cps:transfer-record:query')")
    public CommonResult<PageResult<CpsTransferRecordRespVO>> getTransferPage(
            @Valid CpsTransferRecordPageReqVO reqVO) {
        PageResult<CpsTransferRecordDO> page = transferService.getTransferPage(reqVO);
        PageResult<CpsTransferRecordRespVO> result = BeanUtils.toBean(page, CpsTransferRecordRespVO.class);
        if (CollectionUtils.isEmpty(result.getList())) {
            return success(result);
        }
        Map<Long, MemberUserDO> userMap = convertMap(
                memberUserService.getUserList(convertSet(page.getList(), CpsTransferRecordDO::getMemberId)),
                MemberUserDO::getId);
        result.getList().forEach(record -> {
            MemberUserDO user = userMap.get(record.getMemberId());
            if (user != null) {
                record.setMemberName(user.getNickname());
            }
        });
        return success(result);
    }

}
