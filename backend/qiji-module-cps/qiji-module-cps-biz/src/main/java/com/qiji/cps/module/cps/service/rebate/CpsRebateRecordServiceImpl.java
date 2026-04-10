package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * CPS 返利记录 Service 实现
 *
 * @author CPS System
 */
@Slf4j
@Service
@Validated
public class CpsRebateRecordServiceImpl implements CpsRebateRecordService {

    @Resource
    private CpsRebateRecordMapper rebateRecordMapper;

    @Resource
    private CpsRebateSettleService rebateSettleService;

    @Override
    public PageResult<CpsRebateRecordDO> getRebateRecordPage(CpsRebateRecordPageReqVO pageReqVO) {
        return rebateRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public CpsRebateRecordDO getRebateRecord(Long id) {
        return rebateRecordMapper.selectById(id);
    }

    @Override
    public PageResult<CpsRebateRecordDO> getMemberRebateRecordPage(Long memberId, Integer pageNo, Integer pageSize) {
        CpsRebateRecordPageReqVO reqVO = new CpsRebateRecordPageReqVO();
        reqVO.setMemberId(memberId);
        reqVO.setPageNo(pageNo != null ? pageNo : 1);
        reqVO.setPageSize(pageSize != null ? pageSize : 10);
        return rebateRecordMapper.selectPage(reqVO);
    }

    @Override
    public boolean reverseRebate(Long orderId) {
        log.info("[reverseRebate] 触发订单退款回扣, orderId={}", orderId);
        return rebateSettleService.reverseRebate(orderId);
    }

}
