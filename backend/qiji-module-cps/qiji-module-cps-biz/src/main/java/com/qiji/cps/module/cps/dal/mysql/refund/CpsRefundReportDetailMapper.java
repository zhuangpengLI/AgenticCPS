package com.qiji.cps.module.cps.dal.mysql.refund;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportDetailDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CpsRefundReportDetailMapper extends BaseMapperX<CpsRefundReportDetailDO> {
    default List<CpsRefundReportDetailDO> selectListByImportId(Long importId) {
        return selectList(new LambdaQueryWrapperX<CpsRefundReportDetailDO>()
                .eq(CpsRefundReportDetailDO::getImportId, importId)
                .orderByAsc(CpsRefundReportDetailDO::getId));
    }
    default CpsRefundReportDetailDO selectByImportAndOrder(Long importId, String platformCode, String orderId) {
        return selectOne(new LambdaQueryWrapperX<CpsRefundReportDetailDO>()
                .eq(CpsRefundReportDetailDO::getImportId, importId)
                .eq(CpsRefundReportDetailDO::getPlatformCode, platformCode)
                .eq(CpsRefundReportDetailDO::getPlatformOrderId, orderId));
    }
}
