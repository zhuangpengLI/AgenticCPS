package com.qiji.cps.module.cps.service.transfer;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * CPS 转链记录 Service 实现类
 *
 * @author CPS System
 */
@Service
@Validated
public class CpsTransferServiceImpl implements CpsTransferService {

    @Resource
    private CpsTransferRecordMapper transferMapper;

    @Override
    public PageResult<CpsTransferRecordDO> getTransferPage(CpsTransferRecordPageReqVO reqVO) {
        return transferMapper.selectPage(reqVO);
    }

    @Override
    public long countTodayByMember(Long memberId) {
        return transferMapper.countTodayByMember(memberId, LocalDate.now());
    }

}
