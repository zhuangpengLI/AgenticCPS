package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import com.qiji.cps.module.cps.controller.app.withdraw.vo.AppCpsWithdrawCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.dal.mysql.withdraw.CpsWithdrawMapper;
import com.qiji.cps.module.cps.enums.CpsWithdrawStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.WITHDRAW_NOT_EXISTS;

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
    @Resource
    private CpsWithdrawStepExecutor stepExecutor;
    @Resource
    private CpsWithdrawTransferExecutor transferExecutor;

    @Override
    public Long createWithdraw(Long memberId, AppCpsWithdrawCreateReqVO request) {
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("登录会员不能为空");
        CpsWithdrawDO existing = withdrawMapper.selectByIdempotencyKey(request.getIdempotencyKey());
        if (existing != null) return validateReplay(existing, memberId, request);
        CpsWithdrawCreateCommand command = new CpsWithdrawCreateCommand(memberId, request.getAmountCent(),
                request.getWithdrawType(), request.getWithdrawAccount(), request.getWithdrawAccountName(),
                request.getIdempotencyKey());
        try {
            return stepExecutor.createAndFreeze(command).getId();
        } catch (DuplicateKeyException duplicate) {
            existing = withdrawMapper.selectByIdempotencyKey(request.getIdempotencyKey());
            if (existing == null) throw duplicate;
            return validateReplay(existing, memberId, request);
        }
    }

    private Long validateReplay(CpsWithdrawDO existing, Long memberId, AppCpsWithdrawCreateReqVO request) {
        if (!memberId.equals(existing.getMemberId()) || !request.getAmountCent().equals(existing.getAmountCent())
                || !Objects.equals(request.getWithdrawType(), existing.getWithdrawType())
                || !Objects.equals(request.getWithdrawAccount(), existing.getWithdrawAccount())
                || !Objects.equals(request.getWithdrawAccountName(), existing.getWithdrawAccountName())) {
            throw new IllegalStateException("幂等键已用于其他提现申请");
        }
        return existing.getId();
    }

    @Override
    public PageResult<CpsWithdrawDO> getMemberWithdrawPage(Long memberId, int pageNo, int pageSize) {
        return withdrawMapper.selectMemberPage(memberId, pageNo, pageSize);
    }

    @Override
    public CpsWithdrawDO getMemberWithdraw(Long memberId, Long id) {
        CpsWithdrawDO withdraw = withdrawMapper.selectByMemberIdAndId(memberId, id);
        if (withdraw == null) throw exception(WITHDRAW_NOT_EXISTS);
        return withdraw;
    }

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
        CpsWithdrawDO withdraw = stepExecutor.markReviewing(id, reviewNote,
                SecurityFrameworkUtils.getLoginUserId());
        if (withdraw != null && CpsWithdrawStatusEnum.REVIEWING.getStatus().equals(withdraw.getStatus())) {
            transferExecutor.startTransfer(id);
        }
    }

    @Override
    public void rejectWithdraw(Long id, String reviewNote) {
        stepExecutor.rejectAndUnfreeze(id, reviewNote, "withdraw-reject:" + id);
    }

}
