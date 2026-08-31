package com.qiji.cps.module.cps.dal.mysql.refund;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportImportDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpsRefundReportImportMapper extends BaseMapperX<CpsRefundReportImportDO> {
    default CpsRefundReportImportDO selectByFileHash(String source, String fileHash) {
        return selectOne(new LambdaQueryWrapperX<CpsRefundReportImportDO>()
                .eq(CpsRefundReportImportDO::getSource, source)
                .eq(CpsRefundReportImportDO::getFileHash, fileHash));
    }
}
