package com.qiji.cps.module.cps.dal.mysql.statistics;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.statistics.CpsStatisticsDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * CPS统计数据 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsStatisticsMapper extends BaseMapperX<CpsStatisticsDO> {

    /**
     * 按日期 + 平台编码精确查询
     */
    default CpsStatisticsDO selectByDateAndPlatform(LocalDate date, String platformCode) {
        return selectOne(new LambdaQueryWrapperX<CpsStatisticsDO>()
                .eq(CpsStatisticsDO::getStatDate, date)
                .eq(CpsStatisticsDO::getPlatformCode, platformCode));
    }

    /**
     * 查询日期区间内的趋势数据（按日期升序）
     */
    default List<CpsStatisticsDO> selectTrendList(LocalDate startDate, LocalDate endDate,
                                                   String platformCode) {
        return selectList(new LambdaQueryWrapperX<CpsStatisticsDO>()
                .eq(CpsStatisticsDO::getPlatformCode, platformCode)
                .ge(CpsStatisticsDO::getStatDate, startDate)
                .le(CpsStatisticsDO::getStatDate, endDate)
                .orderByAsc(CpsStatisticsDO::getStatDate));
    }

    /**
     * 查询日期区间内各平台汇总数据（排除 total 行，用于饼图）
     */
    default List<CpsStatisticsDO> selectPlatformSummary(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<CpsStatisticsDO>()
                .ne(CpsStatisticsDO::getPlatformCode, "total")
                .ge(CpsStatisticsDO::getStatDate, startDate)
                .le(CpsStatisticsDO::getStatDate, endDate));
    }

}
