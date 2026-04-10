package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.dal.mysql.withdraw.CpsWithdrawMapper;
import com.qiji.cps.module.cps.enums.CpsWithdrawStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.WITHDRAW_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.WITHDRAW_STATUS_INVALID;

/**
 * CPS 提现申请 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
public class CpsWithdrawServiceImpl implements CpsWithdrawService {

    @Resource
    private CpsWithdrawMapper withdrawMapper;

    @Override
    public PageResult<CpsWithdrawDO> getWithdrawPage(CpsWithdrawPageReqVO reqVO) {
        return withdrawMapper.selectPage(reqVO);
    }

    @Override
    public CpsWithdrawDO getWithdraw(Long id) {
        return withdrawMapper.selectById(id);
    }

    @Override
    public void approveWithdraw(Long id, String reviewNote) {
        CpsWithdrawDO withdraw = withdrawMapper.selectById(id);
        if (withdraw == null) {
            throw exception(WITHDRAW_NOT_EXISTS);
        }
        if (!CpsWithdrawStatusEnum.CREATED.getStatus().equals(withdraw.getStatus())
                && !CpsWithdrawStatusEnum.REVIEWING.getStatus().equals(withdraw.getStatus())) {
            throw exception(WITHDRAW_STATUS_INVALID);
        }
        CpsWithdrawDO update = new CpsWithdrawDO();
        update.setId(id);
        update.setStatus(CpsWithdrawStatusEnum.PASSED.getStatus());
        update.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setReviewNote(reviewNote);
        withdrawMapper.updateById(update);
    }

    @Override
    public void rejectWithdraw(Long id, String reviewNote) {
        CpsWithdrawDO withdraw = withdrawMapper.selectById(id);
        if (withdraw == null) {
            throw exception(WITHDRAW_NOT_EXISTS);
        }
        if (!CpsWithdrawStatusEnum.CREATED.getStatus().equals(withdraw.getStatus())
                && !CpsWithdrawStatusEnum.REVIEWING.getStatus().equals(withdraw.getStatus())) {
            throw exception(WITHDRAW_STATUS_INVALID);
        }
        CpsWithdrawDO update = new CpsWithdrawDO();
        update.setId(id);
        update.setStatus(CpsWithdrawStatusEnum.REJECTED.getStatus());
        update.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setReviewNote(reviewNote);
        withdrawMapper.updateById(update);
    }

}
